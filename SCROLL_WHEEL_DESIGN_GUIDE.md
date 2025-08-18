# Scroll Wheel Component Design Guide

## Overview

This document outlines the design principles and implementation patterns for creating smooth, flicker-free scroll wheel components in Jetpack Compose for Wear OS. It covers the journey from initial implementation through multiple failed approaches to the final working solution.

## Problem Statement

Creating scroll wheel pickers that provide smooth user interactions without visual artifacts (black flashing, stuttering, or recomposition storms) while maintaining proper state synchronization between the component and parent application state.

## Final Working Solution

### Architecture Pattern: Self-Managed Component State

```kotlin
@Composable
fun ScrollablePicker(
  items: List<String>,
  selectedIndex: Int, // Used only for initial positioning
  onSelectionChanged: (Int) -> Unit,
  title: String,
  modifier: Modifier = Modifier,
) {
  // 1. Stabilize items to prevent recreation
  val stableItems = remember(items) { items }
  
  // 2. Create self-managed picker state
  val pickerState = rememberPickerState(
    initialNumberOfOptions = stableItems.size,
    initiallySelectedOption = 0 // Always start at 0
  )
  
  // 3. One-time initialization only
  val isFirstComposition = remember { mutableStateOf(true) }
  if (isFirstComposition.value) {
    LaunchedEffect(Unit) {
      if (selectedIndex in 0 until stableItems.size) {
        pickerState.scrollToOption(selectedIndex)
      }
      isFirstComposition.value = false
    }
  }
  
  // 4. Debounced callback to prevent recomposition storms
  LaunchedEffect(pickerState.selectedOption) {
    if (!isFirstComposition.value) {
      delay(200) // Critical: debounce rapid changes
      onSelectionChanged(pickerState.selectedOption)
    }
  }
  
  // 5. Use official Wear OS Picker component
  Picker(
    state = pickerState,
    contentDescription = "Select ${title.ifEmpty { "value" }}",
    onSelected = { 
      view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) 
    },
    option = { optionIndex ->
      Text(
        text = stableItems[optionIndex],
        style = MaterialTheme.typography.body2,
        textAlign = TextAlign.Center
      )
    }
  )
}
```

### Key Design Principles

#### 1. **Single Source of Truth: Internal State**
- The picker component owns its scroll state completely
- External `selectedIndex` is only used for initial positioning
- No ongoing synchronization between external and internal state

#### 2. **One-Time Initialization**
- Set initial position once during first composition
- Use `isFirstComposition` flag to prevent repeated initialization
- Ignore subsequent external `selectedIndex` changes

#### 3. **Debounced Callbacks**
- 200ms delay before firing `onSelectionChanged`
- Prevents callback storms during rapid scrolling
- Allows smooth internal state changes without external interference

#### 4. **Stable Data References**
- Use `remember(items)` to prevent unnecessary list recreation
- Stabilizes component against parent recompositions
- Reduces composition frequency

#### 5. **Official Component Usage**
- Leverage `androidx.wear.compose.material.Picker`
- Provides built-in accessibility, rotary input, and animations
- Avoid custom scroll implementations

## Failed Approaches and Lessons Learned

### ❌ Approach 1: Reactive State Synchronization

**What we tried:**
```kotlin
// Bidirectional sync between external and internal state
LaunchedEffect(selectedIndex) {
  if (selectedIndex != pickerState.selectedOption) {
    pickerState.animateScrollToOption(selectedIndex)
  }
}

LaunchedEffect(pickerState.selectedOption) {
  onSelectionChanged(pickerState.selectedOption)
}
```

**What went wrong:**
- Created feedback loop: external state → picker sync → callback → new external state → repeat
- During sync delay (~500ms), picker showed black background
- Caused excessive recompositions and visual flashing

**Lesson:** Bidirectional reactive state sync creates unstable feedback loops in scroll components.

