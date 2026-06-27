package com.tvport.dashboard.ui.tiles.f1

import retrofit2.http.GET

/** Jolpica-F1 (Ergast mirror). Base URL https://api.jolpi.ca/ergast/f1/ */
interface F1Api {
    /** The single upcoming race of the current season. */
    @GET("current/next.json")
    suspend fun nextRace(): ErgastResponse
}
