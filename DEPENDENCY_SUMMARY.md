# StreamConverter 依存関係サマリー

## クイックリファレンス

### Spring Boot Ecosystem
| Component | Version | Purpose |
|-----------|---------|---------|
| Spring Boot | 3.3.2 | WebAPI Framework |
| Spring Security | 6.3.1 | Authentication & Authorization |
| Spring Web MVC | 6.1.10 | REST API Controllers |
| Jackson | 2.17.1 | JSON Processing |

### Data Processing
| Library | Version | Purpose |
|---------|---------|---------|
| JSON Schema Validator | 1.5.3 | JSON Schema Validation |
| OpenCSV | 5.9 | CSV Processing |
| Commons Lang3 | 3.18.0 | Utility Functions |
| Commons IO | 2.20.0 | I/O Operations |

### Testing & Quality
| Tool | Version | Purpose |
|------|---------|---------|
| JUnit 5 | 5.13.4 | Unit Testing |
| Mockito | 5.18.0 | Mocking |
| PITest | 1.2.3 | Mutation Testing |

### Documentation
| Tool | Version | Purpose |
|------|---------|---------|
| SpringDoc OpenAPI | 2.6.0 | API Documentation |

## Dependency Tree (Simplified)

```
StreamConverter
├── Spring Boot Starters
│   ├── spring-boot-starter-web
│   │   ├── spring-webmvc
│   │   ├── jackson-databind
│   │   └── tomcat-embed-core
│   ├── spring-boot-starter-security
│   │   └── spring-security-web
│   ├── spring-boot-starter-actuator
│   │   └── micrometer-core
│   └── spring-boot-starter-validation
│       └── hibernate-validator
├── Data Processing
│   ├── json-schema-validator
│   ├── opencsv
│   ├── commons-lang3
│   └── commons-io
├── Documentation
│   └── springdoc-openapi-starter-webmvc-ui
└── Test Dependencies
    ├── spring-boot-starter-test
    ├── mockito-core
    └── pitest-junit5-plugin
```

## License Compatibility

All dependencies use permissive licenses compatible with commercial use:

- **Apache License 2.0**: Spring Boot, Commons libraries, Jackson
- **MIT License**: JSON Schema Validator
- **BSD License**: OpenCSV
- **Eclipse Public License**: JUnit

## Security Considerations

### Regular Updates Required
- **Critical**: Spring Boot security patches
- **Important**: JSON processing libraries (Jackson)
- **Moderate**: Utility libraries

### Vulnerability Monitoring
```bash
# Run security audit
./gradlew dependencyCheckAnalyze

# Generate SBOM (Software Bill of Materials)
./gradlew cycloneDx
```

## Build Requirements

### Minimum Versions
- **Java**: 17+
- **Gradle**: 8.0+
- **Memory**: 1GB heap for build

### Build Commands
```bash
# Clean build with all checks
./gradlew clean build

# Build without tests (faster)
./gradlew assemble

# Run with dependency verification
./gradlew build --write-verification-metadata sha256
```

## Performance Impact

### JAR Size
- **Application JAR**: ~15MB
- **Fat JAR (with dependencies)**: ~45MB

### Startup Time
- **Cold start**: ~3-5 seconds
- **With JVM warmup**: ~1-2 seconds

### Memory Usage
- **Minimum heap**: 128MB
- **Recommended heap**: 512MB
- **Production heap**: 1GB+

---

**Last Updated**: 2025-07-29  
**Next Review**: Quarterly dependency updates