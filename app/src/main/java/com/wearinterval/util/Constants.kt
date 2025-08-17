package com.wearinterval.util

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Shared constants used throughout the WearInterval application.
 * Centralizes magic numbers for maintainability and consistency.
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
        val COUNTDOWN_DECREMENT = 100.milliseconds
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

        // ScrollablePicker
        const val SCROLL_PICKER_ITEM_SPACING = 12
        const val SCROLL_PICKER_PADDING_HEIGHT = 40
        const val SCROLL_PICKER_ITEM_VERTICAL_PADDING = 6
        const val SCROLL_PICKER_CONTAINER_PADDING = 4
        const val SCROLL_PICKER_CORNER_RADIUS = 12

        // History Screen
        const val HISTORY_ITEM_HEIGHT = 56
        const val HISTORY_BUTTON_CORNER_RADIUS = 20
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
}
