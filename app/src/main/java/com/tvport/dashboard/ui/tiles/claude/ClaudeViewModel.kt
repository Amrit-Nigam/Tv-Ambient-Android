package com.tvport.dashboard.ui.tiles.claude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Polls the Claude status endpoint every [AppConfig.claudePollSec] seconds so the bar reflects
 * the terminal in near real-time. Keeps the last good value so a momentary network blip doesn't
 * blank the bar. Never throws (repository returns TileState only).
 */
@HiltViewModel
class ClaudeViewModel @Inject constructor(
    private val repository: ClaudeRepository,
    private val config: ConfigRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<TileState<ClaudeUi>>(TileState.Loading)
    val state: StateFlow<TileState<ClaudeUi>> = _state.asStateFlow()

    private var lastKnown: ClaudeUi? = null

    init {
        viewModelScope.launch {
            while (isActive) {
                val result = repository.fetch(lastKnown)
                if (result is TileState.Content) lastKnown = result.data
                _state.value = result
                val sec = config.config.first().claudePollSec.coerceAtLeast(1)
                delay(sec * 1000L)
            }
        }
    }
}
