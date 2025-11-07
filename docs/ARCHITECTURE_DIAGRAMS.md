# StreamConverter Architecture Diagrams

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [ARCHITECTURE.md](ARCHITECTURE.md) - アーキテクチャ設計の詳細説明

Visual representations of the StreamConverter architecture. For textual explanations and design principles, refer to [ARCHITECTURE.md](ARCHITECTURE.md).

## Overview Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  External       │    │  StreamConverter│    │  IStreamCommand │
│  System         │───▶│  Pipeline       │───▶│  Implementation │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Core Class Hierarchy

```mermaid
classDiagram
    class IStreamCommand {
        <<interface>>
        +execute(InputStream, OutputStream)
    }
    
    class AbstractStreamCommand {
        <<abstract>>
        -Logger log
        +execute(InputStream, OutputStream)
        #processStream(InputStream, OutputStream)*
    }
    
    class CsvNavigateCommand {
        -CSVPath path
        -IRule rule
        +CsvNavigateCommand(CSVPath, IRule)
        +processStream(InputStream, OutputStream)
    }
    
    class JsonNavigateCommand {
        -JSONPath path  
        -IRule rule
        +JsonNavigateCommand(JSONPath, IRule)
        +processStream(InputStream, OutputStream)
    }
    
    class XmlNavigateCommand {
        -XPath path
        -IRule rule
        +XmlNavigateCommand(XPath, IRule)
        +processStream(InputStream, OutputStream)
    }
    
    class LoggingDecorator {
        -IStreamCommand delegate
        -Logger log
        +LoggingDecorator(IStreamCommand)
        +execute(InputStream, OutputStream)
    }
    
    class StreamConverter {
        -IStreamCommand[] commands
        -ExecutorService executor
        +StreamConverter(IStreamCommand[])
        +run(InputStream, OutputStream)
    }

    IStreamCommand <|.. AbstractStreamCommand
    IStreamCommand <|.. LoggingDecorator
    AbstractStreamCommand <|-- CsvNavigateCommand
    AbstractStreamCommand <|-- JsonNavigateCommand  
    AbstractStreamCommand <|-- XmlNavigateCommand
    LoggingDecorator o-- IStreamCommand : delegates to
    StreamConverter o-- IStreamCommand : executes
```

## Path System Class Diagram

```mermaid
classDiagram
    class IPath {
        <<interface>>
        +matches(String): boolean
        +extract(String): String
    }
    
    class CSVPath {
        -String columnIdentifier
        +CSVPath(String)
        +matches(String): boolean
        +extract(String): String
    }
    
    class JSONPath {
        -String pathExpression
        +JSONPath(String)  
        +matches(String): boolean
        +extract(String): String
    }
    
    class XPath {
        -String xpathExpression
        +XPath(String)
        +matches(String): boolean
        +extract(String): String
    }
    
    IPath <|.. CSVPath
    IPath <|.. JSONPath
    IPath <|.. XPath
```

## Rule System Class Diagram

```mermaid
classDiagram
    class IRule {
        <<interface>>
        +apply(String): String
    }
    
    class PassThroughRule {
        +PassThroughRule()
        +apply(String): String
    }
    
    class ChainRule {
        -List~IRule~ rules
        +ChainRule(IRule...)
        +apply(String): String
    }
    
    class CamelToSnakeCaseRule {
        -boolean preserveUnderscores
        +CamelToSnakeCaseRule()
        +apply(String): String
    }

    IRule <|.. PassThroughRule
    IRule <|.. ChainRule
    IRule <|.. CamelToSnakeCaseRule
    ChainRule o-- IRule : chains
```

## Processing Flow Sequence Diagram

```mermaid
sequenceDiagram
    participant Client as External System
    participant SC as StreamConverter
    participant Cmd as IStreamCommand
    participant Path as Path (CSV/JSON/XML)
    participant Rule as IRule
    
    Client->>SC: new StreamConverter(commands[])
    Client->>SC: run(inputStream, outputStream)
    
    loop For each command
        SC->>Cmd: execute(inputStream, outputStream)
        Cmd->>Path: extract data using path
        Path-->>Cmd: extracted data
        Cmd->>Rule: apply transformation
        Rule-->>Cmd: transformed data
        Cmd->>SC: write to outputStream
    end
    
    SC-->>Client: List<CommandResult>
```

## Before vs After Architecture

### Before: Factory Pattern (Eliminated)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  External       │    │ EnhancedCommand │    │  StreamConverter│    │  IStreamCommand │
│  System         │───▶│ Factory         │───▶│  Pipeline       │───▶│  Implementation │
│                 │    │ (615 lines)     │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘    └─────────────────┘
```

### After: Direct Instantiation (Current)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  External       │    │  StreamConverter│    │  IStreamCommand │
│  System         │───▶│  Pipeline       │───▶│  Implementation │
│                 │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │                       ▲
                                │              ┌─────────────────┐
                                │              │ LoggingDecorator│
                                │              │ (Optional)      │
                                └─────────────▶│                 │
                                               └─────────────────┘
```

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                           StreamConverter Processing Pipeline                        │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   Command   │    │   Command   │    │   Command   │    │     ...     │         │
│  │      1      │───▶│      2      │───▶│      3      │───▶│             │         │
│  └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘         │
│                                                                                     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                           Command Internal Structure                                │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                           │
│  │    Path     │    │    Rule     │    │   Logging   │                           │
│  │  (CSV/JSON/ │───▶│ (Transform  │───▶│ (Optional)  │                           │
│  │    XML)     │    │   Rules)    │    │ Decorator   │                           │
│  └─────────────┘    └─────────────┘    └─────────────┘                           │
│                                                                                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Memory and Performance Impact

### Factory Pattern Memory Usage (Before)
```
┌─────────────────────────────────────────────────────────────────┐
│                    Memory Usage Profile                         │
├─────────────────────────────────────────────────────────────────┤
│  Factory Cache:        [████████████████████████████████████]    │
│  Reflection Data:      [████████████████████████████]           │
│  Command Objects:      [████████████]                           │
│  Configuration:        [████████████]                           │
│  Total Overhead:       [████████████████████████████████████]    │
└─────────────────────────────────────────────────────────────────┘
```

### Direct Instantiation Memory Usage (After)
```
┌─────────────────────────────────────────────────────────────────┐
│                    Memory Usage Profile                         │
├─────────────────────────────────────────────────────────────────┤
│  Command Objects:      [████████████]                           │
│  Path Objects:         [████████]                               │
│  Rule Objects:         [████████]                               │
│  Total Usage:          [████████████████████]                   │
│  Memory Saved:         [████████████████████████████████] (60%) │
└─────────────────────────────────────────────────────────────────┘
```

## Code Complexity Metrics

### Lines of Code Comparison
```
Component                    Before    After    Reduction
─────────────────────────────────────────────────────────
EnhancedCommandFactory         615        0      -615
StreamBuilder APIs             506        0      -506
Factory Tests                  400        0      -400
Documentation                   89       12       -77
─────────────────────────────────────────────────────────
Total                       1,610       12    -1,598 (99.3%)
```

### Cyclomatic Complexity
```
Metric                      Before    After    Improvement
─────────────────────────────────────────────────────────
Factory Methods                45        0      -100%
Reflection Calls               23        0      -100%
Configuration Paths            12        3       -75%
Error Handling Paths           18        6       -67%
─────────────────────────────────────────────────────────
Average Complexity             24.5      2.3     -91%
```

## Usage Pattern Diagrams

### Simple Command Creation
```java
// Direct and Clear
IStreamCommand command = new CsvNavigateCommand(
    new CSVPath("columnName"),    ←── Explicit path
    new PassThroughRule()         ←── Explicit rule
);
```

### Pipeline Creation
```java
// Explicit Pipeline Definition
IStreamCommand[] pipeline = {
    new CsvNavigateCommand(new CSVPath("data"), new PassThroughRule()),
    new LoggingDecorator(
        new CharacterConvertCommand("UTF-8", "UTF-16")
    ),
    new LineEndingNormalizeCommand(LineEndingType.UNIX)
};
StreamConverter converter = new StreamConverter(pipeline);
```

### Error Handling Flow
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Input Error   │    │ Command Failure │    │  Stream Error   │
│   Validation    │───▶│   Detection     │───▶│    Handling     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Clear Error    │    │  Stack Trace    │    │   Resource      │
│   Messages      │    │   Logging       │    │   Cleanup       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Summary

Visual representation of the simplified architecture after factory pattern elimination.

For detailed architectural explanations, design principles, and implementation rationale, see [ARCHITECTURE.md](ARCHITECTURE.md).