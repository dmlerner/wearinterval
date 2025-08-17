# WearInterval Development Practices

## Android SDK Setup (Required)

**IMPORTANT**: Set these environment variables before running tests:

```bash
export ANDROID_HOME="/home/david/Android/Sdk"
export PATH="$ANDROID_HOME/tools/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### Quick Test Commands
```bash
# Run full test suite with coverage (includes instrumented tests)
./scripts/headless-test.sh

# Run unit tests only
./gradlew testDebugUnitTest

# Generate coverage report
./gradlew combinedCoverageReport
```

**Current Coverage**: **66%** overall (up from 25% with instrumented tests)

## Overview
This document establishes the development standards, testing practices, and code organization principles for the WearInterval project. These practices ensure high code quality, maintainability, and comprehensive test coverage.

## Code Organization & Structure

### Package Structure
```
com.wearinterval/
├── di/                     # Dependency injection modules
├── data/
│   ├── database/          # Room entities, DAOs, database
│   ├── datastore/         # DataStore preferences
│   ├── service/           # TimerService and related
│   └── repository/        # Repository implementations
├── domain/
│   ├── model/             # Core data models
│   ├── repository/        # Repository interfaces
│   └── usecase/           # Business logic use cases (if needed)
├── ui/
│   ├── screen/
│   │   ├── main/          # MainScreen + MainViewModel
│   │   ├── config/        # ConfigScreen + ConfigViewModel
│   │   ├── history/       # HistoryScreen + HistoryViewModel
│   │   └── settings/      # SettingsScreen + SettingsViewModel
│   ├── component/         # Reusable UI components
│   ├── theme/             # Compose theming
│   └── navigation/        # Navigation setup
├── wearos/
│   ├── tile/              # Tile service implementation
│   ├── complication/      # Complication service
│   └── notification/      # Notification management
└── util/                  # Utility classes and extensions
```

### File Naming Conventions
- **Screens**: `MainScreen.kt`, `ConfigScreen.kt`
- **ViewModels**: `MainViewModel.kt`, `ConfigViewModel.kt`
- **Repositories**: `TimerRepository.kt` (interface), `TimerRepositoryImpl.kt` (implementation)
- **Data Classes**: `TimerState.kt`, `TimerConfiguration.kt`
- **Services**: `TimerService.kt`
- **Tests**: `MainViewModelTest.kt`, `TimerRepositoryTest.kt`

### Class & Function Guidelines

#### Function Length & Responsibility
- **Maximum 20 lines per function** (excluding whitespace/comments)
- **Single Responsibility**: Each function does one thing well
- **Meaningful Names**: Functions should read like sentences
  ```kotlin
  // Good
  fun calculateRemainingTime(startTime: Long, duration: Duration): Duration
  fun isTimerInRestPhase(): Boolean
  fun startNextInterval()
  
  // Avoid
  fun doStuff()
  fun process(data: Any)
  fun handleTimer()
  ```

#### Class Size Limits
- **ViewModels**: Maximum 150 lines
- **Repositories**: Maximum 100 lines per implementation
- **Composables**: Maximum 50 lines per @Composable function
- **Data Classes**: Keep focused and cohesive

#### Extract Complex Logic
When functions exceed limits, extract to:
- **Private helper functions** within the same class
- **Extension functions** for utility operations
- **Separate classes** for complex business logic
- **Use cases** for multi-repository operations

## Testing Strategy & Coverage

### Coverage Requirements
- **Minimum 90% line coverage** for all packages
- **100% coverage** for:
  - ViewModels (all state transitions and event handling)
  - Repositories (all business logic)
  - Data models (all methods/properties)

### Testing Tools & Setup
```kotlin
// build.gradle.kts
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
testImplementation("io.mockk:mockk:1.13.8")
testImplementation("app.cash.turbine:turbine:1.0.0")
testImplementation("com.google.truth:truth:1.1.5")

androidTestImplementation("androidx.test.ext:junit:1.1.5")
androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
androidTestImplementation("androidx.room:room-testing:$room_version")
```

### Test Organization
```
src/
├── test/java/              # Unit tests
│   ├── ui/
│   │   └── screen/
│   │       ├── main/       # MainViewModelTest.kt
│   │       └── config/     # ConfigViewModelTest.kt
│   ├── data/
│   │   └── repository/     # TimerRepositoryTest.kt
│   └── domain/
│       └── model/          # TimerStateTest.kt
└── androidTest/java/       # Integration & UI tests
    ├── data/
    │   └── database/       # ConfigurationDaoTest.kt
    ├── ui/
    │   └── screen/         # MainScreenTest.kt
    └── EndToEndTest.kt
