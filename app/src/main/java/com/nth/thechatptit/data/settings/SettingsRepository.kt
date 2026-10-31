package com.nth.thechatptit.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nth.thechatptit.data.model.AppSettings
import com.nth.thechatptit.data.model.DistanceUnit
import com.nth.thechatptit.data.model.ThemeMode
import com.nth.thechatptit.data.model.WorkoutType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            distanceUnit = runCatching {
                DistanceUnit.valueOf(preferences[Keys.distanceUnit] ?: DistanceUnit.KILOMETERS.name)
            }.getOrDefault(DistanceUnit.KILOMETERS),
            themeMode = runCatching {
                ThemeMode.valueOf(preferences[Keys.themeMode] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            defaultWorkoutType = runCatching {
                WorkoutType.valueOf(preferences[Keys.defaultWorkoutType] ?: WorkoutType.RUNNING.name)
            }.getOrDefault(WorkoutType.RUNNING),
            autoPauseEnabled = preferences[Keys.autoPauseEnabled] ?: false
        )
    }

    suspend fun setAutoPauseEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.autoPauseEnabled] = enabled }
    }

    suspend fun setDistanceUnit(unit: DistanceUnit) {
        dataStore.edit { it[Keys.distanceUnit] = unit.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDefaultWorkoutType(type: WorkoutType) {
        dataStore.edit { it[Keys.defaultWorkoutType] = type.name }
    }

    private object Keys {
        val distanceUnit = stringPreferencesKey("distance_unit")
        val themeMode = stringPreferencesKey("theme_mode")
        val defaultWorkoutType = stringPreferencesKey("default_workout_type")
        val autoPauseEnabled = booleanPreferencesKey("auto_pause_enabled")
    }
}
