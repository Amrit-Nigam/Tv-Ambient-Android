package com.tvport.dashboard.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.tvport.dashboard.ui.theme.BodyFamily
import com.tvport.dashboard.ui.theme.Dimens
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.theme.dashCard

/**
 * The standard floating tile: translucent raised surface, subtle border, rounded corners,
 * a scrim so text stays legible over the visualizer. Every data tile renders inside this.
 */
@Composable
fun TileCard(
    modifier: Modifier = Modifier,
    content: @Composable (() -> Unit),
) {
    Box(
        modifier
            .dashCard()
            .padding(Dimens.tilePadding)
    ) {
        content()
    }
}

/** Small uppercase tile label with an accent dot. Consistent header across tiles. */
@Composable
fun TileHeader(label: String, icon: ImageVector? = null) {
    val c = LocalDash.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon ?: Icons.Filled.Circle,
            contentDescription = null,
            tint = c.accent,
            modifier = Modifier.size(if (icon == null) 10.dp else 18.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = label.uppercase(),
            color = c.textLow,
            fontFamily = BodyFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = 2.sp,
        )
    }
}

/** Quiet centered caption for Idle/Fallback states inside a tile. */
@Composable
fun TileQuietMessage(text: String) {
    val c = LocalDash.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = c.textLow,
            fontFamily = BodyFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
        )
    }
}
