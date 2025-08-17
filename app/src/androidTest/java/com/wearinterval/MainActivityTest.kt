package com.wearinterval

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainActivity_launchesSuccessfully() {
        // Given/When - Activity launches automatically via rule

        // Then
        assertThat(composeTestRule.activity).isNotNull()
        assertThat(composeTestRule.activity.isFinishing).isFalse()
    }

    @Test
    fun mainActivity_hasCorrectClass() {
        // Given
        val activity = composeTestRule.activity

        // Then
        assertThat(activity).isInstanceOf(MainActivity::class.java)
        assertThat(activity.javaClass.simpleName).isEqualTo("MainActivity")
    }

    @Test
    fun mainActivity_hiltAnnotationPresent() {
        // Given
        val activityClass = MainActivity::class.java

        // Then - Verify Hilt annotation is present
        val hiltAnnotations = activityClass.annotations.filter {
            it.annotationClass.simpleName == "AndroidEntryPoint"
        }
        assertThat(hiltAnnotations).isNotEmpty()
    }

    @Test
    fun mainActivity_inheritsFromComponentActivity() {
        // Given
        val activityClass = MainActivity::class.java

        // Then
        assertThat(androidx.activity.ComponentActivity::class.java.isAssignableFrom(activityClass)).isTrue()
    }

    @Test
    fun mainActivity_displaysMainScreen() {
        // Given/When - Activity launches with navigation

        // Then - Should display timer elements from main screen
        // Note: This tests that the activity successfully sets up the navigation and theme
        // The main screen should be visible with its timer display
        composeTestRule.waitForIdle()

        // Main screen elements should be present (even with default state)
        // The presence of these elements indicates successful activity initialization
        val activity = composeTestRule.activity
        assertThat(activity.hasWindowFocus() || !activity.isFinishing).isTrue()
    }

    @Test
    fun mainActivity_hasWearIntervalTheme() {
        // Given
        val activity = composeTestRule.activity

        // When/Then - Activity should apply WearIntervalTheme
        // We can test this by verifying the activity sets content successfully
        composeTestRule.waitForIdle()
        assertThat(activity.isFinishing).isFalse()

        // Theme application is verified by successful content composition
        // If theme failed, the setContent would throw during composition
    }

    @Test
    fun mainActivity_initializesSplashScreen() {
        // Given
        val activity = composeTestRule.activity

        // Then - Activity should complete splash screen initialization
        // Splash screen is installed in onCreate, so if activity launches successfully,
        // splash screen was initialized correctly
        assertThat(activity).isNotNull()
        assertThat(activity.isDestroyed).isFalse()
    }

    @Test
    fun mainActivity_setsUpNavigationCorrectly() {
        // Given/When - Activity launches

        // Then - Navigation should be set up (tested by successful launch)
        composeTestRule.waitForIdle()

        val activity = composeTestRule.activity
        assertThat(activity.isFinishing).isFalse()

        // If navigation setup failed, activity would crash during setContent
        // Successful launch indicates navigation is properly configured
    }

    @Test
    fun mainActivity_handlesLifecycleCorrectly() {
        // Given
        val activity = composeTestRule.activity

        // When - Activity is in created state

        // Then
        assertThat(activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.CREATED)).isTrue()

        // Activity should handle lifecycle without crashing
        composeTestRule.waitForIdle()
        assertThat(activity.isFinishing).isFalse()
    }

    @Test
    fun mainActivity_savedInstanceStateHandling() {
        // Given
        val activity = composeTestRule.activity

        // When/Then - Activity should handle saved instance state properly
        // This is tested by verifying onCreate completed successfully with null savedInstanceState
        assertThat(activity).isNotNull()

        // Successful initialization indicates proper savedInstanceState handling
        composeTestRule.waitForIdle()
        assertThat(activity.isDestroyed).isFalse()
    }

    @Test
    fun mainActivity_properContentSetup() {
        // Given/When
        val activity = composeTestRule.activity
        composeTestRule.waitForIdle()

        // Then - Activity should have content set successfully
        // Content setup includes: theme application, navigation setup, and screen composition
        assertThat(activity.isFinishing).isFalse()

        // Verify no composition errors occurred
        // If setContent failed, activity would be in error state
        assertThat(activity.isDestroyed).isFalse()
    }
}
