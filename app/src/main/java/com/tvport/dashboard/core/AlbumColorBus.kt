package com.tvport.dashboard.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One accent scheme extracted from the current album art. ARGB ints (not Compose Color, to keep
 * core free of UI deps). [accent] is the lively hero hue; [accent2] a secondary; [glow] the ring
 * around the vinyl label.
 */
data class AlbumScheme(
    val accent: Int,
    val accent2: Int,
    val glow: Int,
)

/**
 * App-wide bus carrying the album-art accent scheme. The Now Playing layer extracts colors from
 * the current art and publishes here; the whole dashboard (background gradient, accents, visualizer,
 * every tile) observes it so the page re-themes to the album cover. Null = no art yet (use defaults).
 */
@Singleton
class AlbumColorBus @Inject constructor() {
    private val _scheme = MutableStateFlow<AlbumScheme?>(null)
    val scheme: StateFlow<AlbumScheme?> = _scheme.asStateFlow()

    /** Current album-art URL, used to paint a soft blurred page background. */
    private val _artUrl = MutableStateFlow<String?>(null)
    val artUrl: StateFlow<String?> = _artUrl.asStateFlow()

    fun publish(scheme: AlbumScheme?) {
        _scheme.value = scheme
    }

    fun publishArt(url: String?) {
        _artUrl.value = url
    }
}
