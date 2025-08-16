package com.wearinterval.domain.model

import com.wearinterval.util.TimeUtils
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

const val INFINITE_LAPS = 999

data class TimerConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val laps: Int,
    val workDuration: Duration,
    val restDuration: Duration,
    val lastUsed: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return laps in 1..INFINITE_LAPS &&
                workDuration >= 5.seconds &&
                workDuration <= 10.minutes &&
                restDuration >= 0.seconds &&
                restDuration <= 10.minutes
    }
    
    fun displayString(): String {
        val lapText = if (laps == 1) "" else "$laps x "
        val workText = TimeUtils.formatDuration(workDuration)
        val restText = if (restDuration > 0.seconds) " + ${TimeUtils.formatDuration(restDuration)}" else ""
        
        return "$lapText$workText$restText"
    }
    
    fun shortDisplayString(): String {
        return if (laps == 1) {
            TimeUtils.formatDuration(workDuration)
        } else {
            val infiniteSymbol = if (laps == INFINITE_LAPS) "∞" else laps.toString()
            "$infiniteSymbol×${TimeUtils.formatDuration(workDuration)}"
        }
    }
    
    fun withUpdatedTimestamp(): TimerConfiguration {
        return copy(lastUsed = System.currentTimeMillis())
    }
    
    companion object {
        val DEFAULT = TimerConfiguration(
            laps = 1,
            workDuration = 60.seconds,
            restDuration = 0.seconds
        )
        
        fun validate(
            laps: Int,
            workDuration: Duration,
            restDuration: Duration
        ): TimerConfiguration {
            val validLaps = laps.coerceIn(1, INFINITE_LAPS)
            val validWorkDuration = workDuration.coerceIn(5.seconds, 10.minutes)
            val validRestDuration = restDuration.coerceIn(0.seconds, 10.minutes)
            
            return TimerConfiguration(
                laps = validLaps,
                workDuration = validWorkDuration,
                restDuration = validRestDuration
            )
        }
        
        val COMMON_PRESETS = listOf(
            // Single intervals (timers)
            TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds),
            TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds),
            TimerConfiguration(laps = 1, workDuration = 2.minutes, restDuration = 0.seconds),
            
            // Interval training
            TimerConfiguration(laps = 5, workDuration = 45.seconds, restDuration = 15.seconds),
            TimerConfiguration(laps = 8, workDuration = 25.seconds, restDuration = 5.seconds),
            TimerConfiguration(laps = 10, workDuration = 30.seconds, restDuration = 30.seconds),
            TimerConfiguration(laps = 20, workDuration = 20.seconds, restDuration = 10.seconds),
            
            // Infinite workout
            TimerConfiguration(laps = INFINITE_LAPS, workDuration = 25.seconds, restDuration = 5.seconds)
        )
    }
}