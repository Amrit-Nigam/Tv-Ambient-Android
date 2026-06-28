package com.tvport.dashboard.ui.tiles.battery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvport.dashboard.R
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.ui.dashboard.TileCard
import com.tvport.dashboard.ui.dashboard.TileQuietMessage
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash

/** Apple-style "Batteries" tile: a green ring per device with the icon centred and % below. */
@Composable
fun BatteryTile(modifier: Modifier = Modifier) {
    val vm: BatteryViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    TileCard(modifier) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is TileState.Content -> BatteryBody(s.data)
                is TileState.Fallback -> {
                    val d = s.data
                    if (d != null) BatteryBody(d) else TileQuietMessage("Battery unavailable")
                }
                is TileState.Idle -> TileQuietMessage(s.message)
                TileState.Loading -> TileQuietMessage("Reading batteries…")
            }
        }
    }
}

private enum class DeviceKind { Mac, Phone, AirPods, Case }

@Composable
private fun BatteryBody(ui: BatteryUi) {
    Row(
        Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BatteryRing("MacBook", DeviceKind.Mac, ui.mac)
        BatteryRing("iPhone", DeviceKind.Phone, ui.phone)
        BatteryRing("AirPods", DeviceKind.AirPods, ui.airpods)
        BatteryRing("Case", DeviceKind.Case, ui.airpodsCase)
    }
}

/** One device: green ring with the icon centred and the % below — the Apple "Batteries" look. */
@Composable
private fun BatteryRing(label: String, kind: DeviceKind, dev: DeviceBattery) {
    val c = LocalDash.current
    val level = dev.level
    val pct = (level ?: 0).coerceIn(0, 100)
    val sweep by animateFloatAsState(
        targetValue = if (level == null) 0f else pct / 100f,
        animationSpec = tween(800),
        label = "battSweep",
    )
    val ringColor = when {
        level == null -> c.textLow
        pct <= 20 -> BatteryRed
        else -> BatteryGreen
    }
    val track = Color.White.copy(alpha = 0.10f)
    val iconTint = c.textHi

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(46.dp)) {
                val stroke = 4.5.dp.toPx()
                val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arcSize = Size(d, d)
                drawArc(
                    color = track, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                    topLeft = tl, size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
                if (sweep > 0f) {
                    drawArc(
                        color = ringColor, startAngle = -90f, sweepAngle = 360f * sweep,
                        useCenter = false, topLeft = tl, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            }
            when (kind) {
                DeviceKind.Mac, DeviceKind.Phone -> Icon(
                    imageVector = if (kind == DeviceKind.Mac) Icons.Filled.LaptopMac else Icons.Filled.PhoneIphone,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
                // AirPods Pro icon by Icons8 (https://icons8.com/icon/X862ot5xqpnL/airpods-pro), tinted.
                DeviceKind.AirPods -> Icon(
                    painter = painterResource(R.drawable.ic_airpods_pro),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
                DeviceKind.Case -> Icon(
                    painter = painterResource(R.drawable.ic_airpods_case),
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(26.dp),
                )
            }
            // Charging bolt as a small badge at the ring's top, like the Apple widget.
            if (dev.charging) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier
                            .offset(y = (-7).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(c.bg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Charging",
                            tint = BatteryGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = if (level == null) "—" else "$pct%",
            color = if (dev.stale) c.textLow else c.textHi,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
    }
}

private val BatteryGreen = Color(0xFF34C759)
private val BatteryRed = Color(0xFFFF5A4D)
