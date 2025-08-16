package com.wearinterval.ui.screen.config

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class ConfigViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockConfigRepository = mockk<ConfigurationRepository>()
    private val currentConfigFlow = MutableStateFlow(TimerConfiguration.DEFAULT)

    private lateinit var viewModel: ConfigViewModel

    @Before
    fun setup() {
        every { mockConfigRepository.currentConfiguration } returns currentConfigFlow
        coEvery { mockConfigRepository.updateConfiguration(any()) } returns Result.success(Unit)

        viewModel = ConfigViewModel(mockConfigRepository)
    }

    @Test
    fun `ui state reflects current configuration`() = runTest {
        // Given
        val testConfig = TimerConfiguration(
            laps = 10,
            workDuration = 45.seconds,
            restDuration = 15.seconds,
        )

        // When
        currentConfigFlow.value = testConfig

        // Then
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.laps).isEqualTo(10)
            assertThat(uiState.workMinutes).isEqualTo(0)
            assertThat(uiState.workSeconds).isEqualTo(45)
            assertThat(uiState.restMinutes).isEqualTo(0)
            assertThat(uiState.restSeconds).isEqualTo(15)
        }
    }

    @Test
    fun `increase laps updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 5, workDuration = 60.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.IncreaseLaps)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(laps = 6),
            )
        }
    }

    @Test
    fun `decrease laps updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 5, workDuration = 60.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseLaps)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(laps = 4),
            )
        }
    }

    @Test
    fun `laps cannot go below 1`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseLaps)

        // Then - no update should be called
        coVerify(exactly = 0) {
            mockConfigRepository.updateConfiguration(any())
        }
    }

    @Test
    fun `increase work duration updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.IncreaseWorkDuration)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(workDuration = 35.seconds),
            )
        }
    }

    @Test
    fun `decrease work duration updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseWorkDuration)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(workDuration = 25.seconds),
            )
        }
    }

    @Test
    fun `work duration cannot go below 5 seconds`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 5.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseWorkDuration)

        // Then - no update should be called
        coVerify(exactly = 0) {
            mockConfigRepository.updateConfiguration(any())
        }
    }

    @Test
    fun `increase rest duration updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 10.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.IncreaseRestDuration)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(restDuration = 15.seconds),
            )
        }
    }

    @Test
    fun `decrease rest duration updates configuration`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 10.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseRestDuration)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(
                initialConfig.copy(restDuration = 5.seconds),
            )
        }
    }

    @Test
    fun `rest duration cannot go below 0 seconds`() = runTest {
        // Given
        val initialConfig = TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds)
        currentConfigFlow.value = initialConfig

        // When
        viewModel.onEvent(ConfigEvent.DecreaseRestDuration)

        // Then - no update should be called
        coVerify(exactly = 0) {
            mockConfigRepository.updateConfiguration(any())
        }
    }

    @Test
    fun `reset event resets to default configuration`() = runTest {
        // Given
        val customConfig = TimerConfiguration(
            laps = 20,
            workDuration = 2.minutes,
            restDuration = 30.seconds,
        )
        currentConfigFlow.value = customConfig

        // When
        viewModel.onEvent(ConfigEvent.Reset)

        // Then
        coVerify {
            mockConfigRepository.updateConfiguration(TimerConfiguration.DEFAULT)
        }
    }

    @Test
    fun `ui state calculates display format correctly`() = runTest {
        // Given
        val testConfig = TimerConfiguration(
            laps = 15,
            // 1 minute 15 seconds
            workDuration = 75.seconds,
            // 1 minute 30 seconds
            restDuration = 90.seconds,
        )

        // When
        currentConfigFlow.value = testConfig

        // Then
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.laps).isEqualTo(15)
            assertThat(uiState.workMinutes).isEqualTo(1)
            assertThat(uiState.workSeconds).isEqualTo(15)
            assertThat(uiState.restMinutes).isEqualTo(1)
            assertThat(uiState.restSeconds).isEqualTo(30)
        }
    }

    @Test
    fun `large work duration values are handled correctly`() = runTest {
        // Given
        val testConfig = TimerConfiguration(
            laps = 1,
            // 5:45
            workDuration = 5.minutes + 45.seconds,
            restDuration = 0.seconds,
        )

        // When
        currentConfigFlow.value = testConfig

        // Then
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.workMinutes).isEqualTo(5)
            assertThat(uiState.workSeconds).isEqualTo(45)
        }
    }
}
