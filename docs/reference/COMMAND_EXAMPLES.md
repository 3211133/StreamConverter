# Command Examples

現行 API（`StreamConverter.create(...)` + `void run(...)`）のサンプルです。

## CSV 列変換

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvWalker;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

StreamConverter converter = StreamConverter.create(
    CsvWalker.create(CSVPath.of("name"), new TrimRule())
);
converter.run(inputStream, outputStream);
```

## JSON 値抽出

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonExtractCommand;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonExtractCommand.create(TreePath.fromJson("$.user.active"))
);
converter.run(inputStream, outputStream);
```

## XML 変換 + 検証

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.xml.XmlWalker;
import com.streamconverter.command.impl.xml.ValidateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    XmlWalker.create(TreePath.fromXml("root/element"), new TrimRule()),
    ValidateCommand.create("schema.xsd")
);
converter.run(inputStream, outputStream);
```

## MDC 伝播

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonWalker;
import com.streamconverter.command.rule.MdcPropagatingRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonWalker.create(TreePath.fromJson("$.userId"), MdcPropagatingRule.create("userId"))
);
converter.run(inputStream, outputStream);
```
