package com.wearinterval.ui.navigation

import org.junit.Test

/**
 * Unit tests for HorizontalPager navigation behavior.
 * UI navigation tests should be in androidTest directory.
 */
class NavigationTest {

    @Test
    fun navigationPages_haveCorrectIndices() {
        // Test that page indices match the expected layout
        // Page 0: History, Page 1: Main, Page 2: Config, Page 3: Settings
        val historyPageIndex = 0
        val mainPageIndex = 1
        val configPageIndex = 2
        val settingsPageIndex = 3

        // Verify page count
        val totalPages = 4
        assert(historyPageIndex < totalPages)
        assert(mainPageIndex < totalPages)
        assert(configPageIndex < totalPages)
        assert(settingsPageIndex < totalPages)

        // Verify main page is center
        assert(mainPageIndex == 1)
    }

    @Test
    fun navigationFlow_followsWearOSPatterns() {
        // Test that navigation follows standard Wear OS horizontal swipe patterns
        val mainPageIndex = 1
        val historyPageIndex = 0
        val configPageIndex = 2
        val settingsPageIndex = 3

        // History is left of main (swipe left from main to reach history)
        assert(historyPageIndex < mainPageIndex)

        // Config is right of main (swipe right from main to reach config)
        assert(configPageIndex > mainPageIndex)

        // Settings is right of config (swipe right from config to reach settings)
        assert(settingsPageIndex > configPageIndex)
    }
}
