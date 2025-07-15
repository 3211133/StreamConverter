# 自動ログ出力機能設計書

## 概要
ユーザーが追加実装するライブラリで、ログ出力漏れを防ぐための自動ログ出力メカニズムを実装します。

## 設計方針

### 1. 多層防御アプローチ
- **Level 1**: AbstractStreamCommand強化（基本的な実行ログ）
- **Level 2**: デコレーターパターン（詳細なログ追加）
- **Level 3**: ファクトリーパターン（統一的な生成管理）

### 2. 自動ログ出力の対象

#### 必須ログ出力項目
- **実行開始**: コマンド名、開始時刻
- **実行終了**: コマンド名、終了時刻、実行時間
- **例外発生**: 例外の種類、メッセージ、スタックトレース
- **パフォーマンス**: 実行時間、処理データサイズ

#### 推奨ログ出力項目
- **入力データ情報**: データサイズ、形式
- **出力データ情報**: データサイズ、変換結果
- **リソース使用量**: メモリ使用量、CPU使用率
- **設定情報**: コマンド固有の設定値

## 実装アプローチ

### 1. AbstractStreamCommand強化

```java
public abstract class AbstractStreamCommand implements IStreamCommand {
  private static final Logger log = LoggerFactory.getLogger(AbstractStreamCommand.class);
  
  @Override
  public final void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    String commandName = this.getClass().getSimpleName();
    long startTime = System.currentTimeMillis();
    
    log.info("Starting command execution: {}", commandName);
    log.debug("Command details: {}", getCommandDetails());
    
    try {
      // 入力データサイズ測定
      InputStream measuredInput = new MeasuredInputStream(inputStream);
      OutputStream measuredOutput = new MeasuredOutputStream(outputStream);
      
      // 実際の処理実行
      _execute(measuredInput, measuredOutput);
      
      // 成功ログ出力
      long duration = System.currentTimeMillis() - startTime;
      log.info("Command execution completed: {} ({}ms, input: {}bytes, output: {}bytes)",
               commandName, duration, 
               measuredInput.getBytesRead(), measuredOutput.getBytesWritten());
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Command execution failed: {} ({}ms) - {}", 
               commandName, duration, e.getMessage(), e);
      throw e;
    }
  }
  
  protected abstract String getCommandDetails();
  protected abstract void _execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
```

### 2. LoggingDecorator実装

```java
public class LoggingDecorator implements IStreamCommand {
  private final IStreamCommand delegate;
  private final Logger log;
  
  public LoggingDecorator(IStreamCommand delegate) {
    this.delegate = delegate;
    this.log = LoggerFactory.getLogger(delegate.getClass());
  }
  
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    String commandName = delegate.getClass().getSimpleName();
    
    // 詳細なログ出力
    log.info("=== {} Execution Started ===", commandName);
    log.debug("Input stream: {}", inputStream.getClass().getSimpleName());
    log.debug("Output stream: {}", outputStream.getClass().getSimpleName());
    
    try {
      delegate.execute(inputStream, outputStream);
      log.info("=== {} Execution Completed Successfully ===", commandName);
    } catch (Exception e) {
      log.error("=== {} Execution Failed ===", commandName, e);
      throw e;
    }
  }
}
```

### 3. CommandFactory実装

```java
public class CommandFactory {
  private static final Logger log = LoggerFactory.getLogger(CommandFactory.class);
  
  public static <T extends IStreamCommand> T createWithLogging(Class<T> commandClass, Object... args) {
    try {
      T command = createInstance(commandClass, args);
      log.info("Created command instance: {}", commandClass.getSimpleName());
      return new LoggingDecorator(command);
    } catch (Exception e) {
      log.error("Failed to create command instance: {}", commandClass.getSimpleName(), e);
      throw new RuntimeException("Command creation failed", e);
    }
  }
  
  public static IStreamCommand[] createPipelineWithLogging(CommandConfig... configs) {
    List<IStreamCommand> commands = new ArrayList<>();
    
    for (CommandConfig config : configs) {
      IStreamCommand command = createWithLogging(config.getCommandClass(), config.getArgs());
      commands.add(command);
    }
    
    log.info("Created pipeline with {} commands", commands.size());
    return commands.toArray(new IStreamCommand[0]);
  }
}
```

### 4. パフォーマンス測定の自動化

```java
public class PerformanceMonitor {
  private static final Logger log = LoggerFactory.getLogger(PerformanceMonitor.class);
  
  public static void measureAndLog(String operation, Runnable task) {
    long startTime = System.currentTimeMillis();
    long startMemory = getUsedMemory();
    
    try {
      task.run();
      
      long duration = System.currentTimeMillis() - startTime;
      long memoryUsed = getUsedMemory() - startMemory;
      
      log.info("Performance: {} completed in {}ms, memory used: {}MB", 
               operation, duration, memoryUsed / 1024 / 1024);
      
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.error("Performance: {} failed after {}ms", operation, duration, e);
      throw e;
    }
  }
  
  private static long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}
```

## 使用方法

### 1. 標準的なコマンド実装
```java
// ユーザーは通常通りAbstractStreamCommandを継承するだけ
public class UserCommand extends AbstractStreamCommand {
  @Override
  protected String getCommandDetails() {
    return "UserCommand with custom logic";
  }
  
  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    // 実装コード
    // ログ出力は自動的に追加される
  }
}
```

### 2. ファクトリーを使用した生成
```java
// 自動ログ出力機能付きでコマンドを生成
IStreamCommand command = CommandFactory.createWithLogging(UserCommand.class, "parameter1", "parameter2");

// パイプライン全体を一括生成
IStreamCommand[] pipeline = CommandFactory.createPipelineWithLogging(
    new CommandConfig(CsvNavigateCommand.class, "name"),
    new CommandConfig(JsonNavigateCommand.class, "$.user.name"),
    new CommandConfig(XmlNavigateCommand.class, "user/name")
);
```

### 3. 既存コマンドのラッピング
```java
// 既存のコマンドに詳細ログを追加
IStreamCommand originalCommand = new CsvNavigateCommand("name");
IStreamCommand loggedCommand = new LoggingDecorator(originalCommand);
```

## 利点

### 1. 開発者の負担軽減
- ログ出力を意識する必要がない
- 統一されたログフォーマット
- 自動的なパフォーマンス測定

### 2. 運用時の可視性向上
- 全コマンドの実行状況が把握可能
- 問題発生時の原因特定が容易
- パフォーマンス分析データの自動収集

### 3. 保守性の向上
- 一箇所でログ出力ロジックを管理
- 新しいログ要件への対応が容易
- テストやデバッグが簡単

## 実装優先度
1. **AbstractStreamCommand強化** (必須)
2. **MeasuredStream実装** (推奨)
3. **LoggingDecorator実装** (推奨)
4. **CommandFactory実装** (オプション)
5. **PerformanceMonitor実装** (オプション)

この設計により、ユーザーが新しいコマンドを実装する際に、自動的に適切なログ出力が行われ、運用時の可視性が大幅に向上します。