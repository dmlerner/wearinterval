package com.wearinterval.wearos.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.wearinterval.MainActivity
import com.wearinterval.R
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerState
import com.wearinterval.util.TimeUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages timer notifications and alerts for Wear OS with proper styling.
 *
 * Handles:
 * - Foreground service notifications
 * - Timer alerts (vibration, sound)
 * - Wear OS specific notification features
 * - Screen flash effects
 */
@Singleton
class TimerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationManager: NotificationManager,
    private val vibrator: Vibrator,
) {

    companion object {
        const val TIMER_CHANNEL_ID = "timer_service_channel"
        const val ALERT_CHANNEL_ID = "timer_alert_channel"
        const val TIMER_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2

        private const val TIMER_CHANNEL_NAME = "Timer Service"
        private const val ALERT_CHANNEL_NAME = "Timer Alerts"

        // Vibration patterns
        private const val SINGLE_VIBRATION_MS = 200L
        private const val ALARM_VIBRATION_MS = 500L
        private val ALARM_PATTERN = longArrayOf(0, ALARM_VIBRATION_MS, 300, ALARM_VIBRATION_MS, 300, ALARM_VIBRATION_MS)
    }

    init {
        createNotificationChannels()
    }

    /**
     * Creates notification for foreground service showing current timer status.
     */
    fun createTimerNotification(timerState: TimerState): Notification {
        val contentIntent = createMainAppIntent()

        val title = "WearInterval Timer"
        val text = when {
            timerState.isStopped -> "Ready"
            timerState.isPaused -> "Paused - ${TimeUtils.formatDuration(timerState.timeRemaining)}"
            timerState.isResting -> "Rest - ${TimeUtils.formatDuration(timerState.timeRemaining)}"
            else -> "Work - ${TimeUtils.formatDuration(timerState.timeRemaining)}"
        }

        val subText = if (timerState.isRunning) {
            "Lap ${timerState.displayCurrentLap}"
        } else {
            null
        }

        return NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(subText)
            .setSmallIcon(getTimerIcon(timerState))
            .setContentIntent(contentIntent)
            .setOngoing(timerState.isRunning)
            .setSilent(true)
            .setShowWhen(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .apply {
                // Add Wear OS specific action buttons for running timer
                if (timerState.isRunning) {
                    addAction(createPauseAction())
                    addAction(createStopAction())
                }
            }
            .build()
    }

    /**
     * Shows an alert notification for timer events (interval completion, workout end).
     */
    fun showTimerAlert(title: String, message: String, settings: NotificationSettings, isAlarm: Boolean = false) {
        if (settings.soundEnabled) {
            playAlertSound(isAlarm)
        }

        if (settings.vibrationEnabled) {
            triggerVibration(isAlarm)
        }

        if (settings.flashEnabled) {
            triggerScreenFlash()
        }

        val notification = createAlertNotification(title, message, isAlarm)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)

        // Auto-dismiss alert notification after short delay unless it's an alarm
        if (!isAlarm) {
            // TODO: Implement auto-dismiss after 3 seconds
        }
    }

    /**
     * Updates the ongoing timer notification.
     */
    fun updateTimerNotification(timerState: TimerState) {
        val notification = createTimerNotification(timerState)
        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    /**
     * Dismisses the alert notification.
     */
    fun dismissAlert() {
        notificationManager.cancel(ALERT_NOTIFICATION_ID)
    }

    private fun createNotificationChannels() {
        // Timer service channel (low importance)
        val timerChannel = NotificationChannel(
            TIMER_CHANNEL_ID,
            TIMER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Ongoing timer status notifications"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        // Alert channel (high importance for alerts)
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            ALERT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Timer interval and completion alerts"
            setShowBadge(true)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, SINGLE_VIBRATION_MS)
        }

        notificationManager.createNotificationChannel(timerChannel)
        notificationManager.createNotificationChannel(alertChannel)
    }

    private fun createAlertNotification(title: String, message: String, isAlarm: Boolean): Notification {
        val contentIntent = createMainAppIntent()

        return NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_play_arrow)
            .setContentIntent(contentIntent)
            .setAutoCancel(!isAlarm) // Don't auto-cancel alarms
            .setTimeoutAfter(if (isAlarm) 0 else 3000) // Auto-dismiss after 3s unless alarm
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (isAlarm) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .apply {
                if (isAlarm) {
                    // Add dismiss action for alarms
                    addAction(createDismissAction())
                    setOngoing(true)
                    setFullScreenIntent(contentIntent, true)
                }
            }
            .build()
    }

    private fun createMainAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_PAUSE_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            "Pause",
            pendingIntent,
        ).build()
    }

    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_STOP_TIMER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_media_pause,
            "Stop",
            pendingIntent,
        ).build()
    }

    private fun createDismissAction(): NotificationCompat.Action {
        val intent = Intent(context, TimerNotificationReceiver::class.java).apply {
            action = TimerNotificationReceiver.ACTION_DISMISS_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Dismiss",
            pendingIntent,
        ).build()
    }

    private fun getTimerIcon(timerState: TimerState): Int {
        return when {
            timerState.isPaused -> R.drawable.ic_pause
            timerState.isRunning -> R.drawable.ic_play_arrow
            else -> android.R.drawable.ic_media_play
        }
    }

    private fun triggerVibration(isAlarm: Boolean) {
        try {
            if (isAlarm) {
                // Continuous vibration pattern for alarms
                vibrator.vibrate(VibrationEffect.createWaveform(ALARM_PATTERN, 0))
            } else {
                // Single vibration for alerts
                vibrator.vibrate(VibrationEffect.createOneShot(SINGLE_VIBRATION_MS, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            // Graceful degradation if vibration not available
        }
    }

    private fun playAlertSound(isAlarm: Boolean) {
        // TODO: Implement sound alerts with proper audio stream handling
        // For now, rely on notification channel sound settings
    }

    private fun triggerScreenFlash() {
        // TODO: Implement screen flash effect
        // This would typically involve:
        // 1. Sending intent to activity to flash screen white
        // 2. Or using system UI visibility changes
        // 3. Coordinating with activity lifecycle
    }

    /**
     * Stops continuous vibration (for alarm dismissal).
     */
    fun stopVibration() {
        try {
            vibrator.cancel()
        } catch (e: Exception) {
            // Graceful degradation
        }
    }

    /**
     * Triggers alert notification for interval completion.
     */
    fun triggerIntervalAlert(settings: NotificationSettings) {
        if (settings.vibrationEnabled) {
            triggerVibration(isAlarm = false)
        }
        if (settings.soundEnabled) {
            playAlertSound(isAlarm = false)
        }
        if (settings.flashEnabled) {
            triggerScreenFlash()
        }
    }

    /**
     * Triggers continuous alarm for manual mode interval completion.
     */
    fun triggerContinuousAlarm(settings: NotificationSettings) {
        if (settings.vibrationEnabled) {
            triggerVibration(isAlarm = true)
        }
        if (settings.soundEnabled) {
            playAlertSound(isAlarm = true)
        }
        if (settings.flashEnabled) {
            triggerScreenFlash()
        }
    }

    /**
     * Triggers workout completion notifications (triple beep/vibration).
     */
    fun triggerWorkoutComplete(settings: NotificationSettings) {
        // Triple notification pattern for workout completion
        if (settings.vibrationEnabled || settings.soundEnabled || settings.flashEnabled) {
            repeat(3) { index ->
                if (settings.vibrationEnabled) {
                    triggerVibration(isAlarm = false)
                }
                if (settings.soundEnabled) {
                    playAlertSound(isAlarm = false)
                }
                if (settings.flashEnabled) {
                    triggerScreenFlash()
                }

                // Brief delay between notifications (except after the last one)
                if (index < 2) {
                    Thread.sleep(300)
                }
            }
        }
    }
}
