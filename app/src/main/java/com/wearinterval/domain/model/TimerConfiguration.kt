package com.wearinterval.domain.model

import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class TimerConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val laps: Int,
    val workDuration: Duration,
    val restDuration: Duration,
    val lastUsed: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return laps in 1..999 &&
                workDuration >= 5.seconds &&
                workDuration <= 10.minutes &&
                restDuration >= 0.seconds &&
                restDuration <= 10.minutes
    }
    
    fun displayString(): String {
        val lapText = if (laps == 1) "" else "$laps x "
        val workText = formatDuration(workDuration)
        val restText = if (restDuration > 0.seconds) " + ${formatDuration(restDuration)}" else ""
        
        return "$lapText$workText$restText"
    }
    
    fun shortDisplayString(): String {
        return if (laps == 1) {
            formatDuration(workDuration)
        } else {
            val infiniteSymbol = if (laps == 999) "∞" else laps.toString()
            "$infiniteSymbol×${formatDuration(workDuration)}"
        }
    }
    
    private fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        
        return when {
            minutes == 0L -> "${seconds}s"
            seconds == 0L -> "${minutes}:00"
            else -> "${minutes}:%02d".format(seconds)
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
            val validLaps = laps.coerceIn(1, 999)
            val validWorkDuration = workDuration.coerceIn(5.seconds, 10.minutes)
            val validRestDuration = restDuration.coerceIn(0.seconds, 10.minutes)
            
            return TimerConfiguration(
                laps = validLaps,
                workDuration = validWorkDuration,
                restDuration = validRestDuration
            )
        }
        
        val COMMON_PRESETS = listOf(
            TimerConfiguration(laps = 1, workDuration = 30.seconds, restDuration = 0.seconds),
            TimerConfiguration(laps = 1, workDuration = 60.seconds, restDuration = 0.seconds),
            TimerConfiguration(laps = 1, workDuration = 2.minutes, restDuration = 0.seconds),
            TimerConfiguration(laps = 5, workDuration = 45.seconds, restDuration = 15.seconds),
            TimerConfiguration(laps = 10, workDuration = 30.seconds, restDuration = 30.seconds),
            TimerConfiguration(laps = 20, workDuration = 20.seconds, restDuration = 10.seconds),
            TimerConfiguration(laps = 8, workDuration = 25.seconds, restDuration = 5.seconds),
            TimerConfiguration(laps = 999, workDuration = 25.seconds, restDuration = 5.seconds)
        )
    }
}