#!/usr/bin/env python3
"""Tiny LAN companion server for the TvPort Android TV dashboard.

Runs on the Mac via a LaunchAgent (starts on login), serves JSON over the LAN with permissive
CORS so the TV can poll it. Read-only — it never writes anything back to the machine.

Endpoints:
  GET /status   -> current Claude Code state (read fresh from ~/.claude/statusbar/state.json)
  GET /events   -> same state as a Server-Sent Events stream (push on change + heartbeat)
  GET /battery  -> battery for this Mac, AirPods/case, and the paired iPhone:
                     mac     <- `pmset -g batt`
                     airpods <- `system_profiler SPBluetoothDataType`
                     phone   <- libimobiledevice (`ideviceinfo`) over USB or Wi-Fi
"""
import json
import os
import re
import socket
import subprocess
import threading
import time
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

STATE_PATH = os.path.expanduser("~/.claude/statusbar/state.json")
PORT = 4040
# A busy state with no activity for this long == the run was stopped/interrupted (the Stop hook
# doesn't fire on Ctrl-C / Esc), so we surface idle instead of a forever-spinning "working".
# Liveness = max(state.ts, transcript mtime). 60s avoids false idles during long no-tool reasoning.
STALE_SECS = 60
# "Done" is a brief confirmation; after this long with no new activity, sleep to idle.
DONE_HOLD_SECS = 90

# ── Effective-state machine ───────────────────────────────────────────────────────────────────
# The raw hooks are ambiguous: Claude Code fires a "waiting for your input" Notification BOTH when
# it genuinely needs you mid-turn (a question/permission) AND right after a turn finishes (idle,
# waiting for your next prompt). The raw message can't tell them apart — but the *context* can:
#   - a wait that arrives while actively working (prev = thinking/tool) -> genuine "needs you"
#   - a wait that arrives after the turn ended   (prev = done/idle)     -> the turn is finished
# We track that here and emit a clean effective state the TV can render directly.
_lock = threading.Lock()
_before = "idle"   # last raw state that wasn't waiting/permission (what we were actually doing)
_eff = "idle"      # current effective state
_eff_since = 0.0   # wall-clock when _eff last changed


def _label(eff, raw):
    if eff == "tool":
        return raw.get("label") or "Working"
    return {
        "thinking": "Thinking…",
        "waiting": raw.get("label") or "Waiting for you",
        "permission": "Awaiting permission",
        "done": "Done",
        "idle": "Idle",
    }.get(eff, "Idle")


def read_state():
    global _before, _eff, _eff_since
    try:
        with open(STATE_PATH, "r") as f:
            raw = json.load(f)
    except Exception:
        raw = {"state": "idle", "label": "Idle", "project": "", "startedAt": 0, "ts": 0, "transcript": ""}

    rs = (raw.get("state") or "idle").lower()
    now = time.time()
    live = float(raw.get("ts", 0) or 0)
    tpath = raw.get("transcript")
    if tpath:
        try:
            live = max(live, os.path.getmtime(os.path.expanduser(tpath)))
        except Exception:
            pass
    fresh = (now - live) <= STALE_SECS

    with _lock:
        if rs not in ("waiting", "permission"):
            _before = rs

        if rs in ("thinking", "tool"):
            new_eff = rs if fresh else "idle"            # stale busy == stopped -> idle
        elif rs == "permission":
            new_eff = "permission"
        elif rs == "waiting":
            # genuine "needs you" only if we were actively working; otherwise the turn finished
            new_eff = "waiting" if _before in ("thinking", "tool") else "done"
            # once settled to idle, a post-turn wait must not resurrect "done"
            if new_eff == "done" and _eff == "idle":
                new_eff = "idle"
        elif rs == "done":
            new_eff = "done"
            # Once this finished turn has auto-slept to idle, a stale "done" file (the Stop hook
            # never rewrites it) must NOT resurrect "done" — that re-flashes the card and re-fires
            # the TV chime on a ~DONE_HOLD loop forever. Mirror the waiting-branch guard.
            if _eff == "idle":
                new_eff = "idle"
        else:
            new_eff = "idle"

        if new_eff != _eff:
            _eff = new_eff
            _eff_since = now

        # "Done" auto-sleeps to idle after a quiet stretch.
        if _eff == "done" and (now - _eff_since) > DONE_HOLD_SECS:
            _eff = "idle"
            _eff_since = now

        eff = _eff

    out = dict(raw)
    out["state"] = eff
    out["label"] = _label(eff, raw)
    if eff not in ("thinking", "tool"):
        out["startedAt"] = 0
    return out


# ── Battery ───────────────────────────────────────────────────────────────────────────────────
# Three sources, all read locally on this Mac and exposed at GET /battery:
#   - this Mac     -> `pmset -g batt`
#   - AirPods/case -> `system_profiler SPBluetoothDataType`
#   - iPhone       -> libimobiledevice (`ideviceinfo`) over USB or Wi-Fi
_batt_lock = threading.Lock()


def mac_battery():
    """This Mac's battery via pmset. Returns {level, charging, present}. Desktops -> present False."""
    try:
        out = subprocess.run(
            ["pmset", "-g", "batt"], capture_output=True, text=True, timeout=3
        ).stdout
    except Exception:
        return {"level": None, "charging": False, "present": False}
    m = re.search(r"(\d+)%", out)
    if not m:
        return {"level": None, "charging": False, "present": False}
    level = int(m.group(1))
    low = out.lower()
    # "AC Power" or a charging/charged status == plugged in; "discharging" == on battery.
    charging = ("ac power" in low or "charging" in low or "charged" in low) and "discharging" not in low
    return {"level": level, "charging": charging, "present": True}


_airpods_cache = {"data": None, "ts": 0.0}
_AIRPODS_TTL = 20  # system_profiler is slow (~1-2s); cache its result between polls


def _pct(v):
    try:
        return int(str(v).strip().rstrip("%"))
    except Exception:
        return None


def airpods_battery():
    """AirPods earbuds + case battery via system_profiler (the source the macOS widget uses).

    Returns {earbuds, case, connected}. None levels == not connected / unavailable.
    """
    now = time.time()
    if _airpods_cache["data"] is not None and (now - _airpods_cache["ts"]) < _AIRPODS_TTL:
        return _airpods_cache["data"]

    result = {"earbuds": None, "case": None, "connected": False}
    try:
        raw = subprocess.run(
            ["system_profiler", "SPBluetoothDataType", "-json"],
            capture_output=True, text=True, timeout=8,
        ).stdout
        data = json.loads(raw)

        # Find the connected device that reports earbud/case battery levels.
        found = {}

        def walk(o):
            if isinstance(o, dict):
                if any(k.startswith("device_batteryLevel") for k in o):
                    found.update(o)
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for it in o:
                    walk(it)

        walk(data)
        left = _pct(found.get("device_batteryLevelLeft"))
        right = _pct(found.get("device_batteryLevelRight"))
        main = _pct(found.get("device_batteryLevelMain"))
        case = _pct(found.get("device_batteryLevelCase"))
        # Earbuds: the widget shows the lower of L/R; fall back to "Main" for single-cup devices.
        levels = [x for x in (left, right) if x is not None]
        earbuds = min(levels) if levels else main
        result = {"earbuds": earbuds, "case": case, "connected": earbuds is not None or case is not None}
    except Exception:
        pass

    _airpods_cache["data"] = result
    _airpods_cache["ts"] = now
    return result


# iPhone battery, two layered sources (a background thread polls both so /battery never blocks):
#   1. libimobiledevice (`ideviceinfo`) — authoritative level + charging, over USB or Wi-Fi, but
#      its Wi-Fi mode only sees the phone while it's awake/recently-unlocked.
#   2. Continuity "Instant Hotspot" — the level the Wi-Fi menu shows, parsed from the unified log.
#      Passive (works even while the phone is locked, as long as it's near the Mac); no charging.
IDEVICE = "/opt/homebrew/bin/ideviceinfo"
_idevice = {"level": None, "charging": False, "present": False, "ts": 0.0}
_hotspot = {"level": None, "ts": 0.0}


def _read_idevice():
    """Try USB first, then network (-n). Returns {level, charging, present}."""
    for net in (False, True):
        args = [IDEVICE, "-q", "com.apple.mobile.battery"]
        if net:
            args.insert(1, "-n")
        try:
            out = subprocess.run(args, capture_output=True, text=True, timeout=6).stdout
        except Exception:
            continue
        cap = re.search(r"BatteryCurrentCapacity:\s*(\d+)", out)
        if not cap:
            continue
        chg = re.search(r"BatteryIsCharging:\s*(\w+)", out)
        charging = bool(chg) and chg.group(1).lower() == "true"
        return {"level": max(0, min(100, int(cap.group(1)))), "charging": charging, "present": True}
    return {"level": None, "charging": False, "present": False}


def _read_hotspot():
    """iPhone level from the unified log's 'SFRemoteHotspotDevice ... battery life: N'. Returns N|None."""
    try:
        out = subprocess.run(
            ["/usr/bin/log", "show", "--last", "6m", "--style", "compact",
             "--predicate", 'eventMessage CONTAINS "SFRemoteHotspotDevice"'],
            capture_output=True, text=True, timeout=12,
        ).stdout
        hits = re.findall(r"battery life: (\d+)", out)
        if hits:
            return max(0, min(100, int(hits[-1])))
    except Exception:
        pass
    return None


def _battery_poller():
    """Refresh the iPhone sources every 30s off the request path."""
    while True:
        dev, hs = _read_idevice(), _read_hotspot()
        now = time.time()
        with _batt_lock:
            _idevice.update(dev)
            _idevice["ts"] = now
            _hotspot["level"] = hs
            _hotspot["ts"] = now
        time.sleep(30)


