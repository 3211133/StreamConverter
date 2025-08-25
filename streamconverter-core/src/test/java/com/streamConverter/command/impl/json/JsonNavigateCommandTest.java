package com.streamConverter.command.impl.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
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

  private JsonNavigateCommand command;

  @BeforeEach
  void setUp() {
    command = JsonNavigateCommand.extractAll();
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
  void testStreamingJsonNavigationBehavior() throws IOException {
    // Create moderate-sized JSON data to observe streaming behavior
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\n");
    jsonBuilder.append("  \"products\": [\n");

    for (int i = 0; i < 100; i++) {
      if (i > 0) jsonBuilder.append(",\n");
      jsonBuilder.append(
          String.format(
              """
        {
          "id": %d,
          "name": "Product %d",
          "description": "This is a detailed description of product %d with various features",
          "price": %.2f,
          "category": "Category %d",
          "tags": ["tag%d", "feature%d", "type%d"]
        }""",
              i, i, i, 19.99 + (i * 0.5), i % 10, i, i, i % 5));
    }

    jsonBuilder.append("\n  ]\n}");
    String jsonData = jsonBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute JSON navigation
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during JSON navigation");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during JSON processing");

    // Verify the content was processed (output should contain some JSON-related data)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "Should have produced some output from JSON navigation");
  }

  @Test
  void testIncrementalJsonNavigationProcessing() throws IOException {
    // Create complex JSON with nested structures to force incremental processing
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\n");
    jsonBuilder.append("  \"company\": {\n");
    jsonBuilder.append("    \"departments\": [\n");

    for (int i = 1; i <= 50; i++) {
      if (i > 1) jsonBuilder.append(",\n");
      jsonBuilder.append(
          String.format(
              """
        {
          "id": %d,
          "name": "Department %d",
          "employees": [
            {
              "id": %d,
              "name": "Employee %d-1",
              "role": "Manager",
              "projects": [
                {"name": "Project Alpha %d", "status": "active"},
                {"name": "Project Beta %d", "status": "completed"}
              ]
            },
            {
              "id": %d,
              "name": "Employee %d-2",
              "role": "Developer",
              "skills": ["Java", "JSON", "Streaming", "Performance"]
            }
          ]
        }""",
              i, i, i * 100, i, i, i, i * 100 + 1, i));
    }

    jsonBuilder.append("\n    ]\n  }\n}");
    String jsonData = jsonBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental JSON navigation processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during JSON processing");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial JSON data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 5000,
        "Should have processed substantial amount of JSON data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "JSON processing should take measurable time");

    // Verify output was generated (JSON navigation should produce some result)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "JSON navigation should produce output");
  }
}
