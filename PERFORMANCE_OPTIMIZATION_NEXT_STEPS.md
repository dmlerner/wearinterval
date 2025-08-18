# WearInterval Performance Optimization - Next Steps

## Current Performance Analysis

Based on comprehensive profiling and analysis of the WearOS app performance issues:

### Performance Issues Identified
- **25.83% janky frames** (31 out of 120 frames)
- **Persistent 50ms Draw times** (should be ~16ms)
- **Frame times up to 3700ms** in extreme cases
- **Asymmetric navigation performance**: main→config slow, config→main fast
- **139 high input latency events** during testing
- **30 slow UI thread events** indicating blocking operations

### Root Causes Found and Fixed
✅ **Blocking ViewModel operations**: Removed unnecessary `collect` operations in MainViewModel and ConfigViewModel init blocks  
✅ **Excessive debug logging**: Implemented centralized DebugLogger with BuildConfig.DEBUG flag control  
✅ **Runtime list creation**: Pre-computed display lists (85+ items) moved from composition time to compile time  
✅ **Blocking DataStore access**: Replaced `.first()` calls with `.value` for non-blocking StateFlow access  

### Current Status
**Improvement**: "A little better but not good enough" - indicates major bottlenecks remain despite fixes.

## Highest ROI Next Steps (Prioritized)

### 🎯 **1. HorizontalPager Lazy Loading Replacement (HIGHEST ROI)**

**Priority**: ⭐⭐⭐⭐⭐ **CRITICAL - START HERE**

**Problem Analysis**:
- HorizontalPager with `beyondBoundsPageCount = 0` still pre-composes adjacent pages
- ConfigScreen contains 3 active ScrollablePicker components
- All 4 screens (History, Main, Config, Settings) + their ViewModels initialize simultaneously
- Asymmetric performance (main→config slow, config→main fast) confirms pre-loading issue

**Technical Details**:
```kotlin
// Current problematic approach
HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
    when (page) {
        0 -> HistoryScreen()     // Always composed
        1 -> MainScreen()        // Always composed  
        2 -> ConfigScreen()      // Always composed - 3 ScrollablePickers!
        3 -> SettingsScreen()    // Always composed
    }
}
```

**Proposed Solution**:
Replace HorizontalPager with gesture-based navigation that only composes the current screen:

```kotlin
@Composable
fun WearIntervalNavigation() {
    var currentScreen by remember { mutableStateOf(Screen.Main) }
    
    Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
        detectDragGestures { change, _ ->
            // Handle swipe gestures to change currentScreen
        }
    }) {
        when (currentScreen) {
            Screen.Main -> MainScreen()
            Screen.Config -> ConfigScreen() // Only composed when visible
            // etc.
        }
    }
}
```

**Expected Impact**:
- **75% reduction in simultaneous composition** (only 1 of 4 screens)
- **Eliminates ConfigScreen pre-loading** with 3 ScrollablePickers
- **Reduces ViewModel initialization overhead** by 75%
- **Should eliminate 50ms Draw times** during navigation

**Implementation Steps**:
1. Create custom gesture navigation composable
2. Implement swipe detection with proper thresholds
3. Add smooth transition animations
4. Test performance improvement
5. Fine-tune gesture sensitivity

**Risk**: Low - can easily rollback to HorizontalPager if issues arise

---

### ⚡ **2. ScrollablePicker Performance Optimization (HIGH ROI)**

**Priority**: ⭐⭐⭐⭐ **HIGH - After #1**

**Problem Analysis**:
- Each ScrollablePicker uses Wear OS `Picker` component (complex scrolling widget)
- ConfigScreen has 3 active pickers = 3x overhead
- Each picker has multiple reactive operations:
  - `LaunchedEffect(pickerState.selectedOption)` with 100ms debounce
  - `remember()` calls for state management
  - `SideEffect` for logging
- Potential 300ms cumulative delay (3 pickers × 100ms debounce)

**Current Implementation Issues**:
```kotlin
// Each picker creates expensive reactive operations
LaunchedEffect(pickerState.selectedOption) {
    if (!isFirstComposition.value) {
        kotlinx.coroutines.delay(100) // Debounce
        onSelectionChanged(pickerState.selectedOption)
    }
}
```

**Proposed Solutions**:

**Option A: Custom Lightweight Picker**
```kotlin
@Composable
fun LightweightPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(selectedIndex),
        modifier = Modifier.height(120.dp)
    ) {
        itemsIndexed(items) { index, item ->
            Text(
                text = item,
                modifier = Modifier.clickable { onSelectionChanged(index) }
            )
        }
    }
}
```

**Option B: Optimize Existing Pickers**
- Reduce debounce from 100ms to 50ms
- Remove unnecessary `SideEffect` and `remember` calls
- Use `derivedStateOf` for computed values
- Implement picker value caching

**Expected Impact**:
- **60% reduction in ConfigScreen composition overhead**
- **Elimination of scroll wheel lag**
- **Faster picker interactions**
- **Reduced memory allocations**

**Implementation Priority**: After HorizontalPager replacement

---

### 🔧 **3. Repository StateFlow Optimization (MEDIUM ROI)**

**Priority**: ⭐⭐⭐ **MEDIUM - After #1 and #2**

**Problem Analysis**:
```kotlin
// ConfigurationRepositoryImpl has multiple StateFlow subscriptions
override val currentConfiguration: StateFlow<TimerConfiguration> =
    dataStoreManager.currentConfiguration.stateIn(
        scope = repositoryScope,
        started = SharingStarted.Eagerly, // Always active
        initialValue = TimerConfiguration.DEFAULT
    )

override val recentConfigurations: StateFlow<List<TimerConfiguration>> =
    configurationDao.getRecentConfigurationsFlow(4)
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(
            scope = repositoryScope,
            started = SharingStarted.WhileSubscribed(5000), // Different strategy
            initialValue = emptyList(),
        )
```

**Issues**:
- Mixed sharing strategies (`Eagerly` vs `WhileSubscribed`)
- Database queries running continuously
- Potential StateFlow cascade effects during composition

**Proposed Optimizations**:

1. **Standardize sharing strategies**:
```kotlin
// Use WhileSubscribed for all non-critical StateFlows
started = SharingStarted.WhileSubscribed(5000)
```

2. **Optimize database queries**:
```kotlin
// Cache recent configurations, only update when needed
private val _recentConfigurations = MutableStateFlow<List<TimerConfiguration>>(emptyList())
```

3. **Reduce StateFlow subscriptions**:
- Combine related flows to reduce individual subscriptions
- Use `distinctUntilChanged()` to prevent unnecessary emissions

**Expected Impact**:
- **20% reduction in background processing**
- **Fewer composition triggers**
- **Reduced database query frequency**
- **Lower memory pressure**

---

## 🛠️ **4. Additional Optimizations (LOWER ROI)**

### A. Release Build Configuration
**Impact**: ⭐⭐ **LOW ROI** (but free)
- Configure signing for release builds
- R8 optimization will provide 10-20% performance improvement
- Automatic logging removal

### B. Compose Compiler Optimizations
**Impact**: ⭐⭐ **LOW ROI**
- Enable strong skipping mode
- Optimize recomposition boundaries
- Add `@Stable` annotations where appropriate

### C. Memory and GC Optimization
**Impact**: ⭐⭐ **LOW ROI**
- Object pooling for frequently created objects
- Reduce allocations in hot paths
- Optimize string operations

---

## 📊 **Success Metrics**

### Target Performance Goals:
- **< 5% janky frames** (currently 25.83%)
- **< 20ms average frame time** (currently 50ms)
- **< 100ms navigation transitions** (currently 500ms+)
- **< 10 slow UI thread events** (currently 30)
- **< 50 high input latency events** (currently 139)

### Testing Methodology:
1. **Before/after profiling** with `dumpsys gfxinfo`
2. **Manual navigation testing** on physical device
3. **GPU profiling bars** for visual confirmation
4. **Method tracing** for detailed analysis

---

## 🚀 **Implementation Roadmap**

### Week 1: HorizontalPager Replacement
- [ ] Implement custom gesture navigation
- [ ] Replace HorizontalPager with lazy screen composition
- [ ] Performance testing and validation
- [ ] **Expected Result**: 70% improvement in navigation performance

### Week 2: ScrollablePicker Optimization  
- [ ] Evaluate lightweight picker vs. optimization
- [ ] Implement chosen solution
- [ ] Performance testing
- [ ] **Expected Result**: Smooth config screen interactions

### Week 3: Repository Optimization + Polish
- [ ] Optimize StateFlow sharing strategies
- [ ] Configure release build
- [ ] Final performance validation
- [ ] **Expected Result**: Production-ready performance

---

## 📝 **Notes for Implementation**

### Critical Success Factors:
1. **Test incrementally**: One optimization at a time with profiling between each
2. **Measure impact**: Use `dumpsys gfxinfo` before/after each change
3. **Maintain functionality**: Ensure all features work after optimizations
4. **User experience**: Navigation should feel native and responsive

### Risk Mitigation:
- Keep HorizontalPager implementation as fallback
- Implement feature flags for new navigation
- Extensive testing on physical device before deployment

### Performance Monitoring:
- Set up continuous performance monitoring
- Alert on performance regressions
- Regular profiling on different device types

---

**Last Updated**: 2025-08-18  
**Status**: Ready for implementation - Start with HorizontalPager replacement  
**Priority**: CRITICAL - Address navigation performance first for maximum user impact