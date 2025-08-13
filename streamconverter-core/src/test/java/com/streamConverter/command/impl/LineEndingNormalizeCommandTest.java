package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
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
