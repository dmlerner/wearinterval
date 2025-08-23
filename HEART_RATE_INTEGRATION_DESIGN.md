# Heart Rate Integration Design Document
## WearInterval - Wear OS Interval Timer Application

### Document Overview

**Version**: 1.1  
**Author**: Claude  
**Date**: 2025-08-23  
**Status**: Phase 1-3 Complete - MVP Implemented

This document outlines the design for integrating real-time heart rate monitoring into the WearInterval main screen, positioning the heart rate display below the stop/play buttons using Android Health Services API.

### Implementation Status

**✅ COMPLETED (MVP)**: Phases 1-3 have been fully implemented with simulated heart rate data for development and testing. The integration includes:
- Complete architectural foundation with HeartRateRepository and HealthServicesManager
- Full UI integration with HeartRateDisplay composable positioned below timer controls
- Automatic heart rate monitoring lifecycle tied to timer state
- Permission handling infrastructure
- Custom heart icon and proper Material Design theming

**🔄 IN PROGRESS**: Health Services API research and actual sensor integration (Phase 4)

**📋 PENDING**: Unit tests, integration tests, and performance optimization (Phase 4)

---

## 1. Executive Summary

### 1.1 Objective
Add real-time heart rate monitoring to the main timer screen in WearInterval, displaying current BPM readings below the existing control buttons. This enhancement provides users with valuable biometric feedback during their workout intervals without disrupting the established user interface design.

### 1.2 Key Requirements
- **Real-time Display**: Show current heart rate in BPM below stop/play buttons
- **Non-Intrusive**: Maintain existing UI layout and timer functionality
- **Battery Efficient**: Use Health Services API for optimized sensor access
- **Permission Compliant**: Handle BODY_SENSORS permission appropriately
- **Graceful Degradation**: Function normally when heart rate sensor unavailable

### 1.3 Success Criteria
- Heart rate updates within 2-3 seconds of measurement
- Zero impact on existing timer functionality
- Consistent with current Material Design theme
- Maintains established 90%+ test coverage standard
- Battery consumption increase <5% during active monitoring

---

## 2. Current State Analysis

### 2.1 Existing UI Layout
Based on analysis of `MainScreen.kt:301-389`, the current layout within the dual progress rings contains:

```
┌─────────────────┐
│   Current Time  │
│                 │
│  Lap | Duration│ Total
│                 │
│   [Stop] [Play] │  ← Control buttons at line 314-388
└─────────────────┘
```

**Current Control Button Implementation:**
- Located at `MainScreen.kt:311-389` in `TimerControlsInside` composable
- Horizontal arrangement with `CONTROL_BUTTONS_SPACING` (16.dp)
- Stop button: 40dp size, positioned first (left)
- Play/Pause button: 48dp size, positioned second (right)
- Both buttons have proper semantic labels and haptic feedback

### 2.2 Architecture Foundation
**MVVM + Repository Pattern** with:
- UI Layer: `MainScreen.kt`, `MainViewModel.kt`, `MainContract.kt`
- Domain Layer: Repository interfaces for timer and configuration
- Data Layer: Service implementations, Room database, DataStore

**State Management:**
- StateFlow-based reactive UI updates
- Immutable state classes with copy() operations
- Event-driven architecture with sealed class events

---

## 3. Heart Rate Integration Architecture

### 3.1 Component Overview

```
┌─────────────────────────────────────────────┐
│               UI Layer                      │
├─────────────────────────────────────────────┤
│ MainScreen.kt                               │
│ ├── HeartRateDisplay (NEW)                  │
│ ├── TimerDisplay (MODIFIED)                 │
│ └── TimerControlsInside (MODIFIED)          │
├─────────────────────────────────────────────┤
│ MainContract.kt (MODIFIED)                  │
│ ├── MainUiState + heartRate: Int?           │
│ └── MainEvent + HeartRatePermissionResult   │
├─────────────────────────────────────────────┤
│ MainViewModel.kt (MODIFIED)                 │
│ └── HeartRateRepository integration         │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│              Domain Layer                   │
├─────────────────────────────────────────────┤
│ HeartRateRepository (NEW)                   │
│ └── interface for heart rate operations     │
└─────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────┐
│               Data Layer                    │
├─────────────────────────────────────────────┤
│ HeartRateRepositoryImpl (NEW)               │
│ └── Health Services integration             │
├─────────────────────────────────────────────┤
│ HealthServicesManager (NEW)                 │
│ └── Wrap Health Services API                │
└─────────────────────────────────────────────┘
```

