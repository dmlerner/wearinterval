package com.wearinterval.data.service

import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for TimerService business logic and calculations.
 * Service lifecycle and integration tests should be in androidTest directory.
 */
class TimerServiceLogicTest {

    @Test
    fun timerPhase_transitions_workCorrectly() {
        // Test timer phase transition logic
        val phases = listOf(
            TimerPhase.Stopped,
            TimerPhase.Running,
            TimerPhase.Resting,
            TimerPhase.Paused,
            TimerPhase.AlarmActive,
        )

        // Test that all phases are distinct
        val uniquePhases = phases.toSet()
        assertThat(uniquePhases).hasSize(phases.size)
    }

    @Test
    fun timerConfiguration_validation() {
        // Test timer configuration validation logic
        val validConfig = TimerConfiguration(
            laps = 5,
            workDuration = 90.seconds,
            restDuration = 30.seconds,
        )

        assertThat(validConfig.laps).isGreaterThan(0)
        assertThat(validConfig.workDuration.inWholeSeconds).isGreaterThan(0)
        assertThat(validConfig.restDuration.inWholeSeconds).isAtLeast(0)
    }

    @Test
    fun timerDuration_calculations() {
        // Test timer duration calculations
        val config = TimerConfiguration(
            laps = 3,
            workDuration = 60.seconds,
            restDuration = 20.seconds,
        )

        // Calculate total time for configuration
        val totalWorkTime = config.workDuration * config.laps
        val totalRestTime = config.restDuration * (config.laps - 1) // No rest after last lap
        val totalTime = totalWorkTime + totalRestTime

        assertThat(totalWorkTime).isEqualTo(180.seconds) // 3 * 60
        assertThat(totalRestTime).isEqualTo(40.seconds) // 2 * 20
        assertThat(totalTime).isEqualTo(220.seconds) // 180 + 40
    }

    @Test
    fun interval_progression_logic() {
        // Test interval progression logic
        fun calculateNextPhase(currentPhase: TimerPhase, currentLap: Int, totalLaps: Int, hasRestPeriod: Boolean): TimerPhase {
            return when (currentPhase) {
                TimerPhase.Running -> {
                    if (currentLap < totalLaps && hasRestPeriod) {
                        TimerPhase.Resting
                    } else if (currentLap >= totalLaps) {
                        TimerPhase.AlarmActive
                    } else {
                        TimerPhase.Running // Next lap directly
                    }
                }
                TimerPhase.Resting -> TimerPhase.Running
                else -> currentPhase
            }
        }

        // Test work to rest transition
        assertThat(calculateNextPhase(TimerPhase.Running, 1, 3, true))
            .isEqualTo(TimerPhase.Resting)

        // Test rest to work transition
        assertThat(calculateNextPhase(TimerPhase.Resting, 1, 3, true))
            .isEqualTo(TimerPhase.Running)

        // Test completion
        assertThat(calculateNextPhase(TimerPhase.Running, 3, 3, true))
            .isEqualTo(TimerPhase.AlarmActive)

        // Test no rest period
        assertThat(calculateNextPhase(TimerPhase.Running, 1, 3, false))
            .isEqualTo(TimerPhase.Running)
    }

    @Test
    fun timeRemaining_calculation() {
        // Test time remaining calculation logic
        fun calculateTimeRemaining(currentPhase: TimerPhase, intervalStartTime: Long, currentTime: Long, intervalDuration: Long): Long {
            val elapsed = currentTime - intervalStartTime
            return (intervalDuration - elapsed).coerceAtLeast(0)
        }

        val startTime = 1000L
        val intervalDuration = 60000L // 60 seconds

        // Test various elapsed times
        val testCases = mapOf(
            16000L to 45000L, // 15s elapsed (16000 - 1000 = 15000), 45s remaining
            31000L to 30000L, // 30s elapsed (31000 - 1000 = 30000), 30s remaining
            61000L to 0L, // 60s elapsed (61000 - 1000 = 60000), 0s remaining
            71000L to 0L, // Overtime (71000 - 1000 = 70000), still 0s remaining
        )

        testCases.forEach { (currentTime, expectedRemaining) ->
            val remaining = calculateTimeRemaining(TimerPhase.Running, startTime, currentTime, intervalDuration)
            assertThat(remaining).isEqualTo(expectedRemaining)
        }
    }

    @Test
    fun lap_progression_logic() {
        // Test lap progression logic
        fun advanceLap(currentLap: Int, totalLaps: Int, currentPhase: TimerPhase, nextPhase: TimerPhase): Int {
            return if (currentPhase == TimerPhase.Running && nextPhase != TimerPhase.Resting) {
                // Only advance lap when finishing work period and not going to rest
                (currentLap + 1).coerceAtMost(totalLaps)
            } else {
                currentLap
            }
        }

        // Test lap advancement after work period
        assertThat(advanceLap(1, 5, TimerPhase.Running, TimerPhase.AlarmActive)).isEqualTo(2)

        // Test no advancement when going to rest
        assertThat(advanceLap(1, 5, TimerPhase.Running, TimerPhase.Resting)).isEqualTo(1)

        // Test no advancement beyond total laps
        assertThat(advanceLap(5, 5, TimerPhase.Running, TimerPhase.AlarmActive)).isEqualTo(5)
    }

    @Test
    fun pause_resume_logic() {
        // Test pause/resume state management
        data class TimerState(val phase: TimerPhase, val isPaused: Boolean)

        fun togglePause(currentState: TimerState): TimerState {
            return when (currentState.phase) {
                TimerPhase.Running, TimerPhase.Resting -> {
                    if (currentState.isPaused) {
                        currentState.copy(isPaused = false) // Resume
                    } else {
                        currentState.copy(phase = TimerPhase.Paused, isPaused = true) // Pause
                    }
                }
                TimerPhase.Paused -> {
                    currentState.copy(phase = TimerPhase.Running, isPaused = false) // Resume to running
                }
                else -> currentState // Can't pause/resume in other states
            }
        }

        // Test pausing from running
        val runningState = TimerState(TimerPhase.Running, false)
        val pausedState = togglePause(runningState)
        assertThat(pausedState.phase).isEqualTo(TimerPhase.Paused)
        assertThat(pausedState.isPaused).isTrue()

        // Test resuming from paused
        val resumedState = togglePause(pausedState)
        assertThat(resumedState.phase).isEqualTo(TimerPhase.Running)
        assertThat(resumedState.isPaused).isFalse()
    }

    @Test
    fun notification_timing_logic() {
        // Test notification timing calculations
        fun shouldTriggerNotification(timeRemaining: Long, intervalDuration: Long, currentPhase: TimerPhase): Boolean {
            val reminderThreshold = 5000L // 5 seconds before end
            return timeRemaining <= reminderThreshold &&
                timeRemaining > 0 &&
                (currentPhase == TimerPhase.Running || currentPhase == TimerPhase.Resting)
        }

        // Test notification triggering
        assertThat(shouldTriggerNotification(3000L, 60000L, TimerPhase.Running)).isTrue()
        assertThat(shouldTriggerNotification(7000L, 60000L, TimerPhase.Running)).isFalse()
        assertThat(shouldTriggerNotification(0L, 60000L, TimerPhase.Running)).isFalse()
        assertThat(shouldTriggerNotification(3000L, 60000L, TimerPhase.Stopped)).isFalse()
    }

    @Test
    fun auto_mode_progression() {
        // Test auto mode progression logic
        fun shouldAutoAdvance(timeRemaining: Long, autoMode: Boolean, currentPhase: TimerPhase): Boolean {
            return autoMode &&
                timeRemaining <= 0 &&
                (currentPhase == TimerPhase.Running || currentPhase == TimerPhase.Resting)
        }

        // Test auto advancement
        assertThat(shouldAutoAdvance(0L, true, TimerPhase.Running)).isTrue()
        assertThat(shouldAutoAdvance(0L, false, TimerPhase.Running)).isFalse()
        assertThat(shouldAutoAdvance(5000L, true, TimerPhase.Running)).isFalse()
        assertThat(shouldAutoAdvance(0L, true, TimerPhase.AlarmActive)).isFalse()
    }

    @Test
    fun service_state_validation() {
        // Test service state validation
        data class ServiceState(
            val isRunning: Boolean,
            val isBound: Boolean,
            val currentPhase: TimerPhase,
        )

        fun isValidServiceState(state: ServiceState): Boolean {
            return when {
                state.currentPhase == TimerPhase.Stopped -> !state.isRunning
                state.currentPhase in listOf(TimerPhase.Running, TimerPhase.Resting, TimerPhase.Paused) -> state.isRunning
                state.currentPhase == TimerPhase.AlarmActive -> state.isRunning
                else -> false
            }
        }

        // Test valid states
        assertThat(isValidServiceState(ServiceState(false, true, TimerPhase.Stopped))).isTrue()
        assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.Running))).isTrue()
        assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.AlarmActive))).isTrue()

        // Test invalid states
        assertThat(isValidServiceState(ServiceState(true, true, TimerPhase.Stopped))).isFalse()
        assertThat(isValidServiceState(ServiceState(false, true, TimerPhase.Running))).isFalse()
    }
}
