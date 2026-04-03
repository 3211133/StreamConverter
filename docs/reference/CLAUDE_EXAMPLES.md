# CLAUDE Examples

## Minimal pipeline

```java
StreamConverter converter = StreamConverter.create(
    CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule())
);
converter.run(inputStream, outputStream);
```

## Pipeline with MDC propagation

```java
StreamConverter converter = StreamConverter.create(
    JsonNavigateCommand.create(TreePath.fromJson("$.user.id"), MdcPropagatingRule.create("userId"))
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
