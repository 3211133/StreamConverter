# メモリ効率化テスト戦略設計書

## 概要

StreamConverterのメモリ効率化テストの設計原理と実装戦略を定義します。
大容量データ処理におけるメモリ使用量を監視し、ストリーミング処理の設計原理を確実に検証するためのテスト戦略です。

## 🎯 設計原理

### 核となる2つの設計原理

1. **原理1: メモリに全て持ってしまわないこと**
   - ストリーミング処理によりデータをメモリに蓄積しない
   - データサイズに関係なく一定のメモリ使用量を維持

2. **原理2: 逐次処理の並列化でスタックしないこと**
   - 並列パイプライン処理でデッドロックやメモリリークを発生させない
   - タイムアウト内に処理を完了する

## 📋 テスト分類とアプローチ

### 1. プリンシパルテスト（設計原理検証）

#### 1.1 ストリーミング効率テスト
```java
@Test
@DisplayName("ストリーミング効率テスト - データサイズ非依存性")
void testStreamingEfficiency()
```

**検証内容:**
- **データサイズ**: 環境適応型（ヒープの10-30%）
- **メモリ制限**: 絶対値（50MB以下）
- **測定方法**: プロファイラベース測定（GC非依存）
- **期待結果**: データサイズに関係なく一定メモリ使用量

#### 1.2 並列処理安定性テスト
```java
@Test
@DisplayName("並列処理安定性テスト - デッドロック検出")
void testParallelProcessingStability()
```

**検証内容:**
- **データサイズ**: 大容量（環境の30%）
- **パイプライン**: 3-4段階の複合処理
- **タイムアウト**: プラットフォーム適応型
- **期待結果**: 120秒以内の処理完了、メモリリークなし

### 2. 容量別検証テスト

#### 2.1 段階的容量テスト
```java
@ParameterizedTest
@ValueSource(strings = {"SMALL", "MEDIUM", "LARGE", "XLARGE"})
@DisplayName("段階的容量テスト - スケーラビリティ検証")
void testScalableCapacity(String sizeCategory)
```

**テストマトリックス:**
| カテゴリ | データサイズ | メモリ制限 | 目標スループット |
|---------|-------------|-----------|-----------------|
| SMALL   | 10MB        | 5MB       | 50MB/s          |
| MEDIUM  | 100MB       | 15MB      | 80MB/s          |
| LARGE   | 500MB       | 30MB      | 100MB/s         |
| XLARGE  | 1GB+        | 50MB      | 120MB/s         |

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

## 📊 メモリ効率評価メトリクス

### 主要メトリクス

1. **メモリ効率比 (Memory Efficiency Ratio)**
   ```
   効率比 = 使用メモリ量 / 処理データサイズ
   目標: < 0.05 (5%以下)
   ```

2. **メモリ安定性指標 (Memory Stability Index)**
   ```
   安定性 = メモリピーク値 / メモリ平均値
   目標: < 1.2 (20%以内の変動)
   ```

3. **スループット効率 (Throughput Efficiency)**
   ```
   効率 = (データサイズ/処理時間) / メモリ使用量
   目標: > 2.0 MB/s/MB
   ```

### テスト合格基準

#### レベル1: 基本要件
- メモリ効率比 < 10%
- 処理完了率 = 100%
- タイムアウトなし

#### レベル2: 推奨要件  
- メモリ効率比 < 5%
- メモリ安定性指標 < 1.5
- スループット > 50MB/s

#### レベル3: 優秀レベル
- メモリ効率比 < 2%
- メモリ安定性指標 < 1.2
- スループット > 100MB/s

## 🏗️ テスト実装パターン

### パターン1: プリンシパル検証テンプレート

```java
@Test
@DisplayName("設計原理検証: {principle}")
@Timeout(value = 120, unit = TimeUnit.SECONDS)
void testDesignPrinciple_{principle}() {
    // 1. 環境適応型データサイズ決定
    long dataSize = MemoryTestStrategy.getAdaptiveDataSize();
    
    // 2. リソースモニタリング開始
    EnhancedResourceMonitor monitor = new EnhancedResourceMonitor();
    
    // 3. テスト実行
    ResourceUsage usage = monitor.measureExecution(() -> {
        // StreamConverter実行
    });
    
    // 4. 設計原理検証
    assertThat(usage.getMemoryEfficiencyRatio())
        .isLessThan(PRINCIPLE_MEMORY_LIMIT);
    
    // 5. 結果レポート
    reportPrincipleVerification(principle, usage);
}
```

### パターン2: スケーラビリティ検証テンプレート

```java
@ParameterizedTest
@EnumSource(DataSizeCategory.class)
@DisplayName("スケーラビリティ検証: {arguments}")
void testScalability(DataSizeCategory category) {
    ScalabilityTestConfig config = category.getTestConfig();
    
    EnhancedResourceMonitor monitor = new EnhancedResourceMonitor();
    ResourceUsage usage = monitor.measureExecution(() -> {
        processDataWithSize(config.getDataSize());
    });
    
    // カテゴリ別要件検証
    assertThat(usage).meets(config.getMemoryRequirements());
    assertThat(usage).meets(config.getPerformanceRequirements());
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
   ```json
   {
     "testName": "testStreamingEfficiency",
     "timestamp": "2025-08-15T12:34:56Z",
     "environment": {
       "os": "Linux",
       "heapSize": "2048MB",
       "processors": 4
     },
     "metrics": {
       "dataSize": 1073741824,
       "memoryUsed": 45232640,
       "efficiencyRatio": 0.042,
       "throughput": 156.7
     },
     "result": "PASSED"
   }
   ```

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

## 🔄 マイグレーション計画

### Phase 1: 測定インフラ改善 (Week 1-2)
- [ ] EnhancedResourceMonitorの実装
- [ ] JMXベースメモリ測定の導入
- [ ] プロファイラ統合

### Phase 2: テスト戦略適用 (Week 3-4)  
- [ ] 既存テストのリファクタリング
- [ ] プリンシパルテストの実装
- [ ] スケーラビリティテストの実装

### Phase 3: 継続的改善基盤 (Week 5-6)
- [ ] メトリクス収集システム
- [ ] 自動分析レポート
- [ ] 性能回帰検出

## 関連ドキュメント

- [TESTING.md](TESTING.md) - 総合テスト戦略
- [BENCHMARK_IMPLEMENTATION.md](BENCHMARK_IMPLEMENTATION.md) - ベンチマーク実装
- [ARCHITECTURE.md](ARCHITECTURE.md) - システムアーキテクチャ