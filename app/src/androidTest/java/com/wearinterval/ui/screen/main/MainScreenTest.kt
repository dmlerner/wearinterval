package com.wearinterval.ui.screen.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerPhase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainContent_displaysProgressRingsAndControls() {
        // Given
        val uiState = MainUiState(
            currentLap = 3,
            totalLaps = 10,
            timeRemaining = 45.seconds,
            timerPhase = TimerPhase.Running,
            isPlayButtonEnabled = true,
            isStopButtonEnabled = true,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then - Time display
        composeTestRule.onNodeWithText("45s").assertIsDisplayed()

        // Then - Lap indicator
        composeTestRule.onNodeWithText("3/10").assertIsDisplayed()

        // Then - Control buttons
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Stop").assertIsDisplayed()
    }

    @Test
    fun mainContent_showsPlayButtonWhenStopped() {
        // Given
        val uiState = MainUiState(
            timerPhase = TimerPhase.Stopped,
            isPlayButtonEnabled = true,
            isStopButtonEnabled = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Play").assertIsEnabled()
    }

    @Test
    fun mainContent_showsPauseButtonWhenRunning() {
        // Given
        val uiState = MainUiState(
            timerPhase = TimerPhase.Running,
            isPlayButtonEnabled = true,
            isStopButtonEnabled = true,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pause").assertIsEnabled()
    }

    @Test
    fun mainContent_showsResumeButtonWhenPaused() {
        // Given
        val uiState = MainUiState(
            timerPhase = TimerPhase.Paused,
            isPaused = true,
            isPlayButtonEnabled = true,
            isStopButtonEnabled = true,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Resume").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Resume").assertIsEnabled()
    }

    @Test
    fun playButton_triggersPlayPauseEvent() {
        // Given
        var eventReceived: MainEvent? = null
        val uiState = MainUiState(timerPhase = TimerPhase.Stopped)

        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Play").performClick()

        // Then
        assertThat(eventReceived).isEqualTo(MainEvent.PlayPauseClicked)
    }

    @Test
    fun stopButton_triggersStopEvent() {
        // Given
        var eventReceived: MainEvent? = null
        val uiState = MainUiState(
            timerPhase = TimerPhase.Running,
            isStopButtonEnabled = true,
        )

        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // When
        composeTestRule.onNodeWithContentDescription("Stop").performClick()

        // Then
        assertThat(eventReceived).isEqualTo(MainEvent.StopClicked)
    }

    @Test
    fun stopButton_isDisabledWhenStopped() {
        // Given
        val uiState = MainUiState(
            timerPhase = TimerPhase.Stopped,
            isStopButtonEnabled = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Stop").assertIsNotEnabled()
    }

    @Test
    fun mainContent_displaysCorrectTimeFormats() {
        // Test different time formats
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = MainUiState(
                        timeRemaining = 75.seconds,
                        timerPhase = TimerPhase.Running,
                    ),
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Verify time displays as 1:15
        composeTestRule.onNodeWithText("1:15").assertIsDisplayed()
    }

    @Test
    fun mainContent_displaysRestPhaseIndicator() {
        // Given
        val uiState = MainUiState(
            timerPhase = TimerPhase.Resting,
            timeRemaining = 30.seconds,
            currentLap = 5,
            totalLaps = 10,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("REST").assertIsDisplayed()
        composeTestRule.onNodeWithText("30s").assertIsDisplayed()
        composeTestRule.onNodeWithText("5/10").assertIsDisplayed()
    }

    @Test
    fun mainContent_displaysInfiniteLapsCorrectly() {
        // Given
        val uiState = MainUiState(
            currentLap = 15,
            totalLaps = 999, // Infinite laps
            timerPhase = TimerPhase.Running,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                MainContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateToConfig = {},
                    onNavigateToHistory = {},
                    onNavigateToSettings = {},
                )
            }
        }

        // Then - Should show "Lap 15" for infinite laps
        composeTestRule.onNodeWithText("Lap 15").assertIsDisplayed()
    }
}
