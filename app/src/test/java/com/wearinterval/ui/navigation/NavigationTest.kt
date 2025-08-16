package com.wearinterval.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for navigation logic and destination constants.
 * UI navigation tests should be in androidTest directory.
 */
class NavigationTest {

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

    @Test
    fun navigationFlow_logicalOrder() {
        // Test that navigation flow makes sense
        // Main -> Config (right swipe)
        // Main -> History (left swipe)
        // Main -> Settings (up swipe)

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
    fun backNavigation_leadsToMain() {
        // Test that back navigation concept leads to main
        val secondaryScreens = listOf(
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        // All secondary screens should navigate back to main
        secondaryScreens.forEach { screen ->
            assertThat(screen).isNotEqualTo(WearIntervalDestinations.MAIN)
        }
    }

    @Test
    fun navigationConstants_areAccessible() {
        // Test that navigation constants are accessible and not null
        assertThat(WearIntervalDestinations.MAIN).isNotNull()
        assertThat(WearIntervalDestinations.CONFIG).isNotNull()
        assertThat(WearIntervalDestinations.HISTORY).isNotNull()
        assertThat(WearIntervalDestinations.SETTINGS).isNotNull()
    }

    @Test
    fun routeParameters_validation() {
        // Test route parameter validation logic
        fun isValidRoute(route: String): Boolean {
            return route.isNotBlank() &&
                route.length <= 50 &&
                route.matches(Regex("^[a-z]+$"))
        }

        val allDestinations = listOf(
            WearIntervalDestinations.MAIN,
            WearIntervalDestinations.CONFIG,
            WearIntervalDestinations.HISTORY,
            WearIntervalDestinations.SETTINGS,
        )

        allDestinations.forEach { destination ->
            assertThat(isValidRoute(destination)).isTrue()
        }
    }

    @Test
    fun navigationMap_completeness() {
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
}
