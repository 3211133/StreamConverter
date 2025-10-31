# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StreamConverter is a Java library for efficient stream processing of large files with memory-constrained pipeline architecture. It implements a command pattern for flexible data processing pipelines with concurrent execution capabilities.

**Version**: 1.2.0
**Java Version**: 21
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

# Run large data benchmarks (5GB tests) - requires 3GB heap
./gradlew benchmarkLargeData

# Run memory efficiency benchmarks
./gradlew benchmarkMemoryEfficiency

# Run benchmark infrastructure tests
./gradlew benchmarkInfrastructure
```

**Memory Requirements:**
- Large data benchmarks: `-Xmx3g -Xms1g` (automatically configured)
- Memory efficiency tests: `-Xms1g -Xmx2g` (automatically configured)
- Regular tests: `-Xmx1g -Xms512m` (automatically configured)

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
./gradlew runDirectApiDemo
./gradlew runDemo
```

## Detailed AI Assistant Guides

For detailed architectural information, code examples, and implementation guidelines, refer to:

- **[docs/reference/CLAUDE_ARCHITECTURE.md](docs/reference/CLAUDE_ARCHITECTURE.md)** - Architecture patterns, design decisions, command types
- **[docs/reference/CLAUDE_EXAMPLES.md](docs/reference/CLAUDE_EXAMPLES.md)** - Code examples and common usage patterns
- **[docs/reference/CLAUDE_IMPLEMENTATION.md](docs/reference/CLAUDE_IMPLEMENTATION.md)** - Stream processing constraints, security, performance guidelines

## Available Documentation

The project follows a structured documentation approach with clear hierarchy:

### Core Documentation
- **[docs/INDEX.md](docs/INDEX.md)** - Comprehensive documentation index and navigation
- **[docs/quickstart/basic-usage.md](docs/quickstart/basic-usage.md)** - Basic usage guide for beginners
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - System architecture and design patterns
- **[docs/handbook/](docs/handbook/)** - Concise What/Why/How guides for key features

### Development & Operations
- **[docs/deployment/docker.md](docs/deployment/docker.md)** - Docker containerization guide
- **[docs/reference/TESTING.md](docs/reference/TESTING.md)** - Test strategy and benchmark execution
- **[docs/reference/SECURITY_ANALYSIS.md](docs/reference/SECURITY_ANALYSIS.md)** - Security measures and analysis
- **[docs/guides/BENCHMARK_IMPLEMENTATION.md](docs/guides/BENCHMARK_IMPLEMENTATION.md)** - Performance measurement details
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Development contribution guidelines

### Navigation
Always start with the **[Documentation Index](docs/INDEX.md)** for comprehensive navigation and organization by topic, audience, and document type.

## Workflow Guidelines

- Document insights with concrete examples, e.g.: "Expected stream to process in parallel, implemented concurrent pipeline, but observed sequential execution due to thread pool limits, so adjusted configuration and updated documentation."
- Always validate design decisions and change content appropriateness
- Focus on design validity and content appropriateness (per Copilot guidelines)

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
- Unified Javadoc generation across all modules with `./gradlew javadocAll`
- Consistent versioning strategy (currently 1.2.0)
- Module-specific test execution patterns
- Security vulnerability management with explicit version overrides
- Gradle Kotlin DSL used throughout for type-safe configuration

### Security Focus
- Proactive security vulnerability patching with explicit dependency versions
- XML External Entity (XXE) prevention in XML processing
- SQL injection protection in database operations
- Input sanitization and URL scheme validation
- Regular dependency updates for security fixes
