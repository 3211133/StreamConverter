package com.streamConverter.controller;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OutputType enum.
 *
 * <p>This test class verifies the functionality of the OutputType enum including string conversion,
 * validation, and type safety features.
 */
@DisplayName("OutputType Enum Tests")
class OutputTypeTest {

  @Test
  @DisplayName("Should convert string to OutputType correctly")
  void testFromString() {
    assertEquals(OutputType.CSV_COLUMN, OutputType.fromString("CSV_COLUMN"));
    assertEquals(OutputType.JSON_PROPERTY, OutputType.fromString("JSON_PROPERTY"));
    assertEquals(
        OutputType.VALIDATED_JSON_PROPERTY, OutputType.fromString("VALIDATED_JSON_PROPERTY"));
    assertEquals(OutputType.JSON_FORMATTED, OutputType.fromString("JSON_FORMATTED"));
    assertEquals(OutputType.PROCESSED_DATA, OutputType.fromString("PROCESSED_DATA"));
    assertEquals(OutputType.CSV, OutputType.fromString("CSV"));
    assertEquals(OutputType.TRANSFORMED_DATA, OutputType.fromString("TRANSFORMED_DATA"));
  }

  @Test
  @DisplayName("Should return null for invalid string")
  void testFromStringInvalid() {
    assertNull(OutputType.fromString("INVALID_TYPE"));
    assertNull(OutputType.fromString(""));
    assertNull(OutputType.fromString(null));
    assertNull(OutputType.fromString("csv_column")); // case sensitive
  }

  @Test
  @DisplayName("Should validate OutputType strings correctly")
  void testIsValid() {
    assertTrue(OutputType.isValid("CSV_COLUMN"));
    assertTrue(OutputType.isValid("JSON_PROPERTY"));
    assertTrue(OutputType.isValid("VALIDATED_JSON_PROPERTY"));
    assertTrue(OutputType.isValid("JSON_FORMATTED"));
    assertTrue(OutputType.isValid("PROCESSED_DATA"));
    assertTrue(OutputType.isValid("CSV"));
    assertTrue(OutputType.isValid("TRANSFORMED_DATA"));

    assertFalse(OutputType.isValid("INVALID_TYPE"));
    assertFalse(OutputType.isValid(""));
    assertFalse(OutputType.isValid(null));
    assertFalse(OutputType.isValid("csv_column")); // case sensitive
  }

  @Test
  @DisplayName("Should return correct string value")
  void testGetValue() {
    assertEquals("CSV_COLUMN", OutputType.CSV_COLUMN.getValue());
    assertEquals("JSON_PROPERTY", OutputType.JSON_PROPERTY.getValue());
    assertEquals("VALIDATED_JSON_PROPERTY", OutputType.VALIDATED_JSON_PROPERTY.getValue());
    assertEquals("JSON_FORMATTED", OutputType.JSON_FORMATTED.getValue());
    assertEquals("PROCESSED_DATA", OutputType.PROCESSED_DATA.getValue());
    assertEquals("CSV", OutputType.CSV.getValue());
    assertEquals("TRANSFORMED_DATA", OutputType.TRANSFORMED_DATA.getValue());
  }

  @Test
  @DisplayName("Should return correct description")
  void testGetDescription() {
    assertEquals("Extract specific CSV column data", OutputType.CSV_COLUMN.getDescription());
    assertEquals("Extract JSON property using TreePath", OutputType.JSON_PROPERTY.getDescription());
    assertEquals(
        "Extract and validate JSON property", OutputType.VALIDATED_JSON_PROPERTY.getDescription());
    assertEquals("Format JSON for readability", OutputType.JSON_FORMATTED.getDescription());
    assertEquals(
        "Process data with additional transformations", OutputType.PROCESSED_DATA.getDescription());
    assertEquals("Pass-through CSV processing", OutputType.CSV.getDescription());
    assertEquals(
        "Transform data with multi-stage processing", OutputType.TRANSFORMED_DATA.getDescription());
  }

  @Test
  @DisplayName("Should return correct string representation")
  void testToString() {
    assertEquals("CSV_COLUMN", OutputType.CSV_COLUMN.toString());
    assertEquals("JSON_PROPERTY", OutputType.JSON_PROPERTY.toString());
    assertEquals("VALIDATED_JSON_PROPERTY", OutputType.VALIDATED_JSON_PROPERTY.toString());
    assertEquals("JSON_FORMATTED", OutputType.JSON_FORMATTED.toString());
    assertEquals("PROCESSED_DATA", OutputType.PROCESSED_DATA.toString());
    assertEquals("CSV", OutputType.CSV.toString());
    assertEquals("TRANSFORMED_DATA", OutputType.TRANSFORMED_DATA.toString());
  }

  @Test
  @DisplayName("Should have all expected enum values")
  void testEnumValues() {
    OutputType[] values = OutputType.values();
    assertEquals(7, values.length);

    // Ensure all expected values are present
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.CSV_COLUMN));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.JSON_PROPERTY));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.VALIDATED_JSON_PROPERTY));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.JSON_FORMATTED));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.PROCESSED_DATA));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.CSV));
    assertTrue(java.util.Arrays.asList(values).contains(OutputType.TRANSFORMED_DATA));
  }
}
