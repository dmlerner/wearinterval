package com.wearinterval.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TimerState(
    val phase: TimerPhase,
    val timeRemaining: Duration,
    val currentLap: Int,
    val totalLaps: Int,
    val isPaused: Boolean = false,
    val configuration: TimerConfiguration
) {
    val isRunning: Boolean get() = phase == TimerPhase.Running || phase == TimerPhase.Resting
    val isResting: Boolean get() = phase == TimerPhase.Resting
    val isStopped: Boolean get() = phase == TimerPhase.Stopped
    val isAlarmActive: Boolean get() = phase == TimerPhase.AlarmActive
    
    val currentInterval: Duration get() = if (isResting) configuration.restDuration else configuration.workDuration
    
    val progressPercentage: Float get() {
        if (currentInterval == Duration.ZERO) return 1f
        return 1f - (timeRemaining.inWholeMilliseconds.toFloat() / currentInterval.inWholeMilliseconds.toFloat())
    }
    
    val lapProgressPercentage: Float get() {
        if (totalLaps == 0) return 1f
        val lapProgress = if (isResting) currentLap else currentLap - 1
        return lapProgress.toFloat() / totalLaps.toFloat()
    }
    
    val isInfinite: Boolean get() = totalLaps == 999
    
    val displayCurrentLap: String get() = if (isInfinite) currentLap.toString() else "$currentLap/$totalLaps"
    
    companion object {
        fun stopped(configuration: TimerConfiguration = TimerConfiguration.DEFAULT) = TimerState(
            phase = TimerPhase.Stopped,
            timeRemaining = configuration.workDuration,
            currentLap = 1,
            totalLaps = configuration.laps,
            configuration = configuration
        )
    }
}

sealed class TimerPhase {
    object Stopped : TimerPhase()
    object Running : TimerPhase()
    object Resting : TimerPhase()
    object Paused : TimerPhase()
    object AlarmActive : TimerPhase()
}