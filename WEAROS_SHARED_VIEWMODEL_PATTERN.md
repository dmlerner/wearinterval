# Single Shared ViewModel Pattern for WearOS Multi-Screen Apps

## Overview

The Single Shared ViewModel pattern is the recommended architecture for simple to medium-complexity WearOS apps with 3-5 screens that need to share state. This pattern provides a balance between simplicity and proper state management, particularly suited for timer, fitness, or utility apps where maintaining state across navigation is critical.

## When to Use This Pattern

### ✅ Use When:
- 3-5 screens maximum
- Screens need to share significant state
- Simple business logic (no complex domain layer needed)
- Timer/session-based apps where state persistence is critical
- WearOS apps prioritizing simplicity and performance

### ❌ Don't Use When:
- Complex apps with 6+ screens
- Independent screen functionalities
- Complex business logic requiring domain layer
- Apps with multiple data sources/repositories
- Teams requiring strict separation of concerns

## Core Architecture

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│   SetupScreen   │    │   TimerScreen    │    │  ResultsScreen  │
│                 │    │                  │    │                 │
└─────────┬───────┘    └─────────┬────────┘    └─────────┬───────┘
          │                      │                       │
          └──────────────────────┼───────────────────────┘
                                 │
                    ┌────────────▼───────────┐
                    │   Shared ViewModel     │
                    │  - Timer State         │
                    │  - Navigation Logic    │
                    │  - Business Logic      │
                    └────────────────────────┘
```

## Implementation

### 1. State Definition

```kotlin
data class TimerState(
    // Timer Configuration
    val workDuration: Duration = 30.seconds,
    val restDuration: Duration = 10.seconds,
    val totalSets: Int = 5,
    val prepareTime: Duration = 5.seconds,
    
    // Current State
    val currentSet: Int = 1,
    val currentPhase: TimerPhase = TimerPhase.SETUP,
    val timeRemaining: Duration = 30.seconds,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    
    // Session Data
    val sessionStartTime: Long? = null,
    val completedSets: Int = 0,
    val totalWorkTime: Duration = Duration.ZERO,
    
    // UI State
    val showingResults: Boolean = false,
    val keepScreenOn: Boolean = false
)

enum class TimerPhase {
    SETUP,      // Initial configuration
    PREPARE,    // Get ready countdown
    WORK,       // Work interval
    REST,       // Rest interval  
    COMPLETE    // Session finished
}

sealed class TimerEvent {
    object StartTimer : TimerEvent()
    object PauseTimer : TimerEvent()
    object ResumeTimer : TimerEvent()
    object ResetTimer : TimerEvent()
    object NextPhase : TimerEvent()
    object CompleteSession : TimerEvent()
    
    data class UpdateWorkDuration(val duration: Duration) : TimerEvent()
    data class UpdateRestDuration(val duration: Duration) : TimerEvent()
    data class UpdateTotalSets(val sets: Int) : TimerEvent()
}
```

### 2. Shared ViewModel Implementation

```kotlin
@HiltViewModel
class SharedTimerViewModel @Inject constructor(
    private val preferencesDataStore: PreferencesDataStore,
    private val vibrationManager: VibrationManager
) : ViewModel() {
    
    private val _state = mutableStateOf(TimerState())
    val state: State<TimerState> = _state
    
    private var timerJob: Job? = null
    
    init {
        loadPreferences()
    }
    
    fun handleEvent(event: TimerEvent) {
        when (event) {
            is TimerEvent.StartTimer -> startTimer()
            is TimerEvent.PauseTimer -> pauseTimer()
            is TimerEvent.ResumeTimer -> resumeTimer()
            is TimerEvent.ResetTimer -> resetTimer()
            is TimerEvent.NextPhase -> nextPhase()
            is TimerEvent.CompleteSession -> completeSession()
            is TimerEvent.UpdateWorkDuration -> updateWorkDuration(event.duration)
            is TimerEvent.UpdateRestDuration -> updateRestDuration(event.duration)
            is TimerEvent.UpdateTotalSets -> updateTotalSets(event.sets)
        }
    }
    
    private fun startTimer() {
        _state.value = _state.value.copy(
            isRunning = true,
            isPaused = false,
            sessionStartTime = System.currentTimeMillis(),
            currentPhase = TimerPhase.PREPARE,
            timeRemaining = _state.value.prepareTime,
            keepScreenOn = true
        )
        startTimerCoroutine()
    }
    
    private fun startTimerCoroutine() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.isRunning && !_state.value.isPaused) {
                delay(100) // Update every 100ms for smooth UI
                
                val currentState = _state.value
                val newTimeRemaining = currentState.timeRemaining - 100.milliseconds
                
                if (newTimeRemaining <= Duration.ZERO) {
                    handlePhaseCompletion()
                } else {
                    _state.value = currentState.copy(timeRemaining = newTimeRemaining)
                }
            }
        }
    }
    
    private fun handlePhaseCompletion() {
        vibrationManager.vibrate(VibrationPattern.PHASE_COMPLETE)
        
        when (_state.value.currentPhase) {
            TimerPhase.PREPARE -> startWorkPhase()
            TimerPhase.WORK -> startRestPhase()
            TimerPhase.REST -> {
                if (_state.value.currentSet < _state.value.totalSets) {
                    nextSet()
                } else {
                    completeSession()
                }
            }
            else -> { /* No-op */ }
        }
    }
    
    private fun startWorkPhase() {
        _state.value = _state.value.copy(
            currentPhase = TimerPhase.WORK,
            timeRemaining = _state.value.workDuration
        )
    }
    
    private fun startRestPhase() {
        _state.value = _state.value.copy(
            currentPhase = TimerPhase.REST,
            timeRemaining = _state.value.restDuration,
            completedSets = _state.value.completedSets + 1,
            totalWorkTime = _state.value.totalWorkTime + _state.value.workDuration
        )
    }
    
    private fun nextSet() {
        _state.value = _state.value.copy(
            currentSet = _state.value.currentSet + 1,
            currentPhase = TimerPhase.WORK,
            timeRemaining = _state.value.workDuration
        )
    }
    
    private fun completeSession() {
        timerJob?.cancel()
        vibrationManager.vibrate(VibrationPattern.SESSION_COMPLETE)
        
        _state.value = _state.value.copy(
            isRunning = false,
            currentPhase = TimerPhase.COMPLETE,
            showingResults = true,
            keepScreenOn = false
        )
    }
    
    private fun pauseTimer() {
        _state.value = _state.value.copy(isPaused = true)
        timerJob?.cancel()
    }
    
    private fun resumeTimer() {
        _state.value = _state.value.copy(isPaused = false)
        startTimerCoroutine()
    }
    
    private fun resetTimer() {
        timerJob?.cancel()
        _state.value = TimerState(
            workDuration = _state.value.workDuration,
            restDuration = _state.value.restDuration,
            totalSets = _state.value.totalSets,
            prepareTime = _state.value.prepareTime
        )
    }
    
    // Configuration methods
    private fun updateWorkDuration(duration: Duration) {
        _state.value = _state.value.copy(workDuration = duration)
        savePreferences()
    }
    
    private fun updateRestDuration(duration: Duration) {
        _state.value = _state.value.copy(restDuration = duration)
        savePreferences()
    }
    
    private fun updateTotalSets(sets: Int) {
        _state.value = _state.value.copy(totalSets = sets)
        savePreferences()
    }
    
    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesDataStore.getTimerPreferences().collect { prefs ->
                _state.value = _state.value.copy(
                    workDuration = prefs.workDuration,
                    restDuration = prefs.restDuration,
                    totalSets = prefs.totalSets
                )
            }
        }
    }
    
    private fun savePreferences() {
        viewModelScope.launch {
            preferencesDataStore.saveTimerPreferences(
                workDuration = _state.value.workDuration,
                restDuration = _state.value.restDuration,
                totalSets = _state.value.totalSets
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
```

### 3. Navigation Setup

```kotlin
@Composable
fun TimerApp() {
    val navController = rememberSwipeDismissableNavController()
    val sharedViewModel: SharedTimerViewModel = hiltViewModel()
    
    // Keep screen on during timer
    val state by sharedViewModel.state
    if (state.keepScreenOn) {
        KeepScreenOn()
    }
    
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Screen.Setup.route
    ) {
        composable(Screen.Setup.route) {
            SetupScreen(
                state = state,
                onEvent = sharedViewModel::handleEvent,
                onNavigateToTimer = {
                    navController.navigate(Screen.Timer.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Timer.route) {
            TimerScreen(
                state = state,
                onEvent = sharedViewModel::handleEvent,
                onNavigateToResults = {
                    navController.navigate(Screen.Results.route)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Results.route) {
            ResultsScreen(
                state = state,
                onEvent = sharedViewModel::handleEvent,
                onNavigateToSetup = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(0) // Clear entire back stack
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Timer : Screen("timer")
    object Results : Screen("results")
}
```

### 4. Screen Implementations

#### Setup Screen
```kotlin
@Composable
fun SetupScreen(
    state: TimerState,
    onEvent: (TimerEvent) -> Unit,
    onNavigateToTimer: () -> Unit
) {
    ScreenScaffold(
        timeText = { TimeText() }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = rememberResponsiveColumnPadding()
        ) {
            item {
                Text(
                    text = "Interval Timer",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                DurationPicker(
                    label = "Work",
                    duration = state.workDuration,
                    onDurationChange = { onEvent(TimerEvent.UpdateWorkDuration(it)) }
                )
            }
            
            item {
                DurationPicker(
                    label = "Rest",
                    duration = state.restDuration,
                    onDurationChange = { onEvent(TimerEvent.UpdateRestDuration(it)) }
                )
            }
            
            item {
                SetsPicker(
                    sets = state.totalSets,
                    onSetsChange = { onEvent(TimerEvent.UpdateTotalSets(it)) }
                )
            }
            
            item {
                Button(
                    onClick = {
                        onEvent(TimerEvent.StartTimer)
                        onNavigateToTimer()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start")
                }
            }
        }
    }
}
```

#### Timer Screen
```kotlin
@Composable
fun TimerScreen(
    state: TimerState,
    onEvent: (TimerEvent) -> Unit,
    onNavigateToResults: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Navigate to results when session completes
    LaunchedEffect(state.currentPhase) {
        if (state.currentPhase == TimerPhase.COMPLETE) {
            onNavigateToResults()
        }
    }
    
    ScreenScaffold(
        timeText = { TimeText() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Phase indicator
            Text(
                text = when (state.currentPhase) {
                    TimerPhase.PREPARE -> "Get Ready"
                    TimerPhase.WORK -> "Work"
                    TimerPhase.REST -> "Rest"
                    else -> ""
                },
                style = MaterialTheme.typography.titleMedium,
                color = when (state.currentPhase) {
                    TimerPhase.WORK -> Color.Red
                    TimerPhase.REST -> Color.Green
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timer display
            Text(
                text = formatDuration(state.timeRemaining),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Set progress
            Text(
                text = "Set ${state.currentSet} of ${state.totalSets}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Control buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isRunning && !state.isPaused) {
                    Button(
                        onClick = { onEvent(TimerEvent.PauseTimer) }
                    ) {
                        Text("Pause")
                    }
                } else if (state.isPaused) {
                    Button(
                        onClick = { onEvent(TimerEvent.ResumeTimer) }
                    ) {
                        Text("Resume")
                    }
                }
                
                Button(
                    onClick = {
                        onEvent(TimerEvent.ResetTimer)
                        onNavigateBack()
                    }
                ) {
                    Text("Stop")
                }
            }
        }
    }
}
```

## Benefits of This Pattern

### ✅ Advantages

1. **Simple State Sharing**: No complex state synchronization between screens
2. **Single Source of Truth**: All timer state lives in one place
3. **Persistent State**: Timer continues running during navigation
4. **Minimal Boilerplate**: Less code than Repository pattern
5. **Fast Development**: Quick to implement and modify
6. **WearOS Optimized**: Matches platform expectations for simple apps
7. **Memory Efficient**: Single ViewModel instance across app lifecycle

### ⚠️ Trade-offs

1. **ViewModel Growth**: Can become large with complex features
2. **Tight Coupling**: Screens are coupled to shared state structure
3. **Testing Complexity**: Single large ViewModel harder to unit test
4. **Limited Scalability**: Doesn't scale well beyond 5-6 screens

## Best Practices

### State Management
- Keep state immutable with data classes
- Use sealed classes for events/actions
- Implement proper state validation
- Handle edge cases (low battery, phone calls)

### Performance
- Use `mutableStateOf` instead of `MutableStateFlow` for UI state
- Implement proper coroutine cancellation
- Avoid frequent state updates (batch when possible)
- Use `derivedStateOf` for computed properties

### Navigation
- Use single activity with Compose Navigation
- Implement proper back stack management
- Handle system back gestures appropriately
- Consider deep linking for complex flows

### Error Handling
```kotlin
sealed class TimerError {
    object InvalidDuration : TimerError()
    object TimerAlreadyRunning : TimerError()
    object SystemInterruption : TimerError()
}

// In ViewModel
private val _error = mutableStateOf<TimerError?>(null)
val error: State<TimerError?> = _error

private fun handleError(error: TimerError) {
    _error.value = error
    // Auto-clear after showing
    viewModelScope.launch {
        delay(3000)
        _error.value = null
    }
}
```

## Migration Considerations

### From Simple State Hoisting
1. Move `remember` state to ViewModel
2. Convert direct state access to events
3. Add proper lifecycle management

### To Repository Pattern
1. Extract data operations to Repository
2. Add dependency injection
3. Separate screen-specific ViewModels
4. Implement proper testing structure

## Conclusion

The Single Shared ViewModel pattern is ideal for WearOS apps requiring simplicity, performance, and shared state across multiple screens. It provides the right balance of architecture benefits without over-engineering, making it perfect for timer, fitness, or utility apps where user experience and development speed are priorities.

Use this pattern when you need more structure than simple state hoisting but want to avoid the complexity of full MVVM with Repository patterns. It's particularly well-suited for WearOS's constraint environment and user interaction patterns.