package com.wearinterval.ui.screen.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTextThat
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun mainContent_displaysProgressRingsAndControls() {
    // Given
    val uiState =
      MainUiState(
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
    val uiState =
      MainUiState(
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
    val uiState =
      MainUiState(
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
    val uiState =
      MainUiState(
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
    val uiState =
      MainUiState(
        timerPhase = TimerPhase.Running,
        isStopButtonEnabled = true,
      )

    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = { eventReceived = it },
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
    val uiState =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        isStopButtonEnabled = false,
      )

    // When
    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = {},
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
          uiState =
            MainUiState(
              timeRemaining = 75.seconds,
              timerPhase = TimerPhase.Running,
            ),
          onEvent = {},
        )
      }
    }

    // Verify time displays as 1:15
    composeTestRule.onNodeWithText("1:15").assertIsDisplayed()
  }

  @Test
  fun mainContent_displaysRestPhaseIndicator() {
    // Given
    val uiState =
      MainUiState(
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
    val uiState =
      MainUiState(
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
        )
      }
    }

    // Then - Should show "Lap 15" for infinite laps
    composeTestRule.onNodeWithText("Lap 15").assertIsDisplayed()
  }

  @Test
  fun mainContent_displaysConfigurationWhenStopped() {
    // Given - Custom configuration with specific values
    val customConfig =
      TimerConfiguration(
        id = "test-config",
        laps = 15,
        workDuration = 90.seconds,
        restDuration = 45.seconds,
        lastUsed = System.currentTimeMillis(),
      )

    val uiState =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        configuration = customConfig,
        timeRemaining = 0.seconds, // Timer state should be ignored when stopped
        currentLap = 0, // Timer state should be ignored when stopped
        totalLaps = 0, // Timer state should be ignored when stopped
      )

    // When
    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = {},
        )
      }
    }

    // Then - Should display configuration values, not timer state values
    composeTestRule.onNodeWithText("1:30").assertIsDisplayed() // Work duration 90s = 1:30
    composeTestRule.onNodeWithText("15 laps").assertIsDisplayed() // Configuration laps
    composeTestRule.onNodeWithText("Rest: 45s").assertIsDisplayed() // Configuration rest duration
  }

  @Test
  fun mainContent_displaysInfiniteConfigurationWhenStopped() {
    // Given - Infinite laps configuration
    val infiniteConfig =
      TimerConfiguration(
        id = "infinite-config",
        laps = 999,
        workDuration = 2.minutes,
        restDuration = 30.seconds,
        lastUsed = System.currentTimeMillis(),
      )

    val uiState =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        configuration = infiniteConfig,
      )

    // When
    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = {},
        )
      }
    }

    // Then - Should display infinite symbol for laps
    composeTestRule.onNodeWithText("∞ laps").assertIsDisplayed()
    composeTestRule.onNodeWithText("2:00").assertIsDisplayed() // Work duration 2 minutes
    composeTestRule.onNodeWithText("Rest: 30s").assertIsDisplayed()
  }

  @Test
  fun mainContent_hidesRestDurationWhenZero() {
    // Given - Configuration with no rest duration
    val noRestConfig =
      TimerConfiguration(
        id = "no-rest-config",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 0.seconds,
        lastUsed = System.currentTimeMillis(),
      )

    val uiState =
      MainUiState(
        timerPhase = TimerPhase.Stopped,
        configuration = noRestConfig,
      )

    // When
    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = {},
        )
      }
    }

    // Then - Should not display rest duration when it's zero
    composeTestRule.onNodeWithText("45s").assertIsDisplayed() // Work duration
    composeTestRule.onNodeWithText("5 laps").assertIsDisplayed()
    composeTestRule.onNodeWithText("Rest: 0s").assertDoesNotExist() // Should not show zero rest
  }

  @Test
  fun mainContent_displaysCurrentTime() {
    // Given
    val uiState = MainUiState(timerPhase = TimerPhase.Stopped)

    // When
    composeTestRule.setContent {
      MaterialTheme {
        MainContent(
          uiState = uiState,
          onEvent = {},
        )
      }
    }

    // Then - Should display current time in H:mm format (e.g., "14:30", "9:05")
    composeTestRule.waitForIdle()
    composeTestRule
      .onNode(hasTextThat { text -> text.matches(Regex("^\\d{1,2}:\\d{2}$")) })
      .assertIsDisplayed()
  }
}
