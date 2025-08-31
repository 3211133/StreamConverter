package com.streamConverter.path;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 最小限のXPath実装
 *
 * <p>XML要素パス一致判定のみに特化したシンプルな設計
 */
public class XPath extends AbstractPath<List<String>> {

  // XML element name pattern for validation
  private static final Pattern XML_ELEMENT_NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._-]*$");

  private final String path;
  private final List<String> segments;

  /**
   * XPathを作成
   *
   * @param path XPath表現（例："users/user/name"）
   * @throws IllegalArgumentException パスが不正な場合
   */
  public XPath(String path) {
    super(path);
    this.path = normalizeXPath(path);
    this.segments = List.of(this.path.split("/"));
    validateSegments();
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

  /** パス文字列を正規化 */
  private String normalizeXPath(String rawPath) {
    String trimmedPath = rawPath.trim();

    // Remove leading/trailing slashes and collapse multiple slashes
    String normalizedPath =
        trimmedPath.replaceAll("^/+", "").replaceAll("/+$", "").replaceAll("/+", "/");

    if (normalizedPath.isEmpty()) {
      throw new IllegalArgumentException("XPath cannot be just slashes");
    }

    return normalizedPath;
  }

  /** セグメントの妥当性を検証 */
  private void validateSegments() {
    // Validate no empty segments remain after normalization
    if (segments.contains("")) {
      throw new IllegalArgumentException(
          "XPath must not contain empty segments after normalization: " + path);
    }

    // Validate each segment is a valid XML element name
    for (String segment : segments) {
      if (!isValidXmlElementName(segment)) {
        throw new IllegalArgumentException("Invalid XML element name: " + segment);
      }
    }
  }

  private static boolean isValidXmlElementName(String elementName) {
    if (elementName == null || elementName.isEmpty()) {
      return false;
    }
    return XML_ELEMENT_NAME_PATTERN.matcher(elementName).matches();
  }

  @Override
  public String toString() {
    return path;
  }
}
