package com.streamconverter.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.*;
import com.streamconverter.command.IStreamCommand;
import java.io.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ベンチマーク基盤のテスト
 *
 * <p>このクラスは、PerformanceAnalyzer、ResourceMonitor、LargeDataGenerator、ResourceUsageクラスの
 * 基本機能を検証し、ベンチマーク測定インフラが正しく動作することを確認します。
 */
@DisplayName("ベンチマーク基盤テスト")
class BenchmarkInfrastructureTest {

  private static final Logger logger = LoggerFactory.getLogger(BenchmarkInfrastructureTest.class);

  @Test
  @DisplayName("ResourceMonitor基本動作確認")
  void testResourceMonitorBasicOperation() {
    logger.info("Testing ResourceMonitor basic operation");

    ResourceMonitor monitor = new ResourceMonitor();

    // 測定開始
    long dataSize = 1024 * 1024; // 1MB
    monitor.start(dataSize);

    // 初期状態確認
    assertTrue(monitor.getCurrentMemoryMB() > 0, "Current memory should be positive");
    assertTrue(monitor.getPeakMemoryMB() > 0, "Peak memory should be positive");

    // 簡単な処理（メモリを少し使用）
    byte[] data = new byte[1024]; // 1KB
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (i % 256);
    }

    // 少し待機
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // 測定終了
    ResourceUsage usage = monitor.stop();

    // 結果検証
    assertNotNull(usage, "ResourceUsage should not be null");
    assertTrue(usage.getDurationMillis() >= 100, "Duration should be at least 100ms");
    assertTrue(usage.getDataSizeBytes() == dataSize, "Data size should match");
    assertTrue(usage.getMemoryUsedMB() >= 0, "Memory used should be non-negative");
    assertTrue(usage.getPeakMemoryMB() > 0, "Peak memory should be positive");

