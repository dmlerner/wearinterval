package com.wearinterval.ui.screen.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsButtonsAreDisplayed() {
        // Given
        val uiState = SettingsUiState(
            vibrationEnabled = true,
            soundEnabled = false,
            autoModeEnabled = true,
            flashEnabled = false,
        )

        // When
        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = {},
                    onNavigateBack = {},
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithContentDescription("Toggle vibration")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Toggle sound")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Toggle auto mode")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Toggle screen flash")
            .assertIsDisplayed()
    }

    @Test
    fun vibrationButtonClickTriggersEvent() {
        // Given
        var eventReceived: SettingsEvent? = null
        val uiState = SettingsUiState(vibrationEnabled = false)

        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Toggle vibration")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(SettingsEvent.ToggleVibration)
    }

    @Test
    fun soundButtonClickTriggersEvent() {
        // Given
        var eventReceived: SettingsEvent? = null
        val uiState = SettingsUiState(soundEnabled = false)

        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Toggle sound")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(SettingsEvent.ToggleSound)
    }

    @Test
    fun autoModeButtonClickTriggersEvent() {
        // Given
        var eventReceived: SettingsEvent? = null
        val uiState = SettingsUiState(autoModeEnabled = false)

        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Toggle auto mode")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(SettingsEvent.ToggleAutoMode)
    }

    @Test
    fun flashButtonClickTriggersEvent() {
        // Given
        var eventReceived: SettingsEvent? = null
        val uiState = SettingsUiState(flashEnabled = false)

        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = { eventReceived = it },
                    onNavigateBack = {},
                )
            }
        }

        // When
        composeTestRule
            .onNodeWithContentDescription("Toggle screen flash")
            .performClick()

        // Then
        assertThat(eventReceived).isEqualTo(SettingsEvent.ToggleFlash)
    }

    @Test
    fun allButtonInteractionsWork() {
        // Given
        val eventsReceived = mutableListOf<SettingsEvent>()
        val uiState = SettingsUiState()

        composeTestRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    onEvent = { eventsReceived.add(it) },
                    onNavigateBack = {},
                )
            }
        }

        // When - Click all buttons
        composeTestRule
            .onNodeWithContentDescription("Toggle vibration")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Toggle sound")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Toggle auto mode")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Toggle screen flash")
            .performClick()

        // Then
        assertThat(eventsReceived).containsExactly(
            SettingsEvent.ToggleVibration,
            SettingsEvent.ToggleSound,
            SettingsEvent.ToggleAutoMode,
            SettingsEvent.ToggleFlash,
        )
    }
}
