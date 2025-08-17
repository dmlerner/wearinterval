package com.wearinterval.ui.screen.settings

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
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

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
  )
}

@Composable
internal fun SettingsContent(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
  Box(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // First row: Vibration and Sound
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
      ) {
        SettingsToggleButton(
          text = "Vibration",
          isEnabled = uiState.vibrationEnabled,
          onClick = { onEvent(SettingsEvent.ToggleVibration) },
          contentDescription = "Toggle vibration",
        )

        SettingsToggleButton(
          text = "Sound",
          isEnabled = uiState.soundEnabled,
          onClick = { onEvent(SettingsEvent.ToggleSound) },
          contentDescription = "Toggle sound",
        )
      }

      // Second row: Auto Mode and Flash
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
      ) {
        SettingsToggleButton(
          text = "Auto",
          isEnabled = uiState.autoModeEnabled,
          onClick = { onEvent(SettingsEvent.ToggleAutoMode) },
          contentDescription = "Toggle auto mode",
        )

        SettingsToggleButton(
          text = "Flash",
          isEnabled = uiState.flashEnabled,
          onClick = { onEvent(SettingsEvent.ToggleFlash) },
          contentDescription = "Toggle screen flash",
        )
      }
    }
  }
}

@Composable
private fun SettingsToggleButton(
  text: String,
  isEnabled: Boolean,
  onClick: () -> Unit,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val view = LocalView.current

  Button(
    onClick = {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      onClick()
    },
    modifier = modifier.size(72.dp).semantics { this.contentDescription = contentDescription },
    colors =
      if (isEnabled) {
        ButtonDefaults.primaryButtonColors()
      } else {
        ButtonDefaults.secondaryButtonColors()
      },
  ) {
    Text(
      text = text,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.caption1,
      maxLines = 1,
    )
  }
}

@Preview
@Composable
private fun SettingsContentPreview() {
  MaterialTheme {
    SettingsContent(
      uiState =
        SettingsUiState(
          vibrationEnabled = true,
          soundEnabled = false,
          autoModeEnabled = true,
          flashEnabled = false,
        ),
      onEvent = {},
    )
  }
}

@Preview
@Composable
private fun SettingsToggleButtonPreview() {
  MaterialTheme {
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      SettingsToggleButton(
        text = "Vibration",
        isEnabled = true,
        onClick = {},
        contentDescription = "Toggle vibration",
      )
      SettingsToggleButton(
        text = "Sound",
        isEnabled = false,
        onClick = {},
        contentDescription = "Toggle sound",
      )
    }
  }
}
