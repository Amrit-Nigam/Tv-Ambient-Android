package com.tvport.dashboard.ui.tiles.f1

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Jolpica-F1 (Ergast mirror) response shape for `/ergast/f1/current/next.json`.
 * Same source the portfolio's F1StandingsCard used — free, no API key.
 */
@Serializable
data class ErgastResponse(@SerialName("MRData") val mrData: MrData)

@Serializable
data class MrData(@SerialName("RaceTable") val raceTable: RaceTable)

@Serializable
data class RaceTable(
    val season: String? = null,
    @SerialName("Races") val races: List<RaceDto> = emptyList(),
)

@Serializable
data class RaceDto(
    val round: String? = null,
    val raceName: String,
    val date: String,                 // "2026-06-28"
    val time: String? = null,         // "13:00:00Z"
    @SerialName("Circuit") val circuit: CircuitDto,
)

@Serializable
data class CircuitDto(
    val circuitName: String,
    @SerialName("Location") val location: LocationDto = LocationDto(),
)

@Serializable
data class LocationDto(
    val locality: String? = null,
    val country: String? = null,
)

/** UI model for the Next F1 Race tile. */
data class F1Ui(
    val raceName: String,
    val circuitName: String,
    val location: String,      // "Spielberg, Austria"
    val countryFlagUrl: String?, // flagcdn PNG for the host country (null if unknown)
    val round: String?,        // "Round 8"
    val raceStartMillis: Long, // UTC epoch of lights-out
    val localLabel: String,    // "Sun 28 Jun, 18:30"
)
