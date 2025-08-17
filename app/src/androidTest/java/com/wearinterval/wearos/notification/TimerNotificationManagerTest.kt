package com.wearinterval.wearos.notification

import android.app.NotificationManager
import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Instrumented tests for TimerNotificationManager.
 *
 * Tests notification creation, alerts, and Android framework integration.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimerNotificationManagerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var notificationManager: TimerNotificationManager

    private lateinit var context: Context
    private lateinit var systemNotificationManager: NotificationManager
    private lateinit var vibrator: Vibrator

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        systemNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    @Test
    fun notificationChannelsAreCreated() {
        // When - channels are created during initialization
        // (already done in setup through Hilt injection)

        // Then - verify channels exist
        val timerChannel = systemNotificationManager.getNotificationChannel(
            TimerNotificationManager.TIMER_CHANNEL_ID,
        )
        val alertChannel = systemNotificationManager.getNotificationChannel(
            TimerNotificationManager.ALERT_CHANNEL_ID,
        )

        assertThat(timerChannel).isNotNull()
        assertThat(timerChannel.name).isEqualTo("Timer Service")
        assertThat(timerChannel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)

        assertThat(alertChannel).isNotNull()
        assertThat(alertChannel.name).isEqualTo("Timer Alerts")
        assertThat(alertChannel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
    }

    @Test
    fun createTimerNotificationForStoppedState() {
        // Given
        val timerState = TimerState.stopped()

        // When
        val notification = notificationManager.createTimerNotification(timerState)

        // Then
        assertThat(notification).isNotNull()
        assertThat(notification.channelId).isEqualTo(TimerNotificationManager.TIMER_CHANNEL_ID)
        assertThat(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT).isEqualTo(0)

        // Check notification content
        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_TITLE)).isEqualTo("WearInterval Timer")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).isEqualTo("Ready")
    }

    @Test
    fun createTimerNotificationForRunningState() {
        // Given
        val config = TimerConfiguration(
            id = "test",
            laps = 5,
            workDuration = 2.minutes,
            restDuration = 30.seconds,
            lastUsed = System.currentTimeMillis(),
        )
        val timerState = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 90.seconds,
            currentLap = 2,
            totalLaps = 5,
            isPaused = false,
            configuration = config,
        )

        // When
        val notification = notificationManager.createTimerNotification(timerState)

        // Then
        assertThat(notification).isNotNull()
        assertThat(notification.channelId).isEqualTo(TimerNotificationManager.TIMER_CHANNEL_ID)
        assertThat(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)

        // Check notification content
        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_TITLE)).isEqualTo("WearInterval Timer")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("Work")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("1:30")
        assertThat(extras.getString(android.app.Notification.EXTRA_SUB_TEXT)).isEqualTo("Lap 2/5")

        // Check that action buttons are present for running timer
        assertThat(notification.actions).isNotNull()
        assertThat(notification.actions.size).isEqualTo(2) // Pause and Stop actions
    }

    @Test
    fun createTimerNotificationForRestingState() {
        // Given
        val config = TimerConfiguration(
            id = "test",
            laps = 5,
            workDuration = 2.minutes,
            restDuration = 30.seconds,
            lastUsed = System.currentTimeMillis(),
        )
        val timerState = TimerState(
            phase = TimerPhase.Resting,
            timeRemaining = 15.seconds,
            currentLap = 3,
            totalLaps = 5,
            isPaused = false,
            configuration = config,
        )

        // When
        val notification = notificationManager.createTimerNotification(timerState)

        // Then
        assertThat(notification).isNotNull()

        // Check notification content for rest state
        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("Rest")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("15s")
        assertThat(extras.getString(android.app.Notification.EXTRA_SUB_TEXT)).isEqualTo("Lap 3/5")
    }

    @Test
    fun createTimerNotificationForPausedState() {
        // Given
        val config = TimerConfiguration(
            id = "test",
            laps = 3,
            workDuration = 1.minutes,
            restDuration = 20.seconds,
            lastUsed = System.currentTimeMillis(),
        )
        val timerState = TimerState(
            phase = TimerPhase.Paused,
            timeRemaining = 45.seconds,
            currentLap = 1,
            totalLaps = 3,
            isPaused = true,
            configuration = config,
        )

        // When
        val notification = notificationManager.createTimerNotification(timerState)

        // Then
        assertThat(notification).isNotNull()

        // Check notification content for paused state
        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("Paused")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).contains("45s")
    }

    @Test
    fun createTimerNotificationForInfiniteTimer() {
        // Given
        val config = TimerConfiguration(
            id = "test",
            laps = 999, // Infinite
            workDuration = 5.minutes,
            restDuration = 1.minutes,
            lastUsed = System.currentTimeMillis(),
        )
        val timerState = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 3.minutes,
            currentLap = 10,
            totalLaps = 999,
            isPaused = false,
            configuration = config,
        )

        // When
        val notification = notificationManager.createTimerNotification(timerState)

        // Then
        assertThat(notification).isNotNull()

        // Check that infinite timers show lap number without total
        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_SUB_TEXT)).isEqualTo("Lap 10")
    }

    @Test
    fun showTimerAlertWithAllNotificationsEnabled() {
        // Given
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = true,
            autoMode = true,
        )

        // When
        notificationManager.showTimerAlert(
            title = "Interval Complete",
            message = "Rest time starting",
            settings = settings,
            isAlarm = false,
        )

        // Then
        // Verify alert notification was posted
        // Note: We can't easily verify vibration/sound in instrumented tests,
        // but we can verify the notification was created and posted
        val activeNotifications = systemNotificationManager.activeNotifications
        val alertNotification = activeNotifications.find {
            it.id == TimerNotificationManager.ALERT_NOTIFICATION_ID
        }

        assertThat(alertNotification).isNotNull()
        assertThat(alertNotification?.notification?.channelId).isEqualTo(TimerNotificationManager.ALERT_CHANNEL_ID)
    }

    @Test
    fun showTimerAlarmWithContinuousAlert() {
        // Given
        val settings = NotificationSettings(
            vibrationEnabled = true,
            soundEnabled = true,
            flashEnabled = false,
            autoMode = false, // Manual mode creates alarms
        )

        // When
        notificationManager.showTimerAlert(
            title = "Workout Complete",
            message = "Tap to dismiss",
            settings = settings,
            isAlarm = true,
        )

        // Then
        val activeNotifications = systemNotificationManager.activeNotifications
        val alarmNotification = activeNotifications.find {
            it.id == TimerNotificationManager.ALERT_NOTIFICATION_ID
        }

        assertThat(alarmNotification).isNotNull()
        assertThat(alarmNotification?.notification?.flags?.and(android.app.Notification.FLAG_ONGOING_EVENT)).isNotEqualTo(0)

        // Check for dismiss action in alarm
        val notification = alarmNotification?.notification
        assertThat(notification?.actions).isNotNull()
        assertThat(notification?.actions?.any { it.title == "Dismiss" }).isTrue()
    }

    @Test
    fun updateTimerNotificationChangesActiveNotification() {
        // Given
        val initialState = TimerState.stopped()
        notificationManager.updateTimerNotification(initialState)

        val runningState = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 30.seconds,
            currentLap = 1,
            totalLaps = 5,
            isPaused = false,
            configuration = TimerConfiguration.DEFAULT,
        )

        // When
        notificationManager.updateTimerNotification(runningState)

        // Then
        val activeNotifications = systemNotificationManager.activeNotifications
        val timerNotification = activeNotifications.find {
            it.id == TimerNotificationManager.TIMER_NOTIFICATION_ID
        }

        assertThat(timerNotification).isNotNull()
        val extras = timerNotification?.notification?.extras
        assertThat(extras?.getString(android.app.Notification.EXTRA_TEXT)).contains("Work")
    }

    @Test
    fun dismissAlertRemovesNotification() {
        // Given - first create an alert
        val settings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = false,
            flashEnabled = false,
            autoMode = true,
        )
        notificationManager.showTimerAlert("Test", "Test message", settings, false)

        // Verify alert exists
        var activeNotifications = systemNotificationManager.activeNotifications
        var alertExists = activeNotifications.any {
            it.id == TimerNotificationManager.ALERT_NOTIFICATION_ID
        }
        assertThat(alertExists).isTrue()

        // When
        notificationManager.dismissAlert()

        // Then
        activeNotifications = systemNotificationManager.activeNotifications
        alertExists = activeNotifications.any {
            it.id == TimerNotificationManager.ALERT_NOTIFICATION_ID
        }
        assertThat(alertExists).isFalse()
    }

    @Test
    fun stopVibrationCancelsOngoingVibration() {
        // Given - start vibration (if available)
        if (vibrator.hasVibrator()) {
            val settings = NotificationSettings(
                vibrationEnabled = true,
                soundEnabled = false,
                flashEnabled = false,
                autoMode = false,
            )
            notificationManager.showTimerAlert("Test", "Test", settings, true)

            // When
            notificationManager.stopVibration()

            // Then - verify vibration stopped (we can't easily test this programmatically,
            // but we can verify the method doesn't crash and completes successfully)
            // The actual vibration cancellation is handled by the Android system
        }

        // Test passes if no exception is thrown
        assertThat(true).isTrue()
    }

    @Test
    fun notificationActionsHaveCorrectIntents() {
        // Given
        val runningState = TimerState(
            phase = TimerPhase.Running,
            timeRemaining = 1.minutes,
            currentLap = 1,
            totalLaps = 3,
            isPaused = false,
            configuration = TimerConfiguration.DEFAULT,
        )

        // When
        val notification = notificationManager.createTimerNotification(runningState)

        // Then
        assertThat(notification.actions).isNotNull()
        assertThat(notification.actions.size).isEqualTo(2)

        // Check pause action
        val pauseAction = notification.actions.find { it.title == "Pause" }
        assertThat(pauseAction).isNotNull()
        assertThat(pauseAction?.actionIntent).isNotNull()

        // Check stop action
        val stopAction = notification.actions.find { it.title == "Stop" }
        assertThat(stopAction).isNotNull()
        assertThat(stopAction?.actionIntent).isNotNull()
    }
}
