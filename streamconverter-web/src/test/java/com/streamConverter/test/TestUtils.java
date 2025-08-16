package com.streamConverter.test;

import java.util.Arrays;

/** Test utilities for cross-platform testing */
public class TestUtils {

  /** Cross-platform line separator */
  public static final String LINE_SEPARATOR = System.lineSeparator();

  /**
   * Creates test data with platform-appropriate line separators
   *
   * @param lines Lines to join with platform line separator
   * @return Joined string with correct line separators
   */
  public static String createTestData(String... lines) {
    return String.join(LINE_SEPARATOR, lines);
  }

  /**
   * Creates CSV test data with headers and rows
   *
   * @param headers CSV headers
   * @param rows CSV data rows
   * @return Complete CSV string with platform line separators
   */
  public static String createCsvData(String headers, String... rows) {
    return createTestData(
        java.util.stream.Stream.concat(java.util.stream.Stream.of(headers), Arrays.stream(rows))
            .toArray(String[]::new));
  }

  /**
   * Normalizes line endings in expected vs actual string comparisons
   *
   * @param text Text to normalize
   * @return Text with normalized line endings
   */
  public static String normalizeLineEndings(String text) {
    if (text == null) return null;
    return text.replace("\r\n", "\n").replace("\r", "\n");
  }

  /**
   * Asserts equality ignoring line ending differences
   *
   * @param expected Expected string
   * @param actual Actual string
   * @param message Assertion message
   */
  public static void assertEqualsIgnoreLineEndings(String expected, String actual, String message) {
    String normalizedExpected = normalizeLineEndings(expected);
    String normalizedActual = normalizeLineEndings(actual);

    if (!normalizedExpected.equals(normalizedActual)) {
      throw new AssertionError(
          message
              + "\n"
              + "Expected (normalized): "
              + normalizedExpected
              + "\n"
              + "Actual (normalized): "
              + normalizedActual);
    }
  }

  /** Asserts equality ignoring line ending differences */
  public static void assertEqualsIgnoreLineEndings(String expected, String actual) {
    assertEqualsIgnoreLineEndings(
        expected, actual, "Strings should be equal ignoring line endings");
  }
}
