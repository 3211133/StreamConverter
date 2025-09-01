package com.streamConverter.path;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 最小限のJSONPath実装
 *
 * <p>JSON要素選択のパス一致判定のみに特化したシンプルな設計
 *
 * <p>複数のJSONパスをOR条件で判定する機能を提供
 */
public class JSONPath extends AbstractPath<JsonNode> {

  private final List<String> paths;

  /**
   * 単一パスでJSONPathを作成
   *
   * @param path JSONPathのパス文字列 (e.g., "$.userName", "$.users[0].name")
   * @throws IllegalArgumentException パスが不正な場合
   */
  public JSONPath(String path) {
    super(path);
    this.paths = Collections.singletonList(normalizeJsonPath(path));
  }

  /**
   * 複数パスでJSONPathを作成（OR条件）
   *
   * @param pathList JSONパスのリスト
   * @throws IllegalArgumentException パスが不正な場合
   */
  public JSONPath(List<String> pathList) {
    super(String.join(",", pathList));
    if (pathList == null || pathList.isEmpty()) {
      throw new IllegalArgumentException("Path list cannot be null or empty");
    }
    List<String> temp = new ArrayList<>();
    for (String path : pathList) {
      temp.add(normalizeJsonPath(path));
    }
    this.paths = Collections.unmodifiableList(temp);
  }

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (isNullOrEmpty(rawPath)) {
      throw new IllegalArgumentException("JSONPath cannot be null or empty");
    }
  }

  @Override
  public boolean matches(JsonNode context) {
    if (context == null) {
      return false;
    }

    // OR条件：いずれかのパスがマッチすればtrue
    for (String path : paths) {
      if (matchesSinglePath(path, context)) {
        return true;
      }
    }
    return false;
  }

  /**
   * マッチするすべてのパスを取得（Don't Ask Tell準拠）
   *
   * @param context JSON context
   * @return マッチしたパスのリスト
   */
  public List<String> findMatchingPaths(JsonNode context) {
    List<String> matchingPaths = new ArrayList<>();
    if (context == null) {
      return matchingPaths;
    }

    for (String path : paths) {
      if (matchesSinglePath(path, context)) {
        matchingPaths.add(path);
      }
    }
    return matchingPaths;
  }

  /** 単一パスの一致判定 */
  private boolean matchesSinglePath(String path, JsonNode context) {
    // Root path check
    if ("$".equals(path)) {
      return true;
    }

    // Simple property access: $.property
    if (path.startsWith("$.") && !path.contains("[") && path.indexOf(".", 2) == -1) {
      String propertyName = path.substring(2);
      return context.has(propertyName);
    }

    // For complex paths, basic implementation
    return false;
  }

  /** JSONPathの正規化 */
  private String normalizeJsonPath(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("Path cannot be null");
    }

    String trimmed = rawPath.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Path cannot be empty");
    }

    // Ensure path starts with $
    if (!trimmed.startsWith("$")) {
      trimmed = "$." + trimmed;
    }

    return trimmed;
  }

  @Override
  public String toString() {
    if (paths.size() == 1) {
      return paths.get(0);
    } else {
      return String.join(",", paths);
    }
  }
}
