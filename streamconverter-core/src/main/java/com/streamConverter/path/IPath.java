package com.streamConverter.path;

/**
 * Base interface for type-safe path representations.
 *
 * <p>This interface provides a contract for different types of path selectors used in
 * StreamConverter commands, such as JSONPath, XPath, and CSV column selectors.
 *
 * <p>Path implementations should validate their syntax at construction time and provide meaningful
 * error messages for invalid paths.
 */
public interface IPath {

  /**
   * Validates the path syntax and throws an exception if invalid.
   *
   * @throws IllegalArgumentException if the path syntax is invalid
   */
  void validate();

  /**
   * Returns the string representation of the path.
   *
   * @return the path as a string
   */
  String getPath();

  /**
   * Returns the type of this path for identification purposes.
   *
   * @return the path type (e.g., "JSONPath", "XPath", "CSVPath")
   */
  String getType();

  /**
   * Checks if this path is equivalent to another path.
   *
   * @param other the other path to compare with
   * @return true if the paths are equivalent, false otherwise
   */
  boolean isEquivalentTo(IPath other);
}
