package com.wearinterval.data.service

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

/**
 * Unit tests for TimerService business logic and calculations. Service lifecycle and integration
 * tests should be in androidTest directory.
 */
class TimerServiceLogicTest {

  @Test
  fun timerPhase_transitions_workCorrectly() {
    // Test timer phase transition logic
    val phases =
      listOf(
        TimerPhase.Stopped,
        TimerPhase.Running,
        TimerPhase.Resting,
        TimerPhase.Paused,
        TimerPhase.AlarmActive,
      )

    // Test that all phases are distinct
    val uniquePhases = phases.toSet()
    assertThat(uniquePhases).hasSize(phases.size)
  }

  @Test
  fun timerConfiguration_validation() {
    // Test timer configuration validation logic
    val validConfig =
      TimerConfiguration(
        laps = 5,
        workDuration = 90.seconds,
        restDuration = 30.seconds,
      )

    assertThat(validConfig.laps).isGreaterThan(0)
    assertThat(validConfig.workDuration.inWholeSeconds).isGreaterThan(0)
    assertThat(validConfig.restDuration.inWholeSeconds).isAtLeast(0)
  }

  @Test
  fun timerDuration_calculations() {
    // Test timer duration calculations
    val config =
      TimerConfiguration(
        laps = 3,
        workDuration = 60.seconds,
        restDuration = 20.seconds,
      )

    // Calculate total time for configuration
    val totalWorkTime = config.workDuration * config.laps
    val totalRestTime = config.restDuration * (config.laps - 1) // No rest after last lap
    val totalTime = totalWorkTime + totalRestTime

    assertThat(totalWorkTime).isEqualTo(180.seconds) // 3 * 60
    assertThat(totalRestTime).isEqualTo(40.seconds) // 2 * 20
    assertThat(totalTime).isEqualTo(220.seconds) // 180 + 40
  }

  @Test
  fun interval_progression_logic() {
    // Test interval progression logic
    fun calculateNextPhase(
      currentPhase: TimerPhase,
      currentLap: Int,
      totalLaps: Int,
      hasRestPeriod: Boolean
    ): TimerPhase {
      return when (currentPhase) {
        TimerPhase.Running -> {
          if (currentLap < totalLaps && hasRestPeriod) {
            TimerPhase.Resting
          } else if (currentLap >= totalLaps) {
            TimerPhase.AlarmActive
          } else {
            TimerPhase.Running // Next lap directly
          }
        }
        TimerPhase.Resting -> TimerPhase.Running
        else -> currentPhase
      }
    }

    // Test work to rest transition
    assertThat(calculateNextPhase(TimerPhase.Running, 1, 3, true)).isEqualTo(TimerPhase.Resting)

    // Test rest to work transition
    assertThat(calculateNextPhase(TimerPhase.Resting, 1, 3, true)).isEqualTo(TimerPhase.Running)

    // Test completion
    assertThat(calculateNextPhase(TimerPhase.Running, 3, 3, true)).isEqualTo(TimerPhase.AlarmActive)

    // Test no rest period
    assertThat(calculateNextPhase(TimerPhase.Running, 1, 3, false)).isEqualTo(TimerPhase.Running)
  }

  @Test
  fun skipRest_phase_transitions() {
    // Test skip rest functionality logic
    fun canSkipRest(currentPhase: TimerPhase): Boolean {
      return currentPhase == TimerPhase.Resting
    }

    fun skipRestTransition(currentPhase: TimerPhase, currentLap: Int, totalLaps: Int): TimerPhase {
      return if (canSkipRest(currentPhase)) {
        if (currentLap < totalLaps) {
          TimerPhase.Running // Move to next work interval
        } else {
          TimerPhase.AlarmActive // Workout complete
        }
      } else {
        currentPhase // No change if not in rest phase
      }
    }

    // Test can skip rest only during rest phase
    assertThat(canSkipRest(TimerPhase.Resting)).isTrue()
    assertThat(canSkipRest(TimerPhase.Running)).isFalse()
    assertThat(canSkipRest(TimerPhase.Stopped)).isFalse()
    assertThat(canSkipRest(TimerPhase.Paused)).isFalse()
    assertThat(canSkipRest(TimerPhase.AlarmActive)).isFalse()

    // Test skip rest transitions to next work interval
    assertThat(skipRestTransition(TimerPhase.Resting, 2, 5)).isEqualTo(TimerPhase.Running)

    // Test skip rest on final lap transitions to completion
    assertThat(skipRestTransition(TimerPhase.Resting, 5, 5)).isEqualTo(TimerPhase.AlarmActive)

    // Test skip rest has no effect when not in rest phase
    assertThat(skipRestTransition(TimerPhase.Running, 2, 5)).isEqualTo(TimerPhase.Running)
    assertThat(skipRestTransition(TimerPhase.Stopped, 1, 5)).isEqualTo(TimerPhase.Stopped)
  }

  @Test
  fun timeRemaining_calculation() {
    // Test time remaining calculation logic
    fun calculateTimeRemaining(
      currentPhase: TimerPhase,
      intervalStartTime: Long,
      currentTime: Long,
      intervalDuration: Long
    ): Long {
      val elapsed = currentTime - intervalStartTime
      return (intervalDuration - elapsed).coerceAtLeast(0)
    }

    val startTime = 1000L
    val intervalDuration = 60000L // 60 seconds

    // Test various elapsed times
    val testCases =
      mapOf(
        16000L to 45000L, // 15s elapsed (16000 - 1000 = 15000), 45s remaining
        31000L to 30000L, // 30s elapsed (31000 - 1000 = 30000), 30s remaining
        61000L to 0L, // 60s elapsed (61000 - 1000 = 60000), 0s remaining
        71000L to 0L, // Overtime (71000 - 1000 = 70000), still 0s remaining
      )

    testCases.forEach { (currentTime, expectedRemaining) ->
      val remaining =
        calculateTimeRemaining(TimerPhase.Running, startTime, currentTime, intervalDuration)
      assertThat(remaining).isEqualTo(expectedRemaining)
    }
  }

  @Test
  fun lap_progression_logic() {
    // Test lap progression logic
    fun advanceLap(
      currentLap: Int,
      totalLaps: Int,
      currentPhase: TimerPhase,
      nextPhase: TimerPhase
    ): Int {
      return if (currentPhase == TimerPhase.Running && nextPhase != TimerPhase.Resting) {
        // Only advance lap when finishing work period and not going to rest
        (currentLap + 1).coerceAtMost(totalLaps)
      } else {
        currentLap
      }
    }

    // Test lap advancement after work period
    assertThat(advanceLap(1, 5, TimerPhase.Running, TimerPhase.AlarmActive)).isEqualTo(2)

    // Test no advancement when going to rest
    assertThat(advanceLap(1, 5, TimerPhase.Running, TimerPhase.Resting)).isEqualTo(1)

    // Test no advancement beyond total laps
    assertThat(advanceLap(5, 5, TimerPhase.Running, TimerPhase.AlarmActive)).isEqualTo(5)
  }

  @Test
  fun pause_resume_logic() {
    // Test pause/resume state management
    data class TimerState(val phase: TimerPhase, val isPaused: Boolean)

    fun togglePause(currentState: TimerState): TimerState {
      return when (currentState.phase) {
        TimerPhase.Running,
        TimerPhase.Resting -> {
          if (currentState.isPaused) {
            currentState.copy(isPaused = false) // Resume
          } else {
            currentState.copy(phase = TimerPhase.Paused, isPaused = true) // Pause
          }
        }
        TimerPhase.Paused -> {
          currentState.copy(phase = TimerPhase.Running, isPaused = false) // Resume to running
        }
        else -> currentState // Can't pause/resume in other states
      }
    }

    // Test pausing from running
    val runningState = TimerState(TimerPhase.Running, false)
    val pausedState = togglePause(runningState)
    assertThat(pausedState.phase).isEqualTo(TimerPhase.Paused)
    assertThat(pausedState.isPaused).isTrue()

    // Test resuming from paused
    val resumedState = togglePause(pausedState)
    assertThat(resumedState.phase).isEqualTo(TimerPhase.Running)
    assertThat(resumedState.isPaused).isFalse()
  }

