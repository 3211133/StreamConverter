package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Simplified tree path class for hierarchical path matching.
 *
 * <p>This class provides simple path matching against List&lt;String&gt; hierarchical paths. It
 * supports both JSON-style ("$.user.name") and XML-style ("user/name") path formats.
 */
public class TreePath extends AbstractPath<List<String>> {

  // Internal hierarchical representation as simple string list
  private final List<List<String>> pathSegmentsList;
  private final String originalPath;

  // === Constructors ===

  /**
   * Private constructor for internal use by factory methods
   *
   * @param pathExpression Path expression
   */
  private TreePath(String pathExpression) {
    super(pathExpression);
    this.originalPath = pathExpression;
    this.pathSegmentsList = Collections.singletonList(parsePathToSegments(pathExpression));
  }

  /**
   * Creates TreePath from multiple paths (OR condition)
   *
   * @param pathList List of path expressions
   */
  public TreePath(List<String> pathList) {
    super(String.join(",", pathList));
    if (pathList == null || pathList.isEmpty()) {
      throw new IllegalArgumentException("Path list cannot be null or empty");
    }
    this.originalPath = String.join(",", pathList);
    List<List<String>> temp = new ArrayList<>();
    for (String path : pathList) {
      temp.add(parsePathToSegments(path));
    }
    this.pathSegmentsList = Collections.unmodifiableList(temp);
  }

  /**
   * Creates TreePath from JSON format path
   *
   * @param jsonPath JSON format path (e.g., "$.user.name", "$.items.title")
   * @return TreePath instance
   */
  public static TreePath fromJsonPath(String jsonPath) {
    return new TreePath(jsonPath);
  }

  /**
   * Creates TreePath from XML format path
   *
   * @param xmlPath XML format path (e.g., "user/name", "items/title")
   * @return TreePath instance
   */
  public static TreePath fromXmlPath(String xmlPath) {
    return new TreePath(xmlPath);
  }

  // === AbstractPath Implementation ===

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (rawPath == null || rawPath.trim().isEmpty()) {
      throw new IllegalArgumentException("TreePath cannot be null or empty");
    }
  }

  @Override
  public boolean matches(List<String> currentPath) {
    if (currentPath == null) {
      return false;
    }

    // OR condition: return true if any path matches
    for (List<String> pathSegments : pathSegmentsList) {
      if (pathSegments.equals(currentPath)) {
        return true;
      }
    }
    return false;
  }

  // === Internal Implementation ===

  private List<String> parsePathToSegments(String pathExpression) {
    String trimmed = pathExpression.trim();

    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Path expression cannot be empty");
    }

    // Parse as JSON-style path if starts with "$"
    if (trimmed.startsWith("$")) {
      return parseJsonPathToSegments(trimmed);
    }

    // Parse as slash-separated path if contains "/"
    if (trimmed.contains("/")) {
      return parseXmlPathToSegments(trimmed);
    }

    // Single segment path
    return List.of(trimmed);
  }

  private List<String> parseJsonPathToSegments(String jsonPath) {
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

  private List<String> parseComplexJsonPath(String jsonPath) {
    // For paths with array syntax like $[*].name or $.users[0].name
    // Keep them as single segments to maintain compatibility
    // The actual array handling is done in JsonFilterCommand
    return List.of(jsonPath);
  }

  private List<String> parseXmlPathToSegments(String xmlPath) {
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
  private String removeLeadingTrailingSlashes(String path) {
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

  @Override
  public String toString() {
    return originalPath;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    TreePath treePath = (TreePath) obj;
    return Objects.equals(pathSegmentsList, treePath.pathSegmentsList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pathSegmentsList);
  }
}
