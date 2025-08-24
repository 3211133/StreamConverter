# Version Management

## Current Version
- **Version**: 1.2.0
- **Status**: Active Development
- **Java**: 17+
- **Gradle**: 8.13

## Supported Versions

| Version | Support Status | Security Updates | End of Life |
|---------|----------------|------------------|-------------|
| 1.2.x   | ✅ Active      | ✅ Yes           | TBD         |
| 1.1.x   | 🔄 Maintenance | ✅ Yes           | 2025-12-31  |
| 1.0.x   | ❌ EOL         | ❌ No            | 2025-06-30  |

## Version History

### 1.2.0 (Current)
- ExecutionContext and MDC integration for multi-threaded traceability
- Enhanced CommandFactory with logging duplication avoidance
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
- **Minimum**: Java 17
- **Tested**: Java 17, 21
- **Recommended**: Java 21 LTS

### Dependencies
See [build.gradle.kts](../build.gradle.kts) for current dependency versions.

## 関連ドキュメント

- [Security Policy](../SECURITY.md) - セキュリティポリシーと脆弱性対応
- [Testing Strategy](TESTING.md) - テスト戦略と品質保証
- [Documentation Index](README.md) - その他のドキュメント