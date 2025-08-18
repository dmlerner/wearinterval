package com.wearinterval.util

import android.util.Log

/**
 * Centralized debug logging utility with flag control for performance-sensitive areas.
 *
 * This allows us to keep detailed debug logs in the code but disable them in production or when
 * investigating performance issues.
 */
object DebugLogger {

  /**
   * Master debug flag - set to false to disable all debug logging app-wide. This should be false in
   * production builds.
   */
  private const val DEBUG_ENABLED = true

  /** Specific debug flags for different components */
  private const val UI_COMPOSITION_DEBUG = false
  private const val SCROLL_PICKER_DEBUG = false
  private const val CONFIG_SCREEN_DEBUG = false
  private const val NAVIGATION_DEBUG = false

  /** Log UI composition events (recompositions, state changes) */
  fun logComposition(tag: String, message: String) {
    if (DEBUG_ENABLED && UI_COMPOSITION_DEBUG) {
      Log.d(tag, "[COMPOSITION] $message")
    }
  }

  /** Log scroll picker events (selections, scrolling, haptic feedback) */
  fun logScrollPicker(tag: String, message: String) {
    if (DEBUG_ENABLED && SCROLL_PICKER_DEBUG) {
      Log.d(tag, "[PICKER] $message")
    }
  }

  /** Log config screen events (value changes, calculations) */
  fun logConfigScreen(tag: String, message: String) {
    if (DEBUG_ENABLED && CONFIG_SCREEN_DEBUG) {
      Log.d(tag, "[CONFIG] $message")
    }
  }

  /** Log navigation events (page changes, swipes) */
  fun logNavigation(tag: String, message: String) {
    if (DEBUG_ENABLED && NAVIGATION_DEBUG) {
      Log.d(tag, "[NAV] $message")
    }
  }

  /** Log general debug messages (always enabled when DEBUG_ENABLED is true) */
  fun logDebug(tag: String, message: String) {
    if (DEBUG_ENABLED) {
      Log.d(tag, message)
    }
  }

  /** Log errors (always enabled regardless of debug flags) */
  fun logError(tag: String, message: String, throwable: Throwable? = null) {
    if (throwable != null) {
      Log.e(tag, message, throwable)
    } else {
      Log.e(tag, message)
    }
  }

  /** Log warnings (always enabled regardless of debug flags) */
  fun logWarning(tag: String, message: String) {
    Log.w(tag, message)
  }
}
