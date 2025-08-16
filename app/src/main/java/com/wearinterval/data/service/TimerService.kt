package com.wearinterval.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.wearinterval.R
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@AndroidEntryPoint
class TimerService : Service() {
    
    @Inject
    lateinit var notificationManager: NotificationManager
    
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _timerState = MutableStateFlow<TimerState>(TimerState.stopped())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    // For testing only - allows direct state manipulation in tests
    internal fun setTimerStateForTesting(state: TimerState) {
        _timerState.value = state
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_service_channel"
        private const val CHANNEL_NAME = "Timer Service"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
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
            configuration = config
        )
        
        // TODO: Implement countdown logic in Phase 6
    }
    
    // Store the previous phase when pausing to restore correctly
    private var pausedFromPhase: TimerPhase = TimerPhase.Stopped
    
    fun pauseTimer() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.Running || currentState.phase == TimerPhase.Resting) {
            pausedFromPhase = currentState.phase
            _timerState.value = currentState.copy(
                phase = TimerPhase.Paused,
                isPaused = true
            )
        }
    }
    
    fun resumeTimer() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.Paused) {
            _timerState.value = currentState.copy(
                phase = pausedFromPhase,
                isPaused = false
            )
        }
    }
    
    fun stopTimer() {
        _timerState.value = TimerState.stopped()
    }
    
    fun dismissAlarm() {
        val currentState = _timerState.value
        if (currentState.phase == TimerPhase.AlarmActive) {
            // TODO: Implement alarm dismissal logic in Phase 6
            _timerState.value = currentState.copy(phase = TimerPhase.Stopped)
        }
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timer service notification channel"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WearInterval Timer")
            .setContentText("Timer is running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
}