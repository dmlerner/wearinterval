package com.wearinterval.ui.screen.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Composable
fun ConfigScreen(onNavigateBack: () -> Unit, viewModel: ConfigViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ConfigContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
    )
}

@Composable
private fun ConfigContent(uiState: ConfigUiState, onEvent: (ConfigEvent) -> Unit, onNavigateBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Three-column picker layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Laps Column
                ConfigPickerColumn(
                    title = "Laps",
                    value = uiState.laps.toString(),
                    onIncrement = { onEvent(ConfigEvent.IncreaseLaps) },
                    onDecrement = { onEvent(ConfigEvent.DecreaseLaps) },
                    incrementDescription = "Increase laps",
                    decrementDescription = "Decrease laps",
                )

                // Work Duration Column
                ConfigPickerColumn(
                    title = "Work",
                    value = uiState.totalWorkTimeText,
                    onIncrement = { onEvent(ConfigEvent.IncreaseWorkDuration) },
                    onDecrement = { onEvent(ConfigEvent.DecreaseWorkDuration) },
                    incrementDescription = "Increase work duration",
                    decrementDescription = "Decrease work duration",
                )

                // Rest Duration Column
                ConfigPickerColumn(
                    title = "Rest",
                    value = uiState.totalRestTimeText,
                    onIncrement = { onEvent(ConfigEvent.IncreaseRestDuration) },
                    onDecrement = { onEvent(ConfigEvent.DecreaseRestDuration) },
                    incrementDescription = "Increase rest duration",
                    decrementDescription = "Decrease rest duration",
                )
            }

            // Reset button row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { onEvent(ConfigEvent.Reset) },
                    modifier = Modifier
                        .size(40.dp)
                        .semantics { contentDescription = "Reset to default" },
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    Text(
                        text = "↻",
                        style = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigPickerColumn(
    title: String,
    value: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    incrementDescription: String,
    decrementDescription: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Increment button (+ button)
        Button(
            onClick = onIncrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .semantics { contentDescription = incrementDescription },
            colors = ButtonDefaults.primaryButtonColors(),
        ) {
            Text(
                text = "+",
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
            )
        }

        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.caption2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurfaceVariant,
        )

        // Value display
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1,
        )

        // Decrement button (- button)
        Button(
            onClick = onDecrement,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .semantics { contentDescription = decrementDescription },
            colors = ButtonDefaults.secondaryButtonColors(),
        ) {
            Text(
                text = "−",
                style = MaterialTheme.typography.caption1,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun ConfigContentPreview() {
    MaterialTheme {
        ConfigContent(
            uiState = ConfigUiState(
                laps = 10,
                workMinutes = 1,
                workSeconds = 30,
                restMinutes = 0,
                restSeconds = 15,
            ),
            onEvent = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun ConfigPickerColumnPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConfigPickerColumn(
                title = "Laps",
                value = "5",
                onIncrement = {},
                onDecrement = {},
                incrementDescription = "Increase laps",
                decrementDescription = "Decrease laps",
            )
            ConfigPickerColumn(
                title = "Work",
                value = "1:30",
                onIncrement = {},
                onDecrement = {},
                incrementDescription = "Increase work",
                decrementDescription = "Decrease work",
            )
            ConfigPickerColumn(
                title = "Rest",
                value = "30s",
                onIncrement = {},
                onDecrement = {},
                incrementDescription = "Increase rest",
                decrementDescription = "Decrease rest",
            )
        }
    }
}
