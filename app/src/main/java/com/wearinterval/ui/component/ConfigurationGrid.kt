package com.wearinterval.ui.component

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.ui.screen.history.GridConfigurationItem
import com.wearinterval.util.Constants
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ConfigurationGridContent(
  configurations: List<TimerConfiguration>,
  isLoading: Boolean,
  onConfigurationSelect: (TimerConfiguration) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier =
      modifier.fillMaxSize().background(MaterialTheme.colors.background).semantics {
        contentDescription = "Recent Timer Configurations"
      },
    contentAlignment = Alignment.Center,
  ) {
    when {
      isLoading -> {
        CircularProgressIndicator(
          modifier = Modifier.size(32.dp),
          strokeWidth = 3.dp,
        )
      }
      configurations.isEmpty() -> {
        EmptyConfigurationsContent()
      }
      else -> {
        ConfigurationGrid(
          configurations = configurations,
          onConfigurationSelect = onConfigurationSelect,
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
  val columns = 2
  val rows = if (actualItems == 0) 0 else (actualItems + columns - 1) / columns

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
private fun EmptyConfigurationsContent() {
  Text(
    text = "No recent sets",
    fontSize = 14.sp,
    color = Constants.Colors.DIVIDER_COLOR,
    textAlign = TextAlign.Center,
    modifier = Modifier.semantics { contentDescription = "No recent timer configurations" },
  )
}

@Preview
@Composable
private fun ConfigurationGridContentPreview() {
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
private fun ConfigurationGridContentEmptyPreview() {
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
private fun ConfigurationGridContentLoadingPreview() {
  MaterialTheme {
    ConfigurationGridContent(
      configurations = emptyList(),
      isLoading = true,
      onConfigurationSelect = {},
    )
  }
}
