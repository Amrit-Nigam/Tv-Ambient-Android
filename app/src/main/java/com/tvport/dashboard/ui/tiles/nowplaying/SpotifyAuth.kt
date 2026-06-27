package com.tvport.dashboard.ui.tiles.nowplaying

import android.util.Base64
import android.util.Log
import com.tvport.dashboard.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Spotify token manager (PRIMARY auth for the Now Playing tile).
 *
 * Exchanges the long-lived refresh token (BuildConfig secret) for a short-lived access
 * token via the client-credentials Basic header, caches it, and refreshes ~60s before
 * expiry. Thread-safe via a [Mutex]; [getAccessToken] returns null on any failure so the
 * repository can fall back gracefully instead of throwing.
 */
@Singleton
class SpotifyAuth @Inject constructor(
    okHttpClient: OkHttpClient,
    converterFactory: Converter.Factory,
) {
    private val api: SpotifyAccountsApi = Retrofit.Builder()
        .baseUrl("https://accounts.spotify.com/")
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(SpotifyAccountsApi::class.java)

    private val mutex = Mutex()

    @Volatile private var cachedToken: String? = null
    @Volatile private var expiresAtMs: Long = 0L

    /** Basic base64("clientId:clientSecret") header, computed once. */
    private val basicAuthHeader: String by lazy {
        val raw = "${BuildConfig.SPOTIFY_CLIENT_ID}:${BuildConfig.SPOTIFY_CLIENT_SECRET}"
        "Basic " + Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Returns a valid access token, refreshing if missing/near-expiry. Returns null if the
     * refresh call fails (network down, bad creds) — callers must treat null as a fallback.
     */
    suspend fun getAccessToken(): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = cachedToken
        if (current != null && now < expiresAtMs - REFRESH_SKEW_MS) {
            return current
        }
        return try {
            val resp = api.refreshToken(
                basicAuth = basicAuthHeader,
                refreshToken = BuildConfig.SPOTIFY_REFRESH_TOKEN,
            )
            val token = resp.access_token
            if (token.isNullOrBlank()) {
                Log.w(TAG, "Token refresh returned no access_token")
                null
            } else {
                cachedToken = token
                expiresAtMs = now + resp.expires_in * 1000L
                Log.d(TAG, "Refreshed Spotify access token (expires in ${resp.expires_in}s)")
                token
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Spotify token refresh failed: ${t.message}")
            null
        }
    }

    private companion object {
        const val TAG = "NowPlaying"
        const val REFRESH_SKEW_MS = 60_000L
    }
}
