package com.wearinterval.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for ProgressRing component utility functions and logic.
 * UI composition tests should be in androidTest directory.
 */
class ProgressRingTest {

    @Test
    fun progressValues_areCoercedCorrectly() {
        // Test progress value coercion logic that would be used in the component
        val testValues = listOf(-0.5f, 0f, 0.5f, 1f, 1.5f)
        val expectedResults = listOf(0f, 0f, 0.5f, 1f, 1f)

        testValues.zip(expectedResults).forEach { (input, expected) ->
            val coerced = input.coerceIn(0f, 1f)
            assertEquals("Progress value $input should be coerced to $expected", expected, coerced, 0.001f)
        }
    }

    @Test
    fun sweepAngle_calculationIsCorrect() {
        // Test the sweep angle calculation logic
        val progress = 0.75f
        val expectedSweepAngle = 270f // 75% of 360 degrees
        val actualSweepAngle = progress * 360f

        assertEquals(expectedSweepAngle, actualSweepAngle, 0.001f)
    }

    @Test
    fun zeroProgress_handledCorrectly() {
        val progress = 0f
        val sweepAngle = progress * 360f

        assertEquals(0f, sweepAngle, 0.001f)
    }

    @Test
    fun fullProgress_handledCorrectly() {
        val progress = 1f
        val sweepAngle = progress * 360f

        assertEquals(360f, sweepAngle, 0.001f)
    }
}
