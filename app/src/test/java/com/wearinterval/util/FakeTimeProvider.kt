package com.wearinterval.util

import java.time.Instant
import kotlin.time.Duration

/**
 * Fake implementation of TimeProvider for testing.
 *
 * Allows tests to control time and make time-dependent calculations predictable. Useful for testing
 * timer logic, animations, and other time-sensitive operations.
 */
class FakeTimeProvider(private var currentTime: Instant = Instant.EPOCH) : TimeProvider {

  override fun now(): Instant = currentTime

  /** Sets the current time to the specified instant. */
  fun setCurrentTime(time: Instant) {
    currentTime = time
  }

  /** Sets the current time to the specified value in milliseconds since epoch. */
  fun setCurrentTimeMillis(timeMillis: Long) {
    currentTime = Instant.ofEpochMilli(timeMillis)
  }

  /** Advances the current time by the specified amount. */
  fun advanceTimeBy(duration: Duration) {
    currentTime = currentTime.plusMillis(duration.inWholeMilliseconds)
  }

  /** Advances the current time by the specified amount in milliseconds. */
  fun advanceTimeBy(durationMillis: Long) {
    currentTime = currentTime.plusMillis(durationMillis)
  }
}
