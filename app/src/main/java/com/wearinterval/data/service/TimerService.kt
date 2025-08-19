package com.wearinterval.data.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.util.Constants
import com.wearinterval.wearos.notification.TimerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerService : Service() {

  @Inject lateinit var timerNotificationManager: TimerNotificationManager

  @Inject lateinit var settingsRepository: SettingsRepository

  @Inject lateinit var configurationRepository: ConfigurationRepository

  @Inject lateinit var powerManager: PowerManager

  private val binder = TimerBinder()
  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _timerState = MutableStateFlow<TimerState>(TimerState.stopped())
  val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

  private var countdownJob: Job? = null
  private var wakeLock: PowerManager.WakeLock? = null

  // For testing only - allows direct state manipulation in tests
  internal fun setTimerStateForTesting(state: TimerState) {
    _timerState.value = state
  }

  override fun onCreate() {
    super.onCreate()
    // Notification channels are handled by TimerNotificationManager
    initializeWakeLock()
    initializeTimerState()
    observeConfigurationChanges()
  }

  override fun onBind(intent: Intent): IBinder = binder

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val notification = timerNotificationManager.createTimerNotification(_timerState.value)
    startForeground(TimerNotificationManager.TIMER_NOTIFICATION_ID, notification)
    return START_STICKY
  }

  override fun onDestroy() {
    stopCountdown()
    releaseWakeLock()
    serviceScope.cancel()
    super.onDestroy()
  }

  fun syncConfiguration(config: TimerConfiguration) {
    _timerState.value = _timerState.value.copy(configuration = config)
    if (_timerState.value.isStopped) {
      _timerState.value = TimerState.stopped(config)
    }
    timerNotificationManager.updateTimerNotification(_timerState.value)
  }

  fun startTimer(config: TimerConfiguration) {
    if (_timerState.value.phase != TimerPhase.Stopped) {
      throw IllegalStateException("Timer is already running")
    }

    _timerState.value =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = config.workDuration,
        currentLap = 1,
        totalLaps = config.laps,
        isPaused = false,
        configuration = config,
      )

    // Update notification when timer state changes
    timerNotificationManager.updateTimerNotification(_timerState.value)

    // Acquire wake lock and start countdown
    acquireWakeLock()
    startCountdown()
  }

  // Store the previous phase when pausing to restore correctly
  private var pausedFromPhase: TimerPhase = TimerPhase.Stopped

  fun pauseTimer() {
    val currentState = _timerState.value
    if (currentState.phase == TimerPhase.Running || currentState.phase == TimerPhase.Resting) {
      pausedFromPhase = currentState.phase
      stopCountdown()
      _timerState.value =
        currentState.copy(
          phase = TimerPhase.Paused,
          isPaused = true,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)
    }
  }

  fun resumeTimer() {
    val currentState = _timerState.value
    if (currentState.phase == TimerPhase.Paused) {
      _timerState.value =
        currentState.copy(
          phase = pausedFromPhase,
          isPaused = false,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)
      startCountdown()
    }
  }

  fun stopTimer() {
    stopCountdown()
    releaseWakeLock()
    _timerState.value = TimerState.stopped()
    timerNotificationManager.updateTimerNotification(_timerState.value)
    timerNotificationManager.stopVibration()
    timerNotificationManager.dismissAlert()
  }

  fun dismissAlarm() {
    val currentState = _timerState.value
    if (currentState.phase == TimerPhase.AlarmActive) {
      timerNotificationManager.stopVibration()
      timerNotificationManager.dismissAlert()

      serviceScope.launch {
        val settings = settingsRepository.notificationSettings.first()
        handleAlarmDismissal(currentState, settings)
      }
    }
  }

  // ================================
  // Private Timer Logic Methods
  // ================================

  private fun initializeWakeLock() {
    wakeLock =
      powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "WearInterval:TimerService",
      )
  }

  private fun initializeTimerState() {
    // Initialize with current configuration to ensure consistency - do this synchronously
    val currentConfig = configurationRepository.currentConfiguration.value
    _timerState.value = TimerState.stopped(currentConfig)
  }

  private fun observeConfigurationChanges() {
    serviceScope.launch {
      configurationRepository.currentConfiguration.collect { config ->
        // Only update if timer is stopped to maintain single source of truth
        if (_timerState.value.isStopped) {
          _timerState.value = TimerState.stopped(config)
          timerNotificationManager.updateTimerNotification(_timerState.value)
        } else {}
      }
    }
  }

  private fun acquireWakeLock() {
    wakeLock?.takeIf { !it.isHeld }?.acquire(10 * 60 * 1000L) // 10 minutes max
  }

  private fun releaseWakeLock() {
    wakeLock?.takeIf { it.isHeld }?.release()
  }

  private fun startCountdown() {
    countdownJob?.cancel()
    countdownJob =
      serviceScope.launch {
        while (_timerState.value.isRunning) {
          delay(Constants.TimerService.UPDATE_INTERVAL) // Update every 100ms for smooth progress
          updateTimerState()
        }
      }
  }

  private fun stopCountdown() {
    countdownJob?.cancel()
    countdownJob = null
  }

  private suspend fun updateTimerState() {
    val currentState = _timerState.value
    if (!currentState.isRunning || currentState.isPaused) return

    val newTimeRemaining = currentState.timeRemaining - Constants.TimerService.COUNTDOWN_DECREMENT

    if (newTimeRemaining <= Constants.TimerLimits.MIN_REST_DURATION) {
      // Current interval completed
      handleIntervalComplete(currentState)
    } else {
      // Continue countdown
      _timerState.value = currentState.copy(timeRemaining = newTimeRemaining)
      timerNotificationManager.updateTimerNotification(_timerState.value)
    }
  }

  private suspend fun handleIntervalComplete(currentState: TimerState) {
    val settings = settingsRepository.notificationSettings.first()

    // Trigger notifications for interval completion
    timerNotificationManager.triggerIntervalAlert(settings)

    if (currentState.isResting) {
      // Rest period completed, start next work interval
      handleRestComplete(currentState, settings)
    } else {
      // Work interval completed, start rest or next lap
      handleWorkComplete(currentState, settings)
    }
  }

  private suspend fun handleWorkComplete(currentState: TimerState, settings: NotificationSettings) {
    val config = currentState.configuration

    if (config.restDuration > Constants.TimerLimits.MIN_REST_DURATION) {
      // Start rest period
      _timerState.value =
        currentState.copy(
          phase = TimerPhase.Resting,
          timeRemaining = config.restDuration,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)

      if (settings.autoMode) {
        // Continue automatically after brief delay
        delay(Constants.TimerService.INTERVAL_TRANSITION_DELAY)
      } else {
        // Manual mode: pause and wait for user dismissal
        pausedFromPhase = TimerPhase.Resting
        _timerState.value =
          _timerState.value.copy(
            phase = TimerPhase.AlarmActive,
            isPaused = true,
          )
        timerNotificationManager.updateTimerNotification(_timerState.value)
        timerNotificationManager.triggerContinuousAlarm(settings)
      }
    } else {
      // No rest period, go directly to next lap or complete
      advanceToNextLap(currentState, settings)
    }
  }

  private suspend fun handleRestComplete(currentState: TimerState, settings: NotificationSettings) {
    advanceToNextLap(currentState, settings)
  }

  private suspend fun advanceToNextLap(currentState: TimerState, settings: NotificationSettings) {
    val config = currentState.configuration
    val nextLap = currentState.currentLap + 1

    if (currentState.isInfinite || nextLap <= currentState.totalLaps) {
      // Start next lap
      _timerState.value =
        currentState.copy(
          phase = TimerPhase.Running,
          timeRemaining = config.workDuration,
          currentLap = nextLap,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)

      if (settings.autoMode) {
        // Continue automatically after brief delay
        delay(Constants.TimerService.INTERVAL_TRANSITION_DELAY)
      } else {
        // Manual mode: pause and wait for user dismissal
        pausedFromPhase = TimerPhase.Running
        _timerState.value =
          _timerState.value.copy(
            phase = TimerPhase.AlarmActive,
            isPaused = true,
          )
        timerNotificationManager.updateTimerNotification(_timerState.value)
        timerNotificationManager.triggerContinuousAlarm(settings)
      }
    } else {
      // Workout completed
      handleWorkoutComplete(settings)
    }
  }

  private suspend fun handleWorkoutComplete(settings: NotificationSettings) {
    // Trigger completion notifications
    timerNotificationManager.triggerWorkoutComplete(settings)

    if (settings.autoMode) {
      // Auto mode: stop timer automatically after triple notification
      delay(
        Constants.TimerService.WORKOUT_COMPLETION_DELAY
      ) // Give time for completion sound/vibration
      stopTimer()
    } else {
      // Manual mode: wait for user dismissal
      _timerState.value =
        _timerState.value.copy(
          phase = TimerPhase.AlarmActive,
          isPaused = true,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)
      timerNotificationManager.triggerContinuousAlarm(settings)
    }
  }

  private suspend fun handleAlarmDismissal(
    currentState: TimerState,
    settings: NotificationSettings
  ) {
    // Check if workout is complete: either currentLap > totalLaps OR currentLap == totalLaps and
    // we're at the end of the final lap
    val isWorkoutComplete =
      !currentState.isInfinite &&
        (currentState.currentLap > currentState.totalLaps ||
          (currentState.currentLap == currentState.totalLaps &&
            pausedFromPhase == TimerPhase.Running))

    if (isWorkoutComplete) {
      // Workout is complete, stop timer
      stopTimer()
    } else {
      // Resume to the phase we were in before alarm
      _timerState.value =
        currentState.copy(
          phase = pausedFromPhase,
          isPaused = false,
        )
      timerNotificationManager.updateTimerNotification(_timerState.value)
      startCountdown()
    }
  }

  inner class TimerBinder : Binder() {
    fun getService(): TimerService = this@TimerService
  }
}
