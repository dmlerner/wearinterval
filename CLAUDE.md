# WearInterval - Claude Maintenance Guide

## ⚠️ IMPORTANT: This is a SAPLING repository - NEVER use git commands!
**Always use `sl` (Sapling) commands instead of `git`. Using git will break the repository.**

## Quick Start for Claude
When working on this **completed MVP**, read these files to understand the implemented features and architecture:

### 1. Read Project Foundation (Required)
```
@FEATURE_SPECIFICATION.md - Complete feature specification (implemented)
@ARCHITECTURE_AND_PRACTICES.md - Technical architecture and patterns (as-built)
```

### 2. Understand Current State
- **MVP is complete** with all core features implemented
- **Production-ready** Wear OS interval timer application
- **Comprehensive test coverage** (90%+) across all layers
- **All planned features** from specification are functional

### 3. Maintenance Standards
- **90% test coverage minimum** for all code changes
- **Test existing functionality** before making changes
- **Maximum 20 lines per function**
- **Write tests for any new behaviors**

## Project Overview
**WearInterval** is a **completed** Wear OS interval timer application with:
- Multiple timer configuration interfaces (manual, presets, history)
- Full Wear OS integration (tiles, complications, notifications)
- Robust state management and persistence (Room + DataStore)
- Production-quality architecture with comprehensive test coverage
- All core features from specification implemented and tested

## Architecture Summary
- **MVVM + Repository Pattern** with strict separation of concerns
- **3 Layers**: UI (Compose) → Domain (Repositories) → Data (Service/Room/DataStore)
- **Unidirectional Data Flow** via StateFlow
- **Dependency Injection** with Hilt
- **Comprehensive Testing** at all layers

## Implemented Features
- **Interval Timer Engine** - Precise timing with foreground service
- **Configuration Management** - Manual setup, presets, history persistence
- **Wear OS Integration** - Tiles, complications, notifications
- **Progress Visualization** - Dual-ring progress display
- **State Management** - Comprehensive pause/resume/stop functionality
- **Background Operation** - Timers continue when app minimized

## Maintenance Workflow
1. **Run existing tests** to ensure stability: `./gradlew test`
2. **Check current coverage**: `./gradlew jacocoTestReport`
3. **Write tests for changes** before implementing
4. **Follow established patterns** from architecture document
5. **Maintain 90%+ coverage** for any new code
6. **Test on device/emulator** for Wear OS specific features

## Existing Test Coverage
- **Unit Tests**: ViewModels, Repositories, Data Models (90%+ coverage achieved)
- **Integration Tests**: DAOs, DataStore, Service interactions (implemented)
- **UI Tests**: Critical user flows and screen interactions (implemented)
- **Tools Used**: JUnit5, MockK, Turbine, Compose Testing, Room Testing

## Critical Patterns to Follow
- **Single Source of Truth**: Repositories own their domain data
- **StateFlow for Reactive UI**: All state exposed via StateFlow
- **Result Types**: Use Result<T> for operations that can fail  
- **Immutable Data Classes**: All state-holding classes are immutable
- **Dependency Injection**: Constructor injection throughout

## File Organization
```
com.wearinterval/
├── di/ - Hilt modules
├── data/ - Room, DataStore, Service implementations
├── domain/ - Interfaces and core models
├── ui/ - Compose screens and ViewModels
├── wearos/ - Tiles, complications, notifications
└── util/ - Utility functions and extensions
```

## Maintenance Checklist
When making changes to the completed MVP:

- [ ] Read FEATURE_SPECIFICATION.md and ARCHITECTURE_AND_PRACTICES.md
- [ ] Run existing tests to ensure current functionality: `./gradlew test`
- [ ] Check existing coverage: `./gradlew jacocoTestReport`
- [ ] Write tests for any new functionality before implementing
- [ ] Follow established architectural patterns
- [ ] Maintain or improve test coverage (≥ 90%)
- [ ] Test changes on device/emulator

## Quality Standards for Changes
Before committing any modifications:
- [ ] All existing tests still passing
- [ ] New functionality has comprehensive test coverage
- [ ] No regression in existing features
- [ ] Code follows established patterns and conventions
- [ ] Manual testing completed for affected areas

## Important Commands
```bash
# Test and coverage
./gradlew test
./gradlew jacocoTestReport

# Lint and quality
./gradlew lintDebug

# Build and run
./gradlew assembleRelease
./gradlew installRelease  # Use release builds unless specifically debugging

# SAPLING VERSION CONTROL (NEVER use git!)
sl status                 # Check repository status
sl commit -m "message"    # Commit changes
sl amend                  # Amend last commit
```

## Maintenance Guidelines
- Follow the patterns established in ARCHITECTURE_AND_PRACTICES.md
- Maintain the established testing standards (90%+ coverage)
- Test all changes thoroughly before committing
- Use release builds for normal testing unless specifically debugging

**Remember**: This is a completed, production-quality MVP. Any changes should maintain the high quality standards and comprehensive test coverage that has been achieved.