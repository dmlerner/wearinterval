package com.wearinterval.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.util.Constants
import kotlin.time.Duration.Companion.seconds

@Composable
fun GridConfigurationItem(
  configuration: TimerConfiguration?,
  onClick: (TimerConfiguration) -> Unit,
  modifier: Modifier = Modifier
) {
  val hapticFeedback = LocalHapticFeedback.current

  Box(
    modifier =
      modifier
        .width(62.dp)
        .height(48.dp)
        .clip(RoundedCornerShape(8.dp))
        .then(
          if (configuration != null) {
            Modifier.background(Constants.Colors.HISTORY_ITEM_BACKGROUND)
              .clickable {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick(configuration)
              }
              .semantics {
                contentDescription = "Timer configuration: ${configuration.displayString()}"
              }
          } else {
            Modifier.background(
              Constants.Colors.HISTORY_ITEM_BACKGROUND.copy(alpha = 0.3f)
            ) // Semi-transparent for empty slots
          },
        ),
    contentAlignment = Alignment.Center,
  ) {
    if (configuration != null) {
      Text(
        text = configuration.displayString(),
        fontSize = 14.sp, // Increased from 12sp to 14sp
        color = Constants.Colors.HISTORY_ITEM_TEXT,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Preview
@Composable
private fun GridConfigurationItemPreview() {
  MaterialTheme {
    GridConfigurationItem(
      configuration =
        TimerConfiguration(
          laps = 20,
          workDuration = 45.seconds,
          restDuration = 15.seconds,
        ),
      onClick = {},
    )
  }
}

@Preview
@Composable
private fun GridConfigurationItemEmptyPreview() {
  MaterialTheme {
    GridConfigurationItem(
      configuration = null,
      onClick = {},
    )
  }
}
