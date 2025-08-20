#!/bin/bash

# WearInterval Time Usage Presubmit Check
# This script ensures System.currentTimeMillis() is only used in allowed locations

set -e  # Exit on any error

# Configuration
ALLOWED_FILES=(
    "app/src/main/java/com/wearinterval/util/TimeProvider.kt"
)

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

# Function to check for System.currentTimeMillis() usage
check_time_usage() {
    log_info "Checking for improper System.currentTimeMillis() usage..."
    
    local violations_found=false
    
    # Search main source files for System.currentTimeMillis()
    log_info "Checking main source files..."
    local main_results
    main_results=$(grep -rn "System\.currentTimeMillis" --include="*.kt" app/src/main/ 2>/dev/null || true)
    
    if [ -n "$main_results" ]; then
        # Check each result against allowed files
        while IFS= read -r line; do
            local file=$(echo "$line" | cut -d: -f1)
            local is_allowed=false
            
            for allowed in "${ALLOWED_FILES[@]}"; do
                if [[ "$file" == "$allowed" ]]; then
                    is_allowed=true
                    log_info "✓ Allowed usage in: $file"
                    break
                fi
            done
            
            if [ "$is_allowed" = false ]; then
                log_error "❌ VIOLATION: $line"
                log_error "   💡 Fix: Inject TimeProvider and use timeProvider.currentTimeMillis()"
                violations_found=true
            fi
        done <<< "$main_results"
    fi
    
    # Check test files and fail too
    log_info "Checking test files..."
    local test_results
    test_results=$(grep -rn "System\.currentTimeMillis" --include="*.kt" app/src/test/ 2>/dev/null || true)
    
    if [ -n "$test_results" ]; then
        log_error "❌ VIOLATIONS in test files:"
        while IFS= read -r line; do
            log_error "  $line"
        done <<< "$test_results"
        log_error "   💡 Fix: Use fixed timestamps (e.g., 1000L) or FakeTimeProvider for predictable tests"
        violations_found=true
    fi
    
    if [ "$violations_found" = true ]; then
        log_error ""
        log_error "================================="
        log_error "❌ PRESUBMIT CHECK FAILED"
        log_error "================================="
        log_error ""
        log_error "System.currentTimeMillis() usage found in disallowed locations."
        log_error ""
        log_error "Why this matters:"
        log_error "  • Makes code untestable (can't control time in tests)"
        log_error "  • Prevents proper testing of pause/resume logic"
        log_error "  • Violates dependency injection principles"
        log_error ""
        log_error "How to fix:"
        log_error "  1. Inject TimeProvider into classes that need time"
        log_error "  2. Use timeProvider.currentTimeMillis() instead"
        log_error "  3. For data models, pass timestamp as parameter"
        log_error ""
        log_error "Allowed locations:"
        for allowed in "${ALLOWED_FILES[@]}"; do
            log_error "  • $allowed"
        done
        log_error ""
        return 1
    else
        log_success ""
        log_success "================================="
        log_success "✅ PRESUBMIT CHECK PASSED"
        log_success "================================="
        log_success ""
        log_success "No improper System.currentTimeMillis() usage found."
        log_success "All time access goes through TimeProvider! 🎉"
        log_success ""
        return 0
    fi
}

# Function to display usage
usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "This script checks for improper System.currentTimeMillis() usage in Kotlin source files."
    echo "It ensures all time access goes through the injectable TimeProvider."
    echo ""
    echo "Options:"
    echo "  --help           Show this help message"
    echo ""
    echo "Exit codes:"
    echo "  0    All checks passed"
    echo "  1    Violations found or script error"
}

# Main execution
main() {
    case "${1:-}" in
        --help)
            usage
            exit 0
            ;;
        "")
            # Normal execution
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
    
    log_info "WearInterval Time Usage Presubmit Check"
    log_info "======================================="
    
    # Ensure we're in the project directory
    if [ ! -f "app/build.gradle.kts" ]; then
        log_error "Not in WearInterval project directory"
        log_error "Please run this script from the project root"
        exit 1
    fi
    
    # Run the check
    check_time_usage
}

# Execute main function
main "$@"