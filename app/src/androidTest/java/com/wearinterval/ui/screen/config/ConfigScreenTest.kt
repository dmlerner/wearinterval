package com.wearinterval.ui.screen.config

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            restSeconds = 15
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {}
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
                    onNavigateBack = {}
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
                    onNavigateBack = {}
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
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Reset to default")
            .assertIsDisplayed()
    }

    @Test
    fun increaseLapsButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState(laps = 3)

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Increase laps")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.IncreaseLaps)
    }

    @Test
    fun decreaseLapsButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState(laps = 3)

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Decrease laps")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.DecreaseLaps)
    }

    @Test
    fun increaseWorkDurationButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Increase work duration")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.IncreaseWorkDuration)
    }

    @Test
    fun decreaseWorkDurationButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Decrease work duration")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.DecreaseWorkDuration)
    }

    @Test
    fun increaseRestDurationButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Increase rest duration")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.IncreaseRestDuration)
    }

    @Test
    fun decreaseRestDurationButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Decrease rest duration")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.DecreaseRestDuration)
    }

    @Test
    fun resetButtonTriggersEvent() {
        // Given
        var eventReceived: ConfigEvent? = null
        val uiState = ConfigUiState()

        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {}
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Reset to default")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(ConfigEvent.Reset)
    }

    @Test
    fun uiStateDisplaysCorrectTimeFormats() {
        // Given
        val uiState = ConfigUiState(
            laps = 15,
            workMinutes = 2,
            workSeconds = 45,
            restMinutes = 1,
            restSeconds = 30
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {}
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
            restSeconds = 0
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                ConfigContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("None").assertIsDisplayed()
    }
}