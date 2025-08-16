package com.wearinterval.ui.screen.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.domain.model.NotificationSettings
import com.wearinterval.domain.repository.SettingsRepository
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mockSettingsRepository = mockk<SettingsRepository>()
    private val notificationSettingsFlow = MutableStateFlow(NotificationSettings.DEFAULT)

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        every { mockSettingsRepository.notificationSettings } returns notificationSettingsFlow
        coEvery { mockSettingsRepository.updateSettings(any()) } returns Result.success(Unit)

        viewModel = SettingsViewModel(mockSettingsRepository)
    }

    @Test
    fun `ui state reflects current notification settings`() = runTest {
        // Given
        val testSettings = NotificationSettings(
            vibrationEnabled = false,
            soundEnabled = true,
            autoMode = false,
            flashEnabled = true,
        )

        // When
        notificationSettingsFlow.value = testSettings

        // Then
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.vibrationEnabled).isFalse()
            assertThat(uiState.soundEnabled).isTrue()
            assertThat(uiState.autoModeEnabled).isFalse()
            assertThat(uiState.flashEnabled).isTrue()
        }
    }

    @Test
    fun `toggle vibration updates settings`() = runTest {
        // Given
        val initialSettings = NotificationSettings(vibrationEnabled = false)
        notificationSettingsFlow.value = initialSettings

        // When
        viewModel.onEvent(SettingsEvent.ToggleVibration)

        // Then
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(vibrationEnabled = true),
            )
        }
    }

    @Test
    fun `toggle sound updates settings`() = runTest {
        // Given
        val initialSettings = NotificationSettings(soundEnabled = false)
        notificationSettingsFlow.value = initialSettings

        // When
        viewModel.onEvent(SettingsEvent.ToggleSound)

        // Then
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(soundEnabled = true),
            )
        }
    }

    @Test
    fun `toggle auto progress updates settings`() = runTest {
        // Given
        val initialSettings = NotificationSettings(autoMode = false)
        notificationSettingsFlow.value = initialSettings

        // When
        viewModel.onEvent(SettingsEvent.ToggleAutoMode)

        // Then
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(autoMode = true),
            )
        }
    }

    @Test
    fun `toggle screen flash updates settings`() = runTest {
        // Given
        val initialSettings = NotificationSettings(flashEnabled = false)
        notificationSettingsFlow.value = initialSettings

        // When
        viewModel.onEvent(SettingsEvent.ToggleFlash)

        // Then
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(flashEnabled = true),
            )
        }
    }

    @Test
    fun `ui state emits default settings initially`() = runTest {
        // Given - setup with default settings

        // When/Then
        viewModel.uiState.test {
            val uiState = awaitItem()
            assertThat(uiState.vibrationEnabled).isEqualTo(NotificationSettings.DEFAULT.vibrationEnabled)
            assertThat(uiState.soundEnabled).isEqualTo(NotificationSettings.DEFAULT.soundEnabled)
            assertThat(uiState.autoModeEnabled).isEqualTo(NotificationSettings.DEFAULT.autoMode)
            assertThat(uiState.flashEnabled).isEqualTo(NotificationSettings.DEFAULT.flashEnabled)
        }
    }

    @Test
    fun `multiple toggle events work correctly`() = runTest {
        // Given
        val initialSettings = NotificationSettings()
        notificationSettingsFlow.value = initialSettings

        // When
        viewModel.onEvent(SettingsEvent.ToggleVibration)
        viewModel.onEvent(SettingsEvent.ToggleSound)

        // Then
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(vibrationEnabled = !initialSettings.vibrationEnabled),
            )
        }
        coVerify {
            mockSettingsRepository.updateSettings(
                initialSettings.copy(soundEnabled = !initialSettings.soundEnabled),
            )
        }
    }
}
