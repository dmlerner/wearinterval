package com.wearinterval.util

/**
 * Fake implementation of TimeProvider for testing.
 *
 * Allows tests to control time and make time-dependent calculations predictable. Useful for testing
 * timer logic, animations, and other time-sensitive operations.
 */
class FakeTimeProvider(private var currentTime: Long = 0L) : TimeProvider {

  override fun currentTimeMillis(): Long = currentTime

  /**
   * Sets the current time to the specified value. Subsequent calls to currentTimeMillis() will
   * return this value.
   */
  fun setCurrentTimeMillis(timeMillis: Long) {
    currentTime = timeMillis
  }

  /** Advances the current time by the specified amount. */
  fun advanceTimeBy(durationMillis: Long) {
    currentTime += durationMillis
  }
}
