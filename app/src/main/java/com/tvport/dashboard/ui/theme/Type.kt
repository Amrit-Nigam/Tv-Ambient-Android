package com.tvport.dashboard.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tvport.dashboard.R

/**
 * Geist (Vercel) — https://vercel.com/font.
 *  - [DisplayFamily] / [BodyFamily]: Geist Sans for the clock, titles, and all body text.
 *  - [MonoFamily]: Geist Mono for ticking numerics (countdowns, progress times) — tabular figures
 *    keep the digits from jiggling as they update.
 * Variable-font files render at their default instance on API < 26 and vary weight on 26+.
 */
val DisplayFamily = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal),
    Font(R.font.geist_variable, FontWeight.Medium),
    Font(R.font.geist_variable, FontWeight.SemiBold),
    Font(R.font.geist_variable, FontWeight.Bold),
)

val BodyFamily = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal),
    Font(R.font.geist_variable, FontWeight.Medium),
    Font(R.font.geist_variable, FontWeight.SemiBold),
    Font(R.font.geist_variable, FontWeight.Bold),
)

val MonoFamily = FontFamily(
    Font(R.font.geist_mono_variable, FontWeight.Normal),
    Font(R.font.geist_mono_variable, FontWeight.Medium),
    Font(R.font.geist_mono_variable, FontWeight.Bold),
)
