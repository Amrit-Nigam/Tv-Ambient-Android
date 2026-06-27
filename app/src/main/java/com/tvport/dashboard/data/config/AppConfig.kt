package com.tvport.dashboard.data.config

/**
 * Single config surface for every tunable (BUILD SPEC §11). Sane defaults baked in;
 * persisted overrides come from [ConfigRepository]/DataStore. Secrets are NOT here —
 * they come from BuildConfig (gitignored secrets.properties).
 */
data class AppConfig(
    // Location (default: Kharghar, Navi Mumbai)
    val latitude: Double = 19.0473,
    val longitude: Double = 73.0699,
    val locationLabel: String = "Kharghar",

    // Units & formatting
    val metric: Boolean = true,
    val use24Hour: Boolean = false,

    // Day/night schedule (local wall-clock hours, 0-23). Night dims the UI.
    val nightStartHour: Int = 22,
    val nightEndHour: Int = 7,
    /** Overlay alpha applied at night, 0f (no dim) .. 1f (black). */
    val nightDimLevel: Float = 0.45f,
    /** Slight dim even in the day so a bright wall display isn't harsh. */
    val dayDimLevel: Float = 0.0f,

    // Anti burn-in
    val pixelShiftEnabled: Boolean = true,
    val pixelShiftPeriodSec: Int = 60,
    val pixelShiftMaxPx: Int = 12,

    // FIFA / football
    /** football-data.org competition code (e.g. "WC", "CL", "PL") or empty. */
    val footballCompetition: String = "WC",
    /** Optional team id to follow instead of a competition's next match. 0 = use competition. */
    val footballTeamId: Int = 0,
    /** Labeled static fallback shown when no token / no fixture. */
    val fifaFallbackHome: String = "Argentina",
    val fifaFallbackAway: String = "France",
    val fifaFallbackKickoffIso: String = "2026-07-19T18:00:00Z",

    // Poll / refresh intervals (seconds)
    val spotifyPollPlayingSec: Int = 5,
    val spotifyPollIdleSec: Int = 30,
    val weatherRefreshMin: Int = 20,
    val calendarRefreshMin: Int = 20,
    val fifaRefreshMin: Int = 30,
    val f1RefreshMin: Int = 60,
)
