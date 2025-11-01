# Auto-Logging Infrastructure

StreamConverterは包括的な自動ログ出力機能を提供し、大容量ファイル処理時のトラブルシューティングとモニタリングを支援します。

## 概要

自動ログ機能は以下の2層アプローチで実装されています：

1. **Level 1**: `AbstractStreamCommand` - 基本的な実行ログ（実行時間、メモリ、データサイズ測定）
2. **Level 2**: `LoggingDecorator` - カスタムコマンド向けログ追加（デコレーターパターン）

**重要**: AbstractStreamCommandを継承するコマンドは自動的にログ機能を持つため、LoggingDecoratorによる重複ラップは不要です。

## 基本的な使用方法

### LoggingDecoratorを使用した手動ログ追加

```java
import com.streamConverter.command.LoggingDecorator;
import com.streamConverter.command.impl.CsvNavigateCommand;

// 基本コマンドを作成
IStreamCommand csvCommand = new CsvNavigateCommand("productName");

// ログ機能を追加
IStreamCommand loggedCommand = new LoggingDecorator(csvCommand);

// StreamConverterで使用
StreamConverter converter = StreamConverter.create(loggedCommand);
```

### 複数コマンドのパイプライン作成例

```java
import com.streamConverter.command.LoggingDecorator;
import com.streamConverter.command.impl.CsvNavigateCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;

// 各コマンドを作成
IStreamCommand csvCommand = new CsvNavigateCommand("productName");
IStreamCommand httpCommand = new SendHttpCommand("http://api.example.com");
IStreamCommand jsonCommand = new JsonNavigateCommand("$.result");

// 必要に応じてログ機能を追加
IStreamCommand loggedCsvCommand = new LoggingDecorator(csvCommand);
IStreamCommand loggedHttpCommand = new LoggingDecorator(httpCommand);
IStreamCommand loggedJsonCommand = new LoggingDecorator(jsonCommand);

IStreamCommand[] pipeline = {loggedCsvCommand, loggedHttpCommand, loggedJsonCommand};
StreamConverter converter = StreamConverter.create(pipeline);
```

## 出力されるログ情報

### 必須ログ項目

- **実行開始**: コマンド名、開始時刻
- **実行終了**: コマンド名、終了時刻、実行時間
- **例外発生**: 例外の種類、メッセージ、スタックトレース
- **パフォーマンス**: 実行時間、処理データサイズ

### 詳細ログ項目（DEBUGレベル）

- **入力データ情報**: データサイズ、形式、エンコーディング
- **出力データ情報**: データサイズ、変換結果サマリー
- **リソース使用量**: メモリ使用量、処理スループット
- **設定情報**: コマンド固有の設定値

## 実際のログ出力例

### AbstractStreamCommandベースのコマンド（自動ログ）

```
INFO  [AbstractStreamCommand] Starting command execution: CsvNavigateCommand
DEBUG [AbstractStreamCommand] Command details: CsvNavigateCommand{field=productName}
INFO  [AbstractStreamCommand] Command execution completed: CsvNavigateCommand (7ms, input: 1234567bytes, output: 89012bytes, memory: 2MB)
```

### ExecutionContext付きマルチスレッド実行

```
INFO  [StreamConverter] Starting StreamConverter with 3 commands (executionId: exec-20250824-123456)
INFO  [StreamConverter] Setting up command 1 of 3: CsvNavigateCommand (sequence: 1)
INFO  [StreamConverter] Setting up command 2 of 3: SendHttpCommand (sequence: 2)  
INFO  [StreamConverter] Setting up command 3 of 3: JsonNavigateCommand (sequence: 3)
INFO  [StreamConverter] Completed command: CsvNavigateCommand (sequence: 1)
INFO  [StreamConverter] Completed command: SendHttpCommand (sequence: 2)
INFO  [StreamConverter] Completed command: JsonNavigateCommand (sequence: 3)
INFO  [StreamConverter] All commands completed successfully (executionId: exec-20250824-123456)
```

### エラー発生時のログ

```
ERROR [AbstractStreamCommand] Command execution failed: SendHttpCommand (1523ms, input: 89012bytes, output: 0bytes, memory: 3MB) - Connection timeout
java.net.SocketTimeoutException: Connect timed out
```

