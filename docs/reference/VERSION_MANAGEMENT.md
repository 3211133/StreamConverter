# Version Management

## Current Version

For the current version, see [build.gradle.kts](../../build.gradle.kts) (search for `version = "`).

- **Status**: Pre-release Development
- **Java**: 21+
- **Gradle**: 8.13

> 💡 **Note**: The authoritative version number is defined in `build.gradle.kts`. Documentation references this file to avoid version inconsistencies.

## Supported Versions

| Version | Support Status | Security Updates | End of Life |
|---------|----------------|------------------|-------------|
| 0.0.0   | 🔄 Development | N/A              | TBD         |

*Note: Project is currently in pre-release development. First stable release (1.0.0) is planned.*

## Version History

### 0.0.0 (Current - In Development)
- ExecutionContext and MDC integration for multi-threaded traceability
- Direct instantiation pattern (Factory Pattern removed)
- Reorganized command packages (csv/, json/, xml/ separation)
- Comprehensive auto-logging infrastructure
- Performance benchmarking tools (streamconverter-tools module)
- Web API foundation (streamconverter-web module)

### 1.1.0
- Core streaming pipeline functionality
- Command pattern implementation
- Basic auto-logging infrastructure
- Memory-efficient processing for large files

### 1.0.0
- Initial release version
- Basic stream processing capabilities

## Release Policy

### Development Versions
- **SNAPSHOT**: Active development, may contain breaking changes
- Updated continuously during development
- Not recommended for production use

### Release Versions
- **Major (x.0.0)**: Breaking changes, new architecture
- **Minor (x.y.0)**: New features, backward compatible
- **Patch (x.y.z)**: Bug fixes, security updates

## Security Support

Security updates are provided for:
- Current major version (1.x.x)
- Previous major version for 12 months after new major release

For security issues, please refer to [SECURITY.md](../SECURITY.md).

## Compatibility

### Java Compatibility
- **Required**: Java 21 or later
- **Tested**: Java 21 LTS
- **Build Configuration**: All modules use `JavaLanguageVersion.of(21)`

**Why Java 21?**
- Modern language features (Virtual Threads, Pattern Matching, Records)
- LTS version with long-term support
- Consistent toolchain across all modules

**Note**: Java 17 is NOT supported. The project requires Java 21 as the minimum version.

### Dependencies
See [build.gradle.kts](../../build.gradle.kts) for current dependency versions.

## 関連ドキュメント

- [Security Policy](../SECURITY.md) - セキュリティポリシーと脆弱性対応
- [Testing Strategy](TESTING.md) - テスト戦略と品質保証
- [Documentation Index](README.md) - その他のドキュメント