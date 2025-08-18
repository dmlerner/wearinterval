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

  // Create self-managed picker state
  val pickerState =
    rememberPickerState(
      initialNumberOfOptions = stableItems.size,
      initiallySelectedOption = 0 // Always start at 0
    )

  // One-time initialization only
  val isFirstComposition = remember { mutableStateOf(true) }
  if (isFirstComposition.value) {
    LaunchedEffect(Unit) {
      if (selectedIndex in 0 until stableItems.size) {
        pickerState.scrollToOption(selectedIndex)
      }
      isFirstComposition.value = false
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

    // Wear OS Picker with optimized performance
    Picker(
      state = pickerState,
      contentDescription = "Select ${title.ifEmpty { "value" }}",
      modifier = Modifier.weight(1f).fillMaxWidth(),
      onSelected = { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) },
      option = { optionIndex ->
        val isSelected = optionIndex == pickerState.selectedOption
        Text(
          text = stableItems[optionIndex],
          style = MaterialTheme.typography.body2,
          textAlign = TextAlign.Center,
          color = if (isSelected) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface,
          modifier = Modifier.fillMaxWidth()
        )
      }
    )

    // Debounced callback to prevent recomposition storms
    LaunchedEffect(pickerState.selectedOption) {
      if (!isFirstComposition.value) {
        // Add haptic feedback for selection change
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        kotlinx.coroutines.delay(200) // Critical: debounce rapid changes
        onSelectionChanged(pickerState.selectedOption)
      }
    }
  }
}
