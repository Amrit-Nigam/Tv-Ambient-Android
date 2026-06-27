package com.tvport.dashboard.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.data.config.AppConfig
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Lightweight aggregator: owns the shared [AppConfig] that drives theme (day/night) and the
 * dim/burn-in layer. Individual tiles own their own ViewModels/StateFlows so each fails
 * independently (BUILD SPEC §11/§15) — this VM intentionally does NOT funnel tile state.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    configRepo: ConfigRepository,
) : ViewModel() {
    val config = configRepo.config.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppConfig(),
    )
}
