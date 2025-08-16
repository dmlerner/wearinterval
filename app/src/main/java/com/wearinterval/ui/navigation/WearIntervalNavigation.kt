package com.wearinterval.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.wearinterval.ui.screen.config.ConfigScreen
import com.wearinterval.ui.screen.history.HistoryScreen
import com.wearinterval.ui.screen.main.MainScreen
import com.wearinterval.ui.screen.settings.SettingsScreen

/**
 * Main navigation destinations for the WearInterval app.
 *
 * Navigation flow per design:
 * - Main screen is the central hub
 * - Swipe left: History screen (recent configurations)
 * - Swipe right: Config screen (picker interface)
 * - Swipe up: Settings screen (notification preferences)
 */
object WearIntervalDestinations {
    const val MAIN = "main"
    const val CONFIG = "config"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@Composable
fun WearIntervalNavigation(navController: NavHostController = rememberSwipeDismissableNavController()) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = WearIntervalDestinations.MAIN,
    ) {
        composable(WearIntervalDestinations.MAIN) {
            MainScreen(
                onNavigateToConfig = {
                    navController.navigate(WearIntervalDestinations.CONFIG)
                },
                onNavigateToHistory = {
                    navController.navigate(WearIntervalDestinations.HISTORY)
                },
                onNavigateToSettings = {
                    navController.navigate(WearIntervalDestinations.SETTINGS)
                },
            )
        }

        composable(WearIntervalDestinations.CONFIG) {
            ConfigScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(WearIntervalDestinations.HISTORY) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(WearIntervalDestinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}
