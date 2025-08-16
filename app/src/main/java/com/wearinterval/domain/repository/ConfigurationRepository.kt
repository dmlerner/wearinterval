package com.wearinterval.domain.repository

import com.wearinterval.domain.model.TimerConfiguration
import kotlinx.coroutines.flow.StateFlow

interface ConfigurationRepository {
    val currentConfiguration: StateFlow<TimerConfiguration>
    val recentConfigurations: StateFlow<List<TimerConfiguration>>

    suspend fun updateConfiguration(config: TimerConfiguration): Result<Unit>
    suspend fun selectRecentConfiguration(config: TimerConfiguration): Result<Unit>
    suspend fun deleteConfiguration(configId: String): Result<Unit>
}
