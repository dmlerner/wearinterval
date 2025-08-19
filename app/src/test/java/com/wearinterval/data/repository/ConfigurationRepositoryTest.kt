package com.wearinterval.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.database.TimerConfigurationEntity
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.util.Constants
import com.wearinterval.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConfigurationRepositoryTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private val mockDataStoreManager = mockk<DataStoreManager>(relaxed = true)
  private val mockConfigurationDao = mockk<ConfigurationDao>(relaxed = true)
  private lateinit var repository: ConfigurationRepositoryImpl

  private val defaultConfig = TimerConfiguration.DEFAULT
  private val customConfig =
    TimerConfiguration(
      id = "custom-id",
      laps = 5,
      workDuration = 45.seconds,
      restDuration = 15.seconds,
      lastUsed = 1000L,
    )

  private val recentConfigs =
    listOf(
      TimerConfigurationEntity.fromDomain(customConfig),
      TimerConfigurationEntity.fromDomain(defaultConfig),
    )

  @Before
  fun setup() {
    every { mockDataStoreManager.currentConfiguration } returns MutableStateFlow(defaultConfig)
    every {
      mockConfigurationDao.getRecentConfigurationsFlow(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns MutableStateFlow(recentConfigs)
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 2

    repository = ConfigurationRepositoryImpl(mockDataStoreManager, mockConfigurationDao)
  }

  @Test
  fun `currentConfiguration exposes DataStore flow`() = runTest {
    // Given
    val configFlow = MutableStateFlow(customConfig)
    every { mockDataStoreManager.currentConfiguration } returns configFlow
    repository = ConfigurationRepositoryImpl(mockDataStoreManager, mockConfigurationDao)

    // When/Then
    repository.currentConfiguration.test {
      assertThat(awaitItem()).isEqualTo(customConfig)

      configFlow.value = defaultConfig
      assertThat(awaitItem()).isEqualTo(defaultConfig)
    }
  }

  @Test
  fun `recentConfigurations exposes DAO flow as domain models`() = runTest {
    // Given - create fresh flow and repository for this test
    val entitiesFlow = MutableStateFlow(recentConfigs)
    every {
      mockConfigurationDao.getRecentConfigurationsFlow(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns entitiesFlow
    repository = ConfigurationRepositoryImpl(mockDataStoreManager, mockConfigurationDao)

    // When/Then
    repository.recentConfigurations.test {
      // WhileSubscribed flows may emit initial value (empty list) first
      val firstEmission = awaitItem()

      val configs =
        if (firstEmission.isEmpty()) {
          // If first emission is empty, wait for the real data
          awaitItem()
        } else {
          firstEmission
        }

      assertThat(configs).hasSize(2)
      // Compare domain properties instead of whole objects due to timestamp differences
      assertThat(configs[0].id).isEqualTo(customConfig.id)
      assertThat(configs[0].laps).isEqualTo(customConfig.laps)
      assertThat(configs[0].workDuration).isEqualTo(customConfig.workDuration)
      assertThat(configs[0].restDuration).isEqualTo(customConfig.restDuration)

      assertThat(configs[1].id).isEqualTo(defaultConfig.id)
      assertThat(configs[1].laps).isEqualTo(defaultConfig.laps)
      assertThat(configs[1].workDuration).isEqualTo(defaultConfig.workDuration)
      assertThat(configs[1].restDuration).isEqualTo(defaultConfig.restDuration)
    }
  }

  @Test
  fun `updateConfiguration validates and saves to both DataStore and Room`() = runTest {
    // Given
    val invalidConfig =
      TimerConfiguration(
        laps = 1000, // Invalid - over max
        workDuration = 1.seconds, // Invalid - under min
        restDuration = 15.minutes, // Invalid - over max
      )

    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit

    // When
    val result = repository.updateConfiguration(invalidConfig)

    // Then
    assertThat(result.isSuccess).isTrue()

    coVerify {
      mockDataStoreManager.updateCurrentConfiguration(
        match { config ->
          config.laps == 999 && // Corrected to max
          config.workDuration == 1.seconds && // Corrected to min (actual MIN_WORK_DURATION)
            config.restDuration == 10.minutes // Corrected to max
        },
      )
    }

    coVerify {
      mockConfigurationDao.insertConfiguration(
        match { entity ->
          entity.laps == 999 &&
            entity.workDurationSeconds == 1L && // Corrected to min (1 second)
            entity.restDurationSeconds == 600L
        },
      )
    }
  }

  @Test
  fun `updateConfiguration updates timestamp`() = runTest {
    // Given
    val originalTime = customConfig.lastUsed
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit

    // When
    repository.updateConfiguration(customConfig)

    // Then
    coVerify {
      mockDataStoreManager.updateCurrentConfiguration(
        match { config -> config.lastUsed > originalTime },
      )
    }
  }

  @Test
  fun `updateConfiguration triggers cleanup when over limit`() = runTest {
    // Given
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 25 // Over limit
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
    coEvery {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns Unit

    // When
    repository.updateConfiguration(customConfig)

    // Then
    coVerify {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    }
  }

  @Test
  fun `updateConfiguration fails when DataStore throws exception`() = runTest {
    // Given
    val exception = RuntimeException("DataStore error")
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } throws exception

    // When
    val result = repository.updateConfiguration(customConfig)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `selectRecentConfiguration updates DataStore and DAO timestamp`() = runTest {
    // Given
    val originalTime = customConfig.lastUsed
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.updateLastUsed(any(), any()) } returns Unit
    coEvery { mockConfigurationDao.findConfigurationByValues(any(), any(), any()) } returns null

    // When
    val result = repository.selectRecentConfiguration(customConfig)

    // Then
    assertThat(result.isSuccess).isTrue()

    coVerify {
      mockDataStoreManager.updateCurrentConfiguration(
        match { config -> config.lastUsed > originalTime },
      )
    }

    coVerify {
      mockConfigurationDao.updateLastUsed(
        customConfig.id,
        match { timestamp -> timestamp > originalTime },
      )
    }
  }

  @Test
  fun `selectRecentConfiguration fails when DAO throws exception`() = runTest {
    // Given
    val exception = RuntimeException("DAO error")
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.updateLastUsed(any(), any()) } throws exception

    // When
    val result = repository.selectRecentConfiguration(customConfig)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `deleteConfiguration removes from DAO`() = runTest {
    // Given
    coEvery { mockConfigurationDao.deleteConfiguration(customConfig.id) } returns Unit

    // When
    val result = repository.deleteConfiguration(customConfig.id)

    // Then
    assertThat(result.isSuccess).isTrue()
    coVerify { mockConfigurationDao.deleteConfiguration(customConfig.id) }
  }

  @Test
  fun `deleteConfiguration fails when DAO throws exception`() = runTest {
    // Given
    val exception = RuntimeException("DAO error")
    coEvery { mockConfigurationDao.deleteConfiguration(customConfig.id) } throws exception

    // When
    val result = repository.deleteConfiguration(customConfig.id)

    // Then
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isEqualTo(exception)
  }

  @Test
  fun `updateConfiguration with existing values reuses existing ID (LRU behavior)`() = runTest {
    // Given
    val existingConfig = TimerConfigurationEntity("existing-id", 5, 45, 15, 1000L)
    val newConfigWithSameValues =
      TimerConfiguration(
        id = "new-id",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
        lastUsed = 2000L,
      )

    coEvery { mockConfigurationDao.findConfigurationByValues(5, 45, 15) } returns existingConfig
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 3
    coEvery {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns Unit

    // When
    val result = repository.updateConfiguration(newConfigWithSameValues)

    // Then
    assertThat(result.isSuccess).isTrue()

    // Verify that the existing ID is used (LRU behavior)
    coVerify {
      mockConfigurationDao.insertConfiguration(
        match { entity ->
          entity.id == "existing-id" && // Uses existing ID
          entity.laps == 5 &&
            entity.workDurationSeconds == 45L &&
            entity.restDurationSeconds == 15L &&
            entity.lastUsed > 2000L // Updated timestamp
        },
      )
    }
  }

  @Test
  fun `updateConfiguration with new values creates new entry`() = runTest {
    // Given
    val newConfig =
      TimerConfiguration(
        id = "new-id",
        laps = 10,
        workDuration = 2.minutes,
        restDuration = 30.seconds,
        lastUsed = 2000L,
      )

    coEvery { mockConfigurationDao.findConfigurationByValues(10, 120, 30) } returns null
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 3
    coEvery {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns Unit

    // When
    val result = repository.updateConfiguration(newConfig)

    // Then
    assertThat(result.isSuccess).isTrue()

    // Verify that the new ID is used
    coVerify {
      mockConfigurationDao.insertConfiguration(
        match { entity ->
          entity.id == "new-id" && // Uses new ID
          entity.laps == 10 &&
            entity.workDurationSeconds == 120L &&
            entity.restDurationSeconds == 30L
        },
      )
    }
  }

  @Test
  fun `selectRecentConfiguration with existing values reuses existing ID (LRU behavior)`() =
    runTest {
      // Given
      val existingConfig = TimerConfigurationEntity("existing-id", 5, 45, 15, 1000L)
      val configToSelect =
        TimerConfiguration(
          id = "different-id",
          laps = 5,
          workDuration = 45.seconds,
          restDuration = 15.seconds,
          lastUsed = 2000L,
        )

      coEvery { mockConfigurationDao.findConfigurationByValues(5, 45, 15) } returns existingConfig
      coEvery { mockConfigurationDao.updateLastUsed(any(), any()) } returns Unit
      coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit

      // When
      val result = repository.selectRecentConfiguration(configToSelect)

      // Then
      assertThat(result.isSuccess).isTrue()

      // Verify that the existing ID is used for timestamp update
      coVerify {
        mockConfigurationDao.updateLastUsed(
          "existing-id", // Uses existing ID
          match { timestamp -> timestamp > 2000L },
        )
      }
    }

  @Test
  fun `updateConfiguration triggers cleanup when capacity exceeded`() = runTest {
    // Given
    coEvery { mockConfigurationDao.findConfigurationByValues(any(), any(), any()) } returns null
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
    coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 7 // Exceeds capacity of 6
    coEvery {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    } returns Unit

    // When
    val result = repository.updateConfiguration(customConfig)

    // Then
    assertThat(result.isSuccess).isTrue()
    coVerify {
      mockConfigurationDao.cleanupOldConfigurations(
        Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT
      )
    }
  }

  @Test
  fun `saveToHistory prevents duplicate configurations properly`() = runTest {
    // Given: First config saved to history
    val config1 =
      TimerConfiguration(
        id = "first-id",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
        lastUsed = 1000L
      )

    // Second config with same functional values but different ID
    val config2 =
      TimerConfiguration(
        id = "second-id",
        laps = 5,
        workDuration = 45.seconds,
        restDuration = 15.seconds,
        lastUsed = 2000L
      )

    // Mock that first save finds no existing config
    coEvery { mockConfigurationDao.findConfigurationByValues(5, 45, 15) } returns null
    coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
    coEvery { mockConfigurationDao.getConfigurationCount() } returns 3
    coEvery { mockConfigurationDao.cleanupOldConfigurations(any()) } returns Unit

    // When: Save first config
    val result1 = repository.saveToHistory(config1)

    // Then: First config should be saved successfully
    assertThat(result1.isSuccess).isTrue()

    // Mock that second save finds the existing config from first save
    val savedEntity = TimerConfigurationEntity("first-id", 5, 45, 15, 1000L)
    coEvery { mockConfigurationDao.findConfigurationByValues(5, 45, 15) } returns savedEntity

    // When: Save second config with same values
    val result2 = repository.saveToHistory(config2)

    // Then: Should reuse existing ID, not create duplicate
    assertThat(result2.isSuccess).isTrue()

    // Verify both configurations use the same ID (LRU behavior working correctly)
    coVerify(exactly = 2) { // Called twice - once for each config, but both with same ID
      mockConfigurationDao.insertConfiguration(
        match { entity ->
          entity.id == "first-id" && // Both should use first ID (LRU)
          entity.laps == 5 && entity.workDurationSeconds == 45L && entity.restDurationSeconds == 15L
        }
      )
    }
  }

  @Test
  fun `updateConfiguration with invalid values that validate to existing config should reuse existing ID`() =
    runTest {
      // Given: Existing config with minimum valid values (edge case scenario)
      val existingConfig =
        TimerConfigurationEntity("existing-id", 5, 1, 15, 1000L) // 1 second work duration

      // New config with invalid work duration that will be validated to match existing
      val newConfigWithInvalidValues =
        TimerConfiguration(
          id = "new-id",
          laps = 5,
          workDuration = 0.seconds, // Invalid - will be validated to MIN_WORK_DURATION (1 second)
          restDuration = 15.seconds,
          lastUsed = 2000L
        )

      // Mock: Search with validated values (0s becomes 1s) finds existing config
      coEvery { mockConfigurationDao.findConfigurationByValues(5, 1, 15) } returns existingConfig

      coEvery { mockConfigurationDao.insertConfiguration(any()) } returns Unit
      coEvery { mockDataStoreManager.updateCurrentConfiguration(any()) } returns Unit
      coEvery { mockConfigurationDao.getConfigurationCount() } returns 3
      coEvery { mockConfigurationDao.cleanupOldConfigurations(any()) } returns Unit

      // When: Save config with invalid values
      val result = repository.updateConfiguration(newConfigWithInvalidValues)

      // Then: Should be successful
      assertThat(result.isSuccess).isTrue()

      // FIXED: Now reuses existing ID when validated values match existing config
      coVerify {
        mockConfigurationDao.insertConfiguration(
          match { entity ->
            entity.id == "existing-id" && // FIXED: Uses existing ID, preventing duplicates
            entity.laps == 5 &&
              entity.workDurationSeconds == 1L && // Validated value
              entity.restDurationSeconds == 15L
          }
        )
      }
    }
}
