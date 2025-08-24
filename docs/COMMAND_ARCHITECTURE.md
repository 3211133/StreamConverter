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
    /**
     * ExecutionContext対応の拡張実行メソッド（推奨）
     * MDCコンテキスト伝播とトレーサビリティ機能を提供
     */
    default void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
        throws IOException {
        // デフォルト実装：後方互換性のため基本executeメソッドに委譲
        execute(inputStream, outputStream);
    }

    /**
     * 基本実行メソッド（後方互換性のため維持）
     */
    void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
```

すべてのストリーム処理コマンドが実装すべき基本インターフェース。

**実装オプション:**
- **基本コマンド**: 2パラメータのexecuteメソッドのみ実装
- **コンテキスト対応コマンド**: 3パラメータのexecuteメソッドをオーバーライドして拡張機能を利用

### 2. AbstractStreamCommand (Base Class)

```java
public abstract class AbstractStreamCommand implements IStreamCommand {
    private static final Logger log = LoggerFactory.getLogger(AbstractStreamCommand.class);

    /**
     * final実装：自動ログ機能付きの実行メソッド
     * - 実行時間とメモリ使用量の測定
     * - 入出力データサイズの測定
     * - エラーハンドリングと詳細ログ出力
     */
    @Override
    public final void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // MeasuredInputStream/OutputStreamでデータサイズ測定
        // executeInternal()を呼び出して実際の処理を実行
        // 実行結果とパフォーマンス情報をログ出力
    }

    /**
     * サブクラスで実装すべき抽象メソッド
     */
    protected abstract void executeInternal(InputStream inputStream, OutputStream outputStream) 
        throws IOException;
}
```

コマンドの基本機能と共通処理を提供する抽象基底クラス。

**主要機能:**
- **自動ログ機能**: 実行時間、メモリ使用量、データサイズの自動測定
- **テンプレートメソッドパターン**: executeInternal()でサブクラスが実際の処理を実装
- **統一エラーハンドリング**: 例外発生時の詳細ログ出力

### 3. CommandFactory (Factory)

```java
public class CommandFactory {
    /**
     * ログ機能付きコマンドを生成
     * AbstractStreamCommandの場合は重複を避けるため生成のみ
     */
    public static <T extends IStreamCommand> T createWithLogging(
        Class<T> commandClass, Object... args);

    /**
     * パイプライン全体をログ機能付きで生成
     */
    public static IStreamCommand[] createPipelineWithLogging(
        CommandConfig... configs);
}
```

ログ機能付きコマンドを統一的に生成するファクトリークラス。

**実装仕様:**
- AbstractStreamCommand継承クラスは既にログ機能があるため、LoggingDecoratorでラップしない
- その他のIStreamCommand実装は自動的にLoggingDecoratorでラップ
- リフレクションを使用してコンストラクタ引数からインスタンス生成

### 4. LoggingDecorator (Decorator)

```java
public class LoggingDecorator implements IStreamCommand {
    private final IStreamCommand delegate;
    private final Logger log;
    private final String commandName;

    /**
     * 既存コマンドをログ機能付きでラップ
     */
    public LoggingDecorator(IStreamCommand delegate) {
        this.delegate = delegate;
        this.commandName = delegate.getClass().getSimpleName();
        this.log = LoggerFactory.getLogger(LoggingDecorator.class);
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // 実行前後にログ出力を追加して delegate.execute() を呼び出し
    }
}
```

既存のコマンドにログ機能を追加するデコレータークラス。

**用途:**
- AbstractStreamCommandを継承していないカスタムコマンド向け
- 既存の実装を変更せずにログ機能を追加

## 使用方法

### 基本的なコマンド作成

```java
// 1. 直接インスタンス化（AbstractStreamCommandは自動的にログ機能付き）
IStreamCommand csvCommand = new CsvNavigateCommand("productName");

// 2. ファクトリー経由（推奨方法）
IStreamCommand loggedCsvCommand = CommandFactory.createWithLogging(
    CsvNavigateCommand.class, "productName"
);

// 3. 手動でデコレート（カスタムコマンド用）
IStreamCommand customCommand = new MyCustomCommand();
IStreamCommand decoratedCommand = new LoggingDecorator(customCommand);

// 4. ExecutionContext対応コマンド
IStreamCommand contextAwareCommand = new ContextAwareCsvCommand("productName") {
    @Override
    public void execute(InputStream input, OutputStream output, ExecutionContext context) {
        context.applyToMDCWithStage("csv-processing");
        // コンテキスト情報を活用した処理
        super.execute(input, output, context);
    }
};
```

### パイプライン作成

```java
// 直接配列で作成（シンプルな方法）
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),
    new SendHttpCommand("http://api.example.com"),
    new JsonNavigateCommand("$.result")
};

// StreamConverterでの実行
StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);

// ExecutionContext付きで実行（推奨）
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

StreamConverter contextConverter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = contextConverter.run(inputStream, outputStream);
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

### ナビゲーション・変換コマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `CsvNavigateCommand` | CSV特定列の変換 | `"productName"` |
| `JsonNavigateCommand` | JSON特定パスの変換 | `"$.user.name"` |
| `XmlNavigateCommand` | XML特定要素の変換 | `"//product/@id"` |

### フィルタリングコマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `CsvFilterCommand` | CSV行フィルタリング | `columnSelectors, hasHeader` |
| `JsonFilterCommand` | JSON要素フィルタリング | `jsonPath, predicate` |
| `XmlFilterCommand` | XML要素フィルタリング | `xpath, predicate` |

### 変換コマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `CharacterConvertCommand` | 文字エンコーディング変換 | `"UTF-8", "Shift_JIS"` |
| `LineEndingNormalizeCommand` | 改行コード正規化 | `LineEndingType.UNIX` |
| `xml.ConvertCommand` | XML変換 | `"stylesheet.xsl"` |

### バリデーションコマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `CsvValidateCommand` | CSV構造検証 | `requiredColumns` |
| `JsonValidateCommand` | JSONスキーマ検証 | `"schema.json"` |
| `xml.ValidateCommand` | XMLスキーマ検証 | `"schema.xsd"` |

### 通信コマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `SendHttpCommand` | HTTP送信 | `"http://api.example.com"` |

### ユーティリティコマンド

| クラス | 用途 | 引数例 |
|--------|------|--------|
| `SampleStreamCommand` | サンプル処理・デバッグ | `"processor"` |

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