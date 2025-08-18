package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.benchmark.LargeDataGenerator;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** メモリ効率とパフォーマンスのテスト - 環境非依存 */
class MemoryEfficiencyTest {

  /** 環境適応型のテストデータサイズを計算 - long型で完全対応 */
  static long getAdaptiveTestDataSize() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // プラットフォーム別の最適化
    String osName = System.getProperty("os.name").toLowerCase();
    double memoryRatio;
    long minSize;
    long maxSize;

    if (osName.contains("windows")) {
      // Windows: より保守的な設定
      memoryRatio = 0.05; // 5%
      minSize = 5L * 1024 * 1024; // 5MB
      maxSize = 200L * 1024 * 1024; // 200MB
    } else if (osName.contains("mac")) {
      // macOS: 中程度の設定
      memoryRatio = 0.08; // 8%
      minSize = 8L * 1024 * 1024; // 8MB
      maxSize = 300L * 1024 * 1024; // 300MB
    } else {
      // Linux/その他: より積極的な設定
      memoryRatio = 0.10; // 10%
      minSize = 10L * 1024 * 1024; // 10MB
      maxSize = 500L * 1024 * 1024; // 500MB
    }

    long calculatedSize = Math.min(Math.max((long) (maxMemory * memoryRatio), minSize), maxSize);

    System.out.println(
        "Platform: "
            + osName
            + ", Max heap: "
            + (maxMemory / 1024 / 1024)
            + "MB, Test data size: "
            + (calculatedSize / 1024 / 1024)
            + "MB (ratio: "
            + (memoryRatio * 100)
            + "%)");
    return calculatedSize;
  }

  /** 大容量テスト用の環境適応型サイズ計算 - long型で完全対応 */
  static long getLargeTestDataSize() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // プラットフォーム別の大容量テスト設定
    String osName = System.getProperty("os.name").toLowerCase();
    double memoryRatio;
    long minSize;
    long maxSize;

    if (osName.contains("windows")) {
      // Windows: より保守的（メモリ管理が厳格）
      memoryRatio = 0.15; // 15%
      minSize = 30L * 1024 * 1024; // 30MB
      maxSize = 500L * 1024 * 1024; // 500MB
    } else if (osName.contains("mac")) {
      // macOS: 中程度（メモリ管理は比較的良好）
      memoryRatio = 0.25; // 25%
      minSize = 50L * 1024 * 1024; // 50MB
      maxSize = 1024L * 1024 * 1024; // 1GB
    } else {
      // Linux/その他: より積極的（サーバー環境想定）
      memoryRatio = 0.30; // 30%
      minSize = 50L * 1024 * 1024; // 50MB
      maxSize = 2048L * 1024 * 1024; // 2GB
    }

    long calculatedLargeSize =
        Math.min(Math.max((long) (maxMemory * memoryRatio), minSize), maxSize);

    System.out.println(
        "Large test - Platform: "
            + osName
            + ", Max heap: "
            + (maxMemory / 1024 / 1024)
            + "MB, Test data size: "
            + (calculatedLargeSize / 1024 / 1024)
            + "MB (ratio: "
            + (memoryRatio * 100)
            + "%)");
    return calculatedLargeSize;
  }

  @Test
  @DisplayName("環境適応型メモリ効率テスト - 設計原理検証")
  @Timeout(value = 60, unit = TimeUnit.SECONDS)
  void testAdaptiveMemoryEfficiency() throws IOException {
    // 環境適応型テストデータサイズを取得（long型完全対応）
    long dataSize = getAdaptiveTestDataSize();

    // メモリ使用量監視
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();

    InputStream largeInput = LargeDataGenerator.createLargeDataStream("CSV", dataSize);
    // メモリ効率測定のため、出力は捨てる（設計原理：メモリに全て持たない）
    OutputStream output = new NullOutputStream();

    // 3つのコマンドでパイプライン処理
    StreamConverter converter =
        new StreamConverter(
            new IStreamCommand[] {
              new SampleStreamCommand("stage1"),
              new SampleStreamCommand("stage2"),
              new SampleStreamCommand("stage3")
            });

    // GCを実行してベースライン取得
    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    // 大容量ストリーム処理実行
    List<CommandResult> result = converter.run(largeInput, output);

    // 処理後のメモリ確認
    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // 結果検証
    assertNotNull(result);
    assertEquals(3, result.size());

    // 設計原理1: メモリに全て持たないこと - プラットフォーム適応型の制限
    String osName = System.getProperty("os.name").toLowerCase();
    double memoryLimitRatio;

    if (osName.contains("windows")) {
      memoryLimitRatio = 0.60; // Windows: 60%制限（GC負荷を考慮）
    } else if (osName.contains("mac")) {
      memoryLimitRatio = 0.55; // macOS: 55%制限
    } else {
      memoryLimitRatio = 0.50; // Linux/その他: 50%制限（より厳しい基準）
    }

    long maxAcceptableMemory = (long) (dataSize * memoryLimitRatio);
    System.out.println("=== 環境適応型メモリ効率テスト結果 ===");
    System.out.println("Test data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + "MB");
    System.out.println("Before processing: " + (beforeMemory / 1024 / 1024) + "MB");
    System.out.println("After processing: " + (afterMemory / 1024 / 1024) + "MB");
    System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println(
        "Max acceptable ("
            + (int) (memoryLimitRatio * 100)
            + "% of data): "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: メモリに全てを持ってしまった可能性があります。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB ("
            + (int) (memoryLimitRatio * 100)
            + "% of "
            + (dataSize / 1024 / 1024)
            + "MB data)");
  }

  @Test
  @DisplayName("単一コマンド最適パステスト - 設計原理検証")
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void testSingleCommandOptimalPath() throws IOException {
    Runtime runtime = Runtime.getRuntime();

    // 環境適応型のデータサイズを使用（long型完全対応）
    long dataSize = getAdaptiveTestDataSize() / 2; // より小さなサイズでテスト
    InputStream input = LargeDataGenerator.createLargeDataStream("CSV", dataSize);
    // メモリ効率測定のため、出力は捨てる
    OutputStream output = new NullOutputStream();

    // 単一コマンド（最適パス）
    StreamConverter converter =
        new StreamConverter(new IStreamCommand[] {new SampleStreamCommand("single")});

    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    List<CommandResult> result = converter.run(input, output);

    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // 結果検証
    assertNotNull(result);
    assertEquals(1, result.size());

    // 単一コマンドの場合、メモリ増加は最小限であるべき（プラットフォーム適応型制限）
    String osName = System.getProperty("os.name").toLowerCase();
    double singleCommandLimitRatio;

    if (osName.contains("windows")) {
      singleCommandLimitRatio = 0.25; // Windows: 25%制限
    } else if (osName.contains("mac")) {
      singleCommandLimitRatio = 0.22; // macOS: 22%制限
    } else {
      singleCommandLimitRatio = 0.20; // Linux/その他: 20%制限
    }

    long maxAcceptableMemory = (long) (dataSize * singleCommandLimitRatio);
    System.out.println("=== 単一コマンド最適パステスト結果 ===");
    System.out.println("Test data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Single command memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println(
        "Max acceptable ("
            + (int) (singleCommandLimitRatio * 100)
            + "% of data): "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: 単一コマンドのメモリ効率が悪すぎます。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB ("
            + (int) (singleCommandLimitRatio * 100)
            + "% of "
            + (dataSize / 1024 / 1024)
            + "MB data)");
  }

  @Test
  @DisplayName("並列処理スタック検証テスト - 設計原理2")
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  void testParallelProcessingNoStack() throws IOException {
    Runtime runtime = Runtime.getRuntime();

    // 環境適応型の大容量データサイズを取得（long型完全対応）
    long dataSize = getLargeTestDataSize();
    InputStream largeInput = LargeDataGenerator.createLargeDataStream("CSV", dataSize);
    // メモリ効率測定のため、出力は捨てる
    OutputStream output = new NullOutputStream();

    // 3つのコマンドでパイプライン処理
    StreamConverter converter =
        new StreamConverter(
            new IStreamCommand[] {
              new SampleStreamCommand("stage1"),
              new SampleStreamCommand("stage2"),
              new SampleStreamCommand("stage3")
            });

    // 初期メモリ状態記録
    System.gc();
    System.gc(); // 2回GCを実行して確実にクリーンアップ
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } // GC完了待機
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
    long maxMemoryBefore = runtime.maxMemory();

    System.out.println("=== 1GB Stream Processing Test ===");
    System.out.println("Data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Max heap size: " + (maxMemoryBefore / 1024 / 1024) + "MB");
    System.out.println("Memory before processing: " + (beforeMemory / 1024 / 1024) + "MB");

    // 1GBストリーム処理実行
    long startTime = System.currentTimeMillis();
    List<CommandResult> result = converter.run(largeInput, output);
    long endTime = System.currentTimeMillis();

    // 処理後のメモリ確認
    System.gc();
    System.gc(); // 2回GCを実行
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } // GC完了待機
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // パフォーマンス情報
    long processingTimeMs = endTime - startTime;
    double throughputMBps = (dataSize / 1024.0 / 1024.0) / (processingTimeMs / 1000.0);

    System.out.println("=== 並列処理スタック検証テスト結果 ===");
    System.out.println("Test data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Memory after processing: " + (afterMemory / 1024 / 1024) + "MB");
    System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println("Processing time: " + processingTimeMs + "ms");
    System.out.println("Throughput: " + String.format("%.2f", throughputMBps) + " MB/s");

    // 結果検証
    assertNotNull(result);
    assertEquals(3, result.size());

    // 設計原理2: 逐次処理の並列化でスタックしないこと - プラットフォーム適応型制限
    String osName = System.getProperty("os.name").toLowerCase();
    double parallelLimitRatio;

    if (osName.contains("windows")) {
      parallelLimitRatio = 0.40; // Windows: 40%制限（並列処理でのGC負荷を考慮）
    } else if (osName.contains("mac")) {
      parallelLimitRatio = 0.35; // macOS: 35%制限
    } else {
      parallelLimitRatio = 0.30; // Linux/その他: 30%制限
    }

    long maxAcceptableMemory = (long) (dataSize * parallelLimitRatio);
    System.out.println(
        "Max acceptable ("
            + (int) (parallelLimitRatio * 100)
            + "% of data): "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: 並列処理でメモリがスタックした可能性があります。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB ("
            + (int) (parallelLimitRatio * 100)
            + "% of "
            + (dataSize / 1024 / 1024)
            + "MB data)");

    // 処理が完了したこと自体が「スタックしない」ことの証明
    // タイムアウト内に完了すればスタックしていない
    System.out.println("✅ 並列処理でスタックせず、設計原理を満たしています！");
  }

  /** 全ての出力を破棄するOutputStream（メモリ効率測定用） */
  private static class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
      // 何もしない - データを破棄
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      // 何もしない - データを破棄
    }
  }

  // LargeDataInputStreamを削除 - LargeDataGenerator.createLargeDataStream()に統合完了
  // 技術的負債解消: int制限を解除し、既存のlong対応実装を利用
}
