package com.wearinterval.ui.screen.main

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.FakeTimeProvider
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockTimerRepository = mockk<TimerRepository>(relaxed = true)
  private val mockConfigurationRepository = mockk<ConfigurationRepository>()
  private val mockSettingsRepository = mockk<SettingsRepository>()
  private val fakeTimeProvider = FakeTimeProvider()

  private val timerStateFlow = MutableStateFlow(TimerState.stopped())
  private val configurationFlow = MutableStateFlow(TimerConfiguration.DEFAULT)
  private val isServiceBoundFlow = MutableStateFlow(false)
  private val notificationSettingsFlow = MutableStateFlow(NotificationSettings.DEFAULT)

  private lateinit var viewModel: MainViewModel

  @Before
  fun setup() {
    every { mockTimerRepository.timerState } returns timerStateFlow
    every { mockConfigurationRepository.currentConfiguration } returns configurationFlow
    every { mockTimerRepository.isServiceBound } returns isServiceBoundFlow
    every { mockSettingsRepository.notificationSettings } returns notificationSettingsFlow

    coEvery { mockTimerRepository.startTimer() } returns Result.success(Unit)
    coEvery { mockTimerRepository.pauseTimer() } returns Result.success(Unit)
    coEvery { mockTimerRepository.resumeTimer() } returns Result.success(Unit)
    coEvery { mockTimerRepository.stopTimer() } returns Result.success(Unit)
    coEvery { mockTimerRepository.dismissAlarm() } returns Result.success(Unit)
    coEvery { mockTimerRepository.skipRest() } returns Result.success(Unit)

    viewModel =
      MainViewModel(
        timerRepository = mockTimerRepository,
        configurationRepository = mockConfigurationRepository,
        settingsRepository = mockSettingsRepository,
        timeProvider = fakeTimeProvider,
      )
  }

  @Test
  fun `initial ui state is correct`() = runTest {
    viewModel.uiState.test {
      val initialState = awaitItem()

      assertThat(initialState.timerPhase).isEqualTo(TimerPhase.Stopped)
      assertThat(initialState.timeRemaining).isEqualTo(TimerConfiguration.DEFAULT.workDuration)
      assertThat(initialState.currentLap).isEqualTo(1)
      assertThat(initialState.totalLaps).isEqualTo(999)
      assertThat(initialState.isPaused).isFalse()
      assertThat(initialState.configuration).isEqualTo(TimerConfiguration.DEFAULT)
      assertThat(initialState.isPlayButtonEnabled).isFalse()
      assertThat(initialState.isStopButtonEnabled).isFalse()
      assertThat(initialState.isServiceBound).isFalse()
      assertThat(initialState.flashScreen).isFalse()
    }
  }

  @Test
  fun `ui state reflects configuration changes when stopped`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Update configuration with new values
      val customConfig =
        TimerConfiguration(
          id = "custom-config",
          laps = 10,
          workDuration = 90.seconds,
          restDuration = 30.seconds,
          lastUsed = System.currentTimeMillis(),
        )
      configurationFlow.value = customConfig

      val uiState = awaitItem()

      // When stopped, UI should reflect the current configuration
      assertThat(uiState.timerPhase).isEqualTo(TimerPhase.Stopped)
      assertThat(uiState.configuration).isEqualTo(customConfig)
      assertThat(uiState.configuration.laps).isEqualTo(10)
      assertThat(uiState.configuration.workDuration).isEqualTo(90.seconds)
      assertThat(uiState.configuration.restDuration).isEqualTo(30.seconds)

      // Progress rings should show full when stopped (ready state)
      assertThat(uiState.intervalProgressPercentage).isEqualTo(1.0f)
      assertThat(uiState.overallProgressPercentage).isEqualTo(1.0f)
    }
  }

  @Test
  fun `ui state reflects timer state changes when running`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Set up fake time: interval started 15 seconds ago (60s - 15s = 45s remaining)
      val currentTime = 1000L
      val intervalStartTime = currentTime - 15_000L // 15 seconds ago
      fakeTimeProvider.setCurrentTimeMillis(currentTime)

      // Update timer state to running
      val runningState =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 45.seconds, // This value will be recalculated based on intervalStartTime
          currentLap = 3,
          totalLaps = 10,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
          intervalStartTime = intervalStartTime,
        )
      timerStateFlow.value = runningState

      val uiState = awaitItem()
      assertThat(uiState.timerPhase).isEqualTo(TimerPhase.Running)
      assertThat(uiState.timeRemaining).isEqualTo(45.seconds)
      assertThat(uiState.currentLap).isEqualTo(3)
      assertThat(uiState.totalLaps).isEqualTo(10)
      assertThat(uiState.isPaused).isFalse()
      assertThat(uiState.isRunning).isTrue()
      assertThat(uiState.isStopButtonEnabled).isTrue()
    }
  }

  @Test
  fun `ui state reflects service bound changes`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Service becomes bound
      isServiceBoundFlow.value = true

      val uiState = awaitItem()
      assertThat(uiState.isServiceBound).isTrue()
      assertThat(uiState.isPlayButtonEnabled).isTrue()
    }
  }

  @Test
  fun `ui state reflects configuration changes`() = runTest {
    val customConfig =
      TimerConfiguration(
        laps = 5,
        workDuration = 2.minutes,
        restDuration = 30.seconds,
      )

    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Update configuration
      configurationFlow.value = customConfig

      val uiState = awaitItem()
      assertThat(uiState.configuration).isEqualTo(customConfig)
    }
  }

  @Test
  fun `play pause clicked when stopped starts timer`() = runTest {
    // Given - timer is stopped and service is bound
    isServiceBoundFlow.value = true

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then
    coVerify { mockTimerRepository.startTimer() }
  }

  // TODO: Fix these failing verification tests - the mock verification is not working properly
  // @Test
  // fun `play pause clicked when paused calls resume timer`() = runTest {
  //     // Given - service is bound and timer is paused
  //     isServiceBoundFlow.value = true
  //     timerStateFlow.value = TimerState(
  //         phase = TimerPhase.Paused,
  //         timeRemaining = 30.seconds,
  //         currentLap = 1,
  //         totalLaps = 5,
  //         isPaused = true,
  //         configuration = TimerConfiguration.DEFAULT,
  //     )

  //     // When
  //     viewModel.onEvent(MainEvent.PlayPauseClicked)

  //     // Then - verify the method was called (relaxed mock allows this)
  //     coVerify(exactly = 1) { mockTimerRepository.resumeTimer() }
  // }

  // @Test
  // fun `play pause clicked when running pauses timer`() = runTest {
  //     // Given - service is bound and timer is running
  //     isServiceBoundFlow.value = true
  //     timerStateFlow.value = TimerState(
  //         phase = TimerPhase.Running,
  //         timeRemaining = 30.seconds,
  //         currentLap = 1,
  //         totalLaps = 5,
  //         isPaused = false,
  //         configuration = TimerConfiguration.DEFAULT,
  //     )

  //     // When
  //     viewModel.onEvent(MainEvent.PlayPauseClicked)

  //     // Then
  //     coVerify(timeout = 1000) { mockTimerRepository.pauseTimer() }
  // }

  // @Test
  // fun `play pause clicked when resting pauses timer`() = runTest {
  //     // Given - service is bound and timer is in rest phase
  //     isServiceBoundFlow.value = true
  //     timerStateFlow.value = TimerState(
  //         phase = TimerPhase.Resting,
  //         timeRemaining = 15.seconds,
  //         currentLap = 1,
  //         totalLaps = 5,
  //         isPaused = false,
  //         configuration = TimerConfiguration.DEFAULT,
  //     )

  //     // When
  //     viewModel.onEvent(MainEvent.PlayPauseClicked)

  //     // Then
  //     coVerify(timeout = 1000) { mockTimerRepository.pauseTimer() }
  // }

  @Test
  fun `stop clicked stops timer`() = runTest {
    // When
    viewModel.onEvent(MainEvent.StopClicked)

    // Then
    coVerify { mockTimerRepository.stopTimer() }
  }

  @Test
  fun `dismiss alarm event dismisses alarm`() = runTest {
    // When
    viewModel.onEvent(MainEvent.DismissAlarm)

    // Then
    coVerify { mockTimerRepository.dismissAlarm() }
  }

  @Test
  fun `flash screen dismissed event clears flash state`() = runTest {
    // Given - flash is active
    viewModel.triggerFlash()

    viewModel.uiState.test {
      // Wait for flash to be triggered
      var uiState = awaitItem()
      while (!uiState.flashScreen) {
        uiState = awaitItem()
      }
      assertThat(uiState.flashScreen).isTrue()

      // When - flash is dismissed
      viewModel.onEvent(MainEvent.FlashScreenDismissed)

      // Then - flash should be cleared
      uiState = awaitItem()
      assertThat(uiState.flashScreen).isFalse()
    }
  }

  @Test
  fun `trigger flash sets flash screen state`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // When
      viewModel.triggerFlash()

      // Then
      val uiState = awaitItem()
      assertThat(uiState.flashScreen).isTrue()
    }
  }

  @Test
  fun `ui state computed properties work correctly`() = runTest {
    // Set up the timer state first
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Stopped,
        timeRemaining = 60.seconds,
        currentLap = 0,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // Create fresh ViewModel for this test to ensure proper initialization
    val testViewModel =
      MainViewModel(
        timerRepository = mockTimerRepository,
        configurationRepository = mockConfigurationRepository,
        settingsRepository = mockSettingsRepository,
        timeProvider = FakeTimeProvider(),
      )

    // Allow ViewModels and flows to initialize
    advanceUntilIdle()

    testViewModel.uiState.test {
      // Test stopped state
      var uiState = awaitItem()
      assertThat(uiState.isStopped).isTrue()
      assertThat(uiState.isRunning).isFalse()
      assertThat(uiState.isResting).isFalse()
      assertThat(uiState.isAlarmActive).isFalse()

      // Test running state
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 45.seconds,
          currentLap = 2,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isStopped).isFalse()
      assertThat(uiState.isRunning).isTrue()
      assertThat(uiState.isResting).isFalse()
      assertThat(uiState.isAlarmActive).isFalse()

      // Test resting state
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Resting,
          timeRemaining = 30.seconds,
          currentLap = 2,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isStopped).isFalse()
      assertThat(uiState.isRunning).isFalse()
      assertThat(uiState.isResting).isTrue()
      assertThat(uiState.isAlarmActive).isFalse()

      // Test alarm active state
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.AlarmActive,
          timeRemaining = 0.seconds,
          currentLap = 5,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isStopped).isFalse()
      assertThat(uiState.isRunning).isFalse()
      assertThat(uiState.isResting).isFalse()
      assertThat(uiState.isAlarmActive).isTrue()
    }
  }

  @Test
  fun `progress calculations work correctly`() = runTest {
    val config =
      TimerConfiguration(
        laps = 10,
        workDuration = 60.seconds,
        restDuration = 30.seconds,
      )
    configurationFlow.value = config

    viewModel.uiState.test {
      // Skip initial states
      awaitItem()

      // Set up fake time: work interval started 15 seconds ago (60s - 15s = 45s remaining)
      val currentTime = 2000L
      val intervalStartTime = currentTime - 15_000L // 15 seconds ago
      fakeTimeProvider.setCurrentTimeMillis(currentTime)

      // Test work interval progress
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 45.seconds, // 15 seconds elapsed out of 60
          currentLap = 3,
          totalLaps = 10,
          isPaused = false,
          configuration = config,
          intervalStartTime = intervalStartTime,
        )

      var uiState = awaitItem()
      assertThat(uiState.intervalProgressPercentage)
        .isWithin(0.01f)
        .of(0.75f) // 45/60 = 0.75 (remaining time)
      // Overall progress: (2 completed + 0.25 current progress) / 10 = 0.225 progress → 0.775
      // remaining
      assertThat(uiState.overallProgressPercentage).isWithin(0.01f).of(0.775f)

      // Test rest interval progress: want 20s remaining out of 30s total (10s elapsed)
      val restCurrentTime = currentTime + 20_000L // Move to a different time
      val restStartTime = restCurrentTime - 10_000L // Rest started 10 seconds ago
      fakeTimeProvider.setCurrentTimeMillis(restCurrentTime)

      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Resting,
          timeRemaining = 20.seconds, // 10 seconds elapsed out of 30
          currentLap = 3,
          totalLaps = 10,
          isPaused = false,
          configuration = config,
          intervalStartTime = restStartTime,
        )

      uiState = awaitItem()
      assertThat(uiState.intervalProgressPercentage)
        .isWithin(0.01f)
        .of(0.67f) // 20/30 ≈ 0.67 (remaining time)
      // During rest: (2 completed + 0.33 current progress) / 10 = 0.233 progress → 0.767 remaining
      assertThat(uiState.overallProgressPercentage).isWithin(0.01f).of(0.767f)
    }
  }

  @Test
  fun `current interval duration calculation works correctly`() = runTest {
    val config =
      TimerConfiguration(
        laps = 5,
        workDuration = 90.seconds,
        restDuration = 45.seconds,
      )
    configurationFlow.value = config

    viewModel.uiState.test {
      // Skip initial states
      awaitItem()

      // Test work interval duration
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 60.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = false,
          configuration = config,
        )

      var uiState = awaitItem()
      assertThat(uiState.currentIntervalDuration).isEqualTo(90.seconds)

      // Test rest interval duration
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Resting,
          timeRemaining = 30.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = false,
          configuration = config,
        )

      uiState = awaitItem()
      assertThat(uiState.currentIntervalDuration).isEqualTo(45.seconds)
    }
  }

  @Test
  fun `multiple sequential events work correctly`() = runTest {
    // Given - service is bound
    isServiceBoundFlow.value = true

    // When - multiple events in sequence
    viewModel.onEvent(MainEvent.PlayPauseClicked) // Start
    viewModel.onEvent(MainEvent.PlayPauseClicked) // Pause (if running)
    viewModel.onEvent(MainEvent.StopClicked) // Stop

    // Then - all actions should be called
    coVerify { mockTimerRepository.startTimer() }
    coVerify { mockTimerRepository.stopTimer() }
  }

  @Test
  fun `play pause clicked when paused resumes timer`() = runTest {
    // Given - service is bound and timer is paused
    isServiceBoundFlow.value = true
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Paused,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        isPaused = true,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then
    coVerify { mockTimerRepository.resumeTimer() }
  }

  @Test
  fun `play pause clicked when running pauses timer`() = runTest {
    // Given - service is bound and timer is running
    isServiceBoundFlow.value = true
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then
    coVerify { mockTimerRepository.pauseTimer() }
  }

  @Test
  fun `play pause clicked when resting skips rest`() = runTest {
    // Given - service is bound and timer is in rest phase
    isServiceBoundFlow.value = true
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 1,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then
    coVerify { mockTimerRepository.skipRest() }
  }

  @Test
  fun `service disconnection disables play button`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Given - service is bound initially
      isServiceBoundFlow.value = true
      var uiState = awaitItem()
      assertThat(uiState.isPlayButtonEnabled).isTrue()

      // When - service becomes unbound
      isServiceBoundFlow.value = false
      uiState = awaitItem()

      // Then - play button should be disabled
      assertThat(uiState.isPlayButtonEnabled).isFalse()
      assertThat(uiState.isServiceBound).isFalse()
    }
  }

  @Test
  fun `timer repository start timer failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockTimerRepository.startTimer() } returns
      Result.failure(RuntimeException("Service error"))
    isServiceBoundFlow.value = true

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then - should not crash, repository method still called
    coVerify { mockTimerRepository.startTimer() }
  }

  @Test
  fun `timer repository pause timer failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockTimerRepository.pauseTimer() } returns
      Result.failure(RuntimeException("Service error"))
    isServiceBoundFlow.value = true
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then - should not crash, repository method still called
    coVerify { mockTimerRepository.pauseTimer() }
  }

  @Test
  fun `timer repository stop timer failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockTimerRepository.stopTimer() } returns
      Result.failure(RuntimeException("Service error"))

    // When
    viewModel.onEvent(MainEvent.StopClicked)

    // Then - should not crash, repository method still called
    coVerify { mockTimerRepository.stopTimer() }
  }

  @Test
  fun `timer repository dismiss alarm failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockTimerRepository.dismissAlarm() } returns
      Result.failure(RuntimeException("Service error"))

    // When
    viewModel.onEvent(MainEvent.DismissAlarm)

    // Then - should not crash, repository method still called
    coVerify { mockTimerRepository.dismissAlarm() }
  }

  @Test
  fun `configuration changes during timer preserve timer state display`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Set up fake time: interval started 35 seconds ago (60s - 35s = 25s remaining)
      val currentTime = 3000L
      val intervalStartTime = currentTime - 35_000L // 35 seconds ago
      fakeTimeProvider.setCurrentTimeMillis(currentTime)

      // Given - timer is running with specific state
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 25.seconds,
          currentLap = 3,
          totalLaps = 8,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
          intervalStartTime = intervalStartTime,
        )

      var uiState = awaitItem()
      assertThat(uiState.currentLap).isEqualTo(3)
      assertThat(uiState.totalLaps).isEqualTo(8)
      assertThat(uiState.timeRemaining).isEqualTo(25.seconds)

      // When - configuration changes (e.g., user switches to different config)
      val newConfig =
        TimerConfiguration(
          laps = 15,
          workDuration = 90.seconds,
          restDuration = 60.seconds,
        )
      configurationFlow.value = newConfig

      uiState = awaitItem()

      // Then - timer state should take precedence over configuration while running
      assertThat(uiState.currentLap).isEqualTo(3) // From timer state
      assertThat(uiState.totalLaps).isEqualTo(8) // From timer state
      assertThat(uiState.timeRemaining).isEqualTo(25.seconds) // From timer state
      assertThat(uiState.configuration).isEqualTo(newConfig) // But config still updates
    }
  }

  @Test
  fun `timer state edge transitions are handled correctly`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      awaitItem()

      // Test transition: Stopped -> Running
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 60.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      var uiState = awaitItem()
      assertThat(uiState.isRunning).isTrue()
      assertThat(uiState.isStopButtonEnabled).isTrue()

      // Test transition: Running -> Paused
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Paused,
          timeRemaining = 45.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = true,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isPaused).isTrue()
      assertThat(uiState.isStopButtonEnabled).isTrue()

      // Test transition: Paused -> Resting
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Resting,
          timeRemaining = 30.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isResting).isTrue()
      assertThat(uiState.isPaused).isFalse()

      // Test transition: Resting -> AlarmActive
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.AlarmActive,
          timeRemaining = 0.seconds,
          currentLap = 5,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isAlarmActive).isTrue()
      assertThat(uiState.isStopButtonEnabled).isTrue()

      // Test transition: AlarmActive -> Stopped
      timerStateFlow.value = TimerState.stopped()

      uiState = awaitItem()
      assertThat(uiState.isStopped).isTrue()
      assertThat(uiState.isStopButtonEnabled).isFalse()
    }
  }

  @Test
  fun `flash screen timing works correctly`() = runTest {
    viewModel.uiState.test {
      // Skip initial state
      var uiState = awaitItem()
      assertThat(uiState.flashScreen).isFalse()

      // When - flash is triggered
      viewModel.triggerFlash()
      uiState = awaitItem()
      assertThat(uiState.flashScreen).isTrue()

      // Flash state persists until explicitly dismissed
      expectNoEvents() // Should not automatically clear

      // When - flash is dismissed
      viewModel.onEvent(MainEvent.FlashScreenDismissed)
      uiState = awaitItem()
      assertThat(uiState.flashScreen).isFalse()
    }
  }

  @Test
  fun `skip rest failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockTimerRepository.skipRest() } returns
      Result.failure(RuntimeException("Service error"))
    isServiceBoundFlow.value = true
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 1,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    // When
    viewModel.onEvent(MainEvent.PlayPauseClicked)

    // Then - should not crash, repository method still called
    coVerify { mockTimerRepository.skipRest() }
  }

  @Test
  fun `skip rest during different phases calls correct methods`() = runTest {
    isServiceBoundFlow.value = true

    // Test skip rest when in resting phase
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 10.seconds,
        currentLap = 2,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    viewModel.onEvent(MainEvent.PlayPauseClicked)
    coVerify { mockTimerRepository.skipRest() }

    // Clear previous interactions
    io.mockk.clearMocks(mockTimerRepository)
    coEvery { mockTimerRepository.pauseTimer() } returns Result.success(Unit)

    // Test pause when in running phase
    timerStateFlow.value =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 2,
        totalLaps = 5,
        isPaused = false,
        configuration = TimerConfiguration.DEFAULT,
      )

    viewModel.onEvent(MainEvent.PlayPauseClicked)
    coVerify { mockTimerRepository.pauseTimer() }
    coVerify(exactly = 0) { mockTimerRepository.skipRest() }
  }

  @Test
  fun `stop button enabled state changes correctly`() = runTest {
    viewModel.uiState.test {
      // Initial stopped state - stop button disabled
      var uiState = awaitItem()
      assertThat(uiState.isStopButtonEnabled).isFalse()

      // Running state - stop button enabled
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 60.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = false,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isStopButtonEnabled).isTrue()

      // Paused state - stop button still enabled
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Paused,
          timeRemaining = 45.seconds,
          currentLap = 1,
          totalLaps = 5,
          isPaused = true,
          configuration = TimerConfiguration.DEFAULT,
        )

      uiState = awaitItem()
      assertThat(uiState.isStopButtonEnabled).isTrue()

      // Back to stopped - stop button disabled
      timerStateFlow.value = TimerState.stopped()

      uiState = awaitItem()
      assertThat(uiState.isStopButtonEnabled).isFalse()
    }
  }
}
