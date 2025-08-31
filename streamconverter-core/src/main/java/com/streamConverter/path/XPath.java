package com.streamConverter.path;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 最小限のXPath実装
 *
 * <p>XML要素パス一致判定のみに特化したシンプルな設計
 *
 * <p>複数のXPathをOR条件で判定する機能を提供
 */
public class XPath extends AbstractPath<List<String>> {

  // XML element name pattern for validation
  private static final Pattern XML_ELEMENT_NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._-]*$");

  private final List<String> paths;

  /**
   * 単一パスでXPathを作成
   *
   * @param path XPath表現（例："users/user/name"）
   * @throws IllegalArgumentException パスが不正な場合
   */
  public XPath(String path) {
    super(path);
    this.paths = Collections.singletonList(normalizeXPath(path));
  }

  /**
   * 複数パスでXPathを作成（OR条件）
   *
   * @param pathList XPathのリスト
   * @throws IllegalArgumentException パスが不正な場合
   */
  public XPath(List<String> pathList) {
    super(String.join(",", pathList));
    if (pathList == null || pathList.isEmpty()) {
      throw new IllegalArgumentException("Path list cannot be null or empty");
    }
    List<String> temp = new ArrayList<>();
    for (String path : pathList) {
      temp.add(normalizeXPath(path));
    }
    this.paths = Collections.unmodifiableList(temp);
  }

  @Override
  protected void validateAndNormalize(String rawPath) {
    if (isNullOrEmpty(rawPath)) {
      throw new IllegalArgumentException("XPath cannot be null or empty");
    }
  }

  @Override
  public boolean matches(List<String> currentXmlPath) {
    if (currentXmlPath == null) {
      return false;
    }

    // OR条件：いずれかのパスがマッチすればtrue
    for (String path : paths) {
      if (matchesSinglePath(path, currentXmlPath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * マッチするすべてのパスを取得（Don't Ask Tell準拠）
   *
   * @param currentXmlPath 現在のXMLパス
   * @return マッチしたパスのリスト
   */
  public List<String> findMatchingPaths(List<String> currentXmlPath) {
    List<String> matchingPaths = new ArrayList<>();
    if (currentXmlPath == null) {
      return matchingPaths;
    }

    for (String path : paths) {
      if (matchesSinglePath(path, currentXmlPath)) {
        matchingPaths.add(path);
      }
    }
    return matchingPaths;
  }

  /** 単一パスの一致判定 */
  private boolean matchesSinglePath(String path, List<String> currentXmlPath) {
    List<String> segments = Arrays.asList(path.split("/"));

    if (currentXmlPath.size() != segments.size()) {
      return false;
    }

    for (int i = 0; i < currentXmlPath.size(); i++) {
      if (!currentXmlPath.get(i).equals(segments.get(i))) {
        return false;
      }
    }
    return true;
  }

  /** XPathの正規化 */
  private String normalizeXPath(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("XPath cannot be null");
    }

    String trimmed = rawPath.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("XPath cannot be empty");
    }

    // Remove leading slash if present
    if (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }

    // Validate segments
    String[] segments = trimmed.split("/");
    for (String segment : segments) {
      if (segment.isEmpty()) {
        throw new IllegalArgumentException("XPath cannot contain empty segments: " + rawPath);
      }
      if (!XML_ELEMENT_NAME_PATTERN.matcher(segment).matches()) {
        throw new IllegalArgumentException("Invalid XML element name in XPath: " + segment);
      }
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
