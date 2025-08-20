package com.wearinterval.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.util.MainDispatcherRule
import com.wearinterval.util.TimeProvider
import dagger.Lazy
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class TimerRepositoryTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockContext = mockk<Context>(relaxed = true)
  private val mockConfigurationRepository = mockk<ConfigurationRepository>(relaxed = true)
  private val mockTimeProvider = mockk<TimeProvider>(relaxed = true)
  private lateinit var repository: TimerRepositoryImpl

  private val defaultConfig = TimerConfiguration.DEFAULT
  private val runningState =
    TimerState(
      phase = TimerPhase.Running,
      timeRemaining = 45.seconds,
      currentLap = 1,
      totalLaps = 5,
      configuration = defaultConfig,
    )

  @Before
  fun setup() {
    every { mockContext.bindService(any<Intent>(), any<ServiceConnection>(), any<Int>()) } returns
      true
    every { mockContext.startService(any<Intent>()) } returns mockk<ComponentName>()
    every { mockConfigurationRepository.currentConfiguration } returns
      MutableStateFlow(defaultConfig)

    repository =
      TimerRepositoryImpl(
        mockContext,
        mockk<Lazy<ConfigurationRepository>>() {
          every { get() } returns mockConfigurationRepository
        },
        mockTimeProvider
      )
  }

  @Test
  fun `isServiceBound starts as false`() = runTest {
    repository.isServiceBound.test { assertThat(awaitItem()).isFalse() }
  }

  @Test
  fun `timerState returns stopped state when service not bound`() = runTest {
    repository.timerState.test {
      val state = awaitItem()
      assertThat(state.phase).isEqualTo(TimerPhase.Stopped)
    }
  }

  @Test
  fun `startTimer fails when service not bound`() = runTest {
    // When
    val result = repository.startTimer()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(result.exceptionOrNull()?.message).contains("Timer service is not bound")
  }

  @Test
  fun `pauseTimer fails when service not bound`() = runTest {
    // When
    val result = repository.pauseTimer()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `resumeTimer fails when service not bound`() = runTest {
    // When
    val result = repository.resumeTimer()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `stopTimer fails when service not bound`() = runTest {
    // When
    val result = repository.stopTimer()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `dismissAlarm fails when service not bound`() = runTest {
    // When
    val result = repository.dismissAlarm()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
  }

  @Test
  fun `skipRest fails when service not bound`() = runTest {
    // When
    val result = repository.skipRest()

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalStateException::class.java)
    assertThat(result.exceptionOrNull()?.message).contains("Timer service is not bound")
  }
}
