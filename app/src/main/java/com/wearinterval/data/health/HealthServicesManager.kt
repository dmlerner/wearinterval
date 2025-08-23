package com.wearinterval.data.health

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class HealthServicesManager @Inject constructor(@ApplicationContext private val context: Context) {

  suspend fun hasHeartRateCapability(): Boolean {
    // For now, return true to allow development
    // TODO: Implement actual Health Services capability check
    return true
  }

  fun heartRateMeasureFlow(): Flow<MeasureMessage> = flow {
    // Temporary implementation - emit a simulated heart rate
    // TODO: Implement actual Health Services integration
    emit(MeasureMessage.Available)

    // Simulate heart rate readings for development
    var bpm = 75
    while (true) {
      kotlinx.coroutines.delay(3000) // Update every 3 seconds
      bpm = (70..180).random() // Random BPM for testing
      emit(MeasureMessage.MeasureData(bpm))
    }
  }
}

sealed class MeasureMessage {
  data class MeasureData(val bpm: Int) : MeasureMessage()

  object Available : MeasureMessage()

  object AcquiringFix : MeasureMessage()

  object Unavailable : MeasureMessage()
}
