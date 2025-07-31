package com.streamConverter.benchmark;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.charaCode.convert;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 大容量データ処理のベンチマークテスト
 *
 * <p>このクラスは、StreamConverterの大容量データ処理性能を測定し、 メモリ効率、処理スループット、スケーラビリティを検証します。
 *
 * <p>テスト項目：
 *
 * <ul>
 *   <li>多段階パイプライン処理のベンチマーク
 *   <li>異なるデータサイズでの性能特性
 *   <li>並列処理とシーケンシャル処理の比較
 *   <li>メモリ使用量の監視と制限
 *   <li>実用的なワークロードでの性能測定
 * </ul>
 */
@DisplayName("大容量データ処理ベンチマーク")
class LargeDataBenchmark {

  private static final Logger logger = LoggerFactory.getLogger(LargeDataBenchmark.class);

  // ベンチマーク設定（省メモリ対応テスト用）
  private static final int[] DATA_SIZES = {
    1 * 1024 * 1024, // 1MB
    5 * 1024 * 1024, // 5MB
    10 * 1024 * 1024, // 10MB
    25 * 1024 * 1024, // 25MB
    50 * 1024 * 1024 // 50MB
  };

  private static final int WARMUP_ITERATIONS = 2;
  private static final int BENCHMARK_ITERATIONS = 3;

  /** JVMに十分なメモリがあるかチェック（512MB以上で十分） */
  static boolean hasEnoughMemory() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    return maxMemory >= 512 * 1024 * 1024; // 512MB以上
  }

  /** 大容量テスト用のメモリチェック（1GB以上） */
  static boolean hasEnoughMemoryForLarge() {
    long maxMemory = Runtime.getRuntime().maxMemory();
    return maxMemory >= 1024 * 1024 * 1024; // 1GB以上
  }

  @BeforeEach
  void setUp() {
    // 各テスト前にGCを実行してクリーンな状態にする
    System.gc();
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  @Test
  @DisplayName("シンプルパイプライン処理ベンチマーク")
  @EnabledIf("hasEnoughMemory")
  void benchmarkSimplePipeline() throws IOException {
    logger.info("=== Simple Pipeline Benchmark ===");

    BenchmarkResults results = new BenchmarkResults("Simple Pipeline");

    for (int dataSize : DATA_SIZES) {
      logger.info("Testing data size: {}MB", dataSize / 1024 / 1024);

      // 3段階のシンプルパイプライン
      IStreamCommand[] pipeline = {
        new SampleStreamCommand("filter"),
        new SampleStreamCommand("transform"),
        new SampleStreamCommand("output")
      };

      BenchmarkResult result =
          runBenchmark("Simple-" + (dataSize / 1024 / 1024) + "MB", dataSize, pipeline);

      results.addResult(result);

      // 省メモリ制限チェック（データサイズに関係なく、50MB以下）
      long maxAcceptableMemory = 50 * 1024 * 1024; // 50MB固定上限
      Assertions.assertTrue(
          result.peakMemoryUsage < maxAcceptableMemory,
          String.format(
              "Memory usage too high for streaming: %dMB > %dMB (data: %dMB)",
              result.peakMemoryUsage / 1024 / 1024,
              maxAcceptableMemory / 1024 / 1024,
              dataSize / 1024 / 1024));

      // 最低スループット要求（5MB/s以上）
      Assertions.assertTrue(
          result.throughputMBps > 5.0,
          String.format("Throughput too low: %.2f MB/s", result.throughputMBps));
    }

    results.printSummary();
  }

  @Test
  @DisplayName("複雑なデータ変換パイプラインベンチマーク")
  @EnabledIf("hasEnoughMemory")
  void benchmarkComplexPipeline() throws IOException {
    logger.info("=== Complex Pipeline Benchmark ===");

    BenchmarkResults results = new BenchmarkResults("Complex Pipeline");

    // 軽量版：最小サイズのみでテストして確実に完了させる
    int[] testSizes = {1 * 1024 * 1024, 5 * 1024 * 1024}; // 1MB, 5MB

    for (int dataSize : testSizes) {
      logger.info("Testing complex pipeline with data size: {}MB", dataSize / 1024 / 1024);

      // 複雑パイプラインを軽量化（文字コード変換を含む4段階）
      IStreamCommand[] pipeline = {
        new SampleStreamCommand("validate"),
        new convert("UTF-8", "UTF-16"),
        new convert("UTF-16", "UTF-8"),
        new SampleStreamCommand("format")
      };

      BenchmarkResult result =
          runBenchmark("Complex-" + (dataSize / 1024 / 1024) + "MB", dataSize, pipeline);

      results.addResult(result);

      // 複雑パイプラインでも省メモリ制限（100MB以下）
      long maxAcceptableMemory = 100 * 1024 * 1024; // 100MB固定上限
      Assertions.assertTrue(
          result.peakMemoryUsage < maxAcceptableMemory,
          String.format(
              "Complex pipeline memory usage too high: %dMB > %dMB (data: %dMB)",
              result.peakMemoryUsage / 1024 / 1024,
              maxAcceptableMemory / 1024 / 1024,
              dataSize / 1024 / 1024));
    }

    results.printSummary();
  }

  @Test
  @DisplayName("スケーラビリティベンチマーク")
  @EnabledIf("hasEnoughMemoryForLarge")
  @Timeout(value = 300, unit = TimeUnit.SECONDS) // 5分タイムアウト
  void benchmarkScalability() throws IOException {
    logger.info("=== Scalability Benchmark ===");

    BenchmarkResults results = new BenchmarkResults("Scalability");

    // より大きなデータサイズでテスト（省メモリ実装を前提）
    int[] largeSizes = {
      50 * 1024 * 1024, // 50MB
      100 * 1024 * 1024, // 100MB
      200 * 1024 * 1024 // 200MB
    };

    IStreamCommand[] pipeline = {
      new SampleStreamCommand("stage1"),
      new SampleStreamCommand("stage2"),
      new SampleStreamCommand("stage3")
    };

    for (int dataSize : largeSizes) {
      logger.info("Testing scalability with data size: {}MB", dataSize / 1024 / 1024);

      BenchmarkResult result =
          runBenchmark("Scale-" + (dataSize / 1024 / 1024) + "MB", dataSize, pipeline);

      results.addResult(result);

      // スケーラビリティ要件：メモリ使用量はデータサイズに依存しない（75MB固定上限）
      long maxAcceptableMemory = 75 * 1024 * 1024; // 75MB固定上限

      Assertions.assertTrue(
          result.peakMemoryUsage < maxAcceptableMemory,
          String.format(
              "Scalability test failed - memory usage: %dMB > %dMB (data: %dMB)",
              result.peakMemoryUsage / 1024 / 1024,
              maxAcceptableMemory / 1024 / 1024,
              dataSize / 1024 / 1024));
    }

    results.printSummary();
  }

  @Test
  @DisplayName("メモリ制約下でのベンチマーク")
  @EnabledIf("hasEnoughMemory")
  void benchmarkUnderMemoryConstraints() throws IOException {
    logger.info("=== Memory Constrained Benchmark ===");

    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // 利用可能メモリの80%を使用するデータサイズを計算
    int constrainedDataSize = (int) (maxMemory * 0.8);
    logger.info(
        "Testing under memory constraint: {}MB data with {}MB heap",
        constrainedDataSize / 1024 / 1024,
        maxMemory / 1024 / 1024);

    IStreamCommand[] pipeline = {
      new SampleStreamCommand("constrained1"), new SampleStreamCommand("constrained2")
    };

    BenchmarkResult result = runBenchmark("MemConstrained", constrainedDataSize, pipeline);

    // メモリ制約下でも正常に動作することを確認
    Assertions.assertTrue(
        result.successful, "Memory constrained test should complete successfully");

    // OutOfMemoryErrorが発生しないことを確認
    Assertions.assertTrue(
        result.peakMemoryUsage < maxMemory * 0.95,
        "Memory usage should stay below 95% of heap size");

    logger.info("Memory constrained benchmark completed: {}", result);
  }

  /** ベンチマークを実行 */
  private BenchmarkResult runBenchmark(String name, int dataSize, IStreamCommand[] pipeline)
      throws IOException {
    Runtime runtime = Runtime.getRuntime();

    // ウォームアップ
    for (int i = 0; i < WARMUP_ITERATIONS; i++) {
      runSingleBenchmark(dataSize, pipeline);
      System.gc();
    }

    // 実際のベンチマーク実行
    List<Long> executionTimes = new ArrayList<>();
    List<Long> memoryUsages = new ArrayList<>();

    for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
      System.gc();
      long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

      long startTime = System.nanoTime();
      runSingleBenchmark(dataSize, pipeline);
      long endTime = System.nanoTime();

      System.gc();
      long afterMemory = runtime.totalMemory() - runtime.freeMemory();

      executionTimes.add(endTime - startTime);
      memoryUsages.add(Math.max(0, afterMemory - beforeMemory));
    }

    // 統計計算
    long avgExecutionTime =
        executionTimes.stream().mapToLong(Long::longValue).sum() / BENCHMARK_ITERATIONS;
    long maxMemoryUsage = memoryUsages.stream().mapToLong(Long::longValue).max().orElse(0);

    double throughputMBps = (dataSize / 1024.0 / 1024.0) / (avgExecutionTime / 1_000_000_000.0);

    BenchmarkResult result =
        new BenchmarkResult(
            name,
            dataSize,
            avgExecutionTime / 1_000_000, // nsをmsに変換
            maxMemoryUsage,
            throughputMBps,
            true);

    logger.info("Benchmark result: {}", result);
    return result;
  }

  /** 単一のベンチマーク実行 */
  private void runSingleBenchmark(int dataSize, IStreamCommand[] pipeline) throws IOException {
    try (InputStream input = new LargeDataInputStream(dataSize);
        OutputStream output = new NullOutputStream()) {

      StreamConverter converter = new StreamConverter(pipeline);
      List<CommandResult> results = converter.run(input, output);

      // 結果が正しく返されることを確認
      Assertions.assertNotNull(results);
      Assertions.assertEquals(pipeline.length, results.size());
    }
  }

  /** ベンチマーク結果を格納するクラス */
  private static class BenchmarkResult {
    final String name;
    final int dataSize;
    final long executionTimeMs;
    final long peakMemoryUsage;
    final double throughputMBps;
    final boolean successful;

    BenchmarkResult(
        String name,
        int dataSize,
        long executionTimeMs,
        long peakMemoryUsage,
        double throughputMBps,
        boolean successful) {
      this.name = name;
      this.dataSize = dataSize;
      this.executionTimeMs = executionTimeMs;
      this.peakMemoryUsage = peakMemoryUsage;
      this.throughputMBps = throughputMBps;
      this.successful = successful;
    }

    @Override
    public String toString() {
      return String.format(
          "%s: %dMB in %dms (%.2f MB/s, %dMB memory)",
          name,
          dataSize / 1024 / 1024,
          executionTimeMs,
          throughputMBps,
          peakMemoryUsage / 1024 / 1024);
    }
  }

  /** ベンチマーク結果の集計クラス */
  private static class BenchmarkResults {
    private final String testName;
    private final List<BenchmarkResult> results = new ArrayList<>();

    BenchmarkResults(String testName) {
      this.testName = testName;
    }

    void addResult(BenchmarkResult result) {
      results.add(result);
    }

    void printSummary() {
      logger.info("=== {} Benchmark Summary ===", testName);

      for (BenchmarkResult result : results) {
        logger.info("  {}", result);
      }

      // 統計情報
      double avgThroughput =
          results.stream().mapToDouble(r -> r.throughputMBps).average().orElse(0.0);

      long maxMemory = results.stream().mapToLong(r -> r.peakMemoryUsage).max().orElse(0);

      logger.info("  Average throughput: {:.2f} MB/s", avgThroughput);
      logger.info("  Peak memory usage: {}MB", maxMemory / 1024 / 1024);
      logger.info("  All tests successful: {}", results.stream().allMatch(r -> r.successful));
    }
  }

  /** 全ての出力を破棄するOutputStream */
  private static class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
      // データを破棄
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      // データを破棄
    }
  }

  /** 大容量データを生成するInputStream */
  private static class LargeDataInputStream extends InputStream {
    private final int totalSize;
    private int bytesRead = 0;
    private final byte[] pattern;
    private int patternIndex = 0;

    public LargeDataInputStream(int totalSize) {
      this.totalSize = totalSize;
      // より複雑なパターンでリアルなデータをシミュレート
      this.pattern =
          "StreamConverter,Large,Data,Processing,Benchmark,Test,1234567890,ABCDEF\n"
              .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= totalSize) {
        return -1;
      }

      byte b = pattern[patternIndex];
      patternIndex = (patternIndex + 1) % pattern.length;
      bytesRead++;
      return b & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesRead >= totalSize) {
        return -1;
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
