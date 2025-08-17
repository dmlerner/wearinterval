package com.wearinterval.ui.integration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.data.repository.ConfigurationRepositoryImpl
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.ui.screen.config.ConfigEvent
import com.wearinterval.ui.screen.config.ConfigViewModel
import com.wearinterval.ui.screen.main.MainViewModel
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class ConfigToMainIntegrationTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var configurationRepository: ConfigurationRepository
    private lateinit var timerRepository: TimerRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var configViewModel: ConfigViewModel
    private lateinit var mainViewModel: MainViewModel

    private val dataStoreManager = mockk<DataStoreManager>()
    private val configurationDao = mockk<ConfigurationDao>()
    private val currentConfigFlow = MutableStateFlow<TimerConfiguration?>(TimerConfiguration.DEFAULT)
    private val timerStateFlow = MutableStateFlow(TimerState.stopped())
    private val isServiceBoundFlow = MutableStateFlow(true)

    @Before
    fun setup() {
        // Setup DataStore mock
        every { dataStoreManager.currentConfiguration } returns currentConfigFlow
        coEvery { dataStoreManager.updateCurrentConfiguration(any()) } coAnswers {
            currentConfigFlow.value = firstArg()
            Unit
        }

        // Setup DAO mock
        every { configurationDao.getRecentConfigurationsFlow(any()) } returns MutableStateFlow(emptyList())
        coEvery { configurationDao.insertConfiguration(any()) } returns Unit
        coEvery { configurationDao.getConfigurationCount() } returns 0

        // Create real repository with mocked dependencies
        configurationRepository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)

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

        // Create ViewModels
        configViewModel = ConfigViewModel(configurationRepository, timerRepository)
        mainViewModel = MainViewModel(timerRepository, configurationRepository, settingsRepository)
    }

    @Test
    fun `when config wheel changes laps, main screen should update immediately`() = runTest {
        // Given - initial state with 2 laps
        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(2)

        // When - user scrolls wheel to select 5 laps
        configViewModel.onEvent(ConfigEvent.SetLaps(5))
        advanceUntilIdle()

        // Then - main screen should show 5 laps
        mainViewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.configuration.laps).isEqualTo(5)
        }
    }

    @Test
    fun `when config wheel changes work duration, main screen should update immediately`() = runTest {
        // Given - initial state with 3 seconds work
        assertThat(mainViewModel.uiState.value.configuration.workDuration).isEqualTo(3.seconds)

        // When - user scrolls wheel to select 90 seconds
        configViewModel.onEvent(ConfigEvent.SetWorkDuration(90.seconds))
        advanceUntilIdle()

        // Then - main screen should show 90 seconds
        mainViewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.configuration.workDuration).isEqualTo(90.seconds)
        }
    }

    @Test
    fun `when config wheel changes rest duration, main screen should update immediately`() = runTest {
        // Given - initial state with 3 seconds rest
        assertThat(mainViewModel.uiState.value.configuration.restDuration).isEqualTo(3.seconds)

        // When - user scrolls wheel to select 30 seconds rest
        configViewModel.onEvent(ConfigEvent.SetRestDuration(30.seconds))
        advanceUntilIdle()

        // Then - main screen should show 30 seconds rest
        mainViewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.configuration.restDuration).isEqualTo(30.seconds)
        }
    }

    @Test
    fun `when timer is running and config wheel changes, main screen config should still update`() = runTest {
        // Given - timer is running
        timerStateFlow.value = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 30.seconds,
            currentLap = 1,
            totalLaps = 2,
            configuration = TimerConfiguration.DEFAULT,
        )
        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(2)

        // When - user scrolls wheel to change laps while timer is running
        configViewModel.onEvent(ConfigEvent.SetLaps(10))
        advanceUntilIdle()

        // Then - main screen configuration should update even though timer is running
        // (The timer state itself won't change, but the configuration display should)
        mainViewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.configuration.laps).isEqualTo(10)
            // Timer state should remain unchanged
            assertThat(state.totalLaps).isEqualTo(1) // from timer state
        }
    }

    @Test
    fun `rapid wheel scrolling should update configuration correctly`() = runTest {
        // Given - initial state
        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(2)

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
        // Given - initial state
        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(2)

        // When - user sets laps to 10
        configViewModel.onEvent(ConfigEvent.SetLaps(10))
        advanceUntilIdle()

        // Force the MainViewModel to re-collect by accessing the state
        // This is a workaround for the test environment where combine might not emit immediately
        repeat(5) {
            kotlinx.coroutines.delay(10)
            advanceUntilIdle()
            if (mainViewModel.uiState.value.configuration.laps == 10) return@repeat
        }

        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(10)

        // When - user taps button to reset laps
        configViewModel.onEvent(ConfigEvent.ResetLaps)
        advanceUntilIdle()

        // Same workaround for reset
        repeat(5) {
            kotlinx.coroutines.delay(10)
            advanceUntilIdle()
            if (mainViewModel.uiState.value.configuration.laps == 2) return@repeat
        }

        // Then - main screen should show reset value
        assertThat(mainViewModel.uiState.value.configuration.laps).isEqualTo(2)
    }
}
