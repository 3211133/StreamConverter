# StreamConverter テスト戦略とガイド

このドキュメントでは、StreamConverterプロジェクトの包括的なテスト戦略、実行方法、カバレッジ測定について説明します。

## 🎯 テスト戦略

### テスト対象とアプローチ

StreamConverterプロジェクトでは、以下の包括的なテストアプローチを採用しています：

#### 1. **単体テスト (Unit Tests)**
- **StreamConverter クラス**: コア機能の詳細テスト
  - コンストラクタテスト（正常系・異常系）
  - run メソッドのテスト（正常系・異常系）
  - 複数コマンドの連携テスト
  - 並行処理とパイプライン動作の検証

- **IStreamCommand 実装クラス**: 各コマンドの責務テスト
  - execute メソッドの正常系・異常系テスト
  - データ変換ロジックの検証
  - エラーハンドリングテスト
  - 特殊条件下での動作テスト

#### 2. **統合テスト (Integration Tests)**
- **パイプライン処理テスト**: 複数コマンドの組み合わせ動作検証
- **外部システム連携テスト**: HTTP通信、ファイルI/O等
- **コンテキスト伝播テスト**: マルチスレッド環境でのMDC動作

#### 3. **パフォーマンステスト (Performance Tests)**
- **ベンチマークテスト**: 大容量データ処理性能測定
- **メモリ効率テスト**: メモリ使用量の制約確認（設計原理ベース）
- **スループットテスト**: 処理速度とリソース使用量の最適化検証
- **プラットフォーム適応型テスト**: OS/環境別性能特性への自動適応

#### 4. **メモリ効率化テスト (Memory Efficiency Tests)**
- **設計原理検証**: 2つの核となる設計原理の確実な検証
  - 原理1: メモリに全て持ってしまわないこと（ストリーミング効率）
  - 原理2: 逐次処理の並列化でスタックしないこと（並列処理安定性）
- **環境適応型測定**: プラットフォームとリソースに応じた動的テスト調整
- **プロファイラベース測定**: GC非依存の正確なメモリ使用量監視
- **段階的容量テスト**: SMALL/MEDIUM/LARGE/XLARGEカテゴリ別検証

> 📖 **詳細**: [Memory Efficiency Test Strategy](MEMORY_EFFICIENCY_TEST_STRATEGY.md) - メモリ効率化テストの包括的な設計書

#### 5. **テスト手法とフレームワーク**
- **JUnit 5**: 最新のテストフレームワーク活用
  - `@DisplayName`: 分かりやすいテスト名
  - `@ParameterizedTest`: データ駆動テスト
  - `@EnabledIf`: 条件付きテスト実行
  - `@Timeout`: 実行時間制限
- **Mockito**: モックオブジェクトによる依存関係の分離
- **Awaitility**: 非同期処理のテスト支援

## 📁 テスト構造

### ディレクトリ構成

```
src/test/java/com/streamConverter/
├── StreamConverterTest.java              # StreamConverter コアテスト
├── StreamConverterIntegrationTest.java   # 統合テスト
├── StreamConverterMDCIntegrationTest.java  # コンテキスト機能テスト
├── MainTest.java                          # エントリーポイントテスト
├── MemoryEfficiencyTest.java             # メモリ効率テスト
├── api/
│   ├── StreamBuilderTest.java            # Fluent API テスト
│   └── StreamsTest.java                  # 静的ファクトリテスト
├── benchmark/
│   ├── BenchmarkInfrastructureTest.java  # ベンチマーク基盤テスト
│   ├── LargeDataBenchmark.java           # 大容量データベンチマーク
│   └── MemoryEfficiencyQuickTest.java    # メモリ効率クイックテスト
├── command/
│   ├── AbstractStreamCommandTest.java    # 抽象コマンドテスト
│   ├── ValidationDecoratorTest.java      # デコレータパターンテスト
│   └── impl/
│       ├── CsvNavigateCommandTest.java   # CSV処理テスト
│       ├── JsonNavigateCommandTest.java  # JSON処理テスト
│       ├── XmlNavigateCommandTest.java   # XML処理テスト
│       ├── SampleStreamCommandTest.java  # サンプルコマンドテスト
│       ├── charaCode/
│       │   └── ConvertTest.java          # 文字コード変換テスト
│       ├── csv/
│       │   └── CsvValidateCommandTest.java # CSVバリデーションテスト
│       ├── json/
│       │   └── JsonValidateCommandTest.java # JSONバリデーションテスト
│       └── xml/
│           ├── ConvertCommandTest.java    # XML変換テスト
│           └── ValidateTest.java          # XMLバリデーションテスト
├── context/
│   └── ExecutionContextTest.java         # 実行コンテキストテスト
├── demo/
│   └── StreamConverterDemoTest.java      # デモ機能テスト
├── examples/
│   └── QuickStartTest.java               # クイックスタートテスト
├── pathHandler/
│   └── FixedStaXPathHandlerTest.java     # XMLパスハンドラテスト
└── validation/
    └── ValidationResultTest.java         # バリデーション結果テスト
```

### テストリソース

```
src/test/resources/
├── logback.xml          # テスト用ログ設定
├── test-schema.xsd      # XMLバリデーション用スキーマ
├── valid-test.xml       # 有効なXMLテストファイル
└── invalid-test.xml     # 無効なXMLテストファイル
```

## 🚀 テスト実行方法

### 基本的なテスト実行

> ベンチマーク系テストはデフォルトでスキップされます。必要に応じて後述の benchmark タスクを実行してください。

```bash
# 全テストの実行
./gradlew test

# 特定のテストクラスの実行
./gradlew test --tests "com.streamConverter.StreamConverterTest"

# 特定のテストメソッドの実行
./gradlew test --tests "com.streamConverter.StreamConverterTest.testRunWithValidStreams"

# パッケージ単位でのテスト実行
./gradlew test --tests "com.streamConverter.command.*"
```

### 専門的なテスト実行

```bash
# ベンチマークテスト
./gradlew benchmarkAll                    # 全ベンチマークテスト
./gradlew benchmarkLargeData             # 大容量データテスト
./gradlew benchmarkMemoryEfficiency     # メモリ効率テスト
./gradlew benchmarkInfrastructure        # ベンチマーク基盤テスト

# 統合テスト（タグベース）
./gradlew test --tests "*IntegrationTest"

# 並行テスト実行（パフォーマンス向上）
./gradlew test --parallel --max-workers=4
```

### 継続的インテグレーション

```bash
# CI環境での実行（詳細ログ付き）
./gradlew test --info --stacktrace

# カバレッジ付きテスト実行
./gradlew test jacocoTestReport

# 品質チェック付きテスト
./gradlew check  # テスト + 静的解析 + コードフォーマット
```

## 📊 テストレポートとカバレッジ

### レポート生成場所

- **HTMLテストレポート**: `build/reports/tests/test/index.html`
- **XMLテストレポート**: `build/test-results/test/`
- **JaCoCoカバレッジレポート**: `build/reports/jacoco/test/html/index.html`
- **ベンチマークレポート**: `build/reports/tests/benchmarkLargeData/index.html`

### カバレッジ測定

プロジェクトにはJaCoCoプラグインが設定済みで、以下のコマンドでカバレッジレポートを生成できます：

```bash
# テスト実行とカバレッジレポート生成
./gradlew test jacocoTestReport

# カバレッジ検証（設定された閾値をチェック）
./gradlew jacocoTestCoverageVerification
```

#### カバレッジ目標値

- **ライン カバレッジ**: 80%以上
- **ブランチ カバレッジ**: 70%以上
- **コア機能 (StreamConverter, AbstractStreamCommand)**: 90%以上

### パフォーマンステストの結果解釈

#### ベンチマークテスト結果

1. **大容量データテスト**: 1GB、2GB、5GBデータの処理性能
   - メモリ使用量制限の確認
   - スループット測定
   - 安定性評価

2. **メモリ効率テスト**: ストリーミング処理の効果測定
   - ヒープ使用量の監視
   - ガベージコレクション影響の評価

## 🔧 テスト設定とカスタマイズ

### Gradle設定

```kotlin
// build.gradle.kts でのテスト設定
test {
    useJUnitPlatform()
    
    // JVM設定
    jvmArgs("-Xmx2g", "-XX:+UseG1GC")
    
    // システムプロパティ
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.default", "concurrent")
    
    // テストログ設定
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}
```

### テスト固有の設定

#### 大容量データテスト用メモリ設定

```kotlin
tasks.register<Test>("benchmarkLargeData") {
    jvmArgs("-Xms1g", "-Xmx3g")
    systemProperty("test.data.size", "large")
}
```

#### 条件付きテスト実行

```java
@EnabledIf("hasEnoughMemoryFor5GB")
void test5GBDataProcessing() {
    // 5GBデータ処理テスト
}

static boolean hasEnoughMemoryFor5GB() {
    return Runtime.getRuntime().maxMemory() > 2L * 1024 * 1024 * 1024;
}
```

## 🧠 メモリ効率化テスト戦略

### 改善されたメモリ効率テストアプローチ

従来のGC依存測定から、プロファイラベースの正確な測定への移行：

#### 従来の問題のあるアプローチ
```java
// ❌ 不安定なGC依存測定
System.gc();
long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
// 処理実行
System.gc();
long afterMemory = runtime.totalMemory() - runtime.freeMemory();
```

#### 改善されたアプローチ
```java
// ✅ 安定したプロファイラベース測定
@Test
@DisplayName("設計原理検証: ストリーミング効率")
@EnabledIf("PlatformAdaptiveTestUtils.hasAdequateResources")
void testStreamingEfficiencyPrinciple() {
    // 環境適応型データサイズ決定
    long dataSize = PlatformAdaptiveTestUtils.getAdaptiveDataSize(100L * 1024 * 1024);
    
    // プロファイラベース測定
    EnhancedResourceMonitor monitor = new EnhancedResourceMonitor();
    ResourceUsage usage = monitor.measureExecution(() -> {
        converter.run(createLargeDataStream(dataSize), new NullOutputStream());
    });
    
    // 設計原理1: メモリに全て持たないこと
    // データサイズ1000倍でもメモリ使用量が2倍以下かをチェック
    long memory10MB = measureMemoryUsage(10_000_000L);    // 10MB
    long memory10GB = measureMemoryUsage(10_000_000_000L); // 10GB
    
    boolean isStreaming = (memory10GB <= memory10MB * 2);
    
    if (isStreaming) {
        logger.info("✅ ストリーミング処理: データをメモリに全て持っていない");
    } else {
        logger.error("❌ 非ストリーミング処理: データをメモリに全て持っている");
    }
    
    assertTrue(isStreaming, 
        "データをメモリに全て持ってしまっている: 10GB処理時のメモリが10MB処理時の2倍を超過");
}
```

### 設計原理ベーステストマトリックス

| テストカテゴリ | データサイズ | 検証原理 | 判定基準 |
|---------------|-------------|---------|---------|
| **ストリーミング効率** | 10MB vs 10GB | 原理1 | 10GB時メモリ ≤ 10MB時メモリ×2 |
| **並列処理安定性** | 大容量 | 原理2 | 処理が完了する（時間は問わない） |
| **統合検証** | 両方実施 | 両方 | ストリーミング=TRUE & 安定性=TRUE |

### プラットフォーム適応型制限値

```java
// OS別制限値の動的調整
public class MemoryTestLimits {
    public static long getAdaptiveMemoryLimit(long baseLimit) {
        double platformFactor = PlatformAdaptiveTestUtils.getPlatformPerformanceFactor();
        double ciRelaxation = PlatformAdaptiveTestUtils.isCI() ? 1.5 : 1.0;
        
        return Math.round(baseLimit / platformFactor * ciRelaxation);
    }
}
```

## 環境依存テスト

以下のテストは特定の環境条件により失敗する可能性があります：

### 🔧 メモリ依存テスト

#### MemoryEfficiencyTest.java
```java
@EnabledIf("hasEnoughMemory")              // 512MB以上のヒープメモリが必要
@EnabledIf("hasEnoughMemoryFor1GB")        // 800MB以上のヒープメモリが必要
@Timeout(value = 30, unit = TimeUnit.SECONDS)  // 30秒タイムアウト
@Timeout(value = 120, unit = TimeUnit.SECONDS) // 2分タイムアウト
```

#### LargeDataBenchmark.java
```java
@EnabledIf("hasEnoughMemory")              // 512MB以上
@EnabledIf("hasEnoughMemoryForLarge")      // より大きなメモリ要件
@Timeout(value = 300, unit = TimeUnit.SECONDS) // 5分タイムアウト
```

**失敗する可能性がある環境：**
- 低メモリ環境（CI/CD、コンテナ環境）
- `-Xmx`フラグでヒープサイズが制限されている環境
- メモリ不足時のGC競合状態

### ⏱️ タイミング・並行性依存テスト

#### MDCBehaviorTest.java - マルチスレッドテスト
```java
latch.await(5, TimeUnit.SECONDS);         // 5秒待機
latch.await(10, TimeUnit.SECONDS);        // 10秒待機
Thread.sleep(50);                         // スリープによる同期
Thread.sleep(100);                        // タイミング調整
```

#### BenchmarkInfrastructureTest.java
```java
System.gc();                              // GC依存
System.nanoTime() / System.currentTimeMillis() // タイミング測定
```

**失敗する可能性がある環境：**
- 高負荷環境（CPU使用率が高い状況）
- 仮想化環境での時間精度問題
- GCアルゴリズムの違い（G1GC、ZGC、等）
- マルチテナント環境でのリソース競合

### 🗃️ ファイルシステム依存テスト

#### ValidationDecoratorTest.java、JsonValidateCommandTest.java
```java
@TempDir Path tempDir;                    // 一時ディレクトリ作成
Files.write(tempDir.resolve("schema.json"), ...) // ファイル書き込み
```

**失敗する可能性がある環境：**
- 読み取り専用ファイルシステム
- 権限制限のあるコンテナ環境
- ディスク容量不足
- Windows/Linux間のパス区切り文字問題

### 🌐 ネットワーク依存テスト

#### StreamConverterIntegrationTest.java
```java
new SendHttpCommand("https://httpbin.org/post")  // 外部HTTP接続
new SendHttpCommand("http://httpbin.org/post")   // HTTP接続テスト
```

**失敗する可能性がある環境：**
- ネットワーク接続のない環境
- プロキシ設定が必要な企業環境
- ファイアウォールによるアクセス制限
- 外部サービス（httpbin.org）のダウンタイム

### 📊 性能・ベンチマーク依存テスト

#### BenchmarkInfrastructureTest.java
```java
void testBenchmarkConsistency()          // 性能一貫性テスト
// 以前間欠的に失敗していたテスト
```

**失敗する可能性がある環境：**
- CPU性能が低い環境
- 共有リソース環境での性能変動
- 仮想化オーバーヘッドが大きい環境
- バックグラウンドプロセスによる負荷変動

## 推奨対策

### 1. 環境変数による制御追加
```java
@EnabledIf("System.getenv('CI_ENVIRONMENT') == null")
@EnabledIf("Boolean.parseBoolean(System.getProperty('enable.performance.tests', 'true'))")
```

### 2. リトライ機構の実装
```java
@RepeatedTest(3)  // 3回まで再実行
@Flaky           // 間欠的失敗を許容
```

### 3. モック使用の拡大
```java
// 外部依存をモック化
@MockBean private HttpClient httpClient;
```

## テスト実行環境の要件

### 最小要件
- Java: See [VERSION_MANAGEMENT.md - Java Compatibility](VERSION_MANAGEMENT.md#java-compatibility)
- ヒープメモリ: 512MB以上
- 利用可能ディスク容量: 100MB以上

### 推奨要件
- Java: See [VERSION_MANAGEMENT.md - Java Compatibility](VERSION_MANAGEMENT.md#java-compatibility)
- ヒープメモリ: 1GB以上 (`-Xmx1g`)
- 利用可能ディスク容量: 500MB以上
- ネットワーク接続（HTTPテスト用）

### CI/CD環境での注意点
- メモリ制約のあるコンテナ環境では、大容量データテストが無効化される
- ネットワーク制限のある環境では、HTTP関連テストが失敗する可能性がある
- 高負荷環境では、タイミング依存テストが間欠的に失敗する可能性がある

## テスト実行コマンド

```bash
# 全テスト実行
./gradlew test

# メモリ使用量を増やしてテスト実行
./gradlew test -Xmx2g

# 特定のテストクラスのみ実行
./gradlew test --tests "com.streamConverter.MemoryEfficiencyTest"

# ベンチマークテストを除外
./gradlew test --exclude-task benchmarkTest
```

## 現在のテスト状況

- **総テスト数**: 229
- **成功率**: 100%
- **実行時間**: 約47秒
- **最終更新**: 2025年7月31日

すべてのテストは現在成功していますが、上記の環境条件によっては失敗する可能性があります。

## 関連ドキュメント

- [Memory Efficiency Test Strategy](MEMORY_EFFICIENCY_TEST_STRATEGY.md) - メモリ効率化テストの包括的な設計書
- [Cross-Platform Test Considerations](CROSS_PLATFORM_TEST_CONSIDERATIONS.md) - クロスプラットフォームテストの考慮事項
- [Command Architecture](../archived/COMMAND_ARCHITECTURE.md) - コマンドパターンとテストアーキテクチャ
- [Auto-Logging](../AUTO_LOGGING.md) - ログ機能とテスト環境での活用
- [Benchmark Implementation](../guides/BENCHMARK_IMPLEMENTATION.md) - ベンチマーク実装詳細
- [Documentation Index](../INDEX.md) - その他のドキュメント