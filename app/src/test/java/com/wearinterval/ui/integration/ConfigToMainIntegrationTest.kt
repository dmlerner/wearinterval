package com.wearinterval.ui.integration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.HeartRateState
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.HeartRateRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.ui.screen.config.ConfigEvent
import com.wearinterval.ui.screen.config.ConfigViewModel
import com.wearinterval.ui.screen.main.MainViewModel
import com.wearinterval.util.FakePermissionManager
import com.wearinterval.util.FakeTimeProvider
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
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
class ConfigToMainIntegrationTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var configurationRepository: ConfigurationRepository
  private lateinit var timerRepository: TimerRepository
  private lateinit var settingsRepository: SettingsRepository
  private lateinit var heartRateRepository: HeartRateRepository
  private lateinit var configViewModel: ConfigViewModel
  private lateinit var mainViewModel: MainViewModel

  private val dataStoreManager = mockk<DataStoreManager>()
  private val configurationDao = mockk<ConfigurationDao>()
  private val currentConfigFlow = MutableStateFlow(TimerConfiguration.DEFAULT)
  private val recentConfigsFlow = MutableStateFlow(emptyList<TimerConfiguration>())
  private val timerStateFlow = MutableStateFlow(TimerState.stopped())
  private val isServiceBoundFlow = MutableStateFlow(true)
  private val heartRateStateFlow = MutableStateFlow<HeartRateState>(HeartRateState.Unavailable)

  @Before
  fun setup() {
    // Mock configuration repository directly for better test control
    configurationRepository = mockk {
      every { currentConfiguration } returns currentConfigFlow
      every { recentConfigurations } returns recentConfigsFlow
      coEvery { updateConfiguration(any()) } coAnswers
        {
          currentConfigFlow.value = firstArg()
          Result.success(Unit)
        }
    }

    // Setup timer repository mock
    timerRepository = mockk {
      every { timerState } returns timerStateFlow
      every { isServiceBound } returns isServiceBoundFlow
      coEvery { startTimer() } returns Result.success(Unit)
      coEvery { pauseTimer() } returns Result.success(Unit)
      coEvery { resumeTimer() } returns Result.success(Unit)
      coEvery { stopTimer() } returns Result.success(Unit)
    }

    // Note: Timer service synchronization now happens through reactive flows automatically

    // Setup settings repository mock
    settingsRepository = mockk()

    // Setup heart rate repository mock (consistent with other mocks)
    heartRateRepository =
      mockk(relaxed = true) {
        every { heartRateState } returns heartRateStateFlow
        coEvery { startMonitoring() } returns Result.success(Unit)
        coEvery { stopMonitoring() } returns Result.success(Unit)
        coEvery { checkPermission() } returns true
      }

    // Create ViewModels
    configViewModel = ConfigViewModel(configurationRepository, timerRepository)
    mainViewModel =
      MainViewModel(
        timerRepository,
        configurationRepository,
        settingsRepository,
        heartRateRepository,
        FakePermissionManager(),
        FakeTimeProvider()
      )
  }

  @Test
  fun `when config wheel changes laps, main screen should update immediately`() = runTest {
    // Ensure all ViewModels are initialized and flows are ready
    advanceUntilIdle()

    // Wait for MainViewModel to emit initial state
    mainViewModel.uiState.test {
      val initialState = awaitItem()

      // Given - verify initial state has infinite laps (999)
      assertThat(initialState.configuration.laps).isEqualTo(999)

      // When - user scrolls wheel to select 5 laps
      configViewModel.onEvent(ConfigEvent.SetLaps(5))

      // Then - main screen should show 5 laps
      val updatedState = awaitItem()
      assertThat(updatedState.configuration.laps).isEqualTo(5)
    }
  }

  @Test
  fun `when config wheel changes work duration, main screen should update immediately`() = runTest {
    // Wait for initial state to be established
    advanceUntilIdle()

    // Given - initial state with 1 minute work
    mainViewModel.uiState.test {
      val initialState = awaitItem()
      assertThat(initialState.configuration.workDuration).isEqualTo(1.minutes)

      // When - user scrolls wheel to select 90 seconds
      configViewModel.onEvent(ConfigEvent.SetWorkDuration(90.seconds))
      advanceUntilIdle()

      // Then - main screen should show 90 seconds
      val updatedState = awaitItem()
      assertThat(updatedState.configuration.workDuration).isEqualTo(90.seconds)
    }
  }

  @Test
  fun `when config wheel changes rest duration, main screen should update immediately`() = runTest {
    // Wait for initial state to be established
    advanceUntilIdle()

    // Given - initial state with 0 seconds rest
    mainViewModel.uiState.test {
      val initialState = awaitItem()
      assertThat(initialState.configuration.restDuration).isEqualTo(0.seconds)

      // When - user scrolls wheel to select 30 seconds rest
      configViewModel.onEvent(ConfigEvent.SetRestDuration(30.seconds))
      advanceUntilIdle()

      // Then - main screen should show 30 seconds rest
      val updatedState = awaitItem()
      assertThat(updatedState.configuration.restDuration).isEqualTo(30.seconds)
    }
  }

  @Test
  fun `when timer is running and config wheel changes, main screen config should still update`() =
    runTest {
      // Wait for initial state to be established
      advanceUntilIdle()

      // Given - timer is running
      timerStateFlow.value =
        TimerState(
          phase = TimerPhase.Running,
          timeRemaining = 30.seconds,
          currentLap = 1,
          totalLaps = 999,
          configuration = TimerConfiguration.DEFAULT,
        )
      advanceUntilIdle()

      mainViewModel.uiState.test {
        val initialState = awaitItem()
        assertThat(initialState.configuration.laps).isEqualTo(999)

        // When - user scrolls wheel to change laps while timer is running
        configViewModel.onEvent(ConfigEvent.SetLaps(10))
        advanceUntilIdle()

        // Then - main screen configuration should update even though timer is running
        // (The timer state itself won't change, but the configuration display should)
        val state = awaitItem()
        assertThat(state.configuration.laps).isEqualTo(10)
        // Timer state should remain unchanged (note: totalLaps shows timer state when running)
        assertThat(state.totalLaps).isEqualTo(999) // from timer state
      }
    }

  @Test
  fun `rapid wheel scrolling should update configuration correctly`() = runTest {
    // Given - initial state
    assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(999)

    // When - user rapidly scrolls through multiple values
    configViewModel.onEvent(ConfigEvent.SetLaps(2))
    configViewModel.onEvent(ConfigEvent.SetLaps(3))
    configViewModel.onEvent(ConfigEvent.SetLaps(4))
    configViewModel.onEvent(ConfigEvent.SetLaps(5))
    advanceUntilIdle()

    // Force the MainViewModel to re-collect by accessing the state
    // This is a workaround for the test environment where combine might not emit immediately
    repeat(5) {
      kotlinx.coroutines.delay(10)
      advanceUntilIdle()
      if (mainViewModel.uiState.value.configuration.laps == 5) return@repeat
    }

    // Then - main screen should show final value
    assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(5)
  }

  @Test
  fun `button press events should also update main screen`() = runTest {
    // Ensure all ViewModels are initialized and flows are ready
    advanceUntilIdle()

    // Wait for MainViewModel to emit initial state
    mainViewModel.uiState.test {
      val initialState = awaitItem()

      // Given - verify initial state has infinite laps (999)
      assertThat(initialState.configuration.laps).isEqualTo(999)

      // When - user sets laps to 10
      configViewModel.onEvent(ConfigEvent.SetLaps(10))

      // Then - main screen should show 10 laps
      val firstUpdateState = awaitItem()
      assertThat(firstUpdateState.configuration.laps).isEqualTo(10)

      // When - user taps button to reset laps
      configViewModel.onEvent(ConfigEvent.ResetLaps)

      // Then - main screen should show reset value (999 - infinite laps)
      val resetState = awaitItem()
      assertThat(resetState.configuration.laps).isEqualTo(999)
    }
  }
}
