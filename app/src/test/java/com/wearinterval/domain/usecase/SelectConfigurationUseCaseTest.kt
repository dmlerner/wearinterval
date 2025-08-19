package com.wearinterval.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SelectConfigurationUseCaseTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockConfigurationRepository = mockk<ConfigurationRepository>(relaxed = true)
  private val mockTimerRepository = mockk<TimerRepository>(relaxed = true)
  private lateinit var useCase: SelectConfigurationUseCase

  private val testConfig =
    TimerConfiguration(
      id = "test-id",
      laps = 5,
      workDuration = 45.seconds,
      restDuration = 15.seconds,
    )

  @Before
  fun setup() {
    useCase = SelectConfigurationUseCase(mockConfigurationRepository, mockTimerRepository)
  }

  @Test
  fun `selectConfigurationAndStopTimer should stop timer and select configuration`() = runTest {
    // Given
    coEvery { mockTimerRepository.stopTimer() } returns Result.success(Unit)
    coEvery { mockConfigurationRepository.selectRecentConfiguration(any()) } returns
      Result.success(Unit)

    // When
    val result = useCase.selectConfigurationAndStopTimer(testConfig)

    // Then
    assertThat(result.isSuccess).isTrue()
    coVerify { mockTimerRepository.stopTimer() }
    coVerify { mockConfigurationRepository.selectRecentConfiguration(testConfig) }
  }

  @Test
  fun `selectConfigurationAndStopTimer should handle timer repository failure`() = runTest {
    // Given
    val exception = RuntimeException("Timer error")
    coEvery { mockTimerRepository.stopTimer() } throws exception

    // When
    val result = useCase.selectConfigurationAndStopTimer(testConfig)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
    coVerify { mockTimerRepository.stopTimer() }
  }

  @Test
  fun `selectConfigurationAndStopTimer should handle configuration repository failure`() = runTest {
    // Given
    val exception = RuntimeException("Configuration error")
    coEvery { mockTimerRepository.stopTimer() } returns Result.success(Unit)
    coEvery { mockConfigurationRepository.selectRecentConfiguration(any()) } returns
      Result.failure(exception)

    // When
    val result = useCase.selectConfigurationAndStopTimer(testConfig)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
    coVerify { mockTimerRepository.stopTimer() }
    coVerify { mockConfigurationRepository.selectRecentConfiguration(testConfig) }
  }
}