### 3.2 Data Flow Design

```
Health Services API
    ↓ (DataType.HEART_RATE_BPM)
HealthServicesManager
    ↓ (StateFlow<HeartRateState>)
HeartRateRepositoryImpl
    ↓ (StateFlow<Int?>)
MainViewModel
    ↓ (combine with timer state)
MainUiState.heartRate
    ↓ (collectAsStateWithLifecycle)
HeartRateDisplay Composable
```

---

## 4. Technical Implementation Design

### 4.1 New Domain Models

```kotlin
// domain/model/HeartRateState.kt
sealed class HeartRateState {
    object Unavailable : HeartRateState()
    object PermissionRequired : HeartRateState()
    object Connecting : HeartRateState()
    data class Connected(val bpm: Int) : HeartRateState()
    data class Error(val message: String) : HeartRateState()
}
```

### 4.2 Repository Interface

```kotlin
// domain/repository/HeartRateRepository.kt
interface HeartRateRepository {
    val heartRateState: StateFlow<HeartRateState>
    val isAvailable: StateFlow<Boolean>
    
    suspend fun startMonitoring(): Result<Unit>
    suspend fun stopMonitoring(): Result<Unit>
    suspend fun checkPermission(): Boolean
}
```

### 4.3 Health Services Integration

```kotlin
// data/health/HealthServicesManager.kt
@Singleton
class HealthServicesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val measureClient = HealthServices.getClient(context).measureClient
    
    suspend fun hasHeartRateCapability(): Boolean = runCatching {
        val capabilities = measureClient.getCapabilities()
        DataType.HEART_RATE_BPM in capabilities.supportedDataTypesMeasure
    }.getOrDefault(false)
    
    fun heartRateMeasureFlow(): Flow<MeasureMessage> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onDataReceived(data: DataPointContainer) {
                val heartRateData = data.getData(DataType.HEART_RATE_BPM)
                heartRateData.forEach { dataPoint ->
                    val bpm = dataPoint.value.asDouble().toInt()
                    trySend(MeasureMessage.MeasureData(bpm))
                }
            }
            
            override fun onAvailabilityChanged(
                dataType: DataType<*, *>,
                availability: Availability
            ) {
                when (availability) {
                    is DataTypeAvailability.Available -> 
                        trySend(MeasureMessage.Available)
                    is DataTypeAvailability.AcquiringFix -> 
                        trySend(MeasureMessage.AcquiringFix)
                    else -> trySend(MeasureMessage.Unavailable)
                }
            }
        }
        
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)
        
        awaitClose {
            runBlocking {
                measureClient.unregisterMeasureCallback(
                    DataType.HEART_RATE_BPM, 
                    callback
                )
            }
        }
    }.flowOn(Dispatchers.IO)
}

sealed class MeasureMessage {
    data class MeasureData(val bpm: Int) : MeasureMessage()
    object Available : MeasureMessage()
    object AcquiringFix : MeasureMessage()
    object Unavailable : MeasureMessage()
}
```

### 4.4 Repository Implementation

```kotlin
// data/repository/HeartRateRepositoryImpl.kt
@Singleton
class HeartRateRepositoryImpl @Inject constructor(
    private val healthServicesManager: HealthServicesManager,
    private val permissionManager: PermissionManager,
    @ApplicationContext private val context: Context
) : HeartRateRepository {
    
    private val repositoryScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    
    private val _heartRateState = MutableStateFlow<HeartRateState>(
        HeartRateState.Unavailable
    )
    override val heartRateState: StateFlow<HeartRateState> = 
        _heartRateState.asStateFlow()
    
    override val isAvailable: StateFlow<Boolean> = heartRateState
        .map { it is HeartRateState.Connected }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )
    
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
        
        healthServicesManager.heartRateMeasureFlow()
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
                    else -> { /* Handle other states */ }
                }
            }
            .catch { throwable ->
                _heartRateState.value = HeartRateState.Error(
                    throwable.message ?: "Unknown error"
                )
            }
            .launchIn(repositoryScope)
    }
    
    override suspend fun stopMonitoring(): Result<Unit> = runCatching {
        repositoryScope.coroutineContext.cancelChildren()
        _heartRateState.value = HeartRateState.Unavailable
    }
    
    override suspend fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
```

### 4.5 UI State Integration

```kotlin
// ui/screen/main/MainContract.kt (MODIFIED)
data class MainUiState(
    // ... existing properties
    val heartRateState: HeartRateState = HeartRateState.Unavailable,
) {
    // ... existing computed properties
    
    val heartRateBpm: Int?
        get() = (heartRateState as? HeartRateState.Connected)?.bpm
    
    val showHeartRate: Boolean
        get() = heartRateState !is HeartRateState.Unavailable
}

// Add new event for permission handling
sealed class MainEvent {
    // ... existing events
    data class HeartRatePermissionResult(val granted: Boolean) : MainEvent()
}
```

### 4.6 ViewModel Integration

```kotlin
// ui/screen/main/MainViewModel.kt (MODIFIED)
@HiltViewModel
class MainViewModel @Inject constructor(
    // ... existing dependencies
    private val heartRateRepository: HeartRateRepository
) : ViewModel() {

    override val uiState: StateFlow<MainUiState> = combine(
        timerRepository.timerState,
        configurationRepository.currentConfiguration,
        heartRateRepository.heartRateState, // NEW
        // ... other flows
    ) { timerState, config, heartRateState, /* ... */ ->
        MainUiState(
            // ... existing state mapping
            heartRateState = heartRateState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = MainUiState()
    )
    
    init {
        // Start heart rate monitoring when timer starts
        viewModelScope.launch {
            timerRepository.timerState.collect { timerState ->
                when (timerState.phase) {
                    TimerPhase.Running, TimerPhase.Resting -> {
                        heartRateRepository.startMonitoring()
                    }
                    TimerPhase.Stopped -> {
                        heartRateRepository.stopMonitoring()
                    }
                    else -> { /* No change needed */ }
                }
            }
        }
    }
    
    override fun onEvent(event: MainEvent) {
        when (event) {
            // ... existing event handling
            is MainEvent.HeartRatePermissionResult -> {
                if (event.granted) {
                    viewModelScope.launch {
                        heartRateRepository.startMonitoring()
                    }
                }
            }
        }
    }
}
```

### 4.7 UI Component Design

```kotlin
// ui/screen/main/MainScreen.kt (MODIFIED)

// Add to MainScreenDefaults object
private object MainScreenDefaults {
    // ... existing constants
    val HEART_RATE_SPACING = 8.dp
    val HEART_RATE_TOP_SPACING = 12.dp
}

// Modify TimerDisplay composable to include heart rate
@Composable
private fun TimerDisplay(uiState: MainUiState, onEvent: (MainEvent) -> Unit) {
    // ... existing implementation until line ~300
    
    // Control buttons inside the circle
    TimerControlsInside(
        uiState = uiState,
        onEvent = onEvent,
    )
    
    // Heart rate display below controls (NEW)
    if (uiState.showHeartRate) {
        Spacer(modifier = Modifier.height(MainScreenDefaults.HEART_RATE_TOP_SPACING))
        HeartRateDisplay(
            heartRateState = uiState.heartRateState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// New heart rate display component
@Composable
private fun HeartRateDisplay(
    heartRateState: HeartRateState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Heart icon
        Icon(
            painter = painterResource(id = R.drawable.ic_heart),
            contentDescription = "Heart rate",
            tint = MaterialTheme.colors.error,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(MainScreenDefaults.HEART_RATE_SPACING))
        
        // BPM text
        Text(
            text = when (heartRateState) {
                is HeartRateState.Connected -> "${heartRateState.bpm} BPM"
                is HeartRateState.Connecting -> "..."
                is HeartRateState.PermissionRequired -> "Permission needed"
                is HeartRateState.Error -> "Error"
                else -> "--"
            },
            style = MaterialTheme.typography.caption1,
            color = when (heartRateState) {
                is HeartRateState.Connected -> MaterialTheme.colors.onSurface
                is HeartRateState.Error -> MaterialTheme.colors.error
                else -> MaterialTheme.colors.onSurfaceVariant
            },
            textAlign = TextAlign.Center
        )
    }
}
```

---

## 5. Permissions & Security

### 5.1 Required Permissions

**Add to AndroidManifest.xml:**
```xml
<!-- Heart rate sensor access -->
<uses-permission android:name="android.permission.BODY_SENSORS" />
```

**Permission Request Flow:**
1. Check permission when heart rate monitoring starts
2. If denied, display "Permission needed" in heart rate display
3. Do not automatically request permission - maintain non-intrusive design
4. User can grant permission through system settings if desired

### 5.2 Privacy Considerations

- **Local Processing**: All heart rate data processed locally, never transmitted
- **Temporary Storage**: No persistent storage of heart rate values
- **User Control**: Heart rate monitoring only active during timer operation
- **Graceful Degradation**: App functions normally without heart rate access

---

## 6. Performance & Resource Management

### 6.1 Battery Optimization

**Health Services Efficiency:**
- Use Health Services API for optimized sensor configuration
- Automatically manages sensor sampling rates
- Power-efficient compared to direct sensor access

**Lifecycle Management:**
- Start monitoring only when timer is running/resting
- Stop monitoring immediately when timer stops
- Automatic cleanup on app backgrounding

### 6.2 Memory Management

- StateFlow-based reactive updates (no memory leaks)
- Proper coroutine scoping with viewModelScope
- Health Services callback cleanup in repository

### 6.3 Update Frequency

- Health Services provides measurements every 1-3 seconds
- UI updates via StateFlow (no forced refreshes)
- No impact on existing 100ms timer precision

---

## 7. Testing Strategy

### 7.1 Unit Tests

**HeartRateRepositoryImpl Tests:**
```kotlin
@Test
fun `when heart rate capability available and permission granted, should start monitoring`()

@Test
fun `when permission denied, should emit PermissionRequired state`()

@Test
fun `when health services unavailable, should emit Unavailable state`()
```

**MainViewModel Tests:**
```kotlin
@Test
fun `when timer starts, should start heart rate monitoring`()

@Test
fun `when timer stops, should stop heart rate monitoring`()

@Test
fun `heart rate state should be included in ui state`()
```

### 7.2 Integration Tests

**Health Services Integration:**
- Mock HealthServicesManager for predictable testing
- Test callback registration/unregistration
- Verify proper state transitions

### 7.3 UI Tests

**HeartRateDisplay Tests:**
```kotlin
@Test
fun `heart rate display shows BPM when connected`()

@Test
fun `heart rate display shows permission message when required`()

@Test
fun `heart rate display not visible when unavailable`()
```

### 7.4 Device Testing

- Test on devices with/without heart rate sensors
- Verify permission flow on different Wear OS versions
- Battery usage monitoring during extended sessions

---

## 8. Dependencies & Build Configuration

### 8.1 Gradle Dependencies

**Add to app/build.gradle:**
```kotlin
dependencies {
    // Health Services API
    implementation 'androidx.health:health-services-client:1.0.0-beta03'
    
    // Existing dependencies remain unchanged
}
```

### 8.2 Manifest Updates

**AndroidManifest.xml additions:**
```xml
<!-- Feature declaration -->
<uses-feature
    android:name="android.hardware.sensor.heartrate"
    android:required="false" />

<!-- Permission -->
<uses-permission android:name="android.permission.BODY_SENSORS" />
```

### 8.3 Dependency Injection

