# StreamConverter Code Examples for AI Assistants

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [CLAUDE.md](../../CLAUDE.md) - AI アシスタント向けメインガイドから詳細部分を外部化

This document provides code examples and common usage patterns for AI assistants working with the StreamConverter codebase.

## Common Usage Patterns

### Basic Pipeline: CSV → HTTP API → JSON

```java
// Extract from CSV, send to API, extract from response
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),      // Extract from CSV
    new SendHttpCommand("http://api.example.com"), // Send to API
    new JsonNavigateCommand("$.result")         // Extract from response
};

StreamConverter converter = StreamConverter.create(pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### Context-Aware Processing (Recommended)

```java
// Build execution context with correlation IDs
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

// Create converter with context
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### MDC Auto-Sync with Shared Context (Since 2025-01)

```java
// Create execution context
ExecutionContext context = ExecutionContext.create();

// Extract userId from XML and auto-sync to MDC
XmlNavigateCommand extractUserId = new XmlNavigateCommand(
    TreePath.fromXml("request/userId"),
    new MdcSetupRule(context, "userId")  // Stores in shared context
);

// Downstream commands automatically get userId in MDC
SampleStreamCommand processor = new SampleStreamCommand("processor");

// Pipeline execution - TurboFilter auto-syncs to MDC
StreamConverter.createWithContext(context, extractUserId, processor)
    .run(inputStream, outputStream);

// All logs will show [userId:USER12345] automatically
```

**Key Points**:
- `MdcSetupRule` stores extracted values in shared context
- `ExecutionContextTurboFilter` auto-syncs to MDC on every log output
- Downstream commands are MDC-agnostic
- Works correctly in multi-threaded environments

### Testing Network-Dependent Code

```java
@DisabledIfSystemProperty(
    named = "skipNetworkTests",
    matches = "true",
    disabledReason = "Network-dependent test disabled"
)
@Test
void testWithNetworkAccess() {
    // Test code that requires network
}
```

## Related Documentation

- [quickstart/basic-usage.md](../quickstart/basic-usage.md) - Basic usage guide
- [handbook/](../handbook/) - Feature-specific handbooks with examples
