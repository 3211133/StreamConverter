package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamConverter.benchmark.ResourceMonitor;
import com.streamConverter.benchmark.ResourceUsage;
import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
import com.streamConverter.test.TestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for CsvNavigateCommand. */
class CsvNavigateCommandTest {

  private static final int LARGE_CSV_ROW_COUNT = 8000;

  private CsvNavigateCommand command;

  @BeforeEach
  void setUp() {
    command = new CsvNavigateCommand();
  }

  @Test
  void testCommandCreation() {
    assertNotNull(command);
  }

  @Test
  void testBasicCsvProcessing() throws IOException {
    String csvInput = TestUtils.createTestData("name,age,city", "John,30,NYC", "Jane,25,LA");
    InputStream inputStream = new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));

    String result = outputStream.toString();
    assertNotNull(result);
    // Verify basic CSV navigation functionality
    // The command should handle the input without throwing exceptions
    // and produce some output (specific navigation logic depends on implementation)
  }

  @Test
  void testEmptyInput() throws IOException {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testLargeInput() throws IOException {
    StringBuilder largeInput = new StringBuilder();
    largeInput.append("name,age,city").append(System.lineSeparator());
    for (int i = 0; i < 1000; i++) {
      largeInput
          .append("Person")
          .append(i)
          .append(",")
          .append(20 + i % 50)
          .append(",City")
          .append(i % 10)
          .append(System.lineSeparator());
    }

    InputStream inputStream =
        new ByteArrayInputStream(largeInput.toString().getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testMemoryEfficiencyWithLargeData() throws IOException {
    // Generate larger CSV data to test memory efficiency
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,name,email,department,salary,description\n");

    // Create 1MB of CSV data (simulating larger processing)
    for (int i = 0; i < LARGE_CSV_ROW_COUNT; i++) {
      csvBuilder.append(
          String.format(
              "%d,Employee%d,employee%d@company.com,Department%d,%.2f,Extended employee description with additional data to increase CSV record size %d\n",
              i, i, i, i % 10, 50000.0 + (i * 100), i));
    }

    String largeCsv = csvBuilder.toString();
    long dataSize = largeCsv.getBytes(StandardCharsets.UTF_8).length;

    // Monitor memory usage during processing
    ResourceMonitor monitor = new ResourceMonitor();
    monitor.start(dataSize);

    InputStream inputStream = new ByteArrayInputStream(largeCsv.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Execute command with monitoring
    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));

    ResourceUsage usage = monitor.stop();

    // Verify memory efficiency: should use much less memory than data size
    // Memory usage should be minimal compared to data size (streaming processing)
    assertTrue(
        usage.getMemoryUsedMB() < 50,
        String.format(
            "Memory usage (%.2f MB) should be less than 50MB for %.2f MB data",
            usage.getMemoryUsedMB(), usage.getDataSizeMB()));

    // Verify processing was successful (data size matches)
    assertTrue(usage.getDataSizeMB() > 0, "Should have processed some data");
  }

  @Test
  void testStreamingCsvNavigationBehavior() throws IOException {
    // Create moderate-sized CSV data to observe streaming behavior
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,product_name,category,price,description\n");

    for (int i = 0; i < 100; i++) {
      csvBuilder.append(
          String.format(
              "%d,\"Product %d\",\"Category %d\",%.2f,\"This is a detailed description of product %d with various features and specifications\"\n",
              i, i, i % 10, 19.99 + (i * 0.5), i));
    }
    String csvData = csvBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute CSV navigation
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during CSV navigation");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during CSV processing");

    // Verify the content was processed (output should contain some CSV-related data)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "Should have produced some output from CSV navigation");
  }

  @Test
  void testIncrementalCsvNavigationProcessing() throws IOException {
    // Create complex CSV with various data types to force incremental processing
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append(
        "employee_id,first_name,last_name,department,position,salary,hire_date,email,address,phone,notes\n");

    for (int i = 1; i <= 200; i++) {
      csvBuilder.append(
          String.format(
              "%d,\"Employee%d\",\"LastName%d\",\"Department %d\",\"Position %d\",%.2f,\"2024-01-%02d\",\"emp%d@company.com\",\"Address %d Street, City %d\",\"555-000-%04d\",\"Detailed employee notes and performance review data for employee %d with extensive background information\"\n",
              i, i, i, i % 10, i % 5, 50000.0 + (i * 100), (i % 28) + 1, i, i, i % 20, i, i));
    }
    String csvData = csvBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental CSV navigation processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during CSV processing");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial CSV data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 10000,
        "Should have processed substantial amount of CSV data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "CSV processing should take measurable time");

    // Verify output was generated (CSV navigation should produce some result)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "CSV navigation should produce output");
  }
}
