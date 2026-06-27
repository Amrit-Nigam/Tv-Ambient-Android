package com.tvport.dashboard.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tvport.dashboard.R

/**
 * Two families: Space Grotesk (geometric, tabular figures) for the display clock and
 * large numerics; Inter for everything textual. Variable-font files render at their
 * default instance on API < 26 and vary weight on 26+.
 */
val DisplayFamily = FontFamily(
    Font(R.font.space_grotesk_variable, FontWeight.Normal),
    Font(R.font.space_grotesk_variable, FontWeight.Medium),
    Font(R.font.space_grotesk_variable, FontWeight.Bold),
)

val BodyFamily = FontFamily(
    Font(R.font.inter_variable, FontWeight.Normal),
    Font(R.font.inter_variable, FontWeight.Medium),
    Font(R.font.inter_variable, FontWeight.SemiBold),
    Font(R.font.inter_variable, FontWeight.Bold),
)
