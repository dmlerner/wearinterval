package com.wearinterval.ui.screen.config

import com.wearinterval.util.Constants
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ConfigPickerValues {

  // Laps values as defined in design spec
  val LAPS_VALUES =
    listOf(
      1,
      2,
      3,
      4,
      5,
      6,
      7,
      8,
      9,
      10,
      12,
      15,
      20,
      25,
      30,
      40,
      50,
      60,
      75,
      100,
      150,
      200,
      300,
      500,
      Constants.TimerLimits.INFINITE_LAPS,
    )

  // Duration values - extended up to 24 hours
  val DURATION_VALUES =
    listOf(
      1.seconds,
      2.seconds,
      3.seconds,
      4.seconds,
      5.seconds,
      10.seconds,
      15.seconds,
      20.seconds,
      30.seconds,
      45.seconds,
      1.minutes,
      1.minutes + 15.seconds,
      1.minutes + 30.seconds,
      2.minutes,
      2.minutes + 30.seconds,
      3.minutes,
      4.minutes,
      5.minutes,
      6.minutes,
      8.minutes,
      10.minutes,
      12.minutes,
      15.minutes,
      20.minutes,
      25.minutes,
      30.minutes,
      35.minutes,
      40.minutes,
      45.minutes,
      50.minutes,
      55.minutes,
      1.hours,
      1.hours + 15.minutes,
      1.hours + 30.minutes,
      1.hours + 45.minutes,
      2.hours,
      2.hours + 30.minutes,
      3.hours,
      4.hours,
      5.hours,
      6.hours,
      8.hours,
      10.hours,
      12.hours,
      16.hours,
      20.hours,
      24.hours,
    )

  // Rest duration values (same as duration but with 0 at start for "no rest")
  val REST_DURATION_VALUES = listOf(0.seconds) + DURATION_VALUES

  // Display text for laps
  fun lapsDisplayText(laps: Int): String =
    when (laps) {
      Constants.TimerLimits.INFINITE_LAPS -> "∞"
      else -> laps.toString()
    }

  // Display text for durations
  fun durationDisplayText(duration: Duration): String =
    when {
      duration == Constants.TimerLimits.MIN_REST_DURATION -> "None"
      duration.inWholeHours > 0 -> {
        val hours = duration.inWholeHours
        val minutes = ((duration.inWholeMinutes % 60)).toInt()
        val seconds = (duration.inWholeSeconds % 60).toInt()
        when {
          seconds > 0 -> "${hours}h${minutes}m${seconds}s"
          minutes > 0 -> "${hours}h${minutes}m"
          else -> "${hours}h"
        }
      }
      duration.inWholeMinutes > 0 -> {
        val minutes = duration.inWholeMinutes
        val seconds = (duration.inWholeSeconds % 60).toInt()
        if (seconds == 0) {
          "$minutes:00"
        } else {
          "$minutes:${seconds.toString().padStart(Constants.UI.STRING_PADDING_WIDTH, '0')}"
        }
      }
      else -> "${duration.inWholeSeconds}s"
    }

  // Pre-computed display lists (created once at compile time, not during composition)
  val LAPS_DISPLAY_ITEMS = LAPS_VALUES.map { lapsDisplayText(it) }
  val DURATION_DISPLAY_ITEMS = DURATION_VALUES.map { durationDisplayText(it) }
  val REST_DURATION_DISPLAY_ITEMS = REST_DURATION_VALUES.map { durationDisplayText(it) }

  // Find the index of the value in the list that is closest to the given lap count
  fun findLapsIndex(laps: Int): Int {
    val closest = LAPS_VALUES.minByOrNull { abs(it - laps) } ?: return LAPS_VALUES.size - 1
    val index = LAPS_VALUES.indexOf(closest).takeIf { it >= 0 } ?: (LAPS_VALUES.size - 1)
    return index
  }

  // Find the index of the value in the list that is closest to the given duration
  fun findDurationIndex(duration: Duration, isRest: Boolean = false): Int {
    val values = if (isRest) REST_DURATION_VALUES else DURATION_VALUES
    val closest =
      values.minByOrNull { abs(it.inWholeMilliseconds - duration.inWholeMilliseconds) }
        ?: return values.size - 1
    val index = values.indexOf(closest).takeIf { it >= 0 } ?: (values.size - 1)
    return index
  }
}
