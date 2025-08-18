package com.wearinterval.ui.component

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Picker
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberPickerState
import com.wearinterval.util.Constants

@Composable
fun ScrollablePicker(
  items: List<String>,
  selectedIndex: Int,
  onSelectionChanged: (Int) -> Unit,
  title: String,
  modifier: Modifier = Modifier,
) {
  val componentId = remember { "ScrollablePicker-${title.ifEmpty { "noTitle" }}" }

  Log.d(
    "ScrollablePicker",
    "[$componentId] COMPOSITION START - items.size=${items.size}, selectedIndex=$selectedIndex, title='$title'"
  )

  val view = LocalView.current

  // Stabilize items to prevent unnecessary recreations
  val stableItems =
    remember(items) {
      Log.d("ScrollablePicker", "[$componentId] ITEMS RECREATED - new size: ${items.size}")
      items
    }

  // Create picker state - don't sync with external selectedIndex at all
  val pickerState =
    rememberPickerState(
      initialNumberOfOptions = stableItems.size,
      initiallySelectedOption = 0 // Always start at 0, let user scroll
    )

  // Track if this is the first composition
  val isFirstComposition: MutableState<Boolean> = remember { mutableStateOf(true) }

  // Only set initial position on first composition
  if (isFirstComposition.value) {
    LaunchedEffect(Unit) {
      Log.d(
        "ScrollablePicker",
        "[$componentId] FIRST COMPOSITION - setting initial position to $selectedIndex"
      )
      if (selectedIndex in 0 until stableItems.size) {
        pickerState.scrollToOption(selectedIndex)
      }
      isFirstComposition.value = false
    }
  }

  // Log picker state details
  SideEffect {
    Log.d(
      "ScrollablePicker",
      "[$componentId] SIDE_EFFECT - pickerState.selectedOption=${pickerState.selectedOption}, external_selectedIndex=$selectedIndex"
    )
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Log.d("ScrollablePicker", "[$componentId] COLUMN COMPOSITION - about to render UI")

    // Title (only show if not empty)
    if (title.isNotEmpty()) {
      Log.d("ScrollablePicker", "[$componentId] RENDERING TITLE: '$title'")
      Text(
        text = title,
        style = MaterialTheme.typography.caption2,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Constants.Dimensions.SMALL_SPACING.dp),
      )
    }

    Log.d(
      "ScrollablePicker",
      "[$componentId] RENDERING PICKER - state.selectedOption=${pickerState.selectedOption}, items.size=${stableItems.size}"
    )

    // Wear OS Picker with optimized recomposition
    Picker(
      state = pickerState,
      contentDescription = "Select ${title.ifEmpty { "value" }}",
      modifier = Modifier.weight(1f).fillMaxWidth(),
      onSelected = {
        Log.d("ScrollablePicker", "[$componentId] PICKER onSelected - performing haptic feedback")
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
      },
      option = { optionIndex ->
        // Log every option render (might be verbose)
        if (optionIndex == pickerState.selectedOption) {
          Log.d(
            "ScrollablePicker",
            "[$componentId] OPTION RENDER - index=$optionIndex (SELECTED), text='${stableItems[optionIndex]}'"
          )
        }
        Text(
          text = stableItems[optionIndex],
          style = MaterialTheme.typography.body2,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    )

    Log.d("ScrollablePicker", "[$componentId] PICKER RENDERED")

    // Only call callback when user actually stops scrolling
    LaunchedEffect(pickerState.selectedOption) {
      if (!isFirstComposition.value) { // Don't fire on initial setup
        Log.d(
          "ScrollablePicker",
          "[$componentId] LAUNCHED_EFFECT(pickerState.selectedOption) - debouncing for ${pickerState.selectedOption}"
        )
        kotlinx.coroutines.delay(200) // Longer debounce
        Log.d(
          "ScrollablePicker",
          "[$componentId] LAUNCHED_EFFECT(pickerState.selectedOption) - calling callback with ${pickerState.selectedOption}"
        )
        onSelectionChanged(pickerState.selectedOption)
      }
    }
  }

  Log.d("ScrollablePicker", "[$componentId] COMPOSITION END")
}
