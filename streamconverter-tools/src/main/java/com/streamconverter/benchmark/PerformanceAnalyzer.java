package com.streamconverter.benchmark;

import com.streamConverter.CommandResult;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StreamConverterのパフォーマンス分析とレポート生成
 *
 * <p>このクラスは、CommandResultから詳細なパフォーマンス分析を行い、 実行時間、メモリ使用量、スループットなどの統計情報を提供します。
 *
 * <p>主な機能：
 *
 * <ul>
 *   <li>実行時間の統計分析（平均、中央値、分散など）
 *   <li>メモリ使用量の追跡と分析
 *   <li>スループット計算とボトルネック識別
 *   <li>コマンド別パフォーマンス比較
 *   <li>詳細なレポート生成（テキスト、CSV形式）
 * </ul>
 */
public class PerformanceAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(PerformanceAnalyzer.class);

  private final List<PerformanceRecord> records = new ArrayList<>();

  /** Creates a new analyzer. */
  public PerformanceAnalyzer() {}

  /**
   * CommandResult配列からパフォーマンス記録を追加
   *
   * @param testName テスト名
   * @param results コマンド実行結果のリスト
   * @param totalDataSize 処理したデータの総サイズ
   */
  public void addRecord(String testName, List<CommandResult> results, long totalDataSize) {
    if (results == null || results.isEmpty()) {
      logger.warn("Empty results provided for test: {}", testName);
      return;
    }

    PerformanceRecord record = new PerformanceRecord(testName, results, totalDataSize);
    records.add(record);

    if (logger.isDebugEnabled()) {
      logger.debug("Added performance record: {} with {} commands", testName, results.size());
    }
  }

  /** 全記録をクリア */
  public void clearRecords() {
    records.clear();
  }

  /**
   * パフォーマンス統計を取得
   *
   * @return パフォーマンス統計情報
   */
  public PerformanceStatistics getStatistics() {
    if (records.isEmpty()) {
      logger.warn("No performance records available for statistics");
      return new PerformanceStatistics();
    }

    return new PerformanceStatistics(records);
  }

  /**
   * 詳細なパフォーマンスレポートを生成
   *
   * @return パフォーマンスレポートの文字列
   */
  public String generateDetailedReport() {
    StringWriter writer = new StringWriter();
    try (PrintWriter pw = new PrintWriter(writer)) {
      generateDetailedReport(pw);
    }
    return writer.toString();
  }

  /**
   * 詳細なパフォーマンスレポートを指定のPrintWriterに出力
   *
   * @param writer 出力先のPrintWriter
   */
  public void generateDetailedReport(PrintWriter writer) {
    writer.println("StreamConverter Performance Analysis Report");
    writer.println("==========================================");
    writer.println(
        "Generated: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    writer.println();

    if (records.isEmpty()) {
      writer.println("No performance data available.");
      return;
    }

    PerformanceStatistics stats = getStatistics();

    // サマリー情報
    writer.println("EXECUTIVE SUMMARY");
    writer.println("-----------------");
    writer.printf("Total test runs: %d%n", records.size());
    writer.printf("Average execution time: %.2f ms%n", stats.averageExecutionTime);
    writer.printf("Average throughput: %.2f MB/s%n", stats.averageThroughput);
    writer.printf("Peak memory usage: %.2f MB%n", stats.peakMemoryUsage / 1024.0 / 1024.0);
    writer.printf("Total data processed: %.2f MB%n", stats.totalDataProcessed / 1024.0 / 1024.0);
    writer.println();

    // 個別テスト結果
    writer.println("DETAILED TEST RESULTS");
    writer.println("---------------------");

    for (PerformanceRecord record : records) {
      writer.printf("Test: %s%n", record.testName);
      writer.printf("  Data size: %.2f MB%n", record.totalDataSize / 1024.0 / 1024.0);
      writer.printf("  Total execution time: %d ms%n", record.totalExecutionTime);
      writer.printf("  Throughput: %.2f MB/s%n", record.throughput);
      writer.printf("  Commands: %d%n", record.results.size());
      writer.printf("  Peak memory: %.2f MB%n", record.peakMemoryUsage / 1024.0 / 1024.0);

      // コマンド別詳細
      writer.println("  Command breakdown:");
      for (int i = 0; i < record.results.size(); i++) {
        CommandResult result = record.results.get(i);
        writer.printf(
            "    [%d] %s: %d ms (%.2f MB/s, %.2f MB memory)%n",
            i + 1,
            result.getCommandName(),
            result.getExecutionTimeMillis(),
            calculateThroughput(result, record.totalDataSize),
            result.getInputBytes() / 1024.0 / 1024.0);
      }
      writer.println();
    }

    // 統計分析
    writer.println("STATISTICAL ANALYSIS");
    writer.println("--------------------");
    writer.printf("Execution time statistics:%n");
    writer.printf("  Min: %.2f ms%n", stats.minExecutionTime);
    writer.printf("  Max: %.2f ms%n", stats.maxExecutionTime);
    writer.printf("  Median: %.2f ms%n", stats.medianExecutionTime);
    writer.printf("  Standard deviation: %.2f ms%n", stats.executionTimeStdDev);
    writer.println();

    writer.printf("Throughput statistics:%n");
    writer.printf("  Min: %.2f MB/s%n", stats.minThroughput);
    writer.printf("  Max: %.2f MB/s%n", stats.maxThroughput);
    writer.printf("  Median: %.2f MB/s%n", stats.medianThroughput);
    writer.printf("  Standard deviation: %.2f MB/s%n", stats.throughputStdDev);
    writer.println();

    // パフォーマンス推奨事項
    generateRecommendations(writer, stats);
  }

  /**
   * CSV形式のレポートを生成
   *
   * @return CSV形式のパフォーマンスレポート
   */
  public String generateCSVReport() {
    StringWriter writer = new StringWriter();
    try (PrintWriter pw = new PrintWriter(writer)) {
      generateCSVReport(pw);
    }
    return writer.toString();
  }

  /**
   * CSV形式のレポートを指定のPrintWriterに出力
   *
   * @param writer 出力先のPrintWriter
   */
  public void generateCSVReport(PrintWriter writer) {
    // ヘッダー
    writer.println(
        "Test Name,Data Size (MB),Total Time (ms),Throughput (MB/s),Commands,Peak Memory (MB),Success Rate");

    // データ行
    for (PerformanceRecord record : records) {
      writer.printf(
          "%s,%.2f,%d,%.2f,%d,%.2f,%.2f%n",
          record.testName,
          record.totalDataSize / 1024.0 / 1024.0,
          record.totalExecutionTime,
          record.throughput,
          record.results.size(),
          record.peakMemoryUsage / 1024.0 / 1024.0,
          record.successRate * 100.0);
    }
  }

  /** パフォーマンス推奨事項を生成 */
  private void generateRecommendations(PrintWriter writer, PerformanceStatistics stats) {
    writer.println("PERFORMANCE RECOMMENDATIONS");
    writer.println("---------------------------");

    List<String> recommendations = new ArrayList<>();

    // スループットに基づく推奨事項
    if (stats.averageThroughput < 10.0) {
      recommendations.add(
          "Low throughput detected (< 10 MB/s). Consider optimizing I/O operations or reducing command overhead.");
    }

    // メモリ使用量に基づく推奨事項
    double memoryToDataRatio = stats.peakMemoryUsage / (double) stats.totalDataProcessed;
    if (memoryToDataRatio > 2.0) {
      recommendations.add(
          "High memory usage ratio detected (> 2x data size). Consider implementing streaming optimization.");
    }

    // 実行時間のばらつきに基づく推奨事項
    double cvExecutionTime = stats.executionTimeStdDev / stats.averageExecutionTime;
    if (cvExecutionTime > 0.3) {
      recommendations.add(
          "High execution time variability detected (CV > 30%). Consider investigating performance consistency.");
    }

    // コマンド数に基づく推奨事項
    double avgCommandsPerTest =
        records.stream().mapToInt(r -> r.results.size()).average().orElse(0.0);
    if (avgCommandsPerTest > 5) {
      recommendations.add(
          "Complex pipelines detected (> 5 commands). Consider pipeline optimization or parallel processing.");
    }

    if (recommendations.isEmpty()) {
      writer.println(
          "No specific performance issues detected. Current performance is within acceptable ranges.");
    } else {
      for (int i = 0; i < recommendations.size(); i++) {
        writer.printf("%d. %s%n", i + 1, recommendations.get(i));
      }
    }
    writer.println();
  }

  /** スループットを計算 */
  private double calculateThroughput(CommandResult result, long totalDataSize) {
    if (result.getExecutionTimeMillis() <= 0) {
      return 0.0;
    }
    return (totalDataSize / 1024.0 / 1024.0) / (result.getExecutionTimeMillis() / 1000.0);
  }

  /** パフォーマンス記録クラス */
  private static class PerformanceRecord {
    final String testName;
    final List<CommandResult> results;
    final long totalDataSize;
    final long totalExecutionTime;
    final double throughput;
    final long peakMemoryUsage;
    final double successRate;

    PerformanceRecord(String testName, List<CommandResult> results, long totalDataSize) {
      this.testName = testName;
      this.results = new ArrayList<>(results);
      this.totalDataSize = totalDataSize;
      Instant.now();

      // 統計計算
      this.totalExecutionTime =
          results.stream().mapToLong(CommandResult::getExecutionTimeMillis).sum();

      this.throughput =
          totalExecutionTime > 0
              ? (totalDataSize / 1024.0 / 1024.0) / (totalExecutionTime / 1000.0)
              : 0.0;

      this.peakMemoryUsage =
          results.stream().mapToLong(r -> r.getInputBytes() + r.getOutputBytes()).max().orElse(0L);

      long successCount = results.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
      this.successRate = results.size() > 0 ? (double) successCount / results.size() : 0.0;
    }
  }

  /** パフォーマンス統計クラス */
  public static class PerformanceStatistics {
    /** 平均実行時間（ミリ秒） */
    public final double averageExecutionTime;

    /** 最小実行時間（ミリ秒） */
    public final double minExecutionTime;

    /** 最大実行時間（ミリ秒） */
    public final double maxExecutionTime;

    /** 中央値実行時間（ミリ秒） */
    public final double medianExecutionTime;

    /** 実行時間の標準偏差 */
    public final double executionTimeStdDev;

    /** 平均スループット（MB/s） */
    public final double averageThroughput;

    /** 最小スループット（MB/s） */
    public final double minThroughput;

    /** 最大スループット（MB/s） */
    public final double maxThroughput;

    /** 中央値スループット（MB/s） */
    public final double medianThroughput;

    /** スループットの標準偏差 */
    public final double throughputStdDev;

    /** 処理した総データサイズ（バイト） */
    public final long totalDataProcessed;

    /** ピークメモリ使用量（バイト） */
    public final long peakMemoryUsage;

    /** 全体の成功率 */
    public final double overallSuccessRate;

    /** 総テスト数 */
    public final int totalTests;

    /** 総コマンド数 */
    public final int totalCommands;

    PerformanceStatistics() {
      // 空の統計（データなし）
      this.averageExecutionTime = 0.0;
      this.minExecutionTime = 0.0;
      this.maxExecutionTime = 0.0;
      this.medianExecutionTime = 0.0;
      this.executionTimeStdDev = 0.0;

      this.averageThroughput = 0.0;
      this.minThroughput = 0.0;
      this.maxThroughput = 0.0;
      this.medianThroughput = 0.0;
      this.throughputStdDev = 0.0;

      this.totalDataProcessed = 0L;
      this.peakMemoryUsage = 0L;
      this.overallSuccessRate = 0.0;

      this.totalTests = 0;
      this.totalCommands = 0;
    }

    PerformanceStatistics(List<PerformanceRecord> records) {
      this.totalTests = records.size();
      this.totalCommands = records.stream().mapToInt(r -> r.results.size()).sum();

      // 実行時間統計
      List<Double> executionTimes =
          records.stream()
              .map(r -> (double) r.totalExecutionTime)
              .sorted()
              .collect(Collectors.toList());

      this.averageExecutionTime =
          executionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      this.minExecutionTime = executionTimes.isEmpty() ? 0.0 : executionTimes.get(0);
      this.maxExecutionTime =
          executionTimes.isEmpty() ? 0.0 : executionTimes.get(executionTimes.size() - 1);
      this.medianExecutionTime = calculateMedian(executionTimes);
      this.executionTimeStdDev = calculateStandardDeviation(executionTimes, averageExecutionTime);

      // スループット統計
      List<Double> throughputs =
          records.stream().map(r -> r.throughput).sorted().collect(Collectors.toList());

      this.averageThroughput =
          throughputs.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

      this.minThroughput = throughputs.isEmpty() ? 0.0 : throughputs.get(0);
      this.maxThroughput = throughputs.isEmpty() ? 0.0 : throughputs.get(throughputs.size() - 1);
      this.medianThroughput = calculateMedian(throughputs);
      this.throughputStdDev = calculateStandardDeviation(throughputs, averageThroughput);

      // その他の統計
      this.totalDataProcessed = records.stream().mapToLong(r -> r.totalDataSize).sum();

      this.peakMemoryUsage = records.stream().mapToLong(r -> r.peakMemoryUsage).max().orElse(0L);

      this.overallSuccessRate =
          records.stream().mapToDouble(r -> r.successRate).average().orElse(0.0);
    }

    private double calculateMedian(List<Double> sortedValues) {
      if (sortedValues.isEmpty()) {
        return 0.0;
      }

      int size = sortedValues.size();
      if (size % 2 == 0) {
        return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
      } else {
        return sortedValues.get(size / 2);
      }
    }

    private double calculateStandardDeviation(List<Double> values, double mean) {
      if (values.size() <= 1) {
        return 0.0;
      }

      double sumSquaredDifferences =
          values.stream().mapToDouble(value -> Math.pow(value - mean, 2)).sum();

      return Math.sqrt(sumSquaredDifferences / (values.size() - 1));
    }
  }
}
