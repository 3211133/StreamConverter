package com.streamConverter.path;

import java.util.Objects;

/**
 * Type-safe representation of CSV column selectors.
 *
 * <p>This class encapsulates CSV column selectors used for navigating CSV documents. It supports
 * both column name-based and index-based selection and validates the selector syntax at
 * construction time.
 *
 * <p>Supported selector types: - Column name: "userName", "first_name" - Column index: "0", "1",
 * "2" (zero-based) - Case-insensitive column name matching
 *
 * <p>This implementation provides type safety for CSV column operations in StreamConverter.
 */
public class CSVPath implements IPath {

  private static final String TYPE = "CSVPath";
  private final String selector;
  private final boolean isIndex;
  private final int columnIndex;

  /**
   * Creates a new CSVPath instance.
   *
   * @param selector the column selector (column name or index as string)
   * @throws IllegalArgumentException if the selector is null, empty, or invalid
   */
  public CSVPath(String selector) {
    if (selector == null) {
      throw new IllegalArgumentException("CSV column selector cannot be null");
    }
    if (selector.trim().isEmpty()) {
      throw new IllegalArgumentException("CSV column selector cannot be empty");
    }

    this.selector = selector.trim();

    // Determine if this is an index or name-based selector
    int parsedIndex = parseAsIndex(this.selector);
    if (parsedIndex >= 0) {
      this.isIndex = true;
      this.columnIndex = parsedIndex;
    } else {
      this.isIndex = false;
      this.columnIndex = -1;
    }

    validate();
  }

  /**
   * Factory method for creating a CSVPath from a column index.
   *
   * @param columnIndex the zero-based column index
   * @return a CSVPath for the column index
   * @throws IllegalArgumentException if columnIndex is negative
   */
  public static CSVPath fromIndex(int columnIndex) {
    if (columnIndex < 0) {
      throw new IllegalArgumentException("Column index cannot be negative: " + columnIndex);
    }
    return new CSVPath(String.valueOf(columnIndex));
  }

  /**
   * Factory method for creating a CSVPath from a column name.
   *
   * @param columnName the column name
   * @return a CSVPath for the column name
   * @throws IllegalArgumentException if columnName is invalid
   */
  public static CSVPath fromName(String columnName) {
    if (columnName == null || columnName.trim().isEmpty()) {
      throw new IllegalArgumentException("Column name cannot be null or empty");
    }

    String trimmed = columnName.trim();
    // Ensure it's not a valid index to avoid ambiguity
    if (parseAsIndex(trimmed) >= 0) {
      throw new IllegalArgumentException(
          "Column name cannot be a valid integer index: " + columnName);
    }

    return new CSVPath(trimmed);
  }

  @Override
  public void validate() {
    if (isIndex) {
      // Index validation already done in constructor
      return;
    }

    // Validate column name
    if (!isValidColumnName(selector)) {
      throw new IllegalArgumentException("Invalid CSV column name: " + selector);
    }
  }

  @Override
  public String getPath() {
    return selector;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public boolean isEquivalentTo(IPath other) {
    if (!(other instanceof CSVPath)) {
      return false;
    }
    CSVPath csvPath = (CSVPath) other;
    return Objects.equals(this.selector, csvPath.selector);
  }

  /**
   * Checks if this is an index-based selector.
   *
   * @return true if this selector represents a column index
   */
  public boolean isIndexBased() {
    return isIndex;
  }

  /**
   * Checks if this is a name-based selector.
   *
   * @return true if this selector represents a column name
   */
  public boolean isNameBased() {
    return !isIndex;
  }

  /**
   * Gets the column index if this is an index-based selector.
   *
   * @return the column index, or -1 if this is not an index-based selector
   */
  public int getColumnIndex() {
    return columnIndex;
  }

  /**
   * Gets the column name if this is a name-based selector.
   *
   * @return the column name, or null if this is an index-based selector
   */
  public String getColumnName() {
    return isIndex ? null : selector;
  }

  /**
   * Resolves this selector to a column index using the provided headers.
   *
   * @param headers the CSV headers
   * @return the resolved column index, or -1 if not found
   * @throws IllegalArgumentException if headers is null
   */
  public int resolveIndex(String[] headers) {
    if (headers == null) {
      throw new IllegalArgumentException("Headers cannot be null");
    }

    if (isIndex) {
      // Validate that the index is within bounds
      return columnIndex < headers.length ? columnIndex : -1;
    }

    // Search for column name (case-insensitive)
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].trim().equalsIgnoreCase(selector)) {
        return i;
      }
    }

    return -1; // Not found
  }

  private static int parseAsIndex(String value) {
    try {
      int index = Integer.parseInt(value);
      return index >= 0 ? index : -1;
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  private static boolean isValidColumnName(String columnName) {
    if (columnName == null || columnName.isEmpty()) {
      return false;
    }

    // Basic validation: non-empty, printable characters, common CSV column name patterns
    // Allow letters, digits, underscores, spaces, hyphens, and dots
    return columnName.matches("^[a-zA-Z0-9_ .-]+$") && !columnName.trim().isEmpty();
  }

  @Override
  public String toString() {
    if (isIndex) {
      return String.format("CSVPath(index=%d)", columnIndex);
    } else {
      return String.format("CSVPath(name='%s')", selector);
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    CSVPath csvPath = (CSVPath) obj;
    return Objects.equals(selector, csvPath.selector);
  }

  @Override
  public int hashCode() {
    return Objects.hash(selector);
  }
}
