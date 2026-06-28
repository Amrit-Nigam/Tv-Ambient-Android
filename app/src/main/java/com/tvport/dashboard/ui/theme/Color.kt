package com.tvport.dashboard.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Cohesive "deep teal dusk" palette. Dark by design — this runs 24/7 on a panel, so we
 * keep large areas near-black for burn-in safety and lean on a couple of accent hues.
 */
object Palette {
    // Backgrounds (day)
    val Ink = Color(0xFF0E1116)        // near-black base
    val InkRaised = Color(0xFF161B22)  // tile surface
    val InkBorder = Color(0xFF243038)

    // Backgrounds (night — "midnight ember": deeper, and warm rather than cold-blue, so the
    // panel feels like low candlelight in a dark room instead of a glowing cold screen).
    val NightInk = Color(0xFF070506)      // near-pure black with the faintest warm cast
    val NightRaised = Color(0xFF120E0D)   // tile surface, warm charcoal
    val NightBorder = Color(0xFF2A211C)   // warm hairline

    // Accents
    val Teal = Color(0xFF5BE0C4)       // primary accent (clock, progress)
    val TealDim = Color(0xFF2E7A6C)
    val Coral = Color(0xFFFF8C66)      // secondary accent (alerts, rain, countdown)
    val Gold = Color(0xFFE0C45B)

    // Night accents (warm amber/ember — far less blue light than teal, gentle on dark-adapted eyes)
    val NightAccent = Color(0xFFE0A86A)   // warm amber, primary at night
    val NightAccent2 = Color(0xFFD98158)  // soft terracotta, secondary at night

    // Text
    val TextHi = Color(0xFFF2F5F7)     // high-emphasis
    val TextMid = Color(0xFFAEB9C2)    // secondary
    val TextLow = Color(0xFF6B7780)    // tertiary / captions

    // Night text (lower luminance + a warm tint to reduce glare in a dark room)
    val NightTextHi = Color(0xFFD8CFC4)
    val NightTextMid = Color(0xFF8C8077)
    val NightTextLow = Color(0xFF5C534C)
}
