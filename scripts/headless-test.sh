#!/bin/bash

# WearInterval Headless Emulator Testing Script
# This script sets up and runs instrumented tests on a headless Android emulator

set -e  # Exit on any error

# Configuration
AVD_NAME="WearOS_Headless_CI"
SYSTEM_IMAGE="system-images;android-30;android-wear;x86"
DEVICE_PROFILE="wear_round"
EMULATOR_TIMEOUT=300  # 5 minutes timeout
TEST_TIMEOUT=600      # 10 minutes timeout

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if Android SDK is available
check_android_sdk() {
    log_info "Checking Android SDK setup..."
    
    if [ -z "$ANDROID_HOME" ]; then
        log_error "ANDROID_HOME environment variable is not set"
        log_info "Please set ANDROID_HOME to your Android SDK directory"
        return 1
    fi
    
    if [ ! -d "$ANDROID_HOME" ]; then
        log_error "ANDROID_HOME directory does not exist: $ANDROID_HOME"
        return 1
    fi
    
    # Check for required tools
    local tools_missing=false
    
    if ! command -v avdmanager &> /dev/null; then
        log_error "avdmanager not found. Please ensure Android SDK tools are in PATH"
        tools_missing=true
    fi
    
    if ! command -v emulator &> /dev/null; then
        log_error "emulator not found. Please ensure Android SDK emulator is in PATH"
        tools_missing=true
    fi
    
    if ! command -v adb &> /dev/null; then
        log_error "adb not found. Please ensure Android SDK platform-tools are in PATH"
        tools_missing=true
    fi
    
    if [ "$tools_missing" = true ]; then
        log_info "Add these to your PATH:"
        log_info "  export PATH=\$ANDROID_HOME/tools/bin:\$PATH"
        log_info "  export PATH=\$ANDROID_HOME/emulator:\$PATH"
        log_info "  export PATH=\$ANDROID_HOME/platform-tools:\$PATH"
        return 1
    fi
    
    log_success "Android SDK tools found"
    return 0
}

# Function to check if system image is installed
check_system_image() {
    log_info "Checking if Wear OS system image is installed..."
    
    # Check for the specific Wear OS system image
    if [ -d "$ANDROID_HOME/system-images/android-30/android-wear/x86" ]; then
        log_success "Wear OS system image found at android-30/android-wear/x86"
        return 0
    else
        log_warning "Wear OS system image not found"
        log_info "Install it with: sdkmanager \"$SYSTEM_IMAGE\""
        return 1
    fi
}

# Function to create AVD if it doesn't exist
create_avd() {
    log_info "Checking if AVD '$AVD_NAME' exists..."
    
    if avdmanager list avd | grep -q "Name: $AVD_NAME"; then
        log_info "AVD '$AVD_NAME' already exists"
        return 0
    fi
    
    log_info "Creating AVD '$AVD_NAME'..."
    echo "no" | avdmanager create avd \
        -n "$AVD_NAME" \
        -k "$SYSTEM_IMAGE" \
        --device "$DEVICE_PROFILE" \
        --force
    
    if [ $? -eq 0 ]; then
        log_success "AVD '$AVD_NAME' created successfully"
        return 0
    else
        log_error "Failed to create AVD"
        return 1
    fi
}

# Function to start emulator headlessly
start_emulator() {
    log_info "Starting headless emulator..."
    
    # Kill any existing emulator processes
    pkill -f "emulator.*$AVD_NAME" 2>/dev/null || true
    
    # Wait a moment for processes to terminate
    sleep 2
    
    # Start emulator in background
    emulator -avd "$AVD_NAME" \
        -no-window \
        -no-audio \
        -no-boot-anim \
        -accel on \
        -gpu swiftshader_indirect \
        -memory 2048 \
        -partition-size 2048 \
        -wipe-data &
    
    local emulator_pid=$!
    log_info "Emulator started with PID: $emulator_pid"
    
    # Wait for emulator to boot
    log_info "Waiting for emulator to boot (timeout: ${EMULATOR_TIMEOUT}s)..."
    
    local elapsed=0
    while [ $elapsed -lt $EMULATOR_TIMEOUT ]; do
        if adb shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
            log_success "Emulator booted successfully"
            return 0
        fi
        
        sleep 5
        elapsed=$((elapsed + 5))
        
        if [ $((elapsed % 30)) -eq 0 ]; then
            log_info "Still waiting... (${elapsed}s elapsed)"
        fi
        
        # Check if emulator process is still running
        if ! kill -0 $emulator_pid 2>/dev/null; then
            log_error "Emulator process died"
            return 1
        fi
    done
    
    log_error "Emulator failed to boot within timeout"
    kill $emulator_pid 2>/dev/null || true
    return 1
}

# Function to run instrumented tests
run_instrumented_tests() {
    log_info "Running instrumented tests..."
    
    # Ensure we're in the project directory
    if [ ! -f "app/build.gradle.kts" ]; then
        log_error "Not in WearInterval project directory"
        return 1
    fi
    
    # Run instrumented tests with timeout
    log_info "Executing: ./gradlew connectedDebugAndroidTest"
    
    timeout $TEST_TIMEOUT ./gradlew connectedDebugAndroidTest
    
    if [ $? -eq 0 ]; then
        log_success "Instrumented tests completed successfully"
        return 0
    else
        log_error "Instrumented tests failed"
        return 1
    fi
}

# Function to generate coverage report
generate_coverage_report() {
    log_info "Generating combined coverage report..."
    
    ./gradlew createDebugCoverageReport
    
    if [ $? -eq 0 ]; then
        log_success "Coverage report generated"
        
        # Try to find and display coverage summary
        local coverage_file="app/build/reports/coverage/androidTest/debug/index.html"
        if [ -f "$coverage_file" ]; then
            log_info "Coverage report available at: $coverage_file"
        fi
        
        return 0
    else
        log_error "Failed to generate coverage report"
        return 1
    fi
}

# Function to clean up emulator
cleanup_emulator() {
    log_info "Cleaning up emulator..."
    
    # Kill emulator processes
    pkill -f "emulator.*$AVD_NAME" 2>/dev/null || true
    
    # Kill adb server
    adb kill-server 2>/dev/null || true
    
    log_info "Cleanup completed"
}

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  --setup-only     Only set up AVD, don't run tests"
    echo "  --tests-only     Only run tests (assume emulator is running)"
    echo "  --coverage-only  Only generate coverage report"
    echo "  --cleanup        Clean up and remove AVD"
    echo "  --help           Show this help message"
    echo ""
    echo "Environment Variables:"
    echo "  ANDROID_HOME     Path to Android SDK (required)"
    echo "  AVD_NAME         Name for the AVD (default: $AVD_NAME)"
    echo "  EMULATOR_TIMEOUT Emulator boot timeout in seconds (default: $EMULATOR_TIMEOUT)"
    echo "  TEST_TIMEOUT     Test execution timeout in seconds (default: $TEST_TIMEOUT)"
}

# Function to clean up AVD
cleanup_avd() {
    log_info "Cleaning up AVD '$AVD_NAME'..."
    
    cleanup_emulator
    
    if avdmanager list avd | grep -q "Name: $AVD_NAME"; then
        avdmanager delete avd -n "$AVD_NAME"
        log_success "AVD '$AVD_NAME' deleted"
    else
        log_info "AVD '$AVD_NAME' does not exist"
    fi
}

# Main execution
main() {
    log_info "WearInterval Headless Emulator Testing Script"
    log_info "============================================="
    
    # Parse command line arguments
    case "${1:-}" in
        --setup-only)
            check_android_sdk || exit 1
            check_system_image || exit 1
            create_avd || exit 1
            log_success "Setup completed successfully"
            exit 0
            ;;
        --tests-only)
            run_instrumented_tests || exit 1
            generate_coverage_report || exit 1
            exit 0
            ;;
        --coverage-only)
            generate_coverage_report || exit 1
            exit 0
            ;;
        --cleanup)
            cleanup_avd
            exit 0
            ;;
        --help)
            usage
            exit 0
            ;;
        "")
            # Full execution
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
    
    # Trap to ensure cleanup on exit
    trap cleanup_emulator EXIT
    
    # Full execution flow
    log_info "Starting full headless testing workflow..."
    
    # Check prerequisites
    check_android_sdk || exit 1
    check_system_image || exit 1
    
    # Create AVD
    create_avd || exit 1
    
    # Start emulator
    start_emulator || exit 1
    
    # Run tests
    run_instrumented_tests || exit 1
    
    # Generate coverage
    generate_coverage_report || exit 1
    
    log_success "✅ All tests completed successfully!"
    log_info "📊 Check coverage report in app/build/reports/"
    log_info "🧪 Instrumented test results in app/build/reports/androidTests/"
}

# Execute main function
main "$@"