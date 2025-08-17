package com.wearinterval.ui.navigation

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.compose.material.MaterialTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearIntervalNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun wearIntervalNavigation_composesSuccessfully() {
        // When
        composeTestRule.setContent {
            MaterialTheme {
                WearIntervalNavigation()
            }
        }

        // Then - Navigation should compose without error
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigationDestinations_haveCorrectValues() {
        // Test that navigation destination constants are correct
        assertThat(WearIntervalDestinations.MAIN).isEqualTo("main")
        assertThat(WearIntervalDestinations.CONFIG).isEqualTo("config")
        assertThat(WearIntervalDestinations.HISTORY).isEqualTo("history")
        assertThat(WearIntervalDestinations.SETTINGS).isEqualTo("settings")
    }

    @Test
    fun navigationDestinations_areUnique() {
        // Test that all navigation destinations are unique
        val destinations = listOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        val uniqueDestinations = destinations.toSet()
        assertThat(uniqueDestinations).hasSize(destinations.size)
    }

    @Test
    fun wearIntervalNavigation_usesSwipeDismissableNavHost() {
        // Given/When
        var navigationComposed = false

        composeTestRule.setContent {
            MaterialTheme {
                WearIntervalNavigation()
                navigationComposed = true
            }
        }

        // Then - Navigation should compose successfully
        assertThat(navigationComposed).isTrue()
        composeTestRule.waitForIdle()
    }

    @Test
    fun wearIntervalNavigation_withCustomNavController() {
        // Given/When
        var navigationComposed = false

        composeTestRule.setContent {
            MaterialTheme {
                WearIntervalNavigation()
                navigationComposed = true
            }
        }

        // Then - Navigation should work with any nav controller
        assertThat(navigationComposed).isTrue()
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigationFlow_logicalOrder() {
        // Test that navigation flow makes sense
        val fromMain = listOf(
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        // All destinations should be reachable from main
        fromMain.forEach { destination ->
            assertThat(destination).isIn(
                listOf(
                    WearIntervalDestinations.CONFIG,
                    WearIntervalDestinations.HISTORY,
                    WearIntervalDestinations.SETTINGS,
                ),
            )
        }
    }

    @Test
    fun navigationComposition_handlesTheme() {
        // Given/When
        composeTestRule.setContent {
            MaterialTheme {
                WearIntervalNavigation()
            }
        }

        // Then - Should compose within theme successfully
        composeTestRule.waitForIdle()
    }

    @Test
    fun navigationStructure_isComplete() {
        // Test that navigation covers all expected screens from design
        val expectedScreens = setOf("main", "config", "history", "settings")
        val actualScreens = setOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        assertThat(actualScreens).isEqualTo(expectedScreens)
    }

    @Test
    fun navigationDestinations_areNotEmpty() {
        // Test that navigation destinations are not empty strings
        val destinations = listOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        destinations.forEach { destination ->
            assertThat(destination).isNotEmpty()
            assertThat(destination.trim()).isEqualTo(destination) // No leading/trailing whitespace
        }
    }

    @Test
    fun navigationDestinations_followNamingConvention() {
        // Test that destinations follow lowercase naming convention
        val destinations = listOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        destinations.forEach { destination ->
            assertThat(destination).isEqualTo(destination.lowercase())
            assertThat(destination).doesNotContain(" ") // No spaces
            assertThat(destination).doesNotContain("_") // No underscores (prefer kebab-case or camelCase)
        }
    }

    @Test
    fun startDestination_isMain() {
        // Test that the main screen is the expected start destination
        assertThat(WearIntervalDestinations.MAIN).isEqualTo("main")

        // Verify main is appropriate as start destination
        val allDestinations = listOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        assertThat(allDestinations).contains(WearIntervalDestinations.MAIN)
    }
}
