package com.wearinterval.data.repository

import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.TimerConfiguration
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigurationRepositoryInitTest {

  private val dataStoreManager = mockk<DataStoreManager>(relaxed = true)
  private val configurationDao = mockk<ConfigurationDao>()

  @Test
  fun `should persist DEFAULT configuration when DataStore is empty on first access`() = runTest {
    // Given DataStore returns DEFAULT configuration (first launch)
    val configFlow = MutableStateFlow(TimerConfiguration.DEFAULT)
    every { dataStoreManager.currentConfiguration } returns configFlow
    every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

    // When repository is created and currentConfiguration is accessed
    val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)
    val config = repository.currentConfiguration.first()

    // Give the async persistence time to run
    delay(100)

    // Then DEFAULT configuration should be persisted asynchronously
    coVerify { dataStoreManager.updateCurrentConfiguration(TimerConfiguration.DEFAULT) }

    // And currentConfiguration should return DEFAULT
    assertEquals(TimerConfiguration.DEFAULT, config)
  }

  @Test
  fun `should not persist configuration when DataStore already has saved config`() = runTest {
    // Given DataStore returns an existing configuration
    val existingConfig =
      TimerConfiguration(
        laps = 10,
        workDuration = 30.seconds,
        restDuration = 10.seconds,
      )
    every { dataStoreManager.currentConfiguration } returns flowOf(existingConfig)
    every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

    // When repository is created and currentConfiguration is accessed
    val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)
    val config = repository.currentConfiguration.first()

    // Give any async operations time to run
    delay(100)

    // Then no configuration should be persisted (not called)
    coVerify(exactly = 0) { dataStoreManager.updateCurrentConfiguration(any()) }

    // And currentConfiguration should return the existing config
    assertEquals(existingConfig, config)
  }

  @Test
  fun `currentConfiguration should always return valid config even when DataStore is empty`() =
    runTest {
      // Given DataStore returns DEFAULT configuration
      every { dataStoreManager.currentConfiguration } returns flowOf(TimerConfiguration.DEFAULT)
      every { configurationDao.getRecentConfigurationsFlow(any()) } returns flowOf(emptyList())

      // When repository is created and currentConfiguration is accessed
      val repository = ConfigurationRepositoryImpl(dataStoreManager, configurationDao)
      val config = repository.currentConfiguration.first()

      // Then DEFAULT configuration should be returned
      assertEquals(TimerConfiguration.DEFAULT, config)
    }
}
