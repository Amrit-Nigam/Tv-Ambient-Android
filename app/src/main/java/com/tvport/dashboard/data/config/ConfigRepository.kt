package com.tvport.dashboard.data.config

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "tvport_config")

/**
 * Reads/writes the [AppConfig]. Any persisted override falls back to the AppConfig default,
 * so a fresh install runs entirely on defaults (Kharghar location, 24h, night 22:00-07:00).
 */
@Singleton
class ConfigRepository @Inject constructor(
    private val context: Context,
) {
    private object Keys {
        val lat = doublePreferencesKey("lat")
        val lon = doublePreferencesKey("lon")
        val locLabel = stringPreferencesKey("loc_label")
        val metric = booleanPreferencesKey("metric")
        val use24 = booleanPreferencesKey("use_24h")
        val nightStart = intPreferencesKey("night_start")
        val nightEnd = intPreferencesKey("night_end")
        val nightDim = floatPreferencesKey("night_dim")
        val dayDim = floatPreferencesKey("day_dim")
        val pixelShift = booleanPreferencesKey("pixel_shift")
        val competition = stringPreferencesKey("football_comp")
        val teamId = intPreferencesKey("football_team")
    }

    /** Emits the current config, re-emitting whenever a value changes. */
    val config: Flow<AppConfig> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw e }
        .map { prefs ->
            val d = AppConfig()
            d.copy(
                latitude = prefs[Keys.lat] ?: d.latitude,
                longitude = prefs[Keys.lon] ?: d.longitude,
                locationLabel = prefs[Keys.locLabel] ?: d.locationLabel,
                metric = prefs[Keys.metric] ?: d.metric,
                use24Hour = prefs[Keys.use24] ?: d.use24Hour,
                nightStartHour = prefs[Keys.nightStart] ?: d.nightStartHour,
                nightEndHour = prefs[Keys.nightEnd] ?: d.nightEndHour,
                nightDimLevel = prefs[Keys.nightDim] ?: d.nightDimLevel,
                dayDimLevel = prefs[Keys.dayDim] ?: d.dayDimLevel,
                pixelShiftEnabled = prefs[Keys.pixelShift] ?: d.pixelShiftEnabled,
                footballCompetition = prefs[Keys.competition] ?: d.footballCompetition,
                footballTeamId = prefs[Keys.teamId] ?: d.footballTeamId,
            )
        }

    suspend fun setLocation(lat: Double, lon: Double, label: String) {
        context.dataStore.edit {
            it[Keys.lat] = lat; it[Keys.lon] = lon; it[Keys.locLabel] = label
        }
    }

    suspend fun setNightSchedule(startHour: Int, endHour: Int, dim: Float) {
        context.dataStore.edit {
            it[Keys.nightStart] = startHour; it[Keys.nightEnd] = endHour; it[Keys.nightDim] = dim
        }
    }
}
