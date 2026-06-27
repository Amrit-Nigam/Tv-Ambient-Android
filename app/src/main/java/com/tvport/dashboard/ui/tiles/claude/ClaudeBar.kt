package com.tvport.dashboard.ui.tiles.claude

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.core.tickerFlow
import com.tvport.dashboard.ui.theme.BodyFamily
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.theme.MonoFamily

private val Green = Color(0xFF22C55E) // portfolio accent-dot green
private val Amber = Color(0xFFE0B65B)

/** Whimsical gerunds, in the spirit of Claude Code's terminal status line. */
private val WORDS = listOf(
    "Thinking", "Noodling", "Finagling", "Conjuring", "Percolating", "Whatchamacalliting",
    "Tinkering", "Scheming", "Cooking", "Pondering", "Computing", "Vibing", "Ruminating",
    "Wrangling", "Brewing", "Spelunking", "Marinating", "Concocting", "Untangling", "Manifesting",
)

/**
 * Live Claude terminal status bar, styled after the CLI status line: a pulsing "ping" dot (the
 * portfolio's `ping-dot` animation), a rotating spinner-star, a whimsical rotating gerund word
 * ("Whatchamacalliting…") while busy, and a live elapsed timer + project. Flips to a green
 * "finished" when the turn's Stop hook fires; amber when it needs you; dim when offline.
 */
@Composable
fun ClaudeBar(modifier: Modifier = Modifier) {
    val vm: ClaudeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val c = LocalDash.current

    val ui: ClaudeUi? = when (val s = state) {
        is TileState.Content -> s.data
        is TileState.Fallback -> s.data
        else -> null
    }
    val offline = state is TileState.Fallback
    val busy = ui?.busy == true && !offline
    val tint = when {
        offline || ui == null -> c.textLow
        ui.kind == ClaudeKind.DONE -> Green
        ui.kind == ClaudeKind.WAITING || ui.kind == ClaudeKind.PERMISSION -> Amber
        busy -> c.accent
        else -> c.textLow
    }

    // live elapsed seconds while busy
    val nowSec by produceState(initialValue = System.currentTimeMillis() / 1000L) {
        tickerFlow(1000L).collect { value = System.currentTimeMillis() / 1000L }
    }
    val secs = if (busy && ui!!.startedAt > 0) (nowSec - ui.startedAt).coerceAtLeast(0L) else 0L

    // The headline word: rotate gerunds every ~3s while busy; otherwise the real state label.
    val word = if (busy) WORDS[((secs / 3) % WORDS.size).toInt()] else (ui?.label ?: "Idle")

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.raised.copy(alpha = 0.78f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // The portfolio pixel creature, animated by Claude's state.
            Creature(
                kind = ui?.kind ?: ClaudeKind.IDLE,
                busy = busy,
                modifier = Modifier.size(40.dp),
            )
            // A clean "!" when it needs you (like the portfolio creature's alert).
            if (!offline && (ui?.kind == ClaudeKind.WAITING || ui?.kind == ClaudeKind.PERMISSION)) {
                Spacer(Modifier.width(8.dp))
                PixelBang(Amber)
            }
            Spacer(Modifier.width(16.dp))

            // rotating whimsical word, crossfading
            Crossfade(targetState = word, animationSpec = tween(350), label = "word") { w ->
                Text(
                    text = if (busy) "$w…" else w,
                    color = if (offline) c.textLow else c.textHi,
                    fontFamily = BodyFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 21.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // meta: "· 28s · tvport"  (dim, like the terminal's parenthetical)
            if (!offline && ui != null) {
                val meta = buildString {
                    if (busy && ui.startedAt > 0) append(" · ${fmt(secs)}")
                    if (ui.project.isNotBlank()) append(" · ")
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        color = c.textLow,
                        fontFamily = MonoFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                    )
                }
                if (ui.project.isNotBlank()) ProjectChip(ui.project, tint)
            }
            if (offline) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "laptop asleep / off-network",
                    color = c.textLow,
                    fontFamily = BodyFamily,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.weight(1f))

            if (!offline && ui?.kind == ClaudeKind.DONE) {
                Text("finished", color = Green, fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}

/** A blocky pixel "!" that bobs — the creature's "needs you" alert (matches the portfolio art). */
@Composable
private fun PixelBang(tint: Color) {
    val t = rememberInfiniteTransition(label = "bang")
    val dy by t.animateFloat(-2f, 2f, infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse), label = "bob")
    Canvas(Modifier.size(14.dp, 34.dp).offset(y = dy.dp)) {
        val w = size.width
        val bar = w * 0.55f
        val x = (w - bar) / 2f
        // stem
        drawRect(tint, topLeft = Offset(x, 0f), size = Size(bar, size.height * 0.62f))
        // dot
        drawRect(tint, topLeft = Offset(x, size.height * 0.76f), size = Size(bar, bar))
    }
}

@Composable
private fun ProjectChip(project: String, tint: Color) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(project, color = tint, fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

private fun fmt(secs: Long): String = if (secs >= 60) "%dm %02ds".format(secs / 60, secs % 60) else "${secs}s"
