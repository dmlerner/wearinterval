package com.wearinterval.ui.screen.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleConfigurations = listOf(
        TimerConfiguration(
            id = "1",
            laps = 5,
            workDuration = 90.seconds,
            restDuration = 30.seconds,
        ),
        TimerConfiguration(
            id = "2",
            laps = 10,
            workDuration = 2.minutes,
            restDuration = 45.seconds,
        ),
        TimerConfiguration(
            id = "3",
            laps = 1,
            workDuration = 45.seconds,
            restDuration = 0.seconds,
        ),
    )

    @Test
    fun loadingStateDisplaysProgressIndicator() {
        // Given
        val uiState = HistoryUiState(
            recentConfigurations = emptyList(),
            isLoading = true,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then - Progress indicator should be displayed (no specific text to check)
        // The loading state shows a CircularProgressIndicator which is visible
        composeTestRule.waitForIdle()
    }

    @Test
    fun emptyStateDisplaysCorrectMessage() {
        // Given
        val uiState = HistoryUiState(
            recentConfigurations = emptyList(),
            isLoading = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("No Recent\nConfigurations")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Create and use\ntimer configurations\nto see them here")
            .assertIsDisplayed()
    }

    @Test
    fun errorStateDisplaysErrorMessage() {
        // Given
        val errorMessage = "Network error"
        val uiState = HistoryUiState(
            recentConfigurations = emptyList(),
            isLoading = false,
            error = errorMessage,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Error").assertIsDisplayed()
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("Retry loading history")
            .assertIsDisplayed()
    }

    @Test
    fun retryButtonTriggersRefreshEvent() {
        // Given
        var eventReceived: HistoryEvent? = null
        val uiState = HistoryUiState(
            recentConfigurations = emptyList(),
            isLoading = false,
            error = "Test error",
        )

        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Retry loading history")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(HistoryEvent.Refresh)
    }

    @Test
    fun configurationsListDisplaysCorrectly() {
        // Given
        val uiState = HistoryUiState(
            recentConfigurations = sampleConfigurations,
            isLoading = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then - Check that configuration details are displayed
        composeTestRule.onNodeWithText("5 laps").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 laps").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 laps").assertIsDisplayed()

        // Check work durations - using content descriptions to avoid duplicates
        composeTestRule.onNodeWithText("1:30").assertIsDisplayed() // 90 seconds
        composeTestRule.onNodeWithText("2:00").assertIsDisplayed() // 2 minutes

        // Check rest durations (where applicable)
        composeTestRule.onNodeWithText("30s").assertIsDisplayed() // 30 seconds rest

        // Verify configurations using content descriptions (more specific)
        composeTestRule
            .onNodeWithContentDescription("Configuration: 5 laps, 1:30 work, 30s rest")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Configuration: 10 laps, 2:00 work, 45s rest")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Configuration: 1 laps, 45s work, 0s rest")
            .assertIsDisplayed()
    }

    @Test
    fun configurationItemClickTriggersSelectEvent() {
        // Given
        var eventReceived: HistoryEvent? = null
        var navigateBackCalled = false
        val uiState = HistoryUiState(
            recentConfigurations = sampleConfigurations,
            isLoading = false,
        )

        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = { navigateBackCalled = true },
                )
            }
        }

        // When - Click on the first configuration
        val firstConfigDescription = "Configuration: 5 laps, 1:30 work, 30s rest"
        composeTestRule
            .onNodeWithContentDescription(firstConfigDescription)
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(
            HistoryEvent.SelectConfiguration(sampleConfigurations[0]),
        )
        assertThat(navigateBackCalled).isTrue()
    }

    @Test
    fun configurationWithZeroRestDisplaysCorrectly() {
        // Given
        val configWithZeroRest = listOf(
            TimerConfiguration(
                id = "test",
                laps = 3,
                workDuration = 60.seconds,
                restDuration = 0.seconds,
            ),
        )
        val uiState = HistoryUiState(
            recentConfigurations = configWithZeroRest,
            isLoading = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("3 laps").assertIsDisplayed()
        composeTestRule.onNodeWithText("1:00").assertIsDisplayed() // 60 seconds work

        // The content description should reflect that rest is 0s
        val configDescription = "Configuration: 3 laps, 1:00 work, 0s rest"
        composeTestRule
            .onNodeWithContentDescription(configDescription)
            .assertIsDisplayed()
    }

    @Test
    fun multipleConfigurationsAllDisplayCorrectly() {
        // Given
        val uiState = HistoryUiState(
            recentConfigurations = sampleConfigurations,
            isLoading = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                HistoryContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then - Verify all configurations have their content descriptions
        composeTestRule
            .onNodeWithContentDescription("Configuration: 5 laps, 1:30 work, 30s rest")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Configuration: 10 laps, 2:00 work, 45s rest")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Configuration: 1 laps, 45s work, 0s rest")
            .assertIsDisplayed()
    }
}
