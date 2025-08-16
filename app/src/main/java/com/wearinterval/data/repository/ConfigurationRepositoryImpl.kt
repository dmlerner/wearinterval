package com.wearinterval.data.repository

import com.wearinterval.data.database.ConfigurationDao
import com.wearinterval.data.database.TimerConfigurationEntity
import com.wearinterval.data.datastore.DataStoreManager
import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigurationRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val configurationDao: ConfigurationDao,
) : ConfigurationRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val currentConfiguration: StateFlow<TimerConfiguration> =
        dataStoreManager.currentConfiguration.stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = TimerConfiguration.DEFAULT,
        )

    override val recentConfigurations: StateFlow<List<TimerConfiguration>> =
        configurationDao.getRecentConfigurationsFlow(4)
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(
                scope = repositoryScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    override suspend fun updateConfiguration(config: TimerConfiguration): Result<Unit> {
        return try {
            val validatedConfig = TimerConfiguration.validate(
                config.laps,
                config.workDuration,
                config.restDuration,
            ).copy(
                id = config.id,
                lastUsed = System.currentTimeMillis(),
            )

            configurationDao.insertConfiguration(
                TimerConfigurationEntity.fromDomain(validatedConfig),
            )
            dataStoreManager.updateCurrentConfiguration(validatedConfig)

            cleanupOldConfigurations()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectRecentConfiguration(config: TimerConfiguration): Result<Unit> {
        return try {
            val updatedConfig = config.withUpdatedTimestamp()

            configurationDao.updateLastUsed(updatedConfig.id, updatedConfig.lastUsed)
            dataStoreManager.updateCurrentConfiguration(updatedConfig)

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

    private suspend fun cleanupOldConfigurations() {
        try {
            val count = configurationDao.getConfigurationCount()
            if (count > MAX_STORED_CONFIGURATIONS) {
                configurationDao.cleanupOldConfigurations(MAX_STORED_CONFIGURATIONS)
            }
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }

    companion object {
        private const val MAX_STORED_CONFIGURATIONS = 20
    }
}