```

### Unit Testing Standards

#### ViewModel Testing Template
```kotlin
@ExperimentalCoroutinesApi
class MainViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val mockTimerRepository = mockk<TimerRepository>()
    private val mockConfigRepository = mockk<ConfigurationRepository>()
    
    private lateinit var viewModel: MainViewModel
    
    @Before
    fun setup() {
        every { mockTimerRepository.timerState } returns MutableStateFlow(TimerState.STOPPED)
        every { mockConfigRepository.currentConfiguration } returns MutableStateFlow(TimerConfiguration.DEFAULT)
        
        viewModel = MainViewModel(mockTimerRepository, mockConfigRepository)
    }
    
    @Test
    fun `when play button clicked in stopped state, timer starts`() = runTest {
        // Given
        coEvery { mockTimerRepository.startTimer() } just Runs
        
        // When
        viewModel.onEvent(MainScreenEvent.PlayPauseClicked)
        
        // Then
        coVerify { mockTimerRepository.startTimer() }
    }
    
    @Test
    fun `ui state reflects timer state changes`() = runTest {
        // Given
        val timerStateFlow = MutableStateFlow(TimerState.STOPPED)
        every { mockTimerRepository.timerState } returns timerStateFlow
        
        // When/Then
        viewModel.uiState.test {
            assertEquals(TimerPhase.STOPPED, awaitItem().timerPhase)
            
            timerStateFlow.value = TimerState.RUNNING
            assertEquals(TimerPhase.RUNNING, awaitItem().timerPhase)
        }
    }
}
```

#### Repository Testing Template
```kotlin
@ExperimentalCoroutinesApi
class TimerRepositoryTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private val mockTimerService = mockk<TimerService>()
    private val mockServiceConnection = mockk<ServiceConnection>()
    
    private lateinit var repository: TimerRepositoryImpl
    
    @Before
    fun setup() {
        every { mockTimerService.timerState } returns MutableStateFlow(TimerState.STOPPED)
        repository = TimerRepositoryImpl(mockTimerService)
    }
    
    @Test
    fun `startTimer calls service with correct configuration`() = runTest {
        // Given
        val config = TimerConfiguration(laps = 5, workDuration = 60.seconds)
        coEvery { mockTimerService.startTimer(any()) } just Runs
        
        // When
        repository.startTimer(config)
        
        // Then
        coVerify { mockTimerService.startTimer(config) }
    }
}
```

### Integration Testing

#### Database Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class ConfigurationDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: ConfigurationDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        dao = database.configurationDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveConfiguration() = runTest {
        // Given
        val config = TimerConfigurationEntity(
            id = "test-id",
            laps = 10,
            workDurationSeconds = 60,
            restDurationSeconds = 30,
            lastUsed = System.currentTimeMillis()
        )
        
        // When
        dao.insertConfiguration(config)
        val retrieved = dao.getRecentConfigurations(1).first()
        
        // Then
        assertThat(retrieved).containsExactly(config)
    }
}
```

### UI Testing

#### Composable Testing
```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun playButtonShowsCorrectStateWhenStopped() {
        // Given
        val uiState = MainScreenUiState(
            timerPhase = TimerPhase.STOPPED,
            timeRemaining = 60.seconds,
            isPlayButtonEnabled = true
        )
        
        // When
        composeTestRule.setContent {
            MainScreen(
                uiState = uiState,
                onEvent = {}
            )
        }
        
        // Then
        composeTestRule
            .onNodeWithContentDescription("Play")
            .assertIsDisplayed()
            .assertIsEnabled()
    }
    
    @Test
    fun playButtonClickTriggersEvent() {
        // Given
        var eventReceived: MainScreenEvent? = null
        val uiState = MainScreenUiState(timerPhase = TimerPhase.STOPPED)
        
        composeTestRule.setContent {
            MainScreen(
                uiState = uiState,
                onEvent = { eventReceived = it }
            )
        }
        
        // When
        composeTestRule
            .onNodeWithContentDescription("Play")
            .performClick()
        
        // Then
        assertThat(eventReceived).isEqualTo(MainScreenEvent.PlayPauseClicked)
    }
}
```

## Code Quality Standards

### Kotlin Best Practices

#### Data Classes & Immutability
```kotlin
// Prefer immutable data classes
data class TimerState(
    val phase: TimerPhase,
    val timeRemaining: Duration,
    val currentLap: Int,
    val totalLaps: Int,
    val isPaused: Boolean = false
) {
    // Computed properties for derived state
    val isRunning: Boolean get() = phase == TimerPhase.RUNNING
    val progressPercentage: Float get() = // calculation logic
}

// Use sealed classes for state machines
sealed class TimerPhase {
    object Stopped : TimerPhase()
    object Running : TimerPhase()
    object Resting : TimerPhase()
    object Paused : TimerPhase()
    object AlarmActive : TimerPhase()
}
```

