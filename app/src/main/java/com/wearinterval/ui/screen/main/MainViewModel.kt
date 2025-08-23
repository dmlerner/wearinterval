package com.wearinterval.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.HeartRateRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.util.Logger
import com.wearinterval.util.PermissionManager
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
  private val heartRateRepository: HeartRateRepository,
  private val permissionManager: PermissionManager,
  private val timeProvider: TimeProvider,
) : ViewModel() {

  private val flashScreen = MutableStateFlow(false)

  init {
    Logger.main("MainViewModel initialized")
    // Start heart rate monitoring immediately and keep it running
    Logger.main("Starting initial heart rate monitoring...")
    viewModelScope.launch {
      val result = heartRateRepository.startMonitoring()
      Logger.main("Initial heart rate monitoring result: $result")
    }

    // Observe permission results
    Logger.main("Setting up permission results observer...")
    viewModelScope.launch {
      permissionManager.heartRatePermissionResults.collect { granted ->
        Logger.main("Received heart rate permission result: $granted")
        handleHeartRatePermissionResult(granted)
      }
    }
  }

  // MainViewModel initialized - all state managed via StateFlow combine

  val uiState: StateFlow<MainUiState> =
    combine(
        timerRepository.timerState,
        configurationRepository.currentConfiguration,
        timerRepository.isServiceBound,
        heartRateRepository.heartRateState,
        flashScreen,
      ) { timerState, configuration, isServiceBound, heartRateState, flash ->

        // When stopped, always use configuration values to avoid race conditions
        val displayLaps = if (timerState.isStopped) configuration.laps else timerState.totalLaps
        val displayCurrentLap = if (timerState.isStopped) 1 else timerState.currentLap

        // Calculate accurate time remaining using total running duration
        val displayTimeRemaining =
          when {
            timerState.isStopped -> configuration.workDuration
            timerState.isPaused -> timerState.timeRemaining
            timerState.isRunning || timerState.isResting -> {
              val currentTime = timeProvider.currentTimeMillis()
              val currentRunningTime = (currentTime - timerState.intervalStartTime).milliseconds
              val totalRunningTime = timerState.totalRunningDuration + currentRunningTime
              val totalIntervalDuration =
                if (timerState.isResting) {
                  timerState.configuration.restDuration
                } else {
                  timerState.configuration.workDuration
                }
              // Ensure time remaining never goes negative
              val remaining = totalIntervalDuration - totalRunningTime
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
          isStopButtonEnabled = timerState.phase != TimerPhase.Stopped,
          isServiceBound = isServiceBound,
          flashScreen = flash,
          heartRateState = heartRateState,
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
      MainEvent.HeartRateClicked -> handleHeartRateClick()
      is MainEvent.HeartRatePermissionResult -> handleHeartRatePermissionResult(event.granted)
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

  private fun handleHeartRateClick() {
    Logger.main("Heart rate clicked")
    viewModelScope.launch {
      val hasPermission = heartRateRepository.checkPermission()
      Logger.main("Permission check result: $hasPermission")
      if (!hasPermission) {
        Logger.main("Requesting heart rate permission...")
        permissionManager.requestHeartRatePermission()
      } else {
        Logger.main("Permission already granted, no action needed")
      }
    }
  }

  private fun handleHeartRatePermissionResult(granted: Boolean) {
    Logger.main("Processing heart rate permission result: $granted")
    if (granted) {
      Logger.main("Permission granted, starting heart rate monitoring...")
      viewModelScope.launch {
        val result = heartRateRepository.startMonitoring()
        Logger.main("Heart rate monitoring after permission result: $result")
      }
    } else {
      Logger.main("Permission denied, heart rate monitoring not started")
    }
  }

  fun triggerFlash() {
    flashScreen.value = true
  }
}
