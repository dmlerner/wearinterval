package com.wearinterval.ui.component

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollablePicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val view = LocalView.current

    // Calculate the visible center index based on scroll position
    val centerIndex by derivedStateOf {
        val layoutInfo = listState.layoutInfo
        if (layoutInfo.visibleItemsInfo.isEmpty()) {
            0
        } else {
            val center = layoutInfo.viewportEndOffset / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull {
                kotlin.math.abs((it.offset + it.size / 2) - center)
            }
            // Adjust for padding items (first item is padding, so subtract 1)
            val rawIndex = centerItem?.index ?: 1
            val adjustedIndex = rawIndex - 1
            val finalIndex = kotlin.math.max(0, kotlin.math.min(adjustedIndex, items.size - 1))
            finalIndex
        }
    }

    // Track if we're currently responding to a user scroll to prevent circular updates
    val isUserScrolling = remember { mutableStateOf(false) }
    // Track if we're programmatically scrolling due to external selectedIndex change
    val isProgrammaticScroll = remember { mutableStateOf(false) }
    // Track previous center index to only vibrate on actual changes
    val previousCenterIndex = remember { mutableStateOf(-1) }

    // Handle selection changes with haptic feedback
    LaunchedEffect(centerIndex) {
        if (!isProgrammaticScroll.value &&
            centerIndex != previousCenterIndex.value &&
            centerIndex >= 0 && centerIndex < items.size &&
            previousCenterIndex.value != -1
        ) { // Don't vibrate on initial load
            isUserScrolling.value = true
            onSelectionChanged(centerIndex)
            // Provide haptic feedback for option change
            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            // Reset after a delay to allow the external state change to propagate
            kotlinx.coroutines.delay(Constants.UI.SCROLL_PICKER_DEBOUNCE.inWholeMilliseconds)
            isUserScrolling.value = false
        }
        // Always update previous index
        previousCenterIndex.value = centerIndex
    }

    // Scroll to selected item when selectedIndex changes externally (but not from user scrolling)
    LaunchedEffect(selectedIndex) {
        if (!isUserScrolling.value && selectedIndex != centerIndex && selectedIndex >= 0 && selectedIndex < items.size) {
            isProgrammaticScroll.value = true
            listState.scrollToItem(selectedIndex)
            kotlinx.coroutines.delay(Constants.UI.SCROLL_PICKER_DEBOUNCE.inWholeMilliseconds)
            isProgrammaticScroll.value = false
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

        // Scrollable picker - use remaining height
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                verticalArrangement = Arrangement.spacedBy(
                    Constants.Dimensions.SCROLL_PICKER_ITEM_SPACING.dp,
                ), // More spacing for better selection visibility
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Add padding items at start and end for proper centering
                item {
                    Box(modifier = Modifier.height(Constants.Dimensions.SCROLL_PICKER_PADDING_HEIGHT.dp))
                }

                itemsIndexed(items) { index, item ->
                    val isSelected = index == centerIndex
                    Text(
                        text = item,
                        style = if (isSelected) {
                            MaterialTheme.typography.title3 // Larger, more prominent font
                        } else {
                            MaterialTheme.typography.body2
                        },
                        color = if (isSelected) {
                            androidx.compose.ui.graphics.Color(0xFF2196F3) // Bright blue for selected
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.4f) // More dimmed non-selected
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Constants.Dimensions.SCROLL_PICKER_ITEM_VERTICAL_PADDING.dp), // Slightly more padding
                    )
                }

                item {
                    Box(modifier = Modifier.height(Constants.Dimensions.SCROLL_PICKER_PADDING_HEIGHT.dp))
                }
            }
        }
    }
}
