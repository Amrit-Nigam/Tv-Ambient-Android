package com.tvport.dashboard.ui.tiles.nowplaying

import kotlinx.serialization.Serializable

/**
 * Wire models for the Spotify Web API + the UI model the tile renders.
 *
 * DTOs are deliberately lenient: every field is nullable / defaulted so a missing key
 * (ads, podcasts, partial payloads) never throws during deserialization. The shared
 * [com.tvport.dashboard.di.NetworkModule] Json already sets ignoreUnknownKeys.
 */

// ---- accounts.spotify.com : token exchange ----

@Serializable
data class SpotifyTokenDto(
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Int = 3600,
)

// ---- api.spotify.com : currently-playing ----

@Serializable
data class CurrentlyPlayingDto(
    val is_playing: Boolean = false,
    val progress_ms: Long? = null,
    val currently_playing_type: String? = null,
    val item: TrackDto? = null,
)

@Serializable
data class TrackDto(
    val name: String? = null,
    val duration_ms: Long = 0L,
    val artists: List<ArtistDto> = emptyList(),
    val album: AlbumDto? = null,
)

@Serializable
data class ArtistDto(
    val name: String? = null,
)

@Serializable
data class AlbumDto(
    val name: String? = null,
    val images: List<ImageDto> = emptyList(),
)

@Serializable
data class ImageDto(
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
)

// ---- UI model ----

/**
 * What the tile actually draws. Source-agnostic: filled either from the Spotify Web API
 * (primary) or the local MediaSession (secondary). [source] is the small label shown.
 */
data class NowPlayingUi(
    val trackName: String,
    val artists: String,
    val albumArtUrl: String?,
    val progressMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val source: String,
)
