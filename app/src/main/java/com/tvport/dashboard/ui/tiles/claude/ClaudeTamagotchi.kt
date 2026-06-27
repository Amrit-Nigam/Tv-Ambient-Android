package com.tvport.dashboard.ui.tiles.claude

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tvport.dashboard.ui.theme.Dimens
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.theme.MonoFamily
import com.tvport.dashboard.ui.theme.dashCard

private val Green = Color(0xFF22C55E)
private val Amber = Color(0xFFE0B65B)

/** Whimsical gerunds, in the spirit of Claude Code's terminal status line. */
private val WORDS = listOf(
    "Thinking", "Noodling", "Finagling", "Conjuring", "Percolating", "Whatchamacalliting",
    "Tinkering", "Scheming", "Cooking", "Pondering", "Computing", "Vibing", "Ruminating",
    "Wrangling", "Brewing", "Spelunking", "Marinating", "Concocting", "Untangling", "Manifesting",
)

/**
 * CLAWD — the dashboard's hero. A tamagotchi-style habitat for the live Claude creature: the GIF
 * picks itself from Claude's real session state (working / happy / alert / asleep), floats with a
 * gentle idle bob, and sits over a soft accent glow that breathes faster while busy. Below it, a
 * "nameplate" shows the rotating whimsical word, the live elapsed timer, and the project — flipping
 * to a green "finished" at turn-end, amber "needs you" when it wants you, dim "asleep" when offline.
 */
@Composable
fun ClaudeTamagotchi(modifier: Modifier = Modifier) {
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
    val kind = ui?.kind ?: ClaudeKind.IDLE
    val needsYou = !offline && (kind == ClaudeKind.WAITING || kind == ClaudeKind.PERMISSION)

    // Audible alert on the TWO state CHANGES the user cares about: a friendly two-note chime when a
    // turn finishes (DONE), and an attention double-beep when Claude needs you (WAITING/PERMISSION).
    // Only on transition (prevKind != kind) and never on first load, so it doesn't fire on launch.
    var prevKind by remember { mutableStateOf<ClaudeKind?>(null) }
    LaunchedEffect(kind, offline) {
        val previous = prevKind
        prevKind = kind
        if (offline || previous == null || previous == kind) return@LaunchedEffect
        when (kind) {
            ClaudeKind.DONE -> playClaudeChime(needsHelp = false)
            ClaudeKind.WAITING, ClaudeKind.PERMISSION -> playClaudeChime(needsHelp = true)
            else -> {}
        }
    }

    val tint = when {
        offline || ui == null -> c.textLow
        kind == ClaudeKind.DONE -> Green
        needsYou -> Amber
        busy -> c.accent
        else -> c.textLow
    }

    // live elapsed seconds while busy
    val nowSec by produceState(initialValue = System.currentTimeMillis() / 1000L) {
        tickerFlow(1000L).collect { value = System.currentTimeMillis() / 1000L }
    }
    val secs = if (busy && ui!!.startedAt > 0) (nowSec - ui.startedAt).coerceAtLeast(0L) else 0L
    val word = if (busy) WORDS[((secs / 3) % WORDS.size).toInt()] else (ui?.label ?: "Asleep")

    // --- idle life: a gentle vertical bob (a touch faster while busy) ---
    val life = rememberInfiniteTransition(label = "life")
    val bobPeriod = if (busy) 1400 else 2600
    val bob by life.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(bobPeriod, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )

    Box(
        modifier
            .dashCard(borderTint = tint)
            .padding(Dimens.tilePadding),
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // --- LEFT: the creature, gently floating ---
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .offset(y = bob.dp),
                contentAlignment = Alignment.Center,
            ) {
                Creature(kind = kind, busy = busy, modifier = Modifier.fillMaxSize())
                // No drawn "!" — the alert GIF already shows one.
            }

            Spacer(Modifier.width(16.dp))

            // --- RIGHT: header + rotating word + live meta, left-aligned ---
            Column(
                Modifier.weight(1.25f),
                verticalArrangement = Arrangement.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(tint))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "CLAUDE CODE",
                        color = c.textLow,
                        fontFamily = BodyFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        letterSpacing = 2.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Crossfade(targetState = word, animationSpec = tween(350), label = "word") { w ->
                    Text(
                        text = if (busy) "$w…" else w,
                        color = if (offline) c.textLow else c.textHi,
                        fontFamily = DisplayFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 27.sp,
                        lineHeight = 30.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        offline -> Text(
                            "laptop asleep / off-network",
                            color = c.textLow, fontFamily = BodyFamily, fontSize = 15.sp,
                        )
                        kind == ClaudeKind.DONE -> Text(
                            "finished",
                            color = Green, fontFamily = BodyFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                        )
                        busy && ui!!.startedAt > 0 -> Text(
                            fmt(secs),
                            color = c.textMid, fontFamily = MonoFamily,
                            fontWeight = FontWeight.Medium, fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

private fun fmt(secs: Long): String = if (secs >= 60) "%dm %02ds".format(secs / 60, secs % 60) else "${secs}s"

/**
 * Plays a short notification chime through the TV speakers via [ToneGenerator] (no audio asset
 * needed, mixes over any music). [needsHelp] = an attention-grabbing double-beep; otherwise a
 * friendlier two-note "done". Best-effort: if the device can't allocate the tone generator we just
 * skip silently rather than crash the dashboard.
 */
private suspend fun playClaudeChime(needsHelp: Boolean) {
    val tg = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    } catch (_: RuntimeException) {
        return
    }
    try {
        if (needsHelp) {
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
            delay(700)
        } else {
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            delay(180)
            tg.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            delay(450)
        }
    } finally {
        tg.release()
    }
}
