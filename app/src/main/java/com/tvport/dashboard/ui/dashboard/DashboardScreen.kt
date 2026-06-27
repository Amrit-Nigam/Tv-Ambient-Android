package com.tvport.dashboard.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tvport.dashboard.ui.dim.BurnInDimSurface
import com.tvport.dashboard.ui.dim.rememberDimState
import com.tvport.dashboard.ui.theme.DashTheme
import com.tvport.dashboard.ui.theme.Dimens
import com.tvport.dashboard.ui.theme.LocalDash
import com.tvport.dashboard.ui.tiles.clock.ClockTile
import com.tvport.dashboard.ui.tiles.fifa.FifaTile
import com.tvport.dashboard.ui.tiles.nowplaying.NowPlayingTile
import com.tvport.dashboard.ui.visualizer.VisualizerBackground

/**
 * The single dashboard screen. Layered:
 *   1. Visualizer (full-bleed animated background)
 *   2. Pixel-shift + night-dim surface wrapping everything
 *   3. Clock anchor (top-left) + tile row: Now Playing | Next Match
 */
@Composable
fun DashboardScreen() {
    val vm: DashboardViewModel = hiltViewModel()
    val cfg by vm.config.collectAsStateWithLifecycle()
    val dim = rememberDimState(cfg)

    DashTheme(isNight = dim.isNight) {
        val c = LocalDash.current
        Box(Modifier.fillMaxSize().background(c.bg)) {
            // (1) Ambient visualizer behind everything, dimmed.
            VisualizerBackground(
                modifier = Modifier.fillMaxSize(),
                isNight = dim.isNight,
            )

            // (2) Burn-in pixel-shift + night scrim wrap the content layer.
            BurnInDimSurface(state = dim) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(Dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                ) {
                    // Clock anchor
                    ClockTile()

                    // Tile row: Now Playing | Next Match — takes the bulk of the screen
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.tileGap),
                    ) {
                        NowPlayingTile(Modifier.weight(1.6f).fillMaxHeight())
                        FifaTile(Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}
