# StreamConverter クイックスタート

このガイドでは、StreamConverter の基本的な使用方法を紹介します。

## 前提条件

- Java 21 以上
- Gradle 8 以上（プロジェクト同梱のラッパーを使用）

## 1. CSV データの加工

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

IStreamCommand[] commands = {
    CsvNavigateCommand.create(new CSVPath("email"), new TrimRule())
};

StreamConverter converter = StreamConverter.create(commands);
converter.run(inputStream, outputStream);
```

## 2. JSON パスの変換

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.rule.impl.string.LowerCaseRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonNavigateCommand.create(TreePath.fromJson("$.user.name"), new LowerCaseRule())
);

converter.run(inputStream, outputStream);
```

## 3. コンテキストとメトリクス

```java
import java.util.List;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.composite.ChainRule;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.command.rule.impl.string.LowerCaseRule;
import com.streamconverter.context.ExecutionContext;
import com.streamconverter.path.CSVPath;

ExecutionContext context = ExecutionContext.builder()
    .globalContext("jobId", "daily-import")
    .userContext("operator", "batch-service")
    .build();

StreamConverter converter = StreamConverter.createWithContext(
    context,
    CsvNavigateCommand.create(
        new CSVPath("name"),
        new ChainRule(new TrimRule(), new LowerCaseRule()))
);

List<CommandResult> results = converter.run(inputStream, outputStream);
results.forEach(result ->
    LOG.info("{} -> {} ms", result.getCommandName(), result.getExecutionTimeMillis()));
```

より多くの例は [streamconverter-examples](../../streamconverter-examples/src/main/java/com/streamconverter/examples/) を参照してください。
