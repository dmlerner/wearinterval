package com.wearinterval.ui.component

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollablePicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    title: String,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Calculate the visible center index based on scroll position
    val visibleItemsInfo = listState.layoutInfo.visibleItemsInfo
    val centerIndex by remember {
        derivedStateOf {
            if (visibleItemsInfo.isEmpty()) {
                0
            } else {
                val center = listState.layoutInfo.viewportEndOffset / 2
                visibleItemsInfo.minByOrNull {
                    kotlin.math.abs((it.offset + it.size / 2) - center)
                }?.index ?: 0
            }
        }
    }

    // Handle selection changes with haptic feedback
    LaunchedEffect(centerIndex) {
        snapshotFlow { centerIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index != selectedIndex) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSelectionChanged(index)
                }
            }
    }

    // Scroll to selected item when selectedIndex changes externally
    LaunchedEffect(selectedIndex) {
        listState.animateScrollToItem(selectedIndex)
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            text = title,
            style = MaterialTheme.typography.caption2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // Scrollable picker
        Box(
            modifier = Modifier.height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = snapBehavior,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                // Add padding items at start and end for proper centering
                item {
                    Box(modifier = Modifier.height(40.dp))
                }

                itemsIndexed(items) { index, item ->
                    val isSelected = index == centerIndex
                    Text(
                        text = item,
                        style = if (isSelected) {
                            MaterialTheme.typography.body1
                        } else {
                            MaterialTheme.typography.body2
                        },
                        color = if (isSelected) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    )
                }

                item {
                    Box(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}
