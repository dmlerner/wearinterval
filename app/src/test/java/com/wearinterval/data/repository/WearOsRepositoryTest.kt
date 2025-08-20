package com.wearinterval.data.repository

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ComplicationData
import com.wearinterval.domain.repository.ComplicationType
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.MainDispatcherRule
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
class WearOsRepositoryTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockTimerRepository = mockk<TimerRepository>(relaxed = true)
  private val mockConfigurationRepository = mockk<ConfigurationRepository>(relaxed = true)
  private lateinit var repository: WearOsRepositoryImpl

  private val defaultConfig = TimerConfiguration.DEFAULT
  private val recentConfigs =
    listOf(
      TimerConfiguration(laps = 5, workDuration = 45.seconds, restDuration = 15.seconds),
      TimerConfiguration(laps = 10, workDuration = 30.seconds, restDuration = 30.seconds),
      defaultConfig,
    )

  @Before
  fun setup() {
    every { mockTimerRepository.timerState } returns MutableStateFlow(TimerState.stopped())
    every { mockConfigurationRepository.recentConfigurations } returns
      MutableStateFlow(recentConfigs)

    repository = WearOsRepositoryImpl(mockTimerRepository, mockConfigurationRepository)
  }

  @Test
  fun `updateWearOsComponents returns success`() = runTest {
    // When
    val result = repository.updateWearOsComponents()

    // Then
    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun `getTileData returns current state and recent configurations`() = runTest {
    // Given
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(timerState)

    // When
    val tileData = repository.getTileData()

    // Then
    assertThat(tileData.timerState).isEqualTo(timerState)
    assertThat(tileData.recentConfigurations).isEqualTo(recentConfigs)
  }

  @Test
  fun `getComplicationData ShortText returns correct format for stopped state`() = runTest {
    // Given
    val stoppedState = TimerState.stopped(defaultConfig)
    every { mockTimerRepository.timerState } returns MutableStateFlow(stoppedState)

    // When
    val data = repository.getComplicationData(ComplicationType.ShortText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.ShortText::class.java)
    val shortText = data as ComplicationData.ShortText
    assertThat(shortText.text).isEqualTo("Ready")
    assertThat(shortText.title).isNull()
  }

  @Test
  fun `getComplicationData ShortText returns correct format for running state`() = runTest {
    // Given
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(runningState)

    // When
    val data = repository.getComplicationData(ComplicationType.ShortText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.ShortText::class.java)
    val shortText = data as ComplicationData.ShortText
    assertThat(shortText.text).isEqualTo("45s")
    assertThat(shortText.title).isEqualTo("3/5")
  }

  @Test
  fun `getComplicationData ShortText returns correct format for resting state`() = runTest {
    // Given
    val restingState =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 10.seconds,
        currentLap = 2,
        totalLaps = 5,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(restingState)

    // When
    val data = repository.getComplicationData(ComplicationType.ShortText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.ShortText::class.java)
    val shortText = data as ComplicationData.ShortText
    assertThat(shortText.text).isEqualTo("R:10s")
    assertThat(shortText.title).isEqualTo("2/5")
  }

  @Test
  fun `getComplicationData ShortText handles infinite laps`() = runTest {
    // Given
    val infiniteConfig =
      TimerConfiguration(laps = 999, workDuration = 30.seconds, restDuration = 0.seconds)
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 25.seconds,
        currentLap = 10,
        totalLaps = 999,
        configuration = infiniteConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(runningState)

    // When
    val data = repository.getComplicationData(ComplicationType.ShortText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.ShortText::class.java)
    val shortText = data as ComplicationData.ShortText
    assertThat(shortText.text).isEqualTo("25s")
    assertThat(shortText.title).isEqualTo("10")
  }

  @Test
  fun `getComplicationData LongText returns correct format for stopped state`() = runTest {
    // Given
    val stoppedState = TimerState.stopped(defaultConfig)
    every { mockTimerRepository.timerState } returns MutableStateFlow(stoppedState)

    // When
    val data = repository.getComplicationData(ComplicationType.LongText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.LongText::class.java)
    val longText = data as ComplicationData.LongText
    assertThat(longText.text).isEqualTo("∞×1:00")
    assertThat(longText.title).isEqualTo("Ready")
  }

  @Test
  fun `getComplicationData LongText returns correct format for running state`() = runTest {
    // Given
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 1.minutes + 30.seconds,
        currentLap = 3,
        totalLaps = 5,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(runningState)

    // When
    val data = repository.getComplicationData(ComplicationType.LongText)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.LongText::class.java)
    val longText = data as ComplicationData.LongText
    assertThat(longText.text).isEqualTo("1:30 - Lap 3/5")
    assertThat(longText.title).isNull()
  }

  @Test
  fun `getComplicationData RangedValue returns correct progress`() = runTest {
    // Given - Timer at 25% progress
    val config = TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds)
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds, // 15 seconds elapsed out of 60
        currentLap = 1,
        totalLaps = 1,
        configuration = config,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(runningState)

    // When
    val data = repository.getComplicationData(ComplicationType.RangedValue)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.RangedValue::class.java)
    val rangedValue = data as ComplicationData.RangedValue
    assertThat(rangedValue.value).isWithin(0.01f).of(0.25f)
    assertThat(rangedValue.min).isEqualTo(0f)
    assertThat(rangedValue.max).isEqualTo(1f)
    assertThat(rangedValue.text).isEqualTo("45s")
    assertThat(rangedValue.title).isEqualTo("1/1")
  }

  @Test
  fun `getComplicationData Image returns correct icon for stopped state`() = runTest {
    // Given
    val stoppedState = TimerState.stopped(defaultConfig)
    every { mockTimerRepository.timerState } returns MutableStateFlow(stoppedState)

    // When
    val data = repository.getComplicationData(ComplicationType.MonochromaticImage)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.Image::class.java)
    val image = data as ComplicationData.Image
    assertThat(image.iconRes).isEqualTo(android.R.drawable.ic_media_play)
    assertThat(image.contentDescription).isEqualTo("Start timer")
  }

  @Test
  fun `getComplicationData Image returns correct icon for running state`() = runTest {
    // Given
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 1,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(runningState)

    // When
    val data = repository.getComplicationData(ComplicationType.SmallImage)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.Image::class.java)
    val image = data as ComplicationData.Image
    assertThat(image.iconRes).isEqualTo(android.R.drawable.ic_media_pause)
    assertThat(image.contentDescription).isEqualTo("Pause timer")
  }

  @Test
  fun `getComplicationData Image returns correct icon for resting state`() = runTest {
    // Given
    val restingState =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 10.seconds,
        currentLap = 2,
        totalLaps = 5,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(restingState)

    // When
    val data = repository.getComplicationData(ComplicationType.SmallImage)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.Image::class.java)
    val image = data as ComplicationData.Image
    assertThat(image.iconRes).isEqualTo(android.R.drawable.ic_media_next)
    assertThat(image.contentDescription).isEqualTo("Skip rest")
  }

  @Test
  fun `getComplicationData Image returns correct icon for alarm state`() = runTest {
    // Given
    val alarmState =
      TimerState(
        phase = TimerPhase.AlarmActive,
        timeRemaining = 0.seconds,
        currentLap = 1,
        totalLaps = 1,
        configuration = defaultConfig,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(alarmState)

    // When
    val data = repository.getComplicationData(ComplicationType.MonochromaticImage)

    // Then
    assertThat(data).isInstanceOf(ComplicationData.Image::class.java)
    val image = data as ComplicationData.Image
    assertThat(image.iconRes).isEqualTo(android.R.drawable.ic_delete)
    assertThat(image.contentDescription).isEqualTo("Dismiss alarm")
  }

  @Test
  fun `formatTime handles minutes and seconds correctly`() = runTest {
    // Test different time formats through complication data
    val config =
      TimerConfiguration(laps = 1, workDuration = 2.minutes + 30.seconds, restDuration = 0.seconds)
    val longDurationState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 2.minutes + 30.seconds,
        currentLap = 1,
        totalLaps = 1,
        configuration = config,
      )
    every { mockTimerRepository.timerState } returns MutableStateFlow(longDurationState)

    // When
    val data =
      repository.getComplicationData(ComplicationType.ShortText) as ComplicationData.ShortText

    // Then
    assertThat(data.text).isEqualTo("2:30")
  }
}
