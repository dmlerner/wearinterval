package com.wearinterval.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    if (!healthServicesManager.hasHeartRateCapability()) {
      _heartRateState.value = HeartRateState.Unavailable
      return Result.failure(IllegalStateException("Heart rate not available"))
    }

    if (!checkPermission()) {
      _heartRateState.value = HeartRateState.PermissionRequired
      return Result.failure(SecurityException("BODY_SENSORS permission required"))
    }

    _heartRateState.value = HeartRateState.Connecting

    healthServicesManager
      .heartRateMeasureFlow()
      .onEach { message ->
        when (message) {
          is MeasureMessage.MeasureData -> {
            _heartRateState.value = HeartRateState.Connected(message.bpm)
          }
          is MeasureMessage.Available -> {
            if (_heartRateState.value !is HeartRateState.Connected) {
              _heartRateState.value = HeartRateState.Connecting
            }
          }
          is MeasureMessage.Unavailable -> {
            _heartRateState.value = HeartRateState.Unavailable
          }
          else -> {
            /* Handle other states */
          }
        }
      }
      .catch { throwable ->
        _heartRateState.value = HeartRateState.Error(throwable.message ?: "Unknown error")
      }
      .launchIn(repositoryScope)
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
