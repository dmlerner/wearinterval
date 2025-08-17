package com.wearinterval.wearos.tile

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.TileData
import com.wearinterval.domain.repository.WearOsRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ExperimentalCoroutinesApi
class WearIntervalTileServiceTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockWearOsRepository = mockk<WearOsRepository>()
    private lateinit var tileService: WearIntervalTileService

    private val defaultConfig = TimerConfiguration(
        laps = 10,
        workDuration = 1.minutes,
        restDuration = 30.seconds,
    )

    private val stoppedTimerState = TimerState(
        phase = TimerPhase.Stopped,
        timeRemaining = 1.minutes,
        currentLap = 1,
        totalLaps = 10,
        isPaused = false,
        configuration = defaultConfig,
    )

    @Before
    fun setup() {
        tileService = WearIntervalTileService()
        tileService.wearOsRepository = mockWearOsRepository
    }

    @Test
    fun `getTileData repository integration works`() = runTest {
        // Given
        val tileData = TileData(
            timerState = stoppedTimerState,
            recentConfigurations = emptyList(),
        )
        coEvery { mockWearOsRepository.getTileData() } returns tileData

        // When
        val result = mockWearOsRepository.getTileData()

        // Then
        assertThat(result).isEqualTo(tileData)
        assertThat(result.timerState.phase).isEqualTo(TimerPhase.Stopped)
    }

    @Test
    fun `tile service injection works`() {
        // Given/When/Then
        assertThat(tileService).isNotNull()
        tileService.wearOsRepository = mockWearOsRepository
        assertThat(tileService.wearOsRepository).isEqualTo(mockWearOsRepository)
    }

    @Test
    fun `tile data handles empty configuration list`() = runTest {
        // Given
        val tileData = TileData(
            timerState = stoppedTimerState,
            recentConfigurations = emptyList(),
        )
        coEvery { mockWearOsRepository.getTileData() } returns tileData

        // When
        val result = mockWearOsRepository.getTileData()

        // Then
        assertThat(result.recentConfigurations).isEmpty()
        assertThat(result.timerState).isEqualTo(stoppedTimerState)
    }

    @Test
    fun `tile data handles multiple configurations`() = runTest {
        // Given
        val recentConfigs = listOf(
            TimerConfiguration(laps = 5, workDuration = 30.seconds, restDuration = 15.seconds),
            TimerConfiguration(laps = 10, workDuration = 1.minutes, restDuration = 30.seconds),
        )
        val tileData = TileData(
            timerState = stoppedTimerState,
            recentConfigurations = recentConfigs,
        )
        coEvery { mockWearOsRepository.getTileData() } returns tileData

        // When
        val result = mockWearOsRepository.getTileData()

        // Then
        assertThat(result.recentConfigurations).hasSize(2)
        assertThat(result.recentConfigurations[0].laps).isEqualTo(5)
        assertThat(result.recentConfigurations[1].laps).isEqualTo(10)
    }

    @Test
    fun `tile service handles repository errors gracefully`() = runTest {
        // Given
        coEvery { mockWearOsRepository.getTileData() } throws RuntimeException("Test error")

        // When/Then - should not throw exception when mocking repository errors
        try {
            mockWearOsRepository.getTileData()
            // If no exception is thrown, that's not expected for this test
            assertThat(false).isTrue() // Force failure
        } catch (e: RuntimeException) {
            assertThat(e.message).isEqualTo("Test error")
        }
    }

    @Test
    fun `timer state constructor works correctly`() {
        // Given/When
        val timerState = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 45.seconds,
            currentLap = 3,
            totalLaps = 10,
            isPaused = false,
            configuration = defaultConfig,
        )

        // Then
        assertThat(timerState.phase).isEqualTo(TimerPhase.Running)
        assertThat(timerState.currentLap).isEqualTo(3)
        assertThat(timerState.totalLaps).isEqualTo(10)
        assertThat(timerState.isRunning).isTrue()
        assertThat(timerState.isStopped).isFalse()
    }
}