### ❌ Approach 2: Custom LazyColumn Implementation

**What we tried:**
```kotlin
LazyColumn(
  state = listState,
  flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
) {
  item { Spacer(height = 60.dp) } // Top padding
  itemsIndexed(items, key = { index, item -> "$index-$item" }) { ... }
  item { Spacer(height = 60.dp) } // Bottom padding
}
```

**What went wrong:**
- Over-engineering: reimplementing wheel behavior, accessibility, rotary input
- Missing Wear OS-specific features (proper haptics, scaling, animations)
- Added complexity without solving root state management issues
- Maintenance burden for custom scroll physics

**Lesson:** Don't reinvent platform components when the issue is architectural, not functional.

### ❌ Approach 3: Reduced Synchronization Threshold

**What we tried:**
```kotlin
// Only sync on "large" differences to reduce feedback
val diff = abs(selectedIndex - pickerState.selectedOption)
if (diff > 3 && selectedIndex in 0 until items.size) {
  pickerState.animateScrollToOption(selectedIndex)
}
```

**What went wrong:**
- Still maintained bidirectional sync, just with conditions
- Reduced frequency but didn't eliminate the root cause
- Created confusing behavior where some external changes were ignored
- Black flashing still occurred during qualifying sync operations

**Lesson:** Conditional fixes don't solve fundamental architectural problems.

### ❌ Approach 4: Shorter Debounce Timing

**What we tried:**
```kotlin
LaunchedEffect(pickerState.selectedOption) {
  delay(50) // Too short
  onSelectionChanged(pickerState.selectedOption)
}
```

**What went wrong:**
- 50ms wasn't enough to prevent callback storms
- Still triggered rapid recomposition cycles
- Parent components couldn't keep up with high-frequency updates

**Lesson:** Debounce timing must account for the full parent recomposition cycle time.

## Performance Considerations

### Recomposition Optimization

1. **Minimize Parent Recompositions**
   - Debounce callbacks to reduce update frequency
   - Use `remember` for expensive calculations in parent
   - Avoid unnecessary state propagation

2. **Stable Component Props**
   - Use `remember(items)` to stabilize lists
   - Avoid recreating display items on every recomposition
   - Consider `rememberUpdatedState` for callbacks

3. **Composition Scope Isolation**
   - Keep picker state changes isolated within component
   - Prevent state changes from triggering parent recomposition
   - Use `LaunchedEffect` with appropriate keys

### Memory and Resource Management

```kotlin
// Good: Stable item references
val stableItems = remember(items) { items }

// Bad: Recreated on every composition
val displayItems = items.map { formatDisplayText(it) }

// Good: Memoized display formatting
val displayItems = remember(items) { 
  items.map { formatDisplayText(it) } 
}
```

## Testing Strategies

### Integration Testing

```kotlin
@Test
fun scrollWheelUpdatesParentState() {
  // Arrange: Set up component with initial state
  var selectedValue = 0
  composeTestRule.setContent {
    ScrollablePicker(
      items = listOf("A", "B", "C"),
      selectedIndex = selectedValue,
      onSelectionChanged = { selectedValue = it }
    )
  }
  
  // Act: Perform scroll gesture
  composeTestRule.onNodeWithContentDescription("Select value")
    .performTouchInput { swipeUp() }
  
  // Wait for debounce
  composeTestRule.waitForIdle()
  advanceTimeBy(250)
  
  // Assert: Parent state updated
  assertEquals(1, selectedValue)
}
```

### Performance Testing

```kotlin
@Test
fun scrollWheelDoesNotCauseExcessiveRecompositions() {
  var recompositionCount = 0
  
  composeTestRule.setContent {
    SideEffect { recompositionCount++ }
    
    ScrollablePicker(
      items = heavyItemsList,
      selectedIndex = 0,
      onSelectionChanged = { }
    )
  }
  
  // Perform multiple rapid scrolls
  repeat(10) {
    composeTestRule.onNodeWithContentDescription("Select value")
      .performTouchInput { swipeUp() }
  }
  
  // Verify bounded recomposition count
  assertThat(recompositionCount).isLessThan(expectedThreshold)
}
```

