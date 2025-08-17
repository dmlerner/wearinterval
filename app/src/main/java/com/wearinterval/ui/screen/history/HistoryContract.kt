package com.wearinterval.ui.screen.history

import com.wearinterval.domain.model.TimerConfiguration

data class HistoryUiState(
  val configurations: List<TimerConfiguration> = emptyList(),
  val isLoading: Boolean = false,
) {
  val hasConfigurations: Boolean
    get() = configurations.isNotEmpty()
}

sealed class HistoryEvent {
  data class ConfigurationSelected(val configuration: TimerConfiguration) : HistoryEvent()

  object RefreshHistory : HistoryEvent()
}
