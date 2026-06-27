package com.tvport.dashboard.ui.tiles.fifa

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the NEXT MATCH tile state. A coroutine loop re-fetches the next fixture every
 * [AppConfig.fifaRefreshMin] minutes (immediately on start). The per-second COUNTDOWN is NOT driven
 * here — the composable ticks it from the kickoff millis carried in [FifaUi] — so this VM only does
 * the (relatively expensive) network refresh on the slow cadence.
 *
 * The repository never throws and always returns a [FifaUi] (real or labeled fallback). We surface
 * a real fixture as [TileState.Content] and a labeled fallback as [TileState.Fallback] carrying the
 * sample payload, so the tile is never blank and the fallback stays honestly labeled.
 */
@HiltViewModel
class FifaViewModel @Inject constructor(
    private val config: ConfigRepository,
    private val repository: FifaRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TileState<FifaUi>>(TileState.Loading)

    val state: StateFlow<TileState<FifaUi>> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TileState.Loading,
    )

    init {
        viewModelScope.launch {
            while (true) {
                val cfg = config.config.first()
                refresh()
                val intervalMs = cfg.fifaRefreshMin.coerceAtLeast(1) * 60_000L
                delay(intervalMs)
            }
        }
    }

    private suspend fun refresh() {
        val cfg = config.config.first()
        // Only show Loading on the very first fetch; afterwards keep whatever is on screen.
        if (_state.value is TileState.Loading) {
            // leave as Loading until the first result arrives
        }
        val ui = repository.loadNextMatch(cfg)
        _state.value = if (ui.isFallback) {
            Log.d(TAG, "Showing labeled fallback fixture: ${ui.homeTeam} v ${ui.awayTeam}")
            TileState.Fallback(reason = "No token / no fixture — sample match", data = ui)
        } else {
            TileState.Content(ui)
        }
    }

    private companion object {
        const val TAG = "Fifa"
    }
}
