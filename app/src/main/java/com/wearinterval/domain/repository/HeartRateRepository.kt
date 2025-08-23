package com.wearinterval.domain.repository

import com.wearinterval.domain.model.HeartRateState
import kotlinx.coroutines.flow.StateFlow

interface HeartRateRepository {
  val heartRateState: StateFlow<HeartRateState>
  val isAvailable: StateFlow<Boolean>

  suspend fun startMonitoring(): Result<Unit>

  suspend fun stopMonitoring(): Result<Unit>

  suspend fun checkPermission(): Boolean
}
