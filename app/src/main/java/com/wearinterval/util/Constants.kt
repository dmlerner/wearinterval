package com.wearinterval.util

import androidx.compose.ui.graphics.Color
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Shared constants used throughout the WearInterval application. Centralizes magic numbers for
 * maintainability and consistency.
 */
object Constants {

  // Timer Configuration Limits
  object TimerLimits {
    const val INFINITE_LAPS = 999
    const val MIN_LAPS = 1
    const val MAX_LAPS = INFINITE_LAPS

    val MIN_WORK_DURATION = 1.seconds
    val MAX_WORK_DURATION = 10.minutes
    val MIN_REST_DURATION = 0.seconds
    val MAX_REST_DURATION = 10.minutes
  }

  // Timer Service Configuration
  object TimerService {
    val UPDATE_INTERVAL = 100.milliseconds
    val INTERVAL_TRANSITION_DELAY = 500.milliseconds
    val WORKOUT_COMPLETION_DELAY = 2.seconds
  }

  // UI Timing and Animation
  object UI {
    val FLASH_DURATION = 500.milliseconds
    val SCROLL_PICKER_DEBOUNCE = 100.milliseconds
    const val SUBSCRIPTION_TIMEOUT = 5_000L // WhileSubscribed timeout
    const val STRING_PADDING_WIDTH = 2 // for padStart operations
  }

  // UI Dimensions (in dp)
  object Dimensions {
    // Progress Rings
    const val PROGRESS_RING_DEFAULT_SIZE = 120
    const val PROGRESS_RING_DEFAULT_STROKE = 8
    const val PROGRESS_RING_DUAL_SIZE = 140
    const val PROGRESS_RING_OUTER_STROKE = 6
    const val PROGRESS_RING_INNER_STROKE = 4
    const val PROGRESS_RING_GAP = 12

    // Button Sizes
    const val MAIN_PLAY_BUTTON_SIZE = 56
    const val MAIN_STOP_BUTTON_SIZE = 56
    const val HISTORY_ACTION_BUTTON_SIZE = 40

    // Spacing
    const val COMPONENT_SPACING = 12
    const val CONTROL_BUTTON_SPACING = 16
    const val SMALL_SPACING = 4
    const val MEDIUM_SPACING = 8
    const val LARGE_SPACING = 24

    // ScrollablePicker (now using Wear OS Picker)
    const val SCROLL_PICKER_CONTAINER_PADDING = 4
    const val SCROLL_PICKER_CORNER_RADIUS = 12

    // History Screen
    const val HISTORY_ITEM_HEIGHT = 56
    const val HISTORY_BUTTON_CORNER_RADIUS = 20
    const val RECENT_CONFIGURATIONS_COUNT =
      6 // Configurable number of recent configurations to show

    // Grid Layout (shared between Compose and Tile)
    const val GRID_ITEM_WIDTH = 62
    const val GRID_ITEM_HEIGHT = 48
    const val GRID_COLUMNS = 2
    const val GRID_ITEM_SPACING = 6
    const val GRID_PADDING = 4
    const val GRID_CORNER_RADIUS = 8
    const val GRID_TEXT_SIZE_SP = 14
  }

  // UI Colors
  object Colors {
    // Progress Ring Colors
    val PROGRESS_RING_OUTER_COLOR = Color(0xFF2196F3) // Material Design blue for outer ring
    val PROGRESS_RING_INNER_COLOR = Color(0xFF0099FF) // Electric bright blue for inner ring
    val PROGRESS_RING_DEFAULT_BACKGROUND = Color.Gray
    val PROGRESS_RING_DEFAULT_PROGRESS = Color.Blue

    // Control Button Colors
    val STOP_BUTTON_BACKGROUND = Color.Red
    val STOP_BUTTON_ICON = Color.White
    val PLAY_BUTTON_BACKGROUND = Color.Green
    val PLAY_BUTTON_ICON = Color.Black

    // ScrollablePicker Colors (now handled by Wear OS Picker theme)

    // Background Colors
    val CONFIG_SECTION_BACKGROUND = Color.White.copy(alpha = 0.08f) // Subtle background
    val HISTORY_ITEM_BACKGROUND = Color(0xFF222222) // Dark background for history items

    // Text Colors
    val HISTORY_ITEM_TEXT = Color(0xFFBBBBBB) // Light gray text
    val DIVIDER_COLOR = Color.Gray

    // Heart Rate Zone Colors
    val HEART_RATE_ZONE_1 = Color(0xFF808080) // Zone 1 (50-60%): Gray
    val HEART_RATE_ZONE_2 = Color(0xFF2196F3) // Zone 2 (60-70%): Blue
    val HEART_RATE_ZONE_3 = Color(0xFF4CAF50) // Zone 3 (70-80%): Green
    val HEART_RATE_ZONE_4 = Color(0xFFFFC107) // Zone 4 (80-90%): Yellow
    val HEART_RATE_ZONE_5 = Color(0xFFFF5722) // Zone 5 (90-100%): Red/Orange
    val HEART_RATE_DEFAULT = Color(0xFFBBBBBB) // Default: Light gray

    // Tile-specific ARGB values for Wear Tiles API compatibility
    object Tile {
      const val HISTORY_ITEM_BACKGROUND_ARGB = 0xFF222222.toInt()
      const val HISTORY_ITEM_TEXT_ARGB = 0xFFBBBBBB.toInt()
      const val DIVIDER_COLOR_ARGB = 0xFF808080.toInt() // Color.Gray equivalent
      const val WHITE_ARGB = 0xFFFFFFFF.toInt()
    }
  }

  // Notification Configuration
  object Notifications {
    const val TIMER_CHANNEL_ID = "timer_service_channel"
    const val ALERT_CHANNEL_ID = "timer_alert_channel"

    const val TIMER_NOTIFICATION_ID = 1
    const val ALERT_NOTIFICATION_ID = 2
  }

  // Test Values (commonly used in tests)
  object TestValues {
    val COMMON_WORK_DURATION = 30.seconds
    val COMMON_REST_DURATION = 15.seconds
    val COMMON_LONG_WORK = 2.minutes
    val COMMON_LONG_REST = 45.seconds
    const val COMMON_LAPS = 5
    const val TEST_TIMEOUT = 1000L
  }

  // Data Validation
  object DataLimits {
    const val MAX_ID_LENGTH = 100
    const val MAX_LAPS_FOR_VALIDATION = 999
    const val MIN_INT_VALUE = 1
    const val MAX_INT_VALUE = 999
  }

  // Heart Rate Zone Configuration
  object HeartRate {
    const val DEFAULT_MAX_HEART_RATE = 220
    const val DEFAULT_AGE = 35 // Default age for max HR calculation

    // Zone thresholds as percentages of max heart rate
    const val ZONE_1_MIN = 50
    const val ZONE_1_MAX = 60
    const val ZONE_2_MIN = 60
    const val ZONE_2_MAX = 70
    const val ZONE_3_MIN = 70
    const val ZONE_3_MAX = 80
    const val ZONE_4_MIN = 80
    const val ZONE_4_MAX = 90
    const val ZONE_5_MIN = 90
    const val ZONE_5_MAX = 100
  }
}
