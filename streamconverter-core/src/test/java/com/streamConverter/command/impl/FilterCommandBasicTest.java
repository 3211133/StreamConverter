package com.streamconverter.command.impl;

import static com.streamconverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.impl.csv.CsvFilterCommand;
import com.streamconverter.command.impl.json.JsonFilterCommand;
import com.streamconverter.command.impl.xml.XmlFilterCommand;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
    JsonFilterCommand command = new JsonFilterCommand(TreePath.fromJson("$.name"));

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
    JsonFilterCommand command = new JsonFilterCommand(TreePath.fromJson("$"));

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
    CsvFilterCommand command = CsvFilterCommand.create(new CSVPath("name"));

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
        CsvFilterCommand.create(new CSVPath(Arrays.asList("name", "city")), true);

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
    XmlFilterCommand command = new XmlFilterCommand(TreePath.fromXml("users/user/name"));

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
    JsonFilterCommand command = new JsonFilterCommand(TreePath.fromJson("$.nonexistent"));

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
    CsvFilterCommand command = CsvFilterCommand.create(new CSVPath("0"), false);

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
}
