package com.tvport.dashboard.ui.visualizer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Stateless procedural ambient field. All geometry is driven by a single phase value
 * (0f..1f, supplied by an infinite transition) plus the optional audio amplitude, so it
 * is fully deterministic and allocation-light: the wave [Path] objects are reused across
 * frames (passed in from the composable's `remember`).
 *
 * Visual: a near-black vertical wash, two slow flowing sine "ribbons" in the accent
 * hues, and a few breathing concentric rings centered off to the side. Everything is low
 * alpha so dashboard tiles stay readable on top. Night mode dims every layer further.
 */
class FieldPaths {
    val waveA = Path()
    val waveB = Path()
}

private const val TWO_PI = (PI * 2).toFloat()

fun DrawScope.drawProceduralField(
    phase: Float,        // 0f..1f looping
    pulse: Float,        // 0f..1f extra audio-driven energy (0 when no audio)
    accent: Color,       // teal
    accent2: Color,      // coral
    base: Color,         // near-black background base
    isNight: Boolean,
    paths: FieldPaths,
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val dim = if (isNight) 0.45f else 1f
    val ph = phase * TWO_PI

    // --- Base wash: a gentle radial-ish vertical gradient so it's never flat black. ---
    drawRect(
        brush = Brush.verticalGradient(
            0f to base,
            0.55f to base.copy(alpha = 1f),
            1f to lerpColor(base, accent, 0.06f * dim),
        ),
        size = Size(w, h),
    )

    // Subtle moving glow blob, top-left to add life.
    val glowX = w * (0.3f + 0.12f * sin(ph * 0.5f))
    val glowY = h * (0.32f + 0.10f * cos(ph * 0.37f))
    val glowR = (h * 0.6f) * (1f + 0.08f * pulse)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                accent.copy(alpha = (0.07f + 0.06f * pulse) * dim),
                Color.Transparent,
            ),
            center = Offset(glowX, glowY),
            radius = glowR,
        ),
        radius = glowR,
        center = Offset(glowX, glowY),
    )

    // --- Two flowing ribbons across the lower portion of the screen. ---
    val amp = h * (0.05f + 0.10f * pulse)
    buildWave(
        paths.waveA, w, h,
        baseY = h * 0.72f,
        amp = amp,
        freq = 1.4f,
        phase = ph,
    )
    drawPath(
        path = paths.waveA,
        brush = Brush.verticalGradient(
            colors = listOf(
                accent.copy(alpha = (0.10f + 0.08f * pulse) * dim),
                Color.Transparent,
            ),
            startY = h * 0.6f,
            endY = h,
        ),
        style = Fill,
    )

    buildWave(
        paths.waveB, w, h,
        baseY = h * 0.82f,
        amp = amp * 0.8f,
        freq = 2.1f,
        phase = -ph * 1.3f + 1.7f,
    )
    drawPath(
        path = paths.waveB,
        brush = Brush.verticalGradient(
            colors = listOf(
                accent2.copy(alpha = (0.08f + 0.07f * pulse) * dim),
                Color.Transparent,
            ),
            startY = h * 0.7f,
            endY = h,
        ),
        style = Fill,
    )

    // --- Breathing concentric rings, anchored toward the right edge. ---
    val ringCenter = Offset(w * 0.82f, h * 0.4f)
    val rings = 4
    val maxR = h * 0.5f
    for (i in 0 until rings) {
        val t = (phase + i.toFloat() / rings) % 1f
        val r = maxR * (0.2f + 0.8f * t) * (1f + 0.12f * pulse)
        val fade = (1f - t)
        val a = (0.06f + 0.05f * pulse) * fade * dim
        if (a <= 0.002f) continue
        drawCircle(
            color = (if (i % 2 == 0) accent else accent2).copy(alpha = a),
            radius = r,
            center = ringCenter,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f + 2f * pulse),
        )
    }
}

/** Rebuilds [path] in place as a filled wave band from left to right. */
private fun buildWave(
    path: Path,
    w: Float,
    h: Float,
    baseY: Float,
    amp: Float,
    freq: Float,
    phase: Float,
) {
    path.reset()
    val steps = 28
    path.moveTo(0f, baseY)
    for (i in 0..steps) {
        val x = w * i / steps
        val t = i.toFloat() / steps
        val y = baseY + amp * sin(t * freq * TWO_PI + phase) +
            amp * 0.4f * sin(t * freq * 2.3f * TWO_PI - phase * 0.6f)
        path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.lineTo(0f, h)
    path.close()
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = a.alpha + (b.alpha - a.alpha) * tt,
    )
}
