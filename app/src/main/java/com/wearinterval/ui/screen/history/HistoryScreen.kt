package com.wearinterval.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.util.TimeUtils
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
    val listState = rememberScalingLazyListState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                )
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error,
                    onRetry = { onEvent(HistoryEvent.Refresh) },
                )
            }
            !uiState.hasConfigurations -> {
                EmptyHistoryContent()
            }
            else -> {
                HistoryList(
                    configurations = uiState.recentConfigurations,
                    onConfigurationSelect = { config ->
                        onEvent(HistoryEvent.SelectConfiguration(config))
                    },
                    listState = listState,
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    configurations: List<TimerConfiguration>,
    onConfigurationSelect: (TimerConfiguration) -> Unit,
    listState: ScalingLazyListState,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 24.dp,
            bottom = 24.dp,
            start = 8.dp,
            end = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = listState,
    ) {
        items(configurations) { configuration ->
            ConfigurationItem(
                configuration = configuration,
                onClick = { onConfigurationSelect(configuration) },
            )
        }
    }
}

@Composable
private fun ConfigurationItem(configuration: TimerConfiguration, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .semantics {
                contentDescription = "Configuration: ${configuration.laps} laps, " +
                    "${TimeUtils.formatDuration(configuration.workDuration)} work, " +
                    "${TimeUtils.formatDuration(configuration.restDuration)} rest"
            },
        colors = ChipDefaults.chipColors(),
        label = {
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${configuration.laps} laps",
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = TimeUtils.formatDuration(configuration.workDuration),
                        style = MaterialTheme.typography.caption1,
                        maxLines = 1,
                    )

                    if (configuration.restDuration > 0.seconds) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.caption1,
                        )

                        Text(
                            text = TimeUtils.formatDuration(configuration.restDuration),
                            style = MaterialTheme.typography.caption1,
                            maxLines = 1,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun EmptyHistoryContent() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "No Recent\nConfigurations",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create and use\ntimer configurations\nto see them here",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 3,
        )
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.error,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .semantics { contentDescription = "Retry loading history" },
            colors = ButtonDefaults.secondaryButtonColors(),
        ) {
            Text(
                text = "↻",
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun HistoryContentPreview() {
    val sampleConfigurations = listOf(
        TimerConfiguration(
            id = "1",
            laps = 5,
            workDuration = 90.seconds,
            restDuration = 30.seconds,
        ),
        TimerConfiguration(
            id = "2",
            laps = 10,
            workDuration = 2.minutes,
            restDuration = 45.seconds,
        ),
        TimerConfiguration(
            id = "3",
            laps = 3,
            workDuration = 45.seconds,
            restDuration = 0.seconds,
        ),
    )

    MaterialTheme {
        HistoryContent(
            uiState = HistoryUiState(
                recentConfigurations = sampleConfigurations,
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
                recentConfigurations = emptyList(),
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
                recentConfigurations = emptyList(),
                isLoading = true,
            ),
            onEvent = {},
        )
    }
}

@Preview
@Composable
private fun ConfigurationItemPreview() {
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConfigurationItem(
                configuration = TimerConfiguration(
                    id = "1",
                    laps = 5,
                    workDuration = 90.seconds,
                    restDuration = 30.seconds,
                ),
                onClick = {},
            )

            ConfigurationItem(
                configuration = TimerConfiguration(
                    id = "2",
                    laps = 1,
                    workDuration = 45.seconds,
                    restDuration = 0.seconds,
                ),
                onClick = {},
            )
        }
    }
}
