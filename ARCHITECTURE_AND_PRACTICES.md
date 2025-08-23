# WearInterval - Architecture & Best Practices

## Architectural Overview

WearInterval implements a production-grade **MVVM + Repository Pattern** architecture with strict separation of concerns across three distinct layers. The architecture emphasizes **unidirectional data flow**, **dependency injection**, and **comprehensive testing** to ensure maintainability and reliability.

### Core Architectural Principles

1. **Single Source of Truth**: Each data domain has exactly one authoritative source
2. **Unidirectional Data Flow**: Data flows down, events flow up through StateFlow
3. **Immutable State**: All state-holding classes are immutable data classes
4. **Interface-Based Design**: Domain layer defines contracts implemented by data layer
5. **Constructor Dependency Injection**: All dependencies injected through constructors

## Layer Architecture

### 1. UI Layer (Presentation)
**Location**: `com.wearinterval.ui`

**Responsibilities**:
- Compose UI components and screens
- ViewModels for state management and business logic
- Navigation coordination
- User interaction handling

**Key Components**:
```kotlin
// ViewModels follow consistent pattern
@HiltViewModel
class MainViewModel @Inject constructor(
  private val timerRepository: TimerRepository,
  private val configurationRepository: ConfigurationRepository,
  // ... other dependencies
) : ViewModel() {
  
  val uiState: StateFlow<MainUiState> = combine(
    timerRepository.timerState,
    configurationRepository.currentConfiguration,
    // ... other flows
  ) { /* state transformation */ }
    .stateIn(scope = viewModelScope, started = SharingStarted.Eagerly)
}
```

**State Management Pattern**:
- **StateFlow** for reactive UI updates
- **combine()** operations for complex state derivation
- **Immutable state classes** with copy() for updates
- **Event-driven architecture** with sealed class events

### 2. Domain Layer (Business Logic)
**Location**: `com.wearinterval.domain`

**Responsibilities**:
- Business models and validation logic
- Repository interfaces (contracts)
- Use case implementations
- Domain-specific rules and calculations

**Key Interfaces**:
```kotlin
interface TimerRepository {
  val timerState: StateFlow<TimerState>
  val isServiceBound: StateFlow<Boolean>
  
  suspend fun startTimer(): Result<Unit>
  suspend fun pauseTimer(): Result<Unit>
  // ... other timer operations
}
```

**Data Models**:
- **TimerConfiguration**: Core timer settings with validation
- **TimerState**: Current timer execution state
- **NotificationSettings**: User notification preferences
- All models include **validation methods** and **display formatting**

### 3. Data Layer (Infrastructure)
**Location**: `com.wearinterval.data`

**Responsibilities**:
- Repository implementations
- Local persistence (Room + DataStore)
- Background services
- External integrations (Wear OS APIs)

**Repository Implementation Pattern**:
```kotlin
@Singleton
class TimerRepositoryImpl @Inject constructor(
  @ApplicationContext private val context: Context,
  private val configurationRepository: Lazy<ConfigurationRepository>,
  private val timeProvider: TimeProvider,
) : TimerRepository {
  
  // Service binding with StateFlow for reactivity
  override val timerState: StateFlow<TimerState> = 
    _isServiceBound.flatMapLatest { bound ->
      if (bound && timerService != null) {
        combine(timerService!!.timerState, configurationRepository.get().currentConfiguration)
        { serviceState, config -> /* state mapping */ }
      } else {
        configurationRepository.get().currentConfiguration.map { TimerState.stopped(it) }
      }
    }.stateIn(scope = repositoryScope, started = SharingStarted.Eagerly)
}
```

## Dependency Injection Architecture

### Hilt Configuration
**Framework**: Dagger Hilt with `@AndroidEntryPoint` and `@HiltViewModel`

**Module Structure**:
- **DataModule**: Database, system services, time provider
- **RepositoryModule**: Repository interface bindings
- **Scoping**: Singleton for repositories, ViewModelScoped for ViewModels

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
  
  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
    Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
    
  @Provides
  @Singleton
  fun provideTimeProvider(): TimeProvider = SystemTimeProvider()
}
```

### Testable Time Handling
**Pattern**: TimeProvider abstraction for deterministic testing

```kotlin
interface TimeProvider {
  fun now(): Instant
  fun currentTimeMillis(): Long = now().toEpochMilli()
}

