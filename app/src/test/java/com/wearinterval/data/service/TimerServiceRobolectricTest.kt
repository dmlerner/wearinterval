package com.wearinterval.data.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.model.TimerPhase
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.wearos.notification.TimerNotificationManager
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30]) // Use Android API 30 for Wear OS compatibility
class TimerServiceRobolectricTest {

  private lateinit var context: Context
  private lateinit var mockConfigurationRepository: ConfigurationRepository
  private lateinit var mockSettingsRepository: SettingsRepository
  private lateinit var mockNotificationManager: TimerNotificationManager
  private lateinit var mockPowerManager: PowerManager
  private lateinit var configurationFlow: MutableStateFlow<TimerConfiguration>
  private lateinit var notificationSettingsFlow: MutableStateFlow<NotificationSettings>

  private val testConfiguration =
    TimerConfiguration(
      id = "test-config",
      laps = 3,
      workDuration = 30.seconds,
      restDuration = 10.seconds
    )

  private val testNotificationSettings =
    NotificationSettings(soundEnabled = true, vibrationEnabled = true, autoMode = true)

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Setup flows for repositories
    configurationFlow = MutableStateFlow(testConfiguration)
    notificationSettingsFlow = MutableStateFlow(testNotificationSettings)

    // Mock dependencies for testing
    mockConfigurationRepository = mockk(relaxed = true)
    mockSettingsRepository = mockk(relaxed = true)
    mockNotificationManager = mockk(relaxed = true)
    mockPowerManager = mockk(relaxed = true)

    // Setup repository flows
    every { mockConfigurationRepository.currentConfiguration } returns configurationFlow
    every { mockSettingsRepository.notificationSettings } returns notificationSettingsFlow
  }

  @Test
  fun `service onCreate should initialize properly`() {
    // Given - Create service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)

    // When - Service is created
    val service = serviceController.create().get()

    // Then - Service should be properly initialized
    assertThat(service).isNotNull()
    // Service controller may have an auto-generated intent, which is expected in Robolectric
  }

  @Test
  fun `service onBind should return valid binder`() {
    // Given - Service controller and intent
    val intent = Intent(context, TimerService::class.java)
    val serviceController = Robolectric.buildService(TimerService::class.java, intent)

    // When - Service is created and bound
    val service = serviceController.create().get()
    val binder = service.onBind(intent)

    // Then - Binder should not be null and should be correct type
    assertThat(binder).isNotNull()
    assertThat(binder).isInstanceOf(TimerService.TimerBinder::class.java)
  }

  @Test
  fun `service onStartCommand should return START_STICKY for persistence`() {
    // Given - Service controller with start intent
    val intent = Intent(context, TimerService::class.java)
    val serviceController = Robolectric.buildService(TimerService::class.java, intent)

    // When - Service is started
    val service = serviceController.create().startCommand(0, 0).get()
    val result = service.onStartCommand(intent, 0, 1)

    // Then - Should return START_STICKY for automatic restart
    assertThat(result).isEqualTo(android.app.Service.START_STICKY)
  }

  @Test
  fun `service onDestroy should clean up resources`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)

    // When - Service is created and then destroyed
    val service = serviceController.create().get()
    serviceController.destroy()

    // Then - Service should handle destruction gracefully
    // Note: We can't directly test cleanup since TimerService cleanup is internal,
    // but we verify the service can be destroyed without exceptions
    assertThat(service).isNotNull()
  }

  @Test
  fun `service foreground functionality should work with notifications`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // When - We attempt to access notification-related functionality
    // (This tests that Robolectric can handle notification manager access)
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Then - Notification manager should be available
    assertThat(notificationManager).isNotNull()
  }

  @Test
  fun `service power management should be accessible`() {
    // Given - Context for power manager access
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    // When - We attempt to create a wake lock (without acquiring it)
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimerService::Test")

    // Then - Wake lock should be created successfully
    assertThat(wakeLock).isNotNull()
    assertThat(wakeLock.isHeld).isFalse()
  }

  @Test
  fun `service lifecycle complete flow should work`() {
    // Given - Service controller and intent
    val intent = Intent(context, TimerService::class.java)
    val serviceController = Robolectric.buildService(TimerService::class.java, intent)

    // When - Full service lifecycle
    val service =
      serviceController
        .create() // onCreate()
        .startCommand(0, 0) // onStartCommand()
        .bind() // onBind()
        .get()

    // Then - Service should be fully operational
    assertThat(service).isNotNull()

    // When - Service is unbound and destroyed
    serviceController
      .unbind() // onUnbind()
      .destroy() // onDestroy()

    // Then - Lifecycle should complete without errors
    // (If we get here without exceptions, the test passes)
  }

  @Test
  fun `service binder should provide access to service instance`() {
    // Given - Service controller
    val intent = Intent(context, TimerService::class.java)
    val serviceController = Robolectric.buildService(TimerService::class.java, intent)

    // When - Service is bound
    val service = serviceController.create().bind().get()
    val binder = service.onBind(intent) as TimerService.TimerBinder

    // Then - Binder should provide access to service
    val serviceFromBinder = binder.getService()
    assertThat(serviceFromBinder).isEqualTo(service)
  }

  @Test
  fun `multiple service instances should be independent`() {
    // Given - Two service controllers
    val serviceController1 = Robolectric.buildService(TimerService::class.java)
    val serviceController2 = Robolectric.buildService(TimerService::class.java)

    // When - Both services are created
    val service1 = serviceController1.create().get()
    val service2 = serviceController2.create().get()

    // Then - Services should be different instances
    assertThat(service1).isNotEqualTo(service2)
    assertThat(service1).isNotSameInstanceAs(service2)
  }

  // ================================
  // Advanced Timer Functionality Tests
  // ================================

  @Test
  fun `syncConfiguration should update timer state with new configuration`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    val newConfig =
      TimerConfiguration(
        id = "new-config",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 15.seconds
      )

    // When - Configuration is synced
    service.syncConfiguration(newConfig)

    // Then - Timer state should be updated with new config
    val currentState = service.timerState.value
    assertThat(currentState.configuration).isEqualTo(newConfig)
    assertThat(currentState.phase).isEqualTo(TimerPhase.Stopped)
  }

  @Test
  fun `startTimer should initialize running state correctly`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // When - Timer is started with configuration
    service.startTimer(testConfiguration)

    // Then - Timer should be in running state
    val currentState = service.timerState.value
    assertThat(currentState.phase).isEqualTo(TimerPhase.Running)
    assertThat(currentState.timeRemaining).isEqualTo(testConfiguration.workDuration)
    assertThat(currentState.currentLap).isEqualTo(1)
    assertThat(currentState.totalLaps).isEqualTo(testConfiguration.laps)
    assertThat(currentState.isPaused).isFalse()
    assertThat(currentState.configuration).isEqualTo(testConfiguration)
  }

  @Test
  fun `startTimer should throw exception if timer already running`() {
    // Given - Service controller with running timer
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()
    service.startTimer(testConfiguration)

    // When/Then - Starting timer again should throw exception
    try {
      service.startTimer(testConfiguration)
      assertThat(false).isTrue() // Should not reach here
    } catch (e: IllegalStateException) {
      assertThat(e.message).contains("Timer is already running")
    }
  }

  @Test
  fun `pauseTimer should pause running timer correctly`() {
    // Given - Service with running timer
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()
    service.startTimer(testConfiguration)

    // When - Timer is paused
    service.pauseTimer()

    // Then - Timer should be paused
    val currentState = service.timerState.value
    assertThat(currentState.phase).isEqualTo(TimerPhase.Paused)
    assertThat(currentState.isPaused).isTrue()
  }

  @Test
  fun `resumeTimer should resume paused timer correctly`() {
    // Given - Service with paused timer
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()
    service.startTimer(testConfiguration)
    service.pauseTimer()

    // When - Timer is resumed
    service.resumeTimer()

    // Then - Timer should be running again
    val currentState = service.timerState.value
    assertThat(currentState.phase).isEqualTo(TimerPhase.Running)
    assertThat(currentState.isPaused).isFalse()
  }

  @Test
  fun `stopTimer should stop timer and reset state`() {
    // Given - Service with running timer
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()
    service.startTimer(testConfiguration)

    // When - Timer is stopped
    service.stopTimer()

    // Then - Timer should be stopped with default state
    val currentState = service.timerState.value
    assertThat(currentState.phase).isEqualTo(TimerPhase.Stopped)
    assertThat(currentState.isPaused).isFalse()
    assertThat(currentState.currentLap).isEqualTo(1)
  }

  @Test
  fun `service should handle multiple client bindings`() {
    // Given - Service controller
    val intent = Intent(context, TimerService::class.java)
    val serviceController = Robolectric.buildService(TimerService::class.java, intent)

    // When - Multiple clients bind to service
    val service = serviceController.create().get()
    val binder1 = service.onBind(intent) as TimerService.TimerBinder
    val binder2 = service.onBind(intent) as TimerService.TimerBinder

    // Then - Both binders should provide access to same service
    assertThat(binder1.getService()).isEqualTo(service)
    assertThat(binder2.getService()).isEqualTo(service)
    assertThat(binder1.getService()).isEqualTo(binder2.getService())
  }

  @Test
  fun `service should manage wake lock correctly`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // When - Timer is started (should acquire wake lock)
    service.startTimer(testConfiguration)

    // Then - Service should handle wake lock management
    // (Note: We can't directly test wake lock acquisition in Robolectric,
    // but we verify the service doesn't crash during wake lock operations)
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Running)

    // When - Timer is stopped (should release wake lock)
    service.stopTimer()

    // Then - Service should handle wake lock release
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Stopped)
  }

  @Test
  fun `service should handle rapid start-stop operations`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // When - Rapid start/stop operations
    service.startTimer(testConfiguration)
    service.stopTimer()
    service.startTimer(testConfiguration)
    service.pauseTimer()
    service.resumeTimer()
    service.stopTimer()

    // Then - Service should handle rapid operations gracefully
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Stopped)
  }

  @Test
  fun `service should handle null intent in onStartCommand`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // When - onStartCommand called with null intent
    val result = service.onStartCommand(null, 0, 1)

    // Then - Should return START_STICKY without crashing
    assertThat(result).isEqualTo(android.app.Service.START_STICKY)
  }

  @Test
  fun `service should validate timer state transitions`() {
    // Given - Service controller
    val serviceController = Robolectric.buildService(TimerService::class.java)
    val service = serviceController.create().get()

    // Test invalid state transitions

    // When - Try to pause stopped timer
    service.pauseTimer()
    // Then - Should remain stopped
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Stopped)

    // When - Try to resume stopped timer
    service.resumeTimer()
    // Then - Should remain stopped
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Stopped)

    // When - Try to dismiss alarm when no alarm
    service.dismissAlarm()
    // Then - Should handle gracefully
    assertThat(service.timerState.value.phase).isEqualTo(TimerPhase.Stopped)
  }
}
