package com.streamConverter.path;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Unified path class for tree structure data.
 *
 * <p>This class provides unified handling of JSON and XML path expressions with bidirectional
 * conversion capabilities. Internally maintains hierarchical segment representation and converts to
 * specific formats as needed.
 *
 * <p>Supported formats: - JSON format: "$.user.profile.name", "$.items[0].title" - XML format:
 * "user/profile/name", "items/item[0]/title"
 *
 * <p>Internal representation example: - Segments: ["user", "profile", "name"] - Array index
 * information: profile→none, name→none
 */
public class TreePath extends AbstractPath<Object> {

  private static final String TYPE = "TreePath";

  // Path format enumeration
  public enum PathFormat {
    JSON, // JSONPath format ($.prop.nested)
    XML // XPath format (prop/nested)
  }

  // Internal hierarchical representation
  private final List<PathSegment> segments;
  private final PathFormat sourceFormat;
  private final String originalPath;

  /** Class representing a path segment */
  public static class PathSegment {
    private final String name;
    private final Integer arrayIndex; // Array index for array access
    private final boolean isWildcard; // true for [*] notation

    public PathSegment(String name) {
      this(name, null, false);
    }

    public PathSegment(String name, Integer arrayIndex, boolean isWildcard) {
      this.name = name;
      this.arrayIndex = arrayIndex;
      this.isWildcard = isWildcard;
    }

    public String getName() {
      return name;
    }

    public Optional<Integer> getArrayIndex() {
      return Optional.ofNullable(arrayIndex);
    }

    public boolean isWildcard() {
      return isWildcard;
    }

    public boolean hasArrayAccess() {
      return arrayIndex != null || isWildcard;
    }

    @Override
    public String toString() {
      if (isWildcard) {
        return name + "[*]";
      } else if (arrayIndex != null) {
        return name + "[" + arrayIndex + "]";
      }
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;
      PathSegment segment = (PathSegment) obj;
      return Objects.equals(name, segment.name)
          && Objects.equals(arrayIndex, segment.arrayIndex)
          && isWildcard == segment.isWildcard;
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, arrayIndex, isWildcard);
    }
  }

  // === Constructors ===

  /**
   * Creates TreePath from JSON format path
   *
   * @param jsonPath JSON format path (e.g., "$.user.name", "$.items[0].title")
   * @return TreePath instance
   */
  public static TreePath fromJsonPath(String jsonPath) {
    return new TreePath(jsonPath, PathFormat.JSON);
  }

  /**
   * Creates TreePath from XML format path
   *
   * @param xmlPath XML format path (e.g., "user/name", "items/item[0]/title")
   * @return TreePath instance
   */
  public static TreePath fromXmlPath(String xmlPath) {
    return new TreePath(xmlPath, PathFormat.XML);
  }

  // Note: fromSegments method removed - use fromJsonPath or fromXmlPath instead

  private TreePath(String pathExpression, PathFormat format) {
    super(pathExpression);
    this.originalPath = pathExpression;
    this.sourceFormat = format;
    // parsePathToSegments is static method, can be called after this.path is initialized
    this.segments = parsePathToSegments(pathExpression, format);
    // Execute segment-specific validation
    validateSegments();
  }

  private static String validateAndNormalizeTreePath(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }

    String trimmedPath = rawPath.trim();
    // Empty string is allowed (valid as XML root path)
    // For JSON: "$", for XML: "" represents root path
    return trimmedPath;
  }

  // === AbstractPath Implementation ===

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
  }

  @Override
  public boolean matches(Object context) {
    if (context == null) {
      return false;
    }

    // Basic matching based on context type
    if (context instanceof com.fasterxml.jackson.databind.JsonNode) {
      return matchesJsonContext((com.fasterxml.jackson.databind.JsonNode) context);
    } else if (context instanceof java.util.List) {
      @SuppressWarnings("unchecked")
      java.util.List<String> xmlPath = (java.util.List<String>) context;
      return matchesXmlContext(xmlPath);
    }

    return false;
  }

  private void validateSegments() {
    if (segments == null) {
      throw new IllegalStateException("Segments not initialized");
    }

    // Empty segments (root path) are allowed
    if (segments.isEmpty()) {
      return;
    }

    // Validate segment names
    for (PathSegment segment : segments) {
      if (isNullOrEmpty(segment.getName())) {
        throw new IllegalArgumentException("Path segment name cannot be null or empty");
      }

      // Check if valid as XML element name
      if (!isValidElementName(segment.getName())) {
        throw new IllegalArgumentException("Invalid element name: " + segment.getName());
      }
    }
  }

  // Removed old doMatches method following simplified design

  // Removed complex extraction methods following simplified design

  // Note: Path conversion features removed for simplification
  // Only core matching functionality is retained

  // === 内部実装メソッド ===

  private static List<PathSegment> parsePathToSegments(String pathExpression, PathFormat format) {
    switch (format) {
      case JSON:
        return parseJsonPathToSegments(pathExpression);
      case XML:
        return parseXmlPathToSegments(pathExpression);
      default:
        throw new IllegalArgumentException("Unsupported path format: " + format);
    }
  }

  private static List<PathSegment> parseJsonPathToSegments(String jsonPath) {
    List<PathSegment> result = new ArrayList<>();

    // ルートパス "$" の処理
    if ("$".equals(jsonPath)) {
      return result; // 空のリストでルートを表現
    }

    // "$." で始まることを確認
    if (!jsonPath.startsWith("$.")) {
      throw new IllegalArgumentException("JSON path must start with '$.' : " + jsonPath);
    }

    // "$." を除去してパースを開始
    String pathWithoutRoot = jsonPath.substring(2);
    if (pathWithoutRoot.isEmpty()) {
      return result;
    }

    // 手動パース（正規表現を避けてセキュリティ対策）
    String[] parts = pathWithoutRoot.split("\\.");
    for (String part : parts) {
      result.add(parseSegmentPart(part));
    }

    return result;
  }

  private static List<PathSegment> parseXmlPathToSegments(String xmlPath) {
    List<PathSegment> result = new ArrayList<>();

    // 先頭・末尾の "/" を正規化
    String normalizedPath = xmlPath.replaceAll("^/+", "").replaceAll("/+$", "");
    if (normalizedPath.isEmpty()) {
      return result;
    }

    String[] parts = normalizedPath.split("/");
    for (String part : parts) {
      if (!part.isEmpty()) {
        result.add(parseSegmentPart(part));
      }
    }

    return result;
  }

  private static PathSegment parseSegmentPart(String part) {
    // 配列アクセス [n] または [*] のパース
    int bracketStart = part.indexOf('[');
    if (bracketStart == -1) {
      // 通常のセグメント
      return new PathSegment(part);
    }

    int bracketEnd = part.lastIndexOf(']');
    if (bracketEnd == -1 || bracketEnd <= bracketStart) {
      throw new IllegalArgumentException("Invalid array access syntax: " + part);
    }

    String elementName = part.substring(0, bracketStart);
    String indexPart = part.substring(bracketStart + 1, bracketEnd);

    if ("*".equals(indexPart)) {
      return new PathSegment(elementName, null, true);
    } else {
      try {
        int index = Integer.parseInt(indexPart);
        return new PathSegment(elementName, index, false);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid array index: " + indexPart);
      }
    }
  }

  // Note: Path conversion helper methods removed for simplification

  private boolean matchesJsonContext(JsonNode context) {
    // JSON階層パスマッチング（JsonNavigateCommandと同じロジック）
    return context != null && hasTargetProperty(context);
  }

  private boolean matchesXmlContext(List<String> xmlPath) {
    // XML階層パスマッチング（XmlNavigateCommandと同じロジック）
    if (xmlPath.size() != segments.size()) {
      return false;
    }

    for (int i = 0; i < segments.size(); i++) {
      if (!segments.get(i).getName().equals(xmlPath.get(i))) {
        return false;
      }
    }
    return true;
  }

  private boolean hasTargetProperty(JsonNode node) {
    if (segments.size() == 1 && !segments.get(0).hasArrayAccess()) {
      return node.has(segments.get(0).getName());
    }

    // より複雑なパスの場合は簡易実装
    return navigateJsonNode(node) != null;
  }

  // Note: Data extraction methods removed - TreePath only handles path matching

  private JsonNode navigateJsonNode(JsonNode rootNode) {
    JsonNode currentNode = rootNode;

    for (PathSegment segment : segments) {
      if (currentNode == null || currentNode.isMissingNode()) {
        return null;
      }

      currentNode = currentNode.get(segment.getName());

      if (segment.hasArrayAccess() && currentNode != null && currentNode.isArray()) {
        if (segment.isWildcard()) {
          // ワイルドカードの場合は最初の要素を返す（簡易実装）
          currentNode = currentNode.get(0);
        } else if (segment.getArrayIndex().isPresent()) {
          currentNode = currentNode.get(segment.getArrayIndex().get());
        }
      }
    }

    return currentNode;
  }

  private static boolean isValidElementName(String name) {
    if (isNullOrEmpty(name)) {
      return false;
    }

    // XML要素名の基本パターン（簡易版）
    Pattern xmlElementPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._-]*$");
    return xmlElementPattern.matcher(name).matches();
  }

  // Note: Dynamic path construction methods removed for simplification
  // Only core factory methods and matching functionality are retained

  @Override
  public String toString() {
    return originalPath;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    TreePath treePath = (TreePath) obj;
    return Objects.equals(segments, treePath.segments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segments);
  }
}