// Production implementation
class SystemTimeProvider : TimeProvider {
  override fun now(): Instant = Instant.now()
}

// Test implementation allows controlled time
class FakeTimeProvider(private var currentTime: Instant) : TimeProvider {
  override fun now(): Instant = currentTime
  fun advanceTimeBy(duration: Duration) { /* ... */ }
}
```

## Data Flow Patterns

### 1. Timer State Management
**Pattern**: Service-Repository-ViewModel chain with StateFlow

```
TimerService (Background) 
    ↓ (StateFlow)
TimerRepositoryImpl 
    ↓ (StateFlow + combine)
MainViewModel 
    ↓ (StateFlow)
MainScreen (Compose)
```

**Key Characteristics**:
- **Reactive Updates**: All state changes propagate automatically
- **Configuration Sync**: Timer adapts to configuration changes in real-time  
- **Service Binding**: Repository manages service lifecycle and connection state
- **State Transformation**: ViewModels transform domain state to UI-specific state

### 2. Configuration Management
**Storage**: DataStore for current settings + Room for history

```kotlin
// Current configuration in DataStore
private val _currentConfiguration = MutableStateFlow(TimerConfiguration.DEFAULT)
val currentConfiguration: StateFlow<TimerConfiguration> = _currentConfiguration.asStateFlow()

// History in Room database
override val recentConfigurations: StateFlow<List<TimerConfiguration>> = 
  configurationDao.getRecentConfigurations(RECENT_COUNT)
    .map { entities -> entities.map { it.toTimerConfiguration() } }
    .stateIn(scope = repositoryScope, started = SharingStarted.WhileSubscribed())
```

### 3. Wear OS Integration Data Flow
**Pattern**: Repository abstractions for Wear OS services

```kotlin
interface WearOsRepository {
  suspend fun getTileData(): TileData
  suspend fun getComplicationData(): ComplicationData
}

// Implementation coordinates timer and configuration state
class WearOsRepositoryImpl @Inject constructor(
  private val timerRepository: TimerRepository,
  private val configurationRepository: ConfigurationRepository,
) : WearOsRepository {
  
  override suspend fun getTileData(): TileData {
    val timerState = timerRepository.timerState.first()
    val recentConfigs = configurationRepository.recentConfigurations.first()
    return TileData(timerState, recentConfigs)
  }
}
```

## Service Architecture

### TimerService Design
**Pattern**: Bound service with foreground capability

**Key Features**:
- **Foreground Service**: Ensures timer survives app backgrounding
- **Wake Lock Management**: Partial wake lock during active timers only
- **State Synchronization**: Real-time updates to repository via StateFlow
- **Configuration Sync**: Adapts to configuration changes while running

```kotlin
@AndroidEntryPoint
class TimerService : Service() {
  private val _timerState = MutableStateFlow<TimerState>(TimerState.stopped())
  val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
  
  fun syncConfiguration(config: TimerConfiguration) {
    val currentState = _timerState.value
    if (currentState.isStopped) {
      _timerState.value = TimerState.stopped(config)
    } else {
      // Rescale remaining time to maintain progress percentage
      val rescaledTime = rescaleRemainingDuration(currentState, config)
      _timerState.value = currentState.copy(configuration = config, timeRemaining = rescaledTime)
    }
  }
}
```

**Service Binding Pattern**:
- Repository manages ServiceConnection lifecycle
- Automatic reconnection on service disconnection
- Graceful handling of binding failures

## Persistence Architecture

### 1. Room Database Design
**Purpose**: Timer configuration history with automatic cleanup

```kotlin
@Entity(tableName = "timer_configurations")
data class TimerConfigurationEntity(
  @PrimaryKey val id: String,
  val laps: Int,
  val workDurationSeconds: Long,
  val restDurationSeconds: Long,
  val lastUsedEpochMilli: Long,
) {
  fun toTimerConfiguration(): TimerConfiguration = TimerConfiguration(
    id = id,
    laps = laps,
    workDuration = workDurationSeconds.seconds,
    restDuration = restDurationSeconds.seconds,
    lastUsed = Instant.ofEpochMilli(lastUsedEpochMilli)
  )
}

@Dao
interface ConfigurationDao {
  @Query("SELECT * FROM timer_configurations ORDER BY lastUsedEpochMilli DESC LIMIT :limit")
  fun getRecentConfigurations(limit: Int): Flow<List<TimerConfigurationEntity>>
  
