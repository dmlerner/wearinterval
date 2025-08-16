package com.wearinterval.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimerConfigurationTest {

    @Test
    fun `isValid returns true for valid configuration`() {
        val config = TimerConfiguration(
            laps = 5,
            workDuration = 45.seconds,
            restDuration = 15.seconds
        )
        
        assertThat(config.isValid()).isTrue()
    }

    @Test
    fun `isValid returns false for invalid lap count`() {
        val tooFewLaps = TimerConfiguration(
            laps = 0,
            workDuration = 60.seconds,
            restDuration = 0.seconds
        )
        
        val tooManyLaps = TimerConfiguration(
            laps = 1000,
            workDuration = 60.seconds,
            restDuration = 0.seconds
        )
        
        assertThat(tooFewLaps.isValid()).isFalse()
        assertThat(tooManyLaps.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false for invalid work duration`() {
        val tooShort = TimerConfiguration(
            laps = 1,
            workDuration = 3.seconds,
            restDuration = 0.seconds
        )
        
        val tooLong = TimerConfiguration(
            laps = 1,
            workDuration = 15.minutes,
            restDuration = 0.seconds
        )
        
        assertThat(tooShort.isValid()).isFalse()
        assertThat(tooLong.isValid()).isFalse()
    }

    @Test
    fun `isValid returns false for invalid rest duration`() {
        val negativeRest = TimerConfiguration(
            laps = 1,
            workDuration = 60.seconds,
            restDuration = (-5).seconds
        )
        
        val tooLongRest = TimerConfiguration(
            laps = 1,
            workDuration = 60.seconds,
            restDuration = 15.minutes
        )
        
        assertThat(negativeRest.isValid()).isFalse()
        assertThat(tooLongRest.isValid()).isFalse()
    }

    @Test
    fun `displayString formats single lap correctly`() {
        val config = TimerConfiguration(
            laps = 1,
            workDuration = 90.seconds,
            restDuration = 0.seconds
        )
        
        assertThat(config.displayString()).isEqualTo("1:30")
    }

    @Test
    fun `displayString formats multiple laps without rest`() {
        val config = TimerConfiguration(
            laps = 5,
            workDuration = 45.seconds,
            restDuration = 0.seconds
        )
        
        assertThat(config.displayString()).isEqualTo("5 x 45s")
    }

    @Test
    fun `displayString formats multiple laps with rest`() {
        val config = TimerConfiguration(
            laps = 8,
            workDuration = 30.seconds,
            restDuration = 15.seconds
        )
        
        assertThat(config.displayString()).isEqualTo("8 x 30s + 15s")
    }

    @Test
    fun `displayString formats infinite laps`() {
        val config = TimerConfiguration(
            laps = 999,
            workDuration = 25.seconds,
            restDuration = 5.seconds
        )
        
        assertThat(config.displayString()).isEqualTo("999 x 25s + 5s")
    }

    @Test
    fun `shortDisplayString formats single lap`() {
        val config = TimerConfiguration(
            laps = 1,
            workDuration = 2.minutes,
            restDuration = 30.seconds
        )
        
        assertThat(config.shortDisplayString()).isEqualTo("2:00")
    }

    @Test
    fun `shortDisplayString formats multiple laps`() {
        val config = TimerConfiguration(
            laps = 10,
            workDuration = 45.seconds,
            restDuration = 15.seconds
        )
        
        assertThat(config.shortDisplayString()).isEqualTo("10×45s")
    }

    @Test
    fun `shortDisplayString formats infinite laps with symbol`() {
        val config = TimerConfiguration(
            laps = 999,
            workDuration = 30.seconds,
            restDuration = 10.seconds
        )
        
        assertThat(config.shortDisplayString()).isEqualTo("∞×30s")
    }

    @Test
    fun `formatDuration handles various durations correctly`() {
        val configs = listOf(
            30.seconds to "30s",
            60.seconds to "1:00",
            90.seconds to "1:30",
            2.minutes to "2:00",
            125.seconds to "2:05"
        )
        
        configs.forEach { (duration, expected) ->
            val config = TimerConfiguration(laps = 1, workDuration = duration, restDuration = 0.seconds)
            assertThat(config.displayString()).isEqualTo(expected)
        }
    }

    @Test
    fun `withUpdatedTimestamp updates timestamp`() {
        val originalTime = 1000L
        val config = TimerConfiguration(
            laps = 1,
            workDuration = 60.seconds,
            restDuration = 0.seconds,
            lastUsed = originalTime
        )
        
        val updated = config.withUpdatedTimestamp()
        
        assertThat(updated.lastUsed).isGreaterThan(originalTime)
        assertThat(updated.id).isEqualTo(config.id)
        assertThat(updated.laps).isEqualTo(config.laps)
        assertThat(updated.workDuration).isEqualTo(config.workDuration)
        assertThat(updated.restDuration).isEqualTo(config.restDuration)
    }

    @Test
    fun `validate coerces invalid values to valid ranges`() {
        val config = TimerConfiguration.validate(
            laps = 0,
            workDuration = 2.seconds,
            restDuration = 15.minutes
        )
        
        assertThat(config.laps).isEqualTo(1)
        assertThat(config.workDuration).isEqualTo(5.seconds)
        assertThat(config.restDuration).isEqualTo(10.minutes)
    }

    @Test
    fun `validate preserves valid values`() {
        val config = TimerConfiguration.validate(
            laps = 5,
            workDuration = 45.seconds,
            restDuration = 15.seconds
        )
        
        assertThat(config.laps).isEqualTo(5)
        assertThat(config.workDuration).isEqualTo(45.seconds)
        assertThat(config.restDuration).isEqualTo(15.seconds)
    }

    @Test
    fun `validate handles edge cases correctly`() {
        val maxValid = TimerConfiguration.validate(
            laps = 999,
            workDuration = 10.minutes,
            restDuration = 10.minutes
        )
        
        val minValid = TimerConfiguration.validate(
            laps = 1,
            workDuration = 5.seconds,
            restDuration = 0.seconds
        )
        
        assertThat(maxValid.laps).isEqualTo(999)
        assertThat(maxValid.workDuration).isEqualTo(10.minutes)
        assertThat(maxValid.restDuration).isEqualTo(10.minutes)
        
        assertThat(minValid.laps).isEqualTo(1)
        assertThat(minValid.workDuration).isEqualTo(5.seconds)
        assertThat(minValid.restDuration).isEqualTo(0.seconds)
    }

    @Test
    fun `DEFAULT configuration is valid`() {
        assertThat(TimerConfiguration.DEFAULT.isValid()).isTrue()
        assertThat(TimerConfiguration.DEFAULT.laps).isEqualTo(1)
        assertThat(TimerConfiguration.DEFAULT.workDuration).isEqualTo(60.seconds)
        assertThat(TimerConfiguration.DEFAULT.restDuration).isEqualTo(0.seconds)
    }

    @Test
    fun `COMMON_PRESETS are all valid`() {
        TimerConfiguration.COMMON_PRESETS.forEach { preset ->
            assertThat(preset.isValid()).isTrue()
        }
    }

    @Test
    fun `COMMON_PRESETS include expected configurations`() {
        val presets = TimerConfiguration.COMMON_PRESETS
        
        // Check for single work intervals - compare meaningful fields only
        val hasThirtySecond = presets.any { 
            it.laps == 1 && it.workDuration == 30.seconds && it.restDuration == 0.seconds 
        }
        assertThat(hasThirtySecond).isTrue()
        
        // Check for infinite intervals
        val infinitePreset = presets.find { it.laps == 999 }
        assertThat(infinitePreset).isNotNull()
        
        // Check for tabata-style workout - compare meaningful fields only
        val hasTabata = presets.any { 
            it.laps == 8 && it.workDuration == 25.seconds && it.restDuration == 5.seconds 
        }
        assertThat(hasTabata).isTrue()
    }

    @Test
    fun `configuration equality works correctly`() {
        val config1 = TimerConfiguration(
            id = "test-id",
            laps = 5,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            lastUsed = 1000L
        )
        
        val config2 = TimerConfiguration(
            id = "test-id",
            laps = 5,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
            lastUsed = 1000L
        )
        
        val config3 = config1.copy(laps = 10)
        
        assertThat(config1).isEqualTo(config2)
        assertThat(config1).isNotEqualTo(config3)
    }
}