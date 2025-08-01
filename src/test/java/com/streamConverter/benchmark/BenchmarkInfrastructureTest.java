package com.streamConverter.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.*;
import java.util.List;
import org.junit.jupiter.api.*;

/**
 * ベンチマーク基盤のテスト
 *
 * <p>このクラスは、PerformanceAnalyzerとLargeDataBenchmarkの基本的な機能を検証し、 ベンチマーク測定インフラが正しく動作することを確認します。
 */
@DisplayName("ベンチマーク基盤テスト")
class BenchmarkInfrastructureTest {

  @Test
  @DisplayName("PerformanceAnalyzer基本機能テスト")
  void testPerformanceAnalyzerBasicFunctionality() throws IOException {
    PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    // テストデータ作成
    IStreamCommand command = new SampleStreamCommand("test-command");
    StreamConverter converter = new StreamConverter(new IStreamCommand[] {command});

    // 小さなデータでテスト実行
    String testData = "Hello, World!";
    InputStream input = new ByteArrayInputStream(testData.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(input, output);

    // PerformanceAnalyzerに記録追加
    analyzer.addRecord("basic-test", results, testData.length());

    // 統計取得
    PerformanceAnalyzer.PerformanceStatistics stats = analyzer.getStatistics();
    assertNotNull(stats);
    assertEquals(1, stats.totalTests);
    assertEquals(1, stats.totalCommands);
    assertEquals(testData.length(), stats.totalDataProcessed);
    assertTrue(stats.averageExecutionTime >= 0);
    assertTrue(stats.averageThroughput >= 0);

    // レポート生成
    String detailedReport = analyzer.generateDetailedReport();
    assertNotNull(detailedReport);
    assertTrue(detailedReport.contains("StreamConverter Performance Analysis Report"));
    assertTrue(detailedReport.contains("basic-test"));

    String csvReport = analyzer.generateCSVReport();
    assertNotNull(csvReport);
    assertTrue(csvReport.contains("Test Name"));
    assertTrue(csvReport.contains("basic-test"));
  }

  @Test
  @DisplayName("複数テスト記録と統計分析")
  void testMultipleRecordsAndStatistics() throws IOException {
    PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    // 複数のテストケースを実行
    for (int i = 1; i <= 3; i++) {
      IStreamCommand command = new SampleStreamCommand("test-" + i);
      StreamConverter converter = new StreamConverter(new IStreamCommand[] {command});

      String testData = "Test data " + i + " with more content";
      InputStream input = new ByteArrayInputStream(testData.getBytes());
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      List<CommandResult> results = converter.run(input, output);
      analyzer.addRecord("test-case-" + i, results, testData.length());
    }

    // 統計確認
    PerformanceAnalyzer.PerformanceStatistics stats = analyzer.getStatistics();
    assertEquals(3, stats.totalTests);
    assertEquals(3, stats.totalCommands);
    assertTrue(stats.totalDataProcessed > 0);
    assertTrue(stats.averageExecutionTime >= 0);
    assertTrue(stats.minExecutionTime >= 0);
    assertTrue(stats.maxExecutionTime >= stats.minExecutionTime);

    // レポート内容確認
    String report = analyzer.generateDetailedReport();
    assertTrue(report.contains("test-case-1"));
    assertTrue(report.contains("test-case-2"));
    assertTrue(report.contains("test-case-3"));
    assertTrue(report.contains("STATISTICAL ANALYSIS"));
    assertTrue(report.contains("PERFORMANCE RECOMMENDATIONS"));
  }

  @Test
  @DisplayName("メモリ効率的な小規模ベンチマーク")
  void testSmallScaleBenchmark() throws IOException {
    // 小規模データでベンチマーク機能をテスト
    int smallDataSize = 1024; // 1KB
    IStreamCommand[] pipeline = {
      new SampleStreamCommand("stage1"), new SampleStreamCommand("stage2")
    };

    // LargeDataBenchmarkの内部クラスを使用してテスト
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    StreamConverter converter = new StreamConverter(pipeline);

    try (InputStream input = createTestInputStream(smallDataSize);
        OutputStream output = createNullOutputStream()) {

      List<CommandResult> results = converter.run(input, output);

      assertNotNull(results);
      assertEquals(2, results.size());

      // 基本的なパフォーマンス検証
      for (CommandResult result : results) {
        assertTrue(result.isSuccess());
        assertTrue(result.getExecutionTimeMillis() >= 0);
      }
    }

    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    // メモリ使用量が妥当な範囲内であることを確認
    assertTrue(
        memoryIncrease < smallDataSize * 10, "Memory usage too high: " + memoryIncrease + " bytes");
  }

  @Test
  @DisplayName("ベンチマーク結果の一貫性確認")
  @org.junit.jupiter.api.Disabled(
      "Environment-dependent test - may fail in CI/high-load environments")
  void testBenchmarkConsistency() throws IOException {
    PerformanceAnalyzer analyzer = new PerformanceAnalyzer();
    String testData = "Consistent test data for benchmarking";

    // 同じテストを5回実行
    for (int i = 0; i < 5; i++) {
      IStreamCommand command = new SampleStreamCommand("consistency-test");
      StreamConverter converter = new StreamConverter(new IStreamCommand[] {command});

      InputStream input = new ByteArrayInputStream(testData.getBytes());
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      List<CommandResult> results = converter.run(input, output);
      analyzer.addRecord("consistency-" + i, results, testData.length());
    }

    PerformanceAnalyzer.PerformanceStatistics stats = analyzer.getStatistics();

    // 一貫性検証
    assertEquals(5, stats.totalTests);
    assertTrue(stats.executionTimeStdDev >= 0);
    assertTrue(stats.throughputStdDev >= 0);

    // 変動係数が合理的な範囲内であることを確認
    // 高負荷環境やCI環境では実行時間の変動が大きくなる可能性があるため、
    // より柔軟な閾値を設定（環境依存性を考慮）
    double cvExecutionTime = stats.executionTimeStdDev / stats.averageExecutionTime;
    assertTrue(
        cvExecutionTime < 5.0,
        String.format(
            "Execution time too variable: CV = %.3f (threshold: 5.0). "
                + "This may indicate high system load or CI environment constraints.",
            cvExecutionTime));
  }

  // テスト用ヘルパーメソッド
  private InputStream createTestInputStream(int size) {
    return new InputStream() {
      private int bytesRead = 0;
      private final byte[] pattern = "TestData123".getBytes();
      private int patternIndex = 0;

      @Override
      public int read() {
        if (bytesRead >= size) return -1;
        byte b = pattern[patternIndex];
        patternIndex = (patternIndex + 1) % pattern.length;
        bytesRead++;
        return b & 0xFF;
      }
    };
  }

  private OutputStream createNullOutputStream() {
    return new OutputStream() {
      @Override
      public void write(int b) {
        // データを破棄
      }
    };
  }
}
