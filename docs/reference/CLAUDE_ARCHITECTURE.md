# StreamConverter Architecture Guide for AI Assistants

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [CLAUDE.md](../../CLAUDE.md) - AI アシスタント向けメインガイドから詳細部分を外部化

This document provides detailed architectural information for AI assistants working with the StreamConverter codebase.

## 3-Layer Architecture

```
Core Layer         → StreamConverter, ExecutionContext, CommandResult
Command Layer      → IStreamCommand implementations, AbstractStreamCommand
Foundation Layer   → Path handlers, utilities, security components
```

## Key Design Patterns

- **Command Pattern**: All processing units inherit from `IStreamCommand`
- **Pipeline Pattern**: Commands chained via `StreamConverter.create(commands[])`
- **Factory Pattern**: `EnhancedCommandFactory` with configuration and caching
- **Strategy Pattern**: `IRule` implementations for data transformation rules
- **Built-in Logging**: Automatic logging via `AbstractStreamCommand` base class

## Core Processing Flow

1. **Input Stream** → Wrapped in `MeasuredInputStream` for metrics
2. **Command Pipeline** → Sequential or concurrent command execution
3. **Rule Application** → Data transformation via `IRule` implementations
4. **Output Stream** → Wrapped in `MeasuredOutputStream` for metrics
5. **Context Propagation** → MDC context maintained across threads

## Command Types by Category

### Data Extraction
- `CsvNavigateCommand` - Extract data from CSV files
- `JsonNavigateCommand` - Extract data from JSON documents
- `XmlNavigateCommand` - Extract data from XML documents

### Data Transformation
- `CharacterConvertCommand` - Character encoding conversion
- `LineEndingNormalizeCommand` - Normalize line endings
- `xml.ConvertCommand` - XSLT transformation

### Communication
- `SendHttpCommand` - HTTP requests with Spring WebClient

### Validation
- `JsonValidateCommand` - JSON schema validation
- `JsonStreamingValidateCommand` - Streaming JSON validation
- `CsvValidateCommand` - CSV structure validation
- `ValidateCommand` - XML validation

### Filtering
- `CsvFilterCommand` - Filter CSV rows
- `JsonFilterCommand` - Filter JSON elements
- `XmlFilterCommand` - Filter XML nodes

### Pipeline Control
- `FileBufferCommand` - Buffer data through a temporary file between pipeline stages
  - `FileBufferCommand.create()` — 平文モード
  - `FileBufferCommand.createEncrypted()` — AES-256-GCM 暗号化モード（機密データ向け）
  - バリデーション失敗時に不正データが下流へ流れる問題（`ConsumerCommand` + `TeeInputStream` パターンの issue #545）を解決する

## Testing Patterns

- **Unit Tests**: Standard JUnit 5 with Mockito
- **Integration Tests**: End-to-end pipeline testing
- **Network Tests**: Conditional execution via `@DisabledIfSystemProperty`
- **Benchmark Tests**: Tagged with `@Tag("benchmark")`, excluded from normal test runs
- **Memory Tests**: Special handling for GC-sensitive memory assertions

## Related Documentation

- [ARCHITECTURE.md](../ARCHITECTURE.md) - Comprehensive architecture documentation
- [handbook/architecture.md](../handbook/architecture.md) - Architecture handbook
