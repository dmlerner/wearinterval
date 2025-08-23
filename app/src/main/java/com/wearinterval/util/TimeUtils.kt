package com.wearinterval.util

import kotlin.time.Duration

object TimeUtils {

  /**
   * Formats a duration for display in UI components. Returns format like "2:30", "45s", "½s", or
   * "1:05" depending on the duration.
   */
  fun formatDuration(duration: Duration): String {
    // Handle sub-second durations with fraction symbols
    when (duration.inWholeMilliseconds) {
      250L -> return "¼s"
      500L -> return "½s"
    }

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
   * Formats a duration for shorter display contexts like complications. Returns format like "2:30"
   * or "45s" (optimized for space).
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

  /**
   * Formats a duration for short display contexts (alias for formatTimeCompact). Returns format
   * like "2:30" or "45s" (optimized for space).
   */
  fun formatDurationShort(duration: Duration): String = formatTimeCompact(duration)

  /**
   * Formats a duration with fixed-width padding to prevent layout shift in main timer display.
   * Returns format like " 2:30", " 45s", " ½s", or "12:05" with consistent spacing.
   */
  fun formatDurationFixedWidth(duration: Duration): String {
    // Handle sub-second durations with fraction symbols
    when (duration.inWholeMilliseconds) {
      250L -> return "  ¼s"
      500L -> return "  ½s"
    }

    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return when {
      minutes == 0L -> {
        if (seconds < 10) "  ${seconds}s" else " ${seconds}s"
      }
      minutes < 10 && seconds == 0L -> " $minutes:00"
      minutes < 10 -> " $minutes:%02d".format(seconds)
      seconds == 0L -> "$minutes:00"
      else -> "$minutes:%02d".format(seconds)
    }
  }
}
