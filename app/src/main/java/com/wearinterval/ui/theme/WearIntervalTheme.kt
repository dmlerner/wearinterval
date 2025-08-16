package com.wearinterval.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * WearInterval app theme using Wear OS Material Design.
 * Uses the default Wear OS dark theme optimized for watch displays.
 */
@Composable
fun WearIntervalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content,
    )
}
