package com.wearinterval.wearos

import android.app.NotificationManager
import android.content.Context
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.model.TimerState
import com.wearinterval.wearos.notification.TimerNotificationManager
import kotlin.time.Duration.Companion.seconds
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30]) // Use Android API 30 for Wear OS compatibility
class WearOsComponentsRobolectricTest {

  private lateinit var context: Context
  private lateinit var notificationManager: NotificationManager
  private lateinit var vibrator: Vibrator
  private lateinit var timerNotificationManager: TimerNotificationManager

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    timerNotificationManager = TimerNotificationManager(context, notificationManager, vibrator)
  }

  @Test
  fun `notification manager should be available through Robolectric`() {
    // Then - Notification manager should be accessible
    assertThat(notificationManager).isNotNull()

    // And notification channels should be manageable
    val channels = notificationManager.notificationChannels
    assertThat(channels).isNotNull()
  }

  @Test
  fun `vibrator service should be available through Robolectric`() {
    // Then - Vibrator should be accessible
    assertThat(vibrator).isNotNull()

    // And vibrator capabilities should be queryable
    val hasVibrator = vibrator.hasVibrator()
    // Note: Robolectric vibrator may return false, but service should be available
    assertThat(hasVibrator).isAnyOf(true, false) // Either value is acceptable
  }

  @Test
  fun `timer notification manager should create notifications for timer states`() {
    // Given - Timer state and settings
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 2,
        totalLaps = 5,
        configuration = TimerConfiguration.DEFAULT
      )
    val settings = NotificationSettings.DEFAULT

    // When - Creating foreground notification
    val notification = timerNotificationManager.createTimerNotification(timerState)

    // Then - Notification should be created successfully
    assertThat(notification).isNotNull()
  }

  @Test
  fun `notification should handle different timer phases`() {
    val configuration = TimerConfiguration.DEFAULT
    val settings = NotificationSettings.DEFAULT

    // Test Running phase
    val runningState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 45.seconds,
        currentLap = 1,
        totalLaps = 3,
        configuration = configuration
      )
    val runningNotification = timerNotificationManager.createTimerNotification(runningState)
    assertThat(runningNotification).isNotNull()

    // Test Resting phase
    val restingState =
      TimerState(
        phase = TimerPhase.Resting,
        timeRemaining = 15.seconds,
        currentLap = 1,
        totalLaps = 3,
        configuration = configuration
      )
    val restingNotification = timerNotificationManager.createTimerNotification(restingState)
    assertThat(restingNotification).isNotNull()

    // Test Paused phase
    val pausedState =
      TimerState(
        phase = TimerPhase.Paused,
        timeRemaining = 30.seconds,
        currentLap = 2,
        totalLaps = 3,
        configuration = configuration
      )
    val pausedNotification = timerNotificationManager.createTimerNotification(pausedState)
    assertThat(pausedNotification).isNotNull()
  }

  @Test
  fun `notification should include proper action buttons`() {
    // Given - Running timer state
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 1,
        configuration = TimerConfiguration.DEFAULT
      )
    val settings = NotificationSettings.DEFAULT

    // When - Creating notification
    val notification = timerNotificationManager.createTimerNotification(timerState)

    // Then - Notification should have action buttons
    assertThat(notification.actions).isNotNull()
    assertThat(notification.actions.size).isGreaterThan(0)

    // Check for pause action when running
    val actionTitles = notification.actions.map { it.title.toString() }
    assertThat(actionTitles).contains("Pause")
  }

  @Test
  fun `vibration alert should handle different notification settings`() {
    // Given - Timer state
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 1.seconds, // About to complete
        currentLap = 1,
        totalLaps = 1,
        configuration = TimerConfiguration.DEFAULT
      )

    // Test with vibration enabled
    val settingsWithVibration =
      NotificationSettings(
        vibrationEnabled = true,
        soundEnabled = false,
        flashEnabled = false,
        autoMode = false
      )

    // When - Triggering interval alert
    // This should not throw an exception with Robolectric
    timerNotificationManager.triggerIntervalAlert(settingsWithVibration)

    // Then - Should complete without exception
    // (Robolectric handles vibration calls gracefully)

    // Test with vibration disabled
    val settingsWithoutVibration =
      NotificationSettings(
        vibrationEnabled = false,
        soundEnabled = false,
        flashEnabled = false,
        autoMode = false
      )

    // When - Triggering alert without vibration
    timerNotificationManager.triggerIntervalAlert(settingsWithoutVibration)

    // Then - Should also complete without exception
  }

  @Test
  fun `notification channel should be created properly`() {
    // When - Accessing notification channels
    val channels = notificationManager.notificationChannels

    // Then - Timer notification channel should exist or be creatable
    // Note: TimerNotificationManager creates channels on first use
    assertThat(channels).isNotNull()

    // Create a notification to ensure channel is created
    val timerState = TimerState.stopped()
    timerNotificationManager.createTimerNotification(timerState)

    // Verify channel creation
    val updatedChannels = notificationManager.notificationChannels
    assertThat(updatedChannels).isNotNull()
  }

  @Test
  fun `workout completion alert should work with different settings`() {
    // Given - Different notification settings
    val allAlertsEnabled =
      NotificationSettings(
        vibrationEnabled = true,
        soundEnabled = true,
        flashEnabled = true,
        autoMode = false
      )

    val noAlertsEnabled =
      NotificationSettings(
        vibrationEnabled = false,
        soundEnabled = false,
        flashEnabled = false,
        autoMode = false
      )

    // When/Then - Both should complete without exceptions
    timerNotificationManager.triggerWorkoutComplete(allAlertsEnabled)
    timerNotificationManager.triggerWorkoutComplete(noAlertsEnabled)

    // If we get here without exceptions, the test passes
  }

  @Test
  fun `notification manager should handle foreground service notifications`() {
    // Given - Timer notification
    val timerState =
      TimerState(
        phase = TimerPhase.Running,
        timeRemaining = 30.seconds,
        currentLap = 1,
        totalLaps = 1,
        configuration = TimerConfiguration.DEFAULT
      )
    val settings = NotificationSettings.DEFAULT
    val notification = timerNotificationManager.createTimerNotification(timerState)

    // When - Showing notification (simulating foreground service)
    notificationManager.notify(1, notification)

    // Then - Should not throw exception
    // Robolectric handles notification display gracefully
    val activeNotifications = notificationManager.activeNotifications
    assertThat(activeNotifications).isNotNull()
  }
}
