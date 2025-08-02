# Command Architecture

StreamConverterのコマンドアーキテクチャは、柔軟で拡張可能なパイプライン処理を提供します。コマンドパターン、ファクトリーパターン、デコレーターパターンを組み合わせた設計により、保守性と再利用性を実現しています。

## アーキテクチャ概要

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│  CommandFactory │───▶│  CommandConfig   │───▶│ IStreamCommand  │
│   (Creator)     │    │  (Configuration) │    │  (Interface)    │
└─────────────────┘    └──────────────────┘    └─────────────────┘
         │                                               │
         │                                               ▼
         ▼                                    ┌─────────────────┐
┌─────────────────┐                          │AbstractStream-  │
│LoggingDecorator │                          │Command (Base)   │
│   (Wrapper)     │                          └─────────────────┘
└─────────────────┘                                   │
                                                       ▼
                                            ┌─────────────────┐
                                            │Concrete Command │
                                            │Implementations  │
                                            └─────────────────┘
```

## 主要コンポーネント

### 1. IStreamCommand (Interface)

```java
public interface IStreamCommand {
    void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
```

すべてのストリーム処理コマンドが実装すべき基本インターフェース。

### 2. AbstractStreamCommand (Base Class)

```java
public abstract class AbstractStreamCommand implements IStreamCommand {
    // 共通の実装とテンプレートメソッド
    // ログ出力の基本機能
    // エラーハンドリングの統一化
}
```

コマンドの基本機能と共通処理を提供する抽象基底クラス。

### 3. CommandConfig (Configuration)

```java
public class CommandConfig {
    private final Class<? extends IStreamCommand> commandClass;
    private final Object[] args;
    private final String description;
    
    // コマンドの生成情報を保持
}
```

コマンドの生成に必要な設定情報を管理するクラス。

### 4. CommandFactory (Factory)

```java
public class CommandFactory {
    public static IStreamCommand createWithLogging(Class<? extends IStreamCommand> commandClass, Object... args);
    public static IStreamCommand[] createPipelineWithLogging(CommandConfig... configs);
}
```

ログ機能付きコマンドを統一的に生成するファクトリークラス。

### 5. LoggingDecorator (Decorator)

```java
public class LoggingDecorator implements IStreamCommand {
    private final IStreamCommand delegate;
    
    // 既存コマンドにログ機能を追加
}
```

既存のコマンドにログ機能を追加するデコレータークラス。

## 使用方法

### 基本的なコマンド作成

```java
// 1. 直接インスタンス化
IStreamCommand csvCommand = new CsvNavigateCommand("productName");

// 2. ファクトリー経由（ログ機能付き）
IStreamCommand loggedCsvCommand = CommandFactory.createWithLogging(
    CsvNavigateCommand.class, "productName"
);

// 3. 手動でデコレート
IStreamCommand decoratedCommand = new LoggingDecorator(csvCommand);
```

### パイプライン作成

```java
// 設定を使用したパイプライン作成
CommandConfig[] configs = {
    new CommandConfig(CsvNavigateCommand.class, "input", "CSV入力処理"),
    new CommandConfig(SendHttpCommand.class, "http://api.example.com", "API送信"),
    new CommandConfig(JsonNavigateCommand.class, "$.result", "結果抽出")
};

IStreamCommand[] pipeline = CommandFactory.createPipelineWithLogging(configs);
StreamConverter converter = StreamConverter.create(pipeline);
```

### カスタムコマンドの実装

```java
public class CustomProcessingCommand extends AbstractStreamCommand {
    private final String parameter;
    
    public CustomProcessingCommand(String parameter) {
        this.parameter = parameter;
    }
    
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // カスタム処理の実装
        log.info("Starting custom processing with parameter: {}", parameter);
        
        try {
            // 実際の処理
            performCustomProcessing(inputStream, outputStream);
            
        } catch (Exception e) {
            log.error("Custom processing failed", e);
            throw new StreamProcessingException("Custom processing failed", e);
        }
    }
    
    private void performCustomProcessing(InputStream input, OutputStream output) {
        // 具体的な処理ロジック
    }
}
```

## 利用可能なコマンド

### ナビゲーションコマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `CsvNavigateCommand` | CSVフィールド抽出 | `"productName"` |
| `JsonNavigateCommand` | JSONパス抽出 | `"$.user.name"` |
| `XmlNavigateCommand` | XPath抽出 | `"//product/@id"` |

### 変換コマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `convert` | 文字エンコーディング変換 | `"UTF-8", "Shift_JIS"` |
| `xml.ConvertCommand` | XML変換 | `"stylesheet.xsl"` |
| `xml.ValidateCommand` | XMLバリデーション | `"schema.xsd"` |

### 通信コマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `SendHttpCommand` | HTTP送信 | `"http://api.example.com"` |

## パフォーマンス考慮事項

### ファクトリー使用時の注意点

```java
// 推奨: 事前に設定を準備
CommandConfig csvConfig = new CommandConfig(CsvNavigateCommand.class, "field");
IStreamCommand command = CommandFactory.createWithLogging(csvConfig);

// 避ける: 頻繁なリフレクション使用
for (int i = 0; i < 1000000; i++) {
    IStreamCommand cmd = CommandFactory.createWithLogging(CsvNavigateCommand.class, "field");
    // パフォーマンスが低下
}
```

### メモリ効率的な使用

```java
// 大容量ファイル処理時
IStreamCommand[] efficientPipeline = {
    // 軽量なコマンドから順番に配置
    CommandFactory.createWithLogging(CsvNavigateCommand.class, "id"),
    CommandFactory.createWithLogging(SendHttpCommand.class, "http://api.example.com"),
    // 重い処理は最後に
    CommandFactory.createWithLogging(xml.ConvertCommand.class, "complex.xsl")
};
```

## エラーハンドリング

### 統一されたエラーハンドリング

```java
public class RobustProcessingCommand extends AbstractStreamCommand {
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        try {
            // 処理実装
            
        } catch (IOException e) {
            // I/Oエラーはそのまま再スロー
            throw e;
            
        } catch (Exception e) {
            // その他のエラーはStreamProcessingExceptionでラップ
            throw new StreamProcessingException("Processing failed in " + getClass().getSimpleName(), e);
        }
    }
}
```

### ファクトリーでのエラーハンドリング

```java
try {
    IStreamCommand command = CommandFactory.createWithLogging(
        NonExistentCommand.class, "param"
    );
} catch (IllegalArgumentException e) {
    log.error("Command creation failed: {}", e.getMessage());
    // フォールバック処理
}
```

## 拡張ガイド

### 新しいコマンドの追加

1. `AbstractStreamCommand`を継承
2. `execute`メソッドを実装
3. 適切なコンストラクタを定義
4. エラーハンドリングを実装
5. ユニットテストを作成

```java
public class NewFeatureCommand extends AbstractStreamCommand {
    public NewFeatureCommand(String config) {
        // 初期化
    }
    
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // 実装
    }
}
```

### カスタムデコレーターの実装

```java
public class PerformanceDecorator implements IStreamCommand {
    private final IStreamCommand delegate;
    
    public PerformanceDecorator(IStreamCommand delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        long startTime = System.nanoTime();
        try {
            delegate.execute(inputStream, outputStream);
        } finally {
            long duration = System.nanoTime() - startTime;
            log.info("Command {} took {} ms", delegate.getClass().getSimpleName(), duration / 1_000_000);
        }
    }
}
```

## ベストプラクティス

### 1. ファクトリーの活用

```java
// 推奨: ファクトリーを使用してログ機能を自動追加
IStreamCommand command = CommandFactory.createWithLogging(MyCommand.class, param);

// 非推奨: 手動での組み立て
IStreamCommand command = new LoggingDecorator(new MyCommand(param));
```

### 2. 設定の再利用

```java
// 設定を再利用して一貫性を保つ
CommandConfig csvConfig = new CommandConfig(CsvNavigateCommand.class, "name");

IStreamCommand command1 = CommandFactory.createWithLogging(csvConfig);
IStreamCommand command2 = CommandFactory.createWithLogging(csvConfig);
```

### 3. エラーハンドリングの統一

```java
// AbstractStreamCommandを継承して統一されたエラーハンドリングを利用
public class SafeCommand extends AbstractStreamCommand {
    // 統一されたログ出力とエラーハンドリングが自動的に適用される
}
```

## 関連ドキュメント

- [Auto-Logging Infrastructure](AUTO_LOGGING.md) - 自動ログ機能の詳細
- [Testing Strategy](TESTING.md) - テスト戦略とパフォーマンス最適化
- [Documentation Index](README.md) - その他のドキュメント