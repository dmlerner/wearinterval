# WearInterval Test Coverage Improvement Plan

## Current State (Updated: Aug 18, 2025)
- **Test Count:** 291 tests, 0 failing ✅
- **Overall Coverage:** 19% instruction coverage (unchanged from baseline)
- **Target:** 90% instruction coverage  
- **Strategy:** Focus on unit tests first (faster, more maintainable) before instrumented tests

### Recent Progress Summary
**✅ Completed comprehensive edge case testing** with 38 new test cases added:
- **TimerServiceLogicTest**: +10 tests (configuration changes, countdown accuracy, phase transitions, boundary conditions)
- **ConfigViewModelTest**: +17 tests (UI state management, error handling, input validation)
- **MainViewModelTest**: +11 tests (state transitions, service handling, error scenarios)

## Coverage Analysis by Layer (Actual JaCoCo Data)

| Layer | Current Coverage | Target | Test Type | Priority | Status |
|-------|------------------|--------|-----------|----------|--------|
| Domain Models | 99% | ✅ Complete | Unit | N/A | ✅ |
| Domain Repositories (interfaces) | 100% | ✅ Complete | Unit | N/A | ✅ |
| Data Repositories | 65% | 90% | Unit | High | 🟡 Partial |
| Service Logic | 6% | 80% | Unit | High | 🔴 Critical Gap |
| UI Config Screen | 12% | 90% | Unit | Critical | 🔴 Critical Gap |
| UI Main Screen | 21% | 90% | Unit | Critical | 🔴 Critical Gap |
| UI History Screen | 8% | 90% | Unit | Critical | 🔴 Critical Gap |  
| UI Settings Screen | 17% | 90% | Unit | Critical | 🔴 Critical Gap |
| UI Components | 1% | 60% | Instrumented | Low | 🔴 Low Priority |
| WearOS Tile | 0% | 70% | Unit | Medium | 🔴 Medium Priority |
| WearOS Notification | 3% | 70% | Unit | Medium | 🔴 Medium Priority |
| Utils | 65% | 90% | Unit | Low | 🟡 Nearly Complete |
| DataStore | 14% | 80% | Unit | Medium | 🔴 Medium Priority |

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

## Phase 2: Address Critical Coverage Gaps (Current Phase)
**Goal:** Target 35-40% overall coverage through high-impact areas
**Status:** 🔄 **IN PROGRESS** - Recent edge case tests added, now focus on production code coverage

### 2.1: UI Screen Implementation Coverage 🔴 **HIGH PRIORITY**
**Current Impact:** UI screens are only 8-21% covered despite extensive ViewModel tests
**Root Cause:** Tests exercise ViewModels but not the actual UI composables and state logic

#### Strategy: Target UI State Logic, not Composables
```kotlin
// Focus on testing these areas in UI packages:
- Screen state management functions
- Event handling implementations  
- Data transformation logic
- Navigation state updates
- Error state handling
```

#### Immediate Targets:
- **Config Screen**: 12% → 50% (+3,100 instructions covered)
- **Main Screen**: 21% → 50% (+1,400 instructions covered)  
- **History Screen**: 8% → 40% (+1,500 instructions covered)
- **Settings Screen**: 17% → 40% (+800 instructions covered)

### 2.2: Service Implementation Coverage 🔴 **CRITICAL** 
**Current:** 6% (914 of 978 instructions missed)
**Target:** 50% (+450 instructions covered)
**Status:** ✅ Logic tests added, now need **actual service implementation testing**

#### Strategy: Focus on TimerService Implementation
```kotlin
// Target these uncovered areas in data.service package:
- Service lifecycle methods (onCreate, onDestroy, onBind)
- Foreground service notification management  
- Timer state persistence during service restart
- Background/foreground transitions
- Service binding/unbinding scenarios
- Exception handling in service methods
- Resource cleanup on service destruction
```

#### Recent Work Completed: ✅
- TimerServiceLogicTest: 10 comprehensive edge case tests added
- **Next:** Need integration tests for actual service implementation

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

### 2.3: Updated Phase 2 Success Criteria:
- **35-40% overall instruction coverage** (realistic based on current data)
- **UI screen implementations** tested for state management logic  
- **Service implementation** methods covered (not just logic)
- **Repository error scenarios** completed (currently 65% → 85%)

### Phase 2 Progress Status:
- ✅ **Edge case testing completed** (38 new tests added)
- 🔄 **UI implementation coverage** (in progress)
- 🔄 **Service implementation testing** (next priority)
- 🟡 **Repository completion** (partial - error handling needed)

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