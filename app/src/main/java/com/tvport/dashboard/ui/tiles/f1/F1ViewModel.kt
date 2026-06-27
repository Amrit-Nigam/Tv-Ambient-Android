package com.tvport.dashboard.ui.tiles.f1

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
 * Owns the NEXT F1 RACE tile state. Re-fetches the next race every [AppConfig.f1RefreshMin]
 * minutes (race schedules change rarely, so a slow cadence is plenty). The per-second countdown
 * is ticked by the composable from [F1Ui.raceStartMillis]. The repository never throws.
 */
@HiltViewModel
class F1ViewModel @Inject constructor(
    private val config: ConfigRepository,
    private val repository: F1Repository,
) : ViewModel() {

    private val _state = MutableStateFlow<TileState<F1Ui>>(TileState.Loading)

    val state: StateFlow<TileState<F1Ui>> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TileState.Loading,
    )

    init {
        viewModelScope.launch {
            while (true) {
                _state.value = repository.loadNextRace()
                val intervalMs = config.config.first().f1RefreshMin.coerceAtLeast(1) * 60_000L
                delay(intervalMs)
            }
        }
    }
}
