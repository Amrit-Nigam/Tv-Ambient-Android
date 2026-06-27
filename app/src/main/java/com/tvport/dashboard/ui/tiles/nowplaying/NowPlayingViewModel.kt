package com.tvport.dashboard.ui.tiles.nowplaying

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.core.tickerFlow
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Now Playing tile.
 *
 *  - A poll loop hits [NowPlayingRepository.fetch] on the config cadence: fast
 *    ([AppConfig.spotifyPollPlayingSec]) while a track is playing, slow
 *    ([AppConfig.spotifyPollIdleSec]) when idle/unavailable. Each poll re-syncs progress.
 *  - A 1s ticker advances the progress bar locally between polls so it moves smoothly.
 *
 * Never throws: the repository only ever returns one of the four [TileState]s.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val repository: NowPlayingRepository,
    private val config: ConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TileState<NowPlayingUi>>(TileState.Loading)
    val state: StateFlow<TileState<NowPlayingUi>> = _state.asStateFlow()

    /** Last real track, echoed into Fallback states to keep the tile alive. */
    private var lastKnown: NowPlayingUi? = null

    init {
        startPolling()
        startLocalTicker()
    }

    private fun startPolling() = viewModelScope.launch {
        while (isActive) {
            val cfg = config.config.first()
            val result = repository.fetch(lastKnown)
            if (result is TileState.Content) {
                lastKnown = result.data
            }
            _state.value = result

            val playing = (result as? TileState.Content)?.data?.isPlaying == true
            val periodSec = if (playing) cfg.spotifyPollPlayingSec else cfg.spotifyPollIdleSec
            delay(periodSec.coerceAtLeast(1) * 1000L)
        }
    }

    private fun startLocalTicker() = viewModelScope.launch {
        tickerFlow(1000L).collect {
            _state.update { current ->
                if (current is TileState.Content) {
                    val ui = current.data
                    if (ui.isPlaying && ui.durationMs > 0L && ui.progressMs < ui.durationMs) {
                        TileState.Content(
                            ui.copy(progressMs = (ui.progressMs + 1000L).coerceAtMost(ui.durationMs))
                        )
                    } else {
                        current
                    }
                } else {
                    current
                }
            }
        }
    }
}
