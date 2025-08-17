package com.wearinterval.wearos.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wearinterval.domain.repository.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Broadcast receiver for handling timer notification actions.
 *
 * Handles actions like pause, stop, and dismiss from notification buttons.
 */
@AndroidEntryPoint
class TimerNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var notificationManager: TimerNotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ACTION_PAUSE_TIMER = "com.wearinterval.action.PAUSE_TIMER"
        const val ACTION_STOP_TIMER = "com.wearinterval.action.STOP_TIMER"
        const val ACTION_DISMISS_ALARM = "com.wearinterval.action.DISMISS_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_PAUSE_TIMER -> {
                scope.launch {
                    timerRepository.pauseTimer()
                }
            }

            ACTION_STOP_TIMER -> {
                scope.launch {
                    timerRepository.stopTimer()
                }
            }

            ACTION_DISMISS_ALARM -> {
                scope.launch {
                    timerRepository.dismissAlarm()
                    notificationManager.dismissAlert()
                    notificationManager.stopVibration()
                }
            }
        }
    }
}
