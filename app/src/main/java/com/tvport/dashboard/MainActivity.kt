package com.tvport.dashboard

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.tvport.dashboard.ui.dashboard.DashboardScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Keeps the screen on for 24/7 wall display and draws edge-to-edge.
 * All UI lives in Compose under [DashboardScreen].
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Never let the panel sleep — this is an always-on dashboard (BUILD SPEC §10).
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            DashboardScreen()
        }
    }
}
