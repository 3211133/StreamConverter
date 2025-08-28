package com.streamConverter.path;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
public class CSVPath extends AbstractPath<Integer> {

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
    super(selector, TYPE);
    this.selector = this.path;

    // Determine if this is an index or name-based selector
    int parsedIndex = parseAsIndex(this.selector);
    if (parsedIndex >= 0) {
      this.isIndex = true;
      this.columnIndex = parsedIndex;
    } else {
      this.isIndex = false;
      this.columnIndex = -1;
    }
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
  protected String validateAndNormalize(String rawSelector) {
    if (rawSelector == null) {
      throw new IllegalArgumentException("CSV column selector cannot be null");
    }
    if (rawSelector.trim().isEmpty()) {
      throw new IllegalArgumentException("CSV column selector cannot be empty");
    }

    return rawSelector.trim();
  }

  @Override
  public void validate() {
    // AbstractPathから呼ばれる場合、selectorはまだ設定されていないので、pathを使用
    String selectorToValidate = selector != null ? selector : path;

    if (selectorToValidate == null) {
      throw new IllegalArgumentException("CSV column selector cannot be null");
    }

    // isIndexフィールドが初期化されていない場合、parseAsIndexで判定
    boolean isIndexValue = isIndex;
    if (selector == null) { // まだコンストラクタ中の場合
      int parsedIndex = parseAsIndex(selectorToValidate);
      isIndexValue = parsedIndex >= 0;
    }

    if (isIndexValue) {
      // Index validation already done
      return;
    }

    // Validate column name
    if (!isValidColumnName(selectorToValidate)) {
      throw new IllegalArgumentException("Invalid CSV column name: " + selectorToValidate);
    }
  }

  // === CSVPath特化メソッド ===

  public int getColumnIndex() {
    return columnIndex;
  }

  public String getColumnName() {
    return isIndex ? null : selector;
  }

  // === AbstractPath実装 ===

  @Override
  protected boolean doMatches(Integer currentColumnIndex) {
    // CSV処理でのカラムインデックスマッチング
    if (isIndex) {
      return columnIndex == currentColumnIndex;
    }
    // 名前ベースの場合は実際のCSVヘッダー情報が必要
    // ここでは簡易実装
    return false;
  }

  @Override
  protected <R> Optional<R> doExtract(Object data, Class<R> resultType) {
    validateResultType(resultType);

    try {
      // CSV行データからカラム値を抽出
      if (data instanceof String[]) {
        String[] csvRow = (String[]) data;
        if (isIndex && columnIndex >= 0 && columnIndex < csvRow.length) {
          if (resultType == String.class) {
            return Optional.of(resultType.cast(csvRow[columnIndex]));
          }
        } else if (!isIndex) {
          // 名前ベースアクセスの場合、ヘッダー情報が必要
          // 実装は簡易版のためスキップ
          return Optional.empty();
        }
      } else if (data instanceof java.util.List) {
        // リスト形式のCSVデータ処理
        @SuppressWarnings("unchecked")
        java.util.List<String> csvRow = (java.util.List<String>) data;
        if (isIndex && columnIndex >= 0 && columnIndex < csvRow.size()) {
          if (resultType == String.class) {
            return Optional.of(resultType.cast(csvRow.get(columnIndex)));
          }
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

    // CSV全行からのカラム値抽出
    // 実装は簡易版
    return Stream.empty();
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
