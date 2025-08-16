package com.wearinterval.ui.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.repository.ConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = configurationRepository.recentConfigurations
        .map { configurations ->
            HistoryUiState(
                recentConfigurations = configurations,
                isLoading = false,
                error = null
            )
        }
        .catch { error ->
            emit(
                HistoryUiState(
                    recentConfigurations = emptyList(),
                    isLoading = false,
                    error = error.message ?: "Unknown error occurred"
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )

    fun onEvent(event: HistoryEvent) {
        viewModelScope.launch {
            when (event) {
                is HistoryEvent.SelectConfiguration -> {
                    configurationRepository.selectRecentConfiguration(event.configuration)
                }
                HistoryEvent.ClearHistory -> {
                    // Note: We don't implement clear functionality yet as it's not in the basic spec
                    // This could be added later if needed
                }
                HistoryEvent.Refresh -> {
                    // The StateFlow automatically refreshes from the repository
                    // This event is here for future manual refresh functionality
                }
            }
        }
    }
}