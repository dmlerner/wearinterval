package com.wearinterval.ui.screen.main

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.R
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.ui.component.DualProgressRings
import com.wearinterval.util.Constants
import com.wearinterval.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

// Constants for MainScreen UI
private object MainScreenDefaults {
  val FLASH_DURATION_MS = Constants.UI.FLASH_DURATION.inWholeMilliseconds
  val COMPONENT_SPACING = Constants.Dimensions.COMPONENT_SPACING.dp
  val CONTROL_BUTTON_SPACING = Constants.Dimensions.CONTROL_BUTTON_SPACING.dp
  val CONTROL_BUTTONS_SPACING =
    Constants.Dimensions.CONTROL_BUTTON_SPACING.dp // Match wearinterval button spacing
  val PLAY_BUTTON_SIZE =
    Constants.Dimensions.MAIN_PLAY_BUTTON_SIZE.dp // Match wearinterval button size
  val STOP_BUTTON_SIZE =
    Constants.Dimensions.MAIN_STOP_BUTTON_SIZE.dp // Match wearinterval button size
  val ALARM_SPACING = Constants.Dimensions.CONTROL_BUTTON_SPACING.dp
}

/**
 * Calculates the total remaining time for the entire workout. This includes the current interval
 * time remaining plus all remaining intervals in remaining laps.
 */
private fun calculateTotalConfiguredDuration(uiState: MainUiState): Duration {
  return uiState.configuration.workDuration * uiState.configuration.laps +
    if (uiState.configuration.restDuration > Duration.ZERO) {
      uiState.configuration.restDuration * uiState.configuration.laps
    } else {
      Duration.ZERO
    }
}

private fun calculateTotalRemainingTime(uiState: MainUiState): Duration {
  val currentIntervalRemaining = uiState.timeRemaining

  // Calculate remaining intervals in current lap
  val remainingIntervalsInCurrentLap =
    if (uiState.isResting) {
      // If resting, we still need to complete the work interval
      1
    } else {
      // If working, check if there's a rest interval after
      if (uiState.configuration.restDuration > Duration.ZERO) 1 else 0
    }

  // Calculate total intervals per lap (work + rest if rest duration > 0)
  val intervalsPerLap = if (uiState.configuration.restDuration > Duration.ZERO) 2 else 1

  // Calculate remaining complete laps after current lap
  val remainingCompleteLaps = maxOf(0, uiState.totalLaps - uiState.currentLap)

  // Calculate time for remaining intervals in current lap
  val remainingCurrentLapTime =
    if (remainingIntervalsInCurrentLap > 0) {
      if (uiState.isResting) {
        // Currently resting, need to add work duration for next interval
        uiState.configuration.workDuration
      } else {
        // Currently working, need to add rest duration if it exists
        uiState.configuration.restDuration
      }
    } else {
      Duration.ZERO
    }

  // Calculate time for complete remaining laps
  val timePerLap =
    uiState.configuration.workDuration +
      if (uiState.configuration.restDuration > Duration.ZERO) uiState.configuration.restDuration
      else Duration.ZERO
  val remainingCompleteLapsTime = timePerLap * remainingCompleteLaps

  return currentIntervalRemaining + remainingCurrentLapTime + remainingCompleteLapsTime
}

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  MainContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
  )
}

@Composable
internal fun MainContent(uiState: MainUiState, onEvent: (MainEvent) -> Unit) {
  // Handle screen flash effect
  if (uiState.flashScreen) {
    LaunchedEffect(uiState.flashScreen) {
      delay(MainScreenDefaults.FLASH_DURATION_MS) // Flash for 500ms as per design spec
      onEvent(MainEvent.FlashScreenDismissed)
    }
  }

  // Full screen flash overlay
  val flashModifier =
    if (uiState.flashScreen) {
      Modifier.background(Color.White)
    } else {
      Modifier
    }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(if (uiState.isLoading) Color.Black else MaterialTheme.colors.background)
        .then(flashModifier),
    contentAlignment = Alignment.Center,
  ) {
    if (uiState.isLoading) {
      androidx.wear.compose.material.CircularProgressIndicator()
    } else if (uiState.isAlarmActive) {
      // Full-screen alarm dismissal
      AlarmScreen(
        uiState = uiState,
        onDismiss = { onEvent(MainEvent.DismissAlarm) },
      )
    } else {
      // Normal timer interface
      TimerDisplay(
        uiState = uiState,
        onEvent = onEvent,
      )
    }
  }
}

