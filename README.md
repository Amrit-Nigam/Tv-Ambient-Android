# TvPort Dashboard — Android TV Ambient Display

A single-purpose, always-on **Android TV / Google TV** dashboard. One glanceable screen showing:
**Now Playing (Spotify)** · **ambient audio visualizer** · **large clock + date** · **weather (Open-Meteo)** ·
**Google Calendar** · **next FIFA/football match** · **auto-dim + anti burn-in** for 24/7 use.

Built with Kotlin, Jetpack Compose (+ Compose for TV), Hilt, Retrofit/OkHttp + kotlinx.serialization,
Coroutines/Flow, Coil, DataStore. MVVM — each tile owns a `ViewModel` exposing a `StateFlow<TileState>`
so **every tile fails independently** and never blanks or crashes the screen.

---

## 1. Prerequisites
- **JDK 17** (e.g. `brew install openjdk@17`)
- **Android SDK** with `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`. Min SDK 21, target 34.
- For the emulator path: `system-images;android-34;android-tv;arm64-v8a` + `emulator`.

`local.properties` must point at your SDK:
```
sdk.dir=/path/to/Android/sdk
```

## 2. Secrets — where each value goes
**Nothing is hardcoded in source.** Secrets live in a **gitignored** `secrets.properties` at the repo root
(copy from `secrets.properties.example`). They are injected into `BuildConfig` at build time by `app/build.gradle.kts`.

| Key | Required? | Notes |
|---|---|---|
| `SPOTIFY_CLIENT_ID` | ✅ | Spotify developer app client id |
| `SPOTIFY_CLIENT_SECRET` | ✅ | Spotify developer app client secret |
| `SPOTIFY_REFRESH_TOKEN` | ✅ | One-time OAuth refresh token (see §3) |
| `FOOTBALL_DATA_TOKEN` | ⬜ optional | football-data.org free token. Blank → FIFA tile shows a **labeled static fallback** |
| `CALENDAR_ICAL_URL` | ⬜ optional | Google Calendar "Secret address in iCal format". Blank → Calendar shows "No calendar connected" |

> Weather (Open-Meteo) needs **no key**. Location defaults to **Kharghar, Navi Mumbai** (19.0473, 73.0699) — see `CONFIG.md`.

After editing `secrets.properties`, rebuild so the new values land in `BuildConfig`.

## 3. Generating the Spotify refresh token
The app uses the **Authorization Code flow** and refreshes the access token automatically. You only need to
produce a refresh token **once**:

1. Create an app at <https://developer.spotify.com/dashboard>. Note the **Client ID** and **Client Secret**.
2. Add a **Redirect URI** (e.g. `http://127.0.0.1:8888/callback`) in the app settings.
3. In a browser, authorize with these scopes (URL-encode the space as `%20`):
   ```
   https://accounts.spotify.com/authorize?client_id=CLIENT_ID&response_type=code&redirect_uri=http://127.0.0.1:8888/callback&scope=user-read-currently-playing%20user-read-playback-state
   ```
   Approve, then copy the `code=...` value from the redirected URL.
4. Exchange the code for tokens:
   ```bash
   curl -X POST https://accounts.spotify.com/api/token \
     -d grant_type=authorization_code \
     -d code=PASTE_CODE \
     -d redirect_uri=http://127.0.0.1:8888/callback \
     -u CLIENT_ID:CLIENT_SECRET
   ```
   Copy `refresh_token` from the JSON into `SPOTIFY_REFRESH_TOKEN`.

> Scopes that matter: `user-read-currently-playing` (primary) and optionally `user-read-playback-state`.
> The "currently playing" tile only needs `user-read-currently-playing`.
> **Now Playing reflects your account's active playback** — start a track on any Spotify device and it appears;
> when nothing plays it shows a tasteful idle state. A best-effort `MediaSessionManager` source is also wired as a
> secondary for locally-played audio (often inactive on a stock TV — Spotify Web API is primary).

## 4. Getting the Google Calendar iCal URL (recommended, no OAuth)
1. <https://calendar.google.com> → hover your calendar → **⋮ → Settings and sharing**.
2. Scroll to **Integrate calendar** → copy **Secret address in iCal format** (ends in `/basic.ics`).
3. Put it in `CALENDAR_ICAL_URL` and rebuild. The tile fetches/parses it and shows your next 2–3 events.

(We chose iCal over OAuth — far less setup for a single-user wall display. OAuth would require its own
refresh-token exchange.)

## 5. football-data.org token (optional)
Register free at <https://www.football-data.org/client/register>, put the token in `FOOTBALL_DATA_TOKEN`.
Pick the competition/team in `CONFIG.md` (`footballCompetition`, default `WC`; or set `footballTeamId`).
No token → the tile renders a clearly-labeled **SAMPLE** fixture (no crash).

## 6. Build
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || echo /opt/homebrew/opt/openjdk@17)
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 7. Install on an Android TV via ADB
1. On the TV: **Settings → Device Preferences → About →** click **Build** 7× to enable Developer options,
   then enable **Network debugging / USB debugging**. Note the TV's IP (Settings → Network).
2. From your computer:
   ```bash
   adb connect <tv-ip>:5555
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.tvport.dashboard/.MainActivity
   ```
The app appears in the Android TV **Apps** row (leanback launcher banner included).

## 8. Run on boot
A `BootReceiver` (with `RECEIVE_BOOT_COMPLETED`) relaunches the dashboard after the TV restarts.
**Caveat:** some Google TV builds / Android 10+ background-activity-start limits can block launching an Activity
straight from boot. If your TV blocks it, set TvPort as the default start-on-boot app in the TV's settings, or use
a launcher shortcut. The activity holds `FLAG_KEEP_SCREEN_ON` so the panel never sleeps (night dimming still applies).

## 9. Emulator (optional)
```bash
sdkmanager "system-images;android-34;android-tv;arm64-v8a"
avdmanager create avd -n tvport_tv -k "system-images;android-34;android-tv;arm64-v8a" -d tv_1080p
emulator -avd tvport_tv
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 10. Configuration
Every tunable (location, units, 12/24h, day/night schedule + dim level, pixel-shift, competition, poll
intervals) is documented in **`CONFIG.md`**, backed by `AppConfig` + DataStore.

## 11. Architecture
```
ui/dashboard      DashboardScreen (layout), DashboardViewModel (shared config), TileCard helpers
ui/tiles/*        clock, nowplaying, weather, calendar, fifa — each: Models, Api/Repo, ViewModel, Tile
ui/visualizer     Visualizer-API reactive background + always-on procedural fallback
ui/dim            night dimming + anti burn-in pixel-shift surface
ui/theme          palette, fonts (Space Grotesk / Inter), day/night DashColors
data/config       AppConfig + ConfigRepository (DataStore)
di                Hilt modules (network, app)
core              TileState contract, ticker flow
boot              BootReceiver
media             MediaSessionManager secondary Now-Playing source
```
Resilience: short network timeouts, lenient JSON, last-known-good caching per tile, and `TileState.Fallback/Idle`
states everywhere — one dead API never blanks the screen.
