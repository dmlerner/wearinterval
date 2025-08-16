package com.wearinterval.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.notificationSettings
        .map { settings ->
            SettingsUiState(
                vibrationEnabled = settings.vibrationEnabled,
                soundEnabled = settings.soundEnabled,
                autoModeEnabled = settings.autoMode,
                flashEnabled = settings.flashEnabled
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun onEvent(event: SettingsEvent) {
        viewModelScope.launch {
            val currentSettings = settingsRepository.notificationSettings.value
            val updatedSettings = when (event) {
                SettingsEvent.ToggleVibration -> currentSettings.copy(
                    vibrationEnabled = !currentSettings.vibrationEnabled
                )
                SettingsEvent.ToggleSound -> currentSettings.copy(
                    soundEnabled = !currentSettings.soundEnabled
                )
                SettingsEvent.ToggleAutoMode -> currentSettings.copy(
                    autoMode = !currentSettings.autoMode
                )
                SettingsEvent.ToggleFlash -> currentSettings.copy(
                    flashEnabled = !currentSettings.flashEnabled
                )
            }
            settingsRepository.updateSettings(updatedSettings)
        }
    }
}