package com.tvport.dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.AlbumColorBus
import com.tvport.dashboard.core.AlbumScheme
import com.tvport.dashboard.data.config.AppConfig
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Lightweight aggregator: owns the shared [AppConfig] that drives theme (day/night) and the
 * dim/burn-in layer, plus the [AlbumScheme] extracted from the current album art that re-themes
 * the whole page. Individual tiles own their own ViewModels/StateFlows so each fails independently.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    configRepo: ConfigRepository,
    albumColorBus: AlbumColorBus,
) : ViewModel() {
    val config = configRepo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppConfig(),
    )

    val albumScheme: StateFlow<AlbumScheme?> = albumColorBus.scheme
    val albumArtUrl: StateFlow<String?> = albumColorBus.artUrl
}
