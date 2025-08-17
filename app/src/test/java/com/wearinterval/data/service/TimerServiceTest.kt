package com.wearinterval.data.service

import android.os.PowerManager
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.wearos.notification.TimerNotificationManager
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class TimerServiceTest {

  private lateinit var timerService: TimerService
  private lateinit var mockTimerNotificationManager: TimerNotificationManager
  private lateinit var mockSettingsRepository: SettingsRepository
  private lateinit var mockPowerManager: PowerManager
  private lateinit var mockWakeLock: PowerManager.WakeLock

  @Before
  fun setup() {
    mockTimerNotificationManager = mockk(relaxed = true)
    mockSettingsRepository = mockk(relaxed = true)
    mockPowerManager = mockk(relaxed = true)
    mockWakeLock = mockk(relaxed = true)

    // Set up default settings
    every { mockSettingsRepository.notificationSettings } returns
      MutableStateFlow(NotificationSettings.DEFAULT)
    every { mockPowerManager.newWakeLock(any(), any()) } returns mockWakeLock

    timerService =
      TimerService().apply {
        timerNotificationManager = mockTimerNotificationManager
        settingsRepository = mockSettingsRepository
        powerManager = mockPowerManager
      }
  }

  @Test
  fun initialStateIsStopped() = runTest {
    // When/Then
    timerService.timerState.test {
      val state = awaitItem()
      assertThat(state.phase).isEqualTo(TimerPhase.Stopped)
      assertThat(state.isPaused).isFalse()
    }
  }

  @Test
  fun startTimerSetsRunningState() = runTest {
    // Given
    val config =
      TimerConfiguration(
        id = "test",
        laps = 5,
        workDuration = 60.seconds,
        restDuration = 30.seconds,
        lastUsed = 1000L,
      )

    // When/Then
    timerService.timerState.test {
      // Skip initial stopped state
      skipItems(1)

      timerService.startTimer(config)

      val state = awaitItem()
      assertThat(state.phase).isEqualTo(TimerPhase.Running)
      assertThat(state.timeRemaining).isEqualTo(60.seconds)
      assertThat(state.currentLap).isEqualTo(1)
      assertThat(state.totalLaps).isEqualTo(5)
      assertThat(state.isPaused).isFalse()
      assertThat(state.configuration).isEqualTo(config)
    }
  }

  @Test
  fun startTimerThrowsExceptionWhenAlreadyRunning() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT
    timerService.startTimer(config)

    // When/Then
    try {
      timerService.startTimer(config)
      assertThat(false).isTrue() // Should not reach here
    } catch (e: IllegalStateException) {
      assertThat(e.message).isEqualTo("Timer is already running")
    }
  }

  @Test
  fun pauseTimerSetsPausedStateFromRunning() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT

    // When/Then
    timerService.timerState.test {
      // Initial stopped state
      val initial = awaitItem()
      assertThat(initial.phase).isEqualTo(TimerPhase.Stopped)

      timerService.startTimer(config)

      // Running state after start
      val running = awaitItem()
      assertThat(running.phase).isEqualTo(TimerPhase.Running)

      timerService.pauseTimer()

      // Paused state after pause
      val paused = awaitItem()
      assertThat(paused.phase).isEqualTo(TimerPhase.Paused)
      assertThat(paused.isPaused).isTrue()
    }
  }

  @Test
  fun pauseTimerSetsPausedStateFromResting() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT
    timerService.startTimer(config)

    // Manually set to resting state for test
    timerService.setTimerStateForTesting(
      timerService.timerState.value.copy(
        phase = TimerPhase.Resting,
      ),
    )

    // When/Then
    timerService.timerState.test {
      skipItems(1)

      timerService.pauseTimer()

      val state = awaitItem()
      assertThat(state.phase).isEqualTo(TimerPhase.Paused)
      assertThat(state.isPaused).isTrue()
    }
  }

  @Test
  fun resumeTimerFromPausedRestoresRunningState() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT

    // When/Then
    timerService.timerState.test {
      // Initial stopped state
      val initial = awaitItem()
      assertThat(initial.phase).isEqualTo(TimerPhase.Stopped)

      timerService.startTimer(config)

      // Running state after start
      val running = awaitItem()
      assertThat(running.phase).isEqualTo(TimerPhase.Running)

      timerService.pauseTimer()

      // Paused state after pause
      val paused = awaitItem()
      assertThat(paused.phase).isEqualTo(TimerPhase.Paused)

      timerService.resumeTimer()

      // Running state after resume
      val resumed = awaitItem()
      assertThat(resumed.phase).isEqualTo(TimerPhase.Running)
      assertThat(resumed.isPaused).isFalse()
    }
  }

  @Test
  fun resumeTimerFromPausedRestoresRestingState() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT

    // When/Then
    timerService.timerState.test {
      // Initial stopped state
      val initial = awaitItem()
      assertThat(initial.phase).isEqualTo(TimerPhase.Stopped)

      timerService.startTimer(config)

      // Running state after start
      val running = awaitItem()
      assertThat(running.phase).isEqualTo(TimerPhase.Running)

      // Set to resting state for test
      timerService.setTimerStateForTesting(
        running.copy(phase = TimerPhase.Resting),
      )

      // Resting state after manual set
      val resting = awaitItem()
      assertThat(resting.phase).isEqualTo(TimerPhase.Resting)

      timerService.pauseTimer()

      // Paused state after pause
      val paused = awaitItem()
      assertThat(paused.phase).isEqualTo(TimerPhase.Paused)

      timerService.resumeTimer()

      // Resting state after resume
      val resumed = awaitItem()
      assertThat(resumed.phase).isEqualTo(TimerPhase.Resting)
      assertThat(resumed.isPaused).isFalse()
      assertThat(resumed.isResting).isTrue()
    }
  }

  @Test
  fun stopTimerResetsToStoppedState() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT

    // When/Then
    timerService.timerState.test {
      // Initial stopped state
      val initial = awaitItem()
      assertThat(initial.phase).isEqualTo(TimerPhase.Stopped)

      timerService.startTimer(config)

      // Running state after start
      val running = awaitItem()
      assertThat(running.phase).isEqualTo(TimerPhase.Running)

      timerService.stopTimer()

      // Stopped state after stop
      val stopped = awaitItem()
      assertThat(stopped).isEqualTo(TimerState.stopped())
    }
  }

  @Test
  fun dismissAlarmFromAlarmActiveState() = runTest {
    // Given
    val config = TimerConfiguration.DEFAULT.copy(laps = 1) // Single lap for completion
    timerService.startTimer(config)

    // Manually set to alarm active state after final lap completion
    timerService.setTimerStateForTesting(
      timerService.timerState.value.copy(
        phase = TimerPhase.AlarmActive,
        isPaused = true,
        currentLap = 2, // Past the final lap to trigger completion
      ),
    )

    // When/Then
    timerService.timerState.test {
      skipItems(1) // Skip initial state

      timerService.dismissAlarm()

      // In manual mode after final lap, dismissAlarm should stop the timer
      val state = awaitItem()
      assertThat(state.phase).isEqualTo(TimerPhase.Stopped)
    }
  }

  @Test
  fun pauseTimerOnlyWorksFromRunningOrRestingStates() = runTest {
    // Given - Timer in stopped state
    val originalState = timerService.timerState.value

    // When
    timerService.pauseTimer()

    // Then - State should not change
    timerService.timerState.test {
      val state = awaitItem()
      assertThat(state).isEqualTo(originalState)
    }
  }

  @Test
  fun resumeTimerOnlyWorksFromPausedState() = runTest {
    // Given - Timer in stopped state
    val originalState = timerService.timerState.value

    // When
    timerService.resumeTimer()

    // Then - State should not change
    timerService.timerState.test {
      val state = awaitItem()
      assertThat(state).isEqualTo(originalState)
    }
  }

  @Test
  fun dismissAlarmOnlyWorksFromAlarmActiveState() = runTest {
    // Given - Timer in stopped state
    val originalState = timerService.timerState.value

    // When
    timerService.dismissAlarm()

    // Then - State should not change
    timerService.timerState.test {
      val state = awaitItem()
      assertThat(state).isEqualTo(originalState)
    }
  }

  @Test
  fun timerBinderReturnsCorrectService() {
    // When
    val binder = timerService.TimerBinder()

    // Then
    assertThat(binder.getService()).isEqualTo(timerService)
  }

  @Test
  fun timerStateFlowIsReadOnly() {
    // Given
    val stateFlow = timerService.timerState

    // When/Then - Should not be able to cast to MutableStateFlow
    assertThat(stateFlow).isNotInstanceOf(kotlinx.coroutines.flow.MutableStateFlow::class.java)
  }

  @Test
  fun timerBinder_providesCorrectService() {
    // Given
    val binder = timerService.TimerBinder()

    // When
    val service = binder.getService()

    // Then
    assertThat(service).isEqualTo(timerService)
    assertThat(service).isInstanceOf(TimerService::class.java)
  }

  @Test
  fun setTimerStateForTesting_updatesState() {
    // Given
    val testState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 3,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    timerService.setTimerStateForTesting(testState)

    // Then
    runTest {
      timerService.timerState.test {
        val state = awaitItem()
        assertThat(state).isEqualTo(testState)
      }
    }
  }

  @Test
  fun serviceLifecycle_handlesPauses() = runTest {
    // Test that service can handle pause/resume through lifecycle events
    val config = TimerConfiguration.DEFAULT

    timerService.timerState.test {
      // Initial state
      val initial = awaitItem()
      assertThat(initial.phase).isEqualTo(TimerPhase.Stopped)

      // Start timer
      timerService.startTimer(config)
      val running = awaitItem()
      assertThat(running.phase).isEqualTo(TimerPhase.Running)

      // Pause timer
      timerService.pauseTimer()
      val paused = awaitItem()
      assertThat(paused.phase).isEqualTo(TimerPhase.Paused)

      // Resume timer
      timerService.resumeTimer()
      val resumed = awaitItem()
      assertThat(resumed.phase).isEqualTo(TimerPhase.Running)

      // Stop timer
      timerService.stopTimer()
      val stopped = awaitItem()
      assertThat(stopped.phase).isEqualTo(TimerPhase.Stopped)
    }
  }
}
