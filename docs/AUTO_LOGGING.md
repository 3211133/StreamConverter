# AUTO_LOGGING

## 概要

- `PipelineContext` はパイプライン全体で共有されるコンテキストです。
- `MdcPropagatingRule` は抽出した値を `PipelineContext` と現在スレッドの MDC に設定します。
- `PipelineContextTurboFilter` はログ出力直前に `PipelineContext` の値を MDC へ同期します。

## 基本例

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.rule.MdcPropagatingRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonWalker.create(TreePath.fromJson("$.user.id"), MdcPropagatingRule.create("userId"))
);

converter.run(inputStream, outputStream);
```

## Logback 設定例

```xml
<configuration>
  <turboFilter class="com.streamconverter.logging.PipelineContextTurboFilter"/>
</configuration>
```

## 注意点

- `StreamConverter.run(...)` は `void` を返します。
- 旧 API（`ExecutionContext` / `createWithContext(...)` / `CommandResult`）は現行の公開 API ではありません。
