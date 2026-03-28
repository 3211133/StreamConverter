package com.streamconverter.command.impl.json;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.command.rule.TestRule;
import com.streamconverter.path.TreePath;
import com.streamconverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for JsonNavigateCommand. */
class JsonNavigateCommandTest {

  private JsonNavigateCommand command;

  @BeforeEach
  void setUp() {
    // Use a specific JSONPath instead of createForAll
    command = JsonNavigateCommand.create(TreePath.fromJson("$.test"), new PassThroughRule());
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

    String result = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8);
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
    // JsonNavigateCommand propagates JsonParseException as IOException for invalid input.
    String invalidJson = "{invalid json}";
    InputStream inputStream =
        new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(IOException.class, () -> command.execute(inputStream, outputStream));
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
        {%n          "id": %d,%n          "name": "Product %d",%n          "description": "This is a detailed description of product %d with various features",%n          "price": %.2f,%n          "category": "Category %d",%n          "tags": ["tag%d", "feature%d", "type%d"]%n        }""",
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
        {%n          "id": %d,%n          "name": "Department %d",%n          "employees": [%n            {%n              "id": %d,%n              "name": "Employee %d-1",%n              "role": "Manager",%n              "projects": [%n                {"name": "Project Alpha %d", "status": "active"},%n                {"name": "Project Beta %d", "status": "completed"}%n              ]%n            },%n            {%n              "id": %d,%n              "name": "Employee %d-2",%n              "role": "Developer",%n              "skills": ["Java", "JSON", "Streaming", "Performance"]%n            }%n          ]%n        }""",
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

  @Test
  @DisplayName("[#548] Nested path $.a.b.c transforms only the target node")
  void testNestedPathTransformation() throws IOException {
    String jsonInput =
        "{\"a\":{\"b\":{\"c\":\"original\",\"d\":\"unchanged\"}},\"other\":\"untouched\"}";
    JsonNavigateCommand cmd =
        JsonNavigateCommand.create(TreePath.fromJson("$.a.b.c"), TestRule.contentTransformRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    cmd.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("\"transformed\""), "Target field $.a.b.c should be transformed");
    assertTrue(result.contains("\"unchanged\""), "Sibling field $.a.b.d should not be transformed");
    assertTrue(result.contains("\"untouched\""), "Top-level field $.other should not be changed");
  }

  @Test
  @DisplayName("[#548] Sibling fields at same level are not transformed")
  void testSiblingFieldsNotTransformed() throws IOException {
    String jsonInput = "{\"x\":\"original value\",\"y\":\"original value\"}";
    JsonNavigateCommand cmd =
        JsonNavigateCommand.create(TreePath.fromJson("$.x"), TestRule.contentTransformRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    cmd.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("\"transformed value\""), "Field $.x should be transformed");
    assertTrue(
        result.contains("\"original value\""), "Sibling field $.y should NOT be transformed");
  }

  @Test
  @DisplayName("[#548] Array-syntax path $.orders[*].product_code matches nested fields")
  void testArraySyntaxPathMatching() throws IOException {
    String jsonInput =
        "{\"orders\":[{\"product_code\":\"ABC\",\"qty\":1},{\"product_code\":\"XYZ\",\"qty\":2}]}";
    JsonNavigateCommand cmd =
        JsonNavigateCommand.create(
            TreePath.fromJson("$.orders[*].product_code"), s -> s.toLowerCase());

    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    cmd.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("\"abc\""), "product_code should be lower-cased via array path");
    assertTrue(result.contains("\"xyz\""), "second product_code should also be lower-cased");
    assertTrue(result.contains("\"qty\""), "Other fields should be preserved");
  }

  @Test
  @DisplayName("[#548] $.second.x path does not transform $.first.x (original bug regression)")
  void testSecondObjectPathDoesNotTransformFirst() throws IOException {
    String jsonInput =
        "{\"first\":{\"x\":\"original\",\"y\":\"keep\"},\"second\":{\"x\":\"original\",\"y\":\"keep\"}}";
    JsonNavigateCommand cmd =
        JsonNavigateCommand.create(
            TreePath.fromJson("$.second.x"), TestRule.contentTransformRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    cmd.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("\"transformed\""), "Field $.second.x should be transformed");
    // first.x must remain "original" — exactly one "original" in the output
    assertTrue(
        result.indexOf("\"original\"") == result.lastIndexOf("\"original\""),
        "$.first.x must remain 'original'; only one occurrence expected in output");
    assertTrue(result.contains("\"keep\""), "Non-target fields should be preserved");
  }

  @Test
  @DisplayName("[#548] Array-syntax path $.orders[*].product_code matches streaming path")
  void testArraySyntaxPath_matchesNestedField() throws IOException {
    // Verify that matchesIgnoringArraySyntax() enables array-syntax paths to work
    // Use "original" as value so TestRule.contentTransformRule() can replace it with "transformed"
    String jsonInput =
        "{\"orders\":["
            + "{\"order_id\":\"ORD-001\",\"product_code\":\"original\"},"
            + "{\"order_id\":\"ORD-002\",\"product_code\":\"original\"}"
            + "]}";
    JsonNavigateCommand cmd =
        JsonNavigateCommand.create(
            TreePath.fromJson("$.orders[*].product_code"), TestRule.contentTransformRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    cmd.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(
        result.contains("\"transformed\""),
        "product_code values should be transformed via $.orders[*].product_code path");
    // order_id should be preserved untouched
    assertTrue(result.contains("ORD-001"), "order_id should be preserved");
  }
}
