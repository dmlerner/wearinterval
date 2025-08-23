package com.wearinterval.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.wearinterval.data.health.HealthServicesManager
import com.wearinterval.data.health.MeasureMessage
import com.wearinterval.domain.model.HeartRateState
import com.wearinterval.domain.repository.HeartRateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

@Singleton
class HeartRateRepositoryImpl
@Inject
constructor(
  private val healthServicesManager: HealthServicesManager,
  @ApplicationContext private val context: Context
) : HeartRateRepository {

  private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  private val _heartRateState = MutableStateFlow<HeartRateState>(HeartRateState.Unavailable)
  override val heartRateState: StateFlow<HeartRateState> = _heartRateState.asStateFlow()

  override val isAvailable: StateFlow<Boolean> =
    heartRateState
      .map { it is HeartRateState.Connected }
      .stateIn(scope = repositoryScope, started = SharingStarted.Eagerly, initialValue = false)

  override suspend fun startMonitoring(): Result<Unit> = runCatching {
    Log.d("HeartRate", "HeartRateRepository.startMonitoring() called")

    if (!healthServicesManager.hasHeartRateCapability()) {
      Log.w("HeartRate", "Heart rate capability not available")
      _heartRateState.value = HeartRateState.Unavailable
      return Result.failure(IllegalStateException("Heart rate not available"))
    }

    if (!checkPermission()) {
      Log.w("HeartRate", "BODY_SENSORS permission not granted")
      _heartRateState.value = HeartRateState.PermissionRequired
      return Result.failure(SecurityException("BODY_SENSORS permission required"))
    }

    Log.d("HeartRate", "Starting heart rate monitoring, setting state to Connecting")
    _heartRateState.value = HeartRateState.Connecting

    healthServicesManager
      .heartRateMeasureFlow()
      .onEach { message ->
        Log.d("HeartRate", "Received message: $message")
        when (message) {
          is MeasureMessage.MeasureData -> {
            Log.d("HeartRate", "Heart rate data received: ${message.bpm} BPM")
            _heartRateState.value = HeartRateState.Connected(message.bpm)
          }
          is MeasureMessage.Available -> {
            Log.d("HeartRate", "Heart rate sensor available")
            if (_heartRateState.value !is HeartRateState.Connected) {
              _heartRateState.value = HeartRateState.Connecting
            }
          }
          is MeasureMessage.Unavailable -> {
            Log.w("HeartRate", "Heart rate sensor unavailable")
            _heartRateState.value = HeartRateState.Unavailable
          }
          else -> {
            Log.d("HeartRate", "Other message type: $message")
          }
        }
      }
      .catch { throwable ->
        Log.e("HeartRate", "Error in heart rate flow", throwable)
        _heartRateState.value = HeartRateState.Error(throwable.message ?: "Unknown error")
      }
      .launchIn(repositoryScope)

    Log.d("HeartRate", "Heart rate monitoring started successfully")
    Result.success(Unit)
  }

  override suspend fun stopMonitoring(): Result<Unit> = runCatching {
    repositoryScope.coroutineContext.cancelChildren()
    _heartRateState.value = HeartRateState.Unavailable
  }

  override suspend fun checkPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) ==
      PackageManager.PERMISSION_GRANTED
  }
}
