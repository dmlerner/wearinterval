package com.wearinterval.ui.screen.main

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Constants for MainScreen UI
private object MainScreenDefaults {
    val FLASH_DURATION_MS = Constants.UI.FLASH_DURATION.inWholeMilliseconds
    val COMPONENT_SPACING = Constants.Dimensions.COMPONENT_SPACING.dp
    val CONTROL_BUTTON_SPACING = Constants.Dimensions.CONTROL_BUTTON_SPACING.dp
    val CONTROL_BUTTONS_SPACING = Constants.Dimensions.CONTROL_BUTTON_SPACING.dp // Match wearinterval button spacing
    val PLAY_BUTTON_SIZE = Constants.Dimensions.MAIN_PLAY_BUTTON_SIZE.dp // Match wearinterval button size
    val STOP_BUTTON_SIZE = Constants.Dimensions.MAIN_STOP_BUTTON_SIZE.dp // Match wearinterval button size
    val ALARM_SPACING = Constants.Dimensions.CONTROL_BUTTON_SPACING.dp
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
    val flashModifier = if (uiState.flashScreen) {
        Modifier.background(Color.White)
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .then(flashModifier),
        contentAlignment = Alignment.Center,
    ) {
        if (uiState.isAlarmActive) {
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
    val outerRingColor = MaterialTheme.colors.primary
    val innerRingColor = if (uiState.isResting) {
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
                // Main time display - show configured values when stopped, actual timer when running
                Text(
                    text = when {
                        uiState.isStopped -> TimeUtils.formatDuration(uiState.configuration.workDuration)
                        else -> TimeUtils.formatDuration(uiState.timeRemaining)
                    },
                    style = MaterialTheme.typography.title1, // Smaller to fit with controls
                    color = if (uiState.isResting) {
                        MaterialTheme.colors.secondary
                    } else {
                        MaterialTheme.colors.onSurface
                    },
                    textAlign = TextAlign.Center,
                )

                // Lap indicator - show configured laps when stopped, current progress when running
                Text(
                    text = when {
                        uiState.isStopped -> {
                            if (uiState.configuration.laps == Constants.TimerLimits.INFINITE_LAPS) {
                                "∞ laps"
                            } else {
                                "${uiState.configuration.laps} laps"
                            }
                        }
                        uiState.totalLaps == Constants.TimerLimits.INFINITE_LAPS -> "Lap ${uiState.currentLap}"
                        else -> "${uiState.currentLap}/${uiState.totalLaps}"
                    },
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                // Phase indicator (show rest duration when stopped, "REST" when resting)
                when {
                    uiState.isStopped && uiState.configuration.restDuration.inWholeSeconds > 0 -> {
                        Text(
                            text = "Rest: ${TimeUtils.formatDuration(uiState.configuration.restDuration)}",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    uiState.isResting -> {
                        Text(
                            text = "REST",
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.secondary,
                            textAlign = TextAlign.Center,
                        )
                    }
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
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onEvent(MainEvent.StopClicked)
            },
            modifier = Modifier
                .size(MainScreenDefaults.STOP_BUTTON_SIZE)
                .semantics { contentDescription = "Stop" },
            enabled = uiState.isStopButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = Color.Red,
            ),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_stop),
                contentDescription = "Stop",
                tint = Color.White,
            )
        }

        // Play/Pause button with Material icons (matching wearinterval style)
        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onEvent(MainEvent.PlayPauseClicked)
            },
            modifier = Modifier
                .size(MainScreenDefaults.PLAY_BUTTON_SIZE)
                .semantics {
                    contentDescription = when {
                        uiState.isStopped -> "Play"
                        uiState.isPaused -> "Resume"
                        else -> "Pause"
                    }
                },
            enabled = uiState.isPlayButtonEnabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = when {
                    !uiState.isRunning -> Color.Green
                    uiState.isPaused -> Color.Green
                    else -> Color.Yellow
                },
            ),
        ) {
            Icon(
                painter = painterResource(
                    id = if (uiState.isRunning && !uiState.isPaused) {
                        R.drawable.ic_pause
                    } else {
                        R.drawable.ic_play_arrow
                    },
                ),
                contentDescription = when {
                    uiState.isStopped -> "Play"
                    uiState.isPaused -> "Resume"
                    else -> "Pause"
                },
                tint = Color.Black,
            )
        }
    }
}

@Composable
private fun AlarmScreen(uiState: MainUiState, onDismiss: () -> Unit) {
    val view = LocalView.current

    // Full-screen tap target for alarm dismissal
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            uiState = MainUiState(
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
