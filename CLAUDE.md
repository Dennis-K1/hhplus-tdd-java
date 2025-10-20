# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) exercise project for implementing a user point management system. The project uses Spring Boot with Java 17 and Gradle for build management.

## Build and Development Commands

### Building the Project
```bash
./gradlew build
```

### Running Tests
```bash
# Run all tests
./gradlew test

# Run a specific test class
./gradlew test --tests "FullyQualifiedTestClassName"

# Run tests with coverage report
./gradlew test jacocoTestReport
```

### Running the Application
```bash
./gradlew bootRun
```

## Architecture and Key Components

### Package Structure
- `io.hhplus.tdd` - Root package
  - `point` - Point management domain (controller, models)
  - `database` - In-memory database simulation tables

### Data Layer Design

The project uses **in-memory table classes** (`UserPointTable` and `PointHistoryTable`) to simulate database operations:

- **UserPointTable**: Manages user point data with `selectById()` and `insertOrUpdate()` methods
- **PointHistoryTable**: Manages transaction history with `insert()` and `selectAllByUserId()` methods
- Both tables include artificial throttling (200-300ms random delays) to simulate real database latency
- **IMPORTANT**: Do NOT modify the table classes themselves. Use only their public APIs to interact with data.

### Domain Models

- **UserPoint**: Record containing `id`, `point`, and `updateMillis`
- **PointHistory**: Record containing transaction details (`id`, `userId`, `amount`, `type`, `updateMillis`)
- **TransactionType**: Enum with `CHARGE` (충전/charge) and `USE` (사용/use) values

### API Endpoints (Skeleton Implementation)

All endpoints in `PointController` currently return stub data and need implementation:

- `GET /point/{id}` - Retrieve user point balance
- `GET /point/{id}/histories` - Retrieve user transaction history
- `PATCH /point/{id}/charge` - Charge points to user account
- `PATCH /point/{id}/use` - Use/deduct points from user account

### Error Handling

Global exception handling is configured via `ApiControllerAdvice` which catches all exceptions and returns a 500 error response.

## Implementation Guidelines

When implementing the point management features:

1. **Service Layer**: Create service classes to implement business logic between the controller and table classes
2. **Validation**: Add input validation for point amounts and user IDs
3. **Concurrency**: Consider thread-safety when multiple operations access the same user's points
4. **Transaction History**: Record all charge/use operations in `PointHistoryTable`
5. **Testing**: Write unit tests for business logic and integration tests for API endpoints

## Testing Notes

- JUnit Platform is configured for running tests
- JaCoCo is set up for code coverage reporting (toolVersion 0.8.7)
- Tests are configured to not fail the build (`ignoreFailures = true` in build.gradle.kts)
