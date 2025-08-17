package com.wearinterval.ui.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.ui.component.DualProgressRings
import com.wearinterval.util.TimeUtils
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

// Constants for MainScreen UI
private object MainScreenDefaults {
    const val FLASH_DURATION_MS = 500L
    val TIMER_DISPLAY_SIZE = 140.dp
    val COMPONENT_SPACING = 12.dp
    val CONTROL_BUTTON_SPACING = 16.dp
    val CONTROL_BUTTONS_SPACING = 4.dp
    val PLAY_BUTTON_SIZE = 56.dp
    val STOP_BUTTON_SIZE = 48.dp
    val ALARM_SPACING = 16.dp
}

@Composable
fun MainScreen(
    onNavigateToConfig: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MainContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToConfig = onNavigateToConfig,
        onNavigateToHistory = onNavigateToHistory,
        onNavigateToSettings = onNavigateToSettings,
    )
}

@Composable
internal fun MainContent(
    uiState: MainUiState,
    onEvent: (MainEvent) -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
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
            TimerInterface(
                uiState = uiState,
                onEvent = onEvent,
                onNavigateToConfig = onNavigateToConfig,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun TimerInterface(
    uiState: MainUiState,
    onEvent: (MainEvent) -> Unit,
    onNavigateToConfig: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MainScreenDefaults.COMPONENT_SPACING),
    ) {
        // Timer display with dual progress rings
        TimerDisplay(uiState = uiState)

        // Control buttons
        TimerControls(
            uiState = uiState,
            onEvent = onEvent,
        )

        // Navigation hint
        Text(
            text = "← History • Config → • Settings ↑",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TimerDisplay(uiState: MainUiState) {
    // Determine colors based on timer state
    val outerRingColor = MaterialTheme.colors.primary
    val innerRingColor = if (uiState.isResting) {
        MaterialTheme.colors.secondary // Yellow/amber for rest periods
    } else {
        MaterialTheme.colors.primaryVariant // Green for work periods
    }

    DualProgressRings(
        outerProgress = uiState.overallProgressPercentage,
        innerProgress = uiState.intervalProgressPercentage,
        size = MainScreenDefaults.TIMER_DISPLAY_SIZE,
        outerColor = outerRingColor,
        innerColor = innerRingColor,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MainScreenDefaults.CONTROL_BUTTONS_SPACING),
        ) {
            // Main time display
            Text(
                text = TimeUtils.formatDuration(uiState.timeRemaining),
                style = MaterialTheme.typography.display2,
                color = if (uiState.isResting) {
                    MaterialTheme.colors.secondary
                } else {
                    MaterialTheme.colors.onSurface
                },
                textAlign = TextAlign.Center,
            )

            // Lap indicator
            Text(
                text = if (uiState.totalLaps == 999) {
                    "Lap ${uiState.currentLap}"
                } else {
                    "${uiState.currentLap}/${uiState.totalLaps}"
                },
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            // Phase indicator (only show during rest)
            if (uiState.isResting) {
                Text(
                    text = "REST",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.secondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TimerControls(uiState: MainUiState, onEvent: (MainEvent) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MainScreenDefaults.CONTROL_BUTTON_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play/Pause button
        Button(
            onClick = { onEvent(MainEvent.PlayPauseClicked) },
            modifier = Modifier
                .size(MainScreenDefaults.PLAY_BUTTON_SIZE)
                .clip(CircleShape)
                .semantics {
                    contentDescription = when {
                        uiState.isStopped -> "Play"
                        uiState.isPaused -> "Resume"
                        else -> "Pause"
                    }
                },
            enabled = uiState.isPlayButtonEnabled,
            colors = ButtonDefaults.primaryButtonColors(),
        ) {
            Text(
                text = if (uiState.isRunning || uiState.isResting) {
                    "⏸"
                } else {
                    "▶"
                },
                style = MaterialTheme.typography.title2,
            )
        }

        // Stop button
        Button(
            onClick = { onEvent(MainEvent.StopClicked) },
            modifier = Modifier
                .size(MainScreenDefaults.STOP_BUTTON_SIZE)
                .clip(CircleShape)
                .semantics { contentDescription = "Stop" },
            enabled = uiState.isStopButtonEnabled,
            colors = ButtonDefaults.secondaryButtonColors(),
        ) {
            Text(
                text = "⏹",
                style = MaterialTheme.typography.title2,
            )
        }
    }
}

@Composable
private fun AlarmScreen(uiState: MainUiState, onDismiss: () -> Unit) {
    // Full-screen tap target for alarm dismissal
    Box(
        modifier = Modifier
            .fillMaxSize()
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
            onNavigateToConfig = {},
            onNavigateToHistory = {},
            onNavigateToSettings = {},
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
