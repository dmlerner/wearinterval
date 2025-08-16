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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
    
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
    
    val notificationSettings: Flow<NotificationSettings> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            NotificationSettings(
                vibrationEnabled = preferences[VIBRATION_ENABLED] ?: true,
                soundEnabled = preferences[SOUND_ENABLED] ?: true,
                flashEnabled = preferences[FLASH_ENABLED] ?: true,
                autoMode = preferences[AUTO_MODE] ?: true
            )
        }
    
    val currentConfiguration: Flow<TimerConfiguration> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            TimerConfiguration(
                id = preferences[CURRENT_CONFIG_ID] ?: TimerConfiguration.DEFAULT.id,
                laps = preferences[CURRENT_CONFIG_LAPS] ?: TimerConfiguration.DEFAULT.laps,
                workDuration = (preferences[CURRENT_CONFIG_WORK_DURATION] ?: TimerConfiguration.DEFAULT.workDuration.inWholeSeconds).seconds,
                restDuration = (preferences[CURRENT_CONFIG_REST_DURATION] ?: TimerConfiguration.DEFAULT.restDuration.inWholeSeconds).seconds,
                lastUsed = preferences[CURRENT_CONFIG_LAST_USED] ?: TimerConfiguration.DEFAULT.lastUsed
            )
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
        context.dataStore.edit { preferences ->
            preferences[CURRENT_CONFIG_ID] = config.id
            preferences[CURRENT_CONFIG_LAPS] = config.laps
            preferences[CURRENT_CONFIG_WORK_DURATION] = config.workDuration.inWholeSeconds
            preferences[CURRENT_CONFIG_REST_DURATION] = config.restDuration.inWholeSeconds
            preferences[CURRENT_CONFIG_LAST_USED] = config.lastUsed
        }
    }
}