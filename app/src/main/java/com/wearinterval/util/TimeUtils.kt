package com.wearinterval.util

import kotlin.time.Duration

object TimeUtils {

    /**
     * Formats a duration for display in UI components.
     * Returns format like "2:30", "45s", or "1:05" depending on the duration.
     */
    fun formatDuration(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return when {
            minutes == 0L -> "${seconds}s"
            seconds == 0L -> "$minutes:00"
            else -> "$minutes:%02d".format(seconds)
        }
    }

    /**
     * Formats a duration for shorter display contexts like complications.
     * Returns format like "2:30" or "45s" (optimized for space).
     */
    fun formatTimeCompact(duration: Duration): String {
        val totalSeconds = duration.inWholeSeconds
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60

        return if (minutes > 0) {
            "$minutes:%02d".format(seconds)
        } else {
            "${seconds}s"
        }
    }
}
