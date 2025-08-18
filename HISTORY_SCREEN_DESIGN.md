# History Screen Design Specification
## WearInterval - Last Used Settings Quick Tile

### Overview
The History Screen (accessible via swipe left from main screen) displays the 4 most recently used timer configurations in a 2x2 grid format for quick selection. This provides power users with instant access to their most common workout configurations.

### Visual Design

#### Layout Structure
- **2x2 Grid**: Four rectangular buttons arranged in a 2x2 matrix
- **Grid Spacing**: 6dp between buttons (both horizontal and vertical)
- **Container Padding**: 4dp around the entire grid
- **Button Dimensions**: 62dp wide × 35dp height per button
- **Corner Radius**: 8dp rounded corners for modern appearance

#### Color Scheme
- **Button Background**: `Color(0xFF222222)` - Dark gray for subtle contrast
- **Text Color**: `Color(0xFFBBBBBB)` - Light gray for readability
- **Empty Slots**: Transparent (no background)
- **Screen Background**: Material theme background (black)

#### Typography
- **Font Size**: 12sp for optimal readability on small buttons
- **Text Alignment**: Center aligned
- **Max Lines**: 2 lines to accommodate longer configurations
- **Font Weight**: Regular (not bold to maintain legibility at small size)

### Data Format & Display

#### Configuration Display Format
The display format follows the established wearinterval pattern:

**Format Pattern**: `[laps] x [work] + [rest]`

**Examples**:
- `"20 x 0:45 + 0:15"` - 20 laps, 45 seconds work, 15 seconds rest
- `"1:30"` - Single lap, 1 minute 30 seconds (no rest)
- `"5 x 2:00"` - 5 laps, 2 minutes work (no rest)
- `"∞ x 0:30 + 0:10"` - Infinite laps, 30 seconds work, 10 seconds rest

**Formatting Rules**:
1. **Laps**: Omit "x" prefix if only 1 lap
2. **Infinite Laps**: Display as "∞" (Unicode infinity symbol)
3. **Work Duration**: 
   - Under 60 seconds: `"45s"` format
   - 60+ seconds: `"1:30"` format (MM:SS)
4. **Rest Duration**: 
   - Show as `" + MM:SS"` or `" + SSs"` if > 0 seconds
   - Omit entirely if 0 seconds (no rest)

#### Time Formatting Function
```kotlin
private fun formatTime(duration: Duration): String {
    val totalSeconds = duration.inWholeSeconds
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    return if (minutes > 0) {
        "%d:%02d".format(minutes, seconds)
    } else {
        "${seconds}s"
    }
}
```

### User Interaction

#### Grid Population Logic
- **Position 0 (Top-Left)**: Most recently used configuration
- **Position 1 (Top-Right)**: Second most recent
- **Position 2 (Bottom-Left)**: Third most recent  
- **Position 3 (Bottom-Right)**: Fourth most recent
- **Empty Slots**: Show transparent placeholder if fewer than 4 configurations

#### Interaction Behaviors
1. **Tap Action**: 
   - Immediately apply selected configuration to current timer
   - Provide haptic feedback (`HapticFeedbackConstants.KEYBOARD_TAP`)
   - Automatically navigate back to main screen
   - Update configuration in background (no loading states needed)

2. **Empty Slot Taps**: No action (transparent areas not clickable)

3. **No History State**: 
   - Display centered message: `"No recent sets."`
   - Color: `Color.Gray`, Font: 14sp
   - No grid shown in this state

### Technical Architecture

#### Data Integration
- **Data Source**: ConfigurationRepository.getHistory() - Room database recent configurations
- **Data Limit**: Maximum 4 configurations (take(4) operation)
- **Data Ordering**: Most recently used first (descending by usage timestamp)
- **Data Persistence**: Configurations automatically saved when timer starts

#### State Management
- **ViewModel**: HistoryViewModel extends from established MVVM pattern
- **State Flow**: `StateFlow<HistoryUiState>` for reactive updates
- **Repository Integration**: Direct connection to ConfigurationRepository
- **No Local Caching**: Real-time data from repository (configurations change infrequently)

#### Events & Actions
```kotlin
sealed class HistoryEvent {
    data class ConfigurationSelected(val configuration: TimerConfiguration) : HistoryEvent()
    object RefreshHistory : HistoryEvent()
}

data class HistoryUiState(
    val configurations: List<TimerConfiguration> = emptyList(),
    val isLoading: Boolean = false
)
```

