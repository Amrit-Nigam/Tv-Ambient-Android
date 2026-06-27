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
    border = Palette.InkBorder.copy(alpha = 0.5f),
    textHi = Palette.NightTextHi,
    textMid = Palette.NightTextMid,
    textLow = Palette.TextLow.copy(alpha = 0.7f),
    accent = Palette.TealDim,
    accent2 = Palette.Coral.copy(alpha = 0.8f),
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
    val accent = Color(scheme.accent)
    val accent2 = Color(scheme.accent2)
    val bgMix = if (isNight) 0.92f else 0.87f      // how far toward black
    val raisedMix = if (isNight) 0.86f else 0.80f
    return copy(
        accent = accent,
        accent2 = accent2,
        bg = lerp(accent, Color.Black, bgMix),
        raised = lerp(accent, Color.Black, raisedMix),
        border = accent.copy(alpha = 0.22f),
    )
}

@Composable
fun DashTheme(isNight: Boolean, content: @Composable () -> Unit) {
    val colors = if (isNight) NightColors else DayColors
    CompositionLocalProvider(LocalDash provides colors, content = content)
}
