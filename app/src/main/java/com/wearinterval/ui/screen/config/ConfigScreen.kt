package com.wearinterval.ui.screen.config

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.MaterialTheme
import com.wearinterval.ui.component.ScrollablePicker
import com.wearinterval.util.Constants
import com.wearinterval.util.DebugLogger
import kotlin.time.Duration.Companion.seconds

@Composable
fun ConfigScreen(viewModel: ConfigViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ConfigContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
  )
}

@Composable
internal fun ConfigContent(uiState: ConfigUiState, onEvent: (ConfigEvent) -> Unit) {
  DebugLogger.logConfigScreen(
    "ConfigScreen",
    "ConfigContent COMPOSITION START - laps=${uiState.laps}, work=${uiState.workMinutes}:${uiState.workSeconds}, rest=${uiState.restMinutes}:${uiState.restSeconds}, loading=${uiState.isLoading}"
  )

  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    contentAlignment = Alignment.Center,
  ) {
    if (uiState.isLoading) {
      DebugLogger.logConfigScreen("ConfigScreen", "ConfigContent - SHOWING LOADING")
      androidx.wear.compose.material.CircularProgressIndicator()
    } else {
      // Calculate current indices for each picker - use remember to prevent recalculation
      val lapsIndex =
        remember(uiState.laps) {
          val index = ConfigPickerValues.findLapsIndex(uiState.laps)
          DebugLogger.logConfigScreen(
            "ConfigScreen",
            "ConfigContent - LAPS INDEX CALCULATED: laps=${uiState.laps} -> index=$index"
          )
          index
        }
      val workDuration =
        remember(uiState.workMinutes, uiState.workSeconds) {
          val duration = (uiState.workMinutes * 60 + uiState.workSeconds).seconds
          DebugLogger.logConfigScreen(
            "ConfigScreen",
            "ConfigContent - WORK DURATION CALCULATED: ${uiState.workMinutes}:${uiState.workSeconds} -> $duration"
          )
          duration
        }
      val restDuration =
        remember(uiState.restMinutes, uiState.restSeconds) {
          val duration = (uiState.restMinutes * 60 + uiState.restSeconds).seconds
          DebugLogger.logConfigScreen(
            "ConfigScreen",
            "ConfigContent - REST DURATION CALCULATED: ${uiState.restMinutes}:${uiState.restSeconds} -> $duration"
          )
          duration
        }
      val workDurationIndex =
        remember(workDuration) {
          val index = ConfigPickerValues.findDurationIndex(workDuration, isRest = false)
          DebugLogger.logConfigScreen(
            "ConfigScreen",
            "ConfigContent - WORK INDEX CALCULATED: $workDuration -> index=$index"
          )
          index
        }
      val restDurationIndex =
        remember(restDuration) {
          val index = ConfigPickerValues.findDurationIndex(restDuration, isRest = true)
          DebugLogger.logConfigScreen(
            "ConfigScreen",
            "ConfigContent - REST INDEX CALCULATED: $restDuration -> index=$index"
          )
          index
        }

      // Create display lists - use remember to prevent recreation on recomposition
      val lapsDisplayItems = remember {
        DebugLogger.logConfigScreen("ConfigScreen", "ConfigContent - LAPS DISPLAY ITEMS RECREATED")
        ConfigPickerValues.LAPS_VALUES.map { ConfigPickerValues.lapsDisplayText(it) }
      }
      val durationDisplayItems = remember {
        DebugLogger.logConfigScreen(
          "ConfigScreen",
          "ConfigContent - DURATION DISPLAY ITEMS RECREATED"
        )
        ConfigPickerValues.DURATION_VALUES.map { ConfigPickerValues.durationDisplayText(it) }
      }
      val restDurationDisplayItems = remember {
        DebugLogger.logConfigScreen(
          "ConfigScreen",
          "ConfigContent - REST DURATION DISPLAY ITEMS RECREATED"
        )
        ConfigPickerValues.REST_DURATION_VALUES.map { ConfigPickerValues.durationDisplayText(it) }
      }

      // Track overall state in side effect
      SideEffect {
        DebugLogger.logConfigScreen(
          "ConfigScreen",
          "ConfigContent - SIDE_EFFECT: lapsIndex=$lapsIndex, workIndex=$workDurationIndex, restIndex=$restDurationIndex"
        )
        DebugLogger.logConfigScreen(
          "ConfigScreen",
          "ConfigContent - SIDE_EFFECT: items sizes - laps=${lapsDisplayItems.size}, work=${durationDisplayItems.size}, rest=${restDurationDisplayItems.size}"
        )
      }

      // Three-column picker layout using full screen height
      Row(
        modifier =
          Modifier.fillMaxSize()
            .padding(
              horizontal = Constants.Dimensions.SMALL_SPACING.dp,
              vertical = Constants.Dimensions.MEDIUM_SPACING.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(Constants.Dimensions.MEDIUM_SPACING.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Laps Picker
        DebugLogger.logConfigScreen(
          "ConfigScreen",
          "ConfigContent - RENDERING LAPS PICKER: selectedIndex=$lapsIndex, items.size=${lapsDisplayItems.size}"
        )
        ConfigScrollPicker(
          title = "",
          items = lapsDisplayItems,
          selectedIndex = lapsIndex,
          onSelectionChanged = { index ->
            DebugLogger.logConfigScreen(
              "ConfigScreen",
              "ConfigContent - LAPS PICKER onSelectionChanged: index=$index"
            )
            val selectedLaps = ConfigPickerValues.LAPS_VALUES[index]
            DebugLogger.logConfigScreen(
              "ConfigScreen",
              "ConfigContent - LAPS PICKER: selectedLaps=$selectedLaps, current uiState.laps=${uiState.laps}"
            )
            if (selectedLaps != uiState.laps) {
              DebugLogger.logConfigScreen(
                "ConfigScreen",
                "ConfigContent - LAPS PICKER: SENDING SetLaps event"
              )
              onEvent(ConfigEvent.SetLaps(selectedLaps))
            } else {
              DebugLogger.logConfigScreen(
                "ConfigScreen",
                "ConfigContent - LAPS PICKER: SKIPPING - same value"
              )
            }
          },
          onSingleTap = {
            DebugLogger.logConfigScreen("ConfigScreen", "ConfigContent - LAPS PICKER: onSingleTap")
            onEvent(ConfigEvent.ResetLaps)
          },
          onLongPress = {
            DebugLogger.logConfigScreen("ConfigScreen", "ConfigContent - LAPS PICKER: onLongPress")
            onEvent(ConfigEvent.SetLapsToInfinite)
          },
          modifier = Modifier.weight(1f),
        )

        // Work Duration Picker
        ConfigScrollPicker(
          title = "",
          items = durationDisplayItems,
          selectedIndex = workDurationIndex,
          onSelectionChanged = { index ->
            val selectedDuration = ConfigPickerValues.DURATION_VALUES[index]
            if (selectedDuration != workDuration) {
              onEvent(ConfigEvent.SetWorkDuration(selectedDuration))
            }
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
            if (selectedDuration != restDuration) {
              onEvent(ConfigEvent.SetRestDuration(selectedDuration))
            }
          },
          onSingleTap = { onEvent(ConfigEvent.ResetRest) },
          onLongPress = {
            // Debug: Long press rest to clear data
            onEvent(ConfigEvent.ClearAllData)
          },
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
  val view = LocalView.current

  // Use full height without background to prevent flashing
  Box(
    modifier =
      modifier.fillMaxHeight().padding(Constants.Dimensions.SCROLL_PICKER_CONTAINER_PADDING.dp),
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
      modifier =
        Modifier.fillMaxWidth()
          .height(32.dp)
          .align(Alignment.BottomCenter)
          .pointerInput(Unit) {
            detectTapGestures(
              onTap = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onSingleTap()
              },
              onLongPress = {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onLongPress()
              },
            )
          }
          .semantics { contentDescription = "Tap to reset $title, long press for alternate value" },
    )
  }
}

@Preview
@Composable
private fun ConfigContentPreview() {
  MaterialTheme {
    ConfigContent(
      uiState =
        ConfigUiState(
          laps = 10,
          workMinutes = 1,
          workSeconds = 30,
          restMinutes = 0,
          restSeconds = 15,
        ),
      onEvent = {},
    )
  }
}

@Preview
@Composable
private fun ConfigScrollPickerPreview() {
  MaterialTheme {
    Row(
      horizontalArrangement = Arrangement.spacedBy(Constants.Dimensions.MEDIUM_SPACING.dp),
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
