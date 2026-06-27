package com.tvport.dashboard.ui.tiles.claude

import android.util.Log
import com.tvport.dashboard.BuildConfig
import com.tvport.dashboard.core.TileState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PUSH model: the Mac server streams Claude's state over Server-Sent Events; the TV holds one
 * long-lived connection and receives an update on every lifecycle transition (new prompt, tool,
 * needs-input, finished, stopped) — it never polls. On disconnect it shows "offline" and
 * auto-reconnects with backoff. Blank URL -> a one-shot Idle.
 */
@Singleton
class ClaudeRepository @Inject constructor(
    client: OkHttpClient,
    private val json: Json,
) {
    private val eventsUrl: String = run {
        val raw = BuildConfig.CLAUDE_STATUS_URL.trim().trimEnd('/')
        val base = if (raw.endsWith("/status")) raw.removeSuffix("/status") else raw
        if (base.isBlank()) "" else "$base/events"
    }

    // The server sends an SSE heartbeat (": ping") every ~2s, so a healthy stream always has bytes
    // arriving. We therefore use a FINITE read timeout (not 0): if nothing — not even a heartbeat —
    // arrives for this long, the link is silently dead (laptop asleep / Wi-Fi dropped / NAT timed
    // out the flow without a FIN). OkHttp then fires onFailure and the reconnect loop recovers and
    // shows "offline", instead of sitting forever on a stale "online" state.
    private val sseClient: OkHttpClient = client.newBuilder()
        .readTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun stream(): Flow<TileState<ClaudeUi>> = channelFlow {
        if (eventsUrl.isBlank()) {
            trySend(TileState.Idle("Claude status not configured"))
            awaitClose { }
            return@channelFlow
        }
        val factory = EventSources.createFactory(sseClient)
        var last: ClaudeUi? = null

        while (isActive) {
            val ended = CompletableDeferred<Unit>()
            val listener = object : EventSourceListener() {
                override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                    try {
                        val ui = json.decodeFromString(ClaudeStateDto.serializer(), data).toUi()
                        last = ui
                        trySend(TileState.Content(ui))
                    } catch (e: Exception) {
                        Log.d(TAG, "bad event: ${e.message}")
                    }
                }

                override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                    Log.d(TAG, "SSE failure: ${t?.message}")
                    trySend(TileState.Fallback("offline", last))
                    if (!ended.isCompleted) ended.complete(Unit)
                }

                override fun onClosed(es: EventSource) {
                    if (!ended.isCompleted) ended.complete(Unit)
                }
            }
            val es = factory.newEventSource(Request.Builder().url(eventsUrl).build(), listener)
            try {
                ended.await()
            } finally {
                es.cancel()
            }
            if (isActive) delay(1000) // reconnect backoff (fast recovery)
        }
        awaitClose { }
    }

    private companion object { const val TAG = "ClaudeTile" }
}