## ExecutionContext対応のログ機能

ExecutionContextの基本的な使用方法は [Basic Usage - Context and Metrics](quickstart/basic-usage.md#3-context-and-metrics) を参照してください。

以下は、ExecutionContextとロギング機能を組み合わせた例です：

```java
// ExecutionContext作成（詳細は上記リンク参照）
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

// ロギングが自動的に有効化される
StreamConverter converter = StreamConverter.createWithContext(context, commands);
List<CommandResult> results = converter.run(inputStream, outputStream);

// ログにはrequestIdとuserIdが自動的に含まれる
```

### MDCコンテキスト伝播の確認

```java
// logback-spring.xml での MDC 設定確認
// pattern: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{executionId}] [%X{stage}] %logger{36} - %msg%n"

// 実行すると以下のような MDC 情報付きログが出力される：
// 2025-08-24 12:34:56 [pool-1-thread-1] INFO  [exec-20250824-123456] [CsvNavigateCommand-1] StreamConverter - Processing CSV field: productName

```

## パフォーマンス考慮事項

### ログレベルの選択

- **PRODUCTION**: `INFO`または`WARN` - 必要最小限のログ
- **DEVELOPMENT**: `DEBUG` - 詳細な診断情報
- **TROUBLESHOOTING**: `TRACE` - 最大詳細度（パフォーマンス影響あり）

### メモリ使用量

```java
// 大容量ファイル処理時の設定例
CommandConfig config = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("INFO")                    // DEBUGは避ける
    .enablePerformanceMetrics(false)        // 高頻度測定を無効化
    .setLogBufferSize(1024);                // ログバッファサイズ制限
```

## ログ設定のカスタマイズ

### logback.xmlでの設定

```xml
<configuration>
    <appender name="STREAM_CONVERTER" class="ch.qos.logback.core.FileAppender">
        <file>logs/streamconverter.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="com.streamConverter" level="INFO" additivity="false">
        <appender-ref ref="STREAM_CONVERTER" />
    </logger>
</configuration>
```

### プログラムでのロガー設定

```java
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

// 実行時にログレベルを変更
Logger logger = (Logger) LoggerFactory.getLogger("com.streamConverter");
logger.setLevel(Level.DEBUG);
```

## ベストプラクティス

### 1. 適切なログレベルの使用

```java
// 本番環境
CommandConfig prodConfig = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("WARN");    // エラーと警告のみ

// 開発環境  
CommandConfig devConfig = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("DEBUG");   // 詳細な診断情報
```

### 2. 大容量ファイル処理時の設定

```java
// 10GB+のファイル処理
CommandConfig largeFileConfig = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("INFO")
    .enablePerformanceMetrics(true)     // 処理進捗の把握
    .setMetricsInterval(10000);         // 10,000レコードごとに出力
```

### 3. パイプライン全体のログ統合

```java
// ExecutionContext作成でログ統合を実現
ExecutionContext context = ExecutionContext.builder()
    .globalContext("environment", "production")
    .globalContext("pipeline", "csv-http-json")
    .build();

IStreamCommand[] pipeline = {
    new CsvNavigateCommand("input"),
    new SendHttpCommand("http://api.example.com"),
    new JsonNavigateCommand("$.result")
};

StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
```

## トラブルシューティング

### 一般的な問題と解決策

1. **ログが出力されない**
   ```java
   // logback.xmlでのレベル設定を確認
   // CommandConfigでのログ有効化を確認
   config.enableLogging(true);
   ```

2. **パフォーマンスが低下する**
   ```java
   // ログレベルをINFO以上に設定
   config.setLogLevel("INFO");
   // パフォーマンス測定を無効化
   config.enablePerformanceMetrics(false);
   ```

3. **メモリ使用量が増加する**
   ```java
   // ログバッファサイズを制限
   config.setLogBufferSize(512);
   // 詳細ログを無効化
   config.enableMemoryTracking(false);
   ```

## 関連ドキュメント

- [Command Architecture](archived/COMMAND_ARCHITECTURE.md) - コマンドパターンの詳細
- [Testing Strategy](reference/TESTING.md) - パフォーマンス最適化とベンチマーク
- [Documentation Index](INDEX.md) - その他のドキュメント