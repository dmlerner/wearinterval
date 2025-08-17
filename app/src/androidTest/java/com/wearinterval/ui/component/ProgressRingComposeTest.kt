package com.wearinterval.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for ProgressRing component visual rendering.
 * Tests the actual composition and rendering behavior.
 */
class ProgressRingComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun progressRing_rendersWithCenterContent() {
        composeTestRule.setContent {
            TestProgressRing()
        }

        // Verify center content is displayed
        composeTestRule.onNodeWithText("50%").assertIsDisplayed()
    }

    @Test
    fun dualProgressRings_rendersWithCenterContent() {
        composeTestRule.setContent {
            TestDualProgressRings()
        }

        // Verify center content is displayed
        composeTestRule.onNodeWithText("30s").assertIsDisplayed()
        composeTestRule.onNodeWithText("5/10").assertIsDisplayed()
    }

    @Test
    fun progressRing_handlesZeroProgress() {
        composeTestRule.setContent {
            ProgressRing(
                progress = 0f,
                progressColor = Color.Blue,
            ) {
                Text("0%")
            }
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun progressRing_handlesFullProgress() {
        composeTestRule.setContent {
            ProgressRing(
                progress = 1f,
                progressColor = Color.Green,
            ) {
                Text("100%")
            }
        }

        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun dualProgressRings_handlesIndependentProgress() {
        composeTestRule.setContent {
            DualProgressRings(
                outerProgress = 0.3f,
                innerProgress = 0.8f,
                outerColor = Color.Blue,
                innerColor = Color.Red,
            ) {
                Text("Independent")
            }
        }

        composeTestRule.onNodeWithText("Independent").assertIsDisplayed()
    }

    @Composable
    private fun TestProgressRing() {
        MaterialTheme {
            ProgressRing(
                progress = 0.5f,
                progressColor = Color.Blue,
            ) {
                Text("50%")
            }
        }
    }

    @Composable
    private fun TestDualProgressRings() {
        MaterialTheme {
            DualProgressRings(
                outerProgress = 0.5f,
                innerProgress = 0.75f,
                outerColor = Color.Blue,
                innerColor = Color.Green,
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                ) {
                    Text("30s")
                    Text("5/10")
                }
            }
        }
    }
}
