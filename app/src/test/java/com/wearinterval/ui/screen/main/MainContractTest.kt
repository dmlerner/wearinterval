package com.wearinterval.ui.screen.main

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class MainContractTest {

  // ================================
  // Phase State Property Tests
  // ================================

  @Test
  fun isRunning_returnsTrueWhenTimerPhaseIsRunning() {
    // Given
    val state = MainUiState(timerPhase = TimerPhase.Running)

    // When/Then
    assertThat(state.isRunning).isTrue()
  }

  @Test
  fun isRunning_returnsFalseWhenTimerPhaseIsNotRunning() {
    // Given
    val phases =
      listOf(TimerPhase.Stopped, TimerPhase.Paused, TimerPhase.Resting, TimerPhase.AlarmActive)

    // When/Then
    phases.forEach { phase ->
      val state = MainUiState(timerPhase = phase)
      assertThat(state.isRunning).isFalse()
    }
  }

  @Test
  fun isResting_returnsTrueWhenTimerPhaseIsResting() {
    // Given
    val state = MainUiState(timerPhase = TimerPhase.Resting)

    // When/Then
    assertThat(state.isResting).isTrue()
  }

  @Test
  fun isResting_returnsFalseWhenTimerPhaseIsNotResting() {
    // Given
    val phases =
      listOf(TimerPhase.Stopped, TimerPhase.Paused, TimerPhase.Running, TimerPhase.AlarmActive)

    // When/Then
    phases.forEach { phase ->
      val state = MainUiState(timerPhase = phase)
      assertThat(state.isResting).isFalse()
    }
  }

  @Test
  fun isStopped_returnsTrueWhenTimerPhaseIsStopped() {
    // Given
    val state = MainUiState(timerPhase = TimerPhase.Stopped)

    // When/Then
    assertThat(state.isStopped).isTrue()
  }

  @Test
  fun isStopped_returnsFalseWhenTimerPhaseIsNotStopped() {
    // Given
    val phases =
      listOf(TimerPhase.Running, TimerPhase.Paused, TimerPhase.Resting, TimerPhase.AlarmActive)

    // When/Then
    phases.forEach { phase ->
      val state = MainUiState(timerPhase = phase)
      assertThat(state.isStopped).isFalse()
    }
  }

  @Test
  fun isAlarmActive_returnsTrueWhenTimerPhaseIsAlarmActive() {
    // Given
    val state = MainUiState(timerPhase = TimerPhase.AlarmActive)

    // When/Then
    assertThat(state.isAlarmActive).isTrue()
  }

  @Test
  fun isAlarmActive_returnsFalseWhenTimerPhaseIsNotAlarmActive() {
    // Given
    val phases =
      listOf(TimerPhase.Running, TimerPhase.Paused, TimerPhase.Resting, TimerPhase.Stopped)

    // When/Then
    phases.forEach { phase ->
      val state = MainUiState(timerPhase = phase)
      assertThat(state.isAlarmActive).isFalse()
    }
  }

  // ================================
  // Current Interval Duration Tests
  // ================================

  @Test
  fun currentIntervalDuration_returnsRestDurationWhenResting() {
    // Given
    val config =
      TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds, restDuration = 30.seconds)
    val state = MainUiState(timerPhase = TimerPhase.Resting, configuration = config)

    // When/Then
    assertThat(state.currentIntervalDuration).isEqualTo(30.seconds)
  }

  @Test
  fun currentIntervalDuration_returnsWorkDurationWhenNotResting() {
    // Given
    val config =
      TimerConfiguration.DEFAULT.copy(workDuration = 90.seconds, restDuration = 15.seconds)
    val phases =
      listOf(TimerPhase.Running, TimerPhase.Paused, TimerPhase.Stopped, TimerPhase.AlarmActive)

    // When/Then
    phases.forEach { phase ->
      val state = MainUiState(timerPhase = phase, configuration = config)
      assertThat(state.currentIntervalDuration).isEqualTo(90.seconds)
    }
  }

  @Test
  fun currentIntervalDuration_handlesZeroDurations() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 0.seconds, restDuration = 0.seconds)

    // When/Then
    val restingState = MainUiState(timerPhase = TimerPhase.Resting, configuration = config)
    assertThat(restingState.currentIntervalDuration).isEqualTo(0.seconds)

    val runningState = MainUiState(timerPhase = TimerPhase.Running, configuration = config)
    assertThat(runningState.currentIntervalDuration).isEqualTo(0.seconds)
  }

  // ================================
  // Interval Progress Percentage Tests
  // ================================

  @Test
  fun intervalProgressPercentage_returnsFullWhenStopped() {
    // Given
    val state =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        timeRemaining = 30.seconds,
        configuration = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
      )

    // When/Then
    assertThat(state.intervalProgressPercentage).isEqualTo(1.0f)
  }

  @Test
  fun intervalProgressPercentage_calculatesCorrectlyDuringRunning() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        timeRemaining = 30.seconds, // Half remaining
        configuration = config
      )

    // When/Then - 30s remaining out of 60s total = 0.5
    assertThat(state.intervalProgressPercentage).isEqualTo(0.5f)
  }

  @Test
  fun intervalProgressPercentage_calculatesCorrectlyDuringResting() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(restDuration = 20.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Resting,
        timeRemaining = 5.seconds, // 5s remaining out of 20s
        configuration = config
      )

    // When/Then - 5s remaining out of 20s total = 0.25
    assertThat(state.intervalProgressPercentage).isEqualTo(0.25f)
  }

  @Test
  fun intervalProgressPercentage_handlesZeroTimeRemaining() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        timeRemaining = 0.seconds,
        configuration = config
      )

    // When/Then - 0s remaining = 0.0
    assertThat(state.intervalProgressPercentage).isEqualTo(0.0f)
  }

  @Test
  fun intervalProgressPercentage_handlesZeroIntervalDuration() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 0.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        timeRemaining = 10.seconds, // More time than duration (edge case)
        configuration = config
      )

    // When/Then - Zero duration means no progress possible
    assertThat(state.intervalProgressPercentage).isEqualTo(0.0f)
  }

  @Test
  fun intervalProgressPercentage_clampsToValidRange() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 30.seconds)

    // Test upper bound clamping
    val stateOverflow =
      MainUiState(
        timerPhase = TimerPhase.Running,
        timeRemaining = 60.seconds, // More than duration
        configuration = config
      )

    // When/Then - Should clamp to 1.0
    assertThat(stateOverflow.intervalProgressPercentage).isEqualTo(1.0f)
  }

  @Test
  fun intervalProgressPercentage_handlesEdgeCaseValues() {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 1.seconds)

    // Test very small remaining time
    val stateSmall =
      MainUiState(
        timerPhase = TimerPhase.Running,
        timeRemaining = 100.seconds, // Much larger than duration
        configuration = config
      )

    // When/Then - Should clamp to 1.0 (coerceIn ensures bounds)
    assertThat(stateSmall.intervalProgressPercentage).isEqualTo(1.0f)
  }

  // ================================
  // Overall Progress Percentage Tests
  // ================================

  @Test
  fun overallProgressPercentage_returnsFullWhenStopped() {
    // Given
    val state =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        currentLap = 3,
        totalLaps = 5,
        timeRemaining = 20.seconds
      )

    // When/Then
    assertThat(state.overallProgressPercentage).isEqualTo(1.0f)
  }

  @Test
  fun overallProgressPercentage_calculatesCorrectlyWithMultipleLaps() {
    // Given - 5 total laps, currently on lap 3, halfway through current interval
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        currentLap = 3,
        totalLaps = 5,
        timeRemaining = 30.seconds, // Half of 60s interval remaining
        configuration = config
      )

    // When/Then
    // Completed laps: 2 out of 5 = 0.4
    // Current lap progress: 0.5 interval done = 0.5/5 = 0.1
    // Total progress = 0.4 + 0.1 = 0.5
    // Overall remaining = 1 - 0.5 = 0.5
    assertThat(state.overallProgressPercentage).isEqualTo(0.5f)
  }

  @Test
  fun overallProgressPercentage_calculatesCorrectlyAtWorkoutStart() {
    // Given - First lap, no time elapsed yet
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        currentLap = 1,
        totalLaps = 3,
        timeRemaining = 60.seconds, // Full interval remaining
        configuration = config
      )

    // When/Then
    // Completed laps: 0 out of 3 = 0.0
    // Current lap progress: 0 interval done = 0.0/3 = 0.0
    // Total progress = 0.0 + 0.0 = 0.0
    // Overall remaining = 1 - 0.0 = 1.0 (full workout remaining)
    assertThat(state.overallProgressPercentage).isEqualTo(1.0f)
  }

  @Test
  fun overallProgressPercentage_calculatesCorrectlyNearWorkoutEnd() {
    // Given - Final lap, almost complete
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        currentLap = 5,
        totalLaps = 5,
        timeRemaining = 5.seconds, // Almost done with final interval
        configuration = config
      )

    // When/Then
    // Completed laps: 4 out of 5 = 0.8
    // Current lap progress: 55/60 done = (55/60)/5 = 0.183...
    // Total progress = 0.8 + 0.183 = 0.983
    // Overall remaining = 1 - 0.983 = 0.017 (very little remaining)
    val result = state.overallProgressPercentage
    assertThat(result).isLessThan(0.05f) // Very close to 0
    assertThat(result).isGreaterThan(0.0f) // But not quite 0
  }

  @Test
  fun overallProgressPercentage_handlesZeroTotalLaps() {
    // Given
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        currentLap = 1,
        totalLaps = 0, // Edge case
        timeRemaining = 30.seconds
      )

    // When/Then - Zero total laps = no progress possible
    assertThat(state.overallProgressPercentage).isEqualTo(0.0f)
  }

  @Test
  fun overallProgressPercentage_clampsToValidRange() {
    // Given - Edge case where current lap exceeds total
    val config = TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Running,
        currentLap = 10, // More than total laps
        totalLaps = 5,
        timeRemaining = 0.seconds,
        configuration = config
      )

    // When/Then - Should clamp to valid range [0, 1]
    val result = state.overallProgressPercentage
    assertThat(result).isAtLeast(0.0f)
    assertThat(result).isAtMost(1.0f)
  }

  @Test
  fun overallProgressPercentage_handlesRestingPhase() {
    // Given - Resting phase with specific rest duration
    val config =
      TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds, restDuration = 20.seconds)
    val state =
      MainUiState(
        timerPhase = TimerPhase.Resting,
        currentLap = 2,
        totalLaps = 4,
        timeRemaining = 10.seconds, // Half of rest period remaining
        configuration = config
      )

    // When/Then - Should calculate progress based on rest duration
    // Completed laps: 1 out of 4 = 0.25
    // Current rest progress: 10s remaining of 20s = 0.5 progress = 0.5/4 = 0.125
    // Total progress = 0.25 + 0.125 = 0.375
    // Overall remaining = 1 - 0.375 = 0.625
    assertThat(state.overallProgressPercentage).isEqualTo(0.625f)
  }

  // ================================
  // State Consistency Tests
  // ================================

  @Test
  fun progressPercentages_maintainConsistency() {
    // Given - Multiple states to verify consistency
    val config =
      TimerConfiguration.DEFAULT.copy(workDuration = 60.seconds, restDuration = 30.seconds)

    val states =
      listOf(
        // Start of workout
        MainUiState(
          timerPhase = TimerPhase.Running,
          timeRemaining = 60.seconds,
          currentLap = 1,
          totalLaps = 3,
          configuration = config
        ),
        // Middle of workout
        MainUiState(
          timerPhase = TimerPhase.Resting,
          timeRemaining = 15.seconds,
          currentLap = 2,
          totalLaps = 3,
          configuration = config
        ),
        // End of workout
        MainUiState(
          timerPhase = TimerPhase.Running,
          timeRemaining = 5.seconds,
          currentLap = 3,
          totalLaps = 3,
          configuration = config
        )
      )

    // When/Then - All progress percentages should be valid
    states.forEach { state ->
      assertThat(state.intervalProgressPercentage).isAtLeast(0.0f)
      assertThat(state.intervalProgressPercentage).isAtMost(1.0f)
      assertThat(state.overallProgressPercentage).isAtLeast(0.0f)
      assertThat(state.overallProgressPercentage).isAtMost(1.0f)

      // Interval progress should be more granular than overall progress
      if (!state.isStopped && state.totalLaps > 1) {
        // Overall progress should change more slowly than interval progress
        // (This is a consistency check, not a strict mathematical relationship)
        assertThat(state.intervalProgressPercentage).isAtLeast(0.0f)
        assertThat(state.overallProgressPercentage).isAtLeast(0.0f)
      }
    }
  }

  @Test
  fun mainUiState_defaultValuesAreConsistent() {
    // Given
    val defaultState = MainUiState()

    // When/Then - Default state should have sensible values
    assertThat(defaultState.isLoading).isTrue()
    assertThat(defaultState.isStopped).isTrue()
    assertThat(defaultState.currentLap).isEqualTo(0)
    assertThat(defaultState.totalLaps).isEqualTo(1)
    assertThat(defaultState.isPaused).isFalse()
    assertThat(defaultState.isPlayButtonEnabled).isTrue()
    assertThat(defaultState.isStopButtonEnabled).isFalse()
    assertThat(defaultState.isServiceBound).isFalse()
    assertThat(defaultState.flashScreen).isFalse()
    assertThat(defaultState.intervalProgressPercentage).isEqualTo(1.0f) // Full when stopped
    assertThat(defaultState.overallProgressPercentage).isEqualTo(1.0f) // Full when stopped
  }
}
