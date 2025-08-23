package com.wearinterval.domain.model

sealed class HeartRateState {
  object Unavailable : HeartRateState()

  object PermissionRequired : HeartRateState()

  object Connecting : HeartRateState()

  data class Connected(val bpm: Int) : HeartRateState()

  data class Error(val message: String) : HeartRateState()
}
