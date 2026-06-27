# TvPort Dashboard — Android TV Ambient Display

An always-on **Android TV / Google TV** dashboard. One glanceable screen with:

- 🎵 **Now Playing** — your Spotify track on a spinning **vinyl record**; the whole page re-themes
  to the album cover's color, with the cover blurred behind everything as a faded aura
- 🕐 **Clock + date** — big, legible (12-hour)
- ⚽ **Next Match** — the next football fixture with a live countdown (football-data.org)
- 🏎️ **Next F1 Race** — the next Grand Prix with a live countdown (Jolpica / Ergast)
- 🤖 **Claude status bar** (optional) — a live pixel creature showing what Claude Code is doing
  in your terminal (working / waiting for you / done / idle), pushed in real time from your Mac

Built with Kotlin + Jetpack Compose for TV. Fonts: **Geist** (Vercel).

---

## ⚡ TL;DR (if you already have the tools)

```bash
cp secrets.properties.example secrets.properties   # then fill it in (see Step 2)
./gradlew :app:assembleDebug                        # build the APK
adb connect <your-tv-ip>:5555                        # accept the prompt on the TV
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tvport.dashboard/.MainActivity
```

New to this? Follow the full steps below. ⬇️

---

## What you'll need

- A **Mac or PC** to build the app (one-time)
- An **Android TV / Google TV** (e.g. Sony BRAVIA, Chromecast with Google TV, TCL/Hisense)
- Both on the **same Wi-Fi**
- A **Spotify** account, and optionally a free **football-data.org** account
- ~30 minutes the first time

---

## Step 1 — Get the project and build tools

You need a Java JDK 17 and the Android SDK. Two options:

**Easiest — Android Studio**
1. Install **Android Studio**: <https://developer.android.com/studio>
2. **Open** this `tvport` folder in it — the SDK downloads automatically.

**Or — command line (Mac + Homebrew):**
```bash
brew install openjdk@17
brew install --cask android-commandlinetools
echo "sdk.dir=/opt/homebrew/share/android-commandlinetools" > local.properties
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

---

## Step 2 — Add your secrets

Secrets stay out of the code in a file the app reads at build time. **It's never committed** (git-ignored).

```bash
cp secrets.properties.example secrets.properties
```
Open `secrets.properties` and fill it in:

### 2a. Spotify (required for Now Playing)
1. <https://developer.spotify.com/dashboard> → **Create app**. Note **Client ID** + **Client Secret**.
2. In the app settings add Redirect URI `http://127.0.0.1:8888/callback` → Save.
3. Paste this in a browser (replace `CLIENT_ID`), Enter, approve:
   ```
   https://accounts.spotify.com/authorize?client_id=CLIENT_ID&response_type=code&redirect_uri=http://127.0.0.1:8888/callback&scope=user-read-currently-playing%20user-read-playback-state
   ```
4. The page won't load — that's fine. Copy the `code=...` value from the URL bar.
5. Trade it for a refresh token (replace the 3 values):
   ```bash
   curl -X POST https://accounts.spotify.com/api/token \
     -d grant_type=authorization_code -d code=PASTE_CODE \
     -d redirect_uri=http://127.0.0.1:8888/callback \
     -u CLIENT_ID:CLIENT_SECRET
   ```
6. Put all three into `secrets.properties`:
   ```
   SPOTIFY_CLIENT_ID=...
   SPOTIFY_CLIENT_SECRET=...
   SPOTIFY_REFRESH_TOKEN=...
   ```
> Now Playing mirrors whatever's playing on your Spotify account (any device). Nothing playing → a
> tidy idle state.

### 2b. Football (optional — Next Match tile)
1. Register free at <https://www.football-data.org/client/register> → they email a token.
2. `FOOTBALL_DATA_TOKEN=your_token`
3. Choose the competition in `CONFIG.md` (`footballCompetition`, default `WC`; or `PL`, `CL`, `PD`…).
> No token → the tile shows a labeled "SAMPLE" match (no crash).

