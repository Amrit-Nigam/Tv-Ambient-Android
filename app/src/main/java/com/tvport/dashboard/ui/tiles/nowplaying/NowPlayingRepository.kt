package com.tvport.dashboard.ui.tiles.nowplaying

import android.util.Log
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.media.MediaSessionSource
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the current "now playing" state from the real sources, never throwing to the VM.
 *
 * Priority:
 *  1. Spotify Web API (PRIMARY) — currently-playing for the authed account.
 *  2. MediaSession (SECONDARY, best-effort) — only when Spotify returns nothing.
 *
 * Failure handling:
 *  - token unavailable / HTTP error / network exception => [TileState.Fallback] carrying the
 *    last-known track (passed in) so the tile shows a faded last song rather than an error.
 *  - 204 / empty / non-track item with no local media => [TileState.Idle].
 */
@Singleton
class NowPlayingRepository @Inject constructor(
    private val auth: SpotifyAuth,
    private val mediaSource: MediaSessionSource,
    okHttpClient: OkHttpClient,
    converterFactory: Converter.Factory,
) {
    private val webApi: SpotifyWebApi = Retrofit.Builder()
        .baseUrl("https://api.spotify.com/")
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(SpotifyWebApi::class.java)

    /**
     * One fetch cycle. [lastKnown] is the most recent good track, echoed back inside a
     * [TileState.Fallback] when the primary source is unavailable.
     */
    suspend fun fetch(lastKnown: NowPlayingUi?): TileState<NowPlayingUi> {
        val token = auth.getAccessToken()
            ?: return secondaryOr(TileState.Fallback("Spotify auth unavailable", lastKnown))

        return try {
            val response = webApi.currentlyPlaying("Bearer $token")
            when {
                // 204 No Content (or any successful empty body) => nothing on Spotify.
                response.code() == 204 || response.body() == null && response.isSuccessful ->
                    secondaryOr(TileState.Idle("Nothing playing"))

                response.isSuccessful -> {
                    val body = response.body()
                    val ui = body?.toUi()
                    if (ui == null) {
                        // 200 but no real track (ad / podcast / unknown type).
                        secondaryOr(TileState.Idle("Nothing playing"))
                    } else {
                        TileState.Content(ui)
                    }
                }

                else -> {
                    Log.w(TAG, "currently-playing HTTP ${response.code()}")
                    TileState.Fallback("Spotify HTTP ${response.code()}", lastKnown)
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "currently-playing failed: ${t.message}")
            TileState.Fallback(t.message ?: "Network error", lastKnown)
        }
    }

    /**
     * Try the best-effort local MediaSession; if it has something, prefer it, otherwise return
     * the supplied [primaryResult] (Idle or Fallback) unchanged.
     */
    private fun secondaryOr(primaryResult: TileState<NowPlayingUi>): TileState<NowPlayingUi> {
        val local = mediaSource.current()
        return if (local != null) TileState.Content(local) else primaryResult
    }

    private fun CurrentlyPlayingDto.toUi(): NowPlayingUi? {
        val track = item ?: return null
        val name = track.name?.takeIf { it.isNotBlank() } ?: return null
        val artistNames = track.artists.mapNotNull { it.name }.filter { it.isNotBlank() }
        val art = track.album?.images?.firstOrNull { !it.url.isNullOrBlank() }?.url
        return NowPlayingUi(
            trackName = name,
            artists = artistNames.joinToString(", "),
            albumArtUrl = art,
            progressMs = (progress_ms ?: 0L).coerceAtLeast(0L),
            durationMs = track.duration_ms.coerceAtLeast(0L),
            isPlaying = is_playing,
            source = "Spotify",
        )
    }

    private companion object {
        const val TAG = "NowPlaying"
    }
}
