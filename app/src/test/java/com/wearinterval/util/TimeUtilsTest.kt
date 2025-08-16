package com.wearinterval.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TimeUtilsTest {
    
    @Test
    fun `formatDuration handles seconds only`() {
        assertThat(TimeUtils.formatDuration(30.seconds)).isEqualTo("30s")
        assertThat(TimeUtils.formatDuration(5.seconds)).isEqualTo("5s")
        assertThat(TimeUtils.formatDuration(59.seconds)).isEqualTo("59s")
    }
    
    @Test
    fun `formatDuration handles minutes only`() {
        assertThat(TimeUtils.formatDuration(1.minutes)).isEqualTo("1:00")
        assertThat(TimeUtils.formatDuration(5.minutes)).isEqualTo("5:00")
        assertThat(TimeUtils.formatDuration(10.minutes)).isEqualTo("10:00")
    }
    
    @Test
    fun `formatDuration handles minutes and seconds`() {
        assertThat(TimeUtils.formatDuration(1.minutes + 30.seconds)).isEqualTo("1:30")
        assertThat(TimeUtils.formatDuration(2.minutes + 5.seconds)).isEqualTo("2:05")
        assertThat(TimeUtils.formatDuration(5.minutes + 45.seconds)).isEqualTo("5:45")
    }
    
    @Test
    fun `formatTimeCompact handles seconds only`() {
        assertThat(TimeUtils.formatTimeCompact(30.seconds)).isEqualTo("30s")
        assertThat(TimeUtils.formatTimeCompact(5.seconds)).isEqualTo("5s")
        assertThat(TimeUtils.formatTimeCompact(59.seconds)).isEqualTo("59s")
    }
    
    @Test
    fun `formatTimeCompact handles minutes and seconds`() {
        assertThat(TimeUtils.formatTimeCompact(1.minutes + 30.seconds)).isEqualTo("1:30")
        assertThat(TimeUtils.formatTimeCompact(2.minutes + 5.seconds)).isEqualTo("2:05")
        assertThat(TimeUtils.formatTimeCompact(5.minutes + 45.seconds)).isEqualTo("5:45")
        assertThat(TimeUtils.formatTimeCompact(1.minutes)).isEqualTo("1:00")
    }
    
    @Test
    fun `formatDuration and formatTimeCompact return same results for consistent formatting`() {
        val testDurations = listOf(
            30.seconds,
            1.minutes + 30.seconds,
            2.minutes + 5.seconds,
            5.minutes
        )
        
        testDurations.forEach { duration ->
            val formatted = TimeUtils.formatDuration(duration)
            val compact = TimeUtils.formatTimeCompact(duration)
            
            if (duration.inWholeMinutes > 0) {
                assertThat(compact).isEqualTo(formatted)
            } else {
                assertThat(compact).isEqualTo(formatted)
            }
        }
    }
    
    @Test
    fun `edge cases are handled correctly`() {
        assertThat(TimeUtils.formatDuration(0.seconds)).isEqualTo("0s")
        assertThat(TimeUtils.formatTimeCompact(0.seconds)).isEqualTo("0s")
    }
}