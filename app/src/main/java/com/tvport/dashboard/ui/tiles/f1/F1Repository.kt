package com.tvport.dashboard.ui.tiles.f1

import android.util.Log
import com.tvport.dashboard.core.TileState
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
 * Fetches the NEXT Formula 1 race from Jolpica-F1 (the Ergast mirror the portfolio used — free,
 * no API key) and maps it to [F1Ui]. Builds its own Retrofit from the shared OkHttpClient +
 * converter factory.
 *
 * Resilience (BUILD SPEC §11/§16): never throws. Off-season / empty schedule -> Idle; network or
 * parse failure -> Fallback carrying the last good race so the tile keeps looking alive.
 */
@Singleton
class F1Repository @Inject constructor(
    okHttpClient: OkHttpClient,
    converterFactory: Converter.Factory,
) {
    private val api: F1Api = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(converterFactory)
        .build()
        .create(F1Api::class.java)

    @Volatile private var lastGood: F1Ui? = null

    suspend fun loadNextRace(): TileState<F1Ui> {
        return try {
            val resp = api.nextRace()
            val race = resp.mrData.raceTable.races.firstOrNull()
            if (race == null) {
                Log.i(TAG, "No upcoming race in current season (off-season)")
                TileState.Idle("No upcoming race")
            } else {
                val ui = map(race)
                lastGood = ui
                Log.d(TAG, "Next race: ${ui.raceName} @ ${ui.localLabel}")
                TileState.Content(ui)
            }
        } catch (e: Exception) {
            // Network down / HTTP error / malformed JSON — keep the last good race if we have one.
            Log.w(TAG, "F1 fetch failed (${e.message}) — showing last known", e)
            TileState.Fallback("F1 schedule unavailable", lastGood)
        }
    }

    private fun map(r: RaceDto): F1Ui {
        val millis = parseRaceMillis(r.date, r.time) ?: System.currentTimeMillis()
        val loc = listOfNotNull(r.circuit.location.locality, r.circuit.location.country)
            .joinToString(", ")
        return F1Ui(
            raceName = r.raceName,
            circuitName = r.circuit.circuitName,
            location = loc,
            round = r.round?.let { "Round $it" },
            raceStartMillis = millis,
            localLabel = localFormat().format(Date(millis)),
        )
    }

    /**
     * Ergast splits the start into date ("2026-06-28") + time ("13:00:00Z"). Combine into a UTC
     * instant. Missing time defaults to midnight UTC. minSdk-21 safe (SimpleDateFormat, no desugaring).
     */
    private fun parseRaceMillis(date: String?, time: String?): Long? {
        if (date.isNullOrBlank()) return null
        val t = (time ?: "00:00:00Z").removeSuffix("Z")
        return try {
            utcParser().parse("${date}T${t}Z")?.time
        } catch (e: Exception) {
            Log.w(TAG, "Unparseable race date/time: $date $time")
            null
        }
    }

    private companion object {
        const val TAG = "F1"
        const val BASE_URL = "https://api.jolpi.ca/ergast/f1/"

        fun utcParser(): SimpleDateFormat =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

        fun localFormat(): SimpleDateFormat =
            SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault())
    }
}
