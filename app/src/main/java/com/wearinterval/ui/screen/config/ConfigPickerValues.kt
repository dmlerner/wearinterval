package com.wearinterval.ui.screen.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object ConfigPickerValues {

    // Laps values as defined in design spec
    val LAPS_VALUES = listOf(
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 50, 60, 75, 100, 150, 200, 300, 500, 999,
    )

    // Duration values as defined in design spec
    val DURATION_VALUES = listOf(
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
    )

    // Rest duration values (same as duration but with 0 at start for "no rest")
    val REST_DURATION_VALUES = listOf(0.seconds) + DURATION_VALUES

    // Display text for laps
    fun lapsDisplayText(laps: Int): String = when (laps) {
        999 -> "∞"
        else -> laps.toString()
    }

    // Display text for durations
    fun durationDisplayText(duration: Duration): String = when {
        duration == 0.seconds -> "None"
        duration.inWholeMinutes > 0 -> {
            val minutes = duration.inWholeMinutes
            val seconds = (duration.inWholeSeconds % 60).toInt()
            if (seconds == 0) {
                "$minutes:00"
            } else {
                "$minutes:${seconds.toString().padStart(2, '0')}"
            }
        }
        else -> "${duration.inWholeSeconds}s"
    }

    // Find closest index for a given lap count
    fun findLapsIndex(laps: Int): Int {
        return LAPS_VALUES.indexOfFirst { it >= laps }.takeIf { it >= 0 } ?: (LAPS_VALUES.size - 1)
    }

    // Find closest index for a given duration
    fun findDurationIndex(duration: Duration, isRest: Boolean = false): Int {
        val values = if (isRest) REST_DURATION_VALUES else DURATION_VALUES
        return values.indexOfFirst { it >= duration }.takeIf { it >= 0 } ?: (values.size - 1)
    }
}
