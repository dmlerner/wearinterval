package com.wearinterval.util

/**
 * Abstraction over system time to enable testable time-dependent code.
 *
 * This interface allows injecting different time sources for production vs testing, making
 * time-sensitive calculations predictable and testable.
 */
interface TimeProvider {
  /**
   * Returns the current time in milliseconds since Unix epoch. Equivalent to
   * System.currentTimeMillis() in production.
   */
  fun currentTimeMillis(): Long
}

/** Production implementation that uses actual system time. */
class SystemTimeProvider : TimeProvider {
  override fun currentTimeMillis(): Long = System.currentTimeMillis()
}
