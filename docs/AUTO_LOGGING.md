# Auto-Logging Infrastructure

## このドキュメントの基礎資料
このドキュメントは以下の実装を基に作成されています：
- [AbstractStreamCommand.java](../streamconverter-core/src/main/java/com/streamconverter/command/AbstractStreamCommand.java) - 自動ログ機能の実装

> 💡 **クイック概要**: まず [Logging Handbook](handbook/logging.md) で What/Why/How を理解することをお勧めします。

StreamConverterは包括的な自動ログ出力機能を提供し、大容量ファイル処理時のトラブルシューティングとモニタリングを支援します。

## 概要

すべての標準コマンドは `AbstractStreamCommand` を継承しており、以下のログ機能が自動的に提供されます：

- **実行時間測定**: コマンドの開始から終了までの時間
- **データサイズ測定**: 入力・出力バイト数
- **メモリ使用量測定**: コマンド実行前後のメモリ差分
- **パフォーマンス警告**: 5秒以上の実行時に自動警告
- **実際のクラス名表示**: 各コマンドが自身のクラス名でログ出力
- **MDC自動同期**: ExecutionContextの共有コンテキストがログ出力時に自動的にMDCに同期（2025年1月追加、TurboFilter使用）
- **ソースロケーション情報**: メソッド名と行番号の自動出力（logback設定による）

> ⚠️ **非推奨**: `LoggingDecorator` クラスは現在非推奨です。AbstractStreamCommandが包括的なログ機能を提供するため、LoggingDecoratorは不要になりました。既存のコードでLoggingDecoratorを使用している場合は、単純に削除してください。すべての標準コマンドは自動的にログ出力されます。

## 基本的な使用方法

### 単一コマンドの実行（自動ログ）

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.command.rule.PassThroughRule;

// コマンドを作成するだけでログ機能は自動的に有効
IStreamCommand csvCommand = new CsvNavigateCommand(new CSVPath("productName"), new PassThroughRule());

// StreamConverterで使用（ログは自動出力）
StreamConverter converter = StreamConverter.create(csvCommand);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

### 複数コマンドのパイプライン作成例（自動ログ）

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import com.streamconverter.command.rule.PassThroughRule;

// 各コマンドを作成（ログ機能は自動的に含まれる）
IStreamCommand csvCommand = new CsvNavigateCommand(new CSVPath("productName"), new PassThroughRule());
IStreamCommand httpCommand = new SendHttpCommand("http://api.example.com");
IStreamCommand jsonCommand = new JsonNavigateCommand(TreePath.fromJson("$.result"), new PassThroughRule());

// パイプラインを作成（各コマンドが自動的にログ出力）
IStreamCommand[] pipeline = {csvCommand, httpCommand, jsonCommand};
StreamConverter converter = StreamConverter.create(pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
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

### 単一コマンドの実行（自動ログ）

```
2025-11-02 10:09:27.100 INFO  c.s.command.impl.csv.CsvNavigateCommand.execute:50 [execId:EXEC-123, seq:1] - Starting command execution: CsvNavigateCommand
2025-11-02 10:09:27.102 INFO  c.s.command.impl.csv.CsvNavigateCommand.execute:74 [execId:EXEC-123, seq:1] - Command execution completed: CsvNavigateCommand (2ms, input: 1234567bytes, output: 89012bytes, memory: 2MB)
```

ログの各要素の説明：
- `c.s.command.impl.csv.CsvNavigateCommand` - 実際のコマンドクラス名（短縮表示）
- `.execute:50` - メソッド名と行番号（ソースロケーション情報）
- `[execId:EXEC-123, seq:1]` - MDCコンテキスト情報（実行IDとシーケンス番号）

### ExecutionContext付きマルチスレッド実行

```
2025-11-02 10:09:27.063 INFO  com.streamconverter.StreamConverter.run:204 [execId:EXEC-45916851, seq:0] - Starting StreamConverter with 3 commands (executionId: EXEC-45916851)
2025-11-02 10:09:27.099 INFO  com.streamconverter.StreamConverter.lambda$executeMultipleCommandsWithMDC$0:256 [execId:EXEC-45916851, seq:1] - Setting up command 1 of 3: CsvNavigateCommand (sequence: 1)
2025-11-02 10:09:27.100 INFO  c.s.command.impl.csv.CsvNavigateCommand.execute:50 [execId:EXEC-45916851, seq:1] - Starting command execution: CsvNavigateCommand
2025-11-02 10:09:27.102 INFO  c.s.command.impl.csv.CsvNavigateCommand.execute:74 [execId:EXEC-45916851, seq:1] - Command execution completed: CsvNavigateCommand (2ms, input: 98890bytes, output: 98890bytes, memory: 1MB)
2025-11-02 10:09:27.103 INFO  com.streamconverter.StreamConverter.lambda$executeMultipleCommandsWithMDC$0:280 [execId:EXEC-45916851, seq:1] - Completed command: CsvNavigateCommand (sequence: 1)
2025-11-02 10:09:27.105 INFO  com.streamconverter.StreamConverter.executeMultipleCommandsWithMDC:349 [execId:EXEC-45916851, seq:0] - All commands completed successfully (executionId: EXEC-45916851)
```

### エラー発生時のログ

```
2025-11-02 10:09:27.150 ERROR c.s.command.impl.SendHttpCommand.execute:92 [execId:EXEC-123, seq:2] - Command execution failed: SendHttpCommand (1523ms, input: 89012bytes, output: 0bytes, memory: 3MB) - Connection timeout
java.net.SocketTimeoutException: Connect timed out
    at com.streamconverter.command.impl.SendHttpCommand.executeInternal(SendHttpCommand.java:145)
    ...
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

MDC（Mapped Diagnostic Context）により、以下の情報が自動的にログに含まれます：

- `executionId` - StreamConverter実行ごとの一意ID
- `commandSequence` - パイプライン内でのコマンド実行順序
- `stage` - 実行ステージ名（setStage()で設定した場合）
- `threadName` - 実行スレッド名
- `startTime` - 実行開始時刻
- カスタムコンテキスト - ExecutionContext.builder().globalContext()で追加した任意のキー・バリュー

logback設定例：
```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36}.%method:%line [execId:%X{executionId:-}, seq:%X{commandSequence:-}, stage:%X{stage:-}] - %msg%n</pattern>
```

これにより、マルチスレッド環境でも各ログ行がどの実行に属するか追跡可能です。

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
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- ソースロケーション情報（%method:%line）とMDC情報（%X{...}）を含む推奨パターン -->
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36}.%method:%line [execId:%X{executionId:-}, seq:%X{commandSequence:-}, stage:%X{stage:-}] - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/streamconverter.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>logs/streamconverter.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <maxFileSize>10MB</maxFileSize>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level %logger{36}.%method:%line [execId:%X{executionId:-}, seq:%X{commandSequence:-}, stage:%X{stage:-}] - %msg%n</pattern>
        </encoder>
    </appender>

    <logger name="com.streamconverter" level="INFO" additivity="false">
        <appender-ref ref="STDOUT" />
        <appender-ref ref="FILE" />
    </logger>
</configuration>
```

パターンの各要素：
- `%method:%line` - メソッド名と行番号（ソースロケーション情報）
- `%X{executionId:-}` - ExecutionContextの実行ID（MDC）
- `%X{commandSequence:-}` - コマンド実行順序（MDC）
- `%X{stage:-}` - 実行ステージ名（MDC）
- `:-` - MDC値が設定されていない場合のデフォルト値（空文字列）

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

### 4. MDC自動同期（2025年1月追加）

ExecutionContextの共有コンテキストは、ログ出力時に自動的にMDCに同期されます。

```java
// ExecutionContext作成
ExecutionContext context = ExecutionContext.create();

// XMLからuserIdを抽出してMDCに自動設定
XmlNavigateCommand extractUserId = new XmlNavigateCommand(
    TreePath.fromXml("request/userId"),
    new MdcSetupRule(context, "userId")
);

// 後続のコマンドは自動的にMDCでuserIdを参照可能
SampleStreamCommand processor = new SampleStreamCommand("processor");

// パイプライン実行
StreamConverter.createWithContext(context, extractUserId, processor)
    .run(inputStream, outputStream);
```

**動作の仕組み**:
1. `MdcSetupRule`が抽出した値を`ExecutionContext`の共有コンテキストに保存
2. `AbstractStreamCommand`が`ExecutionContextHolder`にコンテキストを設定
3. ログ出力時に`ExecutionContextTurboFilter`が共有コンテキストをMDCに自動同期
4. logback.xmlのパターン`[userId:%X{userId:-}]`が正しく展開される

**ログ出力例**:
```
2025-01-06 14:00:01.123 INFO  [execId:EXEC-123, seq:1, userId:USER12345] - Starting command execution: XmlNavigateCommand
2025-01-06 14:00:01.125 INFO  [execId:EXEC-123, seq:2, userId:USER12345] - Starting command execution: SampleStreamCommand
2025-01-06 14:00:01.127 INFO  [execId:EXEC-123, seq:2, userId:USER12345] - Processing data for user
```

**利点**:
- アプリケーションコードがMDCを意識する必要がない
- マルチスレッド環境でも自動的に伝播
- パフォーマンス最適化（変更がある場合のみMDC操作）

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