package com.wearinterval.util

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class PermissionManager @Inject constructor() {

  private val _heartRatePermissionRequests = MutableSharedFlow<Unit>(replay = 1)
  val heartRatePermissionRequests: SharedFlow<Unit> = _heartRatePermissionRequests.asSharedFlow()

  private val _heartRatePermissionResults = MutableSharedFlow<Boolean>(replay = 1)
  val heartRatePermissionResults: SharedFlow<Boolean> = _heartRatePermissionResults.asSharedFlow()

  fun requestHeartRatePermission() {
    _heartRatePermissionRequests.tryEmit(Unit)
  }

  fun onHeartRatePermissionResult(granted: Boolean) {
    _heartRatePermissionResults.tryEmit(granted)
  }
}
