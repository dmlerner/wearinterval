package com.wearinterval.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
  val view = LocalView.current

  // Create picker state with proper initialization
  val pickerState =
    rememberPickerState(
      initialNumberOfOptions = items.size,
      initiallySelectedOption = selectedIndex.coerceIn(0, items.size - 1)
    )

  // Track when selection changes and provide haptic feedback
  var lastSelection by remember { mutableStateOf(selectedIndex) }

  LaunchedEffect(pickerState.selectedOption) {
    if (pickerState.selectedOption != lastSelection) {
      onSelectionChanged(pickerState.selectedOption)
      view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
      lastSelection = pickerState.selectedOption
    }
  }

  // Handle external selection changes
  LaunchedEffect(selectedIndex) {
    val clampedIndex = selectedIndex.coerceIn(0, items.size - 1)
    if (clampedIndex != pickerState.selectedOption) {
      pickerState.animateScrollToOption(clampedIndex)
      lastSelection = clampedIndex
    }
  }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // Title (only show if not empty)
    if (title.isNotEmpty()) {
      Text(
        text = title,
        style = MaterialTheme.typography.caption2,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier.padding(bottom = Constants.Dimensions.SMALL_SPACING.dp),
      )
    }

    // Wear OS Picker with standard behavior
    Picker(
      state = pickerState,
      contentDescription = "Select ${title.ifEmpty { "value" }}",
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) { optionIndex ->
      Text(
        text = items[optionIndex],
        style = MaterialTheme.typography.body2,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}
