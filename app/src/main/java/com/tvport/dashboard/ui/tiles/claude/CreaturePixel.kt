package com.tvport.dashboard.ui.tiles.claude

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * The Claude creature ("clawd"), rendered from the animated GIFs in `assets/clawd/`. The clip is
 * chosen from Claude's live state: working while busy, happy when a turn finishes, alert when it
 * needs you, asleep when idle/offline. Decoded by the app-wide Coil ImageLoader (see DashboardApp).
 */
@Composable
fun Creature(kind: ClaudeKind, busy: Boolean, modifier: Modifier = Modifier) {
    val asset = when {
        busy -> "working"
        kind == ClaudeKind.DONE -> "happy"
        kind == ClaudeKind.WAITING || kind == ClaudeKind.PERMISSION -> "alert"
        else -> "sleep" // idle / offline
    }
    val context = LocalContext.current

    // Crossfade between clips when the state flips so the swap doesn't pop.
    Crossfade(targetState = asset, animationSpec = tween(350), label = "clawd") { name ->
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/clawd/$name.gif")
                .crossfade(false)
                .build(),
            contentDescription = "Claude creature ($name)",
            contentScale = ContentScale.Fit,
            modifier = modifier,
        )
    }
}
