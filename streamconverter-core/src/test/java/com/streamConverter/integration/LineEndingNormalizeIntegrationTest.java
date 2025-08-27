package com.streamConverter.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.api.Streams;
import com.streamConverter.command.impl.LineEndingNormalizeCommand;
import com.streamConverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LineEndingNormalizeCommand Integration Tests")
class LineEndingNormalizeIntegrationTest {

  @Test
  @DisplayName("Integration with Streams API - Windows line endings")
  void testStreamsApiIntegrationWindows() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String expected = "line1\r\nline2\r\nline3";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.WINDOWS))
        .toStream(outputStream);

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Integration with Streams API - Unix line endings")
  void testStreamsApiIntegrationUnix() throws IOException {
    // Given
    String input = "line1\r\nline2\r\nline3\r\n";
    String expected = "line1\nline2\nline3\n";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.UNIX))
        .toStream(outputStream);

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Integration with multiple commands in pipeline")
  void testMultipleCommandsPipeline() throws IOException {
    // Given
    String input = "line1\r\nline2\nline3\r";
    String expectedAfterUnix = "line1\nline2\nline3\n";
    String expectedAfterWindows = "line1\r\nline2\r\nline3\r\n";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream intermediateStream = new ByteArrayOutputStream();
    ByteArrayOutputStream finalStream = new ByteArrayOutputStream();

    // When - First normalize to Unix
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.UNIX))
        .toStream(intermediateStream);

    String intermediateResult = intermediateStream.toString(StandardCharsets.UTF_8);
    assertEquals(expectedAfterUnix, intermediateResult);

    // Then normalize to Windows
    ByteArrayInputStream intermediateInput =
        new ByteArrayInputStream(intermediateResult.getBytes(StandardCharsets.UTF_8));

    Streams.from(intermediateInput)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.WINDOWS))
        .toStream(finalStream);

    // Then
    String finalResult = finalStream.toString(StandardCharsets.UTF_8);
    assertEquals(expectedAfterWindows, finalResult);
  }

  @Test
  @DisplayName("Integration with preserve input mode")
  void testPreserveInputMode() throws IOException {
    // Given
    String input = "line1\r\nline2\nline3\r";
    String expected = input; // Should preserve exactly as input

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.PRESERVE_INPUT))
        .toStream(outputStream);

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Integration with system default line endings")
  void testSystemDefaultMode() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String systemSeparator = System.lineSeparator();
    String expected = "line1" + systemSeparator + "line2" + systemSeparator + "line3";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.SYSTEM_DEFAULT))
        .toStream(outputStream);

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Integration test with large input data")
  void testLargeInputIntegration() throws IOException {
    // Given - Generate large input with mixed line endings
    StringBuilder inputBuilder = new StringBuilder();
    StringBuilder expectedBuilder = new StringBuilder();

    for (int i = 0; i < 5000; i++) {
      if (i > 0) {
        // Alternate between different line ending styles
        switch (i % 3) {
          case 0 -> {
            inputBuilder.append("\n");
            expectedBuilder.append("\r\n");
          }
          case 1 -> {
            inputBuilder.append("\r\n");
            expectedBuilder.append("\r\n");
          }
          case 2 -> {
            inputBuilder.append("\r");
            expectedBuilder.append("\r\n");
          }
          default -> {
            // This should never happen with i % 3, but added for SpotBugs SF compliance
            inputBuilder.append("\n");
            expectedBuilder.append("\r\n");
          }
        }
      }
      String line = "Line " + i + " content";
      inputBuilder.append(line);
      expectedBuilder.append(line);
    }

    String input = inputBuilder.toString();
    String expected = expectedBuilder.toString();

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    long startTime = System.currentTimeMillis();

    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.WINDOWS))
        .toStream(outputStream);

    long executionTime = System.currentTimeMillis() - startTime;

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(expected, result);

    // Verify performance (should complete within reasonable time)
    assertTrue(
        executionTime < 5000, "Large input processing took too long: " + executionTime + "ms");
  }

  @Test
  @DisplayName("Integration with empty stream")
  void testEmptyStreamIntegration() throws IOException {
    // Given
    ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // When
    Streams.from(inputStream)
        .addCommand(new LineEndingNormalizeCommand(LineEndingType.WINDOWS))
        .toStream(outputStream);

    // Then
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals("", result);
  }
}
