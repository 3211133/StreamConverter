# StreamConverter Architecture and Dependencies

This document provides a comprehensive overview of the StreamConverter project's internal module dependencies, architectural layers, and external library dependencies. It serves as the single source of truth for dependency information.

## 1. Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        WebAPI Layer                         │
├─────────────────────────────────────────────────────────────┤
│ Controller Layer    │ DTO Layer       │ Exception Handling  │
│ - StreamConverter   │ - TransformReq  │ - GlobalException   │
│   Controller        │ - TransformResp │   Handler           │
├─────────────────────────────────────────────────────────────┤
│                       Service Layer                         │
├─────────────────────────────────────────────────────────────┤
│ Business Logic      │ Pipeline Builder │ Value Extraction   │
│ - TransformService  │ - Command        │ - Decorator        │
│ - BatchTransformSvc │   Factory        │   Pattern          │
├─────────────────────────────────────────────────────────────┤
│                      Command Layer                          │
├─────────────────────────────────────────────────────────────┤
│ Stream Commands     │ Validation      │ Context Management  │
│ - AbstractStream    │ - JSON/XML/CSV  │ - ExecutionContext  │
│   Command           │   Validators    │ - MDC Integration   │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
├─────────────────────────────────────────────────────────────┤
│ Spring Boot        │ External Libs    │ Configuration       │
│ - Web/Security     │ - JSON Schema    │ - Application.yml   │
│ - Actuator/Cache   │ - OpenCSV        │ - Security Config   │
└─────────────────────────────────────────────────────────────┘
```

## 2. External Library Dependencies

This section details the external libraries used in the project, their roles, and versions.

### 2.1. Core Framework (Spring Boot)

| Dependency | Version | Scope | Purpose |
|---|---|---|---|
| `org.springframework.boot:spring-boot-starter-web` | 3.3.2 | impl | Core web framework, REST APIs, embedded Tomcat |
| `org.springframework.boot:spring-boot-starter-validation` | 3.3.2 | impl | Input validation (Bean Validation) |
| `org.springframework.boot:spring-boot-starter-actuator` | 3.3.2 | impl | Monitoring and management (health, metrics) |
| `org.springframework.boot:spring-boot-starter-security` | 3.3.2 | impl | Authentication, authorization, security |
| `org.springframework.boot:spring-boot-starter-cache` | 3.3.2 | impl | Caching abstraction |

### 2.2. Data Processing & Utilities

| Dependency | Version | Scope | Purpose |
|---|---|---|---|
| `org.apache.commons:commons-lang3` | 3.18.0 | impl | General utility functions |
| `commons-io:commons-io` | 2.18.0 | impl | I/O stream utilities |
| `com.networknt:json-schema-validator` | 1.5.8 | impl | JSON Schema validation |
| `com.opencsv:opencsv` | 5.12.0 | impl | CSV reading and processing |
| `com.github.ben-manes.caffeine:caffeine` | 3.1.8 | impl | High-performance caching implementation |

### 2.3. API Documentation

| Dependency | Version | Scope | Purpose |
|---|---|---|---|
| `org.springdoc:springdoc-openapi-starter-webmvc-ui` | 2.6.0 | impl | OpenAPI 3.0 spec and Swagger UI generation |

### 2.4. Testing

| Dependency | Version | Scope | Purpose |
|---|---|---|---|
| `org.springframework.boot:spring-boot-starter-test` | 3.3.2 | test | Spring Boot integration testing |
| `org.springframework.security:spring-security-test` | 6.3.1 (from BOM) | test | Security testing utilities |
| `org.awaitility:awaitility` | 4.2.0 | test | Asynchronous testing utilities |
| `org.junit:junit-bom` | 5.13.4 | test | JUnit 5 dependency management |
| `org.mockito:mockito-core` | 5.18.0 | test | Mocking framework |
| `org.pitest:pitest-junit5-plugin` | 1.2.3 | test | Mutation testing for test quality |

## 3. Dependency Management Strategy

### 3.1. Spring Boot BOM (Bill of Materials)
We use the `io.spring.dependency-management` plugin, which leverages the Spring Boot 3.3.2 BOM to manage versions for সময়োপযোগী (transitive) dependencies like `spring-core`, `jackson-databind`, `logback`, etc. This ensures compatibility across the Spring ecosystem.

### 3.2. Explicit Versioning
Third-party libraries not covered by the Spring Boot BOM are versioned explicitly in `build.gradle.kts`. We select stable, well-maintained versions.

### 3.3. Dependency Health Monitoring
We recommend the following practices to maintain dependency health:
- **Vulnerability Scanning**: Use tools like the OWASP Dependency-Check plugin (`./gradlew dependencyCheckAnalyze`) to scan for known vulnerabilities.
- **Update Checks**: Regularly check for new versions using the `com.github.ben-manes.versions` plugin (`./gradlew dependencyUpdates`).
- **Automated Checks**: Set up CI/CD pipelines to run these checks automatically on a schedule (e.g., weekly).

## 4. Dependency Addition/Removal Process

### Checklist for Adding a New Dependency
1.  **License Compatibility**: Ensure the license is compatible (e.g., Apache 2.0, MIT).
2.  **Security**: Check for known vulnerabilities.
3.  **Maintenance**: Verify the library is actively maintained.
4.  **Impact**: Assess the impact on JAR size and startup performance.
5.  **Necessity**: Confirm the functionality cannot be reasonably achieved with existing dependencies.

### Process for Removing a Dependency
1.  **Impact Analysis**: Identify all code locations using the library.
2.  **Refactoring**: Replace the functionality with an alternative implementation.
3.  **Verification**: Run all tests to ensure no regressions.
4.  **Documentation**: Update this document.

## 5. Troubleshooting

### Resolving Dependency Conflicts
If a transitive dependency conflict arises, use Gradle's resolution strategy to force a specific version:
```gradle
configurations.all {
    resolutionStrategy {
        force 'group:name:version'
    }
}
```

### Viewing the Dependency Tree
To understand the project's dependencies, use the following command:
```bash
# Display the full dependency tree
./gradlew dependencies
```

---
*This document was last updated on 2025-07-30 and should be maintained alongside any changes to `build.gradle.kts`.*
