package com.tvport.dashboard.ui.tiles.nowplaying

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.ui.theme.BodyFamily
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The hero: Now Playing rendered as a spinning vinyl record. The album art is the record's center
 * label, ringed by a glowing accent (the page already re-themes to the album color). A drawn tonearm
 * tracks toward the centre as the song progresses. Below: track, artist, a progress line, and
 * decorative transport controls. Idle => a quiet stationary disc.
 */
@Composable
fun VinylNowPlaying(modifier: Modifier = Modifier) {
    val vm: NowPlayingViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val ui: NowPlayingUi? = when (val s = state) {
        is TileState.Content -> s.data
        is TileState.Fallback -> s.data
        else -> null
    }
    val isPlaying = ui?.isPlaying == true
    val progress = if (ui != null && ui.durationMs > 0L) {
        (ui.progressMs.toFloat() / ui.durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        VinylDisc(
            artUrl = ui?.albumArtUrl,
            spinning = isPlaying,
            progress = progress,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f),
        )

        Spacer(Modifier.height(20.dp))
        TrackInfo(ui = ui, isPlaying = isPlaying, progress = progress)
    }
}

@Composable
private fun VinylDisc(
    artUrl: String?,
    spinning: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val c = LocalDash.current

    // Continuous rotation while playing (one turn ~9s). When paused, hold still.
    val transition = rememberInfiniteTransition(label = "vinyl")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val angle = if (spinning) spin else 0f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 1) The black vinyl disc + grooves + glowing accent ring (static).
        Canvas(Modifier.fillMaxSize()) {
            val s = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val discR = s * 0.46f
            val labelR = s * 0.205f

            // disc body with a faint sheen
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF1A1A1D), Color(0xFF060607)),
                    center = center,
                    radius = discR,
                ),
                radius = discR,
                center = center,
            )
            // concentric grooves
            var r = labelR + s * 0.02f
            while (r < discR - s * 0.01f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.035f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1f),
                )
                r += s * 0.012f
            }
            // outer rim + glowing accent ring around the label
            drawCircle(c.accent.copy(alpha = 0.18f), radius = discR, center = center, style = Stroke(width = 2f))
            drawCircle(c.accent.copy(alpha = 0.85f), radius = labelR + s * 0.012f, center = center, style = Stroke(width = s * 0.012f))
            drawCircle(c.accent.copy(alpha = 0.20f), radius = labelR + s * 0.03f, center = center, style = Stroke(width = s * 0.02f))
        }

        // 2) The album-art label, rotating.
        Box(
            Modifier
                .fillMaxSize(0.41f)
                .rotate(angle)
                .clip(CircleShape)
                .background(c.raised),
            contentAlignment = Alignment.Center,
        ) {
            if (!artUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artUrl,
                    contentDescription = "Album art",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = c.accent,
                    modifier = Modifier.fillMaxSize(0.4f),
                )
            }
        }

        // 3) Spindle hole on top of the label centre.
        Canvas(Modifier.fillMaxSize()) {
            val s = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(Color(0xFF060607), radius = s * 0.012f, center = center)
        }

        // 4) Tonearm — pivots from the top-right, head tracks inward with progress.
        Canvas(Modifier.fillMaxSize()) {
            val s = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val discR = s * 0.46f
            val labelR = s * 0.205f
            val pivot = Offset(size.width * 0.92f, size.height * 0.10f)

            // contact point: outer groove at progress 0 -> label edge at progress 1
            val contactR = androidx.compose.ui.util.lerp(discR * 0.95f, labelR + s * 0.02f, progress)
            val contactAngleRad = Math.toRadians(-58.0) // upper-right of the disc
            val contact = Offset(
                center.x + contactR * cos(contactAngleRad).toFloat(),
                center.y + contactR * sin(contactAngleRad).toFloat(),
            )
            // arm
            drawLine(
                color = Color(0xFFBFC3C7),
                start = pivot,
                end = contact,
                strokeWidth = s * 0.018f,
                cap = StrokeCap.Round,
            )
            // headshell
            drawCircle(c.accent, radius = s * 0.022f, center = contact)
            // pivot base
            drawCircle(Color(0xFF2A2D30), radius = s * 0.045f, center = pivot)
            drawCircle(c.accent.copy(alpha = 0.7f), radius = s * 0.02f, center = pivot)
        }
    }
}

@Composable
private fun TrackInfo(ui: NowPlayingUi?, isPlaying: Boolean, progress: Float) {
    val c = LocalDash.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = ui?.trackName ?: "Nothing playing",
            color = c.textHi,
            fontFamily = DisplayFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (!ui?.artists.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = ui!!.artists,
                color = c.accent,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(16.dp))
        // progress line
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(4.dp)
                .clip(CircleShape)
                .background(c.textLow.copy(alpha = 0.3f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0.001f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(c.accent),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(0.7f),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(formatMs(ui?.progressMs ?: 0L), color = c.textMid, fontFamily = DisplayFamily, fontSize = 13.sp)
            Text(formatMs(ui?.durationMs ?: 0L), color = c.textMid, fontFamily = DisplayFamily, fontSize = 13.sp)
        }
    }
}

/** mm:ss formatter for the progress readout. */
private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60L
    val s = totalSec % 60L
    return "%d:%02d".format(m, s)
}
