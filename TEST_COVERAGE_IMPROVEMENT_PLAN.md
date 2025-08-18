# WearInterval Test Coverage Improvement Plan

## Current State
- **Test Count:** 257 tests, 0 failing ✅
- **Overall Coverage:** 19% instruction coverage
- **Target:** 90% instruction coverage
- **Strategy:** Focus on unit tests first (faster, more maintainable) before instrumented tests

## Coverage Analysis by Layer

| Layer | Current Coverage | Target | Test Type | Priority |
|-------|------------------|--------|-----------|----------|
| Domain Models | 99% | ✅ Complete | Unit | N/A |
| Domain Repositories (interfaces) | 100% | ✅ Complete | Unit | N/A |
| Data Repositories | 65% | 90% | Unit | High |
| UI ViewModels | 8-21% | 90% | Unit | Critical |
| Service Logic | 6% | 80% | Unit | High |
| WearOS Logic | 0-3% | 70% | Unit | Medium |
| UI Components | 1% | 60% | Instrumented | Low |
| MainActivity | 0% | 60% | Instrumented | Low |
| System Integration | 0% | 50% | Instrumented | Low |

---

## Phase 1: Fix Failing Tests ✅ COMPLETED
**Goal:** Achieve green test suite before adding new tests

### Tasks Completed:
1. **✅ Fix WearOS Repository Test**
   - Fixed `WearOsRepositoryTest.getComplicationData LongText` assertion
   - Updated to use `shortDisplayString()` method for proper `"∞×1:00"` formatting
   - File: `WearOsRepositoryImpl.kt:65`

2. **✅ Fix Integration Test Flow Issues**
   - Fixed 6 failing `ConfigToMainIntegrationTest` assertions
   - Root cause: Flow synchronization timing issues with combined StateFlows
   - Solutions: Proper Turbine test patterns, mocked repositories for better control

3. **✅ Fix Repository Test Async Issues**
   - Fixed 5 failing `ConfigurationRepositoryTest` assertions
   - Root cause: Incorrect validation limits, missing mocks, flow emission timing
   - Solutions: Updated test expectations to match actual constants, proper mock setup

4. **✅ Fix MainViewModel Timeout**
   - Fixed `MainViewModelTest.ui state computed properties` timeout
   - Root cause: StateFlow combine() not emitting due to initialization timing
   - Solution: Pre-configure flows before ViewModel creation, proper test sequencing

### Success Criteria: ✅ ACHIEVED
- All 257 tests passing
- Clean test pipeline
- Baseline established for coverage improvements

---

## Phase 2: Critical Unit Test Coverage (Week 2-3)
**Goal:** Target 60% overall coverage through high-impact unit tests

### 2.1: Complete ViewModel Test Coverage
**Impact:** ~15% coverage gain
**Files to enhance:**

#### ConfigViewModel (Current: ~76% → Target: 95%)
```kotlin
// New test cases needed:
- SetLapsToInfinite event handling
- SetWorkToLong/SetRestToLong edge cases  
- ClearAllData event
- Error handling scenarios
- State validation edge cases
```

#### MainViewModel (Current: ~85% → Target: 95%)
```kotlin
// New test cases needed:
- Timer state edge transitions
- Flash screen timing
- Error state handling
- Configuration changes during timer
- Service disconnection scenarios
```

#### HistoryViewModel (Current: ~100% → Maintain)
- Already well-covered, minimal additions needed

#### SettingsViewModel (Current: ~100% → Maintain)  
- Already well-covered, minimal additions needed

### 2.2: Service Business Logic Tests
**Impact:** ~10% coverage gain
**New file:** `TimerServiceLogicTest.kt` (enhance existing)

```kotlin
// Test scenarios:
class TimerServiceLogicTest {
    // Core timer logic
    - Countdown progression accuracy
    - Work/rest phase transitions
    - Lap progression handling
    - Timer completion detection
    - Pause/resume state management
    
    // Configuration changes
    - Mid-timer configuration updates
    - Invalid configuration handling
    - Configuration persistence
    
    // Error scenarios  
    - Invalid timer states
    - Negative time handling
    - Boundary conditions (0 laps, 0 duration)
    
    // Integration points
    - Notification triggers
    - State broadcast accuracy
    - Observer pattern compliance
}
```

### 2.3: Repository Implementation Coverage
**Impact:** ~8% coverage gain (65% → 90%)

#### ConfigurationRepositoryImpl Enhancements
```kotlin
// Additional test scenarios:
- Concurrent configuration updates
- DataStore corruption handling
- Room transaction failures
- Flow error propagation
- Cache invalidation scenarios
- Cleanup logic edge cases
```

#### TimerRepositoryImpl Enhancements  
```kotlin
// Service integration scenarios:
- Service connection failures
- Service disconnection handling  
- State synchronization edge cases
- Binder death scenarios
- Multiple observer management
```

### Success Criteria:
- 60%+ overall instruction coverage
- All ViewModel state flows tested
- Core service logic covered
- Repository error scenarios covered

---

## Phase 3: Specialized Logic Coverage (Week 4)
**Goal:** Target 75% overall coverage through specialized unit tests

### 3.1: WearOS Logic Tests
**Impact:** ~5% coverage gain
**New files:**

#### WearOsRepositoryImpl Comprehensive Tests
```kotlin
class WearOsRepositoryImplTest {
    // Complication data formatting
    - All ComplicationType variations
    - Different timer states (stopped, running, resting, alarm)
    - Edge cases (infinite laps, zero time, long durations)
    - Text formatting accuracy
    - Icon selection logic
    
    // Tile data generation
    - Tile layout based on timer state
    - Action button configurations
    - State-dependent content
    - Error state tile content
    
    // Data transformation
    - Timer state to display mapping
    - Configuration to tile mapping
    - Progress calculations
    - Time formatting variations
}
```

### 3.2: Data Layer Edge Cases
**Impact:** ~3% coverage gain

#### DataStoreManager Enhanced Tests
```kotlin
// Additional scenarios:
- Preference migration handling
- Corrupted data recovery
- Concurrent access patterns
- Flow exception handling
- Default value fallbacks
```

### 3.3: Utility Function Coverage
**Impact:** ~2% coverage gain

#### TimeUtils Comprehensive Tests
```kotlin
// Additional test cases:
- Extreme duration values
- Locale-specific formatting
- Edge case durations (0, max values)
- Format consistency across languages
- Performance with large values
```

### Success Criteria:
- 75%+ overall instruction coverage
- All WearOS display logic tested
- Data layer resilience tested
- Utility functions fully covered

---

## Phase 4: Integration and Edge Cases (Week 5)
**Goal:** Target 85% overall coverage through comprehensive unit tests

### 4.1: Cross-Layer Integration Tests
**New files:**

#### DataFlow Integration Tests
```kotlin
class DataFlowIntegrationTest {
    // End-to-end data flows
    - Configuration change propagation
    - Timer state synchronization
    - Settings persistence flows
    - Error propagation chains
    
    // Repository coordination
    - Multi-repository operations
    - Transaction consistency
    - Rollback scenarios
    - Cache coherence
}
```

#### ViewModelIntegration Tests
```kotlin
class ViewModelIntegrationTest {
    // Cross-ViewModel scenarios
    - Config changes affecting Main screen
    - History selection updating Config
    - Settings changes affecting Timer
    - Navigation state management
}
```

### 4.2: Error Handling and Edge Cases
**Files to enhance:** All existing test files

```kotlin
// Universal error scenarios:
- Network/database failures
- Invalid state transitions  
- Resource exhaustion
- Concurrent modification
- Memory pressure scenarios
- Configuration corruption
```

### 4.3: Performance and Stress Tests
```kotlin
class PerformanceTest {
    // Load testing
    - Large configuration histories
    - Long-running timer sessions
    - Rapid configuration changes
    - Memory leak detection
    - Flow performance under load
}
```

### Success Criteria:
- 85%+ overall instruction coverage
- All error paths tested
- Integration scenarios covered
- Performance characteristics validated

---

## Phase 5: Instrumented Tests (Week 6, Optional)
**Goal:** Target 90%+ overall coverage through selective instrumented tests

### 5.1: Critical UI Component Tests
**New files:** (Only if coverage target not met)

```kotlin
class ProgressRingInstrumentedTest {
    // Visual behavior
    - Progress animation accuracy
    - Color state changes
    - Size adaptations
    - Accessibility compliance
}

class MainActivityInstrumentedTest {
    // Activity lifecycle
    - Configuration changes
    - Intent handling
    - Permission flows
    - Navigation state
}
```

### 5.2: Service Integration Tests
```kotlin
class TimerServiceInstrumentedTest {
    // Android framework integration
    - Foreground service behavior
    - Notification lifecycle
    - Service binding/unbinding
    - Background restrictions
}
```

### Success Criteria:
- 90%+ overall instruction coverage
- Critical UI paths verified
- Service integration validated
- Complete test suite maintained

---

## Implementation Guidelines

### Testing Standards
1. **Test Naming:** `should_expectedBehavior_when_condition()`
2. **Structure:** Given/When/Then pattern
3. **Coverage:** Minimum 3 scenarios per function (happy path, edge case, error)
4. **Assertions:** Specific, meaningful error messages
5. **Isolation:** Each test independent, proper setup/teardown

### Test Organization
```
src/test/java/com/wearinterval/
├── data/
│   ├── repository/         # Repository implementation tests
│   ├── service/           # Service business logic tests
│   └── datastore/         # DataStore integration tests
├── domain/                # Already complete
├── ui/
│   ├── viewmodel/         # ViewModel comprehensive tests
│   └── integration/       # Cross-ViewModel tests
├── wearos/               # WearOS logic tests
└── util/                 # Utility comprehensive tests
```

### Continuous Integration
1. **Coverage Gates:** Fail build if coverage drops below 85%
2. **Test Performance:** Unit tests must complete within 2 minutes
3. **Failure Analysis:** Immediate alerts for test failures
4. **Reporting:** Weekly coverage reports with trend analysis

### Success Metrics
- **Phase 1:** 0 failing tests
- **Phase 2:** 60% instruction coverage
- **Phase 3:** 75% instruction coverage  
- **Phase 4:** 85% instruction coverage
- **Phase 5:** 90% instruction coverage (if needed)
- **Maintenance:** Coverage never drops below 85%

---

## Risk Mitigation

### Technical Risks
1. **Flaky Tests:** Use deterministic mocks, avoid timing dependencies
2. **Test Maintenance:** Keep tests simple, avoid over-mocking
3. **Performance:** Monitor test execution time, parallelize where possible

### Schedule Risks
1. **Scope Creep:** Focus on coverage numbers, not perfect tests
2. **Blocking Issues:** Have fallback plans for difficult areas
3. **Resource Constraints:** Prioritize highest-impact areas first

This plan provides a clear path from 19% to 90% test coverage through systematic, prioritized testing with measurable milestones and clear success criteria.