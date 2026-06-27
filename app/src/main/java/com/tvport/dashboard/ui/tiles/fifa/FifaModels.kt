package com.tvport.dashboard.ui.tiles.fifa

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/* ------------------------------------------------------------------------------------------------
 * football-data.org v4 wire models.
 *   - /v4/competitions/{code}/matches?status=SCHEDULED
 *   - /v4/teams/{id}/matches?status=SCHEDULED
 * Only the fields we consume are declared; the shared Json is lenient + ignores unknown keys, so
 * the many other fields the API returns are simply dropped.
 * ---------------------------------------------------------------------------------------------- */

@Serializable
data class MatchesResponse(
    val matches: List<MatchDto> = emptyList(),
)

@Serializable
data class MatchDto(
    /** ISO-8601 UTC, e.g. "2026-07-19T18:00:00Z". */
    val utcDate: String? = null,
    val status: String? = null,
    val homeTeam: TeamDto? = null,
    val awayTeam: TeamDto? = null,
    val competition: CompetitionDto? = null,
)

@Serializable
data class TeamDto(
    val name: String? = null,
    @SerialName("shortName") val shortName: String? = null,
)

@Serializable
data class CompetitionDto(
    val name: String? = null,
)

/* ------------------------------------------------------------------------------------------------
 * UI model the tile renders. Everything that can be pre-computed by the VM/repository is, so the
 * composable only owns the per-second countdown derived from [kickoffMillis].
 * ---------------------------------------------------------------------------------------------- */

data class FifaUi(
    val homeTeam: String,         // "Argentina"
    val awayTeam: String,         // "France"
    val competition: String?,     // "FIFA World Cup" (nullable)
    val kickoffMillis: Long,      // UTC epoch millis of kick-off
    val kickoffLocalLabel: String,// device-local, e.g. "Sat 19 Jul, 23:30"
    /**
     * TRUE when this is the LABELED STATIC FALLBACK (no token / API error / no fixture), NOT a
     * real fixture from the football API. The UI surfaces this honestly with a tiny "sample" chip.
     */
    val isFallback: Boolean,
)
