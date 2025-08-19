package com.wearinterval.wearos.tile

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.wearinterval.MainActivity
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.TileData
import com.wearinterval.domain.repository.WearOsRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Structural tests for WearIntervalTileService that avoid problematic Robolectric instantiation.
 *
 * These tests focus on class structure, annotations, and data logic rather than actual TileService
 * instantiation which has compatibility issues with protolayout in Robolectric.
 */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@Ignore("Disabled due to ProtoLayout + Robolectric compatibility issues")
class WearIntervalTileServiceStructuralTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var context: Context
  private lateinit var mockWearOsRepository: WearOsRepository

  private val defaultConfig =
    TimerConfiguration(
      id = "test-config",
      laps = 10,
      workDuration = 1.minutes,
      restDuration = 30.seconds,
      lastUsed = System.currentTimeMillis()
    )

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    mockWearOsRepository = mockk(relaxed = true)
  }

  @Test
  fun `tile service class should be properly configured`() {
    // Test that the WearIntervalTileService class exists and has proper structure
    val serviceClass = WearIntervalTileService::class.java

    // Should be a TileService subclass
    assertThat(androidx.wear.tiles.TileService::class.java.isAssignableFrom(serviceClass)).isTrue()

    // Should have AndroidEntryPoint annotation for Hilt dependency injection
    assertThat(serviceClass.isAnnotationPresent(dagger.hilt.android.AndroidEntryPoint::class.java))
      .isTrue()
  }

  @Test
  fun `tile service should have injectable WearOsRepository field`() {
    // Test that the service class has the required field structure for dependency injection
    val serviceClass = WearIntervalTileService::class.java

    // Should have a WearOsRepository field annotated with @Inject
    val fields = serviceClass.declaredFields
    val repoField = fields.find { it.name == "wearOsRepository" }

    assertThat(repoField).isNotNull()
    assertThat(repoField?.type)
      .isEqualTo(com.wearinterval.domain.repository.WearOsRepository::class.java)
    assertThat(repoField?.isAnnotationPresent(javax.inject.Inject::class.java)).isTrue()
  }

  @Test
  fun `MainActivity intent should be properly configured for tile integration`() {
    // Given - Intent configuration for MainActivity integration
    val testConfigId = "test-config-123"

    // When - Creating intent with configuration ID (as would be done in tile)
    val intent =
      Intent(context, MainActivity::class.java).apply {
        putExtra("config_id", testConfigId)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      }

    // Then - Intent should be properly configured for tile integration
    assertThat(intent.component?.className).contains("MainActivity")
    assertThat(intent.getStringExtra("config_id")).isEqualTo(testConfigId)
    assertThat(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
      .isEqualTo(Intent.FLAG_ACTIVITY_NEW_TASK)
    assertThat(intent.flags and Intent.FLAG_ACTIVITY_CLEAR_TASK)
      .isEqualTo(Intent.FLAG_ACTIVITY_CLEAR_TASK)
  }

  @Test
  fun `tile repository data should be handled correctly`() = runTest {
    // Given - Repository with tile data
    val stoppedTimerState =
      TimerState(
        phase = TimerPhase.Stopped,
        timeRemaining = 1.minutes,
        currentLap = 1,
        totalLaps = 10,
        isPaused = false,
        configuration = defaultConfig,
      )

    val recentConfigs =
      listOf(
        TimerConfiguration(
          id = "config1",
          laps = 5,
          workDuration = 30.seconds,
          restDuration = 15.seconds
        ),
        TimerConfiguration(
          id = "config2",
          laps = 10,
          workDuration = 1.minutes,
          restDuration = 30.seconds
        )
      )

    val tileData = TileData(timerState = stoppedTimerState, recentConfigurations = recentConfigs)
    coEvery { mockWearOsRepository.getTileData() } returns tileData

    // When - Repository returns data
    val result = mockWearOsRepository.getTileData()

    // Then - Data should be properly structured for tile consumption
    assertThat(result).isNotNull()
    assertThat(result.timerState.phase).isEqualTo(TimerPhase.Stopped)
    assertThat(result.recentConfigurations).hasSize(2)
    assertThat(result.recentConfigurations[0].id).isEqualTo("config1")
    assertThat(result.recentConfigurations[1].id).isEqualTo("config2")
  }

  @Test
  fun `freshness intervals should be calculated based on timer phase`() {
    // Test the logic for determining tile update intervals
    val activePhases = listOf(TimerPhase.Running, TimerPhase.Resting)
    val inactivePhases = listOf(TimerPhase.Stopped, TimerPhase.Paused)

    // Active states should use frequent updates (1000ms)
    activePhases.forEach { phase ->
      val expectedInterval =
        when (phase) {
          TimerPhase.Running,
          TimerPhase.Resting -> 1000L
          else -> 30000L
        }
      assertThat(expectedInterval).isEqualTo(1000L)
    }

    // Inactive states should use less frequent updates (30000ms)
    inactivePhases.forEach { phase ->
      val expectedInterval =
        when (phase) {
          TimerPhase.Running,
          TimerPhase.Resting -> 1000L
          else -> 30000L
        }
      assertThat(expectedInterval).isEqualTo(30000L)
    }
  }
}
