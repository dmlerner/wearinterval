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
      val stopResult = timerRepository.stopTimer()
      if (stopResult.isFailure) return stopResult

      configurationRepository.selectRecentConfiguration(config)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }
}
