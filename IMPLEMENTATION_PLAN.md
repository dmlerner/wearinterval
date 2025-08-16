# WearInterval Implementation Plan

## Overview
This document provides a detailed, phase-by-phase implementation plan for WearInterval, following the architecture specification and development practices. Each phase builds upon the previous, ensuring a solid foundation with comprehensive testing throughout.

## Implementation Strategy

### Core Principles
- **Test-Driven Development**: Write tests first, then implement
- **Continuous Coverage**: Maintain 90%+ test coverage at each phase
- **Incremental Validation**: Each phase produces working, testable functionality
- **Bottom-Up Architecture**: Build from data layer up to UI layer

### Phase Dependencies
```
Phase 1 (Foundation) 
    ↓
Phase 2 (Data Layer)
    ↓  
Phase 3 (Domain Layer)
    ↓
Phase 4 (UI Layer)
    ↓
Phase 5 (Wear OS Integration)
    ↓
Phase 6 (Polish & Optimization)
```

---

## Phase 1: Foundation & Project Setup
**Goal**: Establish project infrastructure and core models

### 1.1 Project Initialization
**Tasks:**
- [ ] Create new Wear OS project with Compose
- [ ] Set `minSdk = 30` (Wear OS 3.0), `compileSdk = 34`
- [ ] Configure `build.gradle.kts` with all dependencies
- [ ] Set up Git repository with `.gitignore`
- [ ] Configure JaCoCo for test coverage

**Dependencies to Add:**
```kotlin
// Wear OS & Compose
implementation("androidx.wear.compose:compose-material:1.2.1")
implementation("androidx.wear.compose:compose-foundation:1.2.1")
implementation("androidx.wear.compose:compose-navigation:1.2.1")

// Architecture & DI
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-compiler:2.48")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Data & Persistence
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
kapt("androidx.room:room-compiler:2.6.1")
implementation("androidx.datastore:datastore-preferences:1.0.0")

// Coroutines & Flow
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("com.google.truth:truth:1.1.5")
androidTestImplementation("androidx.room:room-testing:2.6.1")
```

**Verification:**
- [ ] Project builds successfully
- [ ] JaCoCo coverage report generates
- [ ] Hilt compilation works

### 1.2 Package Structure Creation
**Tasks:**
- [ ] Create complete package hierarchy per development practices
- [ ] Add placeholder files with TODO comments
- [ ] Set up test directory structure

**Package Structure:**
```
com.wearinterval/
├── WearIntervalApplication.kt
├── MainActivity.kt
├── di/
│   ├── DataModule.kt
│   └── RepositoryModule.kt
├── data/
│   ├── database/
│   ├── datastore/
│   ├── service/
│   └── repository/
├── domain/
│   ├── model/
│   └── repository/
├── ui/
│   ├── screen/
│   │   ├── main/
│   │   ├── config/
│   │   ├── history/
│   │   └── settings/
│   ├── component/
│   ├── theme/
│   └── navigation/
├── wearos/
│   ├── tile/
│   ├── complication/
│   └── notification/
└── util/
```

### 1.3 Core Models Definition
**Tasks:**
- [ ] Create `TimerState` with `TimerPhase` enum
- [ ] Create `TimerConfiguration` data class
- [ ] Create `NotificationSettings` data class
- [ ] Create `UiEvent` sealed class hierarchy
- [ ] Write unit tests for all data model methods

**Models to Implement:**

**TimerState.kt:**
```kotlin
data class TimerState(
    val phase: TimerPhase,
    val timeRemaining: Duration,
    val currentLap: Int,
    val totalLaps: Int,
    val isPaused: Boolean = false,
    val configuration: TimerConfiguration
) {
    val isRunning: Boolean get() = phase == TimerPhase.RUNNING
    val isResting: Boolean get() = phase == TimerPhase.RESTING
    val progressPercentage: Float get() = /* calculation */
    
    companion object {
        val STOPPED = TimerState(/* default values */)
    }
}

sealed class TimerPhase {
    object Stopped : TimerPhase()
    object Running : TimerPhase()
    object Resting : TimerPhase()
    object Paused : TimerPhase()
    object AlarmActive : TimerPhase()
}
```

**TimerConfiguration.kt:**
```kotlin
data class TimerConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val laps: Int,
    val workDuration: Duration,
    val restDuration: Duration,
    val lastUsed: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean = /* validation logic */
    fun displayString(): String = /* formatting logic */
    
    companion object {
        val DEFAULT = TimerConfiguration(
            laps = 1,
            workDuration = 60.seconds,
            restDuration = 0.seconds
        )
    }
}
```

**Testing Requirements:**
- [ ] Test all computed properties
- [ ] Test validation logic
- [ ] Test display formatting
- [ ] Achieve 100% coverage on data models

---

## Phase 2: Data Layer Implementation
**Goal**: Implement robust data persistence and service foundation

### 2.1 Room Database Setup
**Tasks:**
- [ ] Create `TimerConfigurationEntity`
- [ ] Implement `ConfigurationDao` with all CRUD operations
- [ ] Create `AppDatabase` with TypeConverters
- [ ] Write comprehensive DAO integration tests
- [ ] Set up database module in Hilt

**ConfigurationDao.kt:**
```kotlin
@Dao
interface ConfigurationDao {
    @Query("SELECT * FROM timer_configurations ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun getRecentConfigurations(limit: Int): List<TimerConfigurationEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfiguration(config: TimerConfigurationEntity)
    
    @Query("DELETE FROM timer_configurations WHERE id = :id")
    suspend fun deleteConfiguration(id: String)
    
    @Query("UPDATE timer_configurations SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long)
}
```

**Testing Requirements:**
- [ ] Test all DAO operations
- [ ] Test query ordering and limits
- [ ] Test conflict resolution
- [ ] Verify TypeConverter functionality
- [ ] 100% DAO test coverage

### 2.2 DataStore Implementation
**Tasks:**
- [ ] Create `PreferencesDataStore` wrapper
- [ ] Implement settings persistence
- [ ] Implement current configuration persistence
- [ ] Create type-safe preference keys
- [ ] Write DataStore unit tests

**DataStoreManager.kt:**
```kotlin
@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")
    
    val notificationSettings: Flow<NotificationSettings> = /* implementation */
    val currentConfiguration: Flow<TimerConfiguration> = /* implementation */
    
    suspend fun updateNotificationSettings(settings: NotificationSettings)
    suspend fun updateCurrentConfiguration(config: TimerConfiguration)
}
```

### 2.3 TimerService Foundation
**Tasks:**
- [ ] Create service with proper lifecycle
- [ ] Implement binder interface
- [ ] Set up coroutine scope and state management
- [ ] Add foreground service notification basics
- [ ] Create service connection management
- [ ] Write service lifecycle tests

**TimerService.kt Structure:**
```kotlin
class TimerService : Service() {
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private val _timerState = MutableStateFlow(TimerState.STOPPED)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    override fun onBind(intent: Intent): IBinder = binder
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    // Timer logic placeholder methods
    fun startTimer(config: TimerConfiguration) { /* implementation */ }
    fun pauseTimer() { /* implementation */ }
    fun resumeTimer() { /* implementation */ }
    fun stopTimer() { /* implementation */ }
}
```

**Verification Criteria:**
- [ ] Service binds/unbinds correctly
- [ ] StateFlow emits state changes
- [ ] Foreground notification appears
- [ ] Service survives app backgrounding

---

## Phase 3: Domain Layer (Repositories)
**Goal**: Implement business logic layer with comprehensive testing

### 3.1 SettingsRepository Implementation
**Tasks:**
- [ ] Create repository interface
- [ ] Implement concrete repository
- [ ] Write comprehensive unit tests
- [ ] Test error handling scenarios
- [ ] Verify Hilt integration

**SettingsRepository.kt:**
```kotlin
interface SettingsRepository {
    val notificationSettings: StateFlow<NotificationSettings>
    suspend fun updateSettings(settings: NotificationSettings): Result<Unit>
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : SettingsRepository {
    // Implementation with proper error handling
}
```

**Testing Strategy:**
```kotlin
class SettingsRepositoryTest {
    @Test
    fun `updateSettings persists changes and emits new state`()
    
    @Test
    fun `repository handles DataStore errors gracefully`()
    
    @Test
    fun `default settings loaded on first access`()
}
```

### 3.2 ConfigurationRepository Implementation
**Tasks:**
- [ ] Implement full repository with history management
- [ ] Add configuration validation
- [ ] Implement recent configurations caching
- [ ] Write unit tests for all scenarios
- [ ] Test concurrent access patterns

**ConfigurationRepository.kt:**
```kotlin
interface ConfigurationRepository {
    val currentConfiguration: StateFlow<TimerConfiguration>
    val recentConfigurations: StateFlow<List<TimerConfiguration>>
    
    suspend fun updateConfiguration(config: TimerConfiguration): Result<Unit>
    suspend fun selectRecentConfiguration(config: TimerConfiguration): Result<Unit>
    suspend fun deleteConfiguration(configId: String): Result<Unit>
}
```

