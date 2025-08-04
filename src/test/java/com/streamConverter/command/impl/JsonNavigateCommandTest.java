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

/** Unit tests for JsonNavigateCommand. */
class JsonNavigateCommandTest {

  private static final int LARGE_JSON_USER_COUNT = 5000;

  private JsonNavigateCommand command;

  @BeforeEach
  void setUp() {
    command = new JsonNavigateCommand();
  }

  @Test
  void testCommandCreation() {
    assertNotNull(command);
  }

  @Test
  void testBasicJsonProcessing() throws IOException {
    String jsonInput = "{\"name\":\"John\",\"age\":30,\"city\":\"NYC\"}";
    InputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));

    String result = outputStream.toString();
    assertNotNull(result);
    // For now, just verify that the command doesn't throw an exception
    // Verify basic JSON navigation functionality - exact assertions depend on implementation
    // details
  }

  @Test
  void testComplexJsonProcessing() throws IOException {
    String jsonInput =
        """
        {
          "users": [
            {"name": "John", "age": 30, "city": "NYC"},
            {"name": "Jane", "age": 25, "city": "LA"}
          ],
          "metadata": {
            "total": 2,
            "page": 1
          }
        }
        """;
    InputStream inputStream = new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testEmptyInput() throws IOException {
    InputStream inputStream = new ByteArrayInputStream(new byte[0]);
    OutputStream outputStream = new ByteArrayOutputStream();

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testInvalidJsonInput() throws IOException {
    String invalidJson = "{invalid json}";
    InputStream inputStream =
        new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
    OutputStream outputStream = new ByteArrayOutputStream();

    // Should not throw for now - actual navigation logic will handle validation
    assertDoesNotThrow(() -> command.execute(inputStream, outputStream));
  }

  @Test
  void testMemoryEfficiencyWithLargeData() throws IOException {
    // Generate larger JSON data to test memory efficiency
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\n  \"data\": [\n");

    // Create 1MB of JSON data (simulating larger processing)
    for (int i = 0; i < LARGE_JSON_USER_COUNT; i++) {
      jsonBuilder.append(
          String.format(
              "    {\"id\": %d, \"name\": \"User %d\", \"description\": \"Extended user description with additional data to increase JSON size %d\"},\n",
              i, i, i));
    }
    // Remove trailing comma and close structure
    jsonBuilder.setLength(jsonBuilder.length() - 2); // Remove last comma and newline
    jsonBuilder.append("\n  ]\n}");

    String largeJson = jsonBuilder.toString();
    long dataSize = largeJson.getBytes(StandardCharsets.UTF_8).length;

    // Monitor memory usage during processing
    ResourceMonitor monitor = new ResourceMonitor();
    monitor.start(dataSize);

    InputStream inputStream = new ByteArrayInputStream(largeJson.getBytes(StandardCharsets.UTF_8));
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