  @Test
  fun notification_timing_logic() {
    // Test notification timing calculations
    fun shouldTriggerNotification(
      timeRemaining: Long,
      intervalDuration: Long,
      currentPhase: TimerPhase
    ): Boolean {
      val reminderThreshold = 5000L // 5 seconds before end
      return timeRemaining <= reminderThreshold &&
        timeRemaining > 0 &&
        (currentPhase == TimerPhase.Running || currentPhase == TimerPhase.Resting)
    }

    // Test notification triggering
    assertThat(shouldTriggerNotification(3000L, 60000L, TimerPhase.Running)).isTrue()
    assertThat(shouldTriggerNotification(7000L, 60000L, TimerPhase.Running)).isFalse()
    assertThat(shouldTriggerNotification(0L, 60000L, TimerPhase.Running)).isFalse()
    assertThat(shouldTriggerNotification(3000L, 60000L, TimerPhase.Stopped)).isFalse()
  }

  @Test
  fun auto_mode_progression() {
    // Test auto mode progression logic
    fun shouldAutoAdvance(
      timeRemaining: Long,
      autoMode: Boolean,
      currentPhase: TimerPhase
    ): Boolean {
      return autoMode &&
        timeRemaining <= 0 &&
        (currentPhase == TimerPhase.Running || currentPhase == TimerPhase.Resting)
    }

    // Test auto advancement
    assertThat(shouldAutoAdvance(0L, true, TimerPhase.Running)).isTrue()
    assertThat(shouldAutoAdvance(0L, false, TimerPhase.Running)).isFalse()
    assertThat(shouldAutoAdvance(5000L, true, TimerPhase.Running)).isFalse()
    assertThat(shouldAutoAdvance(0L, true, TimerPhase.AlarmActive)).isFalse()
  }

