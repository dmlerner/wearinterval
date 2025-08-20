package com.wearinterval.data.repository

import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.database.TimerConfigurationEntity
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.util.Constants
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class ConfigurationRepositoryImpl
@Inject
constructor(
  private val dataStoreManager: DataStoreManager,
  private val configurationDao: ConfigurationDao,
) : ConfigurationRepository {

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  // Use DataStore as the single source of truth
  override val currentConfiguration: StateFlow<TimerConfiguration> =
    dataStoreManager.currentConfiguration.stateIn(
      scope = repositoryScope,
      started = SharingStarted.Eagerly,
      initialValue = TimerConfiguration.DEFAULT
    )

  override val recentConfigurations: StateFlow<List<TimerConfiguration>> =
    configurationDao
      .getRecentConfigurationsFlow(Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT)
      .map { entities -> entities.map { it.toDomain() } }
      .stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList(),
      )

  override suspend fun updateConfiguration(config: TimerConfiguration): Result<Unit> {
    return try {
      val validatedConfig =
        TimerConfiguration.validate(
          config.laps,
          config.workDuration,
          config.restDuration,
        )

      // Check if a configuration with the same VALIDATED values already exists (LRU behavior)
      // Use VALIDATED values for search to find actual duplicates after validation
      val existingConfig =
        configurationDao.findConfigurationByValues(
          laps = validatedConfig.laps,
          workDurationSeconds = validatedConfig.workDuration.inWholeSeconds,
          restDurationSeconds = validatedConfig.restDuration.inWholeSeconds,
        )

      val finalConfig =
        if (existingConfig != null) {
          // Use existing ID but update timestamp (LRU: move to front)
          validatedConfig.copy(
            id = existingConfig.id,
            lastUsed = System.currentTimeMillis(),
          )
        } else {
          // New configuration
          validatedConfig.copy(
            id = config.id,
            lastUsed = System.currentTimeMillis(),
          )
        }

      configurationDao.insertConfiguration(
        TimerConfigurationEntity.fromDomain(finalConfig),
      )
      dataStoreManager.updateCurrentConfiguration(finalConfig)

      cleanupRecentConfigurations()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun saveToHistory(config: TimerConfiguration): Result<Unit> {
    return try {
      val validatedConfig =
        TimerConfiguration.validate(
          config.laps,
          config.workDuration,
          config.restDuration,
        )

      // Check if a configuration with the same VALIDATED values already exists (LRU behavior)
      // Use VALIDATED values for search to find actual duplicates after validation
      val existingConfig =
        configurationDao.findConfigurationByValues(
          laps = validatedConfig.laps,
          workDurationSeconds = validatedConfig.workDuration.inWholeSeconds,
          restDurationSeconds = validatedConfig.restDuration.inWholeSeconds,
        )

      val finalConfig =
        if (existingConfig != null) {
          // Reuse existing ID and update timestamp (move to front of LRU)
          validatedConfig.copy(
            id = existingConfig.id,
            lastUsed = System.currentTimeMillis(),
          )
        } else {
          // New configuration - create new entry with provided ID
          validatedConfig.copy(
            id = config.id,
            lastUsed = System.currentTimeMillis(),
          )
        }

      // Save with LRU behavior - same configs update timestamp, different configs create new
      // entries
      configurationDao.insertConfiguration(
        TimerConfigurationEntity.fromDomain(finalConfig),
      )

      cleanupRecentConfigurations()

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun selectRecentConfiguration(config: TimerConfiguration): Result<Unit> {
    return try {
      // Just update the current configuration - don't modify history
      // History entries should only be created when timers actually run
      val finalConfig = config.withUpdatedTimestamp()

      // Update DataStore with selected configuration
      dataStoreManager.updateCurrentConfiguration(finalConfig)

      // LRU behavior: Check if a configuration with the same values already exists
      val existingConfig =
        configurationDao.findConfigurationByValues(
          laps = config.laps,
          workDurationSeconds = config.workDuration.inWholeSeconds,
          restDurationSeconds = config.restDuration.inWholeSeconds,
        )

      // Update the timestamp using the existing ID if found, otherwise the config's ID
      val idToUpdate = existingConfig?.id ?: config.id
      configurationDao.updateLastUsed(idToUpdate, finalConfig.lastUsed)

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun deleteConfiguration(configId: String): Result<Unit> {
    return try {
      configurationDao.deleteConfiguration(configId)
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  override suspend fun clearAllData(): Result<Unit> {
    return try {
      dataStoreManager.clearAllData()
      // Note: We don't clear Room data as it's meant to be persistent history
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  private suspend fun cleanupRecentConfigurations() {
    try {
      val count = configurationDao.getConfigurationCount()
      if (count > Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT) {
        configurationDao.cleanupOldConfigurations(Constants.Dimensions.RECENT_CONFIGURATIONS_COUNT)
      }
    } catch (e: Exception) {
      // Log error but don't fail the operation
    }
  }
}
