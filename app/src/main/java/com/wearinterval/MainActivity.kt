package com.wearinterval

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.ui.navigation.WearIntervalNavigation
import com.wearinterval.ui.theme.WearIntervalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var configurationRepository: ConfigurationRepository
  @Inject lateinit var timerRepository: TimerRepository

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)

    // Handle tile configuration selection
    handleTileIntent()

    setContent { WearIntervalTheme { WearIntervalNavigation() } }
  }

  override fun onNewIntent(intent: android.content.Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleTileIntent()
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
            // Stop any running timer and select the new configuration
            timerRepository.stopTimer()
            configurationRepository.selectRecentConfiguration(selectedConfig)
          }
        } catch (e: Exception) {
          // Silently handle errors - app will still function normally
        }
      }
    }
  }
}