### 3.3 TimerRepository Implementation
**Tasks:**
- [ ] Implement service binding logic
- [ ] Create timer state management
- [ ] Add proper error handling for service failures
- [ ] Write unit tests with mock service
- [ ] Test service lifecycle edge cases

**TimerRepository.kt:**
```kotlin
interface TimerRepository {
    val timerState: StateFlow<TimerState>
    val isServiceBound: StateFlow<Boolean>
    
    suspend fun startTimer(): Result<Unit>
    suspend fun pauseTimer(): Result<Unit>
    suspend fun resumeTimer(): Result<Unit>
    suspend fun stopTimer(): Result<Unit>
    suspend fun dismissAlarm(): Result<Unit>
}
```

**Testing Requirements:**
- [ ] Mock TimerService interactions
- [ ] Test service binding/unbinding
- [ ] Test all timer operations
- [ ] Test error scenarios (service unavailable)
- [ ] 100% repository test coverage

---

## Phase 4: UI Layer Implementation
**Goal**: Build complete UI with ViewModels and navigation

### 4.1 Settings Screen (Vertical Slice)
**Tasks:**
- [ ] Create SettingsScreen Composable
- [ ] Implement SettingsViewModel
- [ ] Write UI tests for settings interactions
- [ ] Test ViewModel state management
- [ ] Verify end-to-end settings flow

**SettingsScreen.kt:**
```kotlin
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    SettingsContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    // 2x2 grid of toggle buttons implementation
}
```

**Testing Coverage:**
- [ ] ViewModel state transformations
- [ ] UI event handling
- [ ] Toggle state persistence
- [ ] Navigation behavior

### 4.2 Config Screen (Picker Interface)
**Tasks:**
- [ ] Create three-column picker layout
- [ ] Implement haptic feedback
- [ ] Add gesture shortcuts (tap/long-press)
- [ ] Create ConfigViewModel with immediate updates
- [ ] Write comprehensive UI tests

**ConfigScreen Features:**
- [ ] Scrollable value pickers
- [ ] Real-time configuration updates
- [ ] Reset gestures
- [ ] Proper Wear OS spacing and sizing

### 4.3 History Screen
**Tasks:**
- [ ] Create history grid layout
- [ ] Implement configuration selection
- [ ] Add empty state handling
- [ ] Create HistoryViewModel
- [ ] Test configuration loading and selection

### 4.4 Navigation Setup
**Tasks:**
- [ ] Set up SwipeDismissableNavHost
- [ ] Implement screen transitions
- [ ] Add proper back navigation
- [ ] Test navigation flows
- [ ] Handle deep links from tiles

**Navigation.kt:**
```kotlin
@Composable
fun WearIntervalNavigation(
    navController: NavHostController
) {
    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "main"
    ) {
        composable("main") { /* MainScreen */ }
        composable("config") { /* ConfigScreen */ }
        composable("history") { /* HistoryScreen */ }
        composable("settings") { /* SettingsScreen */ }
    }
}
```

### 4.5 Main Screen Implementation
**Tasks:**
- [ ] Create dual progress ring components
- [ ] Implement timer display
- [ ] Add control buttons with proper states
- [ ] Create MainViewModel with state combining
- [ ] Write comprehensive UI tests
- [ ] Test all timer state visualizations

**MainScreen Complexity:**
- [ ] Dual progress rings (outer: laps, inner: time)
- [ ] Color changes for work/rest states
- [ ] Screen flash for notifications
- [ ] Full-screen alarm dismissal
- [ ] Real-time countdown updates

**MainViewModel Testing:**
```kotlin
class MainViewModelTest {
    @Test
    fun `ui state combines timer and configuration states correctly`()
    
    @Test
    fun `play button triggers appropriate timer action based on state`()
    
    @Test
    fun `progress calculations are accurate for all timer phases`()
}
```

---

## Phase 5: Wear OS Platform Integration
**Goal**: Complete platform integration with tiles and complications

### 5.1 WearOsRepository Implementation
**Tasks:**
- [ ] Create abstraction layer for Wear OS APIs
- [ ] Implement tile data preparation
- [ ] Implement complication data preparation
- [ ] Add proper error handling
- [ ] Write unit tests for data transformation

### 5.2 Tile Service Implementation
**Tasks:**
- [ ] Create TimerTileService
- [ ] Implement stopped state (configuration grid)
- [ ] Implement running state (progress + controls)
- [ ] Add proper click handling
- [ ] Test tile updates and refresh rates

