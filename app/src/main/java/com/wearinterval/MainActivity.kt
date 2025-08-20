package com.wearinterval

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.domain.usecase.SelectConfigurationUseCase
import com.wearinterval.ui.navigation.WearIntervalNavigation
import com.wearinterval.ui.theme.WearIntervalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var configurationRepository: ConfigurationRepository
  @Inject lateinit var timerRepository: TimerRepository
  @Inject lateinit var selectConfigurationUseCase: SelectConfigurationUseCase

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    // Handle tile configuration selection
    handleTileIntent()

    // Observe timer state to manage screen on/off
    observeTimerStateForScreenManagement()

    setContent {
      WearIntervalTheme { WearIntervalNavigation(initialPage = getInitialNavigationPage()) }
    }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleTileIntent()
  }

  private fun getInitialNavigationPage(): Int {
    return when (intent?.getStringExtra("navigate_to")) {
      "main" -> 1 // Main screen
      else -> 1 // Default to main screen for tile navigation
    }
  }

  private fun handleTileIntent() {
    val configId = intent?.getStringExtra("config_id")
    if (configId != null) {
      lifecycleScope.launch {
        try {
          // Find the configuration and select it
          val configurations = configurationRepository.recentConfigurations.value
          val selectedConfig = configurations.find { it.id == configId }
          if (selectedConfig != null) {
            selectConfigurationUseCase.selectConfigurationAndStopTimer(selectedConfig)
          }
        } catch (e: Exception) {
          // Silently handle errors - app will still function normally
        }
      }
    }
  }

  private fun observeTimerStateForScreenManagement() {
    lifecycleScope.launch {
      timerRepository.timerState.collect { timerState ->
        if (timerState.isRunning && !timerState.isPaused) {
          window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
          window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
      }
    }
  }
}
