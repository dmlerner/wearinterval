package com.wearinterval.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakePermissionManager : PermissionManager {

  private val _heartRatePermissionRequests = MutableSharedFlow<Unit>(replay = 1)
  override val heartRatePermissionRequests: SharedFlow<Unit> =
    _heartRatePermissionRequests.asSharedFlow()

  private val _heartRatePermissionResults = MutableSharedFlow<Boolean>(replay = 1)
  override val heartRatePermissionResults: SharedFlow<Boolean> =
    _heartRatePermissionResults.asSharedFlow()

  override fun requestHeartRatePermission() {
    _heartRatePermissionRequests.tryEmit(Unit)
  }

  override fun onHeartRatePermissionResult(granted: Boolean) {
    _heartRatePermissionResults.tryEmit(granted)
  }

  fun simulatePermissionResult(granted: Boolean) {
    onHeartRatePermissionResult(granted)
  }
}
