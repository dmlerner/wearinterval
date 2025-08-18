package com.wearinterval.ui.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wearinterval.ui.screen.config.ConfigScreen
import com.wearinterval.ui.screen.history.HistoryScreen
import com.wearinterval.ui.screen.main.MainScreen
import com.wearinterval.ui.screen.settings.SettingsScreen
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Main navigation for the WearInterval app using custom gesture navigation.
 *
 * Performance optimized: Only composes the current screen instead of all screens. This eliminates
 * 75% composition overhead compared to HorizontalPager.
 *
 * Navigation flow: Page 0: History (swipe left from main) Page 1: Main (center page - primary
 * screen) Page 2: Config (swipe right from main) Page 3: Settings (swipe right from config)
 */
@Composable
fun WearIntervalNavigation() {
  var currentPage by remember { mutableIntStateOf(1) } // Start on Main screen
  var dragOffset by remember { mutableFloatStateOf(0f) }
  var isAnimating by remember { mutableStateOf(false) }
  val density = LocalDensity.current

  // Animation thresholds
  val swipeThreshold = with(density) { 80.dp.toPx() }
  val animationDurationMs = 250

  // Calculate current offset: either from drag or reset to 0
  val currentOffset = if (isAnimating) 0f else dragOffset * 0.3f

  Box(
    modifier =
      Modifier.fillMaxSize()
        .offset { IntOffset(currentOffset.roundToInt(), 0) }
        .pointerInput(Unit) {
          detectDragGestures(
            onDragStart = { isAnimating = false },
            onDragEnd = {
              val absDragOffset = abs(dragOffset)
              if (absDragOffset > swipeThreshold) {
                when {
                  // Swipe left (positive drag) - go to previous page
                  dragOffset > 0 && currentPage > 0 -> {
                    currentPage -= 1
                  }
                  // Swipe right (negative drag) - go to next page
                  dragOffset < 0 && currentPage < 3 -> {
                    currentPage += 1
                  }
                }
              }
              // Reset drag offset and trigger animation
              dragOffset = 0f
              isAnimating = true
            }
          ) { _, dragAmount ->
            if (!isAnimating) {
              dragOffset += dragAmount.x
            }
          }
        }
  ) {
    // Only compose the current screen - eliminates 75% overhead
    when (currentPage) {
      0 -> HistoryScreen()
      1 -> MainScreen() // CENTER - primary screen
      2 -> ConfigScreen() // Right swipe from main
      3 -> SettingsScreen() // Far right
    }
  }
}