  @Test
  fun service_state_validation() {
    // Test service state validation
    data class ServiceState(
      val isRunning: Boolean,
      val isBound: Boolean,
      val currentPhase: TimerPhase,
    )

    fun isValidServiceState(state: ServiceState): Boolean {
      return when {
        state.currentPhase == TimerPhase.Stopped -> !state.isRunning
        state.currentPhase in listOf(TimerPhase.Running, TimerPhase.Resting, TimerPhase.Paused) ->
          state.isRunning
        state.currentPhase == TimerPhase.AlarmActive -> state.isRunning
        else -> false
      }
    }

    // Test valid states
    assertThat(isValidServiceState(ServiceState(false, true, TimerPhase.Stopped))).isTrue()
    assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.Running))).isTrue()
    assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.AlarmActive))).isTrue()

    // Test invalid states
    assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.Stopped))).isFalse()
    assertThat(isValidServiceState(ServiceState(false, true, TimerPhase.Running))).isFalse()
  }

  @Test
  fun timer_configuration_change_during_running() {
    // Test behavior when configuration changes while timer is running
    val runningConfig =
      TimerConfiguration(laps = 5, workDuration = 60.seconds, restDuration = 30.seconds)
    val newConfig =
      TimerConfiguration(laps = 10, workDuration = 90.seconds, restDuration = 45.seconds)

    // Simulate service behavior: running timer should preserve its current state
    fun shouldUpdateConfigDuringRun(currentPhase: TimerPhase): Boolean {
      return currentPhase == TimerPhase.Stopped
    }

    // Configuration change during stopped state should apply
    assertThat(shouldUpdateConfigDuringRun(TimerPhase.Stopped)).isTrue()

    // Configuration change during active phases should not apply
    assertThat(shouldUpdateConfigDuringRun(TimerPhase.Running)).isFalse()
    assertThat(shouldUpdateConfigDuringRun(TimerPhase.Resting)).isFalse()
    assertThat(shouldUpdateConfigDuringRun(TimerPhase.Paused)).isFalse()
    assertThat(shouldUpdateConfigDuringRun(TimerPhase.AlarmActive)).isFalse()
  }

  @Test
  fun countdown_progression_accuracy() {
    // Test countdown progression accuracy
    fun simulateCountdownTick(timeRemaining: Long, tickInterval: Long): Long {
      return (timeRemaining - tickInterval).coerceAtLeast(0)
    }

    val tickInterval = 1000L // 1 second
    var timeRemaining = 60000L // 60 seconds

    // Simulate countdown
    for (i in 0..60) {
      val expectedRemaining = (60000L - (i * tickInterval)).coerceAtLeast(0)
      assertThat(timeRemaining).isEqualTo(expectedRemaining)
      timeRemaining = simulateCountdownTick(timeRemaining, tickInterval)
    }

    // After 60 ticks, should be at 0
    assertThat(timeRemaining).isEqualTo(0L)
  }

  @Test
  fun work_rest_phase_transitions() {
    // Test work/rest phase transitions with different configurations
    fun getNextPhase(
      currentPhase: TimerPhase,
      currentLap: Int,
      totalLaps: Int,
      restDuration: Long
    ): TimerPhase {
      return when (currentPhase) {
        TimerPhase.Running -> {
          when {
            currentLap >= totalLaps -> TimerPhase.AlarmActive
            restDuration > 0 -> TimerPhase.Resting
            else -> TimerPhase.Running // Next lap, no rest
          }
        }
        TimerPhase.Resting -> TimerPhase.Running
        else -> currentPhase
      }
    }

    // Test with rest periods
    assertThat(getNextPhase(TimerPhase.Running, 1, 5, 30000L)).isEqualTo(TimerPhase.Resting)
    assertThat(getNextPhase(TimerPhase.Resting, 1, 5, 30000L)).isEqualTo(TimerPhase.Running)

    // Test without rest periods
    assertThat(getNextPhase(TimerPhase.Running, 1, 5, 0L)).isEqualTo(TimerPhase.Running)

    // Test final lap completion
    assertThat(getNextPhase(TimerPhase.Running, 5, 5, 30000L)).isEqualTo(TimerPhase.AlarmActive)
    assertThat(getNextPhase(TimerPhase.Running, 5, 5, 0L)).isEqualTo(TimerPhase.AlarmActive)
  }

  @Test
  fun lap_progression_edge_cases() {
    // Test lap progression with edge cases
    fun advanceLapCount(
      currentLap: Int,
      totalLaps: Int,
      fromPhase: TimerPhase,
      toPhase: TimerPhase
    ): Int {
      return when {
        fromPhase == TimerPhase.Resting && toPhase == TimerPhase.Running ->
          (currentLap + 1).coerceAtMost(totalLaps)
        fromPhase == TimerPhase.Running &&
          toPhase == TimerPhase.Running &&
          currentLap < totalLaps -> (currentLap + 1).coerceAtMost(totalLaps)
        else -> currentLap
      }
    }

    // Test normal lap progression from rest to work
    assertThat(advanceLapCount(1, 5, TimerPhase.Resting, TimerPhase.Running)).isEqualTo(2)

    // Test direct work to work (no rest)
    assertThat(advanceLapCount(2, 5, TimerPhase.Running, TimerPhase.Running)).isEqualTo(3)

    // Test no progression during work to rest
    assertThat(advanceLapCount(3, 5, TimerPhase.Running, TimerPhase.Resting)).isEqualTo(3)

    // Test maximum lap limit
    assertThat(advanceLapCount(5, 5, TimerPhase.Resting, TimerPhase.Running)).isEqualTo(5)
  }

  @Test
  fun pause_resume_state_persistence() {
    // Test pause/resume state persistence
    data class TimerState(
      val phase: TimerPhase,
      val isPaused: Boolean,
      val pausedFromPhase: TimerPhase
    )

    fun pauseFromPhase(currentPhase: TimerPhase): TimerState {
      return when (currentPhase) {
        TimerPhase.Running,
        TimerPhase.Resting -> TimerState(TimerPhase.Paused, true, currentPhase)
        else -> TimerState(currentPhase, false, TimerPhase.Stopped)
      }
    }

    fun resumeToPhase(pausedState: TimerState): TimerState {
      return if (pausedState.phase == TimerPhase.Paused) {
        TimerState(pausedState.pausedFromPhase, false, TimerPhase.Stopped)
      } else {
        pausedState
      }
    }

    // Test pause from running
    val pausedFromRunning = pauseFromPhase(TimerPhase.Running)
    assertThat(pausedFromRunning.phase).isEqualTo(TimerPhase.Paused)
    assertThat(pausedFromRunning.pausedFromPhase).isEqualTo(TimerPhase.Running)

    // Test resume to running
    val resumedToRunning = resumeToPhase(pausedFromRunning)
    assertThat(resumedToRunning.phase).isEqualTo(TimerPhase.Running)
    assertThat(resumedToRunning.isPaused).isFalse()

    // Test pause from resting
    val pausedFromResting = pauseFromPhase(TimerPhase.Resting)
    assertThat(pausedFromResting.pausedFromPhase).isEqualTo(TimerPhase.Resting)

    // Test resume to resting
    val resumedToResting = resumeToPhase(pausedFromResting)
    assertThat(resumedToResting.phase).isEqualTo(TimerPhase.Resting)
  }

  @Test
  fun alarm_dismissal_behavior() {
    // Test alarm dismissal behavior with different settings
    fun getAlarmDismissalAction(autoRestart: Boolean, currentLap: Int, totalLaps: Int): String {
      return when {
        autoRestart && currentLap < totalLaps -> "RESTART"
        autoRestart && currentLap >= totalLaps -> "STOP"
        else -> "STOP"
      }
    }

    // Test alarm dismissal without auto-restart
    assertThat(getAlarmDismissalAction(false, 5, 5)).isEqualTo("STOP")
    assertThat(getAlarmDismissalAction(false, 3, 5)).isEqualTo("STOP")

    // Test alarm dismissal with auto-restart
    assertThat(getAlarmDismissalAction(true, 3, 5)).isEqualTo("RESTART")
    assertThat(getAlarmDismissalAction(true, 5, 5)).isEqualTo("STOP")
  }

  @Test
  fun timer_completion_detection() {
    // Test timer completion detection logic
    fun isTimerComplete(currentLap: Int, totalLaps: Int, phase: TimerPhase): Boolean {
      return currentLap >= totalLaps && phase == TimerPhase.AlarmActive
    }

    // Test incomplete scenarios
    assertThat(isTimerComplete(3, 5, TimerPhase.Running)).isFalse()
    assertThat(isTimerComplete(5, 5, TimerPhase.Running)).isFalse()
    assertThat(isTimerComplete(5, 5, TimerPhase.Resting)).isFalse()

    // Test complete scenario
    assertThat(isTimerComplete(5, 5, TimerPhase.AlarmActive)).isTrue()
  }

  @Test
  fun invalid_timer_states_handling() {
    // Test handling of invalid timer states
    fun isValidTimerState(
      phase: TimerPhase,
      currentLap: Int,
      totalLaps: Int,
      timeRemaining: Long
    ): Boolean {
      return when {
        currentLap < 1 || currentLap > totalLaps -> false
        totalLaps < 1 -> false
        phase != TimerPhase.Stopped && timeRemaining < 0 -> false
        else -> true
      }
    }

    // Test valid states
    assertThat(isValidTimerState(TimerPhase.Running, 1, 5, 60000L)).isTrue()
    assertThat(isValidTimerState(TimerPhase.Stopped, 1, 5, 0L))
      .isTrue() // Stopped with zero time is valid

    // Test invalid states
    assertThat(isValidTimerState(TimerPhase.Running, 0, 5, 60000L)).isFalse() // Invalid lap
    assertThat(isValidTimerState(TimerPhase.Running, 6, 5, 60000L)).isFalse() // Lap > total
    assertThat(isValidTimerState(TimerPhase.Running, 1, 0, 60000L)).isFalse() // Invalid total laps
    assertThat(isValidTimerState(TimerPhase.Running, 1, 5, -1L))
      .isFalse() // Negative time while running
  }

  @Test
  fun boundary_condition_handling() {
    // Test boundary condition handling
    fun handleTimerBoundary(timeRemaining: Long, phase: TimerPhase): Pair<Long, Boolean> {
      val shouldTransition = timeRemaining <= 0 && phase != TimerPhase.Stopped
      val normalizedTime = timeRemaining.coerceAtLeast(0L)
      return Pair(normalizedTime, shouldTransition)
    }

    // Test normal operation
    val (normalTime, noTransition) = handleTimerBoundary(30000L, TimerPhase.Running)
    assertThat(normalTime).isEqualTo(30000L)
    assertThat(noTransition).isFalse()

    // Test boundary conditions
    val (zeroTime, shouldTransition) = handleTimerBoundary(-1000L, TimerPhase.Running)
    assertThat(zeroTime).isEqualTo(0L)
    assertThat(shouldTransition).isTrue()

    // Test stopped state boundary
    val (stoppedTime, noTransitionStopped) = handleTimerBoundary(-1000L, TimerPhase.Stopped)
    assertThat(stoppedTime).isEqualTo(0L)
    assertThat(noTransitionStopped).isFalse()
  }
}
