# Testing Guide

## 概要

このドキュメントは、StreamConverterプロジェクトのテスト戦略と環境依存性について説明します。

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
- Java 17以上
- ヒープメモリ: 512MB以上
- 利用可能ディスク容量: 100MB以上

### 推奨要件
- Java 17以上
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