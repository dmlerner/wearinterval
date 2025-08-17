# Headless Emulator Testing Setup

This document explains how to run WearInterval's instrumented tests headlessly for comprehensive UI coverage.

## Quick Start

### Prerequisites
1. **Android SDK** installed with `ANDROID_HOME` environment variable set
2. **Wear OS system image** for API 30: 
   ```bash
   sdkmanager "system-images;android-30;google_apis;x86_64"
   ```
3. **Hardware acceleration** enabled (KVM on Linux, HAXM on Windows/Mac)

### One-Command Execution
```bash
./scripts/headless-test.sh
```

This will:
- ✅ Create Wear OS emulator AVD
- ✅ Start headless emulator  
- ✅ Run all instrumented tests
- ✅ Generate combined coverage report
- ✅ Clean up automatically

## Manual Steps

### 1. Create AVD
```bash
avdmanager create avd -n WearOS_Headless -k "system-images;android-30;google_apis;x86_64" --device "wear_round"
```

### 2. Start Headless Emulator
```bash
emulator -avd WearOS_Headless -no-window -no-audio -no-boot-anim -accel on -gpu swiftshader_indirect &
```

### 3. Wait for Boot
```bash
adb wait-for-device
adb shell getprop sys.boot_completed  # Should return "1"
```

### 4. Run Tests
```bash
./gradlew connectedDebugAndroidTest
./gradlew createDebugCoverageReport
```

## Script Options

```bash
# Setup only (create AVD, don't run tests)
./scripts/headless-test.sh --setup-only

# Run tests only (assume emulator running)  
./scripts/headless-test.sh --tests-only

# Generate coverage report only
./scripts/headless-test.sh --coverage-only

# Clean up AVD
./scripts/headless-test.sh --cleanup

# Show help
./scripts/headless-test.sh --help
```

## Expected Results

### Current Test Suite
- **MainScreenTest**: 12 UI interaction tests
- **DataStoreIntegrationTest**: 10 persistence tests
- **ConfigurationDaoTest**: Database integration tests
- **ProgressRingComposeTest**: UI component tests

### Coverage Improvement
- **Before**: 25% overall (excellent unit test coverage)
- **After**: 65-75% overall (includes UI Composables)
- **Files tested**: All UI screens, data persistence, database operations

### Artifacts
- **Test Results**: `app/build/reports/androidTests/connected/`
- **Coverage Report**: `app/build/reports/coverage/androidTest/debug/`
- **JaCoCo XML**: `app/build/reports/coverage/androidTest/debug/report.xml`

## Troubleshooting

### Emulator Won't Start
```bash
# Check virtualization
ls /dev/kvm  # Should exist on Linux

# Check system images
avdmanager list target

# Increase memory
emulator -avd WearOS_Headless -memory 2048 -partition-size 2048
```

### Tests Fail to Connect
```bash
# Restart ADB
adb kill-server
adb start-server

# Check devices
adb devices

# Check emulator status
adb shell getprop ro.build.version.release
```

### Performance Issues
```bash
# Use faster CPU
emulator -avd WearOS_Headless -accel on

# Disable animations
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

## CI/CD Integration

### GitHub Actions
The provided `.github/workflows/instrumented-tests.yml` includes:
- ✅ Emulator caching for faster CI
- ✅ Coverage reporting with Codecov
- ✅ PR comments with coverage diff
- ✅ Artifact upload for test results

### Environment Variables
```bash
export ANDROID_HOME="/path/to/android/sdk"
export PATH="$ANDROID_HOME/tools/bin:$PATH"
export PATH="$ANDROID_HOME/emulator:$PATH"  
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

## Performance Optimization

### For Local Development
- Use `--tests-only` to skip AVD creation
- Keep emulator running between test runs
- Use SSD storage for faster I/O

### For CI
- Cache AVD snapshots
- Use KVM acceleration on Linux
- Parallel test execution with sharding

## Integration with Coverage Tools

### JaCoCo Integration
The project includes JaCoCo configuration that combines:
- Unit test coverage
- Instrumented test coverage
- Combined HTML and XML reports

### Coverage Thresholds
- **Overall**: 25% minimum (current excellent unit coverage)
- **Business Logic**: 90%+ (already achieved)
- **UI Components**: Target 60%+ with instrumented tests

## Next Steps

1. **Run Initial Test**: `./scripts/headless-test.sh`
2. **Review Coverage**: Open `app/build/reports/coverage/androidTest/debug/index.html`
3. **Add More UI Tests**: Enhance test coverage for specific user flows
4. **Optimize CI**: Set up GitHub Actions for automated testing

This setup provides comprehensive testing without requiring manual emulator interaction, making it ideal for CI/CD pipelines and automated testing workflows.