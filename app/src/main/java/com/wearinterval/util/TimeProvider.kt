package com.wearinterval.util

import java.time.Instant

/**
 * Abstraction over system time to enable testable time-dependent code.
 *
 * This interface allows injecting different time sources for production vs testing, making
 * time-sensitive calculations predictable and testable.
 */
interface TimeProvider {
  /** Returns the current time as an Instant. Equivalent to Instant.now() in production. */
  fun now(): Instant

  /**
   * Returns the current time in milliseconds since Unix epoch. Provided for compatibility with
   * legacy code that needs milliseconds.
   */
  fun currentTimeMillis(): Long = now().toEpochMilli()
}

/** Production implementation that uses actual system time. */
class SystemTimeProvider : TimeProvider {
  override fun now(): Instant = Instant.now()
}
