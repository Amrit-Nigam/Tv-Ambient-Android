package com.tvport.dashboard.ui.tiles.claude

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
 * Fetches the Claude statusbar state from the LAN server (serve.py) over plain OkHttp — the
 * endpoint is a full URL from BuildConfig, so no Retrofit base-URL juggling. Never throws:
 * blank URL -> Idle("not configured"); network/parse failure -> Fallback (laptop asleep/off LAN).
 */
@Singleton
class ClaudeRepository @Inject constructor(
    private val client: OkHttpClient,
    private val json: Json,
) {
    private val url: String = BuildConfig.CLAUDE_STATUS_URL

    suspend fun fetch(last: ClaudeUi?): TileState<ClaudeUi> {
        if (url.isBlank()) return TileState.Idle("Claude status not configured")
        return withContext(Dispatchers.IO) {
            try {
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    val body = resp.body?.string()
                    if (!resp.isSuccessful || body.isNullOrBlank()) {
                        return@use TileState.Fallback("HTTP ${resp.code}", last)
                    }
                    val dto = json.decodeFromString(ClaudeStateDto.serializer(), body)
                    TileState.Content(dto.toUi())
                }
            } catch (e: Exception) {
                // Laptop off / asleep / not on the LAN — show last known, labeled offline.
                Log.d(TAG, "Claude status fetch failed: ${e.message}")
                TileState.Fallback("offline", last)
            }
        }
    }

    private companion object { const val TAG = "ClaudeTile" }
}
