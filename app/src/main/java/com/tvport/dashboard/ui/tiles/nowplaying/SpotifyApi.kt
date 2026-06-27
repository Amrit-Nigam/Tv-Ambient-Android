package com.tvport.dashboard.ui.tiles.nowplaying

import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interfaces for the two Spotify hosts.
 *
 *  - [SpotifyAccountsApi] -> https://accounts.spotify.com/  (refresh-token exchange)
 *  - [SpotifyWebApi]      -> https://api.spotify.com/        (currently-playing)
 *
 * Each is built with its own Retrofit instance (different base URLs) inside
 * [SpotifyAuth] / [NowPlayingRepository], reusing the shared OkHttp + JSON converter.
 */
interface SpotifyAccountsApi {

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshToken(
        @Header("Authorization") basicAuth: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
    ): SpotifyTokenDto
}

interface SpotifyWebApi {

    /**
     * 200 -> a [CurrentlyPlayingDto] body. 204 (nothing playing) -> Retrofit returns a
     * successful [Response] with a null body and never invokes the converter, so we read
     * the raw [Response] and branch on [Response.code]/[Response.body] ourselves.
     */
    @GET("v1/me/player/currently-playing")
    suspend fun currentlyPlaying(
        @Header("Authorization") bearer: String,
    ): Response<CurrentlyPlayingDto>
}
