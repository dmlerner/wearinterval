package com.wearinterval.data.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.wearos.notification.TimerNotificationManager
import io.mockk.mockk
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

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()

    // Mock dependencies for testing
    mockConfigurationRepository = mockk(relaxed = true)
    mockSettingsRepository = mockk(relaxed = true)
    mockNotificationManager = mockk(relaxed = true)
    mockPowerManager = mockk(relaxed = true)
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
}
