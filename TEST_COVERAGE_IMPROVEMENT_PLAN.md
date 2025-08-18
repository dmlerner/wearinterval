# WearInterval Test Coverage Plan - Robolectric-First Strategy

## Current State (Aug 18, 2025)
- **Test Count:** 347+ tests (14 new Robolectric tests added)
- **Overall Coverage:** 28% instruction coverage (previous: 25%, baseline: 19%)
- **Target:** 70% instruction coverage (realistic production goal)
- **Strategy:** ✅ **HYBRID APPROACH IMPLEMENTED** - Unit tests + Robolectric for Android framework + Minimal emulator tests

## Key Insights from Robolectric Implementation
✅ **PHASE 3 COMPLETE: Robolectric Infrastructure Successfully Implemented**
1. **Robolectric is highly effective** for Android framework testing (Service lifecycle, notifications, power management)
2. **Unit tests remain king** for business logic and utility classes (MainUiState: 96% coverage)
3. **Emulator tests should be minimal** - only for true device-dependent behavior

### Robolectric Success Results:
- **TimerServiceRobolectricTest**: 9 comprehensive Service lifecycle tests (onCreate, onBind, onDestroy, power management)
- **WearOsComponentsRobolectricTest**: 9 WearOS framework tests (notifications, vibration, alerts, channels)
- **Service Testing**: Now possible without emulator - 5% coverage achieved for data.service package
- **WearOS Components**: 53% coverage for wearos.notification package with Android framework integration
- **Execution Time**: ~16 seconds for all Robolectric tests vs minutes for emulator equivalent

---

## Revised Coverage Strategy by Test Type

### **UNIT TESTABLE (Business Logic) - Primary Focus**
| Component | Current | Target | Strategy | Priority |
|-----------|---------|--------|----------|----------|
| Domain Models | 99% | ✅ | Unit | Complete |
| Domain Repositories | 100% | ✅ | Unit | Complete |
| **MainUiState** | **96%** | ✅ | Unit | Complete |
| ConfigPickerValues | High | ✅ | Unit | Complete |
| Data Repositories | 66% | 85% | Unit + Mock | **High** |
| DataStore | 44% | 75% | Unit + Mock | High |
| Utils | 54% | 85% | Unit | Medium |

### **ROBOLECTRIC TESTABLE (Android Framework) - Proven Effective**
| Component | Current | Target | Strategy | Priority |
|-----------|---------|--------|----------|----------|
| TimerService | 5% | 60% | Robolectric | **High** |
| WearOS Notification | 53% | 75% | Robolectric | **High** |
| WearOS Tile | 0% | 50% | Robolectric | Medium |
| MainActivity | 0% | 40% | Robolectric | Medium |
| Notification Receiver | 0% | 60% | Robolectric | Medium |

### **UI LOGIC EXTRACTABLE (Move to Unit Tests) - Critical Path**
| Component | Current | Target | Strategy | Priority |
|-----------|---------|--------|----------|----------|
| History Screen Logic | 9% | 70% | Extract → Unit | **Critical** |
| Settings Screen Logic | 17% | 70% | Extract → Unit | **Critical** |
| Config Screen Logic | 40% | 75% | Enhance Unit | High |
| Main Screen Logic | 23% | 60% | Enhance Unit | Medium |

### **EMULATOR ONLY (True Device Dependencies) - Minimal**
| Component | Current | Target | Strategy | Priority |
|-----------|---------|--------|----------|----------|
| UI Components (Visual) | 1% | 30% | Emulator | Low |
| Navigation Flow | 0% | 25% | Emulator | Low |

---

## Implementation Phases

### **Phase 4: UI Logic Extraction (Current Priority) 🔄 IN PROGRESS**
**Goal:** Extract testable business logic from UI screens to achieve massive coverage gains

#### 4.1: History Screen Logic Extraction
**Target:** 9% → 70% coverage
```kotlin
// CREATE: HistoryContract.kt with utility functions
object HistoryScreenUtils {
    fun formatConfigurationDisplay(config: TimerConfiguration): String
    fun sortConfigurationsByRecentUsage(configs: List<TimerConfiguration>): List<TimerConfiguration>
    fun filterValidConfigurations(configs: List<TimerConfiguration>): List<TimerConfiguration>
    fun getEmptyStateMessage(isLoading: Boolean): String
}

// CREATE: HistoryScreenUtilsTest.kt - Unit tests for extracted logic
class HistoryScreenUtilsTest {
    - formatConfigurationDisplay() with various timer configurations
    - sortConfigurationsByRecentUsage() algorithm correctness
    - filterValidConfigurations() edge cases
    - getEmptyStateMessage() logic branches
}
```

#### 4.2: Settings Screen Logic Extraction  
**Target:** 17% → 70% coverage
```kotlin
// CREATE: SettingsContract.kt with utility functions
object SettingsScreenUtils {
    fun validateSettingsCombination(settings: NotificationSettings): ValidationResult
    fun getToggleButtonState(setting: Boolean, isEnabled: Boolean): ButtonState
    fun shouldShowSettingsWarning(settings: NotificationSettings): Boolean
    fun getSettingsDescription(settings: NotificationSettings): String
}

// CREATE: SettingsScreenUtilsTest.kt - Unit tests
class SettingsScreenUtilsTest {
    - validateSettingsCombination() business rules
    - getToggleButtonState() UI state logic
    - shouldShowSettingsWarning() condition checks
    - getSettingsDescription() text generation
}
```

**Expected Impact:** +17% overall coverage gain

### **Phase 5: Enhanced Service Testing (Robolectric) 📋 PLANNED**
**Goal:** Comprehensive Android framework component testing

#### 5.1: Advanced TimerService Testing
**Target:** 5% → 60% coverage
```kotlin
// EXPAND: TimerServiceRobolectricTest.kt (currently 9 tests → 25+ tests)
class TimerServiceRobolectricTest {
    // ADD: Advanced service scenarios
    - Service restart and recovery scenarios
    - Multiple client binding edge cases
    - Foreground service notification update flows
    - Wake lock management in different states
    - Service command queuing and processing
    - Background execution limit handling
    - Service crash recovery mechanisms
    - Timer state persistence during restarts
    - Service memory management
    - Concurrent access scenarios
}
```

#### 5.2: MainActivity Robolectric Testing
**Target:** 0% → 40% coverage
```kotlin
// CREATE: MainActivityRobolectricTest.kt
class MainActivityRobolectricTest {
    - Activity lifecycle (onCreate, onResume, onPause, onDestroy)
    - Intent handling and deep link processing
    - Service binding and unbinding lifecycle
    - Configuration change handling
    - Permission request flows
    - Navigation state management
    - Theme application and display settings
    - Background/foreground transition handling
}
```

**Expected Impact:** +10% overall coverage gain

### **Phase 6: Data Layer Enhancement 📋 PLANNED**
**Goal:** Complete data layer testing with proper error scenarios

#### 6.1: Repository Error Scenario Testing
**Target:** 66% → 85% coverage
```kotlin
// EXPAND: Repository tests with comprehensive error scenarios
- DataStore corruption and recovery
- Room database transaction failures
- Concurrent access race conditions
- Network connectivity issues (if applicable)
- Memory pressure scenarios
- Cache invalidation edge cases
- Flow error propagation chains
- Configuration migration failures
```

#### 6.2: DataStore Comprehensive Testing
**Target:** 44% → 75% coverage
```kotlin
// EXPAND: DataStoreManager testing
- Preference corruption recovery mechanisms
- Concurrent write/read scenarios
- Default value fallback logic
- Flow exception handling and recovery
- Migration between preference versions
- Storage quota exceeded scenarios
- File system permission issues
```

**Expected Impact:** +7% overall coverage gain

### **Phase 7: WearOS Integration (Robolectric) 📋 PLANNED**
**Goal:** Complete WearOS platform integration testing

#### 7.1: WearOS Tile Service Testing
**Target:** 0% → 50% coverage
```kotlin
// CREATE: WearIntervalTileServiceRobolectricTest.kt
class WearIntervalTileServiceRobolectricTest {
    - Tile layout generation and updates
    - State-dependent tile content changes
    - Action button functionality testing
    - Progress display accuracy validation
    - Resource management and cleanup
    - Update frequency optimization
    - Tile interaction event handling
    - Performance under different states
}
```

#### 7.2: Enhanced WearOS Components
**Target:** 53% → 75% coverage  
```kotlin
// EXPAND: WearOsComponentsRobolectricTest.kt (currently 9 tests → 20+ tests)
- Advanced notification scenarios
- Custom notification action handling
- Vibration pattern accuracy
- Alert sound management
- Screen flash coordination
- Multi-timer notification management
- Notification priority and channel management
- Workout completion alert flows
```

**Expected Impact:** +5% overall coverage gain

### **Phase 8: Emulator Polish (Final Phase) 📋 PLANNED**  
**Goal:** Cover only truly device-dependent behavior

#### 8.1: Visual UI Component Testing (Minimal)
```kotlin
// Minimal emulator tests for truly device-dependent behavior:
- ProgressRing animation smoothness on real hardware
- Color accuracy and accessibility compliance
- Touch interaction responsiveness
- Screen size adaptation validation
```

**Expected Impact:** +3% overall coverage gain

---

## Coverage Progression Targets

| Phase | Coverage Target | Key Focus | Timeline |
|-------|-----------------|-----------|----------|
| **Current** | 28% | ✅ Robolectric Infrastructure | Complete |
| **Phase 4** | 45% | UI Logic Extraction | Week 1 |
| **Phase 5** | 55% | Enhanced Service/Activity Testing | Week 2 |
| **Phase 6** | 62% | Data Layer Error Scenarios | Week 3 |
| **Phase 7** | 67% | Complete WearOS Integration | Week 4 |
| **Phase 8** | 70% | Emulator Polish (Minimal) | Week 5 |

## Success Metrics & Gates

### Phase 4 Success Criteria:
- [ ] History screen utility logic extracted and 70% covered
- [ ] Settings screen utility logic extracted and 70% covered  
- [ ] Overall coverage reaches 45%+
- [ ] All new tests execute in <2 seconds (unit test speed)

### Phase 5 Success Criteria:
- [ ] TimerService coverage reaches 60%+
- [ ] MainActivity basic lifecycle coverage achieved
- [ ] Advanced service scenarios fully tested
- [ ] All Robolectric tests execute in <30 seconds

### Phase 6 Success Criteria:
- [ ] Repository error scenarios comprehensively tested
- [ ] DataStore edge cases covered
- [ ] Data layer coverage reaches 85%+
- [ ] All tests remain fast (no emulator dependency)

### Phase 7 Success Criteria:
- [ ] WearOS Tile Service testing implemented
- [ ] Notification system fully covered
- [ ] WearOS integration tests complete
- [ ] Overall coverage reaches 67%+

### Phase 8 Success Criteria:
- [ ] Critical UI components validated on real hardware
- [ ] Device-specific behavior verified
- [ ] Final coverage target of 70%+ achieved
- [ ] Emulator test suite remains minimal (<10 tests)

## Key Development Principles

1. **Extract First, Test Second:** Move complex UI logic to testable utility classes before writing tests
2. **Robolectric for Android Framework:** Use Robolectric for all Service, Activity, Notification, and system integration testing  
3. **Unit Tests for Business Logic:** Keep core logic testing fast and reliable
4. **Mock Strategically:** Create controlled test environments for complex scenarios
5. **Emulator Last Resort:** Only use emulator for truly device-dependent visual/interaction testing
6. **Quality Over Quantity:** Focus on instruction count impact and meaningful test scenarios
7. **Fast Feedback Loops:** Maintain sub-30-second test suite execution for rapid development

## Risk Mitigation

### Technical Risks:
- **Robolectric Limitations:** Some Android framework features may not work perfectly in Robolectric
  - *Mitigation:* Identify limitations early and fallback to emulator only when necessary
- **UI Logic Extraction Complexity:** Some UI logic may be tightly coupled to Compose
  - *Mitigation:* Start with simple utility functions and gradually extract more complex logic

### Schedule Risks:
- **Over-ambitious Targets:** 70% coverage may still be challenging
  - *Mitigation:* Focus on high-impact areas first, adjust targets based on real progress
- **Robolectric Learning Curve:** Team may need time to master advanced Robolectric techniques
  - *Mitigation:* Build on proven patterns from current successful implementation

This plan leverages proven Robolectric effectiveness while maintaining realistic, achievable targets based on actual implementation experience.