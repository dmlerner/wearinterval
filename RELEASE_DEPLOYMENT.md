# Release Deployment Guide

## Quick Release Build & Deploy

### Prerequisites
```bash
# Ensure gradle.properties has signing config
echo "android.injected.signing.store.file=$HOME/.android/debug.keystore" > gradle.properties
echo "android.injected.signing.store.password=android" >> gradle.properties  
echo "android.injected.signing.key.alias=androiddebugkey" >> gradle.properties
echo "android.injected.signing.key.password=android" >> gradle.properties
echo "android.useAndroidX=true" >> gradle.properties
```

### Build & Deploy
```bash
# Clean build with all optimizations
./gradlew clean assembleRelease

# Deploy to watch
adb devices  # Verify watch connected
adb -s <WATCH_IP>:43827 uninstall com.wearinterval  # Clean install
adb -s <WATCH_IP>:43827 install app/build/outputs/apk/release/app-release.apk
```

## Release Optimizations Included

- **R8 minification** - Code shrinking and obfuscation
- **ProGuard optimization** - Dead code elimination  
- **ART profile compilation** - Runtime performance optimization
- **Resource compression** - Optimized assets and resources
- **Native library stripping** - Reduced APK size

## Debug vs Release Performance

| Feature | Debug | Release |
|---------|--------|---------|
| Code optimization | None | R8 + ProGuard |
| Native libraries | Full symbols | Stripped |
| Resources | Uncompressed | Compressed |
| ART profiles | None | Compiled |
| Logging | Enabled | Removed |

**Always use release builds for performance testing.**