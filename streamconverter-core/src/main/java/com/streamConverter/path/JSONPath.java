package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 最小限のJSONPath実装
 *
 * <p>JSON要素選択のパス一致判定のみに特化したシンプルな設計
 *
 * <p>複数のJSONパスをOR条件で判定する機能を提供
 *
 * <p>List&lt;String&gt;ベースのフルパス階層比較により、ネストされたJSONパスを正確に処理
 */
public class JSONPath extends AbstractPath<List<String>> {

  private final List<List<String>> pathSegmentsList;
  private final String originalPath;

  /**
   * 単一パスでJSONPathを作成
   *
   * @param path JSONPathのパス文字列 (e.g., "$.userName", "$.users[0].name")
   * @throws IllegalArgumentException パスが不正な場合
   */
  public JSONPath(String path) {
    super(path);
    this.originalPath = path;
    this.pathSegmentsList = Collections.singletonList(parseJsonPath(path));
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
    this.originalPath = String.join(",", pathList);
    List<List<String>> temp = new ArrayList<>();
    for (String path : pathList) {
      temp.add(parseJsonPath(path));
    }
    this.pathSegmentsList = Collections.unmodifiableList(temp);
  }

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (isNullOrEmpty(rawPath)) {
      throw new IllegalArgumentException("JSONPath cannot be null or empty");
    }
  }

  @Override
  public boolean matches(List<String> currentPath) {
    if (currentPath == null) {
      return false;
    }

    // OR条件：いずれかのパスがマッチすればtrue
    for (List<String> pathSegments : pathSegmentsList) {
      if (matchesPathSegments(pathSegments, currentPath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * マッチするすべてのパスを取得（Don't Ask Tell準拠）
   *
   * @param currentPath 現在のパス階層
   * @return マッチしたパスのリスト
   */
  public List<List<String>> findMatchingPaths(List<String> currentPath) {
    List<List<String>> matchingPaths = new ArrayList<>();
    if (currentPath == null || currentPath.isEmpty()) {
      return matchingPaths;
    }

    for (List<String> pathSegments : pathSegmentsList) {
      if (matchesPathSegments(pathSegments, currentPath)) {
        matchingPaths.add(pathSegments);
      }
    }
    return matchingPaths;
  }

  /** パスセグメントの一致判定 */
  private boolean matchesPathSegments(List<String> pathSegments, List<String> currentPath) {
    if (pathSegments == null || pathSegments.isEmpty()) {
      return currentPath == null || currentPath.isEmpty();
    }

    // 完全一致判定
    return pathSegments.equals(currentPath);
  }

  /** JSONPathをパスセグメントリストに変換 */
  private List<String> parseJsonPath(String rawPath) {
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

    // Root pathの場合は空リストを返す
    if ("$".equals(trimmed)) {
      return new ArrayList<>();
    }

    // $.を除去してドット区切りでパース
    if (trimmed.startsWith("$.")) {
      trimmed = trimmed.substring(2);
    }

    List<String> segments = new ArrayList<>();
    if (!trimmed.isEmpty()) {
      String[] parts = trimmed.split("\\.");
      for (String part : parts) {
        if (!part.trim().isEmpty()) {
          segments.add(part.trim());
        }
      }
    }

    return segments;
  }

  @Override
  public String toString() {
    return originalPath;
  }
}
