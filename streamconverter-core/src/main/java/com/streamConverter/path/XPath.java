package com.streamConverter.path;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Type-safe representation of XPath expressions.
 *
 * <p>This class encapsulates XPath expressions used for navigating XML documents. It validates the
 * path syntax at construction time and provides type safety for XML navigation operations.
 *
 * <p>This implementation leverages the existing FixedStaXPathHandler validation logic and supports
 * the same XPath patterns: - Simple element paths: "root/child/grandchild" - No leading or trailing
 * slashes - No empty segments
 *
 * <p>Note: This is a simplified XPath implementation focused on element navigation in
 * StreamConverter.
 */
public class XPath extends AbstractPath<List<String>> {

  private static final String TYPE = "XPath";

  // XML element name pattern for validation
  private static final Pattern XML_ELEMENT_NAME_PATTERN =
      Pattern.compile("^[a-zA-Z_][a-zA-Z0-9._-]*$");

  private final List<String> segments;

  /**
   * Creates a new XPath instance.
   *
   * @param path the XPath expression (e.g., "users/user/name")
   * @throws IllegalArgumentException if the path is null, empty, or has invalid syntax
   */
  public XPath(String path) {
    super(path, TYPE);
    this.segments = List.of(this.path.split("/"));
  }

  @Override
  protected String validateAndNormalize(String rawPath) {
    if (rawPath == null) {
      throw new IllegalArgumentException("XPath cannot be null");
    }

    String trimmedPath = rawPath.trim();
    if (trimmedPath.isEmpty()) {
      throw new IllegalArgumentException("XPath cannot be empty");
    }

    // Normalize path using the same logic as FixedStaXPathHandler
    String normalizedPath =
        trimmedPath
            .replaceAll("^/+", "") // Remove leading slashes
            .replaceAll("/+$", "") // Remove trailing slashes
            .replaceAll("/+", "/"); // Collapse multiple slashes

    if (normalizedPath.isEmpty()) {
      throw new IllegalArgumentException("XPath cannot be just slashes");
    }

    return normalizedPath;
  }

  /**
   * Factory method for creating an XPath from path segments.
   *
   * @param segments the path segments (e.g., "users", "user", "name")
   * @return an XPath constructed from the segments
   * @throws IllegalArgumentException if segments are invalid
   */
  public static XPath fromSegments(String... segments) {
    if (segments == null || segments.length == 0) {
      throw new IllegalArgumentException("Path segments cannot be null or empty");
    }

    for (String segment : segments) {
      if (segment == null || segment.trim().isEmpty()) {
        throw new IllegalArgumentException("Path segment cannot be null or empty");
      }
    }

    return new XPath(String.join("/", segments));
  }

  /**
   * Factory method for creating a single-element XPath.
   *
   * @param elementName the element name
   * @return an XPath for the single element
   * @throws IllegalArgumentException if elementName is invalid
   */
  public static XPath element(String elementName) {
    if (elementName == null || elementName.trim().isEmpty()) {
      throw new IllegalArgumentException("Element name cannot be null or empty");
    }
    return new XPath(elementName.trim());
  }

  @Override
  public void validate() {
    // AbstractPathから呼ばれる場合、segmentsはまだ設定されていないので、pathを使用
    List<String> segmentsToValidate = segments != null ? segments : List.of(path.split("/"));

    // Validate no empty segments remain after normalization
    if (segmentsToValidate.contains("")) {
      throw new IllegalArgumentException(
          "XPath must not contain empty segments after normalization: " + path);
    }

    // Validate each segment is a valid XML element name
    for (String segment : segmentsToValidate) {
      if (!isValidXmlElementName(segment)) {
        throw new IllegalArgumentException("Invalid XML element name: " + segment);
      }
    }
  }

  // === XPath特化メソッド ===

  public int getDepth() {
    return segments.size();
  }

  public boolean isDescendantOf(XPath ancestor) {
    List<String> ancestorSegments = ancestor.getSegments();
    if (segments.size() <= ancestorSegments.size()) {
      return false;
    }
    return segments.subList(0, ancestorSegments.size()).equals(ancestorSegments);
  }

  // === AbstractPath実装 ===

  @Override
  protected boolean doMatches(List<String> currentXmlPath) {
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

  @Override
  protected <R> Optional<R> doExtract(Object data, Class<R> resultType) {
    validateResultType(resultType);

    // XML Document/Element からXPathで値抽出
    // 実装は javax.xml.xpath.XPath APIを使用予定
    // ここではモック実装
    try {
      if (data instanceof org.w3c.dom.Document) {
        // XPath評価による値抽出ロジック
        return extractFromDocument((org.w3c.dom.Document) data, resultType);
      }
      return Optional.empty();
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  protected <R> Stream<R> doExtractAll(Object data, Class<R> resultType) {
    validateResultType(resultType);

    // 複数マッチするXPath結果をStreamで返す
    return Stream.empty(); // モック実装
  }

  private <R> Optional<R> extractFromDocument(org.w3c.dom.Document doc, Class<R> resultType) {
    // XPath APIを使用した実装予定
    return Optional.empty(); // モック実装
  }

  /**
   * Returns the path segments as an immutable list.
   *
   * @return the path segments
   */
  public List<String> getSegments() {
    return List.copyOf(segments);
  }

  /**
   * Checks if this is a single-element path.
   *
   * @return true if the path has only one segment
   */
  public boolean isSingleElement() {
    return segments.size() == 1;
  }

  /**
   * Gets the last element in the path.
   *
   * @return the last element name
   */
  public String getLastElement() {
    return segments.get(segments.size() - 1);
  }

  /**
   * Gets the first element in the path.
   *
   * @return the first element name
   */
  public String getFirstElement() {
    return segments.get(0);
  }

  /**
   * Creates a child XPath by appending an element.
   *
   * @param childElement the child element to append
   * @return a new XPath with the child element appended
   * @throws IllegalArgumentException if childElement is invalid
   */
  public XPath child(String childElement) {
    if (childElement == null || childElement.trim().isEmpty()) {
      throw new IllegalArgumentException("Child element cannot be null or empty");
    }
    return new XPath(path + "/" + childElement.trim());
  }

  private static boolean isValidXmlElementName(String elementName) {
    if (elementName == null || elementName.isEmpty()) {
      return false;
    }

    // Basic XML element name validation
    // Must start with letter or underscore, followed by letters, digits, hyphens, periods, or
    // underscores
    return XML_ELEMENT_NAME_PATTERN.matcher(elementName).matches();
  }

  @Override
  public String toString() {
    return String.format("XPath('%s')", path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    XPath xPath = (XPath) obj;
    return Objects.equals(path, xPath.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(path);
  }
}
