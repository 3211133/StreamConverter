# Command Examples

現行 API（`StreamConverter.create(...)` + `void run(...)`）のサンプルです。

## CSV 列変換/ナビゲーション

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

StreamConverter converter = StreamConverter.create(
    CsvNavigateCommand.create(CSVPath.fromHeaderName("name"), new TrimRule())
);
converter.run(inputStream, outputStream);
```

## JSON フィルタ

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.TreePath;

StreamConverter converter = StreamConverter.create(
    JsonFilterCommand.create(TreePath.fromJson("$.user.active"), new PassThroughRule())
);
converter.run(inputStream, outputStream);
```

## XML 変換 + 検証

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.xml.ConvertCommand;
import com.streamconverter.command.impl.xml.ValidateCommand;

StreamConverter converter = StreamConverter.create(
    ConvertCommand.create("transform.xslt"),
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
