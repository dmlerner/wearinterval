package com.wearinterval.ui.screen.main

import com.wearinterval.domain.model.HeartRateState
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import kotlin.time.Duration

/**
 * UI state for the main timer screen. Combines timer state, configuration, and UI-specific state.
 */
data class MainUiState(
  val isLoading: Boolean = true,
  val timerPhase: TimerPhase = TimerPhase.Stopped,
  val timeRemaining: Duration = Duration.ZERO,
  val currentLap: Int = 0,
  val totalLaps: Int = 1,
  val isPaused: Boolean = false,
  val configuration: TimerConfiguration = TimerConfiguration.DEFAULT,
  val isPlayButtonEnabled: Boolean = true,
  val isStopButtonEnabled: Boolean = false,
  val isServiceBound: Boolean = false,
  val flashScreen: Boolean = false,
  val heartRateState: HeartRateState = HeartRateState.Unavailable,
) {
  val isRunning: Boolean
    get() = timerPhase == TimerPhase.Running

  val isResting: Boolean
    get() = timerPhase == TimerPhase.Resting

  val isStopped: Boolean
    get() = timerPhase == TimerPhase.Stopped

  val isAlarmActive: Boolean
    get() = timerPhase == TimerPhase.AlarmActive

  val currentIntervalDuration: Duration
    get() =
      when {
        isResting -> configuration.restDuration
        else -> configuration.workDuration
      }

  val intervalProgressPercentage: Float
    get() =
      when {
        isStopped -> 1.0f // Show full ring when stopped (ready state)
        isAlarmActive || timeRemaining.inWholeMilliseconds <= 100 ->
          0f // No progress when completed, alarm, or <100ms left
        currentIntervalDuration > Duration.ZERO -> {
          // Progress starts full (1.0) and ticks down to empty (0.0) as time remaining decreases
          (timeRemaining.inWholeMilliseconds.toFloat() /
              currentIntervalDuration.inWholeMilliseconds.toFloat())
            .coerceIn(0f, 1f)
        }
        else -> 0f
      }

  val overallProgressPercentage: Float
    get() =
      when {
        isStopped -> 1.0f // Show full ring when stopped (ready state)
        isAlarmActive || timeRemaining.inWholeMilliseconds <= 100 ->
          0f // No progress when completed, alarm, or <100ms left
        totalLaps > 0 -> {
          // Calculate overall remaining time as a percentage (starts full, ticks down like inner
          // ring)
          // Outer ring shows remaining workout time, ticks down slower by factor of total laps
          val completedLapsProgress = (currentLap - 1).toFloat() / totalLaps.toFloat()
          val currentLapProgress = (1f - intervalProgressPercentage) / totalLaps.toFloat()
          val overallProgress = completedLapsProgress + currentLapProgress

          // Return remaining percentage (1.0 = full workout remaining, 0.0 = workout complete)
          (1f - overallProgress).coerceIn(0f, 1f)
        }
        else -> 0f
      }

  val heartRateBpm: Int?
    get() = (heartRateState as? HeartRateState.Connected)?.bpm

  val showHeartRate: Boolean
    get() = heartRateState !is HeartRateState.Unavailable
}

/** Events that can be triggered from the main screen UI. */
sealed class MainEvent {
  object PlayPauseClicked : MainEvent()

  object StopClicked : MainEvent()

  object DismissAlarm : MainEvent()

  object FlashScreenDismissed : MainEvent()

  object HeartRateClicked : MainEvent()

  data class HeartRatePermissionResult(val granted: Boolean) : MainEvent()
}
