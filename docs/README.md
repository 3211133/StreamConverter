# StreamConverter Documentation

Welcome to the StreamConverter documentation. This project provides efficient stream processing with a simplified, direct instantiation architecture.

## 🚀 Quick Start

### Basic Usage
```java
// Create commands with explicit parameters
IStreamCommand csvCommand = new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule());
StreamConverter converter = new StreamConverter(new IStreamCommand[]{csvCommand});

// Process streams
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### With Logging
```java
// Add logging decorator for detailed monitoring
IStreamCommand baseCommand = new CsvNavigateCommand(new CSVPath("email"), new PassThroughRule());
IStreamCommand loggedCommand = new LoggingDecorator(baseCommand);
StreamConverter converter = new StreamConverter(new IStreamCommand[]{loggedCommand});
```

## 📚 Documentation Structure

### Core Documentation
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Current simplified architecture
- **[ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md)** - Visual diagrams and class relationships
- **[WEB_API.md](WEB_API.md)** - REST API documentation
- **[AUTO_LOGGING.md](AUTO_LOGGING.md)** - Logging capabilities and usage

### Development Guides
- **[guides/PRE_COMMIT_SETUP.md](guides/PRE_COMMIT_SETUP.md)** - Setting up development environment
- **[guides/BRANCH_STRATEGY.md](guides/BRANCH_STRATEGY.md)** - Git workflow and branching
- **[guides/BENCHMARK_IMPLEMENTATION.md](guides/BENCHMARK_IMPLEMENTATION.md)** - Performance testing

### Reference Documentation
- **[reference/TESTING.md](reference/TESTING.md)** - Comprehensive testing guide
- **[reference/SECURITY_ANALYSIS.md](reference/SECURITY_ANALYSIS.md)** - Security considerations
- **[reference/VERSION_MANAGEMENT.md](reference/VERSION_MANAGEMENT.md)** - Version control practices

### Archived Documents
- **[archived/](archived/)** - Legacy documentation from factory pattern era

## 🏗️ Architecture Overview

StreamConverter uses a simplified direct instantiation architecture:

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  External       │    │  StreamConverter│    │  IStreamCommand │
│  System         │───▶│  Pipeline       │───▶│  Implementation │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

**Key Benefits:**
- **99.4% code reduction** from factory pattern elimination (2,083 lines removed)
- **No reflection overhead** - direct instantiation for better performance
- **Clear dependencies** - explicit parameter requirements
- **Easy debugging** - straightforward execution paths

## 🎯 Available Commands

### Data Navigation
- **CsvNavigateCommand** - Navigate CSV data using CSVPath
- **JsonNavigateCommand** - Navigate JSON data using JSONPath
- **XmlNavigateCommand** - Navigate XML data using XPath

### Data Transformation
- **CharacterConvertCommand** - Convert character encodings
- **LineEndingNormalizeCommand** - Normalize line endings
- **SendHttpCommand** - Send HTTP requests with security validation

### Data Validation
- **CsvValidateCommand** - Validate CSV structure and content
- **JsonValidateCommand** - Validate JSON against schema
- **ValidateCommand** - Validate XML against XSD schema

## 📊 Performance Improvements

After factory pattern elimination:

| Metric | Before | After | Improvement |
|--------|--------|-------|-------------|
| Lines of Code | 2,095 | 12 | -99.4% |
| Memory Usage | 100% | 40% | -60% |
| Reflection Calls | 23 | 0 | -100% |
| Cyclomatic Complexity | 24.5 | 2.3 | -91% |

## 🔧 Path Specifications

### CSVPath
```java
new CSVPath("columnName")     // Extract by column name
new CSVPath("0")              // Extract by column index
```

### JSONPath
```java
new JSONPath("user.name")        // Simple property access
new JSONPath("users[0].email")   // Array access
new JSONPath("$.data.items[*]")  // Wildcard access
```

### XPath
```java
new XPath("//user/name")         // XPath expression
new XPath("/root/users/user[1]") // Specific element access
```

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :streamconverter-core:test

# Run with network tests (normally skipped)
./gradlew test -DskipNetworkTests=false
```

## 🌐 Web API

StreamConverter includes a REST API for stream processing:

```bash
# Start the web server
./gradlew bootRun

# Process CSV data
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/csv/extract?columnName=name"
```

## 🔍 More Information

- **RFC Compliance**: CSV processing follows RFC 4180
- **Security**: Built-in protection against XXE, SQL injection, and private IP access
- **Memory Efficiency**: Streaming architecture for large file processing
- **Thread Safety**: MDC context propagation for distributed tracing

## 📝 Contributing

1. Check the [guides/](guides/) directory for development setup
2. Follow the branching strategy in [guides/BRANCH_STRATEGY.md](guides/BRANCH_STRATEGY.md)
3. Run tests and ensure pre-commit hooks pass
4. Update documentation for any architectural changes

---

**Note**: This documentation reflects the simplified architecture after factory pattern elimination (v1.2.0). Legacy factory-related documentation has been moved to [archived/](archived/).