# Command Examples

現行 API（`StreamConverter.create(...)` + `void run(...)`）のサンプルです。

## CSV 列変換/ナビゲーション

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

StreamConverter converter = StreamConverter.create(
    CsvNavigateCommand.create(CSVPath.of("name"), new TrimRule())
);
converter.run(inputStream, outputStream);
```

## JSON フィルタ

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonFilterCommand.create(TreePath.fromJson("$.user.active"))
);
converter.run(inputStream, outputStream);
```

## XML 変換 + 検証

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.command.impl.xml.ValidateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    XmlNavigateCommand.create(TreePath.fromXml("root/element"), new TrimRule()),
    ValidateCommand.create("schema.xsd")
);
converter.run(inputStream, outputStream);
```

## MDC 伝播

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.rule.MdcPropagatingRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonNavigateCommand.create(TreePath.fromJson("$.userId"), MdcPropagatingRule.create("userId"))
);
converter.run(inputStream, outputStream);
```