### Implementation Components

#### Required Components
1. **HistoryScreen.kt**: Main composable screen
2. **HistoryViewModel.kt**: State management and business logic  
3. **HistoryContract.kt**: Events, state, and effects definitions
4. **GridConfigurationItem.kt**: Individual button component
5. **HistoryScreenTest.kt**: Comprehensive UI testing

#### Component Hierarchy
```
HistoryScreen
├── HistoryContent
    ├── ConfigurationGrid (2x2)
    │   ├── ConfigurationGridItem [0] (Top-Left)
    │   ├── ConfigurationGridItem [1] (Top-Right)  
    │   ├── ConfigurationGridItem [2] (Bottom-Left)
    │   └── ConfigurationGridItem [3] (Bottom-Right)
    └── EmptyHistoryMessage (conditional)
```

### Navigation Integration

#### Swipe Gesture Implementation
- **Trigger**: Swipe left from main screen (established in Phase 9)
- **Gesture Threshold**: 100dp horizontal drag distance
- **Animation**: Smooth transition with proper Wear OS navigation patterns
- **Return Navigation**: Automatic return to main screen after selection

#### Navigation State Management
- **Current Implementation**: SwipeDismissableNavHost with gesture detection
- **Route**: `"history"` destination
- **Parameter Passing**: None required (configurations loaded in ViewModel)

### Accessibility & UX

#### Content Descriptions
- **Grid Items**: `"Timer configuration: [formatted string]"`
- **Empty Message**: `"No recent timer configurations"`
- **Screen Title**: `"Recent Timer Configurations"`

#### Touch Targets
- **Button Size**: 62dp × 35dp meets minimum 48dp touch target requirement
- **Spacing**: 6dp provides adequate separation to prevent accidental taps
- **Haptic Feedback**: Consistent with main screen button interactions

#### Visual Feedback
- **Selection**: No visual selection state (immediate navigation)
- **Loading**: No loading indicators (data loads quickly from local database)
- **Error Handling**: Graceful fallback to empty state if repository fails

### Testing Requirements

#### UI Component Tests
- Grid layout rendering with 4 configurations
- Grid layout with fewer than 4 configurations (empty slots)
- Empty state display when no configurations exist
- Button tap interactions and navigation
- Text formatting for all configuration variations

#### Integration Tests
- Repository data loading and display
- Configuration selection and main screen navigation
- Haptic feedback triggering
- State updates and ViewModel interactions

#### Test Coverage Targets
- **UI Tests**: 100% coverage of rendering scenarios
- **ViewModel Tests**: 100% coverage of state management
- **Integration Tests**: Repository and navigation flows
- **Formatting Tests**: All time and configuration display formats

### Performance Considerations

#### Data Loading
- **Repository Access**: Direct database query (Room is fast for small datasets)
- **Update Frequency**: Only when screen becomes visible (no real-time updates needed)
- **Memory Usage**: Minimal (4 configurations maximum)

#### Rendering Performance
- **Static Layout**: 2x2 grid doesn't require dynamic sizing
- **Text Rendering**: Pre-formatted strings, no complex calculations
- **Animation**: Minimal animations, focus on immediate responsiveness

### Implementation Priority

#### Phase 9 Integration
This feature fits into the current Phase 9 production polish:
1. **High Priority**: Completes the three-screen navigation pattern (main ↔ config ↔ history)
2. **User Experience**: Provides essential power-user functionality for quick access
3. **Design Consistency**: Matches established wearinterval patterns and current architecture

#### Development Approach
1. **Extract Current Logic**: Use existing ConfigurationRepository data
2. **Reuse Components**: Leverage established grid patterns from original wearinterval
3. **Follow Architecture**: Maintain MVVM pattern and testing standards
4. **Incremental Testing**: Build with comprehensive test coverage from start

### Success Criteria

#### Functional Requirements
- ✅ Display last 4 used configurations in 2x2 grid
- ✅ Proper text formatting matching wearinterval specifications  
- ✅ Immediate configuration application and navigation
- ✅ Graceful handling of empty history state

#### Non-Functional Requirements
- ✅ 90%+ test coverage (unit + integration + UI tests)
- ✅ Consistent with established Wear OS design patterns
- ✅ Performance: <100ms configuration loading and display
- ✅ Accessibility: Proper content descriptions and touch targets

This design document provides the foundation for implementing a production-quality history screen that enhances user productivity while maintaining the established architecture and design patterns of WearInterval.