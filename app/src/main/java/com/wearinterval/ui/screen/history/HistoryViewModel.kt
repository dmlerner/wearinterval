package com.wearinterval.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.usecase.SelectConfigurationUseCase
import com.wearinterval.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
  private val configurationRepository: ConfigurationRepository,
  private val selectConfigurationUseCase: SelectConfigurationUseCase,
) : ViewModel() {

  val uiState: StateFlow<HistoryUiState> =
    configurationRepository.recentConfigurations
      .map { configurations ->
        HistoryUiState(
          configurations = configurations.take(4), // Limit to 4 most recent for 2x2 grid
          isLoading = false,
        )
      }
      .catch { emit(HistoryUiState(configurations = emptyList(), isLoading = false)) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(Constants.UI.SUBSCRIPTION_TIMEOUT),
        initialValue = HistoryUiState(isLoading = true),
      )

  fun onEvent(event: HistoryEvent) {
    viewModelScope.launch {
      when (event) {
        is HistoryEvent.ConfigurationSelected -> {
          selectConfigurationUseCase.selectConfigurationAndStopTimer(event.configuration)
        }
        HistoryEvent.RefreshHistory -> {
          // The StateFlow automatically refreshes from the repository
          // This event is here for future manual refresh functionality
        }
      }
    }
  }
}
