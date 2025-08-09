package com.streamConverter.test;

import static com.streamConverter.test.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cross-platform compatibility tests for line ending handling */
@DisplayName("Cross-Platform Compatibility Tests")
class CrossPlatformTest {

  @Test
  @DisplayName("Line separator should be platform appropriate")
  void testLineSeparatorIsPlatformAppropriate() {
    String expectedLineSeparator = System.getProperty("line.separator");
    assertEquals(expectedLineSeparator, LINE_SEPARATOR);
  }

  @Test
  @DisplayName("Create test data should use correct line separators")
  void testCreateTestDataUsesCorrectLineSeparators() {
    String result = createTestData("line1", "line2", "line3");
    String expected = "line1" + LINE_SEPARATOR + "line2" + LINE_SEPARATOR + "line3";
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Line ending normalization should handle all formats")
  void testLineEndingNormalization() {
    // Windows style (CRLF)
    String windowsText = "line1\r\nline2\r\nline3";
    String normalized = normalizeLineEndings(windowsText);
    assertEquals("line1\nline2\nline3", normalized);

    // Old Mac style (CR only)
    String macText = "line1\rline2\rline3";
    normalized = normalizeLineEndings(macText);
    assertEquals("line1\nline2\nline3", normalized);

    // Unix style (LF only)
    String unixText = "line1\nline2\nline3";
    normalized = normalizeLineEndings(unixText);
    assertEquals("line1\nline2\nline3", normalized);

    // Mixed styles
    String mixedText = "line1\r\nline2\nline3\rline4";
    normalized = normalizeLineEndings(mixedText);
    assertEquals("line1\nline2\nline3\nline4", normalized);
  }

  @Test
  @DisplayName("Assert equals ignore line endings should work correctly")
  void testAssertEqualsIgnoreLineEndings() {
    String windowsStyle = "Hello\r\nWorld\r\nTest";
    String unixStyle = "Hello\nWorld\nTest";
    String macStyle = "Hello\rWorld\rTest";

    // These should all be considered equal
    assertDoesNotThrow(() -> assertEqualsIgnoreLineEndings(windowsStyle, unixStyle));
    assertDoesNotThrow(() -> assertEqualsIgnoreLineEndings(unixStyle, macStyle));
    assertDoesNotThrow(() -> assertEqualsIgnoreLineEndings(windowsStyle, macStyle));

    // Different content should fail
    String differentContent = "Hello\nWorld\nDifferent";
    assertThrows(
        AssertionError.class, () -> assertEqualsIgnoreLineEndings(windowsStyle, differentContent));
  }

  @Test
  @DisplayName("CSV data creation should handle platform line endings")
  void testCsvDataCreation() {
    String result = createCsvData("id,name", "1,John", "2,Jane");

    String expected = "id,name" + LINE_SEPARATOR + "1,John" + LINE_SEPARATOR + "2,Jane";

    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Null handling in line ending normalization")
  void testNullHandlingInNormalization() {
    assertNull(normalizeLineEndings(null));
  }
}
