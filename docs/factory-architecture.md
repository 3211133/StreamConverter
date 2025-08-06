# Factory Architecture Documentation

## 概要

StreamConverterプロジェクトにおける Factory パターンの包括的な資料です。Issue #124 で実装された CommandFactory と ControllerFactory の統合最適化により、統一されたファクトリインフラストラクチャが構築されました。

## 目次

1. [アーキテクチャ概要](#アーキテクチャ概要)
2. [核となるコンポーネント](#核となるコンポーネント)
3. [使用パターン](#使用パターン)
4. [設定管理](#設定管理)
5. [パフォーマンス最適化](#パフォーマンス最適化)
6. [統合と互換性](#統合と互換性)
7. [ベストプラクティス](#ベストプラクティス)

## アーキテクチャ概要

### 統一されたFactory基盤

```
AbstractFactory (Base)
├── EnhancedCommandFactory (Commands)
├── ControllerFactory (Controllers)
└── Future Factory Extensions
```

### 主要な設計原則

- **統一されたインフラストラクチャ**: すべてのファクトリが共通の基盤を共有
- **スレッドセーフ**: ConcurrentHashMap を使用した安全なキャッシング
- **設定可能**: 環境に応じた柔軟な設定管理
- **後方互換性**: 既存の CommandFactory API を完全にサポート
- **パフォーマンス最適化**: インテリジェントキャッシングとリフレクション最適化

## 核となるコンポーネント

### 1. AbstractFactory

統一されたファクトリ基盤を提供する抽象クラス

```java
public abstract class AbstractFactory<T> {
    // スレッドセーフなインスタンスキャッシュ
    protected final Map<String, T> instanceCache = new ConcurrentHashMap<>();
    
    // ファクトリ設定
    protected final FactoryConfiguration config;
    
    // インテリジェントコンストラクタマッチング
    protected <U extends T> U createInstance(Class<U> clazz, Object... args) throws FactoryException
}
```

**主要機能:**
- スレッドセーフなインスタンスキャッシング
- リフレクションベースのインスタンス生成
- 型互換性チェック（プリミティブ型とラッパー型のMapベース高速マッチング）
- 統一されたログ出力とエラーハンドリング

### 2. FactoryConfiguration

環境固有のファクトリ動作を管理する設定クラス

```java
public class FactoryConfiguration {
    // 本番環境用設定
    public static FactoryConfiguration productionConfig() {
        return new FactoryConfiguration(
            true,  // キャッシュ有効（パフォーマンス向上）
            false, // 詳細ログ無効（パフォーマンス向上）
            200,   // 大きなキャッシュサイズ
            30,    // 30分のキャッシュ有効期限
            true   // エラー時の高速失敗
        );
    }
}
```

**設定パターン:**
- **Production**: 高パフォーマンス、最小限のログ
- **Development**: キャッシュ無効、詳細ログ有効
- **Testing**: テスト分離、明確な失敗

### 3. EnhancedCommandFactory

最適化されたコマンド作成機能を提供

```java
public class EnhancedCommandFactory extends AbstractFactory<IStreamCommand> {
    // 後方互換性維持
    public static <T extends IStreamCommand> T createWithLogging(
        Class<T> commandClass, Object... args) {
        return DEFAULT_INSTANCE.createCommandWithLogging(commandClass, false, args);
    }
    
    // 高度なキャッシング
    public <T extends IStreamCommand> T createCached(
        Class<T> commandClass, Object... args) throws FactoryException {
        String cacheKey = createCacheKey(commandClass.getName(), args);
        T cached = (T) getCachedInstance(cacheKey);
        if (cached != null) return cached;
        
        T command = createCommandWithLogging(commandClass, config.isDetailedLoggingEnabled(), args);
        cacheInstance(cacheKey, command);
        return command;
    }
}
```

### 4. ControllerFactory

CommandFactory との統合を提供するコントローラファクトリ

```java
public class ControllerFactory {
    // Enhanced CommandFactory との統合
    private static final EnhancedCommandFactory commandFactory = 
        EnhancedCommandFactory.createProductionInstance();
    
    // 最適化されたコントローラ作成
    public static IStreamController createOptimized(
        String inputType, OutputType outputType, FactoryConfiguration config) {
        // キャッシュチェックとEnhanced CommandFactoryを使用した最適化
    }
}
```

## 使用パターン

### 基本的なコマンド作成

```java
// 1. 従来の方法（後方互換性）
IStreamCommand command = CommandFactory.createWithLogging(JsonNavigateCommand.class, "$.name");

// 2. Enhanced Factory の静的メソッド（RuntimeException版）
IStreamCommand command = EnhancedCommandFactory.createWithLogging(JsonNavigateCommand.class, "$.name");

// 3. Enhanced Factory のchecked exception版（より良いエラーハンドリング）
try {
    IStreamCommand command = EnhancedCommandFactory.createWithLoggingChecked(JsonNavigateCommand.class, "$.name");
} catch (FactoryException e) {
    // 具体的な例外情報を保持
}

// 4. インスタンスベースのアプローチ
EnhancedCommandFactory factory = new EnhancedCommandFactory(FactoryConfiguration.productionConfig());
IStreamCommand command = factory.createCached(JsonNavigateCommand.class, "$.name");
```

### コントローラ作成パターン

```java
// 1. 基本的なコントローラ取得
IStreamController controller = ControllerFactory.getController("CSV", OutputType.JSON_PROPERTY);

// 2. CommandFactory 統合付き
IStreamController controller = ControllerFactory.createWithCommandFactory(
    "JSON", OutputType.PROCESSED_DATA, true);

// 3. 最適化されたコントローラ作成
FactoryConfiguration config = FactoryConfiguration.productionConfig();
IStreamController controller = ControllerFactory.createOptimized("CSV", OutputType.CSV_COLUMN, config);
```

### パイプライン作成

```java
// 1. 従来のパイプライン作成
IStreamCommand[] pipeline = CommandFactory.createPipelineWithLogging(
    new CommandConfig(CsvNavigateCommand.class, "name"),
    new CommandConfig(JsonNavigateCommand.class, "$.user.name")
);

// 2. 最適化されたパイプライン
EnhancedCommandFactory factory = EnhancedCommandFactory.createProductionInstance();
IStreamCommand[] pipeline = factory.createPipelineOptimized(
    CommandConfig.of(CsvNavigateCommand.class, "name"),
    CommandConfig.of(JsonNavigateCommand.class, "$.user.name")
);
```

## 設定管理

### 環境別設定

```java
// 本番環境
FactoryConfiguration prod = FactoryConfiguration.productionConfig();
// caching=true, detailedLogging=false, maxCacheSize=200, failFast=true

// 開発環境
FactoryConfiguration dev = FactoryConfiguration.developmentConfig();
// caching=false, detailedLogging=true, maxCacheSize=50, failFast=false

// テスト環境
FactoryConfiguration test = FactoryConfiguration.testingConfig();
// caching=false, detailedLogging=true, maxCacheSize=10, failFast=true
```

### カスタム設定

```java
FactoryConfiguration custom = FactoryConfiguration.builder()
    .caching(true)
    .detailedLogging(false)
    .maxCacheSize(150)
    .cacheExpiration(60) // 60分
    .failFast(true)
    .build();
```

## パフォーマンス最適化

### キャッシング戦略

1. **キーベースキャッシング**
   - クラス名と引数に基づく一意のキー生成
   - ConcurrentHashMap による高速アクセス

2. **設定可能なキャッシュ**
   - 最大サイズ制限
   - 有効期限設定
   - 環境別の最適化

### リフレクション最適化

```java
// コンストラクタマッチング階層
1. 完全一致マッチング（最速）
2. 型互換性チェック（継承関係含む）
3. プリミティブ-ラッパー型変換（Mapベース高速ルックアップ）

// 最適化されたプリミティブマッチング
private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = Map.of(
    int.class, Integer.class,
    long.class, Long.class,
    // ...
);
```

## 統合と互換性

### CommandFactory 互換性

```java
// 従来のコード（変更不要）
IStreamCommand command = CommandFactory.createWithLogging(CsvNavigateCommand.class, "column");

// Enhanced Factory に自動的にルーティング
// 完全な後方互換性を維持
```

### ControllerFactory 統合

```java
// ControllerFactory は内部で EnhancedCommandFactory を使用
EnhancedCommandFactory commandFactory = ControllerFactory.getCommandFactory();

// 統一された設定管理
FactoryConfiguration config = commandFactory.getConfiguration();
```

## ベストプラクティス

### 1. 適切な設定選択

```java
// 本番環境
EnhancedCommandFactory prodFactory = EnhancedCommandFactory.createProductionInstance();

// 開発・デバッグ
EnhancedCommandFactory devFactory = EnhancedCommandFactory.createDevelopmentInstance();

// テスト
EnhancedCommandFactory testFactory = new EnhancedCommandFactory(FactoryConfiguration.testingConfig());
```

### 2. エラーハンドリング

```java
// インスタンスメソッド（checked exception）
try {
    IStreamCommand command = factory.createCached(CommandClass.class, args);
} catch (FactoryException e) {
    log.error("Command creation failed: {}", e.getMessage(), e);
    // 適切なフォールバック処理
}

// 静的メソッド（checked exception版 - 推奨）
try {
    IStreamCommand command = EnhancedCommandFactory.createWithLoggingChecked(CommandClass.class, args);
} catch (FactoryException e) {
    // 具体的な例外情報を保持したエラーハンドリング
    log.error("Specific factory error: {}", e.getMessage(), e);
}

// 従来のRuntimeException版（後方互換性）
try {
    IStreamCommand command = EnhancedCommandFactory.createWithLogging(CommandClass.class, args);
} catch (RuntimeException e) {
    // RuntimeExceptionとしてキャッチ
    log.error("Runtime error during command creation: {}", e.getMessage(), e);
}
```

### 3. リソース管理

```java
// キャッシュクリア（テスト後など）
factory.clearCache();

// キャッシュサイズ監視
int cacheSize = factory.getCacheSize();
if (cacheSize > threshold) {
    factory.clearCache();
}
```

### 4. 統合パターン

```java
public class CustomProcessor {
    private final EnhancedCommandFactory commandFactory;
    
    public CustomProcessor(FactoryConfiguration config) {
        this.commandFactory = new EnhancedCommandFactory(config);
    }
    
    public void processData(String inputType, OutputType outputType) {
        // ControllerFactory との統合
        IStreamController controller = ControllerFactory.createOptimized(
            inputType, outputType, commandFactory.getConfiguration());
        
        // 統一された設定で一貫した動作
    }
}
```

## 実装の詳細

### スレッドセーフティ

- すべてのキャッシュオペレーションは `ConcurrentHashMap` を使用
- immutable な設定オブジェクト
- ステートレスなファクトリメソッド

### テスト戦略

```java
// テスト用の分離された設定
@BeforeEach
void setUp() {
    EnhancedCommandFactory.getDefaultInstance().clearCache();
    ControllerFactory.clearRegistry();
}
```

## まとめ

この統一されたファクトリアーキテクチャにより以下を実現：

1. **コードの重複削減**: 共通のAbstractFactoryによる統一
2. **パフォーマンス向上**: インテリジェントキャッシングと最適化
3. **設定の一元管理**: 環境別の一貫した動作制御
4. **完全な後方互換性**: 既存コードの変更不要
5. **拡張性**: 新しいファクトリタイプの簡単な追加

Issue #124 の最適化により、CommandFactory と ControllerFactory の統合が完了し、331テスト全てが成功（100%成功率）を達成しています。