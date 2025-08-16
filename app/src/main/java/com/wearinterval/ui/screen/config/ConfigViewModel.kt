package com.wearinterval.ui.screen.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository,
) : ViewModel() {

    val uiState: StateFlow<ConfigUiState> = configurationRepository.currentConfiguration
        .map { config ->
            ConfigUiState(
                laps = config.laps,
                workMinutes = config.workDuration.inWholeMinutes.toInt(),
                workSeconds = (config.workDuration.inWholeSeconds % 60).toInt(),
                restMinutes = config.restDuration.inWholeMinutes.toInt(),
                restSeconds = (config.restDuration.inWholeSeconds % 60).toInt(),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConfigUiState(),
        )

    fun onEvent(event: ConfigEvent) {
        viewModelScope.launch {
            val currentConfig = configurationRepository.currentConfiguration.value
            val updatedConfig = when (event) {
                ConfigEvent.IncreaseLaps -> {
                    currentConfig.copy(laps = currentConfig.laps + 1)
                }
                ConfigEvent.DecreaseLaps -> {
                    if (currentConfig.laps > 1) {
                        currentConfig.copy(laps = currentConfig.laps - 1)
                    } else {
                        return@launch // Don't update if already at minimum
                    }
                }
                ConfigEvent.IncreaseWorkDuration -> {
                    currentConfig.copy(
                        workDuration = currentConfig.workDuration + 5.seconds,
                    )
                }
                ConfigEvent.DecreaseWorkDuration -> {
                    val newDuration = currentConfig.workDuration - 5.seconds
                    if (newDuration >= 5.seconds) {
                        currentConfig.copy(workDuration = newDuration)
                    } else {
                        return@launch // Don't update if below minimum
                    }
                }
                ConfigEvent.IncreaseRestDuration -> {
                    currentConfig.copy(
                        restDuration = currentConfig.restDuration + 5.seconds,
                    )
                }
                ConfigEvent.DecreaseRestDuration -> {
                    val newDuration = currentConfig.restDuration - 5.seconds
                    if (newDuration >= 0.seconds) {
                        currentConfig.copy(restDuration = newDuration)
                    } else {
                        return@launch // Don't update if below minimum
                    }
                }
                ConfigEvent.Reset -> {
                    TimerConfiguration.DEFAULT
                }
            }

            configurationRepository.updateConfiguration(updatedConfig)
        }
    }
}
