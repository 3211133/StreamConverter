package com.streamconverter.command.impl.csv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamconverter.test.StreamingTestUtils.TrackingInputStream;
import com.streamconverter.test.TestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for CsvWalker. */
class CsvWalkerTest {

  private CsvWalker command;

  @BeforeEach
  void setUp() {
    // Use a specific column selector instead of createForAll
    command = CsvWalker.create(CSVPath.of("name"), new PassThroughRule());
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

    String result = ((ByteArrayOutputStream) outputStream).toString(StandardCharsets.UTF_8);
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
  void testStreamingCsvNavigationBehavior() throws IOException {
    // Create moderate-sized CSV data to observe streaming behavior
    StringBuilder csvBuilder = new StringBuilder();
    csvBuilder.append("id,product_name,category,price,description\n");

    for (int i = 0; i < 100; i++) {
      csvBuilder.append(
          String.format(
              "%d,\"Product %d\",\"Category %d\",%.2f,\"This is a detailed description of product %d with various features and specifications\"%n",
              i, i, i % 10, 19.99 + (i * 0.5), i));
    }
    String csvData = csvBuilder.toString();

    // Create command for the specific column that exists in this test's data
    CsvWalker testCommand = CsvWalker.create(CSVPath.of("id"), new PassThroughRule());

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute CSV navigation
    testCommand.execute(trackingInputStream, monitoringOutputStream);

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
  @DisplayName("[#547] Concurrent execution of same instance does not corrupt column index")
  void testConcurrentExecutionThreadSafety() throws Exception {
    // Use a real transformation (upper-case) so we can verify the correct column was targeted
    String csvInput = "name,age,city\nAlice,30,NYC\nBob,25,LA\nCarol,35,Chicago\n";
    CsvWalker sharedCommand = CsvWalker.create(CSVPath.of("name"), s -> s.toUpperCase());

    int threadCount = 8;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    List<Callable<String>> tasks = new ArrayList<>();

    for (int i = 0; i < threadCount; i++) {
      tasks.add(
          () -> {
            ByteArrayInputStream input =
                new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            sharedCommand.execute(input, output);
            return output.toString(StandardCharsets.UTF_8);
          });
    }

    List<Future<String>> results;
    try {
      results = executor.invokeAll(tasks);
    } finally {
      executor.shutdown();
    }

    for (Future<String> result : results) {
      String output = result.get();
      // Header row should be unchanged
      assertTrue(output.contains("name"), "Header 'name' should appear in output");
      // name column values should be upper-cased (proving the correct column was transformed)
      assertTrue(output.contains("ALICE"), "name column value 'Alice' should be upper-cased");
      assertTrue(output.contains("BOB"), "name column value 'Bob' should be upper-cased");
      // age and city columns should not be upper-cased
      assertTrue(output.contains("30"), "age value should be unchanged");
      // Verify each thread got a complete result: 1 header + 3 data rows = 4 non-blank lines
      long lineCount = output.lines().filter(l -> !l.isBlank()).count();
      assertEquals(4, lineCount, "Output should contain header plus 3 data rows");
    }
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
              "%d,\"Employee%d\",\"LastName%d\",\"Department %d\",\"Position %d\",%.2f,\"2024-01-%02d\",\"emp%d@company.com\",\"Address %d Street, City %d\",\"555-000-%04d\",\"Detailed employee notes and performance review data for employee %d with extensive background information\"%n",
              i, i, i, i % 10, i % 5, 50000.0 + (i * 100), (i % 28) + 1, i, i, i % 20, i, i));
    }
    String csvData = csvBuilder.toString();

    // Create command for the specific column that exists in this test's data
    CsvWalker testCommand = CsvWalker.create(CSVPath.of("employee_id"), new PassThroughRule());

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental CSV navigation processing
    long processingStart = System.nanoTime();
    testCommand.execute(trackingInputStream, monitoringOutputStream);
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

  @Test
  @DisplayName("[#546] RFC 4180: double-quote escaping in target column is preserved")
  void testRfc4180DoubleQuoteEscaping() throws IOException, CsvValidationException {
    // Field containing a quote character: She said "hello"  (RFC 4180: doubled quotes)
    String csvInput = "name,comment\nAlice,\"She said \"\"hello\"\"\"\n";
    // Select the "comment" column which contains the quoted value
    CsvWalker testCommand = CsvWalker.create(CSVPath.of("comment"), new PassThroughRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    testCommand.execute(input, output);

    // Re-parse the output with opencsv to verify the round-trip is exact
    String result = output.toString(StandardCharsets.UTF_8);
    try (CSVReader reader =
        new CSVReader(
            new InputStreamReader(
                new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8))) {
      String[] header = reader.readNext();
      assertNotNull(header, "Output should have a header row");
      int commentIndex = -1;
      for (int i = 0; i < header.length; i++) {
        if ("comment".equals(header[i])) {
          commentIndex = i;
          break;
        }
      }
      assertTrue(commentIndex >= 0, "Output should contain the 'comment' column");

      String[] dataRow = reader.readNext();
      assertNotNull(dataRow, "Output should have at least one data row");
      assertEquals(
          "She said \"hello\"",
          dataRow[commentIndex],
          "RFC 4180 double-quote escaping should round-trip correctly");
    }
  }

  @Test
  @DisplayName("[#546] RFC 4180: output uses CRLF line endings per RFC 4180 §2")
  void testOutputUsesCrlfLineEndings() throws IOException {
    String csvInput = "name,age\nAlice,30\n";
    CsvWalker testCommand = CsvWalker.create(CSVPath.of("name"), new PassThroughRule());

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    testCommand.execute(
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8)), output);

    String result = output.toString(StandardCharsets.UTF_8);
    assertTrue(result.contains("\r\n"), "CSV output must use CRLF (\\r\\n) per RFC 4180 §2");
  }

  @Test
  @DisplayName("[#546] RFC 4180: comma inside quoted field is not split")
  void testRfc4180CommaInsideQuotedField() throws IOException {
    String csvInput = "name,address\nAlice,\"123 Main St, Suite 4\"\n";
    CsvWalker testCommand = CsvWalker.create(CSVPath.of("address"), new PassThroughRule());

    ByteArrayInputStream input =
        new ByteArrayInputStream(csvInput.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    testCommand.execute(input, output);

    String result = output.toString(StandardCharsets.UTF_8);
    // The address field with internal comma should be preserved intact
    assertTrue(result.contains("123 Main St"), "Address should be preserved");
    assertTrue(result.contains("Suite 4"), "Comma-separated part of address should be preserved");
  }
}
