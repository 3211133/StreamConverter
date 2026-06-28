# CLAUDE Examples

> AIアシスタント（Claude Code）向けの要約リファレンスです。詳細は [COMMAND_EXAMPLES.md](COMMAND_EXAMPLES.md) を参照してください。

## Minimal pipeline

```java
StreamConverter converter = StreamConverter.create(
    new CsvWalker(CSVPath.of("name"), new TrimRule())
);
converter.run(inputStream, outputStream);
```

## Pipeline with MDC propagation

```java
StreamConverter converter = StreamConverter.create(
    new JsonWalker(TreePath.fromJson("$.user.id"), MdcPropagatingRule.create("userId"))
);
converter.run(inputStream, outputStream);
```

## Multi command chain

```java
StreamConverter converter = StreamConverter.create(
    new LineEndingNormalizeCommand(LineEndingType.UNIX),
    CharacterConvertCommand.create("UTF-8", "Shift_JIS")
);
converter.run(inputStream, outputStream);
```
