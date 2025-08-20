package com.wearinterval.domain.usecase

import com.wearinterval.domain.model.TimerConfiguration
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectConfigurationUseCase
@Inject
constructor(
  private val configurationRepository: ConfigurationRepository,
  private val timerRepository: TimerRepository,
) {
  suspend fun selectConfigurationAndStopTimer(config: TimerConfiguration): Result<Unit> {
    return try {
      // Set configuration first to minimize UI state changes
      val configResult = configurationRepository.selectRecentConfiguration(config)
      if (configResult.isFailure) return configResult

      // Then stop timer - this ensures UI sees final state in one update
      timerRepository.stopTimer()
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
