package com.wearinterval.ui.screen.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ConfigViewModel
@Inject
constructor(
  private val configurationRepository: ConfigurationRepository,
  private val timerRepository: TimerRepository,
) : ViewModel() {

  // ConfigViewModel initialized - configuration state managed by StateFlow

  // Helper function to convert TimerConfiguration to ConfigUiState
  private fun TimerConfiguration.toUiState() =
    ConfigUiState(
      laps = laps,
      workMinutes = workDuration.inWholeMinutes.toInt(),
      workSeconds = (workDuration.inWholeSeconds % 60).toInt(),
      restMinutes = restDuration.inWholeMinutes.toInt(),
      restSeconds = (restDuration.inWholeSeconds % 60).toInt(),
    )

  val uiState: StateFlow<ConfigUiState> =
    configurationRepository.currentConfiguration
      .map { config ->
        android.util.Log.d(
          "ConfigViewModel",
          "MAP: Config received: ${config.laps} laps, ${config.workDuration}, ${config.restDuration}",
        )
        config.toUiState().copy(isLoading = false)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        // Always use DEFAULT as initial - repository guarantees consistency
        initialValue = ConfigUiState(isLoading = true),
      )

  fun onEvent(event: ConfigEvent) {
    viewModelScope.launch {
      val currentConfig = configurationRepository.currentConfiguration.value
      val updatedConfig =
        when (event) {
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
          ConfigEvent.ClearAllData -> {
            android.util.Log.d("ConfigViewModel", "CLEARING all data")
            configurationRepository.clearAllData()
            return@launch
          }
        }
      android.util.Log.d("ConfigViewModel", "ON_EVENT: event=$event, updatedConfig=$updatedConfig")
      configurationRepository.updateConfiguration(updatedConfig)
    }
  }
}
