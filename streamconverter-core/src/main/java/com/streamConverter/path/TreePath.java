package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

  // XML element name pattern for validation
  private static final Pattern XML_ELEMENT_NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._-]*$");

  // === Constructors ===

  /**
   * Creates TreePath from path string (auto-detects JSON or XML format)
   *
   * @param pathExpression Path expression (e.g., "$.user.name" or "user/name")
   */
  public TreePath(String pathExpression) {
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

    // OR条件：いずれかのパスがマッチすればtrue
    for (List<String> pathSegments : pathSegmentsList) {
      if (pathSegments.equals(currentPath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * マッチするすべてのパスを取得（Don't Ask Tell準拠）
   *
   * @param currentPath 現在のパス階層
   * @return マッチしたパスセグメントのリスト
   */
  public List<List<String>> findMatchingPaths(List<String> currentPath) {
    List<List<String>> matchingPaths = new ArrayList<>();
    if (currentPath == null) {
      return matchingPaths;
    }

    for (List<String> pathSegments : pathSegmentsList) {
      if (pathSegments.equals(currentPath)) {
        matchingPaths.add(pathSegments);
      }
    }
    return matchingPaths;
  }

  // === Internal Implementation ===

  private List<String> parsePathToSegments(String pathExpression) {
    String trimmed = pathExpression.trim();

    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Path expression cannot be empty");
    }

    // Handle JSON format (starts with "$")
    if (trimmed.startsWith("$")) {
      return parseJsonPathToSegments(trimmed);
    }

    // Handle XML format (contains "/" or validate as XML element name)
    if (trimmed.contains("/")) {
      return parseXmlPathToSegments(trimmed);
    }

    // Validate as single XML element name
    if (XML_ELEMENT_NAME_PATTERN.matcher(trimmed).matches()) {
      return List.of(trimmed);
    }

    // Default: treat as single segment (for backward compatibility)
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
    String normalizedPath = xmlPath.replaceAll("^/+", "").replaceAll("/+$", "");
    if (normalizedPath.isEmpty()) {
      return new ArrayList<>();
    }

    List<String> segments =
        Arrays.stream(normalizedPath.split("/"))
            .filter(segment -> !segment.isEmpty())
            .collect(Collectors.toList());

    // Validate XML element names
    for (String segment : segments) {
      if (!XML_ELEMENT_NAME_PATTERN.matcher(segment).matches()) {
        throw new IllegalArgumentException("Invalid XML element name: " + segment);
      }
    }

    return segments;
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
