package com.wearinterval.ui.screen.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.ui.component.ConfigurationGridContent
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun HistoryScreen(
  onNavigateToMain: () -> Unit = {},
  viewModel: HistoryViewModel = hiltViewModel()
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  HistoryContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
    onNavigateToMain = onNavigateToMain,
  )
}

@Composable
internal fun HistoryContent(
  uiState: HistoryUiState,
  onEvent: (HistoryEvent) -> Unit,
  onNavigateToMain: () -> Unit = {}
) {
  ConfigurationGridContent(
    configurations = uiState.configurations,
    isLoading = uiState.isLoading,
    onConfigurationSelect = { config ->
      onEvent(HistoryEvent.ConfigurationSelected(config))
      onNavigateToMain()
    },
  )
}

@Preview
@Composable
private fun HistoryContentGridPreview() {
  val sampleConfigurations =
    listOf(
      TimerConfiguration(
        laps = 20,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
      ),
      TimerConfiguration(
        laps = 1,
        workDuration = 90.seconds,
        restDuration = 0.seconds,
      ),
      TimerConfiguration(
        laps = 5,
        workDuration = 2.minutes,
        restDuration = 0.seconds,
      ),
      TimerConfiguration(
        laps = 999,
        workDuration = 30.seconds,
        restDuration = 10.seconds,
      ),
    )

  MaterialTheme {
    ConfigurationGridContent(
      configurations = sampleConfigurations,
      isLoading = false,
      onConfigurationSelect = {},
    )
  }
}

@Preview
@Composable
private fun HistoryContentPartialGridPreview() {
  val sampleConfigurations =
    listOf(
      TimerConfiguration(
        laps = 20,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
      ),
      TimerConfiguration(
        laps = 1,
        workDuration = 90.seconds,
        restDuration = 0.seconds,
      ),
    )

  MaterialTheme {
    ConfigurationGridContent(
      configurations = sampleConfigurations,
      isLoading = false,
      onConfigurationSelect = {},
    )
  }
}

@Preview
@Composable
private fun HistoryContentSingleItemPreview() {
  val singleConfiguration =
    listOf(
      TimerConfiguration(
        laps = 8,
        workDuration = 30.seconds,
        restDuration = 10.seconds,
      ),
    )

  MaterialTheme {
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = singleConfiguration,
          isLoading = false,
        ),
      onEvent = {},
      onNavigateToMain = {},
    )
  }
}

@Preview
@Composable
private fun HistoryContentThreeItemsPreview() {
  val threeConfigurations =
    listOf(
      TimerConfiguration(
        laps = 20,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
      ),
      TimerConfiguration(
        laps = 1,
        workDuration = 90.seconds,
        restDuration = 0.seconds,
      ),
      TimerConfiguration(
        laps = 8,
        workDuration = 30.seconds,
        restDuration = 10.seconds,
      ),
    )

  MaterialTheme {
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = threeConfigurations,
          isLoading = false,
        ),
      onEvent = {},
      onNavigateToMain = {},
    )
  }
}

@Preview
@Composable
private fun EmptyHistoryContentPreview() {
  MaterialTheme {
    ConfigurationGridContent(
      configurations = emptyList(),
      isLoading = false,
      onConfigurationSelect = {},
    )
  }
}

@Preview
@Composable
private fun LoadingHistoryContentPreview() {
  MaterialTheme {
    ConfigurationGridContent(
      configurations = emptyList(),
      isLoading = true,
      onConfigurationSelect = {},
    )
  }
}
