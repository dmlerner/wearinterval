package com.wearinterval.ui.screen.history

import com.wearinterval.domain.model.TimerConfiguration

data class HistoryUiState(
    val recentConfigurations: List<TimerConfiguration> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val hasConfigurations: Boolean
        get() = recentConfigurations.isNotEmpty()
}

sealed class HistoryEvent {
    data class SelectConfiguration(val configuration: TimerConfiguration) : HistoryEvent()
    object ClearHistory : HistoryEvent()
    object Refresh : HistoryEvent()
}
