package com.wearinterval.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.wearinterval.util.Constants

// Constants for component sizing and spacing
private object ProgressRingDefaults {
  val DEFAULT_SIZE = Constants.Dimensions.PROGRESS_RING_DEFAULT_SIZE.dp
  val DEFAULT_STROKE_WIDTH = Constants.Dimensions.PROGRESS_RING_DEFAULT_STROKE.dp
  val DUAL_RING_SIZE = Constants.Dimensions.PROGRESS_RING_DUAL_SIZE.dp
  val DUAL_RING_OUTER_STROKE = Constants.Dimensions.PROGRESS_RING_OUTER_STROKE.dp
  val DUAL_RING_INNER_STROKE = Constants.Dimensions.PROGRESS_RING_INNER_STROKE.dp
  val DUAL_RING_GAP = Constants.Dimensions.PROGRESS_RING_GAP.dp
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
  backgroundColor: Color =
    Constants.Colors.PROGRESS_RING_DEFAULT_BACKGROUND.copy(
      alpha = ProgressRingDefaults.BACKGROUND_ALPHA
    ),
  progressColor: Color = Constants.Colors.PROGRESS_RING_DEFAULT_PROGRESS,
  startAngle: Float = ProgressRingDefaults.START_ANGLE_TOP, // Start from top
  content: @Composable () -> Unit = {},
) {
  val density = LocalDensity.current
  val strokeWidthPx = with(density) { strokeWidth.toPx() }

  // Animate progress changes for smoother transitions
  val animatedProgress by
    animateFloatAsState(
      targetValue = progress,
      animationSpec = tween(durationMillis = 50), // Fast but smooth animation
      label = "progressAnimation"
    )

  // Determine the final modifier - use size if provided, otherwise use the passed modifier for
  // dynamic sizing
  val finalModifier =
    if (modifier == Modifier) {
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
      val topLeft =
        androidx.compose.ui.geometry.Offset(
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
      if (animatedProgress > 0f) {
        val sweepAngle = (animatedProgress.coerceIn(0f, 1f) * 360f)
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
 * Dual concentric progress rings for the main timer display. Outer ring shows overall workout
 * progress, inner ring shows current interval progress.
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
  outerColor: Color = Constants.Colors.PROGRESS_RING_OUTER_COLOR,
  innerColor: Color = Constants.Colors.PROGRESS_RING_INNER_COLOR,
  content: @Composable () -> Unit = {},
) {
  val outerStrokeWidth = ProgressRingDefaults.DUAL_RING_OUTER_STROKE
  val innerStrokeWidth = ProgressRingDefaults.DUAL_RING_INNER_STROKE
  val ringGap = ProgressRingDefaults.DUAL_RING_GAP // Gap between the rings

  // Animate both progress values for smooth transitions
  val animatedOuterProgress by
    animateFloatAsState(
      targetValue = outerProgress,
      animationSpec = tween(durationMillis = 50), // Fast but smooth animation
      label = "outerProgressAnimation"
    )

  val animatedInnerProgress by
    animateFloatAsState(
      targetValue = innerProgress,
      animationSpec = tween(durationMillis = 50), // Fast but smooth animation
      label = "innerProgressAnimation"
    )

  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center,
  ) {
    // Outer progress ring (overall workout progress) - uses full available size
    ProgressRing(
      progress = animatedOuterProgress,
      modifier = Modifier.fillMaxSize(),
      strokeWidth = outerStrokeWidth,
      progressColor = outerColor,
      backgroundColor = outerColor.copy(alpha = ProgressRingDefaults.DUAL_RING_BACKGROUND_ALPHA),
    )

    // Inner progress ring (current interval progress) - smaller to create the dual ring effect
    ProgressRing(
      progress = animatedInnerProgress,
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
      progressColor = Constants.Colors.PROGRESS_RING_DEFAULT_PROGRESS,
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
      outerColor = Constants.Colors.PROGRESS_RING_OUTER_COLOR,
      innerColor = Constants.Colors.PROGRESS_RING_INNER_COLOR,
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
