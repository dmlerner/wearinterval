package com.wearinterval.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.util.Constants
import kotlin.math.abs

@Composable
fun ScrollablePicker(
  items: List<String>,
  selectedIndex: Int,
  onSelectionChanged: (Int) -> Unit,
  title: String,
  modifier: Modifier = Modifier,
) {
  val view = LocalView.current
  val listState =
    rememberLazyListState(
      initialFirstVisibleItemIndex = selectedIndex + 1 // +1 for padding item
    )

  // No-fling behavior - only scroll by direct touch, no momentum
  val noFlingBehavior = remember {
    object : FlingBehavior {
      override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // Return 0 to prevent any fling/momentum scrolling
        return 0f
      }
    }
  }

  // Calculate center index based on scroll position
  val centerIndex by derivedStateOf {
    val layoutInfo = listState.layoutInfo
    if (layoutInfo.visibleItemsInfo.isEmpty()) {
      0
    } else {
      val center = layoutInfo.viewportEndOffset / 2
      val centerItem =
        layoutInfo.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - center) }
      val rawIndex = centerItem?.index ?: 0
      val adjustedIndex = rawIndex - 1 // Account for padding
      kotlin.math.max(0, kotlin.math.min(adjustedIndex, items.size - 1))
    }
  }

  // Track previous selection
  val previousSelection = remember { mutableStateOf(selectedIndex) }

  // Handle center index changes
  LaunchedEffect(centerIndex) {
    if (centerIndex != previousSelection.value && centerIndex >= 0 && centerIndex < items.size) {
      onSelectionChanged(centerIndex)
      view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
      previousSelection.value = centerIndex
    }
  }

  // Handle external selection changes
  LaunchedEffect(selectedIndex) {
    if (selectedIndex != centerIndex && selectedIndex >= 0 && selectedIndex < items.size) {
      listState.animateScrollToItem(selectedIndex + 1) // +1 for padding
      previousSelection.value = selectedIndex
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

    // Controlled LazyColumn with no fling behavior
    LazyColumn(
      state = listState,
      flingBehavior = noFlingBehavior,
      verticalArrangement =
        Arrangement.spacedBy(Constants.Dimensions.SCROLL_PICKER_ITEM_SPACING.dp),
      modifier = Modifier.weight(1f).fillMaxWidth(),
    ) {
      // Top padding for centering
      item { Box(modifier = Modifier.height(Constants.Dimensions.SCROLL_PICKER_PADDING_HEIGHT.dp)) }

      itemsIndexed(items) { index, item ->
        val isSelected = index == centerIndex
        Text(
          text = item,
          style =
            if (isSelected) MaterialTheme.typography.title3 else MaterialTheme.typography.body2,
          color =
            if (isSelected) {
              Constants.Colors.SCROLLABLE_PICKER_SELECTED
            } else {
              MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            },
          textAlign = TextAlign.Center,
          modifier =
            Modifier.fillMaxWidth()
              .padding(vertical = Constants.Dimensions.SCROLL_PICKER_ITEM_VERTICAL_PADDING.dp)
        )
      }

      // Bottom padding for centering
      item { Box(modifier = Modifier.height(Constants.Dimensions.SCROLL_PICKER_PADDING_HEIGHT.dp)) }
    }
  }
}
