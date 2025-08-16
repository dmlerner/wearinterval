package com.wearinterval.domain.model

sealed class UiEvent {
    
    // Timer Control Events
    sealed class Timer : UiEvent() {
        object PlayPause : Timer()
        object Stop : Timer()
        object DismissAlarm : Timer()
    }
    
    // Configuration Events
    sealed class Configuration : UiEvent() {
        data class UpdateLaps(val laps: Int) : Configuration()
        data class UpdateWorkDuration(val duration: kotlin.time.Duration) : Configuration()
        data class UpdateRestDuration(val duration: kotlin.time.Duration) : Configuration()
        data class SelectConfiguration(val configuration: TimerConfiguration) : Configuration()
        object ResetToDefaults : Configuration()
    }
    
    // Settings Events
    sealed class Settings : UiEvent() {
        data class ToggleVibration(val enabled: Boolean) : Settings()
        data class ToggleSound(val enabled: Boolean) : Settings()
        data class ToggleFlash(val enabled: Boolean) : Settings()
        data class ToggleAutoMode(val enabled: Boolean) : Settings()
    }
    
    // Navigation Events
    sealed class Navigation : UiEvent() {
        object NavigateToConfig : Navigation()
        object NavigateToHistory : Navigation()
        object NavigateToSettings : Navigation()
        object NavigateBack : Navigation()
    }
    
    // History Events
    sealed class History : UiEvent() {
        data class SelectHistoryItem(val configuration: TimerConfiguration) : History()
        data class DeleteHistoryItem(val configurationId: String) : History()
        object ClearHistory : History()
    }
    
    // System Events
    sealed class System : UiEvent() {
        object RefreshData : System()
        object HandlePermissionResult : System()
        data class HandleError(val throwable: Throwable) : System()
    }
}