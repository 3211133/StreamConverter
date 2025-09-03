package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Simplified tree path class for hierarchical path matching.
 *
 * <p>This class provides simple path matching against List&lt;String&gt; hierarchical paths. It
 * supports both JSON-style ("$.user.name") and XML-style ("user/name") path formats.
 */
public class TreePath extends AbstractPath<List<String>> {

  // Internal hierarchical representation as simple string list
  private final List<String> pathSegments;
  private final String originalPath;

  // === Constructors ===

  /**
   * Creates TreePath from path string (auto-detects JSON or XML format)
   *
   * @param pathExpression Path expression (e.g., "$.user.name" or "user/name")
   */
  public TreePath(String pathExpression) {
    super(pathExpression);
    this.originalPath = pathExpression;
    this.pathSegments = parsePathToSegments(pathExpression);
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

    // Simple exact match comparison
    return pathSegments.equals(currentPath);
  }

  // === Internal Implementation ===

  private List<String> parsePathToSegments(String pathExpression) {
    String trimmed = pathExpression.trim();

    // Handle JSON format (starts with "$.")
    if (trimmed.startsWith("$.")) {
      String pathWithoutRoot = trimmed.substring(2);
      if (pathWithoutRoot.isEmpty()) {
        return new ArrayList<>();
      }
      return Arrays.asList(pathWithoutRoot.split("\\."));
    }

    // Handle XML format (simple path separated by "/")
    if (trimmed.contains("/")) {
      String normalizedPath = trimmed.replaceAll("^/+", "").replaceAll("/+$", "");
      if (normalizedPath.isEmpty()) {
        return new ArrayList<>();
      }
      // Split and filter out empty segments caused by multiple consecutive slashes
      return Arrays.stream(normalizedPath.split("/"))
          .filter(segment -> !segment.isEmpty())
          .collect(Collectors.toList());
    }

    // Single segment
    return List.of(trimmed);
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
    return Objects.equals(pathSegments, treePath.pathSegments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pathSegments);
  }
}
