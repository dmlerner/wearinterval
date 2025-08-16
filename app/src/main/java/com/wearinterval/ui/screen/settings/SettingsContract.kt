package com.wearinterval.ui.screen.settings

data class SettingsUiState(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val autoModeEnabled: Boolean = true,
    val flashEnabled: Boolean = true
)

sealed class SettingsEvent {
    object ToggleVibration : SettingsEvent()
    object ToggleSound : SettingsEvent()
    object ToggleAutoMode : SettingsEvent()
    object ToggleFlash : SettingsEvent()
}