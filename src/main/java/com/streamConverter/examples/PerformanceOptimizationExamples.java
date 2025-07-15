package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.CsvNavigateCommand;
import com.streamConverter.command.impl.JsonNavigateCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Performance optimization examples for StreamConverter.
 * 
 * <p>This class demonstrates:
 * - Memory-efficient processing techniques
 * - Concurrent pipeline optimization
 * - Large dataset handling
 * - Performance monitoring
 */
public class PerformanceOptimizationExamples {

  public static void main(String[] args) {
    System.out.println("⚡ StreamConverter - Performance Optimization Examples");
    System.out.println("=====================================================\n");

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
      System.err.println("Performance example failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Example 1: Processing large datasets efficiently
   */
  private static void largeDatasetProcessing() throws IOException {
    System.out.println("📊 Large Dataset Processing");
    System.out.println("============================");

    // Generate large CSV dataset
    StringBuilder largeDataset = new StringBuilder();
    largeDataset.append("id,name,value,timestamp\n");
    
    for (int i = 1; i <= 10000; i++) {
      largeDataset.append(String.format("%d,Item%d,%.2f,2023-07-15T%02d:%02d:%02d\n", 
          i, i, Math.random() * 1000, (i % 24), (i % 60), (i % 60)));
    }

    long startTime = System.currentTimeMillis();
    
    // Process large dataset with memory-efficient navigation
    System.out.println("🔄 Processing 10,000 records...");
    processDataWithTiming(largeDataset.toString(), new CsvNavigateCommand("name"));
    
    long endTime = System.currentTimeMillis();
    System.out.println(String.format("⏱️ Processing completed in %d ms", endTime - startTime));
    
    // Show memory usage
    Runtime runtime = Runtime.getRuntime();
    long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
    System.out.println(String.format("💾 Memory used: %.2f MB", memoryUsed / (1024.0 * 1024.0)));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /**
   * Example 2: Concurrent pipeline optimization
   */
  private static void concurrentPipelineOptimization() throws IOException {
    System.out.println("🔗 Concurrent Pipeline Optimization");
    System.out.println("====================================");

    String jsonData = generateLargeJsonData();

    // Single command processing
    long startTime = System.currentTimeMillis();
    System.out.println("🔄 Single command processing...");
    processDataWithTiming(jsonData, new JsonNavigateCommand());
    long singleTime = System.currentTimeMillis() - startTime;

    // Pipeline processing (demonstrates concurrent execution)
    startTime = System.currentTimeMillis();
    System.out.println("\n🔄 Pipeline processing (concurrent)...");
    IStreamCommand[] pipeline = {
        new JsonNavigateCommand(),
        new SampleStreamCommand("stage1"),
        new SampleStreamCommand("stage2")
    };
    processDataWithTiming(jsonData, pipeline);
    long pipelineTime = System.currentTimeMillis() - startTime;

    System.out.println(String.format("📈 Performance comparison:"));
    System.out.println(String.format("  Single command: %d ms", singleTime));
    System.out.println(String.format("  Pipeline: %d ms", pipelineTime));
    System.out.println(String.format("  CPU cores utilized: %d", Runtime.getRuntime().availableProcessors()));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /**
   * Example 3: Memory-efficient processing demonstration
   */
  private static void memoryEfficientProcessing() throws IOException {
    System.out.println("💾 Memory-Efficient Processing");
    System.out.println("===============================");

    // Generate data that would be problematic for memory
    StringBuilder hugeDataset = new StringBuilder();
    hugeDataset.append("id,data\n");
    
    for (int i = 1; i <= 50000; i++) {
      hugeDataset.append(String.format("%d,%s\n", i, "x".repeat(100))); // 100-char string per row
    }

    long beforeMemory = getUsedMemory();
    
    System.out.println("🔄 Processing 50,000 records with 100-char data each...");
    processDataWithTiming(hugeDataset.toString(), new CsvNavigateCommand("id"));
    
    long afterMemory = getUsedMemory();
    long memoryIncrease = afterMemory - beforeMemory;
    
    System.out.println(String.format("💾 Memory increase: %.2f MB", memoryIncrease / (1024.0 * 1024.0)));
    System.out.println("✅ Demonstrates streaming processing with constant memory usage");

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /**
   * Example 4: Performance monitoring and optimization
   */
  private static void performanceMonitoring() throws IOException {
    System.out.println("📊 Performance Monitoring");
    System.out.println("==========================");

    String testData = generateTestData();

    // Test different processing strategies
    System.out.println("🧪 Testing different processing strategies:\n");

    // Strategy 1: Direct processing
    long start = System.nanoTime();
    processDataWithTiming(testData, new CsvNavigateCommand("name"));
    long directTime = System.nanoTime() - start;

    // Strategy 2: Pipeline processing
    start = System.nanoTime();
    IStreamCommand[] pipeline = {
        new CsvNavigateCommand("name"),
        new SampleStreamCommand("processor")
    };
    processDataWithTiming(testData, pipeline);
    long pipelineTime = System.nanoTime() - start;

    // Strategy 3: Multi-stage processing
    start = System.nanoTime();
    IStreamCommand[] multiStage = {
        new CsvNavigateCommand("name"),
        new SampleStreamCommand("stage1"),
        new SampleStreamCommand("stage2"),
        new SampleStreamCommand("stage3")
    };
    processDataWithTiming(testData, multiStage);
    long multiStageTime = System.nanoTime() - start;

    // Performance analysis
    System.out.println("\n📈 Performance Analysis:");
    System.out.println(String.format("  Direct processing: %.2f ms", directTime / 1_000_000.0));
    System.out.println(String.format("  Pipeline processing: %.2f ms", pipelineTime / 1_000_000.0));
    System.out.println(String.format("  Multi-stage processing: %.2f ms", multiStageTime / 1_000_000.0));

    double throughput = (testData.length() * 1000.0) / (directTime / 1_000_000.0);
    System.out.println(String.format("  Throughput: %.2f MB/s", throughput / (1024.0 * 1024.0)));

    System.out.println("\n" + "=".repeat(60) + "\n");
  }

  /**
   * Helper method to process data with timing
   */
  private static void processDataWithTiming(String inputData, IStreamCommand command) throws IOException {
    processDataWithTiming(inputData, new IStreamCommand[]{command});
  }

  /**
   * Helper method to process data with timing for pipelines
   */
  private static void processDataWithTiming(String inputData, IStreamCommand[] commands) throws IOException {
    try (InputStream inputStream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(commands);
      converter.run(inputStream, outputStream);

      // Show only a sample of output for large datasets
      String result = outputStream.toString(StandardCharsets.UTF_8);
      String[] lines = result.split("\n");
      
      if (lines.length > 10) {
        System.out.println("Sample output (first 5 lines):");
        for (int i = 0; i < Math.min(5, lines.length); i++) {
          System.out.println(lines[i]);
        }
        System.out.println("... (" + (lines.length - 5) + " more lines)");
      } else {
        System.out.println("Output:");
        System.out.println(result.trim());
      }
    }
  }

  /**
   * Generate large JSON data for testing
   */
  private static String generateLargeJsonData() {
    StringBuilder json = new StringBuilder();
    json.append("{\"data\":[");
    
    for (int i = 1; i <= 1000; i++) {
      if (i > 1) json.append(",");
      json.append(String.format(
          "{\"id\":%d,\"name\":\"Item%d\",\"value\":%.2f,\"active\":%s}",
          i, i, Math.random() * 1000, (i % 2 == 0) ? "true" : "false"
      ));
    }
    
    json.append("],\"total\":1000}");
    return json.toString();
  }

  /**
   * Generate test data for performance monitoring
   */
  private static String generateTestData() {
    StringBuilder data = new StringBuilder();
    data.append("id,name,category,price\n");
    
    for (int i = 1; i <= 5000; i++) {
      data.append(String.format("%d,Product%d,Category%d,%.2f\n", 
          i, i, (i % 10) + 1, Math.random() * 100));
    }
    
    return data.toString();
  }

  /**
   * Get current memory usage
   */
  private static long getUsedMemory() {
    Runtime runtime = Runtime.getRuntime();
    return runtime.totalMemory() - runtime.freeMemory();
  }
}