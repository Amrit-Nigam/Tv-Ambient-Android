package com.tvport.dashboard.ui.tiles.fifa

import android.util.Log
import com.tvport.dashboard.BuildConfig
import com.tvport.dashboard.core.Flags
import com.tvport.dashboard.data.config.AppConfig
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches the NEXT scheduled fixture from football-data.org and maps it to [FifaUi]. Builds its OWN
 * Retrofit (football-data base URL) from the shared OkHttpClient + converter factory.
 *
 * Resilience contract (BUILD SPEC §9/§16): this NEVER throws to callers and NEVER returns an empty
 * tile. If the token is empty, the request fails (incl. 403/429), or there is no upcoming fixture,
 * it returns the LABELED STATIC FALLBACK built from [AppConfig] with `isFallback = true`.
 */
@Singleton
class FifaRepository @Inject constructor(
    okHttpClient: OkHttpClient,
    converterFactory: Converter.Factory,
) {
    private val api: FifaApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(FifaApi::class.java)

    /**
     * Always returns a [FifaUi]. Real fixture when a token is set and a SCHEDULED match exists;
     * otherwise the labeled static fallback from config. Reason (for logs) is folded into Log calls.
     */
    suspend fun loadNextMatch(config: AppConfig): FifaUi {
        val token = BuildConfig.FOOTBALL_DATA_TOKEN
        if (token.isBlank()) {
            Log.i(TAG, "No FOOTBALL_DATA_TOKEN set — using labeled static fallback")
            return fallback(config)
        }
        return try {
            val resp = if (config.footballTeamId != 0) {
                api.teamMatches(token = token, teamId = config.footballTeamId)
            } else {
                api.competitionMatches(token = token, competitionCode = config.footballCompetition)
            }
            val ui = pickNext(resp)
            if (ui == null) {
                Log.w(TAG, "No upcoming SCHEDULED fixture — using labeled static fallback")
                fallback(config)
            } else {
                Log.d(TAG, "Next match: ${ui.homeTeam} v ${ui.awayTeam} @ ${ui.kickoffLocalLabel}")
                ui
            }
        } catch (e: Exception) {
            // Covers network failures and HTTP errors (403 forbidden / 429 rate-limited surface as
            // HttpException from Retrofit). Honest, labeled fallback instead of a broken tile.
            Log.w(TAG, "Fixture fetch failed (${e.message}) — using labeled static fallback", e)
            fallback(config)
        }
    }

    /** Earliest match whose kickoff is at/after now, mapped to [FifaUi]. Null if none parseable. */
    private fun pickNext(resp: MatchesResponse): FifaUi? {
        val now = System.currentTimeMillis()
        return resp.matches
            .mapNotNull { m ->
                val millis = parseUtcMillis(m.utcDate) ?: return@mapNotNull null
                m to millis
            }
            .filter { (_, millis) -> millis >= now }
            .minByOrNull { (_, millis) -> millis }
            ?.let { (m, millis) ->
                val home = m.homeTeam?.name ?: m.homeTeam?.shortName ?: "Home"
                val away = m.awayTeam?.name ?: m.awayTeam?.shortName ?: "Away"
                FifaUi(
                    homeTeam = home,
                    awayTeam = away,
                    homeFlagUrl = Flags.url(home),
                    awayFlagUrl = Flags.url(away),
                    competition = m.competition?.name,
                    kickoffMillis = millis,
                    kickoffLocalLabel = localLabel(millis),
                    isFallback = false,
                )
            }
    }

    /** The LABELED STATIC FALLBACK. Clearly a sample — surfaced with a "sample" chip in the UI. */
    private fun fallback(config: AppConfig): FifaUi {
        val millis = parseUtcMillis(config.fifaFallbackKickoffIso)
            ?: (System.currentTimeMillis() + 24L * 60 * 60 * 1000) // never null: +24h safety net
        return FifaUi(
            homeTeam = config.fifaFallbackHome,
            awayTeam = config.fifaFallbackAway,
            homeFlagUrl = Flags.url(config.fifaFallbackHome),
            awayFlagUrl = Flags.url(config.fifaFallbackAway),
            competition = "Sample fixture",
            kickoffMillis = millis,
            kickoffLocalLabel = localLabel(millis),
            isFallback = true,
        )
    }

    /**
     * Parse an ISO-8601 UTC instant (e.g. "2026-07-19T18:00:00Z") to epoch millis.
     * Uses SimpleDateFormat with a fixed UTC TimeZone so it works on minSdk 21 with no desugaring.
     * Returns null on any malformed/blank input.
     */
    private fun parseUtcMillis(iso: String?): Long? {
        if (iso.isNullOrBlank()) return null
        return try {
            utcParser().parse(iso)?.time
        } catch (e: Exception) {
            Log.w(TAG, "Unparseable utcDate: $iso")
            null
        }
    }

    /** Format epoch millis in the DEVICE-LOCAL timezone, e.g. "Sat 19 Jul, 23:30". */
    private fun localLabel(millis: Long): String =
        localFormat().format(Date(millis))

    private companion object {
        const val TAG = "Fifa"
        const val BASE_URL = "https://api.football-data.org/"

        /** UTC parser for the API's "...Z" instants. New instance per call (SDF is not thread-safe). */
        fun utcParser(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        /** Device-local display formatter. New instance per call (SDF is not thread-safe). */
        fun localFormat(): SimpleDateFormat =
            SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
    }
}
