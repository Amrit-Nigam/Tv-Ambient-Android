# CONFIG.md — Every tunable and its default

All runtime tunables live in one place: `AppConfig`
(`app/src/main/java/com/tvport/dashboard/data/config/AppConfig.kt`), backed by DataStore via
`ConfigRepository`. A fresh install runs entirely on the defaults below. Secrets are **not**
here — they come from the gitignored `secrets.properties` (see `README.md`).

## Location & formatting
| Field | Default | Meaning |
|---|---|---|
| `latitude` | `19.0473` | Weather/location latitude (Kharghar, Navi Mumbai) |
| `longitude` | `73.0699` | Weather/location longitude |
| `locationLabel` | `"Kharghar"` | Label shown on the weather tile |
| `metric` | `true` | `true` = °C / mm, `false` = °F / inch |
| `use24Hour` | `true` | Clock format: 24h vs 12h (AM/PM) |

## Day / night & burn-in (BUILD SPEC §10)
| Field | Default | Meaning |
|---|---|---|
| `nightStartHour` | `22` | Hour (0–23, local) night dimming begins |
| `nightEndHour` | `7` | Hour night dimming ends (window may wrap midnight) |
| `nightDimLevel` | `0.45` | Black-overlay alpha at night (0 = none, 1 = black) |
| `dayDimLevel` | `0.0` | Daytime overlay alpha |
| `pixelShiftEnabled` | `true` | Anti burn-in pixel-shift on/off |
| `pixelShiftPeriodSec` | `60` | Seconds between pixel-shift nudges |
| `pixelShiftMaxPx` | `12` | Max shift magnitude in dp |

## FIFA / football (BUILD SPEC §9)
| Field | Default | Meaning |
|---|---|---|
| `footballCompetition` | `"WC"` | football-data.org competition code (WC, CL, PL, …) |
| `footballTeamId` | `0` | If non-zero, follow this team id instead of a competition |
| `fifaFallbackHome` | `"Argentina"` | Labeled static fallback home team (no token / no fixture) |
| `fifaFallbackAway` | `"France"` | Labeled static fallback away team |
| `fifaFallbackKickoffIso` | `"2026-07-19T18:00:00Z"` | Labeled static fallback kickoff (UTC ISO-8601) |

## Refresh / poll intervals
| Field | Default | Meaning |
|---|---|---|
| `spotifyPollPlayingSec` | `5` | Now-Playing poll cadence while audio is playing |
| `spotifyPollIdleSec` | `30` | Now-Playing poll cadence while idle (rate-limit friendly) |
| `weatherRefreshMin` | `20` | Weather refresh interval (WorkManager) |
| `calendarRefreshMin` | `20` | Calendar refresh interval |
| `fifaRefreshMin` | `30` | Next-match refresh interval |

## Changing values
- **Edit defaults:** change `AppConfig.kt` and rebuild.
- **At runtime:** `ConfigRepository` exposes `setLocation(...)` and `setNightSchedule(...)`
  which persist to DataStore and take effect live (the dashboard observes the config flow).
