package com.streamConverter.command.impl.csv;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.path.CSVPath;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV Filter Command Class
 *
 * <p>This class implements pure data extraction from CSV using column selectors. Unlike
 * CsvNavigateCommand which applies transformations, CsvFilterCommand only extracts/filters columns
 * based on specified column names or indices without any modifications.
 *
 * <p>Features: - Extract specific columns using column names or indices - Preserve exact data
 * values from extracted columns - Memory-efficient streaming processing - Support for multiple
 * column selection
 */
public class CsvFilterCommand extends AbstractStreamCommand {

  private final List<CSVPath> columnSelectors;
  private final boolean hasHeader;

  // Deprecated fields for backward compatibility
  @Deprecated private final List<String> legacyColumnSelectors;

  /**
   * Constructor for CSV filtering with single column selector.
   *
   * @param columnSelector the column name or index to extract (e.g., "name", "2")
   * @param hasHeader whether the CSV has a header row
   * @throws IllegalArgumentException if columnSelector is null or empty
   * @deprecated Use {@link #CsvFilterCommand(CSVPath, boolean)} instead
   */
  @Deprecated
  public CsvFilterCommand(String columnSelector, boolean hasHeader) {
    if (columnSelector == null || columnSelector.trim().isEmpty()) {
      throw new IllegalArgumentException("Column selector cannot be null or empty");
    }
    this.legacyColumnSelectors = Arrays.asList(columnSelector.trim());
    this.columnSelectors = Arrays.asList(new CSVPath(columnSelector.trim()));
    this.hasHeader = hasHeader;
  }

  /**
   * Constructor for CSV filtering with single typed column selector.
   *
   * @param columnSelector the typed CSVPath to extract
   * @param hasHeader whether the CSV has a header row
   * @throws IllegalArgumentException if columnSelector is null
   */
  public CsvFilterCommand(CSVPath columnSelector, boolean hasHeader) {
    if (columnSelector == null) {
      throw new IllegalArgumentException("Column selector cannot be null");
    }
    this.columnSelectors = Arrays.asList(columnSelector);
    this.legacyColumnSelectors = Arrays.asList(columnSelector.toString());
    this.hasHeader = hasHeader;
  }

  /**
   * Constructor for CSV filtering with multiple column selectors.
   *
   * @param columnSelectors list of column names or indices to extract
   * @param hasHeader whether the CSV has a header row
   * @throws IllegalArgumentException if columnSelectors is null or empty
   * @deprecated Use {@link #CsvFilterCommand(List, boolean)} with CSVPath list instead
   */
  @Deprecated
  public CsvFilterCommand(List<String> columnSelectors, boolean hasHeader) {
    if (columnSelectors == null || columnSelectors.isEmpty()) {
      throw new IllegalArgumentException("Column selectors cannot be null or empty");
    }
    this.legacyColumnSelectors = new ArrayList<>(columnSelectors);
    this.columnSelectors = columnSelectors.stream().map(CSVPath::new).collect(Collectors.toList());
    this.hasHeader = hasHeader;
  }

  /**
   * Private constructor for internal use with typed column selectors.
   *
   * @param columnSelectors list of typed CSVPaths to extract
   * @param hasHeader whether the CSV has a header row
   * @param internal marker parameter to distinguish from deprecated constructor
   */
  private CsvFilterCommand(List<CSVPath> columnSelectors, boolean hasHeader, boolean internal) {
    if (columnSelectors == null || columnSelectors.isEmpty()) {
      throw new IllegalArgumentException("Column selectors cannot be null or empty");
    }
    this.columnSelectors = new ArrayList<>(columnSelectors);
    this.legacyColumnSelectors =
        columnSelectors.stream().map(CSVPath::toString).collect(Collectors.toList());
    this.hasHeader = hasHeader;
  }

  /**
   * Factory method for CSV filtering with multiple typed column selectors.
   *
   * @param columnSelectors list of typed CSVPaths to extract
   * @param hasHeader whether the CSV has a header row
   * @return a CsvFilterCommand instance
   * @throws IllegalArgumentException if columnSelectors is null or empty
   */
  public static CsvFilterCommand create(List<CSVPath> columnSelectors, boolean hasHeader) {
    return new CsvFilterCommand(columnSelectors, hasHeader, true);
  }

  /**
   * Constructor for CSV filtering with single column selector (assumes header exists).
   *
   * @param columnSelector the column name or index to extract
   * @deprecated Use {@link #create(CSVPath)} instead
   */
  @Deprecated
  public CsvFilterCommand(String columnSelector) {
    this(columnSelector, true);
  }

  /**
   * Factory method for CSV filtering with single typed column selector (assumes header exists).
   *
   * @param columnSelector the typed CSVPath to extract
   * @return a CsvFilterCommand instance
   */
  public static CsvFilterCommand create(CSVPath columnSelector) {
    return create(columnSelector, true);
  }

  /**
   * Factory method for CSV filtering with single typed column selector.
   *
   * @param columnSelector the typed CSVPath to extract
   * @param hasHeader whether the CSV has a header row
   * @return a CsvFilterCommand instance
   */
  public static CsvFilterCommand create(CSVPath columnSelector, boolean hasHeader) {
    return create(Arrays.asList(columnSelector), hasHeader);
  }

