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

  // Stabilize items to prevent unnecessary recreations
  val stableItems = remember(items) { items }

  // Create picker state - sync with selectedIndex efficiently
  val pickerState =
    rememberPickerState(
      initialNumberOfOptions = stableItems.size,
      initiallySelectedOption = selectedIndex.coerceIn(0, stableItems.size - 1)
    )

  // Track if this is the first composition to avoid callback on init
  val isFirstComposition = remember { mutableStateOf(true) }

  // Sync picker state with external selectedIndex only when needed
  LaunchedEffect(selectedIndex) {
    if (selectedIndex != pickerState.selectedOption && selectedIndex in 0 until stableItems.size) {
      pickerState.scrollToOption(selectedIndex)
    }
    isFirstComposition.value = false
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

    // Wear OS Picker with optimized performance
    Picker(
      state = pickerState,
      contentDescription = "Select ${title.ifEmpty { "value" }}",
      modifier = Modifier.weight(1f).fillMaxWidth(),
      onSelected = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
      option = { optionIndex ->
        Text(
          text = stableItems[optionIndex],
          style = MaterialTheme.typography.body2,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    )

    // Optimized callback with reduced debounce
    LaunchedEffect(pickerState.selectedOption) {
      if (!isFirstComposition.value) {
        kotlinx.coroutines.delay(50) // Reduced from 100ms to 50ms
        onSelectionChanged(pickerState.selectedOption)
      }
    }
  }
}
