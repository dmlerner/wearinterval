package com.wearinterval.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.NotificationSettings
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
class SettingsRepositoryTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockDataStoreManager = mockk<DataStoreManager>(relaxed = true)
  private lateinit var repository: SettingsRepositoryImpl

  private val defaultSettings = NotificationSettings.DEFAULT
  private val customSettings =
    NotificationSettings(
      vibrationEnabled = false,
      soundEnabled = true,
      flashEnabled = false,
      autoMode = false,
    )

  @Before
  fun setup() {
    every { mockDataStoreManager.notificationSettings } returns MutableStateFlow(defaultSettings)
    repository = SettingsRepositoryImpl(mockDataStoreManager)
  }

  @Test
  fun `notificationSettings exposes DataStore flow`() = runTest {
    // Given
    val settingsFlow = MutableStateFlow(customSettings)
    every { mockDataStoreManager.notificationSettings } returns settingsFlow
    repository = SettingsRepositoryImpl(mockDataStoreManager)

    // When/Then
    repository.notificationSettings.test {
      // StateFlow.stateIn may emit initialValue first, then the actual value
      val firstItem = awaitItem()
      if (firstItem == NotificationSettings.DEFAULT) {
        // If we get the initial value, wait for the actual value
        assertThat(awaitItem()).isEqualTo(customSettings)
      } else {
        // If we get the actual value immediately, that's fine too
        assertThat(firstItem).isEqualTo(customSettings)
      }

      settingsFlow.value = defaultSettings
      assertThat(awaitItem()).isEqualTo(defaultSettings)
    }
  }

  @Test
  fun `updateSettings succeeds when DataStore succeeds`() = runTest {
    // Given
    coEvery { mockDataStoreManager.updateNotificationSettings(customSettings) } returns Unit

    // When
    val result = repository.updateSettings(customSettings)

    // Then
    assertThat(result.isSuccess).isTrue()
    coVerify { mockDataStoreManager.updateNotificationSettings(customSettings) }
  }

  @Test
  fun `updateSettings fails when DataStore throws exception`() = runTest {
    // Given
    val exception = RuntimeException("DataStore error")
    coEvery { mockDataStoreManager.updateNotificationSettings(customSettings) } throws exception

    // When
    val result = repository.updateSettings(customSettings)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `StateFlow starts with default value`() = runTest {
    // When/Then
    repository.notificationSettings.test { assertThat(awaitItem()).isEqualTo(defaultSettings) }
  }

  @Test
  fun `repository maintains state across multiple operations`() = runTest {
    // Given
    val settingsFlow = MutableStateFlow(defaultSettings)
    every { mockDataStoreManager.notificationSettings } returns settingsFlow
    repository = SettingsRepositoryImpl(mockDataStoreManager)

    coEvery { mockDataStoreManager.updateNotificationSettings(any()) } answers
      {
        settingsFlow.value = firstArg()
      }

    // When/Then
    repository.notificationSettings.test {
      assertThat(awaitItem()).isEqualTo(defaultSettings)

      repository.updateSettings(customSettings)
      assertThat(awaitItem()).isEqualTo(customSettings)
    }
  }
}
