package com.wearinterval.domain.model

import com.wearinterval.util.Constants
import com.wearinterval.util.TimeUtils
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimerConfiguration(
  val id: String = UUID.randomUUID().toString(),
  val laps: Int,
  val workDuration: Duration,
  val restDuration: Duration,
  val lastUsed: Long = System.currentTimeMillis(),
) {
  fun isValid(): Boolean {
    return laps in Constants.TimerLimits.MIN_LAPS..Constants.TimerLimits.MAX_LAPS &&
      workDuration >= Constants.TimerLimits.MIN_WORK_DURATION &&
      workDuration <= Constants.TimerLimits.MAX_WORK_DURATION &&
      restDuration >= Constants.TimerLimits.MIN_REST_DURATION &&
      restDuration <= Constants.TimerLimits.MAX_REST_DURATION
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

  fun withUpdatedTimestamp(): TimerConfiguration {
    return copy(lastUsed = System.currentTimeMillis())
  }

  companion object {
    val DEFAULT =
      TimerConfiguration(
        laps = Constants.TimerLimits.INFINITE_LAPS, // 999 = infinite
        workDuration = 1.minutes,
        restDuration = 0.seconds, // no rest
      )

    fun validate(laps: Int, workDuration: Duration, restDuration: Duration): TimerConfiguration {
      val validLaps = laps.coerceIn(Constants.TimerLimits.MIN_LAPS, Constants.TimerLimits.MAX_LAPS)
      val validWorkDuration =
        workDuration.coerceIn(
          Constants.TimerLimits.MIN_WORK_DURATION,
          Constants.TimerLimits.MAX_WORK_DURATION
        )
      val validRestDuration =
        restDuration.coerceIn(
          Constants.TimerLimits.MIN_REST_DURATION,
          Constants.TimerLimits.MAX_REST_DURATION
        )

      return TimerConfiguration(
        laps = validLaps,
        workDuration = validWorkDuration,
        restDuration = validRestDuration,
      )
    }

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
