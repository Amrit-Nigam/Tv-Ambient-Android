package com.tvport.dashboard.ui.tiles.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash

/**
 * The visual centerpiece. Big tabular display time + date line. Not wrapped in a TileCard —
 * it floats directly on the visualizer as the anchor of the screen (BUILD SPEC §3/§6).
 */
@Composable
fun ClockTile(modifier: Modifier = Modifier) {
    val vm: ClockViewModel = hiltViewModel()
    val ui by vm.ui.collectAsStateWithLifecycle()
    val c = LocalDash.current

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = ui.time,
                color = c.textHi,
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                lineHeight = 66.sp,
                letterSpacing = (-2).sp,
            )
            if (ui.ampm.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = ui.ampm.uppercase(),
                    color = c.accent,
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 34.sp,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
        Text(
            text = ui.date,
            color = c.textMid,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 26.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
    }
}
