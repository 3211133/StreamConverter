package com.streamconverter.path;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a hierarchical path for tree-like data structures (JSON, XML).
 *
 * <p>This class handles parsing and matching of path expressions in both JSON-style ("$.user.name")
 * and XML-style ("user/name") formats. It converts path expressions into hierarchical segments for
 * efficient matching during data processing.
 */
public class TreePath implements ITreeMatcher {

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
    if (xmlPath == null || xmlPath.isBlank()) {
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
    if (jsonPath == null || jsonPath.isBlank()) {
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
  @Override
  public boolean matches(List<String> currentPath) {
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

  /**
   * 2つのTreePathが等しいかどうかをセグメントリストで比較する。
   *
   * <p>注意: 元のパス文字列（{@link #toString()} の値）が異なっていても、セグメントに展開した結果が同じであれば等しいとみなす。 例えば JSONスタイルの {@code
   * "$.user.name"} と XMLスタイルの {@code "user/name"} がパース後に同じセグメントになる場合、 等しいと判定される。
   *
   * @param obj 比較対象のオブジェクト
   * @return セグメントリストが等しい場合true
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    TreePath treePath = (TreePath) obj;
    return segments.equals(treePath.segments);
  }

  /**
   * セグメントリストに基づくハッシュコードを返す。
   *
   * @return ハッシュコード
   */
  @Override
  public int hashCode() {
    return segments.hashCode();
  }

  // === Internal Implementation ===

  private static List<String> parseJsonPathToSegments(String jsonPath) {
    if ("$".equals(jsonPath)) {
      return new ArrayList<>();
    }

    String rest;
    if (jsonPath.startsWith("$.")) {
      rest = jsonPath.substring(2);
    } else if (jsonPath.startsWith("$")) {
      rest = jsonPath.substring(1);
    } else {
      throw new IllegalArgumentException("Invalid JSON path format: " + jsonPath);
    }

    return splitStrippingBrackets(rest);
  }

  /**
   * Splits a JSON path fragment on dots, skipping bracket sections (e.g. {@code [*]}, {@code [0]}).
   * Uses character-by-character scanning to avoid regex backtracking.
   *
   * @throws IllegalArgumentException if an unclosed {@code [} is found
   */
  @SuppressWarnings("PMD.CyclomaticComplexity")
  private static List<String> splitStrippingBrackets(String path) {
    List<String> result = new ArrayList<>();
    StringBuilder segment = new StringBuilder();
    int bracketStart = -1;
    for (int i = 0; i < path.length(); i++) {
      char c = path.charAt(i);
      if (c == '[') {
        bracketStart = i;
      } else if (c == ']') {
        bracketStart = -1;
      } else if (bracketStart < 0) {
        if (c == '.' && !segment.isEmpty()) {
          result.add(segment.toString());
          segment.setLength(0);
        } else if (c != '.') {
          segment.append(c);
        }
      }
    }
    if (bracketStart >= 0) {
      throw new IllegalArgumentException("Unclosed '[' in JSON path: \"" + path + "\"");
    }
    if (!segment.isEmpty()) {
      result.add(segment.toString());
    }
    return result;
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
