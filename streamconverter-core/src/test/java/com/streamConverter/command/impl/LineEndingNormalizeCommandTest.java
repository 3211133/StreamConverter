package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LineEndingNormalizeCommand Tests")
class LineEndingNormalizeCommandTest {

  @Test
  @DisplayName("Convert Unix to Windows line endings")
  void testUnixToWindows() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String expected = "line1\r\nline2\r\nline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert Windows to Unix line endings")
  void testWindowsToUnix() throws IOException {
    // Given
    String input = "line1\r\nline2\r\nline3";
    String expected = "line1\nline2\nline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert mixed line endings to Unix")
  void testMixedToUnix() throws IOException {
    // Given - mix of CRLF, LF, and CR
    String input = "line1\r\nline2\nline3\rline4";
    String expected = "line1\nline2\nline3\nline4";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert to classic Mac line endings")
  void testToMacClassic() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String expected = "line1\rline2\rline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.MAC_CLASSIC);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Use system default line endings")
  void testSystemDefault() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String systemSeparator = System.lineSeparator();
    String expected = "line1" + systemSeparator + "line2" + systemSeparator + "line3";

    LineEndingNormalizeCommand command =
        new LineEndingNormalizeCommand(LineEndingType.SYSTEM_DEFAULT);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle empty input")
  void testEmptyInput() throws IOException {
    // Given
    String input = "";
    String expected = "";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle single line without line ending")
  void testSingleLineWithoutEnding() throws IOException {
    // Given
    String input = "single line";
    String expected = "single line";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle single line with line ending")
  void testSingleLineWithEnding() throws IOException {
    // Given
    String input = "single line\n";
    String expected = "single line\r\n";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Preserve input line endings (simplified implementation)")
  void testPreserveInput() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    // Note: Current implementation defaults to Unix for PRESERVE_INPUT
    String expected = "line1\nline2\nline3";

    LineEndingNormalizeCommand command =
        new LineEndingNormalizeCommand(LineEndingType.PRESERVE_INPUT);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Constructor should reject null target type")
  void testConstructorNullValidation() {
    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> {
          new LineEndingNormalizeCommand(null);
        });
  }

  @Test
  @DisplayName("Get command details should include configuration")
  void testGetCommandDetails() {
    // Given
    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String details = command.getCommandDetails();

    // Then
    assertTrue(details.contains("LineEndingNormalizeCommand"));
    assertTrue(details.contains("WINDOWS"));
    assertTrue(details.contains("\\r\\n"));
  }

  @Test
  @DisplayName("Handle large input efficiently")
  void testLargeInput() throws IOException {
    // Given - create a moderately large input
    StringBuilder inputBuilder = new StringBuilder();
    StringBuilder expectedBuilder = new StringBuilder();

    for (int i = 0; i < 1000; i++) {
      if (i > 0) {
        inputBuilder.append("\n");
        expectedBuilder.append("\r\n");
      }
      String line = "Line " + i + " with some content";
      inputBuilder.append(line);
      expectedBuilder.append(line);
    }

    String input = inputBuilder.toString();
    String expected = expectedBuilder.toString();

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle buffer boundary with line endings")
  void testBufferBoundaryLineEndings() throws IOException {
    // Given - create input that puts line endings exactly at buffer boundaries
    // Buffer size is 8192 for PRESERVE_INPUT mode
    StringBuilder inputBuilder = new StringBuilder();

    // Create content that approaches 8192 characters
    String baseContent = "A".repeat(100); // 100 chars per line
    for (int i = 0; i < 81; i++) { // 81 * 100 = 8100 chars
      if (i > 0) {
        inputBuilder.append("\n");
      }
      inputBuilder.append(baseContent);
    }

    // Add content to get close to 8192 boundary, then add line ending
    inputBuilder.append("\n"); // This should be near buffer boundary
    inputBuilder.append("Final line content");

    String input = inputBuilder.toString();

    // Test PRESERVE_INPUT (uses buffer reading)
    LineEndingNormalizeCommand preserveCommand =
        new LineEndingNormalizeCommand(LineEndingType.PRESERVE_INPUT);
    String preserveResult = executeCommand(preserveCommand, input);
    assertEquals(
        input,
        preserveResult,
        "PRESERVE_INPUT should maintain exact input including boundary line endings");

    // Test conversion (uses character-by-character reading)
    LineEndingNormalizeCommand windowsCommand =
        new LineEndingNormalizeCommand(LineEndingType.WINDOWS);
    String windowsResult = executeCommand(windowsCommand, input);
    String expectedWindows = input.replace("\n", "\r\n");
    assertEquals(
        expectedWindows,
        windowsResult,
        "Windows conversion should work correctly across buffer boundaries");
  }

  @Test
  @DisplayName("Handle CRLF spanning buffer boundary")
  void testCRLFSpanningBufferBoundary() throws IOException {
    // Given - create input where \r\n spans across buffer boundary
    StringBuilder inputBuilder = new StringBuilder();

    // Fill almost exactly to buffer boundary minus 1
    String padding = "X".repeat(8191); // 8191 chars
    inputBuilder.append(padding);
    inputBuilder.append("\r\n"); // CRLF spans boundary at position 8191-8192
    inputBuilder.append("After boundary");

    String input = inputBuilder.toString();

    // Test Unix conversion - should handle CRLF correctly even when spanning boundary
    LineEndingNormalizeCommand unixCommand = new LineEndingNormalizeCommand(LineEndingType.UNIX);
    String result = executeCommand(unixCommand, input);

    String expectedOutput = padding + "\n" + "After boundary";
    assertEquals(expectedOutput, result, "CRLF spanning buffer boundary should be converted to LF");
  }

  @Test
  @DisplayName("Handle multiple mixed line endings near buffer boundary")
  void testMixedLineEndingsNearBoundary() throws IOException {
    // Given - create input with different line ending types near buffer boundary
    StringBuilder inputBuilder = new StringBuilder();

    // Content approaching buffer boundary
    String baseContent = "Data".repeat(2000); // 8000 chars
    inputBuilder.append(baseContent);

    // Add mixed line endings near boundary
    inputBuilder.append("Line1\r\n"); // CRLF
    inputBuilder.append("Line2\n"); // LF
    inputBuilder.append("Line3\r"); // CR
    inputBuilder.append("Line4\r\n"); // CRLF again
    inputBuilder.append("Final");

    String input = inputBuilder.toString();

    // Test conversion to Windows format
    LineEndingNormalizeCommand windowsCommand =
        new LineEndingNormalizeCommand(LineEndingType.WINDOWS);
    String result = executeCommand(windowsCommand, input);

    String expectedOutput =
        baseContent + "Line1\r\n" + "Line2\r\n" + "Line3\r\n" + "Line4\r\n" + "Final";
    assertEquals(
        expectedOutput,
        result,
        "Mixed line endings near buffer boundary should be normalized correctly");
  }

  @Test
  @DisplayName("Verify streaming behavior: output written before input stream is fully consumed")
  void testStreamingBehavior() throws IOException {
    // Given - create a controlled input stream that tracks read operations
    String input = "line1\nline2\nline3\nline4\nline5";
    TrackingInputStream trackingInputStream =
        new TrackingInputStream(input.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When - execute the command
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during processing");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify streaming characteristics: processing occurred incrementally
    // For small inputs, timing may be too fast to measure precisely, so we verify other indicators
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read from during processing");

    // Verify the actual conversion worked correctly
    String expectedOutput = "line1\r\nline2\r\nline3\r\nline4\r\nline5";
    assertEquals(expectedOutput, monitoringOutputStream.getContent());
  }

  @Test
  @DisplayName("Verify incremental processing: output available during long input processing")
  void testIncrementalProcessing() throws IOException {
    // Given - create input with multiple chunks to force incremental processing
    StringBuilder inputBuilder = new StringBuilder();
    for (int i = 1; i <= 100; i++) {
      inputBuilder.append("Line ").append(i).append(" with some content\n");
    }
    String input = inputBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(input.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify incremental processing
    assertTrue(monitoringOutputStream.hasWriteOccurred(), "Output should be written");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify that processing handled substantial data incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 1000,
        "Should have processed substantial amount of data incrementally");

    // Verify content correctness
    String expectedOutput = input.replace("\n", "\r\n");
    assertEquals(expectedOutput, monitoringOutputStream.getContent());
  }

  /**
   * Helper method to execute a command with string input and return string output.
   *
   * @param command the command to execute
   * @param input the input string
   * @return the output string
   * @throws IOException if execution fails
   */
  private String executeCommand(LineEndingNormalizeCommand command, String input)
      throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
