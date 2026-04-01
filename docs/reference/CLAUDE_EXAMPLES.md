# CLAUDE Examples

## Minimal pipeline

```java
StreamConverter converter = StreamConverter.create(
    CsvNavigateCommand.create(CSVPath.fromHeaderName("name"), new TrimRule())
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
    LineEndingNormalizeCommand.create(LineEndingType.LF),
    CharacterConvertCommand.create(Charset.forName("UTF-8"), Charset.forName("Shift_JIS"))
);
converter.run(inputStream, outputStream);
```
