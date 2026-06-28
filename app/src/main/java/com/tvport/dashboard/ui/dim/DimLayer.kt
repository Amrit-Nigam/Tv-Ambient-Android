package com.tvport.dashboard.ui.dim

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tvport.dashboard.core.tickerFlow
import com.tvport.dashboard.data.config.AppConfig
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

/** Warm near-black used for the night dimming scrim (a hint of amber, not pure black). */
private val NightScrim = Color(0xFF0A0604)

/** Snapshot of the dim/burn-in state, recomputed on a slow ticker. */
data class DimState(
    val isNight: Boolean,
    val dimAlpha: Float,
    val shiftXdp: Int,
    val shiftYdp: Int,
)

/**
 * Drives the pixel-shift and applies the caller-controlled [night] flag. Night is NO LONGER
 * time-based — it is toggled manually from the on-screen control (remote-friendly), so the panel
 * only dims when you ask it to. The pixel-shift vector still advances every
 * [AppConfig.pixelShiftPeriodSec] through a small set of offsets so no static element sits on the
 * same physical pixels for long (BUILD SPEC §10).
 */
@Composable
fun rememberDimState(cfg: AppConfig, night: Boolean): DimState {
    // Eight-step path around a small box, keeps motion gentle and bounded.
    val max = cfg.pixelShiftMaxPx
    val path = listOf(
        0 to 0, max to 0, max to max, 0 to max,
        -max to max, -max to 0, -max to -max, 0 to -max,
    )

    val shiftIndex by produceState(initialValue = 0, cfg) {
        if (!cfg.pixelShiftEnabled) { value = 0; return@produceState }
        var i = 0
        tickerFlow(cfg.pixelShiftPeriodSec * 1000L).collectLatest {
            value = i % path.size
            i++
        }
    }
    val (sx, sy) = path[shiftIndex]
    val alpha = if (night) cfg.nightDimLevel else cfg.dayDimLevel
    return DimState(isNight = night, dimAlpha = alpha, shiftXdp = sx, shiftYdp = sy)
}

/**
 * Wraps the whole dashboard: applies the animated pixel-shift offset to [content] and draws
 * a dimming scrim on top for night mode. The shift animates smoothly so it reads as gentle
 * drift, not a jump.
 */
@Composable
fun BurnInDimSurface(
    state: DimState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val animX by animateFloatAsState(state.shiftXdp.toFloat(), tween(4000), label = "shiftX")
    val animY by animateFloatAsState(state.shiftYdp.toFloat(), tween(4000), label = "shiftY")
    val animAlpha by animateFloatAsState(state.dimAlpha, tween(3000), label = "dim")

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(animX.dp.roundToPx(), animY.dp.roundToPx()) }
        ) {
            content()
        }
        if (animAlpha > 0.001f) {
            // A very dark warm scrim (not pure black) so night dimming reads as cozy dusk rather
            // than a flat grey veil — the warmth keeps skin tones / album art from going corpse-blue.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(NightScrim.copy(alpha = animAlpha.coerceIn(0f, 0.95f)))
            )
        }
    }
}
