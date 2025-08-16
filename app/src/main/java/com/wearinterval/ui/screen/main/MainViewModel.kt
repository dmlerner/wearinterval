package com.wearinterval.ui.screen.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.domain.repository.TimerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val configurationRepository: ConfigurationRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val flashScreen = MutableStateFlow(false)

    val uiState: StateFlow<MainUiState> = combine(
        timerRepository.timerState,
        configurationRepository.currentConfiguration,
        timerRepository.isServiceBound,
        flashScreen,
    ) { timerState, configuration, isServiceBound, flash ->
        MainUiState(
            timerPhase = timerState.phase,
            timeRemaining = timerState.timeRemaining,
            currentLap = timerState.currentLap,
            totalLaps = timerState.totalLaps,
            isPaused = timerState.isPaused,
            configuration = configuration,
            isPlayButtonEnabled = isServiceBound,
            isStopButtonEnabled = timerState.phase != com.wearinterval.domain.model.TimerPhase.Stopped,
            isServiceBound = isServiceBound,
            flashScreen = flash,
        )
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(),
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
                currentState.isRunning || currentState.isResting -> timerRepository.pauseTimer()
            }
        }
    }

    private fun handleStopClick() {
        viewModelScope.launch {
            timerRepository.stopTimer()
        }
    }

    private fun handleDismissAlarm() {
        viewModelScope.launch {
            timerRepository.dismissAlarm()
        }
    }

    fun triggerFlash() {
        flashScreen.value = true
    }
}
