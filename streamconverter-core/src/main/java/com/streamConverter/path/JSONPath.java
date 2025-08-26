package com.streamConverter.path;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Type-safe representation of JSONPath expressions.
 *
 * <p>This class encapsulates JSONPath expressions used for navigating JSON documents. It validates
 * the path syntax at construction time and provides type safety for JSON navigation operations.
 *
 * <p>Supported JSONPath patterns: - Simple property access: "$.propertyName" - Nested property
 * access: "$.level1.level2" - Array access: "$.users[0]" - Wildcard array access: "$.users[*]" -
 * Root reference: "$"
 *
 * <p>Note: This is a simplified JSONPath implementation focused on the most common use cases in
 * StreamConverter.
 */
public class JSONPath implements IPath {

  private static final String TYPE = "JSONPath";

  // Simplified and secure regex pattern to avoid polynomial regex issues
  // This pattern avoids nested quantifiers and complex alternations that can cause ReDoS
  private static final Pattern VALID_JSONPATH_PATTERN =
      Pattern.compile("^\\$(?:\\[(?:\\d+|\\*)\\])?(?:\\.\\w+(?:\\[(?:\\d+|\\*)\\])?)*$");

  // Identifier pattern for validation
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

  private final String path;

  /**
   * Creates a new JSONPath instance.
   *
   * @param path the JSONPath expression (e.g., "$.userName", "$.users[*].name")
   * @throws IllegalArgumentException if the path is null, empty, or has invalid syntax
   */
  public JSONPath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("JSONPath cannot be null");
    }
    if (path.trim().isEmpty()) {
      throw new IllegalArgumentException("JSONPath cannot be empty");
    }

    // Normalize path for backward compatibility
    String normalizedPath = normalizeJsonPath(path.trim());
    this.path = normalizedPath;
    validate();
  }

  /**
   * Factory method for creating a JSONPath for root access.
   *
   * @return a JSONPath representing root access ("$")
   */
  public static JSONPath root() {
    return new JSONPath("$");
  }

  /**
   * Factory method for creating a JSONPath for simple property access.
   *
   * @param propertyName the property name
   * @return a JSONPath for the property (e.g., "$.propertyName")
   * @throws IllegalArgumentException if propertyName is invalid
   */
  public static JSONPath property(String propertyName) {
    if (propertyName == null || propertyName.trim().isEmpty()) {
      throw new IllegalArgumentException("Property name cannot be null or empty");
    }
    if (!isValidIdentifier(propertyName.trim())) {
      throw new IllegalArgumentException("Invalid property name: " + propertyName);
    }
    return new JSONPath("$." + propertyName.trim());
  }

  @Override
  public void validate() {
    if (!VALID_JSONPATH_PATTERN.matcher(path).matches()) {
      throw new IllegalArgumentException("Invalid JSONPath syntax: " + path);
    }

    // Additional validation for specific patterns
    if (path.contains("..")) {
      throw new IllegalArgumentException("Empty property segments not allowed: " + path);
    }
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isEquivalentTo(IPath other) {
    if (!(other instanceof JSONPath)) {
      return false;
    }
    return Objects.equals(this.path, ((JSONPath) other).path);
  }

  /**
   * Checks if this is a root path ("$").
   *
   * @return true if this represents root access
   */
  public boolean isRoot() {
    return "$".equals(path);
  }

  /**
   * Checks if this path involves array access.
   *
   * @return true if the path contains array notation
   */
  public boolean hasArrayAccess() {
    return path.contains("[");
  }

  /**
   * Checks if this path uses wildcard array access.
   *
   * @return true if the path contains wildcard array notation
   */
  public boolean hasWildcardAccess() {
    return path.contains("[*]");
  }

  /**
   * Extracts the simple property name if this is a simple property access.
   *
   * @return the property name, or null if not a simple property access
   */
  public String getSimpleProperty() {
    if (path.startsWith("$.") && !path.contains("[") && path.lastIndexOf('.') == 1) {
      return path.substring(2);
    }
    return null;
  }

  private static boolean isValidIdentifier(String identifier) {
    return IDENTIFIER_PATTERN.matcher(identifier).matches();
  }

  /**
   * Normalizes a path for backward compatibility. Converts bare property names to proper JSONPath
   * syntax.
   */
  private String normalizeJsonPath(String path) {
    // If already starts with $, assume it's correct
    if (path.startsWith("$")) {
      return path;
    }

    // If it's a simple identifier, convert to $.identifier
    if (isValidIdentifier(path)) {
      return "$." + path;
    }

    // If it contains array notation but no $, assume it needs $
    if (path.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\[.*\\].*")) {
      return "$." + path;
    }

    // Return as-is for other cases (will be validated later)
    return path;
  }

  @Override
  public String toString() {
    return String.format("JSONPath('%s')", path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    JSONPath jsonPath = (JSONPath) obj;
    return Objects.equals(path, jsonPath.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }
}
