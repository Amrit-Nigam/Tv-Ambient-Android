package com.tvport.dashboard.ui.tiles.battery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.TileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Owns the BATTERIES tile. Battery levels move slowly, so a 30s poll is plenty (and the iPhone
 * only checks in every so often anyway). The repository never throws.
 */
@HiltViewModel
class BatteryViewModel @Inject constructor(
    private val repository: BatteryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TileState<BatteryUi>>(TileState.Loading)

    val state: StateFlow<TileState<BatteryUi>> = _state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TileState.Loading,
    )

    init {
        viewModelScope.launch {
            while (true) {
                _state.value = repository.load()
                delay(30_000L)
            }
        }
    }
}