  /**
   * Constructor for CSV filtering with multiple column selectors (assumes header exists).
   *
   * @param columnSelectors list of column names or indices to extract
   * @deprecated Use constructor with List&lt;CSVPath&gt; instead
   */
  @Deprecated
  public CsvFilterCommand(List<String> columnSelectors) {
    this(columnSelectors, true);
  }

  @Override
  protected String getCommandDetails() {
    List<String> selectorPaths =
        columnSelectors.stream().map(CSVPath::toString).collect(Collectors.toList());
    return String.format("CsvFilterCommand(columns=%s, hasHeader=%s)", selectorPaths, hasHeader);
  }

  @Override
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      List<Integer> columnIndices = new ArrayList<>();
      String[] headers = null;

      // Read first line
      String firstLine = reader.readLine();
      if (firstLine == null) {
        // Empty CSV
        writer.flush();
        return;
      }

      String[] firstRowFields = parseCsvLine(firstLine);

      if (hasHeader) {
        headers = firstRowFields;
        // Map column selectors to indices
        columnIndices = mapColumnSelectorsToIndices(columnSelectors, headers);

        // Write filtered header
        writeFilteredRow(writer, firstRowFields, columnIndices);
        writer.write(System.lineSeparator());
      } else {
        // No header - column selectors must be numeric indices
        columnIndices = parseNumericColumnSelectors(columnSelectors, firstRowFields.length);

        // Write filtered first data row
        writeFilteredRow(writer, firstRowFields, columnIndices);
        writer.write(System.lineSeparator());
      }

      // Process remaining data rows
      String line;
      while ((line = reader.readLine()) != null) {
        String[] fields = parseCsvLine(line);
        writeFilteredRow(writer, fields, columnIndices);
        writer.write(System.lineSeparator());
      }

      writer.flush();
    }
  }

  /**
   * Map column selectors to column indices
   *
   * @param selectors list of column names or indices
   * @param headers array of header names
   * @return list of column indices
   * @throws IllegalArgumentException if column not found
   */
  private List<Integer> mapColumnSelectorsToIndices(List<CSVPath> selectors, String[] headers) {
    List<Integer> indices = new ArrayList<>();

    for (CSVPath selector : selectors) {
      int index = resolveColumnIndex(headers, selector);
      if (index == -1) {
        throw new IllegalArgumentException("Column not found: " + selector.toString());
      }
      indices.add(index);
    }

    return indices;
  }

  /**
   * Parse numeric column selectors when no header exists
   *
   * @param selectors list of column indices as strings
   * @param totalColumns total number of columns available
   * @return list of column indices
   * @throws IllegalArgumentException if index is invalid
   */
  private List<Integer> parseNumericColumnSelectors(List<CSVPath> selectors, int totalColumns) {
    List<Integer> indices = new ArrayList<>();

    for (CSVPath selector : selectors) {
      // Try to find matching column index using matches()
      boolean found = false;
      for (int i = 0; i < totalColumns; i++) {
        if (selector.matches(i)) {
          indices.add(i);
          found = true;
          break;
        }
      }

      if (!found) {
        throw new IllegalArgumentException(
            "Column selector must be numeric when no header: " + selector.toString());
      }
    }

    return indices;
  }

  /**
   * Parse a CSV line into fields (simple implementation)
   *
   * @param line CSV line
   * @return array of field values
   */
  private String[] parseCsvLine(String line) {
    // Simple CSV parsing - handles basic comma separation
    // For production, consider using a proper CSV library
    if (line == null || line.isEmpty()) {
      return new String[0];
    }

    List<String> fields = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);

      if (c == '"') {
        inQuotes = !inQuotes;
      } else if (c == ',' && !inQuotes) {
        fields.add(current.toString().trim());
        current.setLength(0);
      } else {
        current.append(c);
      }
    }

    fields.add(current.toString().trim());
    return fields.toArray(new String[0]);
  }

  /**
   * Write filtered row with only selected columns
   *
   * @param writer output writer
   * @param fields all field values
   * @param columnIndices indices of columns to include
   * @throws IOException if writing fails
   */
  private void writeFilteredRow(Writer writer, String[] fields, List<Integer> columnIndices)
      throws IOException {
    for (int i = 0; i < columnIndices.size(); i++) {
      if (i > 0) {
        writer.write(",");
      }

      int columnIndex = columnIndices.get(i);
      if (columnIndex < fields.length) {
        String field = fields[columnIndex];
        // Quote field if it contains comma or quotes
        if (field.contains(",") || field.contains("\"")) {
          writer.write("\"" + field.replace("\"", "\"\"") + "\"");
        } else {
          writer.write(field);
        }
      } else {
        // Column doesn't exist in this row - write empty field
        writer.write("");
      }
    }
  }

  /** Resolve column index using CSVPath matches() method */
  private int resolveColumnIndex(String[] headers, CSVPath csvPath) {
    // Use matches() method to check each column
    for (int i = 0; i < headers.length; i++) {
      if (csvPath.matches(headers, i)) {
        return i;
      }
    }
    return -1; // Not found
  }
}
