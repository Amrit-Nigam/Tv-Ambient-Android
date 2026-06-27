package com.tvport.dashboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

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

val LocalDash = staticCompositionLocalOf { DayColors }

@Composable
fun DashTheme(isNight: Boolean, content: @Composable () -> Unit) {
    val colors = if (isNight) NightColors else DayColors
    CompositionLocalProvider(LocalDash provides colors, content = content)
}
