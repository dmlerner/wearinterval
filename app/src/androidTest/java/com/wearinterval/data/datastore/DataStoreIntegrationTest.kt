package com.wearinterval.data.datastore

import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class DataStoreIntegrationTest {

    private lateinit var dataStoreManager: DataStoreManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        dataStoreManager = DataStoreManager(context)

        // Clear any existing data
        runTest {
            dataStoreManager.dataStore.edit { it.clear() }
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
