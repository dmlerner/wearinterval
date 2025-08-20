package com.wearinterval.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel
@Inject
constructor(
  private val timerRepository: TimerRepository,
  private val configurationRepository: ConfigurationRepository,
  private val settingsRepository: SettingsRepository,
  private val timeProvider: TimeProvider,
) : ViewModel() {

  private val flashScreen = MutableStateFlow(false)

  // MainViewModel initialized - all state managed via StateFlow combine

  val uiState: StateFlow<MainUiState> =
    combine(
        timerRepository.timerState,
        configurationRepository.currentConfiguration,
        timerRepository.isServiceBound,
        flashScreen,
      ) { timerState, configuration, isServiceBound, flash ->

        // When stopped, always use configuration values to avoid race conditions
        val displayLaps = if (timerState.isStopped) configuration.laps else timerState.totalLaps
        val displayCurrentLap = if (timerState.isStopped) 1 else timerState.currentLap

        // Calculate accurate time remaining using system time for running states
        val displayTimeRemaining =
          when {
            timerState.isStopped -> configuration.workDuration
            timerState.isPaused -> timerState.timeRemaining
            timerState.isRunning || timerState.isResting -> {
              val currentTime = timeProvider.currentTimeMillis()
              val elapsedTime = (currentTime - timerState.intervalStartTime).milliseconds
              val totalIntervalDuration =
                if (timerState.isResting) {
                  timerState.configuration.restDuration
                } else {
                  timerState.configuration.workDuration
                }
              // Ensure time remaining never goes negative
              val remaining = totalIntervalDuration - elapsedTime
              if (remaining < 0.milliseconds) 0.milliseconds else remaining
            }
            else -> timerState.timeRemaining
          }

        MainUiState(
          isLoading = false,
          timerPhase = timerState.phase,
          timeRemaining = displayTimeRemaining,
          currentLap = displayCurrentLap,
          totalLaps = displayLaps,
          isPaused = timerState.isPaused,
          configuration = configuration,
          isPlayButtonEnabled = isServiceBound,
          isStopButtonEnabled =
            timerState.phase != com.wearinterval.domain.model.TimerPhase.Stopped,
          isServiceBound = isServiceBound,
          flashScreen = flash,
        )
      }
      .stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        // Always use DEFAULT as initial - repository guarantees consistency
        initialValue = MainUiState(isLoading = true),
      )

  fun onEvent(event: MainEvent) {
    when (event) {
      MainEvent.PlayPauseClicked -> handlePlayPauseClick()
      MainEvent.StopClicked -> handleStopClick()
      MainEvent.DismissAlarm -> handleDismissAlarm()
      MainEvent.FlashScreenDismissed -> flashScreen.value = false
    }
  }

  private fun handlePlayPauseClick() {
    viewModelScope.launch {
      val currentState = uiState.value
      when {
        currentState.isStopped -> timerRepository.startTimer()
        currentState.isPaused -> timerRepository.resumeTimer()
        currentState.isResting -> timerRepository.skipRest()
        currentState.isRunning -> timerRepository.pauseTimer()
      }
    }
  }

  private fun handleStopClick() {
    viewModelScope.launch { timerRepository.stopTimer() }
  }

  private fun handleDismissAlarm() {
    viewModelScope.launch { timerRepository.dismissAlarm() }
  }

  fun triggerFlash() {
    flashScreen.value = true
  }
}
