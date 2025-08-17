package com.wearinterval.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .semantics { contentDescription = "Recent Timer Configurations" },
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
                    onConfigurationSelect = { config ->
                        onEvent(HistoryEvent.ConfigurationSelected(config))
                    },
                )
            }
        }
    }
}

@Composable
private fun ConfigurationGrid(configurations: List<TimerConfiguration>, onConfigurationSelect: (TimerConfiguration) -> Unit) {
    // 2x2 grid with 4dp padding and 6dp spacing
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Top row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GridConfigurationItem(
                configuration = configurations.getOrNull(0),
                onClick = onConfigurationSelect,
            )
            GridConfigurationItem(
                configuration = configurations.getOrNull(1),
                onClick = onConfigurationSelect,
            )
        }
        // Bottom row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            GridConfigurationItem(
                configuration = configurations.getOrNull(2),
                onClick = onConfigurationSelect,
            )
            GridConfigurationItem(
                configuration = configurations.getOrNull(3),
                onClick = onConfigurationSelect,
            )
        }
    }
}

@Composable
private fun EmptyHistoryContent() {
    Text(
        text = "No recent sets.",
        fontSize = 14.sp,
        color = Color.Gray,
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics {
            contentDescription = "No recent timer configurations"
        },
    )
}

@Preview
@Composable
private fun HistoryContentGridPreview() {
    val sampleConfigurations = listOf(
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
            uiState = HistoryUiState(
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
    val sampleConfigurations = listOf(
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
            uiState = HistoryUiState(
                configurations = sampleConfigurations,
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
            uiState = HistoryUiState(
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
            uiState = HistoryUiState(
                configurations = emptyList(),
                isLoading = true,
            ),
            onEvent = {},
        )
    }
}
