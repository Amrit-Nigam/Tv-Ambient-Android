package com.tvport.dashboard.ui.tiles.battery

import android.util.Log
import com.tvport.dashboard.BuildConfig
import com.tvport.dashboard.core.TileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls the Mac status server's /battery endpoint (same host as the Claude tile) for this Mac's
 * battery plus the iPhone's last-reported level. Reuses [BuildConfig.CLAUDE_STATUS_URL] so there's
 * no extra secret to configure. Never throws: a dead link keeps the last good reading via Fallback.
 */
@Singleton
class BatteryRepository @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val url: String = run {
        val raw = BuildConfig.CLAUDE_STATUS_URL.trim().trimEnd('/')
        val base = if (raw.endsWith("/status")) raw.removeSuffix("/status") else raw
        if (base.isBlank()) "" else "$base/battery"
    }

    @Volatile private var lastGood: BatteryUi? = null

    suspend fun load(): TileState<BatteryUi> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext TileState.Idle("Battery not configured")
        try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext TileState.Fallback("HTTP ${resp.code}", lastGood)
                }
                val body = resp.body?.string().orEmpty()
                val ui = json.decodeFromString<BatteryDto>(body).toUi()
                lastGood = ui
                TileState.Content(ui)
            }
        } catch (e: Exception) {
            Log.w(TAG, "battery fetch failed (${e.message}) — showing last known")
            TileState.Fallback("battery unavailable", lastGood)
        }
    }

    private companion object {
        const val TAG = "BatteryRepository"
    }
}
