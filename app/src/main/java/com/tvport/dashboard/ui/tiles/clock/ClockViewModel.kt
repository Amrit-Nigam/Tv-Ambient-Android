package com.tvport.dashboard.ui.tiles.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvport.dashboard.core.tickerFlow
import com.tvport.dashboard.data.config.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ClockUi(
    val time: String,
    val ampm: String,   // empty in 24h mode
    val date: String,
)

/**
 * Live clock. A 1s ticker drives a recompute of the formatted time; combined with config so
 * the 12/24h preference is honored immediately. No busy loop — see [tickerFlow].
 */
@HiltViewModel
class ClockViewModel @Inject constructor(
    config: ConfigRepository,
) : ViewModel() {

    val ui = combine(tickerFlow(1000L), config.config) { _, cfg ->
        val now = Date()
        val timeFmt = SimpleDateFormat(if (cfg.use24Hour) "HH:mm" else "hh:mm", Locale.getDefault())
        val ampmFmt = SimpleDateFormat("a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEE d MMM yyyy", Locale.getDefault())
        ClockUi(
            time = timeFmt.format(now),
            ampm = if (cfg.use24Hour) "" else ampmFmt.format(now),
            date = dateFmt.format(now),
        )
    }.map { it }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ClockUi("--:--", "", ""),
    )
}
