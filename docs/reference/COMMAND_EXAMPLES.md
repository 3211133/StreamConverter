# Command Examples Reference

StreamConverter で使用できるコマンドの基本的な使用例を示します。

## CSV Commands

### CsvNavigateCommand - CSV列抽出

```java
import com.streamConverter.command.csv.CsvNavigateCommand;

// 特定の列を抽出
IStreamCommand csvCommand = new CsvNavigateCommand("productName");

StreamConverter converter = StreamConverter.create(new IStreamCommand[]{csvCommand});
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### CsvFilterCommand - CSV行フィルタ

```java
import com.streamConverter.command.csv.CsvFilterCommand;

// 条件に一致する行のみ抽出
IStreamCommand filterCommand = new CsvFilterCommand("price", value -> {
    return Integer.parseInt(value) > 1000;
});
```

### CsvValidateCommand - CSV検証

```java
import com.streamConverter.command.csv.CsvValidateCommand;

// CSVファイルの構造を検証
IStreamCommand validateCommand = new CsvValidateCommand(
    Arrays.asList("id", "name", "price")  // 期待される列
);
```

## JSON Commands

### JsonNavigateCommand - JSON要素抽出

```java
import com.streamConverter.command.json.JsonNavigateCommand;

// JSONPathで要素を抽出
IStreamCommand jsonCommand = new JsonNavigateCommand("$.user.name");

StreamConverter converter = StreamConverter.create(new IStreamCommand[]{jsonCommand});
```

### JsonFilterCommand - JSON要素フィルタ

```java
import com.streamConverter.command.json.JsonFilterCommand;

// 条件に一致するJSON要素のみ抽出
IStreamCommand filterCommand = new JsonFilterCommand("$.items[*]", item -> {
    return item.get("price").asInt() > 1000;
});
```

### JsonValidateCommand - JSON Schema検証

```java
import com.streamConverter.command.json.JsonValidateCommand;

// JSONスキーマで検証
IStreamCommand validateCommand = new JsonValidateCommand(schemaInputStream);
```

## XML Commands

### XmlNavigateCommand - XML要素抽出

```java
import com.streamConverter.command.xml.XmlNavigateCommand;

// XPathで要素を抽出
IStreamCommand xmlCommand = new XmlNavigateCommand("/root/user/name");
```

### XmlFilterCommand - XML要素フィルタ

```java
import com.streamConverter.command.xml.XmlFilterCommand;

// 条件に一致するXML要素のみ抽出
IStreamCommand filterCommand = new XmlFilterCommand("/root/item", element -> {
    return Integer.parseInt(element.getAttribute("price")) > 1000;
});
```

### ValidateCommand - XML検証

```java
import com.streamConverter.command.xml.ValidateCommand;

// XMLスキーマで検証
IStreamCommand validateCommand = new ValidateCommand(xsdInputStream);
```

## HTTP Communication

### SendHttpCommand - HTTP リクエスト送信

```java
import com.streamConverter.command.communication.SendHttpCommand;

// HTTP POSTリクエストを送信
IStreamCommand httpCommand = new SendHttpCommand("https://api.example.com/process");

// パイプライン例: CSV → HTTP API → JSON
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),
    new SendHttpCommand("https://api.example.com/lookup"),
    new JsonNavigateCommand("$.result")
};
```

## Data Transformation

### CharacterConvertCommand - 文字コード変換

```java
import com.streamConverter.command.CharacterConvertCommand;

// Shift_JIS から UTF-8 に変換
IStreamCommand convertCommand = new CharacterConvertCommand(
    Charset.forName("Shift_JIS"),
    Charset.forName("UTF-8")
);
```

### LineEndingNormalizeCommand - 改行コード正規化

```java
import com.streamConverter.command.LineEndingNormalizeCommand;

// 改行コードをLFに統一
IStreamCommand normalizeCommand = new LineEndingNormalizeCommand("\n");
```

## Pipeline Patterns

### 単純なパイプライン

```java
// CSV → 変換 → 出力
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("name"),
    new CharacterConvertCommand(Charset.forName("Shift_JIS"), StandardCharsets.UTF_8)
};

StreamConverter converter = StreamConverter.create(pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### ExecutionContext を使用したパイプライン

ExecutionContext の詳細な使用方法は [Basic Usage - Context and Metrics](../quickstart/basic-usage.md#3-context-and-metrics) を参照してください。

```java
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
// CSV → HTTP API → JSON → 検証 → 出力
IStreamCommand[] complexPipeline = {
    new CsvNavigateCommand("productId"),
    new SendHttpCommand("https://api.example.com/products"),
    new JsonNavigateCommand("$.product.details"),
    new JsonValidateCommand(schemaInputStream)
};

ExecutionContext context = ExecutionContext.builder()
    .globalContext("traceId", UUID.randomUUID().toString())
    .build();

StreamConverter converter = StreamConverter.createWithContext(context, complexPipeline);
```

## エラーハンドリング

### CommandResult を使用した結果確認

```java
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
