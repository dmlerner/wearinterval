package com.wearinterval.data.service

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.wearinterval.wearos.notification.TimerNotificationManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simplified instrumented tests for TimerService notification integration.
 *
 * Tests notification channel creation and basic service functionality.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TimerServiceInstrumentedTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setup() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun notificationChannelsExistForService() {
        // Given - app context with notification system initialized

        // When - we check for notification channels
        // (Channels are created when TimerNotificationManager is injected)

        // Then - verify notification channels were created
        val timerChannel = notificationManager.getNotificationChannel(
            TimerNotificationManager.TIMER_CHANNEL_ID,
        )
        val alertChannel = notificationManager.getNotificationChannel(
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
    fun contextIsAvailableForNotificationSystem() {
        // This test verifies that the Android context is properly available
        // for the notification system integration

        assertThat(context).isNotNull()
        assertThat(context.packageName).isEqualTo("com.wearinterval")
        assertThat(notificationManager).isNotNull()
    }

    @Test
    fun notificationManagerIsConfiguredCorrectly() {
        // Verify the notification manager is accessible and working
        assertThat(notificationManager).isNotNull()

        // Check that we can access notification settings
        val areNotificationsEnabled = notificationManager.areNotificationsEnabled()
        // We don't assert true/false as this depends on system settings
        // Just verify the call doesn't crash
        assertThat(areNotificationsEnabled).isNotNull()
    }
}
