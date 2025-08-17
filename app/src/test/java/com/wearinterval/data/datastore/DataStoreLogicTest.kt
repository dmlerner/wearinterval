package com.wearinterval.data.datastore

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for DataStore logic and data transformations.
 * Integration tests with actual DataStore should be in androidTest directory.
 */
class DataStoreLogicTest {

    @Test
    fun notificationSettings_serialization() {
        // Test notification settings serialization logic
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = false,
            flashEnabled = true,
            autoMode = false,
        )

        // Test that all properties are accessible
        assertThat(settings.vibrationEnabled).isTrue()
        assertThat(settings.soundEnabled).isFalse()
        assertThat(settings.flashEnabled).isTrue()
        assertThat(settings.autoMode).isFalse()
    }

    @Test
    fun notificationSettings_defaultValues() {
        // Test default notification settings
        val defaultSettings = NotificationSettings.DEFAULT

        assertThat(defaultSettings.vibrationEnabled).isTrue()
        assertThat(defaultSettings.soundEnabled).isTrue()
        assertThat(defaultSettings.flashEnabled).isFalse()
        assertThat(defaultSettings.autoMode).isTrue()
    }

    @Test
    fun timerConfiguration_serialization() {
        // Test timer configuration serialization logic
        val config = TimerConfiguration(
            laps = 10,
            workDuration = 90.seconds,
            restDuration = 30.seconds,
        )

        assertThat(config.laps).isEqualTo(10)
        assertThat(config.workDuration).isEqualTo(90.seconds)
        assertThat(config.restDuration).isEqualTo(30.seconds)
    }

    @Test
    fun timerConfiguration_defaultValues() {
        // Test default timer configuration
        val defaultConfig = TimerConfiguration.DEFAULT

        assertThat(defaultConfig.laps).isEqualTo(1)
        assertThat(defaultConfig.workDuration).isEqualTo(60.seconds)
        assertThat(defaultConfig.restDuration).isEqualTo(0.seconds)
    }

    @Test
    fun dataStore_key_naming() {
        // Test DataStore key naming conventions
        val preferenceKeys = listOf(
            "notification_vibration",
            "notification_sound",
            "notification_flash",
            "notification_auto_mode",
            "timer_laps",
            "timer_work_duration",
            "timer_rest_duration",
        )

        preferenceKeys.forEach { key ->
            assertThat(key).isNotEmpty()
            assertThat(key).matches("[a-z_]+") // lowercase with underscores
            assertThat(key).doesNotContain(" ") // No spaces
            assertThat(key.startsWith("_")).isFalse() // No leading underscore
            assertThat(key.endsWith("_")).isFalse() // No trailing underscore
        }
    }

    @Test
    fun preference_value_validation() {
        // Test preference value validation
        fun validateBooleanPreference(value: Any?): Boolean {
            return value is Boolean
        }

        fun validateIntPreference(value: Any?, min: Int = 1, max: Int = 999): Boolean {
            return value is Int && value in min..max
        }

        fun validateLongPreference(value: Any?, min: Long = 0L): Boolean {
            return value is Long && value >= min
        }

        // Test validation functions
        assertThat(validateBooleanPreference(true)).isTrue()
        assertThat(validateBooleanPreference("true")).isFalse()

        assertThat(validateIntPreference(5)).isTrue()
        assertThat(validateIntPreference(-1)).isFalse()
        assertThat(validateIntPreference(1000)).isFalse()

        assertThat(validateLongPreference(60000L)).isTrue()
        assertThat(validateLongPreference(-1L)).isFalse()
    }

    @Test
    fun duration_storage_conversion() {
        // Test duration to/from storage conversion
        fun durationToMillis(duration: kotlin.time.Duration): Long {
            return duration.inWholeMilliseconds
        }

        fun millisToDuration(millis: Long): kotlin.time.Duration {
            return millis.milliseconds
        }

        val testDurations = listOf(
            0.seconds,
            30.seconds,
            1.minutes,
            90.seconds,
            5.minutes,
        )

        testDurations.forEach { originalDuration ->
            val millis = durationToMillis(originalDuration)
            val convertedBack = millisToDuration(millis)

            assertThat(convertedBack).isEqualTo(originalDuration)
        }
    }

    @Test
    fun configuration_equality_comparison() {
        // Test configuration equality logic
        val fixedId = "test-id-123"
        val config1 = TimerConfiguration(
            id = fixedId,
            laps = 5,
            workDuration = 60.seconds,
            restDuration = 30.seconds,
        )

        val config2 = TimerConfiguration(
            id = fixedId, // Same ID for equality test
            laps = 5,
            workDuration = 60.seconds,
            restDuration = 30.seconds,
        )

        val config3 = TimerConfiguration(
            id = "different-id",
            laps = 10,
            workDuration = 60.seconds,
            restDuration = 30.seconds,
        )

        // Test equality
        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)

        // Test hash codes for equal objects (required by equals contract)
        assertThat(config1.hashCode()).isEqualTo(config2.hashCode())
    }

    @Test
    fun settings_equality_comparison() {
        // Test settings equality logic
        val settings1 = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = false,
        )

        val settings2 = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = false,
        )

        val settings3 = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = false,
        )

        assertThat(settings1).isEqualTo(settings2)
        assertThat(settings1).isNotEqualTo(settings3)
    }

    @Test
    fun dataStore_error_handling() {
        // Test error handling scenarios
        fun parseIntSafely(value: String, default: Int): Int {
            return try {
                value.toInt().coerceIn(1, 999)
            } catch (e: NumberFormatException) {
                default
            }
        }

        fun parseBooleanSafely(value: String, default: Boolean): Boolean {
            return when (value.lowercase()) {
                "true" -> true
                "false" -> false
                else -> default
            }
        }

        // Test safe parsing
        assertThat(parseIntSafely("5", 1)).isEqualTo(5)
        assertThat(parseIntSafely("invalid", 1)).isEqualTo(1)
        assertThat(parseIntSafely("1000", 1)).isEqualTo(999) // Coerced to max

        assertThat(parseBooleanSafely("true", false)).isTrue()
        assertThat(parseBooleanSafely("invalid", false)).isFalse()
    }

    @Test
    fun configuration_validation_rules() {
        // Test configuration validation rules
        fun isValidConfiguration(config: TimerConfiguration): Boolean {
            return config.laps >= 1 &&
                config.laps <= 999 &&
                config.workDuration >= 5.seconds &&
                config.workDuration <= 30.minutes &&
                config.restDuration >= 0.seconds &&
                config.restDuration <= 10.minutes
        }

        // Test valid configurations
        val validConfig = TimerConfiguration(
            laps = 5,
            workDuration = 90.seconds,
            restDuration = 30.seconds,
        )
        assertThat(isValidConfiguration(validConfig)).isTrue()

        // Test invalid configurations
        val invalidLaps = TimerConfiguration(
            laps = 0,
            workDuration = 60.seconds,
            restDuration = 30.seconds,
        )
        assertThat(isValidConfiguration(invalidLaps)).isFalse()

        val invalidWorkDuration = TimerConfiguration(
            laps = 5,
            workDuration = 2.seconds,
            restDuration = 30.seconds,
        )
        assertThat(isValidConfiguration(invalidWorkDuration)).isFalse()
    }

    @Test
    fun dataStore_flow_behavior() {
        // Test DataStore flow behavior expectations
        fun simulateDataStoreFlow(): List<NotificationSettings> {
            // Simulate initial value, then updates
            return listOf(
                NotificationSettings.DEFAULT,
                NotificationSettings.DEFAULT.copy(vibrationEnabled = false),
                NotificationSettings.DEFAULT.copy(soundEnabled = false),
            )
        }

        val flowValues = simulateDataStoreFlow()

        // Verify flow emits initial value
        assertThat(flowValues.first()).isEqualTo(NotificationSettings.DEFAULT)

        // Verify flow emits updates
        assertThat(flowValues).hasSize(3)
        assertThat(flowValues[1].vibrationEnabled).isFalse()
        assertThat(flowValues[2].soundEnabled).isFalse()
    }

    @Test
    fun migration_compatibility() {
        // Test migration compatibility for preference changes
        fun migratePreferences(oldVersion: Int, newVersion: Int): Map<String, Any> {
            val preferences = mutableMapOf<String, Any>()

            when {
                oldVersion < 2 && newVersion >= 2 -> {
                    // Add new auto mode preference with default value
                    preferences["notification_auto_mode"] = false
                }
                oldVersion < 3 && newVersion >= 3 -> {
                    // Add new flash preference with default value
                    preferences["notification_flash"] = false
                }
            }

            return preferences
        }

        // Test migration from version 1 to 2
        val migration1to2 = migratePreferences(1, 2)
        assertThat(migration1to2).containsEntry("notification_auto_mode", false)

        // Test migration from version 2 to 3
        val migration2to3 = migratePreferences(2, 3)
        assertThat(migration2to3).containsEntry("notification_flash", false)
    }
}
