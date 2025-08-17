package com.wearinterval.ui.screen.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configurationRepository: ConfigurationRepository,
    private val timerRepository: TimerRepository,
) : ViewModel() {

    init {
        android.util.Log.d("ConfigViewModel", "INIT: ConfigViewModel created")
        viewModelScope.launch {
            configurationRepository.currentConfiguration.collect { config ->
                android.util.Log.d(
                    "ConfigViewModel",
                    "INIT: Config from repo: ${config.laps} laps, ${config.workDuration}, ${config.restDuration}",
                )
            }
        }
    }

    val uiState: StateFlow<ConfigUiState> = configurationRepository.currentConfiguration
        .map { config ->
            android.util.Log.d(
                "ConfigViewModel",
                "MAP: Config received: ${config.laps} laps, ${config.workDuration}, ${config.restDuration}",
            )
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
            started = SharingStarted.WhileSubscribed(Constants.UI.SUBSCRIPTION_TIMEOUT),
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
                    currentConfig.copy(laps = TimerConfiguration.DEFAULT.laps)
                }
                ConfigEvent.ResetWork -> {
                    currentConfig.copy(workDuration = TimerConfiguration.DEFAULT.workDuration)
                }
                ConfigEvent.ResetRest -> {
                    currentConfig.copy(restDuration = TimerConfiguration.DEFAULT.restDuration)
                }
                ConfigEvent.SetLapsToInfinite -> {
                    currentConfig.copy(laps = Constants.TimerLimits.INFINITE_LAPS)
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
