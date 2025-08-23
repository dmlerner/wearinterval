package com.wearinterval.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.wearinterval.data.health.HealthServicesManager
import com.wearinterval.data.health.MeasureMessage
import com.wearinterval.domain.model.HeartRateState
import com.wearinterval.domain.repository.HeartRateRepository
import com.wearinterval.util.Logger
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
  private var lastKnownBpm: Int? = null
  override val heartRateState: StateFlow<HeartRateState> = _heartRateState.asStateFlow()

  override val isAvailable: StateFlow<Boolean> =
    heartRateState
      .map { it is HeartRateState.Connected }
      .stateIn(scope = repositoryScope, started = SharingStarted.Eagerly, initialValue = false)

  override suspend fun startMonitoring(): Result<Unit> = runCatching {
    Logger.heartRate("HeartRateRepository.startMonitoring() called")
    Logger.heartRate("Current state: ${_heartRateState.value}")

    if (!healthServicesManager.hasHeartRateCapability()) {
      Logger.heartRate("Heart rate capability not available")
      _heartRateState.value = HeartRateState.Unavailable
      return Result.failure(IllegalStateException("Heart rate not available"))
    }
    Logger.heartRate("Heart rate capability confirmed available")

    if (!checkPermission()) {
      Logger.heartRate("BODY_SENSORS permission not granted")
      _heartRateState.value = HeartRateState.PermissionRequired
      return Result.failure(SecurityException("BODY_SENSORS permission required"))
    }
    Logger.heartRate("BODY_SENSORS permission confirmed granted")

    Logger.heartRate(
      "Starting heart rate monitoring, setting state to Connecting(lastKnownBpm=$lastKnownBpm)"
    )
    _heartRateState.value = HeartRateState.Connecting(lastKnownBpm)

    healthServicesManager
      .heartRateMeasureFlow()
      .onEach { message ->
        Logger.heartRate("Received message: $message (current state: ${_heartRateState.value})")
        when (message) {
          is MeasureMessage.MeasureData -> {
            Logger.heartRate("Heart rate data received: ${message.bpm} BPM")
            lastKnownBpm = message.bpm
            _heartRateState.value = HeartRateState.Connected(message.bpm)
            Logger.heartRate("State updated to Connected(${message.bpm})")
          }
          is MeasureMessage.Available -> {
            Logger.heartRate("Heart rate sensor available")
            if (_heartRateState.value !is HeartRateState.Connected) {
              _heartRateState.value = HeartRateState.Connecting(lastKnownBpm)
              Logger.heartRate("State updated to Connecting($lastKnownBpm)")
            }
          }
          is MeasureMessage.AcquiringFix -> {
            Logger.heartRate("Heart rate sensor acquiring fix")
            _heartRateState.value = HeartRateState.Connecting(lastKnownBpm)
            Logger.heartRate("State updated to Connecting($lastKnownBpm)")
          }
          is MeasureMessage.Unavailable -> {
            Logger.heartRate("Heart rate sensor unavailable")
            _heartRateState.value = HeartRateState.Unavailable
            Logger.heartRate("State updated to Unavailable")
          }
          else -> {
            Logger.heartRate("Other message type: $message")
          }
        }
      }
      .catch { throwable ->
        Logger.heartRateError("Error in heart rate flow", throwable)
        _heartRateState.value =
          HeartRateState.Error(throwable.message ?: "Unknown error", lastKnownBpm)
        Logger.heartRate("State updated to Error(${throwable.message}, $lastKnownBpm)")
      }
      .launchIn(repositoryScope)

    Logger.heartRate("Heart rate monitoring started successfully")
    Result.success(Unit)
  }

  override suspend fun stopMonitoring(): Result<Unit> = runCatching {
    Logger.heartRate("stopMonitoring() called")
    repositoryScope.coroutineContext.cancelChildren()
    _heartRateState.value = HeartRateState.Unavailable
    Logger.heartRate("Monitoring stopped, state set to Unavailable")
  }

  override suspend fun checkPermission(): Boolean {
    val granted =
      ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) ==
        PackageManager.PERMISSION_GRANTED
    Logger.heartRate("checkPermission() result: $granted")
    return granted
  }
}
