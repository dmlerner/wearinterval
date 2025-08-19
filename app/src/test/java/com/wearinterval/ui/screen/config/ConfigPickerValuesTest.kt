package com.wearinterval.ui.screen.config

import com.wearinterval.util.Constants
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigPickerValuesTest {

  @Test
  fun lapsDisplayText_returns_infinity_symbol_for_infinite_laps() {
    val result = ConfigPickerValues.lapsDisplayText(Constants.TimerLimits.INFINITE_LAPS)
    assertEquals("∞", result)
  }

  @Test
  fun lapsDisplayText_returns_number_string_for_finite_laps() {
    assertEquals("1", ConfigPickerValues.lapsDisplayText(1))
    assertEquals("10", ConfigPickerValues.lapsDisplayText(10))
    assertEquals("500", ConfigPickerValues.lapsDisplayText(500))
  }

  @Test
  fun durationDisplayText_returns_none_for_min_rest_duration() {
    val result = ConfigPickerValues.durationDisplayText(Constants.TimerLimits.MIN_REST_DURATION)
    assertEquals("None", result)
  }

  @Test
  fun durationDisplayText_formats_seconds_only() {
    assertEquals("1s", ConfigPickerValues.durationDisplayText(1.seconds))
    assertEquals("30s", ConfigPickerValues.durationDisplayText(30.seconds))
    assertEquals("59s", ConfigPickerValues.durationDisplayText(59.seconds))
  }

  @Test
  fun durationDisplayText_formats_minutes_and_seconds() {
    assertEquals("1:00", ConfigPickerValues.durationDisplayText(1.minutes))
    assertEquals("1:30", ConfigPickerValues.durationDisplayText(1.minutes + 30.seconds))
    assertEquals("2:05", ConfigPickerValues.durationDisplayText(2.minutes + 5.seconds))
    assertEquals("15:45", ConfigPickerValues.durationDisplayText(15.minutes + 45.seconds))
    assertEquals("59:59", ConfigPickerValues.durationDisplayText(59.minutes + 59.seconds))
  }

  @Test
  fun durationDisplayText_formats_hours_minutes_seconds() {
    assertEquals("1h", ConfigPickerValues.durationDisplayText(1.hours))
    assertEquals("1h30m", ConfigPickerValues.durationDisplayText(1.hours + 30.minutes))
    assertEquals(
      "2h15m30s",
      ConfigPickerValues.durationDisplayText(2.hours + 15.minutes + 30.seconds)
    )
    assertEquals("24h", ConfigPickerValues.durationDisplayText(24.hours))
  }

  @Test
  fun durationDisplayText_handles_complex_time_combinations() {
    assertEquals("3h45m", ConfigPickerValues.durationDisplayText(3.hours + 45.minutes))
    assertEquals(
      "12h30m15s",
      ConfigPickerValues.durationDisplayText(12.hours + 30.minutes + 15.seconds)
    )
    assertEquals("1h0m1s", ConfigPickerValues.durationDisplayText(1.hours + 1.seconds))
  }

  @Test
  fun findLapsIndex_returns_exact_match() {
    assertEquals(0, ConfigPickerValues.findLapsIndex(1)) // First item
    assertEquals(9, ConfigPickerValues.findLapsIndex(10)) // 10th item (0-indexed)
    val infiniteIndex = ConfigPickerValues.LAPS_VALUES.indexOf(Constants.TimerLimits.INFINITE_LAPS)
    assertEquals(
      infiniteIndex,
      ConfigPickerValues.findLapsIndex(Constants.TimerLimits.INFINITE_LAPS)
    )
  }

  @Test
  fun findLapsIndex_returns_closest_match() {
    // Test value between list items - should return closest
    val result = ConfigPickerValues.findLapsIndex(11) // Between 10 and 12
    assertTrue(
      "Should be 10 or 12",
      ConfigPickerValues.LAPS_VALUES[result] == 10 || ConfigPickerValues.LAPS_VALUES[result] == 12
    )

    // Test value below minimum - should return first item
    assertEquals(0, ConfigPickerValues.findLapsIndex(0))

    // Test value above maximum - should return closest (infinite laps)
    val maxIndex = ConfigPickerValues.LAPS_VALUES.size - 1
    assertEquals(maxIndex, ConfigPickerValues.findLapsIndex(10000))
  }

  @Test
  fun findDurationIndex_returns_exact_match_for_work_duration() {
    val oneMinuteIndex = ConfigPickerValues.DURATION_VALUES.indexOf(1.minutes)
    assertEquals(oneMinuteIndex, ConfigPickerValues.findDurationIndex(1.minutes, false))

    val thirtySecondsIndex = ConfigPickerValues.DURATION_VALUES.indexOf(30.seconds)
    assertEquals(thirtySecondsIndex, ConfigPickerValues.findDurationIndex(30.seconds, false))
  }

  @Test
  fun findDurationIndex_returns_exact_match_for_rest_duration() {
    val zeroSecondsIndex = ConfigPickerValues.REST_DURATION_VALUES.indexOf(0.seconds)
    assertEquals(zeroSecondsIndex, ConfigPickerValues.findDurationIndex(0.seconds, true))

    val oneMinuteIndex = ConfigPickerValues.REST_DURATION_VALUES.indexOf(1.minutes)
    assertEquals(oneMinuteIndex, ConfigPickerValues.findDurationIndex(1.minutes, true))
  }

  @Test
  fun findDurationIndex_returns_closest_match() {
    // Test duration between list values
    val result =
      ConfigPickerValues.findDurationIndex(75.seconds, false) // Between 1 minute and 1:15
    val actualDuration = ConfigPickerValues.DURATION_VALUES[result]
    assertTrue(
      "Should be close to 75 seconds",
      actualDuration == 1.minutes || actualDuration == (1.minutes + 15.seconds)
    )
  }

  @Test
  fun findDurationIndex_handles_edge_cases() {
    // Test duration below minimum (should return first item which is now 1 second)
    assertEquals(0, ConfigPickerValues.findDurationIndex(0.5.seconds, false))

    // Test duration above maximum
    val maxIndex = ConfigPickerValues.DURATION_VALUES.size - 1
    assertEquals(maxIndex, ConfigPickerValues.findDurationIndex(100.hours, false))
  }

  @Test
  fun laps_values_are_in_ascending_order() {
    for (i in 1 until ConfigPickerValues.LAPS_VALUES.size) {
      assertTrue(
        "Laps values should be in ascending order",
        ConfigPickerValues.LAPS_VALUES[i] >= ConfigPickerValues.LAPS_VALUES[i - 1]
      )
    }
  }

  @Test
  fun duration_values_are_in_ascending_order() {
    for (i in 1 until ConfigPickerValues.DURATION_VALUES.size) {
      assertTrue(
        "Duration values should be in ascending order",
        ConfigPickerValues.DURATION_VALUES[i] >= ConfigPickerValues.DURATION_VALUES[i - 1]
      )
    }
  }

  @Test
  fun rest_duration_values_start_with_zero() {
    assertEquals(0.seconds, ConfigPickerValues.REST_DURATION_VALUES.first())
  }

  @Test
  fun rest_duration_values_contain_all_duration_values() {
    val restWithoutZero = ConfigPickerValues.REST_DURATION_VALUES.drop(1)
    assertEquals(ConfigPickerValues.DURATION_VALUES, restWithoutZero)
  }

  @Test
  fun display_lists_match_value_lists() {
    assertEquals(ConfigPickerValues.LAPS_VALUES.size, ConfigPickerValues.LAPS_DISPLAY_ITEMS.size)
    assertEquals(
      ConfigPickerValues.DURATION_VALUES.size,
      ConfigPickerValues.DURATION_DISPLAY_ITEMS.size
    )
    assertEquals(
      ConfigPickerValues.REST_DURATION_VALUES.size,
      ConfigPickerValues.REST_DURATION_DISPLAY_ITEMS.size
    )
  }

  @Test
  fun display_items_match_display_text_functions() {
    // Test laps display consistency
    for (i in ConfigPickerValues.LAPS_VALUES.indices) {
      val expectedDisplay = ConfigPickerValues.lapsDisplayText(ConfigPickerValues.LAPS_VALUES[i])
      assertEquals(expectedDisplay, ConfigPickerValues.LAPS_DISPLAY_ITEMS[i])
    }

    // Test duration display consistency
    for (i in ConfigPickerValues.DURATION_VALUES.indices) {
      val expectedDisplay =
        ConfigPickerValues.durationDisplayText(ConfigPickerValues.DURATION_VALUES[i])
      assertEquals(expectedDisplay, ConfigPickerValues.DURATION_DISPLAY_ITEMS[i])
    }

    // Test rest duration display consistency
    for (i in ConfigPickerValues.REST_DURATION_VALUES.indices) {
      val expectedDisplay =
        ConfigPickerValues.durationDisplayText(ConfigPickerValues.REST_DURATION_VALUES[i])
      assertEquals(expectedDisplay, ConfigPickerValues.REST_DURATION_DISPLAY_ITEMS[i])
    }
  }

  private fun assertTrue(message: String, condition: Boolean) {
    if (!condition) throw AssertionError(message)
  }
}
