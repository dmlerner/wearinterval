package com.wearinterval.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

/**
 * WearInterval app theme using Wear OS Material Design.
 * Uses a pure black background optimized for watch displays.
 */
@Composable
fun WearIntervalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = Colors(
            primary = Color(0xFF6699FF),
            primaryVariant = Color(0xFF4CAF50),
            secondary = Color(0xFFFFC107),
            secondaryVariant = Color(0xFFFF9800),
            background = Color(0xFF000000), // Pure black background
            surface = Color(0xFF000000), // Pure black surface
            error = Color(0xFFFF5722),
            onPrimary = Color.White,
            onSecondary = Color.Black,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFBBBBBB),
            onError = Color.White,
        ),
        content = content,
    )
}
