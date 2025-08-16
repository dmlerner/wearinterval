# WearInterval - Claude Development Guide

## Quick Start for Claude
When working on this project, **immediately read these files in order** to understand the current state and requirements:

### 1. Read Project Foundation (Required)
```
@WEARINTERVAL_DESIGN_SPEC.md - Complete feature specification and UI requirements
@architecture.md - Technical architecture and patterns
@DEVELOPMENT_PRACTICES.md - Code quality standards, testing, and Sapling workflow
@IMPLEMENTATION_PLAN.md - Detailed phase-by-phase development plan
@PROJECT_STATUS.md - Current progress and next steps
```

### 2. Understand Current State
- **Always check `PROJECT_STATUS.md`** first to see:
  - Current implementation phase
  - Recently completed tasks  
  - What's currently being worked on
  - Next priority tasks
  - Any blockers or issues

### 3. Follow Development Standards
- **90% test coverage minimum** for all code
- **Test-driven development** - write tests first
- **Maximum 20 lines per function**
- **Atomic commits** with descriptive messages using Sapling
- **Update PROJECT_STATUS.md** after significant progress

## Project Overview
**WearInterval** is a Wear OS interval timer application with:
- Multiple timer configuration interfaces
- Comprehensive Wear OS integration (tiles, complications)
- Robust state management and persistence
- Production-quality architecture with full test coverage

## Architecture Summary
- **MVVM + Repository Pattern** with strict separation of concerns
- **3 Layers**: UI (Compose) → Domain (Repositories) → Data (Service/Room/DataStore)
- **Unidirectional Data Flow** via StateFlow
- **Dependency Injection** with Hilt
- **Comprehensive Testing** at all layers

## Key Technical Decisions
- **Wear OS 3.0+** (minSdk 30) with Jetpack Compose
- **TimerService** as foreground service for reliability
- **Room database** for configuration history
- **DataStore** for settings and current configuration
- **Sapling** for version control with conventional commits

## Implementation Phases
1. **Foundation** - Project setup, dependencies, core models
2. **Data Layer** - Room, DataStore, TimerService foundation  
3. **Domain Layer** - Repository implementations with full testing
4. **UI Layer** - All screens, ViewModels, navigation
5. **Wear OS Integration** - Tiles, complications, notifications
6. **Timer Logic & Polish** - Complete functionality and optimization

## Development Workflow
1. **Check current phase** in PROJECT_STATUS.md
2. **Update TodoWrite** with current tasks from implementation plan
3. **Write tests first** (TDD approach)
4. **Implement features** following architecture patterns
5. **Verify 90%+ coverage** with JaCoCo
6. **Commit frequently** with descriptive messages
7. **Update PROJECT_STATUS.md** with progress

## Testing Requirements
- **Unit Tests**: ViewModels, Repositories, Data Models (100% coverage)
- **Integration Tests**: DAOs, DataStore, Service interactions
- **UI Tests**: Critical user flows and screen interactions
- **Tools**: JUnit5, MockK, Turbine, Compose Testing, Room Testing

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

## Getting Started Checklist
When resuming development:

- [ ] Read PROJECT_STATUS.md to understand current state
- [ ] Check which phase you're in from IMPLEMENTATION_PLAN.md
- [ ] Update TodoWrite with current phase tasks
- [ ] Run existing tests: `./gradlew test`
- [ ] Check coverage: `./gradlew jacocoTestReport`  
- [ ] Continue with next task in current phase
- [ ] Write tests first for any new functionality
- [ ] Update PROJECT_STATUS.md when completing milestones

## Quality Gates
Before moving to next phase:
- [ ] All planned features implemented and working
- [ ] Test coverage ≥ 90% for new code
- [ ] All tests passing (unit + integration + UI)
- [ ] No critical code quality issues
- [ ] Manual testing completed on device/emulator

## Important Commands
```bash
# Test and coverage
./gradlew test
./gradlew jacocoTestReport

# Lint and quality
./gradlew lintDebug

# Sapling workflow
sl commit -m "feat(scope): description"
sl amend
sl pr submit
```

## Contact & Issues
- Follow the patterns established in the architecture document
- Maintain the established testing standards
- Use the TodoWrite tool to track progress
- Keep PROJECT_STATUS.md updated as the single source of truth

**Remember**: This is a production-quality codebase. Prioritize maintainability, testability, and adherence to established patterns over speed of implementation.