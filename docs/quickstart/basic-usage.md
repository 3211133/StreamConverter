# StreamConverter クイックスタート

このガイドでは、StreamConverter の基本的な使用方法を紹介します。

## 前提条件

- Java 21 以上
- Gradle 8 以上（プロジェクト同梱のラッパーを使用）

## 1. 基本的なパイプライン

最もシンプルなパイプライン例として、CSVデータを読み込んで文字コード変換を行います。

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

// 2つのコマンドを組み合わせたパイプライン
IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("email"), new TrimRule()),
    new CharacterConvertCommand("UTF-8", "Shift_JIS")
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

このパイプラインは以下の処理を実行します：
1. CSVファイルから `email` 列を抽出し、前後の空白を削除
2. 文字コードをUTF-8からShift_JISに変換

## 2. 実用的なAPI連携パイプライン

実際のアプリケーションでよく使われる、CSV → HTTP API → JSON処理の例です。

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;

// 3つのコマンドを組み合わせた実用的なパイプライン
IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("productId"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/products"),
    JsonNavigateCommand.create(TreePath.fromJson("$.result"), new PassThroughRule())
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

このパイプラインは以下の処理を実行します：
1. CSVファイルから `productId` 列を抽出
2. 抽出したIDをHTTP APIに送信
3. APIレスポンス（JSON）から `result` フィールドを抽出

## 3. ExecutionContext とメトリクス取得

実運用環境では、処理の追跡とメトリクス収集が重要です。ExecutionContextを使用してログトレースとパフォーマンス測定を行います。

```java
import java.util.List;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.json.JsonValidateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.context.ExecutionContext;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;

// ExecutionContextを作成してトレーシング情報を設定
ExecutionContext context = ExecutionContext.builder()
    .globalContext("jobId", "daily-import")
    .globalContext("environment", "production")
    .userContext("operator", "batch-service")
    .build();

// 4つのコマンドを組み合わせた高度なパイプライン
IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("productId"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/products"),
    JsonNavigateCommand.create(TreePath.fromJson("$.result"), new PassThroughRule()),
    JsonValidateCommand.create("schemas/product-schema.json")
};

// ExecutionContext付きでパイプラインを実行
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);

// 各コマンドの実行結果を確認
results.forEach(result -> {
    if (result.isSuccessful()) {
        LOG.info("{} -> {} ms (input: {} bytes, output: {} bytes)",
            result.getCommandName(),
            result.getExecutionTimeMillis(),
            result.getInputBytes(),
            result.getOutputBytes());
    } else {
        LOG.error("{} failed: {}", result.getCommandName(), result.getErrorMessage());
    }
});
```

このパイプラインは以下の処理を実行します：
1. CSVファイルから `productId` 列を抽出
2. 抽出したIDをHTTP APIに送信
3. APIレスポンス（JSON）から `result` フィールドを抽出
4. JSONスキーマで検証

ExecutionContextにより、すべてのログに `jobId`, `environment`, `operator` の情報が自動的に付加され、マルチスレッド環境でも正確なトレーシングが可能になります。

---

より多くの例は [streamconverter-examples](../../streamconverter-examples/src/main/java/com/streamconverter/examples/) を参照してください。
