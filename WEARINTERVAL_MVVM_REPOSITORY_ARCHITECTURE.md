# WearInterval MVVM + Repository Architecture

## Overview

This document defines the MVVM + Repository architecture for the WearInterval app, designed to handle complex timer logic, WearOS integrations, and foreground service coordination while maintaining proper separation of concerns and testability.

## Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                           UI Layer                              │
├─────────────────┬─────────────────┬─────────────────┬───────────┤
│   MainScreen    │  ConfigScreen   │ HistoryScreen   │ Settings  │
│   MainViewModel │ ConfigViewModel │HistoryViewModel │ViewModel  │
└─────────┬───────┴─────────┬───────┴─────────┬───────┴─────┬─────┘
          │                 │                 │             │
          └─────────────────┼─────────────────┼─────────────┘
                            │                 │
┌───────────────────────────▼─────────────────▼─────────────────────┐
│                       Domain Layer                                │
├─────────────────┬─────────────────┬─────────────────┬─────────────┤
│ TimerRepository │ConfigRepository │ SettingsRepo    │ WearOSRepo  │
└─────────┬───────┴─────────┬───────┴─────────┬───────┴─────┬───────┘
          │                 │                 │             │
          └─────────────────┼─────────────────┼─────────────┘
                            │                 │
┌───────────────────────────▼─────────────────▼─────────────────────┐
│                         Data Layer                                │
├─────────────────┬─────────────────┬─────────────────┬─────────────┤
│  TimerService   │  DataStore      │  Room Database  │ WearOS APIs │
│ (Foreground)    │ (Preferences)   │ (History)       │ (Tiles/etc) │
└─────────────────┴─────────────────┴─────────────────┴─────────────┘
```

## Core Data Models

### Timer State Models

```kotlin
data class TimerState(
    val currentLap: Int = 1,
    val totalLaps: Int = 1,
    val timeRemaining: Duration = Duration.ZERO,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isResting: Boolean = false,
    val isAlarmActive: Boolean = false,
    val phase: TimerPhase = TimerPhase.STOPPED,
    val sessionStartTime: Long? = null,
    val lastUpdateTime: Long = System.currentTimeMillis()
)

enum class TimerPhase {
    STOPPED,     // Timer ready but not running
    RUNNING,     // Active work interval
    RESTING,     // Active rest interval
    PAUSED,      // Paused mid-interval
    ALARM_ACTIVE // Manual mode - awaiting dismissal
}

data class TimerConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val laps: Int = 1,
    val workDuration: Duration = 60.seconds,
    val restDuration: Duration = Duration.ZERO,
    val lastUsed: Long = System.currentTimeMillis()
) {
    val isInfinite: Boolean get() = laps == 999
    
    fun formatDisplay(): String = buildString {
        if (laps > 1) append("$laps × ")
        append(workDuration.formatDuration())
        if (restDuration > Duration.ZERO) {
            append(" + ${restDuration.formatDuration()}")
        }
    }
}

data class NotificationSettings(
    val vibrationEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val flashEnabled: Boolean = true,
    val autoMode: Boolean = true
)

sealed class TimerEvent {
    object Start : TimerEvent()
    object Pause : TimerEvent()
    object Resume : TimerEvent()
    object Stop : TimerEvent()
    object DismissAlarm : TimerEvent()
    object CompleteInterval : TimerEvent()
    data class UpdateConfiguration(val config: TimerConfiguration) : TimerEvent()
}
```

### UI State Models

```kotlin
data class MainScreenUiState(
    val timerState: TimerState = TimerState(),
    val configuration: TimerConfiguration = TimerConfiguration(),
    val settings: NotificationSettings = NotificationSettings(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ConfigScreenUiState(
    val currentConfig: TimerConfiguration = TimerConfiguration(),
    val lapOptions: List<Int> = LapOptions.DEFAULT,
    val durationOptions: List<Duration> = DurationOptions.DEFAULT,
    val isLoading: Boolean = false
)

data class HistoryScreenUiState(
    val recentConfigurations: List<TimerConfiguration> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class SettingsScreenUiState(
    val settings: NotificationSettings = NotificationSettings(),
    val isLoading: Boolean = false
)
```

## Repository Layer

### TimerRepository

```kotlin
@Singleton
class TimerRepository @Inject constructor(
    private val timerService: TimerService,
    private val configRepository: ConfigurationRepository,
    private val settingsRepository: SettingsRepository,
    private val wearOSRepository: WearOSRepository,
    private val scope: CoroutineScope
) {
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _events = MutableSharedFlow<TimerEvent>()
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()
    
    init {
        // Listen to service state changes
        scope.launch {
            timerService.timerState.collect { serviceState ->
                _timerState.value = serviceState
                updateWearOSComponents()
            }
        }
        
        // Handle timer events
        scope.launch {
            events.collect { event ->
                handleTimerEvent(event)
            }
        }
    }
    
    suspend fun startTimer(configuration: TimerConfiguration) {
        configRepository.saveRecentConfiguration(configuration)
        timerService.startTimer(configuration)
    }
    
    suspend fun pauseTimer() {
        timerService.pauseTimer()
    }
    
    suspend fun resumeTimer() {
        timerService.resumeTimer()
    }
    
    suspend fun stopTimer() {
        timerService.stopTimer()
    }
    
    suspend fun dismissAlarm() {
        timerService.dismissAlarm()
    }
    
    fun sendEvent(event: TimerEvent) {
        scope.launch {
            _events.emit(event)
        }
    }
    
    private suspend fun handleTimerEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.Start -> {
                val config = configRepository.getCurrentConfiguration()
                startTimer(config)
            }
            is TimerEvent.Pause -> pauseTimer()
            is TimerEvent.Resume -> resumeTimer()
            is TimerEvent.Stop -> stopTimer()
            is TimerEvent.DismissAlarm -> dismissAlarm()
            is TimerEvent.UpdateConfiguration -> {
                configRepository.updateConfiguration(event.config)
            }
            is TimerEvent.CompleteInterval -> {
                handleIntervalCompletion()
            }
        }
    }
    
    private suspend fun handleIntervalCompletion() {
        val currentState = _timerState.value
        val settings = settingsRepository.getSettings()
        
        // Trigger notifications
        if (settings.vibrationEnabled) timerService.triggerVibration()
        if (settings.soundEnabled) timerService.triggerSound()
        if (settings.flashEnabled) timerService.triggerFlash()
        
        // Handle mode-specific behavior
        if (settings.autoMode) {
            // Auto mode: brief notification then continue
            delay(500)
            timerService.proceedToNextInterval()
        } else {
            // Manual mode: pause and wait for dismissal
            timerService.pauseForDismissal()
        }
    }
    
    private suspend fun updateWearOSComponents() {
        wearOSRepository.updateTile(_timerState.value)
        wearOSRepository.updateComplications(_timerState.value)
    }
}
```

### ConfigurationRepository

```kotlin
@Singleton
class ConfigurationRepository @Inject constructor(
    private val configDao: ConfigurationDao,
    private val dataStore: DataStore<Preferences>
) {
    private val _currentConfiguration = MutableStateFlow(TimerConfiguration())
    val currentConfiguration: StateFlow<TimerConfiguration> = _currentConfiguration.asStateFlow()
    
    private val _recentConfigurations = MutableStateFlow<List<TimerConfiguration>>(emptyList())
    val recentConfigurations: StateFlow<List<TimerConfiguration>> = _recentConfigurations.asStateFlow()
    
    init {
        loadCurrentConfiguration()
        loadRecentConfigurations()
    }
    
    suspend fun updateConfiguration(config: TimerConfiguration) {
        _currentConfiguration.value = config
        saveCurrentConfiguration(config)
    }
    
    suspend fun saveRecentConfiguration(config: TimerConfiguration) {
        configDao.insertConfiguration(config.toEntity())
        loadRecentConfigurations()
    }
    
    suspend fun getRecentConfigurations(): List<TimerConfiguration> {
        return configDao.getRecentConfigurations(limit = 4)
            .map { it.toModel() }
    }
    
    fun getCurrentConfiguration(): TimerConfiguration {
        return _currentConfiguration.value
    }
    
    private fun loadCurrentConfiguration() {
        // Load from DataStore
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect { preferences ->
                val config = TimerConfiguration(
                    laps = preferences[Keys.LAPS] ?: 1,
                    workDuration = (preferences[Keys.WORK_DURATION] ?: 60).seconds,
                    restDuration = (preferences[Keys.REST_DURATION] ?: 0).seconds
                )
                _currentConfiguration.value = config
            }
        }
    }
    
    private fun loadRecentConfigurations() {
        CoroutineScope(Dispatchers.IO).launch {
            _recentConfigurations.value = getRecentConfigurations()
        }
    }
    
    private suspend fun saveCurrentConfiguration(config: TimerConfiguration) {
        dataStore.edit { preferences ->
            preferences[Keys.LAPS] = config.laps
            preferences[Keys.WORK_DURATION] = config.workDuration.inWholeSeconds.toInt()
            preferences[Keys.REST_DURATION] = config.restDuration.inWholeSeconds.toInt()
        }
    }
    
    private object Keys {
        val LAPS = intPreferencesKey("laps")
        val WORK_DURATION = intPreferencesKey("work_duration")
        val REST_DURATION = intPreferencesKey("rest_duration")
    }
}
```

### SettingsRepository

```kotlin
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val _settings = MutableStateFlow(NotificationSettings())
    val settings: StateFlow<NotificationSettings> = _settings.asStateFlow()
    
    init {
        loadSettings()
    }
    
    suspend fun updateSettings(settings: NotificationSettings) {
        _settings.value = settings
        saveSettings(settings)
    }
    
    fun getSettings(): NotificationSettings {
        return _settings.value
    }
    
    private fun loadSettings() {
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect { preferences ->
                val settings = NotificationSettings(
                    vibrationEnabled = preferences[Keys.VIBRATION] ?: true,
                    soundEnabled = preferences[Keys.SOUND] ?: true,
                    flashEnabled = preferences[Keys.FLASH] ?: true,
                    autoMode = preferences[Keys.AUTO_MODE] ?: true
                )
                _settings.value = settings
            }
        }
    }
    
    private suspend fun saveSettings(settings: NotificationSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.VIBRATION] = settings.vibrationEnabled
            preferences[Keys.SOUND] = settings.soundEnabled
            preferences[Keys.FLASH] = settings.flashEnabled
            preferences[Keys.AUTO_MODE] = settings.autoMode
        }
    }
    
    private object Keys {
        val VIBRATION = booleanPreferencesKey("vibration_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val FLASH = booleanPreferencesKey("flash_enabled")
        val AUTO_MODE = booleanPreferencesKey("auto_mode")
    }
}
```

### WearOSRepository

```kotlin
@Singleton
class WearOSRepository @Inject constructor(
    private val context: Context,
    private val tileService: TileService,
    private val complicationDataSourceService: ComplicationDataSourceService
) {
    suspend fun updateTile(timerState: TimerState) {
        tileService.getUpdater().requestUpdate(TileService::class.java)
    }
    
    suspend fun updateComplications(timerState: TimerState) {
        ComplicationDataSourceUpdateRequester
            .create(context, ComponentName(context, complicationDataSourceService::class.java))
            .requestUpdateAll()
    }
    
    fun getTileData(timerState: TimerState, recentConfigs: List<TimerConfiguration>): TileData {
        return when (timerState.phase) {
            TimerPhase.STOPPED -> TileData.createStoppedTile(recentConfigs)
            else -> TileData.createRunningTile(timerState)
        }
    }
    
    fun getComplicationData(timerState: TimerState, type: ComplicationType): ComplicationData {
        return when (type) {
            ComplicationType.SHORT_TEXT -> createShortTextComplication(timerState)
            ComplicationType.LONG_TEXT -> createLongTextComplication(timerState)
            ComplicationType.RANGED_VALUE -> createRangedValueComplication(timerState)
            ComplicationType.MONOCHROMATIC_IMAGE -> createMonochromaticImageComplication(timerState)
            ComplicationType.SMALL_IMAGE -> createSmallImageComplication(timerState)
            else -> NoDataComplicationData.Builder().build()
        }
    }
}
```

## ViewModel Layer

### MainViewModel

```kotlin
@HiltViewModel
class MainViewModel @Inject constructor(
    private val timerRepository: TimerRepository,
    private val configRepository: ConfigurationRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                timerRepository.timerState,
                configRepository.currentConfiguration,
                settingsRepository.settings
            ) { timerState, config, settings ->
                MainScreenUiState(
                    timerState = timerState,
                    configuration = config,
                    settings = settings
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
    
    fun onPlayPauseClicked() {
        val currentState = _uiState.value.timerState
        viewModelScope.launch {
            when {
                currentState.phase == TimerPhase.STOPPED -> {
                    timerRepository.sendEvent(TimerEvent.Start)
                }
                currentState.isPaused -> {
                    timerRepository.sendEvent(TimerEvent.Resume)
                }
                currentState.isRunning -> {
                    timerRepository.sendEvent(TimerEvent.Pause)
                }
            }
        }
    }
    
    fun onStopClicked() {
        viewModelScope.launch {
            timerRepository.sendEvent(TimerEvent.Stop)
        }
    }
    
    fun onAlarmDismissed() {
        viewModelScope.launch {
            timerRepository.sendEvent(TimerEvent.DismissAlarm)
        }
    }
    
    fun onScreenTapped() {
        val currentState = _uiState.value.timerState
        if (currentState.isAlarmActive) {
            onAlarmDismissed()
        }
    }
}
```

### ConfigViewModel

```kotlin
@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configRepository: ConfigurationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ConfigScreenUiState())
    val uiState: StateFlow<ConfigScreenUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            configRepository.currentConfiguration.collect { config ->
                _uiState.value = _uiState.value.copy(
                    currentConfig = config
                )
            }
        }
    }
    
    fun updateLaps(laps: Int) {
        val currentConfig = _uiState.value.currentConfig
        val newConfig = currentConfig.copy(laps = laps)
        updateConfiguration(newConfig)
    }
    
    fun updateWorkDuration(duration: Duration) {
        val currentConfig = _uiState.value.currentConfig
        val newConfig = currentConfig.copy(workDuration = duration)
        updateConfiguration(newConfig)
    }
    
    fun updateRestDuration(duration: Duration) {
        val currentConfig = _uiState.value.currentConfig
        val newConfig = currentConfig.copy(restDuration = duration)
        updateConfiguration(newConfig)
    }
    
    fun resetToDefaults() {
        val defaultConfig = TimerConfiguration()
        updateConfiguration(defaultConfig)
    }
    
    fun setToCommonAlternatives() {
        val alternativeConfig = TimerConfiguration(
            laps = 999,
            workDuration = 5.minutes,
            restDuration = 5.minutes
        )
        updateConfiguration(alternativeConfig)
    }
    
    private fun updateConfiguration(config: TimerConfiguration) {
        viewModelScope.launch {
            configRepository.updateConfiguration(config)
        }
    }
}
```

### HistoryViewModel

```kotlin
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val configRepository: ConfigurationRepository,
    private val timerRepository: TimerRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryScreenUiState())
    val uiState: StateFlow<HistoryScreenUiState> = _uiState.asStateFlow()
    
    init {
        loadRecentConfigurations()
    }
    
    fun onConfigurationSelected(config: TimerConfiguration) {
        viewModelScope.launch {
            timerRepository.sendEvent(TimerEvent.UpdateConfiguration(config))
        }
    }
    
    private fun loadRecentConfigurations() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                configRepository.recentConfigurations.collect { configs ->
                    _uiState.value = _uiState.value.copy(
                        recentConfigurations = configs,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
}
```

### SettingsViewModel

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsScreenUiState())
    val uiState: StateFlow<SettingsScreenUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }
    
    fun toggleVibration() {
        val currentSettings = _uiState.value.settings
        val newSettings = currentSettings.copy(
            vibrationEnabled = !currentSettings.vibrationEnabled
        )
        updateSettings(newSettings)
    }
    
    fun toggleSound() {
        val currentSettings = _uiState.value.settings
        val newSettings = currentSettings.copy(
            soundEnabled = !currentSettings.soundEnabled
        )
        updateSettings(newSettings)
    }
    
    fun toggleFlash() {
        val currentSettings = _uiState.value.settings
        val newSettings = currentSettings.copy(
            flashEnabled = !currentSettings.flashEnabled
        )
        updateSettings(newSettings)
    }
    
    fun toggleAutoMode() {
        val currentSettings = _uiState.value.settings
        val newSettings = currentSettings.copy(
            autoMode = !currentSettings.autoMode
        )
        updateSettings(newSettings)
    }
    
    private fun updateSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            settingsRepository.updateSettings(settings)
        }
    }
}
```

## Service Layer

### TimerService (Foreground Service)

```kotlin
@AndroidEntryPoint
class TimerService : Service() {
    
    @Inject
    lateinit var settingsRepository: SettingsRepository
    
    @Inject
    lateinit var notificationManager: TimerNotificationManager
    
    @Inject
    lateinit var audioManager: TimerAudioManager
    
    @Inject
    lateinit var vibrationManager: TimerVibrationManager
    
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val binder = TimerServiceBinder()
    
    override fun onBind(intent: Intent): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val config = intent.getParcelableExtra<TimerConfiguration>(EXTRA_CONFIG)
                config?.let { startTimer(it) }
            }
            ACTION_PAUSE_TIMER -> pauseTimer()
            ACTION_RESUME_TIMER -> resumeTimer()
            ACTION_STOP_TIMER -> stopTimer()
            ACTION_DISMISS_ALARM -> dismissAlarm()
        }
        return START_STICKY
    }
    
    fun startTimer(configuration: TimerConfiguration) {
        _timerState.value = TimerState(
            totalLaps = configuration.laps,
            timeRemaining = configuration.workDuration,
            isRunning = true,
            phase = TimerPhase.RUNNING,
            sessionStartTime = System.currentTimeMillis()
        )
        
        startForeground(NOTIFICATION_ID, notificationManager.createTimerNotification(_timerState.value))
        startTimerCoroutine(configuration)
    }
    
    fun pauseTimer() {
        _timerState.value = _timerState.value.copy(isPaused = true)
        timerJob?.cancel()
        updateNotification()
    }
    
    fun resumeTimer() {
        val currentState = _timerState.value
        if (currentState.isPaused) {
            _timerState.value = currentState.copy(isPaused = false)
            startTimerCoroutine(getCurrentConfiguration())
        }
    }
    
    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun dismissAlarm() {
        val currentState = _timerState.value
        if (currentState.isAlarmActive) {
            _timerState.value = currentState.copy(
                isAlarmActive = false,
                isPaused = false
            )
            
            // Continue to next interval or complete session
            proceedToNextInterval()
        }
    }
    
    fun proceedToNextInterval() {
        val currentState = _timerState.value
        val config = getCurrentConfiguration()
        
        when {
            currentState.isResting -> {
                // End of rest, start next work interval
                if (currentState.currentLap < currentState.totalLaps || currentState.totalLaps == 999) {
                    _timerState.value = currentState.copy(
                        currentLap = if (currentState.totalLaps != 999) currentState.currentLap + 1 else currentState.currentLap + 1,
                        timeRemaining = config.workDuration,
                        isResting = false,
                        phase = TimerPhase.RUNNING
                    )
                    startTimerCoroutine(config)
                } else {
                    // Session complete
                    completeSession()
                }
            }
            else -> {
                // End of work, start rest or next work
                if (config.restDuration > Duration.ZERO) {
                    _timerState.value = currentState.copy(
                        timeRemaining = config.restDuration,
                        isResting = true,
                        phase = TimerPhase.RESTING
                    )
                    startTimerCoroutine(config)
                } else {
                    // No rest, go to next lap
                    proceedToNextInterval()
                }
            }
        }
    }
    
    fun pauseForDismissal() {
        _timerState.value = _timerState.value.copy(
            isPaused = true,
            isAlarmActive = true,
            phase = TimerPhase.ALARM_ACTIVE
        )
        triggerContinuousAlarm()
    }
    
    fun triggerVibration() {
        vibrationManager.vibrateSingle()
    }
    
    fun triggerSound() {
        audioManager.playBeep()
    }
    
    fun triggerFlash() {
        // This would be handled by the UI layer
        // Service can send broadcast to trigger flash
        sendBroadcast(Intent(ACTION_TRIGGER_FLASH))
    }
    
    private fun startTimerCoroutine(configuration: TimerConfiguration) {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (_timerState.value.isRunning && !_timerState.value.isPaused) {
                delay(1000) // Update every second
                
                val currentState = _timerState.value
                val newTimeRemaining = currentState.timeRemaining - 1.seconds
                
                if (newTimeRemaining <= Duration.ZERO) {
                    handleIntervalComplete()
                    break
                } else {
                    _timerState.value = currentState.copy(
                        timeRemaining = newTimeRemaining,
                        lastUpdateTime = System.currentTimeMillis()
                    )
                    updateNotification()
                }
            }
        }
    }
    
    private suspend fun handleIntervalComplete() {
        val settings = settingsRepository.getSettings()
        
        // Trigger notifications
        if (settings.vibrationEnabled) triggerVibration()
        if (settings.soundEnabled) triggerSound()
        if (settings.flashEnabled) triggerFlash()
        
        if (settings.autoMode) {
            delay(500) // Brief pause for notification
            proceedToNextInterval()
        } else {
            pauseForDismissal()
        }
    }
    
    private fun completeSession() {
        val settings = settingsRepository.getSettings()
        
        // Triple notification for completion
        repeat(3) {
            if (settings.vibrationEnabled) triggerVibration()
            if (settings.soundEnabled) triggerSound()
        }
        
        _timerState.value = _timerState.value.copy(
            isRunning = false,
            phase = TimerPhase.STOPPED
        )
        
        stopTimer()
    }
    
    private fun triggerContinuousAlarm() {
        // Implement continuous vibration/sound until dismissed
        CoroutineScope(Dispatchers.Main).launch {
            while (_timerState.value.isAlarmActive) {
                val settings = settingsRepository.getSettings()
                if (settings.vibrationEnabled) vibrationManager.vibrateContinuous()
                if (settings.soundEnabled) audioManager.playAlarmBeep()
                delay(1000)
            }
        }
    }
    
    private fun updateNotification() {
        notificationManager.updateNotification(_timerState.value)
    }
    
    private fun getCurrentConfiguration(): TimerConfiguration {
        // Get from repository or reconstruct from current state
        return TimerConfiguration() // Simplified
    }
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WearInterval::TimerWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes max
    }
    
    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        wakeLock?.release()
    }
    
    inner class TimerServiceBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    companion object {
        const val ACTION_START_TIMER = "START_TIMER"
        const val ACTION_PAUSE_TIMER = "PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "RESUME_TIMER"
        const val ACTION_STOP_TIMER = "STOP_TIMER"
        const val ACTION_DISMISS_ALARM = "DISMISS_ALARM"
        const val ACTION_TRIGGER_FLASH = "TRIGGER_FLASH"
        
        const val EXTRA_CONFIG = "TIMER_CONFIG"
        
        private const val NOTIFICATION_ID = 1001
    }
}
```

## Navigation and UI Integration

### Navigation Setup

```kotlin
@Composable
fun WearIntervalApp() {
    val navController = rememberSwipeDismissableNavController()
    
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            val viewModel: MainViewModel = hiltViewModel()
            MainScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onPlayPauseClick = viewModel::onPlayPauseClicked,
                onStopClick = viewModel::onStopClicked,
                onScreenTap = viewModel::onScreenTapped,
                onNavigateToConfig = {
                    navController.navigate(Screen.Config.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Config.route) {
            val viewModel: ConfigViewModel = hiltViewModel()
            ConfigScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onLapsChange = viewModel::updateLaps,
                onWorkDurationChange = viewModel::updateWorkDuration,
                onRestDurationChange = viewModel::updateRestDuration,
                onResetDefaults = viewModel::resetToDefaults,
                onSetAlternatives = viewModel::setToCommonAlternatives,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.History.route) {
            val viewModel: HistoryViewModel = hiltViewModel()
            HistoryScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onConfigurationSelect = { config ->
                    viewModel.onConfigurationSelected(config)
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                uiState = viewModel.uiState.collectAsState().value,
                onVibrationToggle = viewModel::toggleVibration,
                onSoundToggle = viewModel::toggleSound,
                onFlashToggle = viewModel::toggleFlash,
                onAutoModeToggle = viewModel::toggleAutoMode,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Config : Screen("config")
    object History : Screen("history")
    object Settings : Screen("settings")
}
```

## Dependency Injection Setup

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    abstract fun bindTimerRepository(
        timerRepositoryImpl: TimerRepositoryImpl
    ): TimerRepository
    
    @Binds
    abstract fun bindConfigRepository(
        configRepositoryImpl: ConfigurationRepositoryImpl
    ): ConfigurationRepository
    
    @Binds
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
    
    @Binds
    abstract fun bindWearOSRepository(
        wearOSRepositoryImpl: WearOSRepositoryImpl
    ): WearOSRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("timer_preferences") }
        )
    }
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TimerDatabase {
        return Room.databaseBuilder(
            context,
            TimerDatabase::class.java,
            "timer_database"
        ).build()
    }
    
    @Provides
    fun provideConfigDao(database: TimerDatabase): ConfigurationDao {
        return database.configurationDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {
    
    @Provides
    @Singleton
    fun provideTimerService(@ApplicationContext context: Context): TimerService {
        return TimerService()
    }
}
```

## Testing Strategy

### Repository Testing

```kotlin
@ExperimentalCoroutinesTest
class TimerRepositoryTest {
    
    @MockK
    private lateinit var timerService: TimerService
    
    @MockK
    private lateinit var configRepository: ConfigurationRepository
    
    @MockK
    private lateinit var settingsRepository: SettingsRepository
    
    @MockK
    private lateinit var wearOSRepository: WearOSRepository
    
    private lateinit var repository: TimerRepository
    
    private val testScope = TestScope()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        repository = TimerRepository(
            timerService,
            configRepository,
            settingsRepository,
            wearOSRepository,
            testScope
        )
    }
    
    @Test
    fun `startTimer saves configuration and starts service`() = testScope.runTest {
        // Given
        val config = TimerConfiguration()
        coEvery { configRepository.saveRecentConfiguration(any()) } just Runs
        coEvery { timerService.startTimer(any()) } just Runs
        
        // When
        repository.startTimer(config)
        
        // Then
        coVerify { configRepository.saveRecentConfiguration(config) }
        coVerify { timerService.startTimer(config) }
    }
}
```

### ViewModel Testing

```kotlin
@ExperimentalCoroutinesTest
class MainViewModelTest {
    
    @MockK
    private lateinit var timerRepository: TimerRepository
    
    @MockK
    private lateinit var configRepository: ConfigurationRepository
    
    @MockK
    private lateinit var settingsRepository: SettingsRepository
    
    private lateinit var viewModel: MainViewModel
    
    private val testScope = TestScope()
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        every { timerRepository.timerState } returns MutableStateFlow(TimerState())
        every { configRepository.currentConfiguration } returns MutableStateFlow(TimerConfiguration())
        every { settingsRepository.settings } returns MutableStateFlow(NotificationSettings())
        
        viewModel = MainViewModel(
            timerRepository,
            configRepository,
            settingsRepository
        )
    }
    
    @Test
    fun `onPlayPauseClicked starts timer when stopped`() = testScope.runTest {
        // Given
        coEvery { timerRepository.sendEvent(any()) } just Runs
        
        // When
        viewModel.onPlayPauseClicked()
        
        // Then
        coVerify { timerRepository.sendEvent(TimerEvent.Start) }
    }
}
```

## Benefits of This Architecture

### ✅ Advantages

1. **Separation of Concerns**: Each layer has a clear responsibility
2. **Testability**: Easy to unit test ViewModels and Repositories independently
3. **Scalability**: Can add new features without modifying existing code
4. **Service Integration**: Proper foreground service integration for background timers
5. **WearOS Integration**: Dedicated repository for tiles and complications
6. **Data Persistence**: Multiple data sources (DataStore, Room) properly abstracted
7. **Error Handling**: Centralized error handling in repositories
8. **Performance**: Efficient state updates through reactive streams

### ⚠️ Trade-offs

1. **Initial Complexity**: More boilerplate than simpler patterns
2. **Learning Curve**: Requires understanding of MVVM + Repository pattern
3. **More Files**: More classes and interfaces to maintain

## Migration Path

### Phase 1: Core Architecture
1. Set up dependency injection with Hilt
2. Create data models and repositories
3. Implement TimerService

### Phase 2: UI Layer
1. Create ViewModels
2. Implement screens with Compose
3. Set up navigation

### Phase 3: WearOS Integration
1. Implement tile service
2. Create complication data sources
3. Add notification management

### Phase 4: Polish
1. Add comprehensive testing
2. Performance optimization
3. Error handling improvements

This architecture provides the robust foundation needed for the complex WearInterval app while maintaining proper separation of concerns and testability.