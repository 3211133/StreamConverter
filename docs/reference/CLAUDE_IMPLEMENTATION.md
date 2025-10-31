# StreamConverter Implementation Details for AI Assistants

## このドキュメントの基礎資料
このドキュメントは以下の資料を基に作成されています：
- [CLAUDE.md](../../CLAUDE.md) - AI アシスタント向けメインガイドから詳細部分を外部化

This document provides important implementation constraints and guidelines for AI assistants working with the StreamConverter codebase.

## Stream Processing Constraints

### Critical Rules
- **Never read entire InputStream into memory** - Always use streaming approaches
- **Commands must support streaming** - Process data as it flows through
- **Memory-efficient operations** - Use `MeasuredInputStream`/`MeasuredOutputStream` for monitoring

### Example: Incorrect vs Correct

```java
// ❌ INCORRECT: Reads entire stream into memory
byte[] allData = inputStream.readAllBytes();
String content = new String(allData);

// ✅ CORRECT: Stream processing
try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
    String line;
    while ((line = reader.readLine()) != null) {
        // Process line by line
    }
}
```

## Security Considerations

### SQL Injection Protection
- Use prepared statements in `DatabaseFetchRule`
- Never concatenate user input into SQL queries

### XML External Entity (XXE) Prevention
- Disable external entity processing in XML parsers
- Use secure factory configurations

### URL Scheme Validation
- Validate URL schemes in HTTP commands
- Only allow http/https protocols

### Input Sanitization
- Validate and sanitize all user inputs
- Apply appropriate encoding/escaping

## Context Management

### MDC Integration
- MDC (Mapped Diagnostic Context) for distributed tracing
- Automatic context propagation across threads

### ExecutionContext Usage
- Use for request correlation
- Track performance metrics
- Thread-safe context propagation in concurrent processing

### Example: Context Propagation

```java
ExecutionContext context = ExecutionContext.builder()
    .globalContext("traceId", UUID.randomUUID().toString())
    .build();

// Context is automatically propagated to all commands
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
```

## Performance Guidelines

### Benchmark Considerations
- Benchmark tests are isolated with memory configuration (`-Xmx3g` for 5GB tests)
- Memory measurements can be unreliable due to GC timing - use cautiously
- Prefer functional verification over performance-based assertions

### Known Behavior
- Spring WebClient uses sequential processing, not parallel
- Verified via `InputStreamCloseTimingVerification`
- Thread pool configuration affects concurrent pipeline execution

## Personal Insights (Lessons Learned)

- Do not read the entire InputStream at once
- It has been verified that Spring WebClient performs sequential, not parallel, processing
- Memory efficiency tests may be unreliable due to the effects of GC timing
- Do not use `@SuppressWarning` annotation

## Related Documentation

- [reference/TESTING.md](TESTING.md) - Testing strategy
- [reference/SECURITY_ANALYSIS.md](SECURITY_ANALYSIS.md) - Security measures
- [guides/BENCHMARK_IMPLEMENTATION.md](../guides/BENCHMARK_IMPLEMENTATION.md) - Performance benchmarks
