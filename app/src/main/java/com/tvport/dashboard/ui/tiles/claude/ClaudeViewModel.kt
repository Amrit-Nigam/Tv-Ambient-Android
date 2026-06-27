package com.tvport.dashboard.ui.tiles.claude

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.TileState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Exposes the pushed Claude status as a StateFlow. No polling — [ClaudeRepository.stream] is a
 * server-pushed SSE flow; we just hold the latest value (and keep the connection alive while the
 * dashboard is on screen).
 */
@HiltViewModel
class ClaudeViewModel @Inject constructor(
    repository: ClaudeRepository,
) : ViewModel() {

    val state: StateFlow<TileState<ClaudeUi>> = repository.stream().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TileState.Loading,
    )
}
