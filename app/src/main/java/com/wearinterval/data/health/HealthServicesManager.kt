package com.wearinterval.data.health

import android.content.Context
import android.util.Log
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.guava.await

@Singleton
class HealthServicesManager @Inject constructor(@ApplicationContext private val context: Context) {
  private val healthServicesClient = HealthServices.getClient(context)
  private val measureClient = healthServicesClient.measureClient

  suspend fun hasHeartRateCapability(): Boolean =
    runCatching {
        val capabilities = measureClient.getCapabilitiesAsync().await()
        val hasCapability = DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure
        Log.d("HeartRate", "Heart rate capability check: $hasCapability")
        Log.d("HeartRate", "Supported data types: ${capabilities.supportedDataTypesMeasure}")
        hasCapability
      }
      .getOrElse { throwable ->
        Log.e("HeartRate", "Error checking capability", throwable)
        false
      }

  fun heartRateMeasureFlow(): Flow<MeasureMessage> = callbackFlow {
    Log.d("HeartRate", "Starting heart rate measure flow")

    try {
      val hasCapability = hasHeartRateCapability()

      if (!hasCapability) {
        Log.w("HeartRate", "No heart rate capability, emitting Unavailable")
        trySend(MeasureMessage.Unavailable)
        close()
        return@callbackFlow
      }
    } catch (e: Exception) {
      Log.e("HeartRate", "Error checking heart rate capability", e)
      trySend(MeasureMessage.Unavailable)
      close()
      return@callbackFlow
    }

    Log.d("HeartRate", "Heart rate capability confirmed, setting up callback")

    val callback =
      object : MeasureCallback {
        override fun onAvailabilityChanged(
          dataType: DeltaDataType<*, *>,
          availability: Availability
        ) {
          Log.d("HeartRate", "Availability changed: $availability for $dataType")
          // Only handle DataTypeAvailability (not LocationAvailability)
          if (availability is DataTypeAvailability) {
            when (availability) {
              DataTypeAvailability.ACQUIRING -> {
                Log.d("HeartRate", "Heart rate acquiring fix")
                trySend(MeasureMessage.AcquiringFix)
              }
              DataTypeAvailability.AVAILABLE -> {
                Log.d("HeartRate", "Heart rate available")
                trySend(MeasureMessage.Available)
              }
              else -> {
                Log.d("HeartRate", "Heart rate unavailable: $availability")
                trySend(MeasureMessage.Unavailable)
              }
            }
          }
        }

        override fun onDataReceived(data: DataPointContainer) {
          Log.d("HeartRate", "Data received: $data")
          val heartRateData = data.getData(DataType.HEART_RATE_BPM)
          Log.d("HeartRate", "Heart rate data points: ${heartRateData.size}")
          heartRateData.lastOrNull()?.let { dataPoint ->
            val bpm = dataPoint.value.toInt()
            Log.d("HeartRate", "Heart rate BPM: $bpm")
            trySend(MeasureMessage.MeasureData(bpm))
          }
        }
      }

    try {
      Log.d("HeartRate", "Registering measure callback")
      measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
      Log.d("HeartRate", "Callback registered successfully")
    } catch (e: Exception) {
      Log.e("HeartRate", "Failed to register callback", e)
      trySend(MeasureMessage.Unavailable)
      close()
      return@callbackFlow
    }

    awaitClose {
      Log.d("HeartRate", "Unregistering heart rate callback")
      runCatching {
          measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
        }
        .onFailure { Log.e("HeartRate", "Failed to unregister callback", it) }
    }
  }
}

sealed class MeasureMessage {
  data class MeasureData(val bpm: Int) : MeasureMessage()

  object Available : MeasureMessage()

  object AcquiringFix : MeasureMessage()

  object Unavailable : MeasureMessage()
}
