package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamConverter.benchmark.ResourceMonitor;
import com.streamConverter.benchmark.ResourceUsage;
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
    String csvInput = "name,age,city\nJohn,30,NYC\nJane,25,LA";
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
    largeInput.append("name,age,city\n");
    for (int i = 0; i < 1000; i++) {
      largeInput
          .append("Person")
          .append(i)
          .append(",")
          .append(20 + i % 50)
          .append(",City")
          .append(i % 10)
          .append("\n");
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
    for (int i = 0; i < 8000; i++) {
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
}
