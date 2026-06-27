package com.tvport.dashboard.ui.tiles.fifa

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * football-data.org v4. Base URL: https://api.football-data.org/
 * Auth is the per-request `X-Auth-Token` header (supplied from BuildConfig.FOOTBALL_DATA_TOKEN).
 * We only ever ask for SCHEDULED matches and pick the earliest upcoming one in the repository.
 */
interface FifaApi {

    @GET("v4/competitions/{code}/matches")
    suspend fun competitionMatches(
        @Header("X-Auth-Token") token: String,
        @Path("code") competitionCode: String,
        @Query("status") status: String = "SCHEDULED",
    ): MatchesResponse

    @GET("v4/teams/{id}/matches")
    suspend fun teamMatches(
        @Header("X-Auth-Token") token: String,
        @Path("id") teamId: Int,
        @Query("status") status: String = "SCHEDULED",
    ): MatchesResponse
}
