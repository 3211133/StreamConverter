# StreamConverter Architecture

> 💡 **クイック概要**: まず [Architecture Handbook](handbook/architecture.md) で What/Why/How を理解することをお勧めします。

This document describes the simplified StreamConverter architecture after factory pattern elimination:

**External Systems → Direct Instantiation → StreamConverter → Commands**

## Overview

The StreamConverter provides a clean, direct approach to stream processing:

- **Direct Instantiation** creates commands with explicit parameters
- **StreamConverter** handles command orchestration and stream processing
- **Commands** focus purely on stream transformation logic
- **Automatic Logging** is built into all commands via AbstractStreamCommand

This simplified architecture removes factory complexity while maintaining all functionality.

## Architecture Flow Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  External       │    │  StreamConverter│    │  IStreamCommand │
│  System         │───▶│  Pipeline       │───▶│  Implementation │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                                             │
         │                                             │
         │                                    (AbstractStreamCommand)
         │                                      (Built-in Logging)
         │
         ▼
┌─────────────────────────────────────────────────────────────────┐
│              Direct Instantiation Pattern                      │
├─────────────────────────────────────────────────────────────────┤
│  CsvWalker.create(CSVPath.of("column"), rule)                  │
│  JsonWalker.create(TreePath.fromJson("$.path"), rule)          │
│  XmlWalker.create(TreePath.fromXml("element/child"), rule)     │
└─────────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. Direct Command Instantiation

Commands are created via static factory methods with explicit parameters. For detailed examples, see [Command Examples](reference/COMMAND_EXAMPLES.md).

```java
// Example: CSV command
IStreamCommand csvCommand = CsvWalker.create(
    CSVPath.of("name"),
    new PassThroughRule()
);
```

### 2. StreamConverter Pipeline

```java
// Single command
StreamConverter converter = StreamConverter.create(command);

// Multiple commands pipeline
StreamConverter converter = StreamConverter.create(command1, command2, command3);

// Execute processing
converter.run(inputStream, outputStream);
```

### 3. Automatic Logging

All commands extending `AbstractStreamCommand` automatically include comprehensive logging:

```java
// All standard commands have built-in logging
IStreamCommand csvCommand = CsvWalker.create(CSVPath.of("name"), new PassThroughRule());
StreamConverter converter = StreamConverter.create(csvCommand);

// Logs automatically include:
// - Execution time, data sizes, memory usage
// - Performance warnings for slow operations
// - Error details with context
```

See [AUTO_LOGGING.md](AUTO_LOGGING.md) for details on the automatic logging infrastructure.


## Architecture Benefits

**Benefits of direct instantiation**:
- No hidden complexity or magic
- No reflection overhead
- Compile-time type safety
- Clear, explicit dependency requirements
- Easy to understand and debug
- Automatic logging via `AbstractStreamCommand` — no extra configuration
- 60% memory reduction vs factory pattern; 2,500+ lines of factory code removed

### Integration Patterns

**File-based Processing**:
```java
try (FileInputStream input = new FileInputStream("data.csv");
     FileOutputStream output = new FileOutputStream("result.txt")) {
    
    IStreamCommand command = CsvWalker.create(CSVPath.of("name"), new PassThroughRule());
    StreamConverter converter = StreamConverter.create(command);
    converter.run(input, output);
}
```

**Pipeline Processing**:
```java
// Multi-stage processing pipeline (all commands auto-logged)
StreamConverter converter = StreamConverter.create(
    CsvWalker.create(CSVPath.of("data"), new PassThroughRule()),
    new CharacterConvertCommand("UTF-8", "UTF-16"),
    new LineEndingNormalizeCommand(LineEndingNormalizeCommand.LineEndingType.UNIX)
);
```

## Key Improvements After Factory Elimination

