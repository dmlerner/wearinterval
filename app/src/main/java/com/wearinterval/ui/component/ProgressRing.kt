package com.wearinterval.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = Color.Gray.copy(alpha = 0.3f),
    progressColor: Color = Color.Blue,
    startAngle: Float = -90f, // Start from top
    content: @Composable () -> Unit = {},
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { strokeWidth.toPx() }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Progress ring canvas
        Canvas(
            modifier = Modifier.size(size),
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
 * @param modifier Modifier for the component
 * @param size Overall size of the dual rings
 * @param outerColor Color of the outer progress ring
 * @param innerColor Color of the inner progress ring
 * @param content Content to display in the center
 */
@Composable
fun DualProgressRings(
    outerProgress: Float,
    innerProgress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    outerColor: Color = Color.Blue,
    innerColor: Color = Color.Green,
    content: @Composable () -> Unit = {},
) {
    val outerStrokeWidth = 6.dp
    val innerStrokeWidth = 4.dp
    val ringGap = 12.dp // Gap between the rings

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Outer progress ring (overall workout progress)
        ProgressRing(
            progress = outerProgress,
            size = size,
            strokeWidth = outerStrokeWidth,
            progressColor = outerColor,
            backgroundColor = outerColor.copy(alpha = 0.2f),
        )

        // Inner progress ring (current interval progress)
        ProgressRing(
            progress = innerProgress,
            size = size - (outerStrokeWidth * 2) - ringGap,
            strokeWidth = innerStrokeWidth,
            progressColor = innerColor,
            backgroundColor = innerColor.copy(alpha = 0.2f),
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
            outerColor = Color.Blue,
            innerColor = Color.Green,
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