### 2c. Location
Default is **Kharghar, Navi Mumbai**. Change `latitude`/`longitude` in `CONFIG.md` if you want.

### 2d. Claude bar URL (optional)
Leave `CLAUDE_STATUS_URL` blank unless you're doing Step 6.

---

## Step 3 — Build the APK

**Android Studio:** **Build → Build APK(s)**.

**Command line:**
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17    # adjust to your JDK
./gradlew :app:assembleDebug
```
Result → `app/build/outputs/apk/debug/app-debug.apk`

---

## Step 4 — Install on your Google TV

1. **Enable debugging on the TV:**
   - **Settings → System → About →** click **"Android TV OS build" 7 times**.
   - **Settings → System → Developer options →** turn on **Network debugging**.
   - Note the TV's IP: **Settings → Network & Internet → (your Wi-Fi) → IP address** (e.g. `192.168.1.42`).

2. **From your computer** (same Wi-Fi):
   ```bash
   adb connect 192.168.1.42:5555      # ← your TV's IP. Choose "Allow" on the popup that appears ON THE TV
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.tvport.dashboard/.MainActivity
   ```
   > Says `unauthorized`? Accept the **"Allow USB debugging?"** popup on the TV (tick "Always allow"),
   > then run `adb connect` again.

🎉 The dashboard launches.

---

## Step 5 — Add to Home & make it always-on

- **Add to Home:** **Home → Apps → Your apps →** find **TvPort Dashboard** → **press & hold** Select →
  **Add to favorites** → "Move" it to the front. *(Not listed? Open it once via Apps → See all apps.)*
- **Stop the screensaver:** **Settings → System → Ambient mode → "When to start screensaver" → Never**.
- **After a TV reboot:** Google TV won't auto-launch apps — open it from favorites once.

---

## Step 6 — (Optional) Live Claude status bar

Shows what Claude Code is doing in your terminal, on the TV, in real time. Needs a tiny helper on the
**Mac** you run Claude on, which reads `~/.claude/statusbar/state.json` (the file your Claude statusline
writes — fields `state`, `label`, `project`, `startedAt`).

1. **Status server** — `~/.claude/statusbar/serve.py` reads that file and streams changes over your
   LAN (Server-Sent Events). It's installed as a LaunchAgent that auto-starts on login. Verify:
   ```bash
   curl http://127.0.0.1:4040/status
   ```
2. **Point the app at your Mac** — find your Mac's LAN IP (`System Settings → Wi-Fi → Details → IP`),
   set it in `secrets.properties`, then rebuild + reinstall (Steps 3–4):
   ```
   CLAUDE_STATUS_URL=http://192.168.1.40:4040/status
   ```
3. **Reliability:** reserve a **static IP** for your Mac in your router so the address never changes,
   and keep the Mac **awake** on the same Wi-Fi. Otherwise the bar just shows "offline".

The creature: **alive** while working · **surprised + "!"** when it needs you · **winks** when done ·
**sleeps** when idle/stopped. Blank `CLAUDE_STATUS_URL` simply hides it — everything else still works.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| `adb: command not found` | Use the full path (`.../platform-tools/adb`) or add it to PATH |
| `adb connect` → `unauthorized` | Accept the "Allow USB debugging" popup on the TV, then reconnect |
| Installs to the wrong device | An emulator is also connected — use `adb -s <tv-ip>:5555 install …` or close the emulator |
| Now Playing stuck on "Nothing playing" | Play a song on Spotify; re-check the refresh token |
| Next Match shows "SAMPLE" | Add a `FOOTBALL_DATA_TOKEN` (Step 2b) |
| Claude bar says "offline" | Mac asleep / off-Wi-Fi / IP changed — see Step 6.3 |
| Build fails first run | Let Gradle finish downloading; ensure JDK 17 + Android SDK are installed |

---

## Files

- **`CONFIG.md`** — every tunable (location, units, day/night dim, competition, poll intervals) + defaults
- **`secrets.properties.example`** — template (copy to `secrets.properties`)
- **`app/`** — the Android app (Kotlin + Compose for TV)

Enjoy your wall display. 🎶
