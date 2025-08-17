package com.wearinterval.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.wearinterval.data.service.TimerService
import com.wearinterval.domain.model.TimerState
import com.wearinterval.domain.repository.ConfigurationRepository
import com.wearinterval.domain.repository.TimerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class TimerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configurationRepository: ConfigurationRepository,
) : TimerRepository {

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var timerService: TimerService? = null
    private val _isServiceBound = MutableStateFlow(false)

    override val isServiceBound: StateFlow<Boolean> = _isServiceBound.asStateFlow()

    override val timerState: StateFlow<TimerState> = _isServiceBound.flatMapLatest { bound ->
        if (bound && timerService != null) {
            combine(
                timerService!!.timerState,
                configurationRepository.currentConfiguration,
            ) { serviceState, config ->
                // Always ensure stopped state uses current configuration
                if (serviceState.isStopped) {
                    TimerState.stopped(config)
                } else {
                    serviceState
                }
            }
        } else {
            configurationRepository.currentConfiguration.map { config ->
                TimerState.stopped(config)
            }
        }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly,
        initialValue = TimerState.stopped(),
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            // Force service to sync with current configuration immediately
            repositoryScope.launch {
                val currentConfig = configurationRepository.currentConfiguration.value
                if (timerService?.timerState?.value?.isStopped == true) {
                    // Service is stopped, ensure it has the latest config
                    android.util.Log.d("TimerRepo", "Service connected, syncing config: ${currentConfig.laps} laps")
                }
            }
            _isServiceBound.value = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            _isServiceBound.value = false
        }
    }

    init {
        bindToService()
    }

    override suspend fun startTimer(): Result<Unit> {
        return try {
            ensureServiceBound()
            val config = configurationRepository.currentConfiguration.value

            // Save configuration to history when timer starts
            configurationRepository.saveToHistory(
                config.copy(
                    id = UUID.randomUUID().toString(), // Generate new ID to create unique history entry
                    lastUsed = System.currentTimeMillis(),
                ),
            )

            timerService?.startTimer(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun pauseTimer(): Result<Unit> {
        return try {
            ensureServiceBound()
            timerService?.pauseTimer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resumeTimer(): Result<Unit> {
        return try {
            ensureServiceBound()
            timerService?.resumeTimer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun stopTimer(): Result<Unit> {
        return try {
            ensureServiceBound()
            timerService?.stopTimer()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun dismissAlarm(): Result<Unit> {
        return try {
            ensureServiceBound()
            timerService?.dismissAlarm()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun bindToService() {
        val intent = Intent(context, TimerService::class.java)
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun ensureServiceBound() {
        if (!_isServiceBound.value || timerService == null) {
            throw IllegalStateException("Timer service is not bound")
        }
    }

    fun unbindService() {
        try {
            context.unbindService(serviceConnection)
            _isServiceBound.value = false
            timerService = null
        } catch (e: Exception) {
            // Service was already unbound
        }
    }

    fun cleanup() {
        unbindService()
        repositoryScope.cancel()
    }
}
