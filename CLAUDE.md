# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StreamConverter is a Java library for efficient stream processing of large files with memory-constrained pipeline architecture. It implements a command pattern for flexible data processing pipelines with concurrent execution capabilities.

**Version**: 1.2.0  
**Java Version**: 17  
**Build System**: Gradle with Kotlin DSL  
**Architecture**: Multi-module project with 4-layer design

### Module Structure
- `streamconverter-core` - Core library with command patterns and stream processing
- `streamconverter-web` - Web integration and REST API components
- `streamconverter-examples` - Usage examples and demos
- `streamconverter-tools` - Development tools and utilities

## Essential Commands

### Build and Test
```bash
# Build all modules
./gradlew build

# Run all tests (excludes benchmarks)
./gradlew test

# Run specific module tests
./gradlew :streamconverter-core:test

# Run individual test class
./gradlew :streamconverter-core:test --tests "*SendHttpCommandTest*"

# Run individual test method
./gradlew :streamconverter-core:test --tests "*.SendHttpCommandTest.testValidHttpsUrlCreation"

# Run with network tests (normally skipped)
./gradlew test -DskipNetworkTests=false
```

### Benchmarks and Performance
```bash
# Run all benchmarks (requires more memory)
./gradlew benchmarkAll

# Run large data benchmarks (5GB tests)
./gradlew benchmarkLargeData

# Run memory efficiency benchmarks
./gradlew benchmarkMemoryEfficiency
```

### Code Quality
```bash
# Apply code formatting
./gradlew spotlessApply

# Run static analysis (PMD + SpotBugs)
./gradlew check

# Generate unified Javadoc for all modules
./gradlew javadocAll
```

### Examples
```bash
# Run basic usage examples
./gradlew runQuickStart
./gradlew runAutoLoggingDemo
./gradlew runContextDemo
./gradlew runMDC
./gradlew runDataProcessing
```

## High-Level Architecture

### 4-Layer Architecture
```
Controller Layer    → CsvProcessingController, JsonProcessingController
Core Layer         → StreamConverter, ExecutionContext, CommandResult  
Command Layer      → IStreamCommand implementations, AbstractStreamCommand
Foundation Layer   → Path handlers, utilities, security components
```

### Key Design Patterns
- **Command Pattern**: All processing units inherit from `IStreamCommand`
- **Pipeline Pattern**: Commands chained via `StreamConverter.create(commands[])`
- **Factory Pattern**: `EnhancedCommandFactory` with configuration and caching
- **Strategy Pattern**: `IRule` implementations for data transformation rules
- **Decorator Pattern**: `LoggingDecorator`, `ContextPropagatingDecorator`

### Core Processing Flow
1. **Input Stream** → Wrapped in `MeasuredInputStream` for metrics
2. **Command Pipeline** → Sequential or concurrent command execution 
3. **Rule Application** → Data transformation via `IRule` implementations
4. **Output Stream** → Wrapped in `MeasuredOutputStream` for metrics
5. **Context Propagation** → MDC context maintained across threads

### Command Types by Category
- **Data Extraction**: `CsvNavigateCommand`, `JsonNavigateCommand`, `XmlNavigateCommand`
- **Data Transformation**: `CharacterConvertCommand`, `LineEndingNormalizeCommand`
- **Communication**: `SendHttpCommand` (HTTP requests with Spring WebClient)
- **Validation**: `JsonValidateCommand`, `CsvValidateCommand`, `xml.ValidateCommand`
- **Filtering**: `CsvFilterCommand`, `JsonFilterCommand`, `XmlFilterCommand`

### Testing Patterns
- **Unit Tests**: Standard JUnit 5 with Mockito
- **Integration Tests**: End-to-end pipeline testing
- **Network Tests**: Conditional execution via `@DisabledIfSystemProperty`
- **Benchmark Tests**: Tagged with `@Tag("benchmark")`, excluded from normal test runs
- **Memory Tests**: Special handling for GC-sensitive memory assertions

## Important Implementation Details

### Stream Processing Constraints
- **Never read entire InputStream into memory** - Always use streaming approaches
- **Commands must support streaming** - Process data as it flows through
- **Memory-efficient operations** - Use `MeasuredInputStream`/`MeasuredOutputStream` for monitoring

### Testing Network-Dependent Code
```java
@DisabledIfSystemProperty(
    named = "skipNetworkTests", 
    matches = "true",
    disabledReason = "Network-dependent test disabled"
)
```

### Security Considerations
- SQL injection protection in `DatabaseFetchRule`
- XML external entity (XXE) prevention in XML processing
- URL scheme validation for HTTP commands
- Input sanitization and validation throughout

### Context Management
- MDC (Mapped Diagnostic Context) integration for distributed tracing
- `ExecutionContext` for request correlation and performance tracking
- Thread-safe context propagation in concurrent processing

### Performance Guidelines
- Benchmark tests are isolated with memory configuration (`-Xmx3g` for 5GB tests)
- Memory measurements can be unreliable due to GC timing - use cautiously
- Prefer functional verification over performance-based assertions
- Spring WebClient uses sequential processing, not parallel (verified via InputStreamCloseTimingVerification)

## Workflow Guidelines

- When proposing workflows, output to a separate proposal directory
- Log unexpected behavior during implementation for troubleshooting
- Document insights: "Thought X, did Y, but Z happened, so revised understanding to A and will do B"
- Always validate design decisions and change content appropriateness

## Personal Insights

- InputStreamを全て読み込んではならない
- Spring WebClientは並列処理ではなく逐次処理を行うことが検証済み
- メモリ効率テストはGCタイミングの影響で信頼性が低い場合がある

## Development Notes

### Code Style
- Google Java Format applied automatically via Spotless
- UTF-8 encoding enforced
- Javadoc required for public APIs
- No unused imports or trailing whitespace

### Git Workflow
- Conventional Commits specification required
- Pre-commit hooks validate formatting, compilation, and tests
- Branch protection rules require pull requests for `develop` branch

### Multi-Module Considerations
- Cross-module dependencies managed via Gradle composite builds
- Unified Javadoc generation across all modules
- Consistent versioning strategy (currently 1.2.0)
- Module-specific test execution patterns