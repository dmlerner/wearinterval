package com.wearinterval.ui.screen.main

import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import kotlin.time.Duration

/**
 * UI state for the main timer screen.
 * Combines timer state, configuration, and UI-specific state.
 */
data class MainUiState(
    val timerPhase: TimerPhase = TimerPhase.Stopped,
    val timeRemaining: Duration = Duration.ZERO,
    val currentLap: Int = 0,
    val totalLaps: Int = 1,
    val isPaused: Boolean = false,
    val configuration: TimerConfiguration = TimerConfiguration.DEFAULT,
    val isPlayButtonEnabled: Boolean = true,
    val isStopButtonEnabled: Boolean = false,
    val isServiceBound: Boolean = false,
    val flashScreen: Boolean = false,
) {
    val isRunning: Boolean get() = timerPhase == TimerPhase.Running
    val isResting: Boolean get() = timerPhase == TimerPhase.Resting
    val isStopped: Boolean get() = timerPhase == TimerPhase.Stopped
    val isAlarmActive: Boolean get() = timerPhase == TimerPhase.AlarmActive

    val currentIntervalDuration: Duration get() = when {
        isResting -> configuration.restDuration
        else -> configuration.workDuration
    }

    val intervalProgressPercentage: Float get() = if (currentIntervalDuration > Duration.ZERO) {
        // Progress starts full (1.0) and ticks down to empty (0.0) as time remaining decreases
        (timeRemaining.inWholeMilliseconds.toFloat() / currentIntervalDuration.inWholeMilliseconds.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    val overallProgressPercentage: Float get() = if (totalLaps > 0) {
        (currentLap.toFloat() / totalLaps.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
}

/**
 * Events that can be triggered from the main screen UI.
 */
sealed class MainEvent {
    object PlayPauseClicked : MainEvent()
    object StopClicked : MainEvent()
    object DismissAlarm : MainEvent()
    object FlashScreenDismissed : MainEvent()
}
