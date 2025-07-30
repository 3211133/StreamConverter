# Testing Strategy

This document outlines the testing strategy, tools, and execution procedures for the StreamConverter project.

## 1. Testing Philosophy

Our testing philosophy is based on the "Testing Pyramid" concept. We aim for a healthy mix of:
- **Unit Tests**: Fast, isolated tests for individual classes and methods.
- **Integration Tests**: Tests for component collaboration within the application.
- **End-to-End (E2E) Tests**: Tests that verify the entire application flow from the API endpoint to the database (or external services).

## 2. Test Types and Scope

### 2.1. Unit Tests
- **Location**: `src/test/java/com/streamConverter/...`
- **Purpose**: To verify the logic of a single class in isolation. Dependencies should be mocked.
- **Examples**:
    - `BatchTransformServiceTest`: Testing batching logic, error handling, and parallelism with a mocked `TransformService`.
    - `CsvNavigateCommandTest`: Testing the CSV parsing and navigation logic of a single command.
- **Key Tools**: JUnit 5, Mockito.

### 2.2. Integration Tests
- **Location**: `src/test/java/com/streamConverter/api/integration/...`
- **Purpose**: To verify the interaction between different components, such as the controller, service, and repository layers, within a running Spring context.
- **Examples**:
    - `StreamConverterIntegrationTest`: An E2E test that sends real HTTP requests to a running application on a random port.
- **Key Tools**: Spring Boot Test (`@SpringBootTest`), `TestRestTemplate`, Awaitility (for async testing).

### 2.3. Mutation Tests
- **Purpose**: To evaluate the quality and effectiveness of our existing tests. Mutation testing tools automatically introduce small changes (mutations) into the source code and check if the tests fail. If no test fails, it indicates a potential gap in the test suite.
- **Key Tools**: Pitest (`info.solidsoft.pitest`).

## 3. Testing Tools

| Tool | Version | Purpose |
|---|---|---|
| **JUnit 5** | 5.13.4 (via BOM) | The core testing framework. |
| **Mockito** | 5.18.0 | For creating mock objects in unit tests. |
| **AssertJ** | (from Spring BOM) | For writing fluent, readable assertions. |
| **Spring Boot Test** | 3.3.2 | For writing integration and E2E tests. |
| **Awaitility** | 4.2.0 | For testing asynchronous systems reliably. |
| **Pitest** | 1.15.0 | For mutation testing. |
| **JaCoCo** | (from Gradle plugin) | For measuring test coverage. |

## 4. How to Run Tests

All tests can be executed from the project root directory using the Gradle wrapper.

### 4.1. Run All Tests
```bash
./gradlew test
```

### 4.2. Run a Specific Test Class
Use the `--tests` flag with the fully qualified class name.
```bash
./gradlew test --tests "com.streamConverter.api.integration.StreamConverterIntegrationTest"
```

### 4.3. Run a Specific Test Method
```bash
./gradlew test --tests "com.streamConverter.api.integration.StreamConverterIntegrationTest.testSynchronousBatchProcessing"
```

## 5. Test Reports

### 5.1. Test Execution Report
After running the tests, a detailed HTML report is generated at:
- `build/reports/tests/test/index.html`

XML results suitable for CI/CD systems are available in `build/test-results/test/`.

### 5.2. Test Coverage Report (JaCoCo)
The `build.gradle.kts` file is already configured to generate coverage reports. After running `./gradlew test`, the report is available at:
- `build/reports/jacoco/test/html/index.html`

### 5.3. Mutation Testing Report (Pitest)
To run mutation tests, execute the following command:
```bash
./gradlew pitest
```
The report will be generated in the `build/reports/pitest/` directory.

## 6. Test Resources
Test-specific resources, such as sample XML files, schemas, or test data, are located in `src/test/resources/`.
```
src/test/resources/
├── application-test.yml  # Configuration for tests
├── test-schema.xsd       # XML schema for validation tests
├── valid-test.xml        # Valid XML test file
└── invalid-test.xml      # Invalid XML test file
```
