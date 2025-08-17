package com.wearinterval.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
open class DataStoreManager
@Inject
constructor(
  @ApplicationContext private val context: Context,
) {
  private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

  // Expose dataStore for testing
  open val dataStore: DataStore<Preferences>
    get() = context.dataStore

  companion object {
    // NotificationSettings keys
    private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val FLASH_ENABLED = booleanPreferencesKey("flash_enabled")
    private val AUTO_MODE = booleanPreferencesKey("auto_mode")

    // Current configuration keys
    private val CURRENT_CONFIG_ID = stringPreferencesKey("current_config_id")
    private val CURRENT_CONFIG_LAPS = intPreferencesKey("current_config_laps")
    private val CURRENT_CONFIG_WORK_DURATION = longPreferencesKey("current_config_work_duration")
    private val CURRENT_CONFIG_REST_DURATION = longPreferencesKey("current_config_rest_duration")
    private val CURRENT_CONFIG_LAST_USED = longPreferencesKey("current_config_last_used")
  }

  val notificationSettings: Flow<NotificationSettings> =
    context.dataStore.data
      .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
      .map { preferences ->
        NotificationSettings(
          vibrationEnabled = preferences[VIBRATION_ENABLED]
              ?: NotificationSettings.DEFAULT.vibrationEnabled,
          soundEnabled = preferences[SOUND_ENABLED] ?: NotificationSettings.DEFAULT.soundEnabled,
          flashEnabled = preferences[FLASH_ENABLED] ?: NotificationSettings.DEFAULT.flashEnabled,
          autoMode = preferences[AUTO_MODE] ?: NotificationSettings.DEFAULT.autoMode,
        )
      }

  val currentConfiguration: Flow<TimerConfiguration> =
    context.dataStore.data
      .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
      .map { preferences ->
        val configId = preferences[CURRENT_CONFIG_ID]
        val rawLaps = preferences[CURRENT_CONFIG_LAPS]
        val rawWork = preferences[CURRENT_CONFIG_WORK_DURATION]
        val rawRest = preferences[CURRENT_CONFIG_REST_DURATION]
        android.util.Log.d(
          "DataStore",
          "RAW preferences - ID: $configId, laps: $rawLaps, work: $rawWork, rest: $rawRest"
        )

        if (configId == null) {
          android.util.Log.d(
            "DataStore",
            "No config ID, returning DEFAULT: ${TimerConfiguration.DEFAULT}"
          )
          TimerConfiguration.DEFAULT
        } else {
          val config =
            TimerConfiguration(
              id = configId,
              laps = rawLaps ?: TimerConfiguration.DEFAULT.laps,
              workDuration =
                (rawWork ?: TimerConfiguration.DEFAULT.workDuration.inWholeSeconds).seconds,
              restDuration =
                (rawRest ?: TimerConfiguration.DEFAULT.restDuration.inWholeSeconds).seconds,
              lastUsed = preferences[CURRENT_CONFIG_LAST_USED]
                  ?: TimerConfiguration.DEFAULT.lastUsed,
            )
          android.util.Log.d("DataStore", "Constructed config: $config")
          config
        }
      }

  suspend fun updateNotificationSettings(settings: NotificationSettings) {
    context.dataStore.edit { preferences ->
      preferences[VIBRATION_ENABLED] = settings.vibrationEnabled
      preferences[SOUND_ENABLED] = settings.soundEnabled
      preferences[FLASH_ENABLED] = settings.flashEnabled
      preferences[AUTO_MODE] = settings.autoMode
    }
  }

  suspend fun updateCurrentConfiguration(config: TimerConfiguration) {
    android.util.Log.d("DataStore", "UPDATING config: $config")
    context.dataStore.edit { preferences ->
      preferences[CURRENT_CONFIG_ID] = config.id
      preferences[CURRENT_CONFIG_LAPS] = config.laps
      preferences[CURRENT_CONFIG_WORK_DURATION] = config.workDuration.inWholeSeconds
      preferences[CURRENT_CONFIG_REST_DURATION] = config.restDuration.inWholeSeconds
      preferences[CURRENT_CONFIG_LAST_USED] = config.lastUsed
      android.util.Log.d(
        "DataStore",
        "SAVED to preferences - laps: ${config.laps}, work: ${config.workDuration.inWholeSeconds}, rest: ${config.restDuration.inWholeSeconds}"
      )
    }
  }

  suspend fun clearAllData() {
    android.util.Log.d("DataStore", "CLEARING all DataStore data")
    context.dataStore.edit { preferences -> preferences.clear() }
  }
}
