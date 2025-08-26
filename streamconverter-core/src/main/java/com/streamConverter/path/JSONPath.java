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

  // No regex validation to completely avoid ReDoS vulnerabilities
  // Use manual parsing for secure validation instead

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
    validateJsonPathSafely(path);
  }

  /**
   * Manually validates JSONPath without regex to avoid ReDoS vulnerabilities. Validates patterns
   * like: $, $.prop, $.prop[0], $.prop[*], $.prop.nested[0]
   */
  private static void validateJsonPathSafely(String path) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException("JSONPath cannot be null or empty");
    }

    // Must start with $
    if (!path.startsWith("$")) {
      throw new IllegalArgumentException("JSONPath must start with '$': " + path);
    }

    // Just $ is valid (root)
    if (path.equals("$")) {
      return;
    }

    // Parse character by character to avoid regex vulnerabilities
    int i = 1; // Start after '$'

    while (i < path.length()) {
      char c = path.charAt(i);

      if (c == '.') {
        // Dot must be followed by property name
        i++; // Move past '.'
        if (i >= path.length()) {
          throw new IllegalArgumentException("JSONPath cannot end with '.': " + path);
        }

        // Read property name
        int propStart = i;
        while (i < path.length()) {
          char propChar = path.charAt(i);
          if (Character.isLetterOrDigit(propChar) || propChar == '_') {
            i++;
          } else {
            break; // Stop at non-property character like '[' or '.'
          }
        }

        if (i == propStart) {
          throw new IllegalArgumentException(
              "Empty property name after '.' at position " + propStart + ": " + path);
        }

        String propName = path.substring(propStart, i);
        if (!isValidPropertyName(propName)) {
          throw new IllegalArgumentException(
              "Invalid property name '" + propName + "' at position " + propStart + ": " + path);
        }

        continue;
      }

      if (c == '[') {
        // Parse array accessor [123] or [*]
        int endBracket = path.indexOf(']', i);
        if (endBracket == -1) {
          throw new IllegalArgumentException(
              "Missing closing bracket ']' after position " + i + ": " + path);
        }

        String arrayContent = path.substring(i + 1, endBracket);
        if (arrayContent.equals("*")) {
          // Wildcard is valid
        } else if (isNumericIndex(arrayContent)) {
          // Numeric index is valid
        } else {
          throw new IllegalArgumentException(
              "Invalid array index '" + arrayContent + "' at position " + i + ": " + path);
        }

        i = endBracket + 1;
        continue;
      }

      // If we get here, we have an unexpected character
      throw new IllegalArgumentException(
          "Unexpected character '" + c + "' at position " + i + ": " + path);
    }
  }

  private static boolean isValidPropertyName(String propName) {
    if (propName == null || propName.isEmpty()) {
      return false;
    }

    char first = propName.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      return false;
    }

    for (int i = 1; i < propName.length(); i++) {
      char c = propName.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        return false;
      }
    }

    return true;
  }

  private static boolean isNumericIndex(String index) {
    if (index == null || index.isEmpty()) {
      return false;
    }

    for (int i = 0; i < index.length(); i++) {
      if (!Character.isDigit(index.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Checks if the path has array notation like property[index] or property[*] without using regex
   * to avoid ReDoS vulnerabilities.
   */
  private static boolean hasArrayNotation(String path) {
    if (path == null || path.isEmpty()) {
      return false;
    }

    // Look for pattern: starts with valid identifier, then has [...]
    int bracketPos = path.indexOf('[');
    if (bracketPos == -1) {
      return false;
    }

    // Check if there's a valid identifier before the bracket
    String beforeBracket = path.substring(0, bracketPos);
    if (!isValidIdentifier(beforeBracket)) {
      return false;
    }

    // Check if there's a closing bracket
    int closeBracketPos = path.indexOf(']', bracketPos);
    return closeBracketPos != -1;
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
    if (hasArrayNotation(path)) {
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
