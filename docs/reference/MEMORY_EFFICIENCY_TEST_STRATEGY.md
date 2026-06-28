# メモリ効率化テスト戦略設計書

## 概要

StreamConverterのメモリ効率化テストの設計原理と実装戦略を定義します。
大容量データ処理におけるメモリ使用量を監視し、ストリーミング処理の設計原理を確実に検証するためのテスト戦略です。

## 🎯 設計原理

### 核となる2つの設計原理

1. **原理1: メモリに全て持ってしまわないこと**
   - ストリーミング処理によりデータをメモリに蓄積しない
   - 検証方法: データサイズが10倍、100倍になってもメモリ使用量がほぼ変わらない
   - 判定基準: **データサイズとメモリ使用量が比例関係にない**（Yes/No判定）

2. **原理2: 逐次処理の並列化でスタックしないこと**
   - 並列パイプライン処理でデッドロックやメモリリークを発生させない
   - 処理が最終的に完了する（時間は問わない）

## 📋 テスト分類とアプローチ

### 1. プリンシパルテスト（設計原理検証）

#### 1.1 ストリーミング効率テスト
```java
@Test
@DisplayName("ストリーミング効率テスト - データサイズ非依存性")
void testStreamingEfficiency()
```

**検証内容:**
- **データサイズ**: 段階的増加（10MB → 100MB → 1GB → 10GB）
- **測定方法**: プロファイラベース測定（GC非依存）
- **検証原理**: ストリーミング処理の確認
  - 10MBと10GBで **ほぼ同じメモリ使用量** であることを確認
  - データサイズ1000倍に対してメモリが比例増加していない
- **判定**: データをメモリに全て持っているか？ **Yes/No**

#### 1.2 並列処理安定性テスト
```java
@Test
@DisplayName("並列処理安定性テスト - デッドロック検出")
void testParallelProcessingStability()
```

**検証内容:**
- **データサイズ**: 大容量（環境の30%）
- **パイプライン**: 3-4段階の複合処理
- **タイムアウト**: 十分な時間（10分等）
- **期待結果**: 処理の完了、メモリリークなし

### 2. 容量別検証テスト

#### 2.1 段階的容量テスト
```java
@ParameterizedTest
@ValueSource(strings = {"SMALL", "MEDIUM", "LARGE", "XLARGE"})
@DisplayName("段階的容量テスト - スケーラビリティ検証")
void testScalableCapacity(String sizeCategory)
```

**テストマトリックス:**
| カテゴリ | データサイズ | メモリ使用量期待値 | 判定基準 |
|---------|-------------|------------------|---------|
| SMALL   | 10MB        | ~30MB            | ベースライン |
| MEDIUM  | 100MB       | ~30MB            | **10倍データでも同じメモリ** |
| LARGE   | 1GB         | ~30MB            | **100倍データでも同じメモリ** |
| XLARGE  | 10GB        | ~30MB            | **1000倍データでも同じメモリ** |

**判定**: 全サイズで同程度のメモリ使用量 → **ストリーミング成功**  
**判定**: データサイズに比例してメモリ増加 → **ストリーミング失敗（全データをメモリに保持）**

### 3. プラットフォーム適応型テスト

#### 3.1 環境適応型メモリテスト
```java
@EnabledIf("PlatformAdaptiveTestUtils.hasAdequateResources")
@DisplayName("環境適応型メモリテスト")
void testEnvironmentAdaptiveMemory()
```

**適応ロジック:**
- **Windows/macOS**: メモリ制限を20%緩和
- **CI環境**: タイムアウトを50%延長
- **低スペック環境**: テストサイズを自動縮小

## 🔧 測定インフラストラクチャ

### メモリ測定の改善

#### 現在の問題
```java
// 不安定なGC依存測定
System.gc();
long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
```

#### 改善されたアプローチ
```java
// プロファイラベース測定
MemoryProfiler profiler = new MemoryProfiler();
profiler.startMonitoring();

// 処理実行
converter.run(input, output);

MemoryUsageSnapshot snapshot = profiler.stopAndGetSnapshot();
```

### ResourceMonitor統合アプローチ

```java
public class EnhancedResourceMonitor {
    // JMXベースの正確なメモリ測定
    private final MemoryMXBean memoryMXBean;
    private final List<GarbageCollectorMXBean> gcBeans;
    
    public ResourceUsage measureExecution(Runnable execution) {
        MemorySnapshot beforeSnapshot = captureMemorySnapshot();
        long startTime = System.nanoTime();
        
        execution.run();
        
        long endTime = System.nanoTime();
        MemorySnapshot afterSnapshot = captureMemorySnapshot();
        
        return new ResourceUsage(beforeSnapshot, afterSnapshot, 
                                 endTime - startTime);
    }
}
```

## 📊 ストリーミング処理検証

### 判定基準（Yes/No判定）

#### 1. **ストリーミング処理検証**
```java
// 単純明快な判定
boolean isStreaming = (memory_10GB <= memory_10MB * 2);
// データサイズ1000倍でもメモリが2倍以下 → ストリーミング成功

if (isStreaming) {
    System.out.println("✅ ストリーミング処理: データをメモリに全て持っていない");
} else {
    System.out.println("❌ 非ストリーミング処理: データをメモリに全て持っている");
}
```

#### 2. **並列処理安定性検証**
```java
// 処理完了の確認（時間は問わない）
boolean isStable = processingCompleted && !errorOccurred;

if (isStable) {
    System.out.println("✅ 並列処理安定: スタックしていない");  
    System.out.println("   処理完了: " + processingCompleted);
} else {
    System.out.println("❌ 並列処理不安定: スタックまたはエラー");
    System.out.println("   処理完了: " + processingCompleted);
    System.out.println("   エラー発生: " + errorOccurred);
}
```

### テスト合格基準（シンプル）

#### 必須要件
- **ストリーミング処理**: `true` （データを全て持っていない）
- **並列処理安定性**: `true` （処理が完了する）
- **処理完了率**: `100%`

#### 失敗ケース
- データサイズに比例してメモリ使用量が増加 → **ストリーミング失敗**
- 処理が完了しない（デッドロック、無限ループ等） → **並列処理失敗**

## 🏗️ テスト実装パターン

### パターン1: プリンシパル検証テンプレート

```java
@Test
@DisplayName("設計原理検証: ストリーミング処理確認")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
void testStreamingPrinciple() {
    // 1. 小容量データでベースライン測定
    long memory10MB = measureMemoryUsage(10_000_000L);    // 10MB
    
    // 2. 大容量データで測定
    long memory10GB = measureMemoryUsage(10_000_000_000L); // 10GB (1000倍)
    
    // 3. ストリーミング処理判定（Yes/No）
    boolean isStreaming = (memory10GB <= memory10MB * 2);
    
    // 4. 判定結果
    if (isStreaming) {
        logger.info("✅ ストリーミング処理: データをメモリに全て持っていない");
        logger.info("   10MB: {}MB, 10GB: {}MB", 
                   memory10MB/1024/1024, memory10GB/1024/1024);
    } else {
        logger.error("❌ 非ストリーミング処理: データをメモリに全て持っている");
        logger.error("   10MB: {}MB, 10GB: {}MB ({}倍増加)", 
                    memory10MB/1024/1024, memory10GB/1024/1024,
                    (double)memory10GB/memory10MB);
    }
    
    // 5. アサーション（単純明快）
    assertTrue(isStreaming, 
        "データをメモリに全て持ってしまっている: 10GB処理時のメモリが10MB処理時の2倍を超過");
}
```

### パターン2: 並列処理安定性検証テンプレート

```java
@Test
@DisplayName("並列処理安定性検証: スタック検出")
@Timeout(value = 600, unit = TimeUnit.SECONDS) // 10分 - 十分な時間
void testParallelProcessingStability() {
    // 1. 大容量データで並列パイプライン実行
    long dataSize = 1024L * 1024 * 1024; // 1GB
    
    boolean completed = false;
    boolean errorOccurred = false;
    
    try {
        // 並列パイプライン実行
        converter.run(createLargeDataStream(dataSize), new NullOutputStream());
        completed = true;
    } catch (Exception e) {
        errorOccurred = true;
        logger.error("並列処理中にエラー発生: {}", e.getMessage());
    }
    
    // 2. シンプルな判定（完了したかどうかのみ）
    boolean isStable = completed && !errorOccurred;
    
    // 3. 結果ログ
    if (isStable) {
        logger.info("✅ 並列処理安定: スタックしていない");
    } else {
        logger.error("❌ 並列処理不安定: スタックまたはエラー");
    }
    
    // 4. アサーション（シンプル）
    assertTrue(isStable, "並列処理がスタックした（完了しなかった）");
}
```

## 🎛️ 設定可能なテストパラメータ

### 環境変数による制御

```bash
# メモリテスト制御
MEMORY_TEST_ENABLED=true                    # メモリテスト有効化
MEMORY_TEST_MAX_DATA_SIZE=1073741824      # 最大データサイズ (1GB)
MEMORY_TEST_MEMORY_LIMIT=52428800         # メモリ制限 (50MB)

# プラットフォーム適応制御
PLATFORM_ADAPTIVE_ENABLED=true            # プラットフォーム適応有効化
PERFORMANCE_COEFFICIENT_OVERRIDE=0.8      # 性能係数手動設定

# CI/CD環境制御  
CI_MEMORY_TEST_TIMEOUT_MULTIPLIER=2.0     # CI環境でのタイムアウト倍率
CI_MEMORY_LIMIT_RELAXATION=1.5            # CI環境でのメモリ制限緩和
```

### JVMシステムプロパティ

```java
// テスト実行時の設定
-Dmemory.test.strategy=ADAPTIVE            # ADAPTIVE, FIXED, MINIMAL
-Dmemory.test.verification.level=STANDARD  # BASIC, STANDARD, STRICT
-Dmemory.test.profiler.enabled=true       # プロファイラ有効化
```

## 📈 継続的改善フレームワーク

### メトリクス収集と分析

1. **テスト実行ログの構造化**
   - testName、timestamp、environment、metrics、result を含む構造化ログを出力する

2. **傾向分析とベンチマーク更新**
   - 月次でメトリクス分析実施
   - 環境別性能プロファイル更新
   - 目標値の適切性レビュー

### 失敗分析と改善

1. **失敗パターンの分類**
   - メモリリーク検出
   - スループット低下
   - タイムアウト発生

2. **根本原因分析プロセス**
   - JFRプロファイリング
   - ヒープダンプ分析
   - GCログ解析

## 関連ドキュメント

- [TESTING.md](TESTING.md) - 総合テスト戦略
- [BENCHMARK_IMPLEMENTATION.md](BENCHMARK_IMPLEMENTATION.md) - ベンチマーク実装
- [ARCHITECTURE.md](ARCHITECTURE.md) - システムアーキテクチャ