package com.wearinterval.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

// Constants for component sizing and spacing
private object ProgressRingDefaults {
    val DEFAULT_SIZE = 120.dp
    val DEFAULT_STROKE_WIDTH = 8.dp
    val DUAL_RING_SIZE = 140.dp
    val DUAL_RING_OUTER_STROKE = 6.dp
    val DUAL_RING_INNER_STROKE = 4.dp
    val DUAL_RING_GAP = 12.dp
    const val BACKGROUND_ALPHA = 0.3f
    const val DUAL_RING_BACKGROUND_ALPHA = 0.2f
    const val START_ANGLE_TOP = -90f
}

/**
 * A circular progress ring component for WearInterval.
 *
 * @param progress Progress value between 0.0 and 1.0
 * @param modifier Modifier for the component
 * @param size Overall size of the progress ring
 * @param strokeWidth Width of the progress ring stroke
 * @param backgroundColor Color of the background track
 * @param progressColor Color of the progress indicator
 * @param startAngle Starting angle in degrees (0 = top, 90 = right, etc.)
 * @param content Optional content to display in the center of the ring
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = ProgressRingDefaults.DEFAULT_SIZE,
    strokeWidth: Dp = ProgressRingDefaults.DEFAULT_STROKE_WIDTH,
    backgroundColor: Color = Color.Gray.copy(alpha = ProgressRingDefaults.BACKGROUND_ALPHA),
    progressColor: Color = Color.Blue,
    startAngle: Float = ProgressRingDefaults.START_ANGLE_TOP, // Start from top
    content: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    // Determine the final modifier - use size if provided, otherwise use the passed modifier for dynamic sizing
    val finalModifier = if (modifier == Modifier) {
        Modifier.size(size)
    } else {
        modifier
    }

    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center,
    ) {
        // Progress ring canvas - use fillMaxSize for dynamic sizing
        Canvas(
            modifier = Modifier.fillMaxSize(),
        ) {
            val canvasSize = this.size.minDimension
            val radius = (canvasSize - strokeWidthPx) / 2f
            val center = this.center
            val topLeft = androidx.compose.ui.geometry.Offset(
                center.x - radius,
                center.y - radius,
            )
            val size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

            // Draw background track
            drawArc(
                color = backgroundColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = size,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            // Draw progress arc
            if (progress > 0f) {
                val sweepAngle = (progress.coerceIn(0f, 1f) * 360f)
                drawArc(
                    color = progressColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        // Center content
        content()
    }
}

/**
 * Dual concentric progress rings for the main timer display.
 * Outer ring shows overall workout progress, inner ring shows current interval progress.
 *
 * @param outerProgress Overall workout progress (0.0 to 1.0)
 * @param innerProgress Current interval progress (0.0 to 1.0)
 * @param modifier Modifier for the component (can include fillMaxSize for edge-to-edge display)
 * @param outerColor Color of the outer progress ring
 * @param innerColor Color of the inner progress ring
 * @param content Content to display in the center
 */
@Composable
fun DualProgressRings(
    outerProgress: Float,
    innerProgress: Float,
    modifier: Modifier = Modifier,
    outerColor: Color = Color(0xFF00C853), // Bright green for outer ring
    innerColor: Color = Color(0xFF2196F3), // Bright blue for inner ring
    content: @Composable () -> Unit = {},
) {
    val outerStrokeWidth = ProgressRingDefaults.DUAL_RING_OUTER_STROKE
    val innerStrokeWidth = ProgressRingDefaults.DUAL_RING_INNER_STROKE
    val ringGap = ProgressRingDefaults.DUAL_RING_GAP // Gap between the rings

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        // Outer progress ring (overall workout progress) - uses full available size
        ProgressRing(
            progress = outerProgress,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = outerStrokeWidth,
            progressColor = outerColor,
            backgroundColor = outerColor.copy(alpha = ProgressRingDefaults.DUAL_RING_BACKGROUND_ALPHA),
        )

        // Inner progress ring (current interval progress) - smaller to create the dual ring effect
        ProgressRing(
            progress = innerProgress,
            modifier = Modifier.fillMaxSize(0.9f), // 90% of parent size for inner ring (closer to outer)
            strokeWidth = innerStrokeWidth,
            progressColor = innerColor,
            backgroundColor = innerColor.copy(alpha = ProgressRingDefaults.DUAL_RING_BACKGROUND_ALPHA),
        )

        // Center content
        content()
    }
}

@Preview
@Composable
private fun ProgressRingPreview() {
    MaterialTheme {
        ProgressRing(
            progress = 0.75f,
            progressColor = Color.Blue,
        ) {
            Text("75%")
        }
    }
}

@Preview
@Composable
private fun DualProgressRingsPreview() {
    MaterialTheme {
        DualProgressRings(
            outerProgress = 0.6f, // 60% overall progress
            innerProgress = 0.8f, // 80% current interval progress
            outerColor = Color(0xFF00C853), // Bright green for outer ring
            innerColor = Color(0xFF2196F3), // Bright blue for inner ring
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "45s",
                    style = MaterialTheme.typography.display3,
                )
                Text(
                    text = "3/20",
                    style = MaterialTheme.typography.body2,
                )
            }
        }
    }
}
