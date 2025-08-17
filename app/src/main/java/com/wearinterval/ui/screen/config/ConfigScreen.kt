package com.wearinterval.ui.screen.config

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import com.wearinterval.ui.component.ScrollablePicker
import kotlin.time.Duration.Companion.seconds

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
internal fun ConfigContent(uiState: ConfigUiState, onEvent: (ConfigEvent) -> Unit, onNavigateBack: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current

    // Calculate current indices for each picker
    val lapsIndex = ConfigPickerValues.findLapsIndex(uiState.laps)
    val workDuration = (uiState.workMinutes * 60 + uiState.workSeconds).seconds
    val restDuration = (uiState.restMinutes * 60 + uiState.restSeconds).seconds
    val workDurationIndex = ConfigPickerValues.findDurationIndex(workDuration, isRest = false)
    val restDurationIndex = ConfigPickerValues.findDurationIndex(restDuration, isRest = true)

    // Create display lists
    val lapsDisplayItems = ConfigPickerValues.LAPS_VALUES.map { ConfigPickerValues.lapsDisplayText(it) }
    val durationDisplayItems = ConfigPickerValues.DURATION_VALUES.map { ConfigPickerValues.durationDisplayText(it) }
    val restDurationDisplayItems = ConfigPickerValues.REST_DURATION_VALUES.map { ConfigPickerValues.durationDisplayText(it) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Three-column picker layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top,
            ) {
                // Laps Picker
                ConfigScrollPicker(
                    title = "",
                    items = lapsDisplayItems,
                    selectedIndex = lapsIndex,
                    onSelectionChanged = { index ->
                        val selectedLaps = ConfigPickerValues.LAPS_VALUES[index]
                        onEvent(ConfigEvent.SetLaps(selectedLaps))
                    },
                    onSingleTap = { onEvent(ConfigEvent.ResetLaps) },
                    onLongPress = { onEvent(ConfigEvent.SetLapsToInfinite) },
                    modifier = Modifier.weight(1f),
                )

                // Work Duration Picker
                ConfigScrollPicker(
                    title = "",
                    items = durationDisplayItems,
                    selectedIndex = workDurationIndex,
                    onSelectionChanged = { index ->
                        val selectedDuration = ConfigPickerValues.DURATION_VALUES[index]
                        onEvent(ConfigEvent.SetWorkDuration(selectedDuration))
                    },
                    onSingleTap = { onEvent(ConfigEvent.ResetWork) },
                    onLongPress = { onEvent(ConfigEvent.SetWorkToLong) },
                    modifier = Modifier.weight(1f),
                )

                // Rest Duration Picker
                ConfigScrollPicker(
                    title = "",
                    items = restDurationDisplayItems,
                    selectedIndex = restDurationIndex,
                    onSelectionChanged = { index ->
                        val selectedDuration = ConfigPickerValues.REST_DURATION_VALUES[index]
                        onEvent(ConfigEvent.SetRestDuration(selectedDuration))
                    },
                    onSingleTap = { onEvent(ConfigEvent.ResetRest) },
                    onLongPress = { onEvent(ConfigEvent.SetRestToLong) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ConfigScrollPicker(
    title: String,
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    onSingleTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Use ScrollablePicker with gesture tap area
        Box(
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
        ) {
            ScrollablePicker(
                items = items,
                selectedIndex = selectedIndex,
                onSelectionChanged = onSelectionChanged,
                title = title,
                modifier = Modifier.fillMaxSize(),
            )

            // Invisible tap area at bottom for gestures
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .align(Alignment.BottomCenter)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSingleTap()
                            },
                            onLongPress = {
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                onLongPress()
                            },
                        )
                    }
                    .semantics {
                        contentDescription = "Tap to reset $title, long press for alternate value"
                    },
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
private fun ConfigScrollPickerPreview() {
    MaterialTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            ConfigScrollPicker(
                title = "Laps",
                items = listOf("1", "2", "3", "4", "5"),
                selectedIndex = 2,
                onSelectionChanged = {},
                onSingleTap = {},
                onLongPress = {},
                modifier = Modifier.weight(1f),
            )
            ConfigScrollPicker(
                title = "Work",
                items = listOf("30s", "45s", "1:00", "1:30", "2:00"),
                selectedIndex = 2,
                onSelectionChanged = {},
                onSingleTap = {},
                onLongPress = {},
                modifier = Modifier.weight(1f),
            )
            ConfigScrollPicker(
                title = "Rest",
                items = listOf("None", "15s", "30s", "45s", "1:00"),
                selectedIndex = 2,
                onSelectionChanged = {},
                onSingleTap = {},
                onLongPress = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}
