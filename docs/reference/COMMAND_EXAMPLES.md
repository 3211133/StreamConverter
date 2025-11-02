# Command Examples Reference

StreamConverter で使用できるコマンドの基本的な使用例を示します。

> ✅ **検証済みコード**: このドキュメントのコード例は [BasicUsageExamples.java](../../streamconverter-examples/src/main/java/com/streamconverter/examples/docs/BasicUsageExamples.java) で実際にコンパイル・実行可能な形で管理されています。

## CSV Commands

### CsvNavigateCommand - CSV列変換

```java
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.StreamConverter;
import com.streamconverter.CommandResult;
import java.util.List;

// 特定の列に変換ルールを適用（CSV構造全体を保持）
IStreamCommand csvCommand = new CsvNavigateCommand(
    new CSVPath("productName"),
    new PassThroughRule()
);

StreamConverter converter = StreamConverter.create(new IStreamCommand[]{csvCommand});
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### CsvFilterCommand - CSV列抽出

```java
import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.path.CSVPath;

// 特定の列のみ抽出（指定した列だけを出力、ヘッダーありと仮定）
IStreamCommand filterCommand = CsvFilterCommand.create(new CSVPath("price"));

// ヘッダーの有無を明示的に指定
IStreamCommand filterCommand2 = new CsvFilterCommand(new CSVPath("name"), true);
```

### CsvValidateCommand - CSV検証

```java
import com.streamconverter.command.impl.csv.CsvValidateCommand;

// CSVファイルの構造を検証（必須カラムを指定）
IStreamCommand validateCommand = new CsvValidateCommand("id", "name", "price");

// 詳細設定を指定
IStreamCommand validateCommand2 = new CsvValidateCommand(
    true,  // hasHeader
    10,    // maxErrorsToReport
    "id", "name", "price"  // requiredColumns
);
```

## JSON Commands

### JsonNavigateCommand - JSON要素変換

```java
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.path.TreePath;
import com.streamconverter.command.rule.PassThroughRule;

// 特定の要素に変換ルールを適用（JSON構造全体を保持）
IStreamCommand jsonCommand = new JsonNavigateCommand(
    new TreePath("user", "name"),
    new PassThroughRule()
);

StreamConverter converter = StreamConverter.create(new IStreamCommand[]{jsonCommand});
```

### JsonFilterCommand - JSON要素抽出

```java
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.path.TreePath;

// TreePathを使用してJSON要素を抽出（指定した要素のみを出力）
IStreamCommand filterCommand = new JsonFilterCommand(
    new TreePath("items", "0", "price")
);
```

### JsonValidateCommand - JSON Schema検証

```java
import com.streamconverter.command.impl.json.JsonValidateCommand;

// JSONスキーマで検証（静的ファクトリメソッドを使用）
IStreamCommand validateCommand = JsonValidateCommand.create("schemas/user-schema.json");
```

## XML Commands

### XmlNavigateCommand - XML要素変換

```java
import com.streamconverter.command.impl.xml.XmlNavigateCommand;
import com.streamconverter.path.TreePath;
import com.streamconverter.command.rule.PassThroughRule;

// 特定の要素に変換ルールを適用（XML構造全体を保持）
IStreamCommand xmlCommand = new XmlNavigateCommand(
    new TreePath("root", "user", "name"),
    new PassThroughRule()
);
```

### XmlFilterCommand - XML要素抽出

```java
import com.streamconverter.command.impl.xml.XmlFilterCommand;
import com.streamconverter.path.TreePath;

// TreePathを使用してXML要素を抽出（指定した要素のみを出力）
IStreamCommand filterCommand = new XmlFilterCommand(
    new TreePath("root", "item")
);
```

### ValidateCommand - XML検証

```java
import com.streamconverter.command.impl.xml.ValidateCommand;

// XMLスキーマで検証（スキーマパスを指定）
IStreamCommand validateCommand = new ValidateCommand("schemas/document.xsd");
```

## HTTP Communication

### SendHttpCommand - HTTP リクエスト送信

```java
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import com.streamconverter.command.rule.PassThroughRule;

// HTTP POSTリクエストを送信
IStreamCommand httpCommand = new SendHttpCommand("https://api.example.com/process");