@Composable
private fun TimerDisplay(uiState: MainUiState, onEvent: (MainEvent) -> Unit) {
  // Determine colors based on timer state
  val outerRingColor = Constants.Colors.PROGRESS_RING_OUTER_COLOR
  val innerRingColor =
    if (uiState.isResting) {
      MaterialTheme.colors.secondary // Yellow/amber for rest periods
    } else {
      MaterialTheme.colors.primaryVariant // Green for work periods
    }

  // Use fillMaxSize to extend rings to the edge of the watch face
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    DualProgressRings(
      outerProgress = uiState.overallProgressPercentage,
      innerProgress = uiState.intervalProgressPercentage,
      modifier = Modifier.fillMaxSize(),
      outerColor = outerRingColor,
      innerColor = innerRingColor,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        // Current time display
        var currentTime by remember { mutableStateOf("") }

        LaunchedEffect(Unit) {
          val formatter = SimpleDateFormat("h:mm", Locale.getDefault())
          while (true) {
            currentTime = formatter.format(Date())
            kotlinx.coroutines.delay(1000) // Update every second
          }
        }

        Text(
          text = currentTime,
          style = MaterialTheme.typography.caption2,
          color = MaterialTheme.colors.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Time display with lap duration perfectly centered
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          // Left side - lap count (show always unless infinite laps)
          Text(
            text =
              if (uiState.totalLaps == Constants.TimerLimits.INFINITE_LAPS) {
                ""
              } else {
                when {
                  uiState.isStopped -> {
                    // Show 1/total format when stopped (ready to start)
                    "1/${uiState.configuration.laps}"
                  }
                  else -> {
                    // Show current progress when running or paused
                    "${uiState.currentLap}/${uiState.totalLaps}"
                  }
                }
              },
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
          )

          if (uiState.totalLaps != Constants.TimerLimits.INFINITE_LAPS) {
            Spacer(modifier = Modifier.width(16.dp))
          }

          // Center - main time display (always centered)
          Text(
            text =
              when {
                uiState.isStopped -> TimeUtils.formatDuration(uiState.configuration.workDuration)
                else -> TimeUtils.formatDuration(uiState.timeRemaining)
              },
            style = MaterialTheme.typography.title1,
            color =
              if (uiState.isResting) {
                MaterialTheme.colors.secondary
              } else {
                MaterialTheme.colors.onSurface
              },
            textAlign = TextAlign.Center,
          )

          // Right side - total duration (show always unless infinite laps)
          if (uiState.totalLaps != Constants.TimerLimits.INFINITE_LAPS) {
            Spacer(modifier = Modifier.width(16.dp))
          }

          Text(
            text =
              if (uiState.totalLaps == Constants.TimerLimits.INFINITE_LAPS) {
                ""
              } else {
                when {
                  uiState.isStopped || uiState.isPaused -> {
                    // Show total configured duration when stopped/paused to prevent flicker
                    TimeUtils.formatDuration(calculateTotalConfiguredDuration(uiState))
                  }
                  else -> {
                    // Show remaining duration only when actively running
                    TimeUtils.formatDuration(calculateTotalRemainingTime(uiState))
                  }
                }
              },
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
          )
        }
        // Control buttons inside the circle
        TimerControlsInside(
          uiState = uiState,
          onEvent = onEvent,
        )
      }
    }
  }
}

@Composable
private fun TimerControlsInside(uiState: MainUiState, onEvent: (MainEvent) -> Unit) {
  val view = LocalView.current

  Row(
    horizontalArrangement = Arrangement.spacedBy(MainScreenDefaults.CONTROL_BUTTONS_SPACING),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // Stop button with Material icon (matching wearinterval style - stop button first)
    Button(
      onClick = {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onEvent(MainEvent.StopClicked)
      },
      modifier =
        Modifier.size(MainScreenDefaults.STOP_BUTTON_SIZE).semantics {
          contentDescription = "Stop"
        },
      enabled = uiState.isStopButtonEnabled,
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor = Constants.Colors.STOP_BUTTON_BACKGROUND,
        ),
    ) {
      Icon(
        painter = painterResource(id = R.drawable.ic_stop),
        contentDescription = "Stop",
        tint = Constants.Colors.STOP_BUTTON_ICON,
      )
    }

    // Play/Pause button with Material icons (matching wearinterval style)
    Button(
      onClick = {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onEvent(MainEvent.PlayPauseClicked)
      },
      modifier =
        Modifier.size(MainScreenDefaults.PLAY_BUTTON_SIZE).semantics {
          contentDescription =
            when {
              uiState.isStopped -> "Play"
              uiState.isPaused -> "Resume"
              else -> "Pause"
            }
        },
      enabled = uiState.isPlayButtonEnabled,
      colors =
        ButtonDefaults.buttonColors(
          backgroundColor =
            when {
              !uiState.isRunning -> Constants.Colors.PLAY_BUTTON_BACKGROUND
              uiState.isPaused -> Constants.Colors.PLAY_BUTTON_BACKGROUND
              else -> Color.Yellow
            },
        ),
    ) {
      Icon(
        painter =
          painterResource(
            id =
              if (uiState.isRunning && !uiState.isPaused) {
                R.drawable.ic_pause
              } else {
                R.drawable.ic_play_arrow
              },
          ),
        contentDescription =
          when {
            uiState.isStopped -> "Play"
            uiState.isPaused -> "Resume"
            else -> "Pause"
          },
        tint = Constants.Colors.PLAY_BUTTON_ICON,
      )
    }
  }
}

@Composable
private fun AlarmScreen(uiState: MainUiState, onDismiss: () -> Unit) {
  val view = LocalView.current

  // Full-screen tap target for alarm dismissal
  Box(
    modifier =
      Modifier.fillMaxSize()
        .clickable {
          view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
          onDismiss()
        }
        .semantics { contentDescription = "Tap to dismiss alarm" },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(MainScreenDefaults.ALARM_SPACING),
    ) {
      Text(
        text = "ALARM",
        style = MaterialTheme.typography.display2,
        color = MaterialTheme.colors.error,
      )
      Text(
        text = "Tap to dismiss",
        style = MaterialTheme.typography.body1,
        color = MaterialTheme.colors.onSurfaceVariant,
      )
    }
  }
}

@Preview
@Composable
private fun MainContentPreview() {
  MaterialTheme {
    MainContent(
      uiState =
        MainUiState(
          timerPhase = TimerPhase.Running,
          timeRemaining = 45.seconds,
          currentLap = 3,
          totalLaps = 20,
          configuration = TimerConfiguration.DEFAULT,
          isPlayButtonEnabled = true,
          isStopButtonEnabled = true,
        ),
      onEvent = {},
    )
  }
}

@Preview
@Composable
private fun AlarmScreenPreview() {
  MaterialTheme {
    AlarmScreen(
      uiState = MainUiState(timerPhase = TimerPhase.AlarmActive),
      onDismiss = {},
    )
  }
}
