# WearInterval Design Specification

## Overview
WearInterval is a Wear OS interval timer application designed for fitness and workout routines. It provides multiple interfaces for timer configuration and real-time feedback through various Wear OS integration points.

## Core Functionality

### Timer Configuration
- **Laps**: Number of intervals (1-999, with 999 representing infinite)
- **Lap Duration**: Duration of each work interval (5 seconds to 10 minutes)
- **Rest Duration**: Duration between intervals (0 seconds to 10 minutes, 0 = no rest)

### Timer States
- **Stopped**: Timer is ready but not running, shows configured values
- **Running**: Timer is actively counting down during work interval
- **Resting**: Timer is actively counting down during rest interval  
- **Paused**: Timer is stopped mid-interval (can occur in both auto and manual modes)
- **Alarm Active**: Timer has reached zero and is awaiting user dismissal (manual mode only)

## User Interface Screens

### 1. Main Screen (Primary Interface)

#### Visual Elements
- **Dual Progress Rings**: 
  - Outer ring: Overall workout progress (current lap / total laps)
  - Inner ring: Current interval progress (time elapsed / interval duration)
  - Color coding: Blue outer ring, green inner ring (changes to yellow during rest)

- **Timer Display**:
  - Large countdown showing time remaining in current interval
  - Current lap indicator (e.g., "3/20" or "3" for infinite)
  - Work/Rest indicator (yellow accent during rest periods)

- **Control Buttons**:
  - Play/Pause button (green when ready/paused, context-appropriate icon)
  - Stop button (red when running, gray when stopped)

#### Behaviors
- **Tap to Start**: When stopped, play button starts the timer
- **Tap to Pause**: When running, play button pauses the timer
- **Stop Functionality**: Resets timer to beginning of current configuration
- **Visual Feedback**: Screen flashes white when notifications are enabled
- **Alarm State**: Full-screen tap target when timer reaches zero (manual mode)

#### Navigation
- **Swipe Left**: Navigate to left config screen (last 4 intervals history)
- **Swipe Right**: Navigate to right config screen (3-column picker interface)
- **Swipe Up**: Navigate to alarm/notification settings screen

### 2. Left Config Screen (History Interface)

#### Visual Elements
- **4-Item Grid**: Shows last 4 used timer configurations
- **Grid Items**: Each shows formatted configuration (e.g., "4 x 1:00 + 0:30")
- **Tap Targets**: Large, easy-to-tap grid items

#### Behaviors
- **Configuration Selection**: Tapping any grid item immediately applies that configuration
- **Automatic Navigation**: After selection, automatically returns to main screen
- **History Management**: Most recently used configurations appear first
- **Persistence**: Selected configurations persist when navigating back to main screen

#### Data Format
Each history item displays:
- Lap count (omitted if 1 lap)
- Work duration (MM:SS or SS format)
- Rest duration (if > 0, shown as "+ MM:SS")
- Example: "20 x 0:45 + 0:15", "1:30", "5 x 2:00"

### 3. Right Config Screen (Picker Interface)

#### Visual Elements
- **3-Column Layout**:
  - Left: Laps picker (1, 2, 3... 999/∞)
  - Center: Work duration picker (5s, 10s, 15s... 10m)
  - Right: Rest duration picker (0s, 5s, 10s... 10m)

- **Picker Behavior**:
  - Scroll wheels with haptic feedback
  - Selected values highlighted in green
  - Immediate updates (no debouncing)

#### Interaction Patterns
- **Scroll to Select**: Each picker responds to vertical scrolling
- **Haptic Feedback**: Light haptic on value change
- **Real-time Updates**: Configuration changes immediately update underlying state
- **Gesture Shortcuts**:
  - **Single Tap (bottom area)**: Reset to default value
    - Laps: 1
    - Work: 60 seconds
    - Rest: 0 seconds
  - **Long Press (bottom area)**: Set to common alternative
    - Laps: 999 (infinite)
    - Work: 5 minutes
    - Rest: 5 minutes

#### Value Ranges
- **Laps**: 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20, 25, 30, 40, 50, 60, 75, 100, 150, 200, 300, 500, 999
- **Durations**: 5s, 10s, 15s, 20s, 30s, 45s, 1:00, 1:15, 1:30, 2:00, 2:30, 3:00, 4:00, 5:00, 6:00, 8:00, 10:00

### 4. Alarm/Notification Settings Screen

#### Visual Elements
- **2x2 Grid Layout** of toggle buttons:
  - **Top Left**: Vibration toggle (vibration icon)
  - **Top Right**: Sound toggle (speaker icon)
  - **Bottom Left**: Flash toggle (lightning icon)
  - **Bottom Right**: Auto/Manual mode toggle (autorenew/hand icon)

#### Toggle States
- **Enabled State**: Green background, black icon
- **Disabled State**: Dark gray background, white icon
- **Auto/Manual Toggle**: Green (auto) vs blue (manual) background

#### Notification Types
- **Vibration**: 500ms vibration pulse for single alerts, continuous pattern for alarms
- **Sound**: Beep tone using system alarm stream, triple beep for completion
- **Flash**: Screen flashes white for 500ms, continuous flashing for alarms
- **Auto Mode**: Timer automatically progresses between intervals
- **Manual Mode**: Timer pauses and requires user dismissal between intervals

#### Auto vs Manual Mode Behavior

**Auto Mode (autoMode = true)**:
- **Between Intervals**: Timer automatically continues after brief notification (500ms delay)
- **User Control**: User can still manually pause/resume timer at any time using play/pause button
- **Workout Completion**: Plays triple notification, then automatically stops timer completely
- **Paused State**: Only occurs when user manually pauses during active timer

**Manual Mode (autoMode = false)**:
- **Between Intervals**: Timer automatically pauses and requires user dismissal after each interval
- **User Control**: User can still manually pause/resume timer at any time using play/pause button  
- **Workout Completion**: Timer pauses with continuous alarm until user dismisses
- **Paused State**: Occurs both from user manual pause AND automatic pause between intervals

#### Pause State Details
**Paused state can occur in BOTH modes:**
1. **User Manual Pause** (both auto & manual modes):
   - User presses play/pause button during active timer
   - State: `isPaused = true, isRunning = true` (preserves running state for resume)

2. **Automatic Pause** (manual mode only):
   - After work interval: `isPaused = true, isRunning = true, isResting = false`
   - After rest interval: `isPaused = true, isRunning = true, isResting = true`
   - After final lap: `isPaused = true, isRunning = false`
   - Continuous alarm plays until user dismisses by tapping screen

#### Alarm Dismissal
- **Full-screen tap target**: When alarm is active, entire screen becomes tap target
- **Alarm Stops**: Tapping dismisses alarm and either resumes timer or completes workout
- **Visual Indicator**: Screen shows paused state with alarm overlay

## Wear OS Integrations

### 1. Tile Integration

#### Tile States
- **Stopped State**: Shows 2x2 grid of recent configurations (last 4 used)
- **Running State**: Shows minimal control buttons and progress bar
- **Empty History**: Shows "No recent sets" message

#### Stopped Tile Behavior
- **Configuration Grid**: 4 clickable items showing recent timer setups
- **Tap to Launch**: Tapping any configuration launches app with that setup pre-selected
- **Format**: Same as left config screen (e.g., "4 x 1:00 + 0:30")

#### Running Tile Behavior
- **Progress Indicator**: Horizontal progress bar showing current interval completion
- **Control Buttons**: Two minimal circular buttons (purpose determined by current state)
- **Tap to Open**: Tapping anywhere launches main app
- **Live Updates**: Updates every second when timer is running

#### Technical Details
- **Freshness Interval**: 1000ms when running, 0ms when stopped
- **Auto-Updates**: Tile automatically refreshes during active timers
- **State Persistence**: Tile reflects current app state even when app is closed

### 2. Complication Integration

#### Complication Types Supported
- **Short Text**: Time remaining with lap indicator
- **Long Text**: Full status with context
- **Ranged Value**: Progress bar with time and lap info
- **Monochromatic Image**: Play/pause icon based on state
- **Small Image**: Play/pause icon based on state

#### Display Formats
- **Short Text Examples**:
  - Running: "45s" with title "3/20"
  - Resting: "R:30s" with title "3/20"
  - Stopped: "Ready"
  - Infinite laps: "45s" with title "3"

- **Long Text Examples**:
  - Running: "45s - Lap 3/20"
  - Resting: "Rest: 30s - Lap 3/20"
  - Stopped: "1:00 × 20" with title "Ready"

- **Ranged Value**:
  - Progress bar fills as interval progresses
  - Text shows time remaining
  - Title shows lap progress
  - Inverted progress (bar fills up, not down)

#### Update Behavior
- **Real-time Updates**: Complications update with live timer state
- **Tap to Launch**: All complications open main app when tapped
- **State Synchronization**: Always reflects current timer state

### 3. System Integration

#### Notification Management
- **Foreground Service**: Timer runs as foreground service for reliability
- **System Notification**: Persistent notification shows timer status
- **Wake Lock**: Maintains CPU wake lock during active timers

#### Audio Integration
- **Stream Selection**: Uses ALARM audio stream for beeps
- **Fallback Handling**: Falls back to NOTIFICATION stream if alarm unavailable
- **Volume Respect**: Respects system volume settings

#### Haptic Integration
- **Vibration Patterns**: 
  - Single pulse for interface feedback
  - 500ms pulse for timer alerts
  - Continuous pattern for manual mode alarms
- **Amplitude**: Uses system default vibration amplitude

### Error Handling
- **Graceful Degradation**: App functions even if audio/vibration unavailable
- **State Recovery**: App recovers timer state after system kills
- **Configuration Validation**: Invalid configurations automatically corrected

## Technical Requirements

### Performance
- **Responsive UI**: All interactions provide immediate feedback
- **Efficient Updates**: UI updates only when state actually changes
- **Battery Optimization**: Timer pauses background updates when not visible
- **Memory Management**: Proper cleanup of resources and timers

### Accessibility
- **Content Descriptions**: All interactive elements have descriptive content descriptions
- **Large Touch Targets**: All buttons meet minimum 48dp touch target size
- **High Contrast**: Sufficient color contrast for visibility
- **Haptic Feedback**: Tactile feedback for all user interactions

### Compatibility
- **Wear OS 3.0+**: Designed for modern Wear OS devices. Explore stricter (newer) requirement if it helps functionality.
- **Various Screen Sizes**: Responsive layout adapts to different watch sizes (default to pixel watch 2)
- **Round/Square Screens**: UI works on both round and square watch faces (prioritize round for pixel watch 2)
- **Hardware Variations**: Graceful handling of missing hardware features

## User Experience Principles

### Discoverability
- **Intuitive Navigation**: Swipe gestures follow platform conventions
- **Visual Hierarchy**: Clear distinction between different UI elements
- **Contextual Controls**: Controls appear when relevant and expected

### Efficiency
- **Quick Access**: Recent configurations easily accessible from tile
- **Minimal Taps**: Common tasks require minimal user interaction
- **Smart Defaults**: Sensible default values for new users

### Reliability
- **Consistent Behavior**: Same actions produce same results across contexts
- **State Persistence**: User configurations and timer state survive app restarts
- **Error Prevention**: UI prevents invalid configurations

### Feedback
- **Immediate Response**: All user actions provide immediate visual/haptic feedback
- **Clear Status**: Current timer state always clearly communicated
- **Progress Indication**: Visual progress indicators for long-running operations
