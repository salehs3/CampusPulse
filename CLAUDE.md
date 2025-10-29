# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application project for CS124 Fall 2025 (AY2025-MP-Java) that implements an event discovery system called "Eventable". The app fetches and displays event summaries from a local mock server.

## Build System

- **Build tool**: Gradle with Kotlin DSL (`.gradle.kts` files)
- **Java version**: Java 21 (enforced via toolchain)
- **Android SDK**: Compile SDK 36, Min SDK 36, Target SDK 36

## Common Commands

### Building and Running Tests
```bash
./gradlew test                    # Run all unit tests
./gradlew testDebugUnitTest       # Run debug unit tests only
./gradlew check                   # Run all checks (tests, checkstyle, linting)
./gradlew app:grade               # Run grading for current checkpoint
```

### Running Specific Checkpoint Tests
Tests are organized by checkpoint (MP0Test, MP1Test, etc.). There are two ways to run tests:

**Option 1: Run specific checkpoint test suite (recommended)**
```bash
# MP0 tests
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest --tests "edu.illinois.cs.cs124.ay2025.mp.test.MP0Test"

# MP1 tests
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest --tests "edu.illinois.cs.cs124.ay2025.mp.test.MP1Test"

# MP2 tests
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest --tests "edu.illinois.cs.cs124.ay2025.mp.test.MP2Test"

# MP3 tests
./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest --tests "edu.illinois.cs.cs124.ay2025.mp.test.MP3Test"
```

**Option 2: Use grade task (runs tests for current checkpoint)**
Configure which checkpoint to grade by editing `grade.yaml`:
```yaml
checkpoint: 0  # Set to 0, 1, 2, or 3
```
Then run:
```bash
./gradlew app:grade               # Runs tests + checkstyle + scoring for current checkpoint
```
After changing the checkpoint in grade.yaml, sync the project: File → Sync Project with Gradle Files

### Running Single Test Method
```bash
./gradlew :app:testDebugUnitTest --tests "edu.illinois.cs.cs124.ay2025.mp.test.MP0Test.testMethodName"
```

### Code Quality
```bash
./gradlew spotlessApply           # Auto-format code (Google Java Format + ktlint)
./gradlew spotlessCheck           # Check code formatting
./gradlew checkstyle              # Run checkstyle verification
./gradlew checkTimeProvider       # Verify proper use of time provider abstraction
```

## Architecture

### Package Structure
- `activities/` - Android Activity classes (UI controllers)
  - `MainActivity.java` - Main app screen with RecyclerView for event list
- `adapters/` - RecyclerView adapters
  - `SummaryListAdapter.java` - Adapter for displaying Summary items
- `application/` - Android Application class
  - `EventableApplication.java` - App-level singleton, manages server URL and Client instance
- `helpers/` - Utility classes
  - `Helpers.java` - Shared utilities including Jackson ObjectMapper and TimeProvider abstraction
  - `ResultMightThrow.java` - Result wrapper for async operations
- `models/` - Data models
  - `EventData.java` - Java record for complete event data
  - `Summary.java` - Summary view of event (subset of EventData fields)
- `network/` - Networking layer
  - `Server.java` - MockWebServer Dispatcher for local testing
  - `Client.java` - OkHttp-based HTTP client with async callbacks

### Key Design Patterns

**Time Abstraction**: Code must use `Helpers.getTimeProvider().now()` instead of `Instant.now()` to enable time mocking in tests. The `checkTimeProvider` task enforces this.

**Async Networking**: `Client` uses OkHttp with ExecutorService for async requests. Callbacks use `ResultMightThrow<T>` wrapper to handle success/error states. UI updates must happen on main thread via `runOnUiThread()`.

**Mock Server**: `Server` extends MockWebServer's Dispatcher to provide local testing without real network calls. It filters events to only return those starting today or later (America/Chicago timezone).

**JSON Serialization**: Jackson is configured in `Helpers.OBJECT_MAPPER` with parameter names module for constructor-based deserialization.

### Data Flow
1. `MainActivity.onResume()` → `loadSummaries()`
2. `Client.getSummaries()` executes async HTTP request to `/summary/`
3. Server returns filtered list of Summary objects (events ≥ today)
4. Callback receives `ResultMightThrow<List<Summary>>`
5. `MainActivity` updates UI via `runOnUiThread()` and notifies adapter

## Important Constraints

- **No direct time calls**: Always use `Helpers.getTimeProvider().now()` (enforced by `checkTimeProvider` task)
- **Use proper imports**: Always use import statements at the top of files instead of fully qualified class names in the code body (e.g., `import java.util.List;` then use `List`, not `java.util.List` in code)
- **Descriptive variable names**: Always use clear, descriptive variable names that convey purpose and meaning (e.g., `eventSummaries` instead of `list`, `startDateTime` instead of `dt`)
- **Checkstyle required**: Code must pass checkstyle verification (10 points of grade)
- **Early deadlines**: Checkpoints 2 and 3 have early deadline bonuses (10 points each)
- **ASCII paths only**: Project path, Android SDK path, and Gradle home must contain only ASCII characters (verified by `checkPaths` task)
- **Test fingerprints**: Test suite has fingerprint checking to ensure test integrity
- **Commit required**: Grading requires committed changes (enforced by gradlegrader)

## Testing

Tests use Robolectric for Android unit testing without emulator. Test helpers provide utilities for:
- Adaptive timeouts (configurable via environment variables)
- RecyclerView matchers for UI testing
- HTTP test utilities
- Mock data generation

Test configuration environment variables:
- `TEST_TIMEOUT_MULTIPLIER` (default: 4)
- `TEST_TIMEOUT_MIN` (default: 5000ms)
- `TEST_TIMEOUT_MAX` (default: 30000ms)


## About me

I am a beginner in coding in the class CS124 and i am excited to learn and understand the code

- Always use descriptive variable names
