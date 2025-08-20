package com.wearinterval.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wearinterval.ui.screen.config.ConfigScreen
import com.wearinterval.ui.screen.history.HistoryScreen
import com.wearinterval.ui.screen.main.MainScreen
import com.wearinterval.ui.screen.settings.SettingsScreen
import kotlinx.coroutines.launch

/**
 * Main navigation for the WearInterval app using optimized HorizontalPager.
 *
 * Testing different approaches to eliminate the 75% composition overhead while maintaining smooth
 * Wear OS swipe gestures and proper animations.
 *
 * Navigation flow: Page 0: History (swipe left from main) Page 1: Main (center page - primary
 * screen) Page 2: Config (swipe right from main) Page 3: Settings (swipe right from config)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearIntervalNavigation(initialPage: Int = 1) {
  val pagerState =
    rememberPagerState(
      initialPage = initialPage, // Start on specified screen
      pageCount = { 4 }, // History, Main, Config, Settings
    )

  val scope = rememberCoroutineScope()

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    // Test different parameters for lazy loading
    beyondViewportPageCount = 0 // Try newer parameter name
  ) { page ->
    when (page) {
      0 -> HistoryScreen(onNavigateToMain = { scope.launch { pagerState.animateScrollToPage(1) } })
      1 -> MainScreen() // CENTER - primary screen
      2 -> ConfigScreen() // Right swipe from main
      3 -> SettingsScreen() // Far right
    }
  }
}
