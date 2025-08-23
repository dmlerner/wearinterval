package com.wearinterval.util

import com.wearinterval.domain.model.HeartRateState
import com.wearinterval.domain.repository.HeartRateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class FakeHeartRateRepository : HeartRateRepository {

  private val fakeScope = CoroutineScope(SupervisorJob())

  private val _heartRateState = MutableStateFlow<HeartRateState>(HeartRateState.Unavailable)
  override val heartRateState: StateFlow<HeartRateState> = _heartRateState.asStateFlow()

  override val isAvailable: StateFlow<Boolean> =
    heartRateState
      .map { it is HeartRateState.Connected }
      .stateIn(scope = fakeScope, started = SharingStarted.Eagerly, initialValue = false)

  var hasPermission = false
  var hasCapability = false
  var shouldFailMonitoring = false

  override suspend fun startMonitoring(): Result<Unit> {
    return if (shouldFailMonitoring) {
      _heartRateState.value = HeartRateState.Error("Fake monitoring failure")
      Result.failure(RuntimeException("Fake monitoring failure"))
    } else if (!hasPermission) {
      _heartRateState.value = HeartRateState.PermissionRequired
      Result.failure(SecurityException("Permission required"))
    } else if (!hasCapability) {
      _heartRateState.value = HeartRateState.Unavailable
      Result.failure(UnsupportedOperationException("Heart rate not available"))
    } else {
      _heartRateState.value = HeartRateState.Connecting
      Result.success(Unit)
    }
  }

  override suspend fun stopMonitoring(): Result<Unit> {
    _heartRateState.value = HeartRateState.Unavailable
    return Result.success(Unit)
  }

  override suspend fun checkPermission(): Boolean = hasPermission

  // Test helper methods
  fun setHeartRateData(bpm: Int) {
    _heartRateState.value = HeartRateState.Connected(bpm)
  }

  fun setConnecting() {
    _heartRateState.value = HeartRateState.Connecting
  }

  fun setError(message: String) {
    _heartRateState.value = HeartRateState.Error(message)
  }

  fun setUnavailable() {
    _heartRateState.value = HeartRateState.Unavailable
  }

  fun setPermissionRequired() {
    _heartRateState.value = HeartRateState.PermissionRequired
  }
}
