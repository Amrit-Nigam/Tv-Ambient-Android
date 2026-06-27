package com.tvport.dashboard.ui.tiles.fifa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * NEXT MATCH tile (football-data.org). Shows the upcoming fixture's teams, competition, a LIVE
 * countdown to kick-off (re-computed every second from the ticker), and the local kick-off time.
 *
 * Real data when a FOOTBALL_DATA_TOKEN is configured; otherwise a LABELED STATIC FALLBACK arrives
 * as [TileState.Fallback] carrying a sample [FifaUi] (isFallback = true) — rendered with a small
 * "sample" chip so it is never mistaken for a real fixture. Never blanks, never crashes.
 */
@Composable
fun FifaTile(modifier: Modifier = Modifier) {
    val vm: FifaViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    TileCard(modifier) {
        Column(Modifier.fillMaxSize()) {
            TileHeader(label = "Next Match", icon = Icons.Filled.SportsSoccer)
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is TileState.Content -> FifaBody(s.data)
                is TileState.Fallback -> {
                    val data = s.data
                    if (data != null) FifaBody(data)
                    else TileQuietMessage("No fixtures")
                }
                is TileState.Idle -> TileQuietMessage(s.message)
                TileState.Loading -> TileQuietMessage("Loading fixture…")
            }
        }
    }
}

@Composable
private fun FifaBody(ui: FifaUi) {
    val c = LocalDash.current

    // Re-compute "now" every second off the shared 1s ticker so the countdown ticks live.
    // produceState seeds with the current time, then updates on each tick emission.
    val now by produceState(initialValue = System.currentTimeMillis()) {
        tickerFlow(1000L).collect { value = System.currentTimeMillis() }
    }
    val remaining = ui.kickoffMillis - now

    Column(Modifier.fillMaxSize()) {
        // --- Competition + (optional) sample chip ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ui.competition != null) {
                Text(
                    text = ui.competition.uppercase(),
                    color = c.accent,
                    fontFamily = BodyFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    letterSpacing = 1.5.sp,
                )
            }
            if (ui.isFallback) {
                if (ui.competition != null) Spacer(Modifier.width(10.dp))
                SampleChip()
            }
        }

        Spacer(Modifier.height(10.dp))

        // --- HOME  v  AWAY, big and bold ---
        Text(
            text = ui.homeTeam,
            color = c.textHi,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 27.sp,
            maxLines = 1,
        )
        Text(
            text = "v  ${ui.awayTeam}",
            color = c.textHi,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 27.sp,
            maxLines = 1,
        )

        Spacer(Modifier.height(16.dp))

        // --- Live countdown, prominent in coral/accent2 ---
        Text(
            text = formatCountdown(remaining),
            color = c.accent2,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
        )

        Spacer(Modifier.height(6.dp))

        // --- Local kick-off date + time ---
        Text(
            text = ui.kickoffLocalLabel,
            color = c.textMid,
            fontFamily = BodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
        )
    }
}

/** Tiny subtle pill that honestly labels the static fallback as a sample (not a real fixture). */
@Composable
private fun SampleChip() {
    val c = LocalDash.current
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(c.textLow.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = "SAMPLE",
            color = c.textLow,
            fontFamily = BodyFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
        )
    }
}

/**
 * Human countdown from millis remaining:
 *   - past kick-off:        "Kick-off!" (just started) then "Live"
 *   - >= 1 hour out:        "in 2d 4h 31m"
 *   - < 1 hour out:         "in 12m 09s"
 */
private fun formatCountdown(remainingMillis: Long): String {
    if (remainingMillis <= 0L) {
        // Within ~2 min after kickoff show "Kick-off!", then settle to "Live".
        return if (remainingMillis > -2 * 60_000L) "Kick-off!" else "Live"
    }
    val totalSeconds = remainingMillis / 1000L
    val days = totalSeconds / 86_400L
    val hours = (totalSeconds % 86_400L) / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L

    return if (remainingMillis >= 60 * 60_000L) {
        // An hour or more away: coarse "in Xd Yh Zm" (drop days when zero).
        if (days > 0L) "in ${days}d ${hours}h ${minutes}m"
        else "in ${hours}h ${minutes}m"
    } else {
        // Final hour: tick seconds, zero-padded, e.g. "in 12m 09s".
        "in ${minutes}m ${seconds.toString().padStart(2, '0')}s"
    }
}