// パイプライン例: CSV → HTTP API → JSON
IStreamCommand[] pipeline = {
    new CsvNavigateCommand(new CSVPath("productName"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/lookup"),
    new JsonNavigateCommand(new TreePath("result"), new PassThroughRule())
};
```

## Data Transformation

### CharacterConvertCommand - 文字コード変換

```java
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;

// Shift_JIS から UTF-8 に変換（String パラメータを使用）
IStreamCommand convertCommand = new CharacterConvertCommand("Shift_JIS", "UTF-8");
```

### LineEndingNormalizeCommand - 改行コード正規化

```java
import com.streamconverter.command.impl.LineEndingNormalizeCommand;
import com.streamconverter.command.impl.LineEndingNormalizeCommand.LineEndingType;

// 改行コードをLFに統一（列挙型を使用）
IStreamCommand normalizeCommand = new LineEndingNormalizeCommand(LineEndingType.UNIX);

// Windowsスタイル (CRLF)
IStreamCommand windowsCommand = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

// システムデフォルト
IStreamCommand systemCommand = new LineEndingNormalizeCommand(LineEndingType.SYSTEM_DEFAULT);
```

## Pipeline Patterns

### 単純なパイプライン

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.CommandResult;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.command.rule.PassThroughRule;
import java.util.List;
import java.io.InputStream;
import java.io.OutputStream;

// CSV → 変換 → 出力
IStreamCommand[] pipeline = {
    new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule()),
    new CharacterConvertCommand("Shift_JIS", "UTF-8")
};

StreamConverter converter = StreamConverter.create(pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### ExecutionContext を使用したパイプライン

ExecutionContext の詳細な使用方法は [Basic Usage - Context and Metrics](../quickstart/basic-usage.md#3-context-and-metrics) を参照してください。

```java
import com.streamconverter.ExecutionContext;
import com.streamconverter.StreamConverter;
import com.streamconverter.CommandResult;
import java.util.List;

// コンテキスト作成
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

// コンテキスト付きパイプライン実行
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### 複雑なパイプライン例

```java
import com.streamconverter.ExecutionContext;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.json.JsonValidateCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import com.streamconverter.command.rule.PassThroughRule;
import java.util.UUID;

// CSV → HTTP API → JSON → 検証 → 出力
IStreamCommand[] complexPipeline = {
    new CsvNavigateCommand(new CSVPath("productId"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/products"),
    new JsonNavigateCommand(new TreePath("product", "details"), new PassThroughRule()),
    JsonValidateCommand.create("schemas/product-schema.json")
};

ExecutionContext context = ExecutionContext.builder()
    .globalContext("traceId", UUID.randomUUID().toString())
    .build();

StreamConverter converter = StreamConverter.createWithContext(context, complexPipeline);
```

## エラーハンドリング

### CommandResult を使用した結果確認

```java
import com.streamconverter.CommandResult;
import java.util.List;

List<CommandResult> results = converter.run(inputStream, outputStream);

for (CommandResult result : results) {
    if (!result.isSuccessful()) {
        System.err.println("Command failed: " + result.getCommandName());
        System.err.println("Error: " + result.getErrorMessage());
    } else {
        System.out.println("Success: " + result.getCommandName() +
                         " (duration: " + result.getExecutionTimeMs() + "ms)");
    }
}
```

### try-with-resources パターン

```java
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import com.streamconverter.StreamConverter;
import com.streamconverter.CommandResult;
import com.streamconverter.command.IStreamCommand;
import java.util.List;

try (InputStream input = new FileInputStream("input.csv");
     OutputStream output = new FileOutputStream("output.txt")) {

    StreamConverter converter = StreamConverter.create(pipeline);
    List<CommandResult> results = converter.run(input, output);

    // 結果を確認
    boolean allSuccessful = results.stream().allMatch(CommandResult::isSuccessful);
    if (!allSuccessful) {
        throw new RuntimeException("Pipeline execution failed");
    }

} catch (IOException e) {
    e.printStackTrace();
}
```

## 関連ドキュメント

- [Basic Usage](../quickstart/basic-usage.md) - 初心者向けチュートリアル
- [ARCHITECTURE.md](../ARCHITECTURE.md) - アーキテクチャ詳細
- [VALIDATION.md](../features/VALIDATION.md) - バリデーション機能の詳細
- [AUTO_LOGGING.md](../AUTO_LOGGING.md) - ロギング機能
- [COMMAND_REFERENCE.md](COMMAND_REFERENCE.md) - Gradle コマンドリファレンス
