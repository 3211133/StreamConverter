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
    CsvNavigateCommand.create(CSVPath.of("email"), new TrimRule()),
    CharacterConvertCommand.create("UTF-8", "Shift_JIS")
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
    CsvNavigateCommand.create(CSVPath.of("productId"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/products"),
    JsonNavigateCommand.create(TreePath.fromJson("$.result"), new PassThroughRule())
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

このパイプラインは以下の処理を実行します：
1. CSVファイルから `productId` 列を抽出
2. 抽出したIDをHTTP APIに送信（`SendHttpCommand` は `streamconverter-http` モジュールで提供）
3. APIレスポンス（JSON）から `result` フィールドを抽出

## 3. バリデーションとバッファリング

バリデーションコマンドが失敗した場合に不正データが後続コマンドへ流れないようにするには、`FileBufferCommand` をステージ間に挟みます。

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.FileBufferCommand;

StreamConverter converter = StreamConverter.create(
    validateCmd,                  // バリデーション（失敗時は IOException をスロー）
    FileBufferCommand.create(),   // 一時ファイルでステージを分離
    transformCmd                  // バリデーション通過後のみ実行される
);
converter.run(inputStream, outputStream);
```

`validateCmd` が `IOException` をスローすると、`transformCmd` はまったく実行されません。一時ファイルは実行終了後に自動削除されます。

機密データを扱うパイプラインでは、AES-256-GCM で一時ファイルを暗号化するモードを使用できます：

```java
StreamConverter converter = StreamConverter.create(
    validateCmd,
    FileBufferCommand.createEncrypted(),  // 一時ファイルを AES-256-GCM で暗号化
    transformCmd
);
```

## 4. MDC によるログトレーシング

実運用環境では、ログにリクエスト・ジョブレベルのコンテキスト情報を付加することで、分散トレーシングが容易になります。SLF4J の MDC と `MdcPropagatingRule` を組み合わせることで、ストリームから抽出した値を自動的にログコンテキストへ伝搬できます。

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.MdcPropagatingRule;
import com.streamconverter.logging.MDCInitializer;
import com.streamconverter.path.CSVPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

private static final Logger LOG = LoggerFactory.getLogger(MyProcessor.class);

// アプリ起動時に一度呼び出し、MDC を子スレッドへ継承可能にする
MDCInitializer.initialize();

// ジョブレベルのトレーシング情報を親スレッドの MDC に設定
MDC.put("jobId", "daily-import");
MDC.put("environment", "production");

try {
    IStreamCommand[] pipeline = {
        CsvNavigateCommand.create(CSVPath.of("productId"), MdcPropagatingRule.create("productId")),
        (IStreamCommand) (in, out) -> {
            // 上流コマンドと並列実行されるため、開始時点での productId の有無は非決定的
            // （入力が小さい場合は上流が先に完了し、既に MDC に含まれることもある）
            LOG.info("Command start"); // MDC: jobId, environment（productId は不定）
            in.transferTo(out);
            // transferTo 完了後は上流が全行処理済みのため productId が MDC に確実に存在する
            LOG.info("Command end");   // MDC: jobId, environment, productId
        }
    };
    StreamConverter converter = StreamConverter.create(pipeline);
    converter.run(inputStream, outputStream);
} finally {
    MDC.clear();
}
```

このパイプラインは以下の処理を実行します：
1. `MDCInitializer.initialize()` でアプリ起動時に MDC の子スレッド継承を有効化
2. `MDC.put()` でジョブ/リクエストレベルのコンテキストを設定
3. `MdcPropagatingRule` でストリームから抽出した値（`productId`）を MDC に自動伝搬
4. 後続コマンドのすべてのログに `jobId`・`environment`・`productId` が付加される

### Logback の設定

`MdcPropagatingRule` が書き込む値は `PipelineContext` 経由で伝搬されます。別コマンドのスレッドのログに反映させるには、`logback.xml` に `PipelineContextTurboFilter` を追加してください。

```xml
<configuration>
    <turboFilter class="com.streamconverter.logging.PipelineContextTurboFilter"/>

    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- %mdc で MDC の全キー=値 を出力 -->
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} MDC={%mdc} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
```

---

より多くの例は [BasicUsageExamples.java](../../streamconverter-examples/src/main/java/com/streamconverter/examples/docs/BasicUsageExamples.java)（コンパイル検証済み）を参照してください。
