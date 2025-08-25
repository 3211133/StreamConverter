package com.streamConverter.path;

import java.util.List;
import java.util.Objects;

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
public class XPath implements IPath {

  private static final String TYPE = "XPath";
  private final String path;
  private final List<String> pathSegments;

  /**
   * Creates a new XPath instance.
   *
   * @param path the XPath expression (e.g., "users/user/name")
   * @throws IllegalArgumentException if the path is null, empty, or has invalid syntax
   */
  public XPath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("XPath cannot be null");
    }

    String trimmedPath = path.trim();
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

    this.path = normalizedPath;
    this.pathSegments = List.of(normalizedPath.split("/"));

    validate();
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
    // Validate no empty segments remain after normalization
    if (pathSegments.contains("")) {
      throw new IllegalArgumentException(
          "XPath must not contain empty segments after normalization: " + path);
    }

    // Validate each segment is a valid XML element name
    for (String segment : pathSegments) {
      if (!isValidXmlElementName(segment)) {
        throw new IllegalArgumentException("Invalid XML element name: " + segment);
      }
    }
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isEquivalentTo(IPath other) {
    if (!(other instanceof XPath)) {
      return false;
    }
    return Objects.equals(this.path, ((XPath) other).path);
  }

  /**
   * Returns the path segments as an immutable list.
   *
   * @return the path segments
   */
  public List<String> getSegments() {
    return List.copyOf(pathSegments);
  }

  /**
   * Gets the number of path segments.
   *
   * @return the depth of the path
   */
  public int getDepth() {
    return pathSegments.size();
  }

  /**
   * Checks if this is a single-element path.
   *
   * @return true if the path has only one segment
   */
  public boolean isSingleElement() {
    return pathSegments.size() == 1;
  }

  /**
   * Gets the last element in the path.
   *
   * @return the last element name
   */
  public String getLastElement() {
    return pathSegments.get(pathSegments.size() - 1);
  }

  /**
   * Gets the first element in the path.
   *
   * @return the first element name
   */
  public String getFirstElement() {
    return pathSegments.get(0);
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
    return elementName.matches("^[a-zA-Z_][a-zA-Z0-9._-]*$");
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
