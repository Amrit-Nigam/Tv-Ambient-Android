package com.tvport.dashboard.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.tvport.dashboard.ui.dim.BurnInDimSurface
import com.tvport.dashboard.ui.dim.rememberDimState
import com.tvport.dashboard.ui.theme.DashTheme
import com.tvport.dashboard.ui.theme.Dimens
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.theme.withAlbumScheme
import com.tvport.dashboard.ui.tiles.claude.ClaudeBar
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
    val dim = rememberDimState(cfg)

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
                if (!artUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(1.2f)       // overscan so blur has no hard edges
                            .blur(50.dp)
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
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(Dimens.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                        ) {
                            // LEFT: the vinyl hero
                            VinylNowPlaying(
                                Modifier
                                    .weight(1.25f)
                                    .fillMaxHeight(),
                            )

                            // RIGHT: clock + the two sports tiles
                            Column(
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                            ) {
                                ClockTile()
                                FifaTile(Modifier.fillMaxWidth().weight(1f))
                                F1Tile(Modifier.fillMaxWidth().weight(1f))
                            }
                        }

                        // BOTTOM: live Claude terminal status bar (full width)
                        ClaudeBar(
                            Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                        )
                    }
                }
            }
        }
    }
}