### Before: Complex Factory Pattern
```
┌─────────────────────────────────────────────────────────────────┐
│                    Factory Pattern Complexity                  │
├─────────────────────────────────────────────────────────────────┤
│  EnhancedCommandFactory:  [████████████████████████████████████] 615 lines │
│  StreamBuilder APIs:      [██████████████████████████████]       506 lines │
│  Factory Tests:           [████████████████████████]            400 lines │
│  Configuration:           [██████████]                           89 lines  │
├─────────────────────────────────────────────────────────────────┤
│  Total Code:              [████████████████████████████████]     1,610 lines│
│  Reflection Calls:        23 methods                            │
│  Cyclomatic Complexity:   24.5 average                          │
└─────────────────────────────────────────────────────────────────┘
```

### After: Direct Instantiation
```
┌─────────────────────────────────────────────────────────────────┐
│                   Direct Instantiation Simplicity             │
├─────────────────────────────────────────────────────────────────┤
│  Command Creation:        [████████]                    Direct calls │
│  Path Specifications:     [████████]                    Explicit    │
│  Rule Definitions:        [████████]                    Clear        │
│  Optional Logging:        [████████]                    Decorator    │
├─────────────────────────────────────────────────────────────────┤
│  Total Code:              [████████]                    12 lines    │
│  Reflection Calls:        0 methods                              │
│  Cyclomatic Complexity:   2.3 average                           │
│  Code Reduction:          99.4% (-2,083 lines)                  │
└─────────────────────────────────────────────────────────────────┘
```

## Implementation Details

### Error Handling
StreamConverter provides built-in error handling:
- Input/output stream validation
- Command execution error handling
- Detailed error messages and stack traces
- Automatic resource cleanup

### Performance Characteristics

#### Memory Usage Comparison
```
Factory Pattern (Before):
┌─────────────────────────────────────────────────────────────────┐
│ Factory Cache:      [████████████████████████████████████]      │
│ Reflection Data:    [████████████████████████████]              │
│ Command Objects:    [████████████]                              │
│ Configuration:      [████████████]                              │
│ Total Memory:       [████████████████████████████████████]      │
└─────────────────────────────────────────────────────────────────┘

Direct Instantiation (After):
┌─────────────────────────────────────────────────────────────────┐
│ Command Objects:    [████████████]                              │
│ Path Objects:       [████████]                                  │
│ Rule Objects:       [████████]                                  │
│ Total Memory:       [████████████████████] (60% reduction)      │
│ Memory Saved:       [████████████████████████████████] (40%)    │
└─────────────────────────────────────────────────────────────────┘
```

## Available Commands

### Data Navigation / Transformation Commands
- **CsvWalker**: Navigate and transform CSV columns using `CSVPath`
- **JsonWalker**: Navigate and transform JSON values using `TreePath.fromJson()`
- **XmlWalker**: Navigate and transform XML elements using `TreePath.fromXml()`
- **CsvFilterCommand**: Filter CSV rows by column values

### Data Extraction Commands
- **JsonExtractCommand**: Extract JSON data using JSONPath expressions
- **XmlExtractCommand**: Extract XML elements using XPath expressions

### Data Validation Commands
- **CsvValidateCommand**: Validate CSV structure and content
- **ValidateCommand**: Validate XML against XSD schema

### Data Conversion Commands
- **CharacterConvertCommand**: Convert character encodings
- **LineEndingNormalizeCommand**: Normalize line endings

### HTTP Integration Commands (streamconverter-http)
- **SendHttpCommand**: Send HTTP requests (with security validation)

## Path Specifications

### CSVPath
```java
CSVPath.of("columnName")  // Extract by column name
CSVPath.of("0")           // Extract by column index
```

### TreePath (JSON)
```java
TreePath.fromJson("user.name")        // Simple property access
TreePath.fromJson("users[0].email")   // Array access
TreePath.fromJson("$.data.items[*]")  // Wildcard access
```

### TreePath (XML)
```java
TreePath.fromXml("user/name")          // Element path
TreePath.fromXml("root/users/user[1]") // Specific element access
```

## Rule Specifications

### PassThroughRule
```java
new PassThroughRule()  // No transformation, just pass data through
```

