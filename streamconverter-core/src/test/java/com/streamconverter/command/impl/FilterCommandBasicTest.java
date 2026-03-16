package com.streamconverter.command.impl;

import static com.streamconverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.command.impl.xml.XmlFilterCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import com.streamconverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Basic tests for FilterCommand implementations
 *
 * <p>Tests the basic functionality of JsonFilterCommand, XmlFilterCommand, and CsvFilterCommand to
 * ensure they can extract data correctly without applying transformations.
 */
class FilterCommandBasicTest {

  @Test
  void testJsonFilterCommand_SimpleProperty() throws IOException {
    // Test data
    String jsonInput = "{\"name\":\"田中太郎\",\"age\":30,\"city\":\"東京\"}";

    // Create command to extract "name" property
    JsonFilterCommand command = JsonFilterCommand.create(TreePath.fromJson("$.name"));

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify
    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals("\"田中太郎\"", result);
  }

  @Test
  void testJsonFilterCommand_RootPath() throws IOException {
    // Test data
    String jsonInput = "{\"userId\":\"1001\",\"amount\":120000}";

    // Create command to extract entire JSON (root path)
    JsonFilterCommand command = JsonFilterCommand.create(TreePath.fromJson("$"));

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify
    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals(jsonInput, result);
  }

  @Test
  void testCsvFilterCommand_SingleColumn() throws IOException {
    // Test data
    String csvInput = createTestData("name,age,city", "田中太郎,30,東京", "佐藤花子,25,大阪");

    // Create command to extract "name" column
    CsvFilterCommand command = CsvFilterCommand.create(CSVPath.of("name"));

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify
    String result = output.toString(StandardCharsets.UTF_8);
    String expected = createTestData("name", "田中太郎", "佐藤花子", "");
    assertEquals(expected, result);
  }

  @Test
  void testCsvFilterCommand_MultipleColumns() throws IOException {
    // Test data
    String csvInput = createTestData("name,age,city,country", "田中太郎,30,東京,日本", "佐藤花子,25,大阪,日本");

    // Create command to extract "name" and "city" columns
    CsvFilterCommand command =
        CsvFilterCommand.create(CSVPath.of(Arrays.asList("name", "city")), true);

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify
    String result = output.toString(StandardCharsets.UTF_8);
    String expected = createTestData("name,city", "田中太郎,東京", "佐藤花子,大阪", "");
    assertEquals(expected, result);
  }

  @Test
  void testXmlFilterCommand_SimpleElement() throws IOException {
    // Test data
    String xmlInput =
        "<?xml version=\"1.0\"?><users><user><name>田中太郎</name><age>30</age></user></users>";

    // Create command to extract "name" elements
    XmlFilterCommand command = XmlFilterCommand.create(TreePath.fromXml("users/user/name"));

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify output contains the extracted name element
    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("田中太郎"));
    assertTrue(result.contains("<name>") || result.contains("name"));
  }

  @Test
  void testJsonFilterCommand_NonExistentProperty() throws IOException {
    // Test data
    String jsonInput = "{\"name\":\"田中太郎\",\"age\":30}";

    // Create command to extract non-existent property
    JsonFilterCommand command = JsonFilterCommand.create(TreePath.fromJson("$.nonexistent"));

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(jsonInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify returns null for non-existent property
    String result = output.toString(StandardCharsets.UTF_8);
    assertEquals("null", result);
  }

  @Test
  void testCsvFilterCommand_NumericIndex() throws IOException {
    // Test data - no header
    String csvInput = createTestData("田中太郎,30,東京", "佐藤花子,25,大阪");

    // Create command to extract first column (index 0) without header
    CsvFilterCommand command = CsvFilterCommand.create(CSVPath.of("0"), false);

    // Execute
    ByteArrayInputStream input =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    // Verify
    String result = output.toString(StandardCharsets.UTF_8);
    String expected = createTestData("田中太郎", "佐藤花子", "");
    assertEquals(expected, result);
  }

  @Test
  void testStreamingJsonFilterBehavior() throws IOException {
    // Create multiple JSON objects to observe streaming behavior
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("[\n");

    for (int i = 0; i < 100; i++) {
      if (i > 0) jsonBuilder.append(",\n");
      jsonBuilder.append(
          String.format(
              "  {\"id\": %d, \"name\": \"User %d\", \"category\": \"Category %d\", \"value\": %d}",
              i, i, i % 10, i * 100));
    }
    jsonBuilder.append("\n]");

    String jsonData = jsonBuilder.toString();

    JsonFilterCommand command = JsonFilterCommand.create(TreePath.fromJson("$[*].name"));

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute JSON filtering
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during JSON filtering");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during JSON filtering");

    // Verify the filtering was successful
    String output = monitoringOutputStream.getContent();
    assertTrue(output.contains("User"), "Should contain filtered JSON data");
  }

  @Test
  void testStreamingCsvFilterBehavior() throws IOException {
    // Create CSV data with multiple columns to observe streaming behavior
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,name,email,department,salary\n");

    for (int i = 0; i < 200; i++) {
      csvBuilder.append(
          String.format(
              "%d,Employee %d,emp%d@company.com,Department %d,%.2f%n",
              i, i, i, i % 10, 50000.0 + (i * 100)));
    }
    String csvData = csvBuilder.toString();

    CsvFilterCommand command =
        CsvFilterCommand.create(CSVPath.of(Arrays.asList("name", "department")), true);

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute CSV filtering
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during CSV filtering");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during CSV filtering");

    // Verify the filtering was successful
    String output = monitoringOutputStream.getContent();
    assertTrue(
        output.contains("name,department"), "Should contain CSV header for selected columns");
    assertTrue(output.contains("Employee"), "Should contain filtered CSV data");
  }

  @Test
  void testStreamingXmlFilterBehavior() throws IOException {
    // Create XML data with multiple elements to observe streaming behavior
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xmlBuilder.append("<records>\n");

    for (int i = 0; i < 150; i++) {
      xmlBuilder.append(
          String.format(
              "  <record id=\"%d\">%n"
                  + "    <name>Record %d</name>%n"
                  + "    <category>Category %d</category>%n"
                  + "    <data>Streaming test data for record %d with detailed information</data>%n"
                  + "    <status>active</status>%n"
                  + "  </record>%n",
              i, i, i % 10, i));
    }
    xmlBuilder.append("</records>");

    String xmlData = xmlBuilder.toString();

    XmlFilterCommand command = XmlFilterCommand.create(TreePath.fromXml("records/record/name"));

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(xmlData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute XML filtering
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during XML filtering");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during XML filtering");

    // Verify the filtering was successful
    String output = monitoringOutputStream.getContent();
    assertTrue(output.contains("Record"), "Should contain filtered XML data");
  }

  @Test
  void testIncrementalFilterProcessing() throws IOException {
    // Create large mixed format data for comprehensive incremental processing test
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\n  \"users\": [\n");

    for (int i = 0; i < 300; i++) {
      if (i > 0) jsonBuilder.append(",\n");
      jsonBuilder.append(
          String.format(
              "    {\"id\": %d, \"name\": \"User %d\", \"email\": \"user%d@example.com\", "
                  + "\"profile\": {\"department\": \"Dept %d\", \"role\": \"Role %d\"}, "
                  + "\"metadata\": {\"created\": \"2024-01-%02d\", \"active\": true}}",
              i, i, i, i % 20, i % 5, (i % 28) + 1));
    }
    jsonBuilder.append("\n  ]\n}");

    String jsonData = jsonBuilder.toString();

    JsonFilterCommand command =
        JsonFilterCommand.create(TreePath.fromJson("$.users[*].profile.department"));

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental filtering processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(), "Output should be written during filtering");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 20000,
        "Should have processed substantial amount of JSON data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "Filtering should take measurable time");

    // Verify output was generated correctly
    String output = monitoringOutputStream.getContent();
    assertTrue(output.contains("Dept"), "Filtering should produce expected department data");
  }

  @Test
  @DisplayName("[#550] XmlFilterCommand captures nested child elements correctly")
  void testXmlFilterCommand_NestedChildElements() throws IOException {
    String xmlInput =
        "<?xml version=\"1.0\"?>"
            + "<catalog>"
            + "<book id=\"1\"><title>Java</title><author>Gosling</author></book>"
            + "<book id=\"2\"><title>Kotlin</title><author>JetBrains</author></book>"
            + "</catalog>";

    XmlFilterCommand command = XmlFilterCommand.create(TreePath.fromXml("catalog/book"));

    ByteArrayInputStream input =
        new ByteArrayInputStream(xmlInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    // Both book elements should be captured with their children
    assertTrue(result.contains("Java"), "First book title should be present");
    assertTrue(result.contains("Gosling"), "First book author should be present");
    assertTrue(result.contains("Kotlin"), "Second book title should be present");
    assertTrue(result.contains("JetBrains"), "Second book author should be present");
    assertTrue(result.contains("<book"), "Book start tag should be preserved");
    assertTrue(result.contains("</book>"), "Book end tag should be preserved");
  }
}