## Debugging Techniques

### Composition Tracking

```kotlin
// Add logging to track composition frequency
@Composable
fun ScrollablePicker(...) {
  val componentId = remember { "ScrollablePicker-${UUID.randomUUID()}" }
  
  Log.d("ScrollablePicker", "[$componentId] COMPOSITION START")
  
  SideEffect {
    Log.d("ScrollablePicker", "[$componentId] SIDE_EFFECT - state=${pickerState.selectedOption}")
  }
  
  // ... component implementation
}
```

### State Synchronization Monitoring

```kotlin
// Track state mismatches
LaunchedEffect(selectedIndex, pickerState.selectedOption) {
  if (selectedIndex != pickerState.selectedOption) {
    Log.w("ScrollablePicker", "STATE MISMATCH: external=$selectedIndex, internal=${pickerState.selectedOption}")
  }
}
```

## Common Pitfalls

### 1. **Reactive State Anti-Patterns**
```kotlin
// ❌ Don't do this - creates feedback loops
LaunchedEffect(externalState) {
  internalState.update(externalState)
}
LaunchedEffect(internalState) {
  updateExternalState(internalState)
}
```

### 2. **Insufficient Debouncing**
```kotlin
// ❌ Too fast - causes recomposition storms
LaunchedEffect(pickerState.selectedOption) {
  delay(50) // Not enough time
  callback(pickerState.selectedOption)
}

// ✅ Proper debouncing
LaunchedEffect(pickerState.selectedOption) {
  delay(200) // Allows parent recomposition to complete
  callback(pickerState.selectedOption)
}
```

### 3. **Unstable References**
```kotlin
// ❌ Recreates list on every composition
fun ScrollablePicker(items: List<String>) {
  val displayItems = items.map { formatText(it) }
  
// ✅ Stable references
fun ScrollablePicker(items: List<String>) {
  val displayItems = remember(items) { items.map { formatText(it) } }
```

### 4. **Over-Engineering Solutions**
- Don't build custom scroll components when platform components exist
- Don't add complex state management when simple patterns work
- Don't optimize prematurely - measure first

## Migration Guide

### From Bidirectional Sync to Self-Managed State

1. **Remove external state synchronization:**
   ```kotlin
   // Remove this
   LaunchedEffect(selectedIndex) {
     pickerState.animateScrollToOption(selectedIndex)
   }
   ```

2. **Add one-time initialization:**
   ```kotlin
   val isFirstComposition = remember { mutableStateOf(true) }
   if (isFirstComposition.value) {
     LaunchedEffect(Unit) {
       pickerState.scrollToOption(selectedIndex)
       isFirstComposition.value = false
     }
   }
   ```

3. **Add callback debouncing:**
   ```kotlin
   LaunchedEffect(pickerState.selectedOption) {
     if (!isFirstComposition.value) {
       delay(200)
       onSelectionChanged(pickerState.selectedOption)
     }
   }
   ```

4. **Stabilize item references:**
   ```kotlin
   val stableItems = remember(items) { items }
   ```

## Conclusion

Creating smooth scroll wheel components requires careful attention to state management patterns and composition optimization. The key insight is that scroll components work best with self-managed internal state and minimal external synchronization.

**Success Factors:**
- Single source of truth (internal state)
- One-time initialization only
- Proper callback debouncing (200ms)
- Stable component references
- Use of official platform components

**Avoid:**
- Bidirectional reactive state synchronization
- Custom scroll implementations
- Insufficient debouncing
- Unstable prop references
- Over-engineering solutions

This pattern has been validated to eliminate visual flashing and provide smooth user interactions across different Wear OS devices and usage patterns.