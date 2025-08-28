package com.streamConverter.path;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
public class JSONPath extends AbstractPath<JsonNode> {

  private static final String TYPE = "JSONPath";

  // No regex validation to completely avoid ReDoS vulnerabilities
  // Use manual parsing for secure validation instead

  private final List<String> segments;
  private final boolean isSimpleProperty;
  private final boolean isArrayAccess;
  private final boolean isWildcardAccess;

  /**
   * Creates a new JSONPath instance.
   *
   * @param path the JSONPath expression (e.g., "$.userName", "$.users[*].name")
   * @throws IllegalArgumentException if the path is null, empty, or has invalid syntax
   */
  public JSONPath(String path) {
    super(path, TYPE);
    this.segments = parseSegments(this.path);
    this.isSimpleProperty = detectSimpleProperty();
    this.isArrayAccess = detectArrayAccess();
    this.isWildcardAccess = detectWildcardAccess();
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

  @Override
  protected String validateAndNormalize(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("JSONPath cannot be null");
    }
    if (rawPath.trim().isEmpty()) {
      throw new IllegalArgumentException("JSONPath cannot be empty");
    }

    // Normalize path for backward compatibility
    String normalizedPath = normalizeJsonPath(rawPath.trim());
    return normalizedPath;
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

  // === JSONPath特化メソッド ===

  /**
   * 簡単なプロパティの場合、プロパティ名を抽出
   *
   * @return 簡単なプロパティアクセスの場合はプロパティ名、そうでなければEmpty
   */
  public Optional<String> extractSimpleProperty() {
    if (isSimpleProperty && segments.size() == 1) {
      return Optional.of(segments.get(0));
    }
    return Optional.empty();
  }

  public boolean isSimplePropertyAccess() {
    return isSimpleProperty;
  }

  public boolean isArrayAccess() {
    return isArrayAccess;
  }

  public boolean isWildcardAccess() {
    return isWildcardAccess;
  }

  // === AbstractPath実装 ===

  @Override
  protected boolean doMatches(JsonNode context) {
    // 簡易実装: 単純なプロパティアクセスのみ
    try {
      if (isSimpleProperty && segments.size() == 1) {
        return context.has(segments.get(0));
      }
      return false;
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  protected <R> Optional<R> doExtract(Object data, Class<R> resultType) {
    validateResultType(resultType);

    try {
      JsonNode jsonNode;

      // データをJsonNodeに変換
      if (data instanceof JsonNode) {
        jsonNode = (JsonNode) data;
      } else if (data instanceof String) {
        try {
          com.fasterxml.jackson.databind.ObjectMapper mapper =
              new com.fasterxml.jackson.databind.ObjectMapper();
          jsonNode = mapper.readTree((String) data);
        } catch (Exception e) {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }

      // JSONPath処理ロジック統合
      JsonNode resultNode = navigateJsonPath(jsonNode);
      if (resultNode != null && !resultNode.isMissingNode() && !resultNode.isNull()) {
        if (resultType == String.class) {
          return Optional.of(resultType.cast(resultNode.asText()));
        } else if (resultType == JsonNode.class) {
          return Optional.of(resultType.cast(resultNode));
        }
      }
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  protected <R> Stream<R> doExtractAll(Object data, Class<R> resultType) {
    validateResultType(resultType);

    try {
      // 配列/リスト結果を想定した実装
      if (data instanceof JsonNode) {
        JsonNode jsonNode = (JsonNode) data;

        if (jsonNode.isArray()) {
          return StreamSupport.stream(jsonNode.spliterator(), false)
              .filter(node -> !node.isMissingNode())
              .map(
                  node -> {
                    if (resultType == String.class) {
                      return resultType.cast(node.asText());
                    }
                    return null;
                  })
              .filter(Objects::nonNull);
        }
      }
      return Stream.empty();
    } catch (Exception e) {
      return Stream.empty();
    }
  }

  // === 内部実装メソッド ===

  private List<String> parseSegments(String normalizedPath) {
    // $.user.name -> ["user", "name"]
    // $.users[*].name -> ["users[*]", "name"]
    if (normalizedPath.equals("$")) {
      return List.of();
    }

    String withoutRoot = normalizedPath.substring(2); // $.を除去
    if (withoutRoot.isEmpty()) {
      return List.of();
    }

    return List.of(withoutRoot.split("\\."));
  }

  private boolean detectSimpleProperty() {
    return segments.size() == 1 && !segments.get(0).contains("[") && !segments.get(0).contains("*");
  }

  private boolean detectArrayAccess() {
    return segments.stream().anyMatch(s -> s.contains("["));
  }

  private boolean detectWildcardAccess() {
    return segments.stream().anyMatch(s -> s.contains("*"));
  }

  /** JSONPathナビゲーション処理（JsonNavigateCommandから統合） */
  private JsonNode navigateJsonPath(JsonNode rootNode) {
    if (isRoot()) {
      return rootNode;
    }

    // 単純なプロパティアクセス
    Optional<String> simpleProperty = extractSimpleProperty();
    if (simpleProperty.isPresent()) {
      return rootNode.get(simpleProperty.get());
    }

    // 複雑なパスの場合はJsonPointerを使用
    String pathStr = getPath();
    if (pathStr.startsWith("$.")) {
      String propertyPath = pathStr.substring(2);
      return rootNode.at("/" + propertyPath.replace(".", "/"));
    }

    return rootNode;
  }

  /**
   * ストリーミング処理用のパス判定メソッド JSON階層パスのリアルタイム判定に使用（XMLのpathHandlerと同様）
   *
   * @param currentPath 現在のJSONパス階層
   * @return パスがマッチする場合true
   */
  public boolean matchesStreamingPath(java.util.List<String> currentPath) {
    if (currentPath == null) {
      return false;
    }

    // ルートパス("$")の場合
    if (isRoot()) {
      return currentPath.isEmpty();
    }

    // セグメント数が一致しない場合はfalse
    if (currentPath.size() != segments.size()) {
      return false;
    }

    // 各セグメントを順次比較（XMLのisTargetと同じロジック）
    for (int i = 0; i < segments.size(); i++) {
      if (!segments.get(i).equals(currentPath.get(i))) {
        return false;
      }
    }

    return true;
  }

  // === 既存メソッド保持 ===

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
   * Get the simple property name if this path represents a simple property access like "$.name".
   *
   * @return Optional containing the property name, or empty if not a simple property access
   */
  public Optional<String> findSimpleProperty() {
    if (path.startsWith("$.") && !path.contains("[") && path.lastIndexOf('.') == 1) {
      return Optional.of(path.substring(2));
    }
    return Optional.empty();
  }

  private static boolean isValidIdentifier(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return false;
    }

    char first = identifier.charAt(0);
    if (!(Character.isLetter(first) || first == '_')) {
      return false;
    }

    for (int i = 1; i < identifier.length(); i++) {
      char c = identifier.charAt(i);
      if (!(Character.isLetterOrDigit(c) || c == '_')) {
        return false;
      }
    }

    return true;
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
