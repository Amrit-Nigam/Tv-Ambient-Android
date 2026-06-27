package com.tvport.dashboard.media

import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import com.tvport.dashboard.ui.tiles.nowplaying.NowPlayingUi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SECONDARY, best-effort Now Playing source for locally-played audio.
 *
 * The Spotify Web API is the PRIMARY source for this tile. This reads the system's active
 * [android.media.session.MediaController] via [MediaSessionManager] to surface whatever is
 * playing on the device itself (a local player, casting, etc.).
 *
 * IMPORTANT: [MediaSessionManager.getActiveSessions] requires either the MEDIA_CONTENT_CONTROL
 * signature permission OR a registered+enabled NotificationListenerService component. On a
 * normal TV build neither is granted, so this call throws SecurityException — which is why
 * EVERYTHING here is wrapped in try/catch and returns null. This source is expected to be
 * inactive in most deployments and must NEVER crash the tile.
 */
@Singleton
class MediaSessionSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Returns the currently-playing local media as a [NowPlayingUi], or null when no session
     * is available / accessible. Fully guarded — any failure (SecurityException, missing
     * service, null metadata) collapses to null.
     */
    fun current(): NowPlayingUi? {
        return try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
                as? MediaSessionManager ?: return null

            // null componentName => requires MEDIA_CONTENT_CONTROL; throws SecurityException
            // on a normal app, caught below.
            val controllers = manager.getActiveSessions(null)
            if (controllers.isEmpty()) return null

            val controller = controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: controllers.first()

            val metadata = controller.metadata ?: return null
            val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                ?: return null

            val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
                ?: ""

            val durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
                .coerceAtLeast(0L)
            val artUrl = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_ART_URI)
                ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI)

            val playbackState = controller.playbackState
            val progressMs = playbackState?.position?.coerceAtLeast(0L) ?: 0L
            val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

            NowPlayingUi(
                trackName = title,
                artists = artist,
                albumArtUrl = artUrl,
                progressMs = progressMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                source = "Device",
            )
        } catch (t: Throwable) {
            // Expected on most TVs (no MEDIA_CONTENT_CONTROL / notification listener).
            Log.d(TAG, "MediaSession unavailable: ${t.message}")
            null
        }
    }

    private companion object {
        const val TAG = "NowPlaying"
    }
}
