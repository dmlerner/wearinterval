package com.wearinterval.ui.screen.config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun configPickerColumnsAreDisplayed() {
        // Given
        val uiState = ConfigUiState(
            laps = 5,
            workMinutes = 1,
            workSeconds = 30,
            restMinutes = 0,
            restSeconds = 15,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then - Check column titles
        composeTestRule.onNodeWithText("Laps").assertIsDisplayed()
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Rest").assertIsDisplayed()

        // Then - Check values
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("1:30").assertIsDisplayed()
        composeTestRule.onNodeWithText("15s").assertIsDisplayed()
    }

    @Test
    fun incrementButtonsAreDisplayed() {
        // Given
        val uiState = ConfigUiState()

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Increase laps")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Increase work duration")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Increase rest duration")
            .assertIsDisplayed()
    }

    @Test
    fun decrementButtonsAreDisplayed() {
        // Given
        val uiState = ConfigUiState()

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Decrease laps")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Decrease work duration")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Decrease rest duration")
            .assertIsDisplayed()
    }

    @Test
    fun resetButtonIsDisplayed() {
        // Given
        val uiState = ConfigUiState()

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then - No reset button in new design
        // Test that scroll pickers are displayed instead
        composeTestRule
            .onNodeWithContentDescription("Tap to reset , long press for alternate value")
            .assertIsDisplayed()
    }

    @Test
    fun lapsResetTapTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState(laps = 3)

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Tap to reset , long press for alternate value")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.ResetLaps)
    }

    @Test
    fun lapsLongPressTriggersInfiniteEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState(laps = 3)

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Tap to reset , long press for alternate value")
            .performTouchInput { longClick() }

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.SetLapsToInfinite)
    }

    @Test
    fun workResetTapTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When - Find the work duration picker tap area
        composeTestRule
            .onAllNodesWithContentDescription("Tap to reset , long press for alternate value")
            .onFirst()
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.ResetWork)
    }

    @Test
    fun workLongPressTriggersLongEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When - Find the work duration picker tap area
        composeTestRule
            .onAllNodesWithContentDescription("Tap to reset , long press for alternate value")
            .onFirst()
            .performTouchInput { longClick() }

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.SetWorkToLong)
    }

    @Test
    fun restResetTapTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When - Find the rest duration picker tap area (third one)
        composeTestRule
            .onAllNodesWithContentDescription("Tap to reset Rest, long press for alternate value")
            .onFirst()
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.ResetRest)
    }

    @Test
    fun restLongPressTriggersLongEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When - Find the rest duration picker tap area (third one)
        composeTestRule
            .onAllNodesWithContentDescription("Tap to reset Rest, long press for alternate value")
            .onFirst()
            .performTouchInput { longClick() }

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.SetRestToLong)
    }

    @Test
    fun uiStateDisplaysCorrectTimeFormats() {
        // Given
        val uiState = ConfigUiState(
            laps = 15,
            workMinutes = 2,
            workSeconds = 45,
            restMinutes = 1,
            restSeconds = 30,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("2:45").assertIsDisplayed()
        composeTestRule.onNodeWithText("1:30").assertIsDisplayed()
    }

    @Test
    fun zeroRestDurationDisplaysNone() {
        // Given
        val uiState = ConfigUiState(
            laps = 1,
            workMinutes = 1,
            workSeconds = 0,
            restMinutes = 0,
            restSeconds = 0,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("None").assertIsDisplayed()
    }
}
