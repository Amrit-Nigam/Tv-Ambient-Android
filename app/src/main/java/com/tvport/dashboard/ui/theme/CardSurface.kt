package com.tvport.dashboard.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The single shared surface for every floating tile on the dashboard, so the boxes read as one
 * cohesive family instead of each styling itself. Gives depth the flat translucent panels lacked:
 *
 *  - a soft drop shadow to lift the card off the busy blurred album background,
 *  - a gentle top→bottom gradient fill (a hair more opaque than before, for legible text over any
 *    album colour),
 *  - a "glassy" hairline border that's brightest at the top edge and fades down.
 *
 * [borderTint] overrides the neutral border with a state colour (e.g. Claude's green/amber) while
 * keeping the same gradient falloff, so state still reads at a glance.
 */
@Composable
fun Modifier.dashCard(
    corner: Dp = Dimens.tileCorner,
    borderTint: Color? = null,
): Modifier {
    val c = LocalDash.current
    val shape = RoundedCornerShape(corner)

    // Fill: lighten the base "raised" slightly at the top so the card feels lit from above.
    val top = Color.White.copy(alpha = 0.05f).compositeOver(c.raised).copy(alpha = 0.90f)
    val bottom = c.raised.copy(alpha = 0.96f)

    val borderBrush = if (borderTint != null) {
        Brush.verticalGradient(
            listOf(borderTint.copy(alpha = 0.60f), borderTint.copy(alpha = 0.28f)),
        )
    } else {
        Brush.verticalGradient(
            listOf(c.accent.copy(alpha = 0.22f), c.border.copy(alpha = 0.45f)),
        )
    }

    return this
        .shadow(elevation = 14.dp, shape = shape, clip = false, spotColor = Color.Black, ambientColor = Color.Black)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, borderBrush, shape)
}