  @Upsert
  suspend fun upsert(configuration: TimerConfigurationEntity)
}
```

### 2. DataStore Integration
**Purpose**: Current configuration and app settings

```kotlin
class DataStoreManager @Inject constructor(
  @ApplicationContext private val context: Context
) {
  private val dataStore = context.dataStore
  
  val currentConfiguration: Flow<TimerConfiguration> = dataStore.data
    .catch { emit(emptyPreferences()) }
    .map { preferences -> preferences.toTimerConfiguration() ?: TimerConfiguration.DEFAULT }
    
  suspend fun saveCurrentConfiguration(config: TimerConfiguration) {
    dataStore.edit { preferences ->
      preferences[LAPS_KEY] = config.laps
      preferences[WORK_DURATION_KEY] = config.workDuration.inWholeSeconds
      preferences[REST_DURATION_KEY] = config.restDuration.inWholeSeconds
    }
  }
}
```

## Testing Architecture

### Testing Strategy
**Coverage Target**: 90%+ across all layers with emphasis on business logic

**Test Structure**:
- **Unit Tests**: ViewModels, repositories, data models, business logic
- **Integration Tests**: Database operations, service interactions, DataStore
- **UI Tests**: Critical user flows, screen interactions, navigation
- **Robolectric Tests**: Android framework integration without emulator

### Key Testing Patterns

#### 1. ViewModel Testing
```kotlin
@Test
fun `when play button clicked in stopped state, should start timer`() = runTest {
  // Given
  val timerRepository = mockk<TimerRepository>()
  coEvery { timerRepository.startTimer() } returns Result.success(Unit)
  
  // When
  viewModel.onEvent(MainEvent.PlayPauseClicked)
  
  // Then
  coVerify { timerRepository.startTimer() }
}
```

#### 2. Repository Testing with Fakes
```kotlin
@Test
fun `timer state reflects service state when bound`() = runTest {
  // Given
  val fakeService = FakeTimerService()
  repository.bindToFakeService(fakeService)
  
  // When
  fakeService.startTimer(TimerConfiguration.DEFAULT)
  
  // Then
  repository.timerState.test {
    val state = expectMostRecentItem()
    assertThat(state.phase).isEqualTo(TimerPhase.Running)
  }
}
```

#### 3. Time-Based Testing
```kotlin
@Test  
fun `timer countdown decreases over time`() = runTest {
  // Given
  val fakeTimeProvider = FakeTimeProvider(Instant.parse("2023-01-01T12:00:00Z"))
  val service = TimerService(timeProvider = fakeTimeProvider)
  
  // When
  service.startTimer(TimerConfiguration.DEFAULT)
  fakeTimeProvider.advanceTimeBy(10.seconds)
  
  // Then
  assertThat(service.timerState.value.timeRemaining).isEqualTo(50.seconds)
}
```

## Code Quality Standards

### Code Organization
**Package Structure**:
```
com.wearinterval/
├── data/                  # Data layer implementations
│   ├── database/         # Room entities, DAOs, database
│   ├── datastore/        # DataStore managers
│   ├── repository/       # Repository implementations  
│   └── service/          # Background services
├── di/                   # Dependency injection modules
├── domain/               # Business logic and contracts
│   ├── model/           # Data models and business rules
│   ├── repository/      # Repository interfaces
│   └── usecase/         # Complex business operations
├── ui/                   # Presentation layer
│   ├── component/       # Reusable UI components
│   ├── navigation/      # Navigation setup
│   ├── screen/          # Screen-specific UI and ViewModels
│   └── theme/           # Material theme customization
├── util/                 # Utility classes and extensions
└── wearos/              # Wear OS specific integrations
    ├── complication/    # Watch face complications
    ├── notification/    # Notification management
    └── tile/            # Tile service implementation
