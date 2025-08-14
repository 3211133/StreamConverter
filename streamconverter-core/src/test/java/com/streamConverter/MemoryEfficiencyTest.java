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

/** メモリ効率とパフォーマンスのテスト */
class MemoryEfficiencyTest {

  /** 環境適応型のテストデータサイズを計算 - long型で完全対応 */
  static long getAdaptiveTestDataSize() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // ヒープサイズの10%をテストデータサイズとして使用（最小10MB、最大500MB）
    long calculatedSize = Math.min(Math.max(maxMemory / 10, 10L * 1024 * 1024), 500L * 1024 * 1024);

    System.out.println(
        "Max heap: "
            + (maxMemory / 1024 / 1024)
            + "MB, Test data size: "
            + (calculatedSize / 1024 / 1024)
            + "MB");
    return calculatedSize;
  }

  /** 大容量テスト用の環境適応型サイズ計算 - long型で完全対応 */
  static long getLargeTestDataSize() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // ヒープサイズの30%をテストデータサイズとして使用（最小50MB、上限なし）
    long calculatedLargeSize = Math.max(maxMemory / 3, 50L * 1024 * 1024);

    System.out.println(
        "Large test - Max heap: "
            + (maxMemory / 1024 / 1024)
            + "MB, Test data size: "
            + (calculatedLargeSize / 1024 / 1024)
            + "MB");
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

    // 設計原理1: メモリに全て持たないこと - データサイズの50%以下のメモリ増加であること
    long maxAcceptableMemory = dataSize / 2; // 50%制限（より厳しい基準）
    System.out.println("=== 環境適応型メモリ効率テスト結果 ===");
    System.out.println("Test data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + "MB");
    System.out.println("Before processing: " + (beforeMemory / 1024 / 1024) + "MB");
    System.out.println("After processing: " + (afterMemory / 1024 / 1024) + "MB");
    System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println(
        "Max acceptable (50% of data): " + (maxAcceptableMemory / 1024 / 1024) + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: メモリに全てを持ってしまった可能性があります。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB (50% of "
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

    // 単一コマンドの場合、メモリ増加は最小限であるべき（データサイズの20%以下）
    long maxAcceptableMemory = dataSize / 5; // 20%制限
    System.out.println("=== 単一コマンド最適パステスト結果 ===");
    System.out.println("Test data size: " + (dataSize / 1024 / 1024) + "MB");
    System.out.println("Single command memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println(
        "Max acceptable (20% of data): " + (maxAcceptableMemory / 1024 / 1024) + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: 単一コマンドのメモリ効率が悪すぎます。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB (20% of "
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

    // 設計原理2: 逐次処理の並列化でスタックしないこと - データサイズの30%以下のメモリ使用量
    long maxAcceptableMemory = dataSize * 3 / 10; // 30%制限
    System.out.println(
        "Max acceptable (30% of data): " + (maxAcceptableMemory / 1024 / 1024) + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "設計原理違反: 並列処理でメモリがスタックした可能性があります。Memory usage: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB (30% of "
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
