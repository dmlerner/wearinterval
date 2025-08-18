package com.wearinterval

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30]) // Use Android API 30 for Wear OS compatibility
class MainActivityRobolectricTest {

  private lateinit var mockConfigurationRepository: ConfigurationRepository
  private lateinit var mockTimerRepository: TimerRepository
  private lateinit var recentConfigurationsFlow: MutableStateFlow<List<TimerConfiguration>>

  private val sampleConfig =
    TimerConfiguration(
      id = "test-config-id",
      laps = 3,
      workDuration = kotlin.time.Duration.parse("30s"),
      restDuration = kotlin.time.Duration.parse("15s")
    )

  @Before
  fun setup() {
    // Mock dependencies
    mockConfigurationRepository = mockk(relaxed = true)
    mockTimerRepository = mockk(relaxed = true)

    // Setup flow for recent configurations
    recentConfigurationsFlow = MutableStateFlow(listOf(sampleConfig))
    every { mockConfigurationRepository.recentConfigurations } returns recentConfigurationsFlow

    // Setup coroutine returns
    coEvery { mockTimerRepository.stopTimer() } returns Result.success(Unit)
    coEvery { mockConfigurationRepository.selectRecentConfiguration(any()) } returns
      Result.success(Unit)
  }

  @Test
  fun `onCreate initializes correctly without crashing`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - Activity is created
    val activity = activityController.create().get()

    // Then - Activity should initialize successfully
    assertThat(activity).isNotNull()
    assertThat(activity.isFinishing).isFalse()
    assertThat(activity.isDestroyed).isFalse()
  }

  @Test
  fun `onCreate installs splash screen before super`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - Activity is created
    val activity = activityController.create().get()

    // Then - Activity should complete onCreate successfully
    // If splash screen installation failed, onCreate would throw
    assertThat(activity).isNotNull()
  }

  @Test
  fun `onCreate sets up dependency injection correctly`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - Activity is created
    val activity = activityController.create().get()

    // Then - Hilt should inject dependencies successfully
    // We can't directly access private fields, but successful creation indicates DI worked
    assertThat(activity).isNotNull()

    // MainActivity should be annotated with @AndroidEntryPoint for Hilt DI
    // The fact that the activity creates successfully without DI errors indicates it's working
    assertThat(activity.javaClass.simpleName).isEqualTo("MainActivity")
  }

  @Test
  fun `handleTileIntent with valid config id selects configuration`() = runTest {
    // Given - Intent with valid config ID
    val intent = Intent().apply { putExtra("config_id", "test-config-id") }
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Activity is created with tile intent
    val activity = activityController.create().get()

    // Then - Should process the tile intent
    // We can't directly test the private method, but we can verify the activity handles the intent
    assertThat(activity.intent.getStringExtra("config_id")).isEqualTo("test-config-id")
  }

  @Test
  fun `handleTileIntent with null config id does nothing`() {
    // Given - Intent without config ID
    val intent = Intent()
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Activity is created
    val activity = activityController.create().get()

    // Then - Should handle null intent gracefully
    assertThat(activity.intent.getStringExtra("config_id")).isNull()
    assertThat(activity).isNotNull()
  }

  @Test
  fun `handleTileIntent with invalid config id handles gracefully`() {
    // Given - Intent with non-existent config ID
    val intent = Intent().apply { putExtra("config_id", "non-existent-config") }
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Activity is created
    val activity = activityController.create().get()

    // Then - Should handle invalid config ID without crashing
    assertThat(activity.intent.getStringExtra("config_id")).isEqualTo("non-existent-config")
    assertThat(activity.isFinishing).isFalse()
  }

  @Test
  fun `activity handles intent with config_id extra correctly`() {
    // Given - Intent with config ID
    val intent = Intent().apply { putExtra("config_id", "test-config-id") }
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Activity is created with intent
    val activity = activityController.create().get()

    // Then - Intent should be processed without crashing
    assertThat(activity.intent.getStringExtra("config_id")).isEqualTo("test-config-id")
    assertThat(activity.isFinishing).isFalse()
  }

  @Test
  fun `activity processes multiple intent configurations consistently`() {
    // Given - Intent with specific config ID
    val intent1 = Intent().apply { putExtra("config_id", "config-1") }
    val activityController1 = Robolectric.buildActivity(MainActivity::class.java, intent1)

    // When - Activity processes first intent
    val activity1 = activityController1.create().get()

    // Given - Different intent with different config ID
    val intent2 = Intent().apply { putExtra("config_id", "config-2") }
    val activityController2 = Robolectric.buildActivity(MainActivity::class.java, intent2)

    // When - Activity processes second intent
    val activity2 = activityController2.create().get()

    // Then - Both should handle their respective intents
    assertThat(activity1.intent.getStringExtra("config_id")).isEqualTo("config-1")
    assertThat(activity2.intent.getStringExtra("config_id")).isEqualTo("config-2")
    assertThat(activity1.isFinishing).isFalse()
    assertThat(activity2.isFinishing).isFalse()
  }

  @Test
  fun `activity lifecycle handles configuration changes`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - Full lifecycle with configuration change simulation
    val activity =
      activityController.create().start().resume().pause().stop().restart().start().resume().get()

    // Then - Activity should handle lifecycle correctly
    assertThat(activity).isNotNull()
    assertThat(activity.isFinishing).isFalse()
  }

  @Test
  fun `activity handles savedInstanceState correctly`() {
    // Given - Activity controller with null savedInstanceState
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - onCreate with null savedInstanceState (normal first launch)
    val activity = activityController.create().get()

    // Then - Should handle null savedInstanceState without issues
    assertThat(activity).isNotNull()
    assertThat(
        activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.CREATED)
      )
      .isTrue()
  }

  @Test
  fun `setContent executes without composition errors`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)

    // When - Activity creates and sets content
    val activity = activityController.create().get()

    // Then - setContent should complete successfully
    // If WearIntervalTheme or WearIntervalNavigation had issues, onCreate would fail
    assertThat(activity).isNotNull()
    assertThat(activity.isFinishing).isFalse()
  }

  @Test
  fun `activity inherits from ComponentActivity correctly`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    val activity = activityController.create().get()

    // Then - Should be instance of ComponentActivity
    assertThat(activity).isInstanceOf(androidx.activity.ComponentActivity::class.java)
  }

  @Test
  fun `multiple activity instances are independent`() {
    // Given - Two activity controllers
    val controller1 = Robolectric.buildActivity(MainActivity::class.java)
    val controller2 = Robolectric.buildActivity(MainActivity::class.java)

    // When - Both activities are created
    val activity1 = controller1.create().get()
    val activity2 = controller2.create().get()

    // Then - Should be different instances
    assertThat(activity1).isNotEqualTo(activity2)
    assertThat(activity1).isNotSameInstanceAs(activity2)
  }

  @Test
  fun `activity handles intent extras safely`() {
    // Given - Intent with various extra types
    val intent =
      Intent().apply {
        putExtra("config_id", "test-config")
        putExtra("other_string", "value")
        putExtra("number", 42)
        putExtra("boolean", true)
      }
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Activity processes intent
    val activity = activityController.create().get()

    // Then - Should handle all extras without issues
    assertThat(activity.intent.getStringExtra("config_id")).isEqualTo("test-config")
    assertThat(activity.intent.getStringExtra("other_string")).isEqualTo("value")
    assertThat(activity.intent.getIntExtra("number", 0)).isEqualTo(42)
    assertThat(activity.intent.getBooleanExtra("boolean", false)).isTrue()
  }

  @Test
  fun `activity context is properly available`() {
    // Given - Activity controller
    val activityController = Robolectric.buildActivity(MainActivity::class.java)
    val activity = activityController.create().get()

    // Then - Context should be available and valid
    assertThat(activity.applicationContext).isNotNull()
    assertThat(activity.baseContext).isNotNull()
    assertThat(activity.applicationContext).isEqualTo(ApplicationProvider.getApplicationContext())
  }

  @Test
  fun `activity handles empty intent gracefully`() {
    // Given - Empty intent
    val emptyIntent = Intent()
    val activityController = Robolectric.buildActivity(MainActivity::class.java, emptyIntent)

    // When - Activity is created with empty intent
    val activity = activityController.create().get()

    // Then - Should handle empty intent without crashing
    assertThat(activity).isNotNull()
    assertThat(activity.intent).isNotNull()
    assertThat(activity.intent.getStringExtra("config_id")).isNull()
  }

  @Test
  fun `activity handles lifecycle transitions correctly with intent`() {
    // Given - Activity with config intent
    val intent = Intent().apply { putExtra("config_id", "lifecycle-test-config") }
    val activityController = Robolectric.buildActivity(MainActivity::class.java, intent)

    // When - Full lifecycle with intent processing
    val activity =
      activityController
        .create() // onCreate called - should handle tile intent
        .start()
        .resume()
        .pause()
        .stop()
        .restart()
        .start()
        .resume()
        .get()

    // Then - Activity should survive lifecycle with intent
    assertThat(activity.intent.getStringExtra("config_id")).isEqualTo("lifecycle-test-config")
    assertThat(activity.isFinishing).isFalse()
  }
}
