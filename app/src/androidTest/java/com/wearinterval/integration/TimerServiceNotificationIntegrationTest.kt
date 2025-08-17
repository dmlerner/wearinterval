package com.wearinterval.integration

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.TimerRepository
import com.wearinterval.wearos.notification.TimerNotificationManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Integration tests for timer and notification system.
 *
 * Tests end-to-end functionality between timer state changes and notifications.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimerServiceNotificationIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var timerRepository: TimerRepository

    @Inject
    lateinit var timerNotificationManager: TimerNotificationManager

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun timerRepositoryAndNotificationManagerIntegration() = runTest {
        // Given - timer repository and notification manager are injected
        assertThat(timerRepository).isNotNull()
        assertThat(timerNotificationManager).isNotNull()

        // When/Then - verify timer state can be observed
        timerRepository.timerState.test {
            val initialState = awaitItem()
            assertThat(initialState.phase).isEqualTo(TimerPhase.Stopped)

            // Test starting timer
            timerRepository.startTimer()
            val runningState = awaitItem()
            assertThat(runningState.phase).isEqualTo(TimerPhase.Running)

            // Test stopping timer
            timerRepository.stopTimer()
            val stoppedState = awaitItem()
            assertThat(stoppedState.phase).isEqualTo(TimerPhase.Stopped)
        }
    }

    @Test
    fun notificationManagerCreatesValidNotifications() {
        // Given - a timer state
        val timerState = timerRepository.timerState.value

        // When - creating a notification
        val notification = timerNotificationManager.createTimerNotification(timerState)

        // Then - notification is valid
        assertThat(notification).isNotNull()
        assertThat(notification.channelId).isEqualTo(TimerNotificationManager.TIMER_CHANNEL_ID)

        val extras = notification.extras
        assertThat(extras.getString(android.app.Notification.EXTRA_TITLE)).isEqualTo("WearInterval Timer")
        assertThat(extras.getString(android.app.Notification.EXTRA_TEXT)).isEqualTo("Ready")
    }

    @Test
    fun timerStateChangesCanTriggerNotificationUpdates() = runTest {
        // Given - initial state
        val initialState = timerRepository.timerState.value
        assertThat(initialState.phase).isEqualTo(TimerPhase.Stopped)

        // When - creating notifications for different states
        val stoppedNotification = timerNotificationManager.createTimerNotification(initialState)
        assertThat(stoppedNotification.extras.getString(android.app.Notification.EXTRA_TEXT)).isEqualTo("Ready")

        // Update timer state through repository
        timerRepository.startTimer()

        timerRepository.timerState.test {
            val runningState = awaitItem()
            assertThat(runningState.phase).isEqualTo(TimerPhase.Running)

            // Create notification for running state
            val runningNotification = timerNotificationManager.createTimerNotification(runningState)
            assertThat(runningNotification.extras.getString(android.app.Notification.EXTRA_TEXT)).contains("Work")
        }
    }

    @Test
    fun pauseAndResumeIntegration() = runTest {
        timerRepository.timerState.test {
            // Start with stopped state
            val stoppedState = awaitItem()
            assertThat(stoppedState.phase).isEqualTo(TimerPhase.Stopped)

            // Start timer
            timerRepository.startTimer()
            val runningState = awaitItem()
            assertThat(runningState.phase).isEqualTo(TimerPhase.Running)

            // Pause timer
            timerRepository.pauseTimer()
            val pausedState = awaitItem()
            assertThat(pausedState.phase).isEqualTo(TimerPhase.Paused)
            assertThat(pausedState.isPaused).isTrue()

            // Resume timer
            timerRepository.resumeTimer()
            val resumedState = awaitItem()
            assertThat(resumedState.phase).isEqualTo(TimerPhase.Running)
            assertThat(resumedState.isPaused).isFalse()

            // Stop timer
            timerRepository.stopTimer()
            val finalStoppedState = awaitItem()
            assertThat(finalStoppedState.phase).isEqualTo(TimerPhase.Stopped)
        }
    }

    @Test
    fun notificationChannelsAreProperlyConfigured() {
        // Verify that the notification system is properly set up
        val timerChannel = notificationManager.getNotificationChannel(
            TimerNotificationManager.TIMER_CHANNEL_ID,
        )
        val alertChannel = notificationManager.getNotificationChannel(
            TimerNotificationManager.ALERT_CHANNEL_ID,
        )

        assertThat(timerChannel).isNotNull()
        assertThat(timerChannel.importance).isEqualTo(NotificationManager.IMPORTANCE_LOW)
        assertThat(timerChannel.name).isEqualTo("Timer Service")

        assertThat(alertChannel).isNotNull()
        assertThat(alertChannel.importance).isEqualTo(NotificationManager.IMPORTANCE_HIGH)
        assertThat(alertChannel.name).isEqualTo("Timer Alerts")
    }
}
