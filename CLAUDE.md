# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StreamConverter is a Java library for efficient stream processing of large files with memory-constrained pipeline architecture. It implements a command pattern for flexible data processing pipelines with concurrent execution capabilities.

### Module Structure
- `streamconverter-core` - Core library with command patterns and stream processing
- `streamconverter-web` - Web integration and REST API components
- `streamconverter-examples` - Usage examples and demos
- `streamconverter-tools` - Development tools and utilities

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
- Javadoc required for public APIs
- No unused imports or trailing whitespace

### Git Workflow
- Conventional Commits specification required
- Pre-commit hooks validate formatting, compilation, and tests
- Branch protection rules require pull requests for `develop` branch
- **New branches must always be created from `origin/develop`**, not from the current working branch.
  Before creating a branch, always run: `git checkout -b <branch-name> origin/develop`
  Failure to do so causes unrelated commits from the working branch to appear in the PR.

### Multi-Module Considerations
- Cross-module dependencies managed via Gradle composite builds
- Module-specific test execution patterns
- Security vulnerability management with explicit version overrides

### Security Focus
- Proactive security vulnerability patching with explicit dependency versions
- XML External Entity (XXE) prevention in XML processing
- SQL injection protection in database operations
- Input sanitization and URL scheme validation
- Regular dependency updates for security fixes
- Consult with Codex for security-related technical decisions as needed