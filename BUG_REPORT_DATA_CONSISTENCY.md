# WearInterval Data Consistency Bug Report

## Bug Description
The main screen and config screen display inconsistent timer configuration values, particularly on app launch and sometimes during configuration changes.

## Observed Behavior

### Initial App Launch
**Expected**: Main screen and config screen show identical configuration values
**Actual**: Different values displayed on each screen

Example inconsistencies observed:
- Main screen: 25 laps × 5s × 10s
- Config screen: 40 laps × 5s × 20s

### During Configuration Changes
**Expected**: Changes made on config screen immediately reflected on main screen
**Actual**: Main screen sometimes shows stale values temporarily or permanently

Example sequence:
1. Config shows: 25 laps × 10s × 10s
2. User changes duration to 20s on config screen
3. Config now shows: 25 laps × 20s × 10s
4. Main screen still shows: 25 laps × 10s × 10s (incorrect - should match config)

### After Multiple Changes
**Expected**: Both screens remain synchronized throughout usage
**Actual**: Inconsistency can persist or reappear even after successful synchronization

Example:
1. Initially inconsistent on launch
2. Make config change → screens become consistent
3. Make another config change → screens become inconsistent again

## Correct vs Incorrect States

### Correct State
- Main screen lap count = Config screen lap count
- Main screen work duration = Config screen work duration  
- Main screen rest duration = Config screen rest duration
- Changes on config screen immediately visible on main screen

### Incorrect State
- Any mismatch between main screen and config screen values
- Config changes not reflected on main screen
- Different initial values on app launch

## Frequency
- Occurs reliably on fresh app launch
- Occurs intermittently during configuration changes
- Sometimes resolves after making additional changes
- Sometimes persists until app restart