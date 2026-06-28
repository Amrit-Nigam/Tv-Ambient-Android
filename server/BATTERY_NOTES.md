# Battery research notes (parked for later)

A log of **every method tried** to get device battery onto the dashboard, what worked, and the
walls we hit — so future-us doesn't re-walk the same paths. The working code for the good ones
lives in this folder's `serve.py`. The live companion server has been reverted to Claude-status-only;
all of this is branch material.

Legend: ✅ works · ⚠️ works with caveats · ❌ dead end

---

## Mac — ✅ solved
`pmset -g batt` → percentage + charging (`AC Power` / `charging` / `charged` vs `discharging`).
Cheap, instant, reliable. Done.

## AirPods + case — ⚠️ levels only
`system_profiler SPBluetoothDataType -json` → `device_batteryLevelLeft / Right / Case`. This is the
exact source the macOS Batteries widget uses. Earbuds % = min(L, R).

- ❌ **Charging state is not available.** macOS exposes no AirPods charge flag to any CLI. Confirmed
  by reading AirBattery's source — it literally hardcodes `isCharging: 0` for AirPods from this data.
- The `IsCharging = Yes` seen in `ioreg` is the **Mac's own** internal battery, not the AirPods.
- AirBattery gets the bolt by parsing the **unified log** (`log stream`, category
  `CBStackDeviceMonitor` / `Server.GATT`) for `Battery M +NN%` lines where the `+`/`-` sign is the
  charge state. Needs a persistent debug-level `log stream` + the device connected; the clean lines
  are sparse and it's CPU-hungry. Decided not worth it for a wall display.

## iPhone — the hard one

### 1. Apple Shortcut push — ✅ best wireless option
Shortcut: **Get Battery Level → Get Contents of URL** `…/battery/phone?level=<BatteryLevel>&charging=1`.
Server endpoint `GET /battery/phone?level=NN&charging=1` caches it.

- Works **anywhere on Wi-Fi**, carries **charging**, needs **no special macOS permission**.
- Cost: iPhone-side automation setup (charger connected/disconnected for the bolt; app-open / time
  triggers to refresh level — iOS has no true "every N min" trigger).
- Signed `.shortcut` files can be generated on macOS: build the WFWorkflow plist, then
  `shortcuts sign --mode anyone --input X.shortcut --output Y.shortcut` (**input must have a
  `.shortcut` extension or signing errors**). Keep the workflow simple — one variable in the URL
  field; the JSON-dictionary body format is fragile and Shortcuts silently rejects a malformed one
  (opens then closes).

### 2. Continuity / Instant Hotspot — ⚠️ passive level, no charging
Parse the unified log for `SFRemoteHotspotDevice … battery life: N` — the same value the Wi-Fi menu
shows next to the iPhone.

- ✅ Fully automatic, no iPhone setup, refreshes ~every minute.
- ⚠️ Only while the phone is **near the Mac** (Bluetooth/Continuity range) **and** broadcasting
  Instant Hotspot. Drops out otherwise. **No charging.**
- `log show --last 6m --predicate 'eventMessage CONTAINS "SFRemoteHotspotDevice"'` → grab the last
  `battery life: N`.

### 3. libimobiledevice over USB — ✅ but only while cabled
`brew install libimobiledevice`; one-time `idevicepair pair` after a USB "Trust". Then
`ideviceinfo -q com.apple.mobile.battery` → `BatteryCurrentCapacity` + `BatteryIsCharging`.

- ✅ Authoritative **level + charging**.
- ❌ Only while the phone is physically plugged into the Mac.

### 4. libimobiledevice over Wi-Fi — ❌ blocked by macOS
`ideviceinfo -n` (network mode). The goal: USB-pair once, then read wirelessly forever.

- Worked **exactly once** right after unplugging (warm session), then `idevice_id -n` went empty and
  stayed empty.
- Enabled Finder → **"Show this iPhone when on Wi-Fi"** (verified checked). The phone **does**
  advertise the beacon — `dns-sd -B _apple-mobdev2._tcp` shows it.
- But `idevice_id -n` / `ideviceinfo -n -u <UDID>` still return **"No device found"**.
- **Root cause: macOS Local Network privacy permission.** mDNS/Bonjour discovery is gated; a
  background tool (LaunchAgent / Terminal) isn't granted Local Network access, so its browse returns
  nothing — even though the system `dns-sd` (pre-authorized) sees the beacon fine.
- Tried AirBattery's **own bundled, patched** `idevice_id` / `ideviceinfo` + its custom
  `libusbmuxd` dylib (extracted from the 1.6.3 `.dmg`): **also empty from the CLI.** So it's the
  process-context permission, not the binary.

### AirBattery, how it actually does it
- iPhone: bundles libimobiledevice **+ a custom `wificonnection` helper**. Read its `wificonnection.c`
  — it only does `lockdownd_set_value("com.apple.mobile.wireless_lockdown", "EnableWifiConnections",
  true)`, i.e. the Finder checkbox programmatically. The real reason it works wirelessly is that
  **AirBattery.app holds the Local Network entitlement** and is approved in System Settings; a GUI
  app gets discovery, our background server can't.
- So replicating wireless libimobiledevice cleanly from a daemon is effectively impossible without
  shipping a signed, entitled GUI app.

---

## Where it landed (in this branch's `serve.py`)
`/battery` returns `mac`, `airpods`, and `phone`. The iPhone uses a layered, best-available source:

1. **USB** (libimobiledevice) — level + charging, while cabled
2. **Shortcut push** — level + charging, anywhere on Wi-Fi  ← most reliable wireless
3. **Continuity hotspot** — level only, passive, near the Mac

A background thread polls the local sources every 30s so `/battery` never blocks.

## Open question for next time
Wireless **charging bolt without cabling the phone**. Realistic options, all unsatisfying:
- Run **AirBattery.app** itself (has the entitlement) and find a way to read its data.
- Ship our own tiny signed GUI helper with the Local Network entitlement that does the mDNS +
  libimobiledevice read and writes to a file `serve.py` can read.
- Accept the Shortcut as the wireless charging source (already works) and treat USB/hotspot as the
  automatic bonuses.