#### Error Handling
```kotlin
// Use Result for operations that can fail
suspend fun saveConfiguration(config: TimerConfiguration): Result<Unit> {
    return try {
        dataStore.save(config)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Handle errors at appropriate boundaries
class ConfigurationViewModel @Inject constructor(
    private val repository: ConfigurationRepository
) : ViewModel() {
    fun saveConfiguration(config: TimerConfiguration) {
        viewModelScope.launch {
            repository.saveConfiguration(config)
                .onSuccess { /* Update UI state */ }
                .onFailure { error -> /* Handle error */ }
        }
    }
}
```

#### Resource Management
```kotlin
// Proper coroutine scope usage
class TimerService : Service() {
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

// Cleanup in ViewModels
class MainViewModel @Inject constructor() : ViewModel() {
    override fun onCleared() {
        // Cleanup resources
        super.onCleared()
    }
}
```

### Documentation Standards

#### KDoc Requirements
```kotlin
/**
 * Manages the core timer functionality including countdown logic and state transitions.
 * 
 * This service runs as a foreground service to ensure timer reliability even when
 * the app is not in the foreground.
 * 
 * @param notificationManager Handles timer notifications and alerts
 * @param wakeLockManager Manages CPU wake locks during active timers
 */
class TimerService @Inject constructor(
    private val notificationManager: NotificationManager,
    private val wakeLockManager: WakeLockManager
) : Service() {
    
    /**
     * Starts a new timer session with the specified configuration.
     * 
     * @param config The timer configuration including laps and durations
     * @throws IllegalStateException if timer is already running
     */
    fun startTimer(config: TimerConfiguration) {
        // Implementation
    }
}
```

## CI/CD Integration

### Gradle Coverage Configuration
```kotlin
// build.gradle.kts
android {
    buildTypes {
        debug {
            enableAndroidTestCoverage = true
            enableUnitTestCoverage = true
        }
    }
    
    testCoverageEnabled = true
}

dependencies {
    // JaCoCo for coverage
    testImplementation("org.jacoco:org.jacoco.core:0.8.8")
}

// Coverage task
tasks.register("jacocoTestReport", JacocoReport::class) {
    dependsOn("testDebugUnitTest")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    executionData.setFrom(fileTree("$buildDir/jacoco").include("**/*.exec"))
    
    sourceSets(sourceSets.main.get())
}
```

## Version Control & Commit Practices

### Sapling Workflow
WearInterval uses **Sapling** for version control, following these practices:

#### Commit Standards
- **Atomic commits**: Each commit represents a single logical change
- **Descriptive messages**: Follow conventional commit format
- **Frequent commits**: Commit working code often (multiple times per day)
- **CRITICAL RULE**: **NEVER** claim tasks are "completed" without successful build and test verification

#### Commit Message Format
```
type(scope): brief description

Optional detailed explanation of what and why.

- List specific changes if needed
- Reference relevant issues
```

**Types:**
- `feat`: New feature or functionality
- `fix`: Bug fix
- `test`: Adding or updating tests
- `refactor`: Code refactoring without functionality changes
- `docs`: Documentation updates
- `style`: Code style/formatting changes
- `perf`: Performance improvements

**Examples:**
```
feat(timer): implement countdown logic with state transitions

Add core timer functionality including work/rest intervals,
auto-progression, and proper state management.

- Implement TimerService countdown mechanism
- Add interval transition logic
- Include wake lock management
- Add comprehensive unit tests

test(repository): add comprehensive TimerRepository tests

Cover all timer operations and error scenarios with 100% coverage.

refactor(ui): extract MainScreen progress ring into component

Improve reusability and testing by separating progress ring logic.
```

#### Sapling Commands
```bash
# Create new commit
sl commit -m "feat(timer): implement basic countdown logic"

# Amend current commit
sl amend

# Rebase current changes
sl rebase -d main

# Submit for review
sl pr submit

# Update working copy
sl pull --rebase
```

### Pre-commit Hooks
Configure pre-commit validation to **PREVENT** committing broken code:

**`.pre-commit-config.yaml`:**
```yaml
repos:
  - repo: local
    hooks:
      - id: build-check
        name: Verify project builds
        entry: ./gradlew assemble
        language: system
        pass_filenames: false
        stages: [commit]
        
      - id: unit-tests
        name: Run unit tests
        entry: ./gradlew testDebugUnitTest
        language: system
        pass_filenames: false
        stages: [commit]
        
      - id: test-coverage
        name: Check test coverage
        entry: ./gradlew jacocoTestReport
        language: system
        pass_filenames: false
        stages: [commit]
        
      - id: lint
        name: Run Android lint
        entry: ./gradlew lintDebug
        language: system
        pass_filenames: false
        stages: [commit]
```

