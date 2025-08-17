package com.wearinterval.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.util.Constants
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  HistoryContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
  )
}

@Composable
internal fun HistoryContent(uiState: HistoryUiState, onEvent: (HistoryEvent) -> Unit) {
  Box(
    modifier =
      Modifier.fillMaxSize().background(MaterialTheme.colors.background).semantics {
        contentDescription = "Recent Timer Configurations"
      },
    contentAlignment = Alignment.Center,
  ) {
    when {
      uiState.isLoading -> {
        CircularProgressIndicator(
          modifier = Modifier.size(32.dp),
          strokeWidth = 3.dp,
        )
      }
      !uiState.hasConfigurations -> {
        EmptyHistoryContent()
      }
      else -> {
        ConfigurationGrid(
          configurations = uiState.configurations,
          onConfigurationSelect = { config -> onEvent(HistoryEvent.ConfigurationSelected(config)) },
        )
      }
    }
  }
}

@Composable
private fun ConfigurationGrid(
  configurations: List<TimerConfiguration>,
  onConfigurationSelect: (TimerConfiguration) -> Unit
) {
  val actualItems = configurations.size

  // Dynamic grid layout - always use 2 columns for better balance on watch
  val columns = 2

  // Only create as many rows as needed for actual items
  val rows = if (actualItems == 0) 0 else (actualItems + columns - 1) / columns // Ceiling division

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier.padding(4.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      repeat(rows) { rowIndex ->
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          repeat(columns) { colIndex ->
            val itemIndex = rowIndex * columns + colIndex
            if (itemIndex < actualItems) {
              GridConfigurationItem(
                configuration = configurations[itemIndex],
                onClick = onConfigurationSelect,
              )
            } else {
              // Add spacer to maintain grid alignment for incomplete rows
              Box(
                modifier = Modifier.width(62.dp).height(48.dp),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyHistoryContent() {
  Text(
    text = "No recent sets.",
    fontSize = 14.sp,
    color = Constants.Colors.DIVIDER_COLOR,
    textAlign = TextAlign.Center,
    modifier = Modifier.semantics { contentDescription = "No recent timer configurations" },
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
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = sampleConfigurations,
          isLoading = false,
        ),
      onEvent = {},
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
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = sampleConfigurations,
          isLoading = false,
        ),
      onEvent = {},
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
    )
  }
}

@Preview
@Composable
private fun EmptyHistoryContentPreview() {
  MaterialTheme {
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = emptyList(),
          isLoading = false,
        ),
      onEvent = {},
    )
  }
}

@Preview
@Composable
private fun LoadingHistoryContentPreview() {
  MaterialTheme {
    HistoryContent(
      uiState =
        HistoryUiState(
          configurations = emptyList(),
          isLoading = true,
        ),
      onEvent = {},
    )
  }
}
