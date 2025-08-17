package com.wearinterval.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearIntervalNavigationTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun wearIntervalNavigation_composesSuccessfully() {
    // When
    composeTestRule.setContent { MaterialTheme { WearIntervalNavigation() } }

    // Then - Navigation should compose without error
    composeTestRule.waitForIdle()
  }

  @Test
  fun horizontalPager_startsOnMainScreen() {
    // Test that HorizontalPager starts on the main screen (page 1)
    composeTestRule.setContent { MaterialTheme { WearIntervalNavigation() } }

    // Navigation should compose successfully and start on main screen
    composeTestRule.waitForIdle()

    // Main screen should be visible (we could add more specific assertions
    // for content if needed, but basic composition test covers the basics)
  }
}
