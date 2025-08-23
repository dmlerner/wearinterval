package com.wearinterval.domain.model

sealed class HeartRateState {
  object Unavailable : HeartRateState()

  object PermissionRequired : HeartRateState()

  data class Connecting(val lastKnownBpm: Int? = null) : HeartRateState()

  data class Connected(val bpm: Int) : HeartRateState()

  data class Error(val message: String, val lastKnownBpm: Int? = null) : HeartRateState()
}
