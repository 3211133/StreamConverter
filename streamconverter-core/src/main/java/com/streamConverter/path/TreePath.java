package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a hierarchical path for tree-like data structures (JSON, XML).
 *
 * <p>This class handles parsing and matching of path expressions in both JSON-style ("$.user.name")
 * and XML-style ("user/name") formats. It converts path expressions into hierarchical segments for
 * efficient matching during data processing.
 */
public class TreePath implements IPath<List<String>> {

  private final List<String> segments;
  private final String originalPath;

  // Private constructor for factory methods
  private TreePath(String originalPath, List<String> segments) {
    this.originalPath = originalPath;
    this.segments = segments;
  }

  /**
   * Creates a TreePath from an XML path expression.
   *
   * @param xmlPath the XML path expression (e.g., "user/name")
   * @return TreePath instance
   * @throws IllegalArgumentException if xmlPath is null or invalid
   */
  public static TreePath fromXml(String xmlPath) {
    if (xmlPath == null || xmlPath.trim().isEmpty()) {
      throw new IllegalArgumentException("XML path cannot be null or empty");
    }
    String trimmedPath = xmlPath.trim();
    List<String> segments = parseXmlPathToSegments(trimmedPath);
    return new TreePath(trimmedPath, segments);
  }

  /**
   * Creates a TreePath from a JSON path expression.
   *
   * @param jsonPath the JSON path expression (e.g., "$.user.name")
   * @return TreePath instance
   * @throws IllegalArgumentException if jsonPath is null or invalid
   */
  public static TreePath fromJson(String jsonPath) {
    if (jsonPath == null || jsonPath.trim().isEmpty()) {
      throw new IllegalArgumentException("JSON path cannot be null or empty");
    }
    String trimmedPath = jsonPath.trim();
    List<String> segments = parseJsonPathToSegments(trimmedPath);
    return new TreePath(trimmedPath, segments);
  }

  /**
   * Checks if current path matches the target path segments
   *
   * @param currentPath current path segments to match against
   * @return true if paths match exactly
   */
  public boolean matches(List<String> currentPath) {
    if (currentPath == null) {
      return false;
    }
    return segments.equals(currentPath);
  }

  /**
   * Returns the original path expression.
   *
   * @return the original path expression
   */
  @Override
  public String toString() {
    return originalPath;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    TreePath treePath = (TreePath) obj;
    return segments.equals(treePath.segments);
  }

  @Override
  public int hashCode() {
    return segments.hashCode();
  }

  // === Internal Implementation ===

  private static List<String> parseJsonPathToSegments(String jsonPath) {
    // Handle root path
    if ("$".equals(jsonPath)) {
      return new ArrayList<>();
    }

    // Handle array syntax preservation for JsonFilterCommand compatibility
    if (jsonPath.contains("[")) {
      // For complex paths with arrays, preserve original parsing logic
      // This ensures JsonFilterCommand continues to work
      return parseComplexJsonPath(jsonPath);
    }

    // Simple property paths: $.property or $.nested.property
    if (jsonPath.startsWith("$.")) {
      String pathWithoutRoot = jsonPath.substring(2);
      if (pathWithoutRoot.isEmpty()) {
        return new ArrayList<>();
      }
      return Arrays.asList(pathWithoutRoot.split("\\."));
    }

    throw new IllegalArgumentException("Invalid JSON path format: " + jsonPath);
  }

  private static List<String> parseComplexJsonPath(String jsonPath) {
    // For paths with array syntax like $[*].name or $.users[0].name
    // Keep them as single segments to maintain compatibility
    // The actual array handling is done in JsonFilterCommand
    return List.of(jsonPath);
  }

  private static List<String> parseXmlPathToSegments(String xmlPath) {
    // Remove leading and trailing slashes without regex to avoid polynomial complexity
    String normalizedPath = removeLeadingTrailingSlashes(xmlPath);
    if (normalizedPath.isEmpty()) {
      return new ArrayList<>();
    }

    // Split by single slash and filter empty segments to handle multiple consecutive slashes
    List<String> segments = new ArrayList<>();
    int start = 0;
    for (int i = 0; i <= normalizedPath.length(); i++) {
      if (i == normalizedPath.length() || normalizedPath.charAt(i) == '/') {
        if (i > start) {
          segments.add(normalizedPath.substring(start, i));
        }
        start = i + 1;
      }
    }

    return segments;
  }

  /**
   * Removes leading and trailing slashes from path without regex
   *
   * @param path the input path
   * @return path with leading/trailing slashes removed
   */
  private static String removeLeadingTrailingSlashes(String path) {
    if (path == null || path.isEmpty()) {
      return "";
    }

    int start = 0;
    int end = path.length();

    // Remove leading slashes
    while (start < end && path.charAt(start) == '/') {
      start++;
    }

    // Remove trailing slashes
    while (end > start && path.charAt(end - 1) == '/') {
      end--;
    }

    return path.substring(start, end);
  }
}
