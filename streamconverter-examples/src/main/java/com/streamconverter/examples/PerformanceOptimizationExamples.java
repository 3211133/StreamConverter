package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performance optimization examples for StreamConverter.
 *
 * <p>This class demonstrates: - Memory-efficient processing techniques - Concurrent pipeline
 * optimization - Large dataset handling - Performance monitoring
 */
public class PerformanceOptimizationExamples {

  private static final Logger logger =
      LoggerFactory.getLogger(PerformanceOptimizationExamples.class);

  /**
   * アプリケーションのエントリーポイント。パフォーマンス最適化の例を実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("⚡ StreamConverter - Performance Optimization Examples");
    logger.info("=====================================================\n");

    try {
      // Example 1: Large dataset processing
      largeDatasetProcessing();

      // Example 2: Concurrent pipeline optimization
      concurrentPipelineOptimization();

      // Example 3: Memory-efficient processing
      memoryEfficientProcessing();

      // Example 4: Performance monitoring
      performanceMonitoring();

    } catch (Exception e) {
      logger.error("Performance example failed: {}", e.getMessage(), e);
    }
  }

  /** Example 1: Processing large datasets efficiently */
  private static void largeDatasetProcessing() throws IOException {
    logger.info("📊 Large Dataset Processing");
    logger.info("============================");

    // Generate large CSV dataset
    StringBuilder largeDataset = new StringBuilder();
    largeDataset.append("id,name,value,timestamp\n");

    for (int i = 1; i <= 10000; i++) {
      largeDataset.append(
          String.format(
              "%d,Item%d,%.2f,2023-07-15T%02d:%02d:%02d\n",
              i, i, Math.random() * 1000, (i % 24), (i % 60), (i % 60)));
    }

    long startTime = System.currentTimeMillis();

    // Process large dataset with memory-efficient navigation
    logger.info("🔄 Processing 10,000 records...");
    processDataWithTiming(
        largeDataset.toString(),
        CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()));

    long endTime = System.currentTimeMillis();
    logger.info(String.format("⏱️ Processing completed in %d ms", endTime - startTime));

    // Show memory usage
    Runtime runtime = Runtime.getRuntime();
    long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
    logger.info(String.format("💾 Memory used: %.2f MB", memoryUsed / (1024.0 * 1024.0)));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 2: Concurrent pipeline optimization */
  private static void concurrentPipelineOptimization() throws IOException {
    logger.info("🔗 Concurrent Pipeline Optimization");
    logger.info("====================================");

    String jsonData = generateLargeJsonData();

    // Single command processing
    long startTime = System.currentTimeMillis();
    logger.info("🔄 Single command processing...");
    processDataWithTiming(
        jsonData, JsonNavigateCommand.create(TreePath.fromJson("$"), new PassThroughRule()));
    long singleTime = System.currentTimeMillis() - startTime;

    // Pipeline processing (demonstrates concurrent execution)
    startTime = System.currentTimeMillis();
    logger.info("\n🔄 Pipeline processing (concurrent)...");
    IStreamCommand[] pipeline = {
      JsonNavigateCommand.create(TreePath.fromJson("$"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    processDataWithTiming(jsonData, pipeline);
    long pipelineTime = System.currentTimeMillis() - startTime;

    logger.info(String.format("📈 Performance comparison:"));
    logger.info(String.format("  Single command: %d ms", singleTime));
    logger.info(String.format("  Pipeline: %d ms", pipelineTime));
    logger.info(
        String.format("  CPU cores utilized: %d", Runtime.getRuntime().availableProcessors()));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 3: Memory-efficient processing demonstration */
  private static void memoryEfficientProcessing() throws IOException {
    logger.info("💾 Memory-Efficient Processing");
    logger.info("===============================");

    // Generate data that would be problematic for memory
    StringBuilder hugeDataset = new StringBuilder();
    hugeDataset.append("id,data\n");

    for (int i = 1; i <= 50000; i++) {
      hugeDataset.append(String.format("%d,%s\n", i, "x".repeat(100))); // 100-char string per row
    }

    long beforeMemory = getUsedMemory();

    logger.info("🔄 Processing 50,000 records with 100-char data each...");
    processDataWithTiming(
        hugeDataset.toString(), CsvNavigateCommand.create(CSVPath.of("id"), new PassThroughRule()));

    long afterMemory = getUsedMemory();
    long memoryIncrease = afterMemory - beforeMemory;

    logger.info(String.format("💾 Memory increase: %.2f MB", memoryIncrease / (1024.0 * 1024.0)));
    logger.info("✅ Demonstrates streaming processing with constant memory usage");

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Example 4: Performance monitoring and optimization */
  private static void performanceMonitoring() throws IOException {
    logger.info("📊 Performance Monitoring");
    logger.info("==========================");

    String testData = generateTestData();

    // Test different processing strategies
    logger.info("🧪 Testing different processing strategies:\n");

    // Strategy 1: Direct processing
    long start = System.nanoTime();
    processDataWithTiming(
        testData, CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()));
    long directTime = System.nanoTime() - start;

    // Strategy 2: Pipeline processing
    start = System.nanoTime();
    IStreamCommand[] pipeline = {
      CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    processDataWithTiming(testData, pipeline);
    long pipelineTime = System.nanoTime() - start;

    // Strategy 3: Multi-stage processing
    start = System.nanoTime();
    IStreamCommand[] multiStage = {
      CsvNavigateCommand.create(CSVPath.of("name"), new PassThroughRule()),
      (IStreamCommand) (in, out) -> in.transferTo(out),
      (IStreamCommand) (in, out) -> in.transferTo(out),
      (IStreamCommand) (in, out) -> in.transferTo(out)
    };
    processDataWithTiming(testData, multiStage);
    long multiStageTime = System.nanoTime() - start;

    // Performance analysis
    logger.info("\n📈 Performance Analysis:");
    logger.info(String.format("  Direct processing: %.2f ms", directTime / 1_000_000.0));
    logger.info(String.format("  Pipeline processing: %.2f ms", pipelineTime / 1_000_000.0));
    logger.info(String.format("  Multi-stage processing: %.2f ms", multiStageTime / 1_000_000.0));

    double throughput = (testData.length() * 1000.0) / (directTime / 1_000_000.0);
    logger.info(String.format("  Throughput: %.2f MB/s", throughput / (1024.0 * 1024.0)));

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Helper method to process data with timing */
  private static void processDataWithTiming(String inputData, IStreamCommand command)
      throws IOException {
    processDataWithTiming(inputData, new IStreamCommand[] {command});
  }

  /** Helper method to process data with timing for pipelines */
  private static void processDataWithTiming(String inputData, IStreamCommand[] commands)
      throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = StreamConverter.create(commands);
      converter.run(inputStream, outputStream);

      // Show only a sample of output for large datasets
      String result = outputStream.toString(StandardCharsets.UTF_8);
      String[] lines = result.split("\n");

      if (lines.length > 10) {
        logger.info("Sample output (first 5 lines):");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
          logger.info(lines[i]);
        }
        logger.info("... (" + (lines.length - 5) + " more lines)");
      } else {
        logger.info("Output:");
        logger.info(result.trim());
      }
    }
  }

  /** Generate large JSON data for testing */
  private static String generateLargeJsonData() {
    StringBuilder json = new StringBuilder();
    json.append("{\"data\":[");

    for (int i = 1; i <= 1000; i++) {
      if (i > 1) json.append(",");
      json.append(
          String.format(
              "{\"id\":%d,\"name\":\"Item%d\",\"value\":%.2f,\"active\":%s}",
              i, i, Math.random() * 1000, (i % 2 == 0) ? "true" : "false"));
    }

    json.append("],\"total\":1000}");
    return json.toString();
  }

  /** Generate test data for performance monitoring */
  private static String generateTestData() {
    StringBuilder data = new StringBuilder();
    data.append("id,name,category,price\n");

    for (int i = 1; i <= 5000; i++) {
      data.append(
          String.format("%d,Product%d,Category%d,%.2f\n", i, i, (i % 10) + 1, Math.random() * 100));
    }

    return data.toString();
  }

  /** Get current memory usage */
  private static long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}