**Add to DataModule:**
```kotlin
@Provides
@Singleton
fun provideHealthServicesManager(
    @ApplicationContext context: Context
): HealthServicesManager = HealthServicesManager(context)
```

**Add to RepositoryModule:**
```kotlin
@Binds
abstract fun bindHeartRateRepository(
    heartRateRepositoryImpl: HeartRateRepositoryImpl
): HeartRateRepository
```

---

## 9. Implementation Phases

### 9.1 Phase 1: Core Infrastructure
1. Create HeartRateState and repository interface
2. Implement HealthServicesManager
3. Create HeartRateRepositoryImpl with basic functionality
4. Add dependency injection setup

### 9.2 Phase 2: ViewModel Integration
1. Modify MainUiState to include heart rate state
2. Update MainViewModel to integrate heart rate repository
3. Implement automatic start/stop based on timer state
4. Add permission handling logic

### 9.3 Phase 3: UI Implementation
1. Create HeartRateDisplay composable
2. Modify TimerDisplay to include heart rate below controls
3. Add heart rate icon resource
4. Implement state-based display logic

### 9.4 Phase 4: Testing & Polish
1. Write comprehensive unit tests
2. Create integration tests with mock Health Services
3. Add UI tests for heart rate display
4. Performance testing and optimization

### 9.5 Phase 5: Documentation & Release
1. Update README with heart rate feature description
2. Add permission documentation
3. Create user guide for heart rate functionality
4. Final device testing across Wear OS versions

---

## 10. Risk Assessment & Mitigation

### 10.1 Technical Risks

**Risk**: Health Services API compatibility issues
- **Mitigation**: Extensive device testing, graceful fallback to unavailable state

**Risk**: Battery drain from continuous monitoring
- **Mitigation**: Use Health Services optimization, monitor only during active timer

**Risk**: Permission denial by user
- **Mitigation**: Non-intrusive design, app functions normally without permission

### 10.2 User Experience Risks

**Risk**: UI cluttering with heart rate information
- **Mitigation**: Minimal design, only show when relevant, proper spacing

**Risk**: Delayed heart rate readings confusing users
- **Mitigation**: Clear status indicators (connecting, error states)

### 10.3 Maintenance Risks

**Risk**: Health Services API changes in future Android versions
- **Mitigation**: Use stable API versions, monitor Android Health updates

---

## 11. Success Metrics

### 11.1 Technical Metrics
- **Test Coverage**: Maintain >90% coverage including heart rate components
- **Performance**: <5% battery usage increase during monitoring
- **Accuracy**: Heart rate updates within 3 seconds of sensor reading
- **Reliability**: No crashes or memory leaks during extended use

### 11.2 User Experience Metrics
- **Non-Intrusion**: Existing timer functionality completely unaffected
- **Accessibility**: Proper content descriptions for heart rate display
- **Visual Integration**: Consistent with existing Material Design theme
- **Graceful Degradation**: Seamless operation on devices without heart rate sensors

### 11.3 Code Quality Metrics
- **Architecture Consistency**: Follows established MVVM + Repository pattern
- **Function Size**: All new functions <20 lines per established standard
- **Documentation**: Comprehensive KDoc for all public APIs
- **Testing**: Complete unit, integration, and UI test coverage

---

## 12. Future Enhancements

### 12.1 Potential Extensions (Out of Scope)
- Heart rate zone indicators during workouts
- Average/max heart rate tracking per session
- Heart rate-based workout intensity recommendations
- Historical heart rate data storage and analysis

### 12.2 Health Connect Integration
- Future migration path to Health Connect API
- Compatibility with broader health ecosystem
- Data sharing with other fitness applications

---

## 13. Conclusion

This design provides a comprehensive, non-intrusive integration of heart rate monitoring into WearInterval's main screen. By leveraging the Health Services API and following the established architectural patterns, the implementation will provide valuable biometric feedback while maintaining the app's existing functionality, performance characteristics, and high code quality standards.

The phased implementation approach ensures systematic development with comprehensive testing at each stage, ultimately delivering a feature that enhances the user experience without compromising the robust timer functionality that defines WearInterval.