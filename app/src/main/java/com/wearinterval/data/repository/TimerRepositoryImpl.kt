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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
            timerService?.timerState ?: flowOf(TimerState.stopped())
        } else {
            flowOf(TimerState.stopped())
        }
    }.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Lazily,
        initialValue = TimerState.stopped(),
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
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