```

### Naming Conventions
- **Classes**: PascalCase with descriptive names (`TimerConfiguration`, `MainViewModel`)
- **Functions**: camelCase with verb-noun structure (`startTimer()`, `formatDuration()`)
- **Constants**: SCREAMING_SNAKE_CASE in Constants object (`TIMER_NOTIFICATION_ID`)
- **Files**: Match class names, test files end with `Test.kt`

### Function Size Limits
**Maximum 20 lines per function** enforced through code review:
```kotlin
// Good: Focused, single responsibility
fun startTimer(config: TimerConfiguration) {
  if (_timerState.value.phase != TimerPhase.Stopped) {
    throw IllegalStateException("Timer is already running")
  }
  
  _timerState.value = TimerState(
    phase = TimerPhase.Running,
    timeRemaining = config.workDuration,
    currentLap = 1,
    totalLaps = config.laps,
    configuration = config,
    intervalStartTime = timeProvider.currentTimeMillis(),
  )
  
  timerNotificationManager.updateTimerNotification(_timerState.value)
  acquireWakeLock()
  startCountdown()
}
```

### Error Handling Patterns

#### 1. Result Types for Operations
```kotlin
// Repository operations return Result<T> for composable error handling
suspend fun startTimer(): Result<Unit> {
  return try {
    ensureServiceBound()
    val config = configurationRepository.currentConfiguration.value
    timerService?.startTimer(config)
    Result.success(Unit)
  } catch (e: Exception) {
    Result.failure(e)
  }
}
```

#### 2. StateFlow Error Handling
```kotlin
// Graceful degradation with catch operators
val timerState: StateFlow<TimerState> = serviceStateFlow
  .catch { emit(TimerState.stopped()) }
  .stateIn(scope = repositoryScope, started = SharingStarted.Eagerly)
```

## Performance Optimizations

### 1. Memory Management
- **Lifecycle-Aware Scoping**: ViewModels cleared automatically
- **StateFlow Subscriptions**: `WhileSubscribed()` for automatic cleanup
- **Service Binding**: Proper unbinding in repository cleanup
- **Wake Lock Management**: Released immediately when not needed

### 2. UI Performance
- **Lazy Composition**: `beyondViewportPageCount = 0` in HorizontalPager
- **State Derivation**: `combine()` operations minimize recomposition
- **Immutable State**: Copy operations instead of mutable state
- **Key-based Lists**: Proper keys for LazyColumn/Grid items

### 3. Background Efficiency
- **Foreground Service**: Only when timer is actually running
- **Update Intervals**: 100ms for smooth progress, 30s for tile updates
- **Coroutine Scoping**: SupervisorJob for independent coroutine failure
- **Database Queries**: Flow-based with automatic updates

## Security Considerations

### 1. Service Security
- **Exported Services**: Only tile and complication services exported
- **Permissions**: Minimal required permissions (WAKE_LOCK, VIBRATE, POST_NOTIFICATIONS)
- **Intent Validation**: Proper validation of tile launch intents

### 2. Data Privacy
- **Local Storage**: All data stored locally, no network transmission
- **Backup Rules**: Configured to exclude sensitive timing data
- **Service Isolation**: Timer service isolated from other app components

## Development Workflow

### 1. Test-Driven Development
1. **Write Tests First**: Start with failing tests that define expected behavior
2. **Implement Minimum**: Write minimal code to make tests pass  
3. **Refactor**: Improve code quality while maintaining test coverage
4. **Coverage Verification**: Use JaCoCo to ensure 90%+ coverage

### 2. Commit Standards
**Pattern**: Conventional commits with clear scope
```
feat(timer): add skip rest functionality during rest intervals
fix(ui): correct progress ring color during rest periods  
test(service): add comprehensive timer service integration tests
refactor(repo): extract configuration synchronization logic
```

### 3. Quality Gates
**Before each merge**:
- [ ] All tests passing (unit + integration + UI)
- [ ] 90%+ test coverage for new/modified code
- [ ] No lint violations or code style issues
- [ ] Manual testing on target device completed
- [ ] Documentation updated for public API changes

## Scalability Considerations

### 1. Modular Architecture
- **Clear Layer Boundaries**: Easy to swap implementations
- **Interface-Based Design**: Facilitates testing and mocking
- **Dependency Injection**: Supports different configurations for testing/production

### 2. Configuration Management
- **Constants Centralization**: All magic numbers in Constants object
- **Type-Safe Configuration**: Compile-time validation of configuration values
- **Extensible Models**: Data classes support evolution with copy() methods

### 3. Testing Infrastructure
- **Fake Implementations**: Comprehensive fakes for all major components
- **Test Utilities**: Reusable test fixtures and helper functions  
- **Parameterized Tests**: Data-driven tests for comprehensive coverage

This architecture provides a solid foundation for a production-quality Wear OS application with excellent testability, maintainability, and performance characteristics.