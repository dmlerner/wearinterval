package com.wearinterval.util

import android.util.Log
import com.wearinterval.BuildConfig

/**
 * Centralized logging utility that can be easily disabled for release builds. Automatically
 * disables logging in release builds via BuildConfig.ENABLE_LOGGING.
 */
object Logger {

  // Enable/disable all logging - automatically false in release builds
  private val LOGGING_ENABLED = BuildConfig.ENABLE_LOGGING

  private const val DEFAULT_TAG = "WearInterval"

  fun d(tag: String = DEFAULT_TAG, message: String) {
    if (LOGGING_ENABLED) {
      Log.d(tag, message)
    }
  }

  fun i(tag: String = DEFAULT_TAG, message: String) {
    if (LOGGING_ENABLED) {
      Log.i(tag, message)
    }
  }

  fun w(tag: String = DEFAULT_TAG, message: String) {
    if (LOGGING_ENABLED) {
      Log.w(tag, message)
    }
  }

  fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
    if (LOGGING_ENABLED) {
      Log.w(tag, message, throwable)
    }
  }

  fun e(tag: String = DEFAULT_TAG, message: String) {
    if (LOGGING_ENABLED) {
      Log.e(tag, message)
    }
  }

  fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable) {
    if (LOGGING_ENABLED) {
      Log.e(tag, message, throwable)
    }
  }

  fun v(tag: String = DEFAULT_TAG, message: String) {
    if (LOGGING_ENABLED) {
      Log.v(tag, message)
    }
  }

  // Convenience methods with automatic tagging
  fun heartRate(message: String) = d("HeartRate", message)

  fun heartRateError(message: String, throwable: Throwable? = null) {
    if (throwable != null) {
      e("HeartRate", message, throwable)
    } else {
      e("HeartRate", message)
    }
  }

  fun timer(message: String) = d("Timer", message)

  fun ui(message: String) = d("UI", message)

  fun main(message: String) = d("Main", message)
}
