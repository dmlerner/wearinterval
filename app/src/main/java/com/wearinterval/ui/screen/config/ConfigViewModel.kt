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
import kotlin.time.Duration.Companion.minutes
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
                is ConfigEvent.SetLaps -> {
                    currentConfig.copy(laps = event.laps)
                }
                is ConfigEvent.SetWorkDuration -> {
                    currentConfig.copy(workDuration = event.duration)
                }
                is ConfigEvent.SetRestDuration -> {
                    currentConfig.copy(restDuration = event.duration)
                }
                ConfigEvent.Reset -> {
                    TimerConfiguration.DEFAULT
                }
                ConfigEvent.ResetLaps -> {
                    currentConfig.copy(laps = 1)
                }
                ConfigEvent.ResetWork -> {
                    currentConfig.copy(workDuration = 60.seconds)
                }
                ConfigEvent.ResetRest -> {
                    currentConfig.copy(restDuration = 0.seconds)
                }
                ConfigEvent.SetLapsToInfinite -> {
                    currentConfig.copy(laps = 999)
                }
                ConfigEvent.SetWorkToLong -> {
                    currentConfig.copy(workDuration = 5.minutes)
                }
                ConfigEvent.SetRestToLong -> {
                    currentConfig.copy(restDuration = 5.minutes)
                }
            }

            configurationRepository.updateConfiguration(updatedConfig)
        }
    }
}
