package com.tvport.dashboard.ui.tiles.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tvport.dashboard.core.TileState
import com.tvport.dashboard.ui.dashboard.TileCard
import com.tvport.dashboard.ui.dashboard.TileHeader
import com.tvport.dashboard.ui.dashboard.TileQuietMessage
import com.tvport.dashboard.ui.theme.BodyFamily
import com.tvport.dashboard.ui.theme.DisplayFamily
import com.tvport.dashboard.ui.theme.LocalDash

/**
 * NOW PLAYING tile. Real Spotify Web API data (PRIMARY) with a best-effort local
 * MediaSession fallback (SECONDARY). Album art, track + artist, and an accent progress bar
 * that advances every second between polls. Idle => a tasteful "Nothing playing"; on API
 * failure the last track is shown faded rather than an error.
 */
@Composable
fun NowPlayingTile(modifier: Modifier = Modifier) {
    val vm: NowPlayingViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    TileCard(modifier) {
        Column(Modifier.fillMaxSize()) {
            TileHeader(label = "Now Playing", icon = Icons.Filled.MusicNote)
            Spacer(Modifier.height(12.dp))

            when (val s = state) {
                is TileState.Content -> NowPlayingBody(s.data, faded = false)
                is TileState.Fallback -> {
                    val data = s.data
                    if (data != null) NowPlayingBody(data, faded = true)
                    else TileQuietMessage("Now Playing unavailable")
                }
                is TileState.Idle -> TileQuietMessage(s.message)
                TileState.Loading -> TileQuietMessage("Connecting…")
            }
        }
    }
}

@Composable
private fun NowPlayingBody(ui: NowPlayingUi, faded: Boolean) {
    val c = LocalDash.current

    Column(Modifier.fillMaxSize().alpha(if (faded) 0.45f else 1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlbumArt(url = ui.albumArtUrl)
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = ui.trackName,
                    color = c.textHi,
                    fontFamily = DisplayFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (ui.artists.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = ui.artists,
                        color = c.textMid,
                        fontFamily = BodyFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))
        ProgressBar(progressMs = ui.progressMs, durationMs = ui.durationMs)

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatMs(ui.progressMs),
                color = c.textMid,
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
            Text(
                text = if (faded) "${ui.source} · offline" else ui.source,
                color = c.textLow,
                fontFamily = BodyFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
            )
            Text(
                text = formatMs(ui.durationMs),
                color = c.textMid,
                fontFamily = DisplayFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun AlbumArt(url: String?) {
    val c = LocalDash.current
    val shape = RoundedCornerShape(14.dp)
    Box(
        Modifier
            .size(96.dp)
            .clip(shape)
            .background(c.raised),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Album art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = c.accent,
                modifier = Modifier.size(44.dp),
            )
        }
    }
}

@Composable
private fun ProgressBar(progressMs: Long, durationMs: Long) {
    val c = LocalDash.current
    val fraction = if (durationMs > 0L) {
        (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(c.border),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(c.accent),
        )
    }
}

/** Milliseconds -> m:ss (or h:mm:ss for long tracks). */
private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