# An iPhone Shortcut automation can push its battery here (GET /battery/phone?level=NN&charging=1).
# This is the most reliable *wireless* source: works anywhere on Wi-Fi, carries charging, and needs
# no special macOS permission (unlike libimobiledevice's Wi-Fi mode, which the OS blocks for
# background tools). Kept alongside USB + hotspot as the layered iPhone sources.
_phone = {"level": None, "charging": False, "ts": 0.0}
_PHONE_STALE_SECS = 6 * 60 * 60


def phone_battery():
    """Best iPhone source, in priority order:
      1. libimobiledevice over USB  -> authoritative level + charging (only while cabled to the Mac)
      2. Shortcut push              -> level + charging, anywhere on Wi-Fi
      3. Continuity hotspot         -> level only, passive, while broadcasting near the Mac
    """
    now = time.time()
    with _batt_lock:
        dev = dict(_idevice)
        sc = dict(_phone)
        hs = dict(_hotspot)

    if dev["present"] and dev["level"] is not None and (now - dev["ts"]) <= 120:
        return {"level": dev["level"], "charging": dev["charging"], "present": True, "source": "usb"}
    if sc["level"] is not None and (now - sc["ts"]) <= _PHONE_STALE_SECS:
        return {"level": sc["level"], "charging": sc["charging"], "present": True, "source": "shortcut"}
    if hs["level"] is not None and (now - hs["ts"]) <= 120:
        return {"level": hs["level"], "charging": False, "present": True, "source": "hotspot"}
    return {"level": None, "charging": False, "present": False, "source": "none"}


def battery_state():
    return {"mac": mac_battery(), "phone": phone_battery(), "airpods": airpods_battery()}


class Handler(BaseHTTPRequestHandler):
    def _send(self, code, body=b""):
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        if body:
            self.wfile.write(body)

    def do_GET(self):
        parsed = urllib.parse.urlparse(self.path)
        p = parsed.path.rstrip("/")
        if p in ("", "/status"):
            self._send(200, json.dumps(read_state()).encode())
        elif p == "/events":
            self._stream_events()
        elif p == "/battery":
            self._send(200, json.dumps(battery_state()).encode())
        elif p == "/battery/phone":
            # iPhone Shortcut push, single URL field: /battery/phone?level=80&charging=1
            self._update_phone(urllib.parse.parse_qs(parsed.query))
        else:
            self._send(404, b'{"error":"not found"}')

    def _update_phone(self, qs):
        def first(v):
            return v[0] if isinstance(v, list) else v
        try:
            raw = float(first(qs.get("level")))
            if 0.0 < raw <= 1.0:               # accept a 0.0-1.0 fraction too
                raw *= 100.0
            level = max(0, min(100, int(round(raw))))
        except Exception:
            self._send(400, b'{"error":"bad level"}')
            return
        # charging is OPTIONAL: a level-only push (the periodic automation) must not clear the bolt;
        # only the charger-connected/disconnected automations send it and flip it.
        has_charging = "charging" in qs
        charging = str(first(qs.get("charging"))).strip().lower() in ("1", "true", "yes", "on")
        with _batt_lock:
            _phone["level"] = level
            if has_charging:
                _phone["charging"] = charging
            _phone["ts"] = time.time()
            cur = _phone["charging"]
        self._send(200, json.dumps({"ok": True, "level": level, "charging": cur}).encode())

    def _stream_events(self):
        """SSE: push the state to this client on every change (and a heartbeat to keep alive).

        Covers every scenario by construction — whatever the hooks write to state.json (new prompt
        -> thinking, tool, needs-input -> waiting/permission, finished -> done) is detected here and
        pushed; the staleness guard in read_state() pushes an idle when a stopped run goes quiet.
        """
        self.send_response(200)
        self.send_header("Content-Type", "text/event-stream")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Connection", "keep-alive")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        # Disable Nagle so each small push goes out immediately (no ~200ms coalescing).
        try:
            self.connection.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        except Exception:
            pass
        last_key = None
        last_ping = time.time()
        try:
            while True:
                s = read_state()
                key = json.dumps(s, sort_keys=True)
                if key != last_key:
                    self.wfile.write(f"data: {json.dumps(s)}\n\n".encode())
                    self.wfile.flush()
                    last_key = key
                    last_ping = time.time()
                elif time.time() - last_ping >= 2:
                    # Frequent heartbeat keeps the TV's Wi-Fi radio awake so pushes aren't batched,
                    # and lets the client detect a dropped link within ~2s.
                    self.wfile.write(b": ping\n\n")
                    self.wfile.flush()
                    last_ping = time.time()
                time.sleep(0.2)
        except (BrokenPipeError, ConnectionResetError, OSError):
            return  # client disconnected

    def do_OPTIONS(self):
        self._send(204)

    def handle(self):
        # Clients (the TV) drop the connection all the time (reload, reconnect, sleep). Swallow the
        # resulting socket errors so they don't spam the log with tracebacks.
        try:
            super().handle()
        except (BrokenPipeError, ConnectionResetError, OSError):
            pass

    def log_message(self, *args):
        pass  # quiet


if __name__ == "__main__":
    threading.Thread(target=_battery_poller, daemon=True).start()
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
