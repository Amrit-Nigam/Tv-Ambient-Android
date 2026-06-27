package com.tvport.dashboard.ui.visualizer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.tvport.dashboard.ui.theme.LocalDash

/**
 * Ambient audio-visualizer background. Renders full-bleed BEHIND all tiles.
 *
 * - PRIMARY: tries to read the global output mix via [rememberAudioState] (Visualizer
 *   API). When a readable session exists, its smoothed amplitude modulates the field.
 * - FALLBACK: a continuous procedural field driven by an infinite transition, so the
 *   screen is *never* static even when no audio can be read (the common TV case where
 *   playback is a protected stream).
 *
 * Crash-proof: all audio access lives in [rememberAudioState] behind try/catch. This
 * composable never requests a runtime permission and never throws.
 */
@Composable
fun VisualizerBackground(modifier: Modifier = Modifier, isNight: Boolean = false) {
    val dash = LocalDash.current
    val audio = rememberAudioState()

    // Single looping phase drives all procedural motion. ~24s loop = slow & gentle.
    val transition = rememberInfiniteTransition(label = "ambient")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // A slow secondary pulse keeps the fallback alive even with zero audio.
    val breathe by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val paths = remember { FieldPaths() }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reading State values inside draw scope keeps the canvas invalidating each frame.
        val active = audio.active.value
        val level = audio.level.value

        // pulse: real audio when present, otherwise a gentle procedural breath.
        val pulse = if (active) {
            (0.15f + level * 0.85f).coerceIn(0f, 1f)
        } else {
            0.12f + 0.10f * breathe
        }

        drawProceduralField(
            phase = phase,
            pulse = pulse,
            accent = dash.accent,
            accent2 = dash.accent2,
            base = dash.bg,
            isNight = isNight,
            paths = paths,
        )
    }
}
