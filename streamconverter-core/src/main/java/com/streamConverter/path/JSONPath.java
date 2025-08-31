package com.streamConverter.path;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 最小限のJSONPath実装
 *
 * <p>パス一致判定のみに特化したシンプルな設計
 */
public class JSONPath extends AbstractPath<JsonNode> {

  private final String path;

  /**
   * JSONPathを作成
   *
   * @param path JSONPathのパス文字列 (e.g., "$.userName", "$.users[0].name")
   * @throws IllegalArgumentException パスが不正な場合
   */
  public JSONPath(String path) {
    super(path);
    this.path = normalizeJsonPath(path);
  }

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (isNullOrEmpty(rawPath)) {
      throw new IllegalArgumentException("JSONPath cannot be null or empty");
    }

    String normalized = normalizeJsonPath(rawPath.trim());

    // 基本的な構文チェック
    if (!normalized.startsWith("$")) {
      throw new IllegalArgumentException("JSONPath must start with '$': " + normalized);
    }
  }

  @Override
  public boolean matches(JsonNode context) {
    if (context == null) {
      return false;
    }

    try {
      // 単純なプロパティアクセス "$.propertyName" のみサポート
      if (path.startsWith("$.") && path.indexOf('.', 2) == -1 && !path.contains("[")) {
        String propertyName = path.substring(2);
        return context.has(propertyName);
      }

      // ルートアクセス "$"
      if ("$".equals(path)) {
        return true;
      }

      return false;
    } catch (Exception e) {
      return false;
    }
  }

  /** パス文字列を正規化 */
  private String normalizeJsonPath(String rawPath) {
    if (rawPath.startsWith("$")) {
      return rawPath;
    }

    // 単純な識別子の場合は $.identifier に変換
    if (isValidIdentifier(rawPath)) {
      return "$." + rawPath;
    }

    return rawPath;
  }

  private static boolean isValidIdentifier(String identifier) {
    if (identifier == null || identifier.isEmpty()) {
      return false;
    }

    char first = identifier.charAt(0);
    if (!Character.isLetter(first) && first != '_') {
      return false;
    }

    for (int i = 1; i < identifier.length(); i++) {
      char c = identifier.charAt(i);
      if (!Character.isLetterOrDigit(c) && c != '_') {
        return false;
      }
    }

    return true;
  }

  @Override
  public String toString() {
    return path;
  }
}
