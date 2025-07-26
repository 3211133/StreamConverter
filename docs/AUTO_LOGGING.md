# Auto-Logging Infrastructure

StreamConverterは包括的な自動ログ出力機能を提供し、大容量ファイル処理時のトラブルシューティングとモニタリングを支援します。

## 概要

自動ログ機能は以下の3層アプローチで実装されています：

1. **Level 1**: `AbstractStreamCommand` - 基本的な実行ログ
2. **Level 2**: `LoggingDecorator` - 詳細なログ追加（デコレーターパターン）
3. **Level 3**: `CommandFactory` - 統一的な生成管理（ファクトリーパターン）

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

### CommandFactoryを使用した自動ログ設定

```java
import com.streamConverter.command.CommandFactory;
import com.streamConverter.command.CommandConfig;

// 設定でログを有効化
CommandConfig config = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("DEBUG");

// ファクトリーでコマンド作成（自動的にログ機能が追加される）
IStreamCommand command = CommandFactory.createCsvNavigateCommand("productName", config);
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

```
INFO  [LoggingDecorator] Starting execution: CsvNavigateCommand(field=productName)
DEBUG [LoggingDecorator] Input data size: 1.2MB, encoding: UTF-8
DEBUG [LoggingDecorator] Memory usage before: 45.2MB
INFO  [CsvNavigateCommand] Processing CSV navigation for field: productName
DEBUG [LoggingDecorator] Processing throughput: 156 records/sec
DEBUG [LoggingDecorator] Memory usage after: 47.8MB
INFO  [LoggingDecorator] Completed execution: CsvNavigateCommand, duration: 7.6ms
```

## CommandFactoryの高度な使用方法

### 設定オプション

```java
CommandConfig config = new CommandConfig()
    .enableLogging(true)                    // ログ有効化
    .setLogLevel("INFO")                    // ログレベル設定
    .enablePerformanceMetrics(true)         // パフォーマンス測定
    .enableMemoryTracking(true)             // メモリ使用量追跡
    .setCustomLoggerName("MyProcessor");    // カスタムロガー名
```

### 利用可能なファクトリーメソッド

```java
// CSV処理コマンド
IStreamCommand csvCmd = CommandFactory.createCsvNavigateCommand("fieldName", config);

// JSON処理コマンド  
IStreamCommand jsonCmd = CommandFactory.createJsonNavigateCommand("$.path", config);

// XML処理コマンド
IStreamCommand xmlCmd = CommandFactory.createXmlNavigateCommand("//element", config);

// HTTP送信コマンド
IStreamCommand httpCmd = CommandFactory.createSendHttpCommand("http://api.example.com", config);
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
CommandConfig config = new CommandConfig()
    .enableLogging(true)
    .setLogLevel("INFO");

IStreamCommand[] pipeline = {
    CommandFactory.createCsvNavigateCommand("input", config),
    CommandFactory.createSendHttpCommand("http://api.example.com", config),
    CommandFactory.createJsonNavigateCommand("$.result", config)
};

StreamConverter converter = StreamConverter.create(pipeline);
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

- [Command Architecture](COMMAND_ARCHITECTURE.md) - コマンドパターンの詳細
- [Performance Optimization](../docs/performance-optimization.md) - パフォーマンス最適化
- [Configuration Reference](../docs/configuration-reference.md) - 設定リファレンス