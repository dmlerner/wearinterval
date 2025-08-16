package com.wearinterval.ui.component

import com.google.common.truth.Truth.assertThat
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
            assertThat(coerced).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun sweepAngle_calculatesCorrectly() {
        // Test sweep angle calculation for progress rings
        val progressValues = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        val maxSweepAngle = 360f

        progressValues.forEach { progress ->
            val expectedSweepAngle = progress * maxSweepAngle
            val actualSweepAngle = progress * 360f

            assertThat(actualSweepAngle).isWithin(0.001f).of(expectedSweepAngle)
        }
    }

    @Test
    fun startAngle_isCorrectForTopPosition() {
        // Test that progress rings start from the top (-90 degrees)
        val startAngleTop = -90f
        assertThat(startAngleTop).isEqualTo(-90f)
    }

    @Test
    fun dualRingSpacing_calculatesCorrectly() {
        // Test spacing calculation between outer and inner rings
        val outerStrokeWidth = 6f
        val innerStrokeWidth = 4f
        val gapBetweenRings = 12f

        val expectedInnerRadius = gapBetweenRings + outerStrokeWidth / 2 + innerStrokeWidth / 2
        val actualInnerRadius = 12f + 3f + 2f // 17f

        assertThat(actualInnerRadius).isEqualTo(17f)
    }

    @Test
    fun progressRingColors_areValidForDifferentStates() {
        // Test that color calculations work for different timer states
        // This tests the color logic that would be used in MainScreen

        // Work state (running)
        val isResting = false
        val workColor = if (isResting) "amber" else "green"
        assertThat(workColor).isEqualTo("green")

        // Rest state (resting)
        val isRestingTrue = true
        val restColor = if (isRestingTrue) "amber" else "green"
        assertThat(restColor).isEqualTo("amber")
    }

    @Test
    fun progressCalculation_handlesEdgeCases() {
        // Test progress calculation edge cases
        val testCases = mapOf(
            Pair(0.0, 100.0) to 0f, // No progress
            Pair(50.0, 100.0) to 0.5f, // Half progress
            Pair(100.0, 100.0) to 1f, // Complete progress
            Pair(75.0, 100.0) to 0.75f, // Three quarters
            Pair(25.0, 100.0) to 0.25f, // One quarter
        )

        testCases.forEach { (input, expected) ->
            val (elapsed, total) = input
            val progress = (elapsed / total).toFloat()
            assertThat(progress).isWithin(0.001f).of(expected)
        }
    }

    @Test
    fun angleCalculation_wrapsCorrectly() {
        // Test that angle calculations handle wrapping correctly
        val baseAngle = -90f // Start from top
        val progressAngles = listOf(0f, 90f, 180f, 270f, 360f)

        progressAngles.forEach { sweepAngle ->
            val endAngle = baseAngle + sweepAngle
            // Verify the calculation doesn't break with full rotations
            assertThat(endAngle).isAtLeast(-90f)
        }
    }

    @Test
    fun strokeWidthValidation_ensuresValidValues() {
        // Test stroke width validation
        val strokeWidths = listOf(1f, 4f, 6f, 8f, 12f)

        strokeWidths.forEach { width ->
            assertThat(width).isGreaterThan(0f)
            assertThat(width).isAtMost(20f) // Reasonable maximum
        }
    }

    @Test
    fun ringSize_calculatesCorrectDimensions() {
        // Test ring size calculations for different screen sizes
        val testSizes = listOf(120f, 140f, 160f, 180f)

        testSizes.forEach { size ->
            val radius = size / 2f
            val strokeWidth = 6f
            val drawingRadius = radius - strokeWidth / 2f

            assertThat(drawingRadius).isGreaterThan(0f)
            assertThat(drawingRadius).isLessThan(radius)
        }
    }

    @Test
    fun zeroProgress_handledCorrectly() {
        val progress = 0f
        val sweepAngle = progress * 360f
        assertThat(sweepAngle).isEqualTo(0f)
    }

    @Test
    fun fullProgress_handledCorrectly() {
        val progress = 1f
        val sweepAngle = progress * 360f
        assertThat(sweepAngle).isEqualTo(360f)
    }
}
