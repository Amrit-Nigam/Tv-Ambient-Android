package com.tvport.dashboard.ui.tiles.f1

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsMotorsports
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.core.tickerFlow
import com.tvport.dashboard.ui.dashboard.TileCard
import com.tvport.dashboard.ui.dashboard.TileHeader
import com.tvport.dashboard.ui.dashboard.TileQuietMessage
import com.tvport.dashboard.ui.theme.BodyFamily
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash

/**
 * NEXT F1 RACE tile. Real data from Jolpica-F1 (the Ergast mirror the portfolio used — free, no
 * key). Shows the round, race name, circuit + location, a LIVE countdown to lights-out, and the
 * local race time. Off-season -> quiet Idle; fetch failure -> last-known via Fallback.
 */
@Composable
fun F1Tile(modifier: Modifier = Modifier) {
    val vm: F1ViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    TileCard(modifier) {
        Column(Modifier.fillMaxSize()) {
            TileHeader(label = "Next Race", icon = Icons.Filled.SportsMotorsports)
            Spacer(Modifier.height(8.dp))

            when (val s = state) {
                is TileState.Content -> F1Body(s.data)
                is TileState.Fallback -> {
                    val data = s.data
                    if (data != null) F1Body(data) else TileQuietMessage("F1 schedule unavailable")
                }
                is TileState.Idle -> TileQuietMessage(s.message)
                TileState.Loading -> TileQuietMessage("Loading race…")
            }
        }
    }
}

@Composable
private fun F1Body(ui: F1Ui) {
    val c = LocalDash.current

    val now by produceState(initialValue = System.currentTimeMillis()) {
        tickerFlow(1000L).collect { value = System.currentTimeMillis() }
    }
    val remaining = ui.raceStartMillis - now

    Column(Modifier.fillMaxSize()) {
        if (ui.round != null) {
            Text(
                text = ui.round.uppercase(),
                color = c.accent,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(6.dp))
        }

        Text(
            text = ui.raceName,
            color = c.textHi,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 25.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = ui.location.ifBlank { ui.circuitName },
            color = c.textMid,
            fontFamily = BodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = formatCountdown(remaining),
            color = c.accent2,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
        )
    }
}

/** "in 1d 2h 14m" while far out, "in 14m 09s" in the final hour, "Lights out!"/"Racing" once started. */
private fun formatCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0L) {
        return if (remainingMillis > -2 * 60_000L) "Lights out!" else "Racing"
    }
    val total = remainingMillis / 1000L
    val days = total / 86_400L
    val hours = (total % 86_400L) / 3600L
    val minutes = (total % 3600L) / 60L
    val seconds = total % 60L
    return if (remainingMillis >= 60 * 60_000L) {
        if (days > 0L) "in ${days}d ${hours}h ${minutes}m" else "in ${hours}h ${minutes}m"
    } else {
        "in ${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
