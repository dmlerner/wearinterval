package com.wearinterval.ui.screen.config

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConfigViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockConfigRepository = mockk<ConfigurationRepository>()
  private val mockTimerRepository = mockk<TimerRepository>()
  private val currentConfigFlow = MutableStateFlow(TimerConfiguration.DEFAULT)

  private lateinit var viewModel: ConfigViewModel

  @Before
  fun setup() {
    every { mockConfigRepository.currentConfiguration } returns currentConfigFlow
    coEvery { mockConfigRepository.updateConfiguration(any()) } returns Result.success(Unit)

    viewModel = ConfigViewModel(mockConfigRepository, mockTimerRepository)
  }

  @Test
  fun `ui state reflects current configuration`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(
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
  fun `set laps updates configuration`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 5, workDuration = 60.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetLaps(10))

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(laps = 10),
      )
    }
  }

  @Test
  fun `reset laps sets to default value`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 20, workDuration = 60.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.ResetLaps)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(laps = 999),
      )
    }
  }

  @Test
  fun `set laps to infinite sets to 999`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 5, workDuration = 60.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetLapsToInfinite)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(laps = 999),
      )
    }
  }

  @Test
  fun `set work duration updates configuration`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetWorkDuration(45.seconds))

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(workDuration = 45.seconds),
      )
    }
  }

  @Test
  fun `reset work duration sets to default value`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.ResetWork)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(workDuration = 1.minutes),
      )
    }
  }

  @Test
  fun `set work to long duration sets to 5 minutes`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetWorkToLong)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(workDuration = 5.minutes),
      )
    }
  }

  @Test
  fun `set rest duration updates configuration`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 10.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetRestDuration(30.seconds))

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(restDuration = 30.seconds),
      )
    }
  }

  @Test
  fun `reset rest duration sets to default value`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 10.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.ResetRest)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(restDuration = 0.seconds),
      )
    }
  }

  @Test
  fun `set rest to long duration sets to 5 minutes`() = runTest {
    // Given
    val initialConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetRestToLong)

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(
        initialConfig.copy(restDuration = 5.minutes),
      )
    }
  }

  @Test
  fun `reset event resets to default configuration`() = runTest {
    // Given
    val customConfig =
      TimerConfiguration(
        laps = 20,
        workDuration = 2.minutes,
        restDuration = 30.seconds,
      )
    currentConfigFlow.value = customConfig

    // When
    viewModel.onEvent(ConfigEvent.Reset)

    // Then
    coVerify { mockConfigRepository.updateConfiguration(TimerConfiguration.DEFAULT) }
  }

  @Test
  fun `ui state calculates display format correctly`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(
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
    val testConfig =
      TimerConfiguration(
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

  @Test
  fun `clear all data event calls repository clear method`() = runTest {
    // Given
    coEvery { mockConfigRepository.clearAllData() } returns Result.success(Unit)

    // When
    viewModel.onEvent(ConfigEvent.ClearAllData)

    // Then
    coVerify { mockConfigRepository.clearAllData() }
    coVerify(exactly = 0) { mockConfigRepository.updateConfiguration(any()) }
  }

  @Test
  fun `ui state loading is false when configuration loaded`() = runTest {
    // Given
    val testConfig = TimerConfiguration.DEFAULT.copy(laps = 5)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.isLoading).isFalse()
    }
  }

  @Test
  fun `ui state computes total work time text correctly`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 3.minutes + 25.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.totalWorkTimeText).isEqualTo("3:25")
    }
  }

  @Test
  fun `ui state computes total work time text for seconds only`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 45.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.totalWorkTimeText).isEqualTo("45s")
    }
  }

  @Test
  fun `ui state computes total rest time text correctly`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 2.minutes + 15.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.totalRestTimeText).isEqualTo("2:15")
    }
  }

  @Test
  fun `ui state computes total rest time text for seconds only`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 30.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.totalRestTimeText).isEqualTo("30s")
    }
  }

  @Test
  fun `ui state shows none for zero rest duration`() = runTest {
    // Given
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.totalRestTimeText).isEqualTo("None")
    }
  }

  @Test
  fun `repository update configuration failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockConfigRepository.updateConfiguration(any()) } returns
      Result.failure(RuntimeException("Update failed"))

    // When
    viewModel.onEvent(ConfigEvent.SetLaps(10))

    // Then - Should not crash, repository method still called
    coVerify { mockConfigRepository.updateConfiguration(any()) }
  }

  @Test
  fun `repository clear all data failure is handled gracefully`() = runTest {
    // Given
    coEvery { mockConfigRepository.clearAllData() } returns
      Result.failure(RuntimeException("Clear failed"))

    // When
    viewModel.onEvent(ConfigEvent.ClearAllData)

    // Then - Should not crash, repository method still called
    coVerify { mockConfigRepository.clearAllData() }
  }

  @Test
  fun `set laps event validates input boundary values`() = runTest {
    // Given
    val initialConfig = TimerConfiguration.DEFAULT
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetLaps(1))

    // Then
    coVerify { mockConfigRepository.updateConfiguration(initialConfig.copy(laps = 1)) }
  }

  @Test
  fun `set work duration event handles zero duration`() = runTest {
    // Given
    val initialConfig = TimerConfiguration.DEFAULT
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetWorkDuration(0.seconds))

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(initialConfig.copy(workDuration = 0.seconds))
    }
  }

  @Test
  fun `set rest duration event handles zero duration`() = runTest {
    // Given
    val initialConfig = TimerConfiguration.DEFAULT
    currentConfigFlow.value = initialConfig

    // When
    viewModel.onEvent(ConfigEvent.SetRestDuration(0.seconds))

    // Then
    coVerify {
      mockConfigRepository.updateConfiguration(initialConfig.copy(restDuration = 0.seconds))
    }
  }

  @Test
  fun `ui state handles very large durations correctly`() = runTest {
    // Given - 59:59 duration
    val testConfig =
      TimerConfiguration(laps = 1, workDuration = 59.minutes + 59.seconds, restDuration = 0.seconds)
    currentConfigFlow.value = testConfig

    // When/Then
    viewModel.uiState.test {
      val uiState = awaitItem()
      assertThat(uiState.workMinutes).isEqualTo(59)
      assertThat(uiState.workSeconds).isEqualTo(59)
      assertThat(uiState.totalWorkTimeText).isEqualTo("59:59")
    }
  }
}
