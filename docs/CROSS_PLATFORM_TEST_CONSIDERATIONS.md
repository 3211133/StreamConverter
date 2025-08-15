# クロスプラットフォームテスト考慮事項設計書

## 概要

StreamConverterプロジェクトにおけるクロスプラットフォーム（Windows/macOS/Linux）での
テスト実行における差異、制約、考慮事項を整理し、適切な対応策を定義します。

## 🎯 基本方針

### 核となる考え方

**「OS差異による性能差は正常動作の範囲内」**
- 処理時間の違いは性能の問題であり、機能の問題ではない
- 重要なのは「正しく動作するかどうか」であり、「どのくらい速いか」ではない
- OS固有の制約は環境の特性として受け入れる

## 📋 OS別特性と考慮事項

### 1. **ファイルI/O処理の差異**

#### Windows固有の特性
```java
// 特性
- NTFSファイルシステムの特性（断片化、ロック機構）
- ウイルススキャナーによるリアルタイムスキャン遅延
- ファイルハンドル管理の違い
- 大容量ファイル作成時の遅延

// テストへの影響
- ファイル作成・読み書きテストが遅くなる可能性
- 一時ファイル削除が遅延する可能性
- しかし、最終的に完了すれば正常動作

// 対応方針
- タイムアウトを十分に長く設定（機能テストでは時間制限を緩く）
- ファイルロック例外の適切なハンドリング
```

#### macOS固有の特性
```java
// 特性
- APFS/HFS+ファイルシステムの特性
- Spotlight indexingによるI/O競合
- ファイルシステム監視（FSEvents）の遅延
- SIPによるファイルアクセス制限

// テストへの影響
- ファイル監視テストで遅延が発生する可能性
- システムディレクトリアクセス時の制限
- しかし、最終的に完了すれば正常動作

// 対応方針
- ファイル監視テストでの待機時間延長
- システム保護領域を避けたテスト設計
```

#### Linux固有の特性
```java
// 特性
- ext4/XFSファイルシステムの高効率
- inotifyによる高速ファイル監視
- 一般的に最も安定した性能

// テストへの影響
- 基準となる性能を示すことが多い
- 最も予測しやすい動作

// 対応方針
- 他OSとの性能比較ベースライン
```

### 2. **スレッド・並列処理の差異**

#### Windows
```java
// 特性
- Win32 APIベースのスレッド実装
- コンテキストスイッチコストが相対的に高い
- スレッドプールの実装差異

// テストへの影響
- 並列処理テストで処理時間が延長される可能性
- スレッド作成・破棄のオーバーヘッド
- しかし、デッドロックしなければ正常動作

// 対応方針
- 並列処理の「完了」に着目、時間は問わない
```

#### macOS
```java
// 特性
- pthreadベース + Grand Central Dispatch (GCD)
- メモリプレッシャー時のスレッド制限
- プロセス優先度制御の違い

// テストへの影響
- 高負荷時にスレッド作成が制限される可能性
- しかし、適切に完了すれば正常動作

// 対応方針
- スレッド制限を前提としたテスト設計
```

#### Linux
```java
// 特性
- NPTL (Native POSIX Thread Library)
- 軽量なスレッド実装
- 高い並列処理性能

// テストへの影響
- 最も安定した並列処理性能
- 他OSとの比較基準

// 対応方針
- ベースライン性能の参考
```

### 3. **メモリ管理の差異**

#### Windows
```java
// 特性
- Virtual Memory Manager
- ページファイル使用時の大幅な性能低下
- .NET Frameworkとの相互作用

// テストへの影響
- メモリ不足時の動作が顕著に遅くなる
- しかし、メモリ効率的な設計なら影響は最小限

// 対応方針
- ストリーミング処理の設計原理確認が重要
```

#### macOS
```java
// 特性
- Memory pressure system
- Compressed memory機能
- 積極的なメモリ回収

// テストへの影響
- メモリプレッシャー発生時に処理が一時停止する可能性
- しかし、最終的に回復すれば正常動作

// 対応方針
- メモリプレッシャー回復の待機時間を考慮
```

#### Linux
```java
// 特性
- 効率的なページキャッシュ
- OOM Killerによる保護
- 予測しやすいメモリ管理

// テストへの影響
- 最も安定したメモリ管理動作

// 対応方針
- 他OSとの比較基準
```

### 4. **ネットワーク処理の差異**

#### OS別I/O多重化
```java
// Linux: epoll (高効率)
// macOS: kqueue (高効率)
// Windows: IOCP (異なるAPI)

// テストへの影響
- HTTP接続テストでの性能差
- 同時接続数の制限差
- しかし、接続が成功すれば正常動作

// 対応方針
- 接続成功率に着目、速度は問わない
```

## 🧪 テスト戦略における考慮事項

### 1. **機能テストでの考慮事項**

#### 基本方針
```java
// ✅ 正しいアプローチ
@Test
@Timeout(value = 600, unit = TimeUnit.SECONDS) // 十分な時間
void testCrossplatformFunctionality() {
    // 機能が正しく動作するかのみを検証
    // 時間は十分に長く設定
    
    boolean result = processLargeData();
    assertTrue(result, "処理が完了しなかった");
    // 処理時間は測定するが判定には使わない
}

// ❌ 間違ったアプローチ  
@Test
void testWithPlatformSpecificTimeout() {
    long timeout = OS.isWindows() ? 300 : 120; // OS別タイムアウト
    // これは性能差を機能差と誤解している
}
```

#### 適用例
```java
// ファイル処理テスト
@Test
void testFileProcessing() {
    // Windows: 遅い、macOS: 中程度、Linux: 速い
    // でも全てで「正しく処理される」ことが重要
    
    File result = processLargeFile(inputFile);
    assertNotNull(result);
    assertTrue(result.exists());
    assertEquals(expectedContent, readFile(result));
}
```

### 2. **性能テストでの考慮事項**

#### OS差異の記録と分析
```java
@Test
void performanceComparisonTest() {
    String osName = System.getProperty("os.name");
    
    long startTime = System.currentTimeMillis();
    processData();
    long duration = System.currentTimeMillis() - startTime;
    
    // OS別性能記録（判定には使わない）
    logger.info("OS: {}, 処理時間: {}ms", osName, duration);
    
    // 機能面での検証のみ
    assertTrue(isProcessingComplete(), "処理が完了していない");
}
```

### 3. **環境固有制約への対応**

#### リソース制限
```java
// Unix系
ulimit -n 1024  // ファイルディスクリプタ制限
ulimit -u 4096  // プロセス数制限

// Windows  
// ハンドル数制限（通常10,000）

// 対応方針
@Test
void testWithResourceConstraints() {
    try {
        // リソース集約的な処理
        performResourceIntensiveTask();
    } catch (ResourceLimitException e) {
        // OS固有の制約は想定範囲内として受け入れ
        logger.warn("OS固有のリソース制限: {}", e.getMessage());
        assumeFalse("リソース制限によりテストスキップ");
    }
}
```

#### ファイルシステム制限
```java
@Test
void testFileSystemLimits() {
    if (OS.isWindows() && isLongPathNotSupported()) {
        // Windows固有の長いパス制限
        assumeTrue("Windowsの長いパス制限によりスキップ");
    }
    
    // ファイルシステム操作テスト
    performFileSystemOperations();
}
```

## 🎛️ 実装ガイドライン

### 1. **タイムアウト設定のベストプラクティス**

#### 機能テスト
```java
// ✅ 推奨: 十分に長いタイムアウト
@Timeout(value = 600, unit = TimeUnit.SECONDS) // 10分

// ✅ 推奨: OS固有制約の受け入れ
@Test
void testFunctionality() {
    // 完了することのみを検証
    // 時間は気にしない
}
```

#### 統合テスト
```java
// ✅ 推奨: さらに長いタイムアウト
@Timeout(value = 1800, unit = TimeUnit.SECONDS) // 30分

// 外部システム連携等で予期しない遅延を考慮
```

### 2. **ログ出力とデバッグ情報**

```java
@Test
void testWithPlatformInformation() {
    // プラットフォーム情報の記録
    logger.info("OS: {}", System.getProperty("os.name"));
    logger.info("Java Version: {}", System.getProperty("java.version"));
    logger.info("Available Memory: {}MB", 
                Runtime.getRuntime().maxMemory() / 1024 / 1024);
    
    // テスト実行
    long startTime = System.currentTimeMillis();
    boolean result = executeTest();
    long duration = System.currentTimeMillis() - startTime;
    
    // 性能情報の記録（判定には使わない）
    logger.info("実行時間: {}ms", duration);
    
    // 機能面での検証
    assertTrue(result, "機能テストが失敗");
}
```

### 3. **OS固有テストの分離**

```java
// OS固有動作のテスト
@Test
@EnabledOnOs(OS.WINDOWS)
void testWindowsSpecificBehavior() {
    // Windows固有の動作テスト
    // 他OSでは実行されない
}

@Test
@EnabledOnOs(OS.MAC)
void testMacSpecificBehavior() {
    // macOS固有の動作テスト
}

@Test
@DisabledOnOs({OS.WINDOWS, OS.MAC})
void testLinuxSpecificBehavior() {
    // Linux固有の動作テスト
}
```

## 📊 監視とメトリクス

### 1. **OS別性能監視**

```java
public class PlatformPerformanceTracker {
    private static final Map<String, List<Long>> performanceData = new HashMap<>();
    
    public static void recordPerformance(String testName, long duration) {
        String osName = System.getProperty("os.name");
        String key = osName + ":" + testName;
        
        performanceData.computeIfAbsent(key, k -> new ArrayList<>()).add(duration);
    }
    
    public static void reportPlatformComparison() {
        // OS別性能比較レポート出力
        // 判定には使わず、参考情報として記録
    }
}
```

### 2. **失敗パターンの分析**

```java
@TestExecutionListener
public class PlatformFailureAnalyzer {
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        String osName = System.getProperty("os.name");
        
        // OS別失敗パターンの記録
        logger.error("テスト失敗: OS={}, テスト={}, 原因={}", 
                    osName, context.getDisplayName(), cause.getMessage());
        
        // OS固有の問題かどうかを分析
        if (isOSSpecificIssue(cause)) {
            logger.warn("OS固有の問題の可能性: {}", cause.getMessage());
        }
    }
}
```

## 🔄 継続的改善

### 1. **OS別安定性追跡**

- 各OSでのテスト成功率の監視
- OS固有失敗パターンの蓄積
- 環境固有制約の文書化

### 2. **性能プロファイルの更新**

- OS別性能特性の定期的な更新
- 新しいOS版での動作確認
- ハードウェア差異の考慮

## 関連ドキュメント

- [Memory Efficiency Test Strategy](MEMORY_EFFICIENCY_TEST_STRATEGY.md) - メモリ効率化テスト戦略
- [TESTING.md](TESTING.md) - 総合テスト戦略
- [Platform Adaptive Test Utils](../streamconverter-tools/src/test/java/com/streamConverter/test/PlatformAdaptiveTestUtils.java) - プラットフォーム適応型テストユーティリティ