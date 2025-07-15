package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;

/** メモリ効率とパフォーマンスのテスト */
class MemoryEfficiencyTest {

  /** JVMに十分なメモリがあるかチェック */
  static boolean hasEnoughMemory() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    return maxMemory > 512 * 1024 * 1024; // 512MB以上
  }

  /** 1GBテスト用のメモリチェック */
  static boolean hasEnoughMemoryFor1GB() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    return maxMemory > 800 * 1024 * 1024; // 800MB以上（1GBヒープの場合）
  }

  @Test
  @DisplayName("Large Stream Processing - Memory Efficiency Test")
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  @EnabledIf("hasEnoughMemory")
  void testLargeStreamMemoryEfficiency() throws IOException {
    // メモリ使用量監視
    Runtime runtime = Runtime.getRuntime();
    long initialMemory = runtime.totalMemory() - runtime.freeMemory();

    // 100MB相当のデータストリーム作成
    int dataSize = 100 * 1024 * 1024; // 100MB
    InputStream largeInput = new LargeDataInputStream(dataSize);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

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
    List<Object> result = converter.run(largeInput, output);

    // 処理後のメモリ確認
    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // 結果検証
    assertNotNull(result);
    assertEquals(3, result.size());
    assertEquals(dataSize, output.size());

    // メモリ使用量検証 - データサイズの2倍以下に抑制
    long maxAcceptableMemory = dataSize * 2;
    System.out.println("Initial memory: " + (initialMemory / 1024 / 1024) + "MB");
    System.out.println("Before processing: " + (beforeMemory / 1024 / 1024) + "MB");
    System.out.println("After processing: " + (afterMemory / 1024 / 1024) + "MB");
    System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println("Max acceptable: " + (maxAcceptableMemory / 1024 / 1024) + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "Memory usage too high: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB");
  }

  @Test
  @DisplayName("Single Command - Zero Memory Overhead Test")
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void testSingleCommandMemoryEfficiency() throws IOException {
    Runtime runtime = Runtime.getRuntime();

    // 50MB相当のデータストリーム
    int dataSize = 50 * 1024 * 1024; // 50MB
    InputStream input = new LargeDataInputStream(dataSize);
    // メモリ効率測定のため、出力は捨てる
    OutputStream output = new NullOutputStream();

    // 単一コマンド（最適パス）
    StreamConverter converter =
        new StreamConverter(new IStreamCommand[] {new SampleStreamCommand("single")});

    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    List<Object> result = converter.run(input, output);

    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // 結果検証
    assertNotNull(result);
    assertEquals(1, result.size());

    // 単一コマンドの場合、メモリ増加は最小限であるべき
    long maxAcceptableMemory = 10 * 1024 * 1024; // 10MB以下
    System.out.println("Single command memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");

    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "Single command memory usage too high: " + (memoryIncrease / 1024 / 1024) + "MB");
  }

  @Test
  @DisplayName("Extreme Large Stream - 1GB Data Processing Test")
  @Timeout(value = 120, unit = TimeUnit.SECONDS)
  @EnabledIf("hasEnoughMemoryFor1GB")
  void test1GBStreamMemoryEfficiency() throws IOException {
    Runtime runtime = Runtime.getRuntime();

    // 1GB相当のデータストリーム
    int dataSize = 1024 * 1024 * 1024; // 1GB
    InputStream largeInput = new LargeDataInputStream(dataSize);
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
    List<Object> result = converter.run(largeInput, output);
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

    System.out.println("Memory after processing: " + (afterMemory / 1024 / 1024) + "MB");
    System.out.println("Memory increase: " + (memoryIncrease / 1024 / 1024) + "MB");
    System.out.println("Processing time: " + processingTimeMs + "ms");
    System.out.println("Throughput: " + String.format("%.2f", throughputMBps) + " MB/s");

    // 結果検証
    assertNotNull(result);
    assertEquals(3, result.size());

    // メモリ使用量検証 - データサイズの20%以下に抑制（1GBデータに対して200MB以下）
    long maxAcceptableMemory = dataSize / 5; // 20%
    assertTrue(
        memoryIncrease < maxAcceptableMemory,
        "1GB processing memory usage too high: "
            + (memoryIncrease / 1024 / 1024)
            + "MB > "
            + (maxAcceptableMemory / 1024 / 1024)
            + "MB");

    // パフォーマンス検証 - 最低1MB/s以上のスループット
    assertTrue(
        throughputMBps > 1.0,
        "Processing throughput too slow: " + String.format("%.2f", throughputMBps) + " MB/s");

    System.out.println("✅ 1GB stream processing completed successfully!");
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

  /** 大容量データを生成するInputStream */
  private static class LargeDataInputStream extends InputStream {
    private final int totalSize;
    private int bytesRead = 0;
    private final byte[] pattern = "0123456789ABCDEF".getBytes();
    private int patternIndex = 0;

    public LargeDataInputStream(int totalSize) {
      this.totalSize = totalSize;
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= totalSize) {
        return -1; // EOF
      }

      byte b = pattern[patternIndex];
      patternIndex = (patternIndex + 1) % pattern.length;
      bytesRead++;
      return b & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesRead >= totalSize) {
        return -1; // EOF
      }

      int remaining = totalSize - bytesRead;
      int toRead = Math.min(len, remaining);

      for (int i = 0; i < toRead; i++) {
        b[off + i] = pattern[patternIndex];
        patternIndex = (patternIndex + 1) % pattern.length;
      }

      bytesRead += toRead;
      return toRead;
    }

    @Override
    public long skip(long n) throws IOException {
      long remaining = totalSize - bytesRead;
      long toSkip = Math.min(n, remaining);
      bytesRead += (int) toSkip;
      patternIndex = (patternIndex + (int) (toSkip % pattern.length)) % pattern.length;
      return toSkip;
    }

    @Override
    public int available() throws IOException {
      return totalSize - bytesRead;
    }
  }
}
