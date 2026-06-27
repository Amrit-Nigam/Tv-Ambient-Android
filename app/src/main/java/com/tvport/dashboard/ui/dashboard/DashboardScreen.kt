package com.tvport.dashboard.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tvport.dashboard.ui.dim.BurnInDimSurface
import com.tvport.dashboard.ui.dim.rememberDimState
import com.tvport.dashboard.ui.theme.DashTheme
import com.tvport.dashboard.ui.theme.Dimens
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.theme.withAlbumScheme
import com.tvport.dashboard.ui.tiles.claude.ClaudeTamagotchi
import com.tvport.dashboard.ui.tiles.clock.ClockTile
import com.tvport.dashboard.ui.tiles.f1.F1Tile
import com.tvport.dashboard.ui.tiles.fifa.FifaTile
import com.tvport.dashboard.ui.tiles.nowplaying.VinylNowPlaying
import com.tvport.dashboard.ui.visualizer.VisualizerBackground

/**
 * The single dashboard screen, re-themed live to the album art:
 *   1. Album-color background gradient + full-bleed reactive visualizer (tinted by the accent)
 *   2. Pixel-shift + night-dim surface wrapping everything
 *   3. Left: spinning VINYL Now Playing hero. Right: clock + Next Match + Next Race.
 *
 * All accents/backgrounds animate toward the current album scheme, so changing track recolors the
 * whole page smoothly.
 */
@Composable
fun DashboardScreen() {
    val vm: DashboardViewModel = hiltViewModel()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val scheme by vm.albumScheme.collectAsStateWithLifecycle()
    val artUrl by vm.albumArtUrl.collectAsStateWithLifecycle()
    // Night dim is now MANUAL (toggled by the on-screen remote control), not time-based.
    var night by rememberSaveable { mutableStateOf(false) }
    val dim = rememberDimState(cfg, night)

    DashTheme(isNight = dim.isNight) {
        // Blend the album scheme into the base palette, then animate each channel for smooth swaps.
        val target = LocalDash.current.withAlbumScheme(scheme)
        val accent by animateColorAsState(target.accent, tween(900), label = "accent")
        val accent2 by animateColorAsState(target.accent2, tween(900), label = "accent2")
        val bg by animateColorAsState(target.bg, tween(900), label = "bg")
        val raised by animateColorAsState(target.raised, tween(900), label = "raised")
        val border by animateColorAsState(target.border, tween(900), label = "border")
        val dynamic = target.copy(accent = accent, accent2 = accent2, bg = bg, raised = raised, border = border)

        CompositionLocalProvider(LocalDash provides dynamic) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                // (1a) Ambient visualizer at the very back (uses the album accent).
                VisualizerBackground(
                    modifier = Modifier.fillMaxSize(),
                    isNight = dim.isNight,
                )

                // (1b) The album cover IS the page background — blurred big, faded.
                // Decode it TINY (32px) and let Crop upscale it: that upscaling IS the blur, and it
                // works on every Android version. Modifier.blur only exists on API 31+ (the BRAVIA is
                // API 30), so we can't rely on it — we add it on top purely as extra smoothing where
                // it's supported.
                if (!artUrl.isNullOrBlank()) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(ctx)
                            .data(artUrl)
                            .size(32)          // decode a 32px thumbnail → upscaling blurs it
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)       // overscan so edges have no hard seam
                            .blur(50.dp)       // extra smoothing on API 31+, ignored on API 30
                            .alpha(0.85f),
                    )
                }
                // (1c) Faded AURA: a soft accent glow in the centre fading to a dark vignette at the
                // edges, so the cover melts into the frame rather than ending in a hard rectangle.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.0f to accent.copy(alpha = 0.12f),
                                    0.42f to Color.Transparent,
                                    0.78f to Color.Black.copy(alpha = 0.42f),
                                    1.0f to Color.Black.copy(alpha = 0.82f),
                                ),
                                radius = 1500f,
                            )
                        )
                )
                // (1d) Gentle veil for text legibility over the brighter cover.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.22f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.40f),
                                ),
                            )
                        )
                )

                // (2) Burn-in pixel-shift + night scrim wrap the content layer.
                BurnInDimSurface(state = dim) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenPadding),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                    ) {
                        // LEFT: the vinyl hero
                        VinylNowPlaying(
                            Modifier
                                .weight(1.25f)
                                .fillMaxHeight(),
                        )

                        // RIGHT: clock → CLAWD tamagotchi hero → compact match/race strip
                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                        ) {
                            ClockTile()
                            ClaudeTamagotchi(Modifier.fillMaxWidth().weight(1.0f))
                            Row(
                                Modifier.fillMaxWidth().weight(1.15f),
                                horizontalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                            ) {
                                FifaTile(Modifier.weight(1f).fillMaxHeight())
                                F1Tile(Modifier.weight(1f).fillMaxHeight())
                            }
                        }
                    }
                }

                // Remote-focusable night toggle, floating above everything (incl. the dim scrim).
                NightToggle(
                    night = night,
                    onToggle = { night = !night },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Dimens.screenPadding),
                )
            }
        }
    }
}

/**
 * A small circular control that toggles night dim. It is the only focusable element on the
 * dashboard, so it grabs focus on launch — press the D-pad center on the remote to flip dim on/off.
 * Shows a moon when bright (tap to dim) and a sun when dimmed (tap to brighten); the accent ring
 * appears while focused so it's obvious it's selected.
 */
@Composable
private fun NightToggle(night: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalDash.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    // Grab focus on launch so the remote's OK button toggles immediately. requestFocus() can no-op
    // if the node isn't placed yet, so retry a few frames until it sticks.
    LaunchedEffect(Unit) {
        repeat(10) {
            try {
                focusRequester.requestFocus()
                return@LaunchedEffect
            } catch (_: Throwable) {
            }
            delay(80)
        }
    }
    val scale by animateFloatAsState(if (focused) 1.12f else 1f, label = "toggleScale")

    Box(
        modifier
            .size(54.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(c.raised.copy(alpha = if (focused) 0.95f else 0.6f))
            .border(if (focused) 3.dp else 1.dp, if (focused) c.accent else c.border, CircleShape)
            .focusRequester(focusRequester)
            .focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null) { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (night) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            contentDescription = if (night) "Turn dim off" else "Turn dim on",
            tint = if (focused) c.accent else c.textMid,
            modifier = Modifier.size(26.dp),
        )
    }
}