**TileService Features:**
- [ ] Dynamic layout based on timer state
- [ ] Recent configurations in stopped state
- [ ] Live progress in running state
- [ ] Launch app with correct configuration

### 5.3 Complication Service Implementation
**Tasks:**
- [ ] Create TimerComplicationService
- [ ] Implement all complication types:
  - [ ] Short text with lap indicator
  - [ ] Long text with full status
  - [ ] Ranged value with progress
  - [ ] Monochromatic/Small image icons
- [ ] Test complication updates
- [ ] Verify proper data formatting

### 5.4 Notification System
**Tasks:**
- [ ] Implement foreground service notification
- [ ] Add notification actions (play/pause/stop)
- [ ] Create alert notification system
- [ ] Implement vibration patterns
- [ ] Add audio feedback system
- [ ] Test notification behavior

---

## Phase 6: Timer Logic & Polish
**Goal**: Complete timer functionality and final optimization

### 6.1 Complete Timer Logic
**Tasks:**
- [ ] Implement countdown mechanics in TimerService
- [ ] Add work/rest interval transitions
- [ ] Implement auto vs manual mode logic
- [ ] Add alarm state management
- [ ] Implement wake lock management
- [ ] Add comprehensive timer logic tests

**Timer Logic Components:**
```kotlin
class TimerService {
    private fun startCountdown() { /* Implementation */ }
    private fun handleIntervalComplete() { /* Auto/Manual logic */ }
    private fun triggerAlarm() { /* Alarm state management */ }
    private fun transitionToNextInterval() { /* Work/Rest transitions */ }
}
```

### 6.2 Comprehensive UI Testing
**Tasks:**
- [ ] Write end-to-end user flow tests
- [ ] Test complete timer sessions
- [ ] Test configuration persistence
- [ ] Test notification interactions
- [ ] Test tile/complication updates
- [ ] Performance testing on device

**Critical Test Scenarios:**
- [ ] Complete workout session (auto mode)
- [ ] Manual mode with alarm dismissal
- [ ] App backgrounding during timer
- [ ] Configuration changes and persistence
- [ ] Tile interactions and app launching

### 6.3 Performance Optimization
**Tasks:**
- [ ] Profile recomposition performance
- [ ] Optimize StateFlow usage
- [ ] Test battery usage patterns
- [ ] Optimize database queries
- [ ] Test memory usage and leaks

### 6.4 Coverage Verification & Final Polish
**Tasks:**
- [ ] Verify 90%+ test coverage across all modules
- [ ] Run full test suite on device
- [ ] Performance profiling
- [ ] Final code review and cleanup
- [ ] Documentation completion

---

## Success Metrics

### Phase Completion Criteria
Each phase must meet these criteria before proceeding:

1. **All planned features implemented and working**
2. **Test coverage ≥ 90% for new code**
3. **All tests passing (unit + integration + UI)**
4. **No critical code quality issues**
5. **Manual testing completed on device/emulator**

### Final Acceptance Criteria
- [ ] Complete timer functionality working end-to-end
- [ ] All UI screens functional with proper navigation
- [ ] Tiles and complications updating correctly
- [ ] Comprehensive test coverage (≥90%)
- [ ] Performance benchmarks met
- [ ] No memory leaks or battery drain issues
- [ ] Code follows development practices guidelines

## Risk Mitigation

### Technical Risks
- **Service binding complexity**: Mitigate with comprehensive testing
- **State synchronization**: Use single source of truth pattern strictly
- **Wear OS API changes**: Abstract platform APIs through repositories
- **Performance issues**: Regular profiling and optimization

### Schedule Risks
- **Complex UI requirements**: Start with MVP, iterate
- **Testing overhead**: Parallel test writing with implementation
- **Integration challenges**: Early proof-of-concept for critical paths

## Tools & Infrastructure

### Development Tools
- **IDE**: Android Studio with Wear OS emulator
- **Testing**: JUnit5, MockK, Turbine, Compose Testing
- **Coverage**: JaCoCo with HTML reports
- **Profiling**: Android Profiler for performance
- **CI/CD**: GitHub Actions for automated testing

### Quality Gates
- **Pre-commit**: Lint, unit tests, coverage check
- **PR Requirements**: All tests pass, coverage maintained
- **Release**: Full test suite, performance validation

This implementation plan provides a structured approach to building WearInterval with high quality, comprehensive testing, and proper separation of concerns throughout the development process.