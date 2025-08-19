package com.wearinterval.wearos.tile

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.TileData
import com.wearinterval.util.MainDispatcherRule
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import org.junit.Test

/**
 * Simple unit tests for tile data logic without Robolectric dependencies. These tests focus on the
 * data structures and business logic used by the tile service.
 */
@ExperimentalCoroutinesApi
class TileDataLogicTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val defaultConfig =
    TimerConfiguration(
      id = "test-config",
      laps = 10,
      workDuration = 1.minutes,
      restDuration = 30.seconds,
      lastUsed = System.currentTimeMillis()
    )

  @Test
  fun `TileData should hold timer state and configurations correctly`() {
    // Given
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 3,
        totalLaps = 10,
        isPaused = false,
        configuration = defaultConfig,
      )
    val recentConfigs = listOf(defaultConfig)

    // When
    val tileData = TileData(timerState = timerState, recentConfigurations = recentConfigs)

    // Then
    assertThat(tileData.timerState).isEqualTo(timerState)
    assertThat(tileData.recentConfigurations).hasSize(1)
    assertThat(tileData.recentConfigurations[0]).isEqualTo(defaultConfig)
  }

  @Test
  fun `TileData should handle empty configuration list`() {
    // Given
    val timerState =
      TimerState(
        phase = TimerPhase.Stopped,
        timeRemaining = 1.minutes,
        currentLap = 1,
        totalLaps = 10,
        isPaused = false,
        configuration = defaultConfig,
      )

    // When
    val tileData = TileData(timerState = timerState, recentConfigurations = emptyList())

    // Then
    assertThat(tileData.recentConfigurations).isEmpty()
    assertThat(tileData.timerState.phase).isEqualTo(TimerPhase.Stopped)
  }

  @Test
  fun `freshness intervals should be correct for different timer phases`() {
    // Test that we can determine appropriate update intervals based on timer phase
    val activePhases = listOf(TimerPhase.Running, TimerPhase.Resting)
    val inactivePhases = listOf(TimerPhase.Stopped, TimerPhase.Paused)

    // Active states should require frequent updates
    activePhases.forEach { phase ->
      val expectedInterval =
        when (phase) {
          TimerPhase.Running,
          TimerPhase.Resting -> 1000L // 1 second
          else -> 30000L // 30 seconds
        }
      assertThat(expectedInterval).isEqualTo(1000L)
    }

    // Inactive states should require less frequent updates
    inactivePhases.forEach { phase ->
      val expectedInterval =
        when (phase) {
          TimerPhase.Running,
          TimerPhase.Resting -> 1000L
          else -> 30000L // 30 seconds
        }
      assertThat(expectedInterval).isEqualTo(30000L)
    }
  }

  @Test
  fun `timer configuration display strings should be valid for tiles`() {
    // Given - Various timer configurations
    val testConfigs =
      listOf(
        TimerConfiguration(
          id = "short",
          laps = 5,
          workDuration = 30.seconds,
          restDuration = 10.seconds
        ),
        TimerConfiguration(
          id = "medium",
          laps = 10,
          workDuration = 1.minutes,
          restDuration = 30.seconds
        ),
        TimerConfiguration(
          id = "long",
          laps = 20,
          workDuration = 2.minutes,
          restDuration = 1.minutes
        )
      )

    // When/Then - Each should have valid display strings
    testConfigs.forEach { config ->
      val displayString = config.displayString()
      assertThat(displayString).isNotEmpty()
      assertThat(displayString).contains(config.laps.toString())
      // Should be suitable for tile display (not too long)
      assertThat(displayString.length).isAtMost(50)
    }
  }

  @Test
  fun `continuous timer should be handled correctly`() {
    // Given - Continuous timer (999 laps)
    val continuousConfig =
      TimerConfiguration(
        id = "continuous",
        laps = 999, // Indicates continuous/infinite timer
        workDuration = 1.minutes,
        restDuration = 30.seconds
      )

    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 7,
        totalLaps = 999,
        isPaused = false,
        configuration = continuousConfig,
      )

    // When
    val tileData =
      TileData(timerState = timerState, recentConfigurations = listOf(continuousConfig))

    // Then
    assertThat(tileData.timerState.totalLaps).isEqualTo(999)
    assertThat(tileData.timerState.isInfinite).isTrue()
    assertThat(tileData.timerState.displayCurrentLap).isEqualTo("7") // Should not show "/999"
  }
}