### MANDATORY Pre-Submit Verification
**Before claiming any task as "completed" or "done":**

```bash
# 1. MUST successfully build
./gradlew assemble

# 2. MUST pass all tests
./gradlew test

# 3. MUST generate coverage report
./gradlew jacocoTestReport

# 4. MUST pass lint checks
./gradlew lintDebug

# 5. MUST have clean repository status
sl status  # Should only show intended changes
```

**Failure to verify means task is NOT completed.**

### Repository Management
**Keep repository clean and organized:**

#### .slignore Configuration
- **Build artifacts**: `**/.gradle/`, `**/build/`
- **Generated files**: `*.class`, `*.dex`, `*.ap_`, `*.exec`, `*.ec`
- **Local configs**: `local.properties`
- **IDE files**: `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`
- **Regeneratable**: `gradle/wrapper/gradle-wrapper.jar`

#### Commit Strategy
- **Commit frequently** with atomic, logical changes
- **Clean build artifacts** before committing: `./gradlew clean && rm -rf .gradle`
- **Remove temporary scripts** and development tools
- **Verify clean status**: `sl status` should show only intended files

#### Repository Hygiene Rules
- **NEVER commit build artifacts** or generated files
- **NEVER commit local configuration** (`local.properties`, IDE settings)
- **NEVER commit temporary scripts** created for development convenience
- **ALWAYS verify** repository is clean before phase completion

## Development Workflow

### Test-Driven Development (TDD)
1. **Red**: Write a failing test first
2. **Green**: Write minimal code to make test pass
3. **Refactor**: Improve code while keeping tests green

### Feature Development Process
1. **Create feature branch**: `sl bookmark feature-timer-logic`
2. **Write tests** for the feature first
3. **Implement** minimum viable functionality
4. **Commit frequently** with descriptive messages
5. **Verify coverage** meets requirements (90%+)
6. **Submit for review**: `sl pr submit`
7. **Land changes**: `sl pr land` after approval

### Task Tracking & Progress Management
Maintain continuous project state awareness through these practices:

#### Progress Tracking File: `PROJECT_STATUS.md`
**Location**: Root directory  
**Purpose**: Single source of truth for current project state  
**Updates**: After each significant milestone or daily at minimum

**Content Structure:**
```markdown
# WearInterval Project Status

## Current State
**Phase**: [Current implementation phase]
**Last Updated**: [Date]
**Overall Progress**: [X/Y tasks completed]

## Recently Completed
- [Task description with date]
- [Task description with date]

## Currently Working On
- [Task description with status]
- [Blockers or challenges]

## Next Up
- [Next 2-3 tasks in priority order]

## Coverage Metrics
- **Unit Tests**: X%
- **Integration Tests**: X%
- **UI Tests**: X%
- **Overall**: X%

## Architecture Decisions
- [Recent architectural choices made]
- [Rationale for decisions]

## Issues & Blockers
- [Current blockers]
- [Technical debt items]
```

#### Daily Progress Routine
**Start of Day:**
1. Review `PROJECT_STATUS.md`
2. Update TodoWrite with current tasks
3. Check test coverage reports
4. Review recent commits

**End of Day:**
1. Update `PROJECT_STATUS.md` with progress
2. Mark completed todos
3. Plan next day's priorities
4. Commit work with descriptive messages

#### Weekly Reviews
**Every Friday:**
1. Review overall phase progress
2. Update architecture decisions log
3. Assess test coverage trends
4. Plan upcoming week priorities
5. Document any technical debt

### Daily Practices
- **Run tests** before committing: `./gradlew test`
- **Check coverage**: `./gradlew jacocoTestReport`
- **Lint code**: `./gradlew lintDebug`
- **Update progress**: Maintain `PROJECT_STATUS.md`
- **Review changes** for single responsibility principle
- **Commit frequently** with atomic, well-described changes

### Performance Monitoring
- **Recomposition tracking** in Compose UI
- **Memory leak detection** using LeakCanary
- **Battery usage profiling** for TimerService
- **Network usage monitoring** (minimal for this app)

## Enforcement & Reviews

### Code Review Checklist
- [ ] Functions under 20 lines
- [ ] Classes follow single responsibility
- [ ] Tests cover new functionality (90%+ coverage)
- [ ] No hardcoded values
- [ ] Proper error handling
- [ ] KDoc for public APIs
- [ ] Meaningful variable/function names
- [ ] No TODO comments in production code

### Automated Checks
- **GitHub Actions** run tests on PR
- **Coverage threshold** enforced in CI
- **Lint checks** must pass
- **Build verification** on multiple API levels

This development practices document ensures high-quality, maintainable code with comprehensive test coverage throughout the WearInterval development process.