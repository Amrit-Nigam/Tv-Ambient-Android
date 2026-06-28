package com.tvport.dashboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.tvport.dashboard.core.AlbumScheme

/**
 * Day/night-aware color set. Tiles read from [LocalDash] rather than hardcoding colors,
 * so the day→night palette swap (BUILD SPEC §10) happens for free everywhere. The dim
 * overlay alpha is applied separately by the dimming layer; this swaps the actual hues.
 */
data class DashColors(
    val bg: Color,
    val raised: Color,
    val border: Color,
    val textHi: Color,
    val textMid: Color,
    val textLow: Color,
    val accent: Color,
    val accent2: Color,
    val isNight: Boolean,
)

val DayColors = DashColors(
    bg = Palette.Ink,
    raised = Palette.InkRaised,
    border = Palette.InkBorder,
    textHi = Palette.TextHi,
    textMid = Palette.TextMid,
    textLow = Palette.TextLow,
    accent = Palette.Teal,
    accent2 = Palette.Coral,
    isNight = false,
)

val NightColors = DashColors(
    bg = Palette.NightInk,
    raised = Palette.NightRaised,
    border = Palette.NightBorder,
    textHi = Palette.NightTextHi,
    textMid = Palette.NightTextMid,
    textLow = Palette.NightTextLow,
    accent = Palette.NightAccent,
    accent2 = Palette.NightAccent2,
    isNight = true,
)

// compositionLocalOf (NOT static): the provided DashColors is a brand-new instance on every frame
// of the album-color tweens, so we want only the composables that actually read it to recompose —
// not the entire dashboard subtree each frame.
val LocalDash = compositionLocalOf { DayColors }

/**
 * Re-theme the palette around the current album-art [scheme]: accents become the cover's hues and
 * the backgrounds become a deep tint of the dominant color, so the whole page takes on the album.
 * Text stays light. Night mode keeps the tint but darker. Returns the base palette unchanged when
 * no art has loaded yet.
 */
fun DashColors.withAlbumScheme(scheme: AlbumScheme?): DashColors {
    if (scheme == null) return this
    var accent = Color(scheme.accent)
    var accent2 = Color(scheme.accent2)
    // At night, pull the album accents toward the warm ember tone and knock them back so the
    // page never lights up the room — the cover still tints, but quietly.
    if (isNight) {
        accent = lerp(accent, Palette.NightAccent, 0.45f)
        accent2 = lerp(accent2, Palette.NightAccent2, 0.45f)
    }
    val bgMix = if (isNight) 0.94f else 0.87f      // how far toward black
    val raisedMix = if (isNight) 0.90f else 0.80f
    return copy(
        accent = accent,
        accent2 = accent2,
        bg = lerp(accent, Color.Black, bgMix),
        raised = lerp(accent, Color.Black, raisedMix),
        border = accent.copy(alpha = if (isNight) 0.16f else 0.22f),
    )
}

@Composable
fun DashTheme(isNight: Boolean, content: @Composable () -> Unit) {
    val colors = if (isNight) NightColors else DayColors
    CompositionLocalProvider(LocalDash provides colors, content = content)
}
