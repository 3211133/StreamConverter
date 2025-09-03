package com.streamConverter.controller;

import java.util.Optional;

/**
 * Enumeration of supported output data types for stream controllers.
 *
 * <p>This enum defines the standard output types that can be produced by stream controllers,
 * providing type safety and consistency across the controller factory and builder implementations.
 *
 * <p>Each output type represents a specific format or processing result that can be achieved
 * through the controller framework. The enum values correspond to the capabilities of different
 * controller implementations.
 *
 * <p>Usage examples:
 *
 * <pre>
 * // Using enum for type-safe output specification
 * OutputType outputType = OutputType.CSV_COLUMN;
 * String outputTypeValue = outputType.getValue();
 * </pre>
 *
 * @author StreamConverter Team
 * @version 1.0
 * @since 1.0
 */
public enum OutputType {

  /** Extract specific CSV column data */
  CSV_COLUMN("CSV_COLUMN", "Extract specific CSV column data"),

  /** Process CSV data with additional transformations */
  PROCESSED_DATA("PROCESSED_DATA", "Process data with additional transformations"),

  /** Pass-through CSV processing */
  CSV("CSV", "Pass-through CSV processing"),

  /** Extract JSON property using TreePath */
  JSON_PROPERTY("JSON_PROPERTY", "Extract JSON property using TreePath"),

  /** Extract and validate JSON property */
  VALIDATED_JSON_PROPERTY("VALIDATED_JSON_PROPERTY", "Extract and validate JSON property"),

  /** Format JSON for readability */
  JSON_FORMATTED("JSON_FORMATTED", "Format JSON for readability"),

  /** Transform data with multi-stage processing */
  TRANSFORMED_DATA("TRANSFORMED_DATA", "Transform data with multi-stage processing");

  /** The string representation of the output type */
  private final String value;

  /** Human-readable description of the output type */
  private final String description;

  /**
   * Constructs an OutputType with the specified value and description.
   *
   * @param value the string representation of the output type
   * @param description the human-readable description
   */
  OutputType(String value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * Gets the string representation of this output type.
   *
   * @return the string value
   */
  public String getValue() {
    return value;
  }

  /**
   * Gets the human-readable description of this output type.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Returns the string representation of this output type.
   *
   * @return the string value
   */
  @Override
  public String toString() {
    return value;
  }

  /**
   * Converts a string value to the corresponding OutputType.
   *
   * @param value the string value to convert
   * @return Optional containing the corresponding OutputType, or empty if not found
   */
  public static Optional<OutputType> findByValue(String value) {
    if (value == null) {
      return Optional.empty();
    }

    for (OutputType type : values()) {
      if (type.value.equals(value)) {
        return Optional.of(type);
      }
    }

    return Optional.empty();
  }

  /**
   * Converts a string value to the corresponding OutputType.
   *
   * @param value the string value to convert
   * @return the corresponding OutputType, or null if not found
   * @deprecated Use {@link #findByValue(String)} instead to avoid null returns
   */
  @Deprecated
  public static OutputType fromString(String value) {
    return findByValue(value).orElse(null);
  }

  /**
   * Checks if a string value represents a valid OutputType.
   *
   * @param value the string value to check
   * @return true if the value is valid, false otherwise
   */
  public static boolean isValid(String value) {
    return fromString(value) != null;
  }
}
