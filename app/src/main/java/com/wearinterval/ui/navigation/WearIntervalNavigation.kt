package com.wearinterval.ui.navigation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wearinterval.ui.screen.config.ConfigScreen
import com.wearinterval.ui.screen.history.HistoryScreen
import com.wearinterval.ui.screen.main.MainScreen
import com.wearinterval.ui.screen.settings.SettingsScreen

/**
 * Main navigation for the WearInterval app using HorizontalPager.
 *
 * Navigation flow: Page 0: History (swipe left from main) Page 1: Main (center page - primary
 * screen) Page 2: Config (swipe right from main) Page 3: Settings (swipe right from config)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WearIntervalNavigation() {
  val pagerState =
    rememberPagerState(
      initialPage = 1, // Start on Main screen
      pageCount = { 4 }, // History, Main, Config, Settings
    )

  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
  ) { page ->
    when (page) {
      0 -> HistoryScreen()
      1 -> MainScreen() // CENTER - primary screen
      2 -> ConfigScreen() // Right swipe from main
      3 -> SettingsScreen() // Far right
    }
  }
}
