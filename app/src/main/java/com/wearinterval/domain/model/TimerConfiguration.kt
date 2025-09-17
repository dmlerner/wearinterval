package com.wearinterval.domain.model

import com.wearinterval.util.Constants
import com.wearinterval.util.TimeUtils
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimerConfiguration(
  val id: String = UUID.randomUUID().toString(),
  val laps: Int,
  val workDuration: Duration,
  val restDuration: Duration,
  val lastUsed: Instant = Instant.EPOCH,
) {
  fun isValid(): Boolean {
    return true
  }

  fun displayString(): String {
    val lapText = if (laps == 1) "" else "$laps x "
    val workText = TimeUtils.formatDuration(workDuration)
    val restText =
      if (restDuration > Constants.TimerLimits.MIN_REST_DURATION)
        " + ${TimeUtils.formatDuration(restDuration)}"
      else ""

    return "$lapText$workText$restText"
  }

  fun shortDisplayString(): String {
    return if (laps == 1) {
      TimeUtils.formatDuration(workDuration)
    } else {
      val infiniteSymbol = if (laps == Constants.TimerLimits.INFINITE_LAPS) "∞" else laps.toString()
      "$infiniteSymbol×${TimeUtils.formatDuration(workDuration)}"
    }
  }

  fun withUpdatedTimestamp(currentTime: Instant): TimerConfiguration {
    return copy(lastUsed = currentTime)
  }

  companion object {
    val DEFAULT =
      TimerConfiguration(
        laps = Constants.TimerLimits.INFINITE_LAPS, // 999 = infinite
        workDuration = 1.minutes,
        restDuration = 0.seconds, // no rest
      )

    val COMMON_PRESETS =
      listOf(
        // Single intervals (timers)
        TimerConfiguration(
          laps = 1,
          workDuration = Constants.TestValues.COMMON_WORK_DURATION,
          restDuration = Constants.TimerLimits.MIN_REST_DURATION,
        ),
        TimerConfiguration(
          laps = 1,
          workDuration = 60.seconds,
          restDuration = Constants.TimerLimits.MIN_REST_DURATION
        ),
        TimerConfiguration(
          laps = 1,
          workDuration = 2.minutes,
          restDuration = Constants.TimerLimits.MIN_REST_DURATION
        ),

        // Interval training
        TimerConfiguration(
          laps = 5,
          workDuration = 45.seconds,
          restDuration = Constants.TestValues.COMMON_REST_DURATION
        ),
        TimerConfiguration(laps = 8, workDuration = 25.seconds, restDuration = 5.seconds),
        TimerConfiguration(
          laps = 10,
          workDuration = Constants.TestValues.COMMON_WORK_DURATION,
          restDuration = Constants.TestValues.COMMON_WORK_DURATION,
        ),
        TimerConfiguration(laps = 20, workDuration = 20.seconds, restDuration = 10.seconds),

        // Infinite workout
        TimerConfiguration(
          laps = Constants.TimerLimits.INFINITE_LAPS,
          workDuration = 25.seconds,
          restDuration = 5.seconds
        ),
      )
  }
}