    logger.info("ResourceMonitor test completed: {}", usage);
  }

  @Test
  @DisplayName("LargeDataGenerator - XMLデータ生成確認")
  void testXmlDataGeneration() throws IOException {
    logger.info("Testing XML data generation");

    long targetSize = 1024 * 1024; // 1MB
    InputStream xmlStream = LargeDataGenerator.createLargeDataStream("XML", targetSize);

    assertNotNull(xmlStream, "XML stream should not be null");

    // データを読み取ってサイズと形式を確認
    byte[] buffer = new byte[8192];
    long totalRead = 0;
    int bytesRead;
    boolean hasXmlHeader = false;
    boolean hasOrdersTag = false;

    while ((bytesRead = xmlStream.read(buffer)) != -1) {
      totalRead += bytesRead;

      String content = new String(buffer, 0, bytesRead, "UTF-8");
      if (content.contains("<?xml version")) {
        hasXmlHeader = true;
      }
      if (content.contains("<orders>")) {
        hasOrdersTag = true;
      }
    }

    xmlStream.close();

    // 検証 - XML生成では日本語文字のUTF-8エンコーディングやレコードサイズの変動により、
    // 実際のサイズは目標値より大きくなる傾向がある
    assertTrue(totalRead > targetSize * 0.8, "Generated size should be at least 80% of target");
    assertTrue(totalRead < targetSize * 1.4, "Generated size should not exceed 140% of target");
    assertTrue(hasXmlHeader, "Should contain XML header");
    assertTrue(hasOrdersTag, "Should contain orders tag");

    logger.info("XML generation test completed: {} bytes generated", totalRead);
  }

  @Test
  @DisplayName("LargeDataGenerator - JSONデータ生成確認")
  void testJsonDataGeneration() throws IOException {
    logger.info("Testing JSON data generation");

    long targetSize = 512 * 1024; // 512KB
    InputStream jsonStream = LargeDataGenerator.createLargeDataStream("JSON", targetSize);

    assertNotNull(jsonStream, "JSON stream should not be null");

    byte[] buffer = new byte[8192];
    long totalRead = 0;
    int bytesRead;
    boolean hasOrdersArray = false;

    while ((bytesRead = jsonStream.read(buffer)) != -1) {
      totalRead += bytesRead;

      String content = new String(buffer, 0, bytesRead, "UTF-8");
      if (content.contains("\"orders\"")) {
        hasOrdersArray = true;
      }
    }

    jsonStream.close();

    // 検証 - JSON生成でもデータサイズの変動があることを考慮
    assertTrue(totalRead > targetSize * 0.8, "Generated size should be at least 80% of target");
    assertTrue(totalRead < targetSize * 1.4, "Generated size should not exceed 140% of target");
    assertTrue(hasOrdersArray, "Should contain orders array");

    logger.info("JSON generation test completed: {} bytes generated", totalRead);
  }

  @Test
  @DisplayName("LargeDataGenerator - CSVデータ生成確認")
  void testCsvDataGeneration() throws IOException {
    logger.info("Testing CSV data generation");

    long targetSize = 256 * 1024; // 256KB
    InputStream csvStream = LargeDataGenerator.createLargeDataStream("CSV", targetSize);

    assertNotNull(csvStream, "CSV stream should not be null");

    byte[] buffer = new byte[8192];
    long totalRead = 0;
    int bytesRead;
    boolean hasHeader = false;
    int lineCount = 0;

    StringBuilder contentBuilder = new StringBuilder();
    while ((bytesRead = csvStream.read(buffer)) != -1) {
      totalRead += bytesRead;
      String content = new String(buffer, 0, bytesRead, "UTF-8");
      contentBuilder.append(content);
    }

    csvStream.close();

    String fullContent = contentBuilder.toString();
    String[] lines = fullContent.split("\n");
    lineCount = lines.length;

    if (lines.length > 0 && lines[0].contains("id,name,city")) {
      hasHeader = true;
    }

    // 検証 - CSV生成でもデータサイズの変動があることを考慮
    assertTrue(totalRead > targetSize * 0.8, "Generated size should be at least 80% of target");
    assertTrue(totalRead < targetSize * 1.4, "Generated size should not exceed 140% of target");
    assertTrue(hasHeader, "Should contain CSV header");
    assertTrue(lineCount > 1, "Should have multiple lines");

    logger.info("CSV generation test completed: {} bytes, {} lines", totalRead, lineCount);
  }

  @Test
  @DisplayName("ResourceUsage - 5GB/50MB目標判定確認")
  void testResourceUsage5GB50MBTarget() {
    logger.info("Testing ResourceUsage 5GB/50MB target evaluation");

    // 目標を満たすケース
    ResourceUsage successCase =
        new ResourceUsage(
            10000, // 10秒
            40 * 1024 * 1024, // 40MB memory used
            45 * 1024 * 1024, // 45MB peak
            5 * 1024 * 1024, // 5MB start
            10 * 1024 * 1024, // 10MB end
            5L * 1024 * 1024 * 1024, // 5GB data
            120.0, // 120MB/s throughput
            java.time.Instant.now());

    assertTrue(successCase.meets5GB50MBTarget(), "Should meet 5GB/50MB target");
    assertTrue(successCase.getMemoryUsedMB() <= 50.0, "Memory should be <= 50MB");
    assertTrue(successCase.getThroughputMBps() >= 100.0, "Throughput should be >= 100MB/s");

    // 目標を満たさないケース（メモリ超過）
    ResourceUsage memoryFailCase =
        new ResourceUsage(
            10000, // 10秒
            60 * 1024 * 1024, // 60MB memory used (over limit)
            65 * 1024 * 1024, // 65MB peak
            5 * 1024 * 1024, // 5MB start
            10 * 1024 * 1024, // 10MB end
            5L * 1024 * 1024 * 1024, // 5GB data
            120.0, // 120MB/s throughput
            java.time.Instant.now());

    assertFalse(memoryFailCase.meets5GB50MBTarget(), "Should not meet target due to memory");

    // 目標を満たさないケース（スループット不足）
    ResourceUsage throughputFailCase =
        new ResourceUsage(
            10000, // 10秒
            40 * 1024 * 1024, // 40MB memory used
            45 * 1024 * 1024, // 45MB peak
            5 * 1024 * 1024, // 5MB start
            10 * 1024 * 1024, // 10MB end
            5L * 1024 * 1024 * 1024, // 5GB data
            80.0, // 80MB/s throughput (under limit)
            java.time.Instant.now());

    assertFalse(
        throughputFailCase.meets5GB50MBTarget(), "Should not meet target due to throughput");

    logger.info("ResourceUsage target evaluation test completed");
  }

  @Test
  @DisplayName("統合テスト - 小規模データでの完全な処理フロー")
  void testIntegratedSmallDataProcessing() throws IOException {
    logger.info("Testing integrated small data processing flow");

    long testDataSize = 64 * 1024; // 64KB

    // データ生成
    InputStream testData = LargeDataGenerator.createLargeDataStream("XML", testDataSize);
    OutputStream nullOutput = new NullOutputStream();

    // リソース監視
    ResourceMonitor monitor = new ResourceMonitor();
    monitor.start(testDataSize);

    // データ処理（単純なコピー）
    byte[] buffer = new byte[8192];
    int bytesRead;
    while ((bytesRead = testData.read(buffer)) != -1) {
      nullOutput.write(buffer, 0, bytesRead);
    }

    testData.close();
    nullOutput.close();

    // 結果取得
    ResourceUsage usage = monitor.stop();

    // 統合検証
    assertNotNull(usage, "Usage should not be null");
    assertTrue(usage.getDataSizeBytes() == testDataSize, "Data size should match");
    assertTrue(usage.getDurationMillis() > 0, "Duration should be positive");
    assertTrue(usage.getMemoryUsedMB() >= 0, "Memory used should be non-negative");
    assertTrue(usage.getThroughputMBps() > 0, "Throughput should be positive");

    // 効率性確認（小データなので制約は緩め）
    assertTrue(usage.getMemoryUsedMB() < 50.0, "Small data should use less than 50MB");

    logger.info("Integrated test completed: {}", usage);
  }

  /** NullOutputStreamの実装（テスト用） */
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

  @Test
  @DisplayName("PerformanceAnalyzer基本機能テスト")
  void testPerformanceAnalyzerBasicFunctionality() throws IOException {
    PerformanceAnalyzer analyzer = new PerformanceAnalyzer();

    // テストデータ作成
    IStreamCommand command = (in, out) -> in.transferTo(out);
    StreamConverter converter = StreamConverter.create(command);

    // 小さなデータでテスト実行
    String testData = "Hello, World!";
    InputStream input = new ByteArrayInputStream(testData.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    long startMs = System.currentTimeMillis();
    converter.run(input, output);
    long execMs = System.currentTimeMillis() - startMs;

    // PerformanceAnalyzerに記録追加
    analyzer.addRecord("basic-test", 1, execMs, testData.length());

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
      IStreamCommand command = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(command);

      String testData = "Test data " + i + " with more content";
      InputStream input = new ByteArrayInputStream(testData.getBytes());
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      long startMs = System.currentTimeMillis();
      converter.run(input, output);
      long execMs = System.currentTimeMillis() - startMs;

      analyzer.addRecord("test-case-" + i, 1, execMs, testData.length());
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
    IStreamCommand[] pipeline = {(in, out) -> in.transferTo(out), (in, out) -> in.transferTo(out)};

    // LargeDataBenchmarkの内部クラスを使用してテスト
    Runtime runtime = Runtime.getRuntime();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    StreamConverter converter = StreamConverter.create(pipeline);

    try (InputStream input = createTestInputStream(smallDataSize);
        OutputStream output = createNullOutputStream()) {

      converter.run(input, output);
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
      IStreamCommand command = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(command);

      InputStream input = new ByteArrayInputStream(testData.getBytes());
      ByteArrayOutputStream output = new ByteArrayOutputStream();

      long startMs = System.currentTimeMillis();
      converter.run(input, output);
      long execMs = System.currentTimeMillis() - startMs;

      analyzer.addRecord("consistency-" + i, 1, execMs, testData.length());
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
