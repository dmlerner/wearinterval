package com.wearinterval.domain.model

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class TimerStateTest {

  private val testConfig =
    TimerConfiguration(
      laps = 5,
      workDuration = 60.seconds,
      restDuration = 30.seconds,
    )

  @Test
  fun `isRunning returns true for Running and Resting phases`() {
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    val restingState = runningState.copy(phase = TimerPhase.Resting)

    assertThat(runningState.isRunning).isTrue()
    assertThat(restingState.isRunning).isTrue()
  }

  @Test
  fun `isRunning returns false for non-running phases`() {
    val stoppedState = TimerState.stopped(testConfig)
    val pausedState = stoppedState.copy(phase = TimerPhase.Paused)
    val alarmState = stoppedState.copy(phase = TimerPhase.AlarmActive)

    assertThat(stoppedState.isRunning).isFalse()
    assertThat(pausedState.isRunning).isFalse()
    assertThat(alarmState.isRunning).isFalse()
  }

  @Test
  fun `isResting returns true only for Resting phase`() {
    val restingState =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    val runningState = restingState.copy(phase = TimerPhase.Running)

    assertThat(restingState.isResting).isTrue()
    assertThat(runningState.isResting).isFalse()
  }

  @Test
  fun `progressPercentage calculates correctly for work interval`() {
    val state =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    // 30 seconds remaining of 60 second work interval = 50% progress
    assertThat(state.progressPercentage).isWithin(0.01f).of(0.5f)
  }

  @Test
  fun `progressPercentage calculates correctly for rest interval`() {
    val state =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 10.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    // 10 seconds remaining of 30 second rest interval = 66.67% progress
    assertThat(state.progressPercentage).isWithin(0.01f).of(0.667f)
  }

  @Test
  fun `progressPercentage handles zero duration gracefully`() {
    val configWithNoRest = testConfig.copy(restDuration = 0.seconds)
    val state =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 0.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = configWithNoRest,
      )

    assertThat(state.progressPercentage).isEqualTo(1f)
  }

  @Test
  fun `lapProgressPercentage calculates correctly`() {
    val state =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = testConfig,
      )

    // Lap 3 of 5, during work phase = (3-1)/5 = 0.4
    assertThat(state.lapProgressPercentage).isWithin(0.01f).of(0.4f)
  }

  @Test
  fun `lapProgressPercentage during rest uses current lap`() {
    val state =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = testConfig,
      )

    // Lap 3 of 5, during rest phase = 3/5 = 0.6
    assertThat(state.lapProgressPercentage).isWithin(0.01f).of(0.6f)
  }

  @Test
  fun `isInfinite returns true for 999 laps`() {
    val infiniteConfig = testConfig.copy(laps = 999)
    val state = TimerState.stopped(infiniteConfig)

    assertThat(state.isInfinite).isTrue()
  }

  @Test
  fun `isInfinite returns false for finite laps`() {
    val state = TimerState.stopped(testConfig)

    assertThat(state.isInfinite).isFalse()
  }

  @Test
  fun `displayCurrentLap shows lap count for finite laps`() {
    val state =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = testConfig,
      )

    assertThat(state.displayCurrentLap).isEqualTo("3/5")
  }

  @Test
  fun `displayCurrentLap shows only current lap for infinite laps`() {
    val infiniteConfig = testConfig.copy(laps = 999)
    val state =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 15,
        totalLaps = 999,
        configuration = infiniteConfig,
      )

    assertThat(state.displayCurrentLap).isEqualTo("15")
  }

  @Test
  fun `stopped creates proper default state`() {
    val state = TimerState.stopped(testConfig)

    assertThat(state.phase).isEqualTo(TimerPhase.Stopped)
    assertThat(state.timeRemaining).isEqualTo(testConfig.workDuration)
    assertThat(state.currentLap).isEqualTo(1)
    assertThat(state.totalLaps).isEqualTo(testConfig.laps)
    assertThat(state.isPaused).isFalse()
    assertThat(state.configuration).isEqualTo(testConfig)
  }

  @Test
  fun `currentInterval returns work duration during work phase`() {
    val state =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    assertThat(state.currentInterval).isEqualTo(testConfig.workDuration)
  }

  @Test
  fun `currentInterval returns rest duration during rest phase`() {
    val state =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 1,
        totalLaps = 5,
        configuration = testConfig,
      )

    assertThat(state.currentInterval).isEqualTo(testConfig.restDuration)
  }

  @Test
  fun `phase specific boolean properties work correctly`() {
    val stoppedState = TimerState.stopped(testConfig)
    val alarmState = stoppedState.copy(phase = TimerPhase.AlarmActive)

    assertThat(stoppedState.isStopped).isTrue()
    assertThat(stoppedState.isAlarmActive).isFalse()

    assertThat(alarmState.isStopped).isFalse()
    assertThat(alarmState.isAlarmActive).isTrue()
  }
}
