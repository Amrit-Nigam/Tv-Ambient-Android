package com.tvport.dashboard

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
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

    // Keeps the Wi-Fi radio out of power-save while the dashboard is up, so the long-lived SSE
    // connection delivers Claude pushes instantly instead of being batched until the next beacon
    // (the cause of the occasional 5–10s lag). Held only while the activity is in the foreground.
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Never let the panel sleep — this is an always-on dashboard.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiManager.WIFI_MODE_FULL_LOW_LATENCY
        } else {
            @Suppress("DEPRECATION")
            WifiManager.WIFI_MODE_FULL_HIGH_PERF
        }
        wifiLock = wifi.createWifiLock(mode, "tvport:sse").apply {
            setReferenceCounted(false)
            runCatching { acquire() }
        }

        setContent {
            DashboardScreen()
        }
    }

    override fun onDestroy() {
        runCatching { wifiLock?.takeIf { it.isHeld }?.release() }
        super.onDestroy()
    }
}
