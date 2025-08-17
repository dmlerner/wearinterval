package com.wearinterval.data.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.wearos.notification.TimerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var timerNotificationManager: TimerNotificationManager

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _timerState = MutableStateFlow<TimerState>(TimerState.stopped())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    // For testing only - allows direct state manipulation in tests
    internal fun setTimerStateForTesting(state: TimerState) {
        _timerState.value = state
    }

    override fun onCreate() {
        super.onCreate()
        // Notification channels are handled by TimerNotificationManager
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = timerNotificationManager.createTimerNotification(_timerState.value)
        startForeground(TimerNotificationManager.TIMER_NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    fun startTimer(config: TimerConfiguration) {
        if (_timerState.value.phase != TimerPhase.Stopped) {
            throw IllegalStateException("Timer is already running")
        }

        _timerState.value = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = config.workDuration,
            currentLap = 1,
            totalLaps = config.laps,
            isPaused = false,
            configuration = config,
        )

        // Update notification when timer state changes
        timerNotificationManager.updateTimerNotification(_timerState.value)

        // TODO: Implement countdown logic in Phase 7
    }

    // Store the previous phase when pausing to restore correctly
    private var pausedFromPhase: TimerPhase = TimerPhase.Stopped

    fun pauseTimer() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.Running || currentState.phase == TimerPhase.Resting) {
            pausedFromPhase = currentState.phase
            _timerState.value = currentState.copy(
                phase = TimerPhase.Paused,
                isPaused = true,
            )
            timerNotificationManager.updateTimerNotification(_timerState.value)
        }
    }

    fun resumeTimer() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.Paused) {
            _timerState.value = currentState.copy(
                phase = pausedFromPhase,
                isPaused = false,
            )
            timerNotificationManager.updateTimerNotification(_timerState.value)
        }
    }

    fun stopTimer() {
        _timerState.value = TimerState.stopped()
        timerNotificationManager.updateTimerNotification(_timerState.value)
        timerNotificationManager.stopVibration()
        timerNotificationManager.dismissAlert()
    }

    fun dismissAlarm() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.AlarmActive) {
            // TODO: Implement alarm dismissal logic in Phase 7
            _timerState.value = currentState.copy(phase = TimerPhase.Stopped)
            timerNotificationManager.updateTimerNotification(_timerState.value)
            timerNotificationManager.stopVibration()
            timerNotificationManager.dismissAlert()
        }
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
}
