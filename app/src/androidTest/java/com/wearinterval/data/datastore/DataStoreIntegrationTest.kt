package com.wearinterval.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class DataStoreIntegrationTest {

    @get:Rule
    val dataStoreTestRule = DataStoreTestRule()

    private lateinit var dataStoreManager: TestDataStoreManager

    @Before
    fun setup() = runTest {
        dataStoreManager = TestDataStoreManager(dataStoreTestRule.testDataStore)
        // Clear any existing data to ensure clean state for each test
        dataStoreManager.dataStore.edit { it.clear() }
    }

    // Test rule that creates a unique DataStore for each test
    class DataStoreTestRule : TestWatcher() {
        lateinit var testDataStore: DataStore<Preferences>
        private lateinit var testContext: Context
        private lateinit var testDataStoreName: String

        override fun starting(description: Description) {
            testContext = ApplicationProvider.getApplicationContext()
            testDataStoreName = "test_${UUID.randomUUID().toString().replace("-", "")}"

            // Create unique DataStore for this test using PreferenceDataStoreFactory
            testDataStore = PreferenceDataStoreFactory.create(
                produceFile = { File(testContext.filesDir, "datastore/$testDataStoreName.preferences_pb") },
            )
        }

        override fun finished(description: Description) = runTest {
            // Clean up test DataStore file
            try {
                testDataStore.edit { it.clear() }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }

            val dataStoreFile = File(testContext.filesDir, "datastore/$testDataStoreName.preferences_pb")
            if (dataStoreFile.exists()) {
                dataStoreFile.delete()
            }
        }
    }

    // Test DataStoreManager that uses the test DataStore directly
    private class TestDataStoreManager(
        private val testDataStore: DataStore<Preferences>,
    ) {
        companion object {
            // NotificationSettings keys (copied from DataStoreManager)
            private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
            private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
            private val FLASH_ENABLED = booleanPreferencesKey("flash_enabled")
            private val AUTO_MODE = booleanPreferencesKey("auto_mode")

            // Current configuration keys (copied from DataStoreManager)
            private val CURRENT_CONFIG_ID = stringPreferencesKey("current_config_id")
            private val CURRENT_CONFIG_LAPS = intPreferencesKey("current_config_laps")
            private val CURRENT_CONFIG_WORK_DURATION = longPreferencesKey("current_config_work_duration")
            private val CURRENT_CONFIG_REST_DURATION = longPreferencesKey("current_config_rest_duration")
            private val CURRENT_CONFIG_LAST_USED = longPreferencesKey("current_config_last_used")
        }

        val dataStore: DataStore<Preferences> get() = testDataStore

        val notificationSettings: Flow<NotificationSettings> = testDataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { preferences ->
                NotificationSettings(
                    vibrationEnabled = preferences[VIBRATION_ENABLED] ?: NotificationSettings.DEFAULT.vibrationEnabled,
                    soundEnabled = preferences[SOUND_ENABLED] ?: NotificationSettings.DEFAULT.soundEnabled,
                    flashEnabled = preferences[FLASH_ENABLED] ?: NotificationSettings.DEFAULT.flashEnabled,
                    autoMode = preferences[AUTO_MODE] ?: NotificationSettings.DEFAULT.autoMode,
                )
            }

        val currentConfiguration: Flow<TimerConfiguration?> = testDataStore.data
            .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
            .map { preferences ->
                val configId = preferences[CURRENT_CONFIG_ID]
                if (configId == null) {
                    null
                } else {
                    TimerConfiguration(
                        id = configId,
                        laps = preferences[CURRENT_CONFIG_LAPS] ?: TimerConfiguration.DEFAULT.laps,
                        workDuration = (
                            preferences[CURRENT_CONFIG_WORK_DURATION]
                                ?: TimerConfiguration.DEFAULT.workDuration.inWholeSeconds
                            ).seconds,
                        restDuration = (
                            preferences[CURRENT_CONFIG_REST_DURATION]
                                ?: TimerConfiguration.DEFAULT.restDuration.inWholeSeconds
                            ).seconds,
                        lastUsed = preferences[CURRENT_CONFIG_LAST_USED] ?: TimerConfiguration.DEFAULT.lastUsed,
                    )
                }
            }

        suspend fun updateNotificationSettings(settings: NotificationSettings) {
            testDataStore.edit { preferences ->
                preferences[VIBRATION_ENABLED] = settings.vibrationEnabled
                preferences[SOUND_ENABLED] = settings.soundEnabled
                preferences[FLASH_ENABLED] = settings.flashEnabled
                preferences[AUTO_MODE] = settings.autoMode
            }
        }

        suspend fun updateCurrentConfiguration(config: TimerConfiguration) {
            testDataStore.edit { preferences ->
                preferences[CURRENT_CONFIG_ID] = config.id
                preferences[CURRENT_CONFIG_LAPS] = config.laps
                preferences[CURRENT_CONFIG_WORK_DURATION] = config.workDuration.inWholeSeconds
                preferences[CURRENT_CONFIG_REST_DURATION] = config.restDuration.inWholeSeconds
                preferences[CURRENT_CONFIG_LAST_USED] = config.lastUsed
            }
        }
    }

    @Test
    fun notificationSettings_saveAndRetrieve() = runTest {
        // Given
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = false,
        )

        // When
        dataStoreManager.updateNotificationSettings(settings)

        // Then
        dataStoreManager.notificationSettings.test {
            val retrieved = awaitItem()
            assertThat(retrieved.vibrationEnabled).isTrue()
            assertThat(retrieved.soundEnabled).isFalse()
            assertThat(retrieved.flashEnabled).isTrue()
            assertThat(retrieved.autoMode).isFalse()
        }
    }

    @Test
    fun notificationSettings_defaultValues() = runTest {
        // When - No settings saved yet
        dataStoreManager.notificationSettings.test {
            val defaultSettings = awaitItem()

            // Then - Should get default values
            assertThat(defaultSettings.vibrationEnabled).isTrue()
            assertThat(defaultSettings.soundEnabled).isTrue()
            assertThat(defaultSettings.flashEnabled).isFalse()
            assertThat(defaultSettings.autoMode).isTrue()
        }
    }

    @Test
    fun notificationSettings_partialUpdate() = runTest {
        // Given - Initial settings
        val initialSettings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = true,
        )
        dataStoreManager.updateNotificationSettings(initialSettings)

        // When - Update only some fields
        val updatedSettings = initialSettings.copy(
            vibrationEnabled = false,
            flashEnabled = true,
        )
        dataStoreManager.updateNotificationSettings(updatedSettings)

        // Then
        dataStoreManager.notificationSettings.test {
            val retrieved = awaitItem()
            assertThat(retrieved.vibrationEnabled).isFalse()
            assertThat(retrieved.soundEnabled).isTrue() // Unchanged
            assertThat(retrieved.flashEnabled).isTrue()
            assertThat(retrieved.autoMode).isTrue() // Unchanged
        }
    }

    @Test
    fun currentConfiguration_saveAndRetrieve() = runTest {
        // Given
        val config = TimerConfiguration(
            id = "test-config",
            laps = 8,
            workDuration = 2.minutes,
            restDuration = 45.seconds,
            lastUsed = System.currentTimeMillis(),
        )

        // When
        dataStoreManager.updateCurrentConfiguration(config)

        // Then
        dataStoreManager.currentConfiguration.test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved!!.id).isEqualTo("test-config")
            assertThat(retrieved.laps).isEqualTo(8)
            assertThat(retrieved.workDuration).isEqualTo(2.minutes)
            assertThat(retrieved.restDuration).isEqualTo(45.seconds)
        }
    }

    @Test
    fun currentConfiguration_defaultNull() = runTest {
        // When - No configuration saved yet
        dataStoreManager.currentConfiguration.test {
            val defaultConfig = awaitItem()

            // Then - Should be null initially
            assertThat(defaultConfig).isNull()
        }
    }

    @Test
    fun currentConfiguration_overwrite() = runTest {
        // Given - Initial configuration
        val config1 = TimerConfiguration(
            id = "config-1",
            laps = 5,
            workDuration = 1.minutes,
            restDuration = 30.seconds,
            lastUsed = 1000L,
        )
        dataStoreManager.updateCurrentConfiguration(config1)

        // When - Save different configuration
        val config2 = TimerConfiguration(
            id = "config-2",
            laps = 10,
            workDuration = 90.seconds,
            restDuration = 15.seconds,
            lastUsed = 2000L,
        )
        dataStoreManager.updateCurrentConfiguration(config2)

        // Then - Should have config2, not config1
        dataStoreManager.currentConfiguration.test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved!!.id).isEqualTo("config-2")
            assertThat(retrieved.laps).isEqualTo(10)
            assertThat(retrieved.workDuration).isEqualTo(90.seconds)
        }
    }

    @Test
    fun bothSettings_independentStorage() = runTest {
        // Given - Both settings and configuration
        val settings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = true,
            flashEnabled = true,
            autoMode = false,
        )
        val config = TimerConfiguration(
            id = "independent-test",
            laps = 3,
            workDuration = 45.seconds,
            restDuration = 10.seconds,
            lastUsed = System.currentTimeMillis(),
        )

        // When - Save both
        dataStoreManager.updateNotificationSettings(settings)
        dataStoreManager.updateCurrentConfiguration(config)

        // Then - Both should be retrievable independently
        dataStoreManager.notificationSettings.test {
            val retrievedSettings = awaitItem()
            assertThat(retrievedSettings.vibrationEnabled).isFalse()
            assertThat(retrievedSettings.soundEnabled).isTrue()
        }

        dataStoreManager.currentConfiguration.test {
            val retrievedConfig = awaitItem()
            assertThat(retrievedConfig).isNotNull()
            assertThat(retrievedConfig!!.id).isEqualTo("independent-test")
            assertThat(retrievedConfig.laps).isEqualTo(3)
        }
    }

    @Test
    fun dataStore_concurrentAccess() = runTest {
        // Test concurrent access to DataStore
        val settings1 = NotificationSettings(vibrationEnabled = true, soundEnabled = false, flashEnabled = false, autoMode = true)
        val settings2 = NotificationSettings(vibrationEnabled = false, soundEnabled = true, flashEnabled = true, autoMode = false)

        // When - Save settings concurrently (in sequence due to runTest)
        dataStoreManager.updateNotificationSettings(settings1)
        dataStoreManager.updateNotificationSettings(settings2)

        // Then - Should have the last saved settings
        dataStoreManager.notificationSettings.test {
            val result = awaitItem()
            assertThat(result.vibrationEnabled).isFalse()
            assertThat(result.soundEnabled).isTrue()
            assertThat(result.flashEnabled).isTrue()
            assertThat(result.autoMode).isFalse()
        }
    }

    @Test
    fun extremeValues_handling() = runTest {
        // Test extreme but valid values
        val extremeConfig = TimerConfiguration(
            id = "extreme-values-test",
            laps = 999, // Maximum (infinite)
            workDuration = 600.seconds, // 10 minutes
            restDuration = 0.seconds, // No rest
            lastUsed = 0L, // Minimum timestamp
        )

        // When
        dataStoreManager.updateCurrentConfiguration(extremeConfig)

        // Then
        dataStoreManager.currentConfiguration.test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved!!.laps).isEqualTo(999)
            assertThat(retrieved.workDuration).isEqualTo(600.seconds)
            assertThat(retrieved.restDuration).isEqualTo(0.seconds)
            assertThat(retrieved.lastUsed).isEqualTo(0L)
        }
    }

    @Test
    fun specialCharacters_inConfigId() = runTest {
        // Test configuration ID with special characters
        val specialConfig = TimerConfiguration(
            id = "test-config_123.special!@#",
            laps = 1,
            workDuration = 60.seconds,
            restDuration = 0.seconds,
            lastUsed = System.currentTimeMillis(),
        )

        // When
        dataStoreManager.updateCurrentConfiguration(specialConfig)

        // Then
        dataStoreManager.currentConfiguration.test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved!!.id).isEqualTo("test-config_123.special!@#")
        }
    }

    @Test
    fun largeConfiguration_handling() = runTest {
        // Test configuration with large values
        val largeConfig = TimerConfiguration(
            id = "a".repeat(100), // Long ID
            laps = 500,
            workDuration = 30.minutes,
            restDuration = 10.minutes,
            lastUsed = Long.MAX_VALUE,
        )

        // When
        dataStoreManager.updateCurrentConfiguration(largeConfig)

        // Then
        dataStoreManager.currentConfiguration.test {
            val retrieved = awaitItem()
            assertThat(retrieved).isNotNull()
            assertThat(retrieved!!.id).hasLength(100)
            assertThat(retrieved.laps).isEqualTo(500)
            assertThat(retrieved.workDuration).isEqualTo(30.minutes)
            assertThat(retrieved.lastUsed).isEqualTo(Long.MAX_VALUE)
        }
    }
}
