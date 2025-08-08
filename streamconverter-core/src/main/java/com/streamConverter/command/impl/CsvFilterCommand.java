package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
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

  private final List<String> columnSelectors;
  private final boolean hasHeader;

  /**
   * Constructor for CSV filtering with single column selector.
   *
   * @param columnSelector the column name or index to extract (e.g., "name", "2")
   * @param hasHeader whether the CSV has a header row
   * @throws IllegalArgumentException if columnSelector is null or empty
   */
  public CsvFilterCommand(String columnSelector, boolean hasHeader) {
    if (columnSelector == null || columnSelector.trim().isEmpty()) {
      throw new IllegalArgumentException("Column selector cannot be null or empty");
    }
    this.columnSelectors = Arrays.asList(columnSelector.trim());
    this.hasHeader = hasHeader;
  }

  /**
   * Constructor for CSV filtering with multiple column selectors.
   *
   * @param columnSelectors list of column names or indices to extract
   * @param hasHeader whether the CSV has a header row
   * @throws IllegalArgumentException if columnSelectors is null or empty
   */
  public CsvFilterCommand(List<String> columnSelectors, boolean hasHeader) {
    if (columnSelectors == null || columnSelectors.isEmpty()) {
      throw new IllegalArgumentException("Column selectors cannot be null or empty");
    }
    this.columnSelectors = new ArrayList<>(columnSelectors);
    this.hasHeader = hasHeader;
  }

  /**
   * Constructor for CSV filtering with single column selector (assumes header exists).
   *
   * @param columnSelector the column name or index to extract
   */
  public CsvFilterCommand(String columnSelector) {
    this(columnSelector, true);
  }

  /**
   * Constructor for CSV filtering with multiple column selectors (assumes header exists).
   *
   * @param columnSelectors list of column names or indices to extract
   */
  public CsvFilterCommand(List<String> columnSelectors) {
    this(columnSelectors, true);
  }

  @Override
  protected String getCommandDetails() {
    return String.format("CsvFilterCommand(columns=%s, hasHeader=%s)", columnSelectors, hasHeader);
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
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
  private List<Integer> mapColumnSelectorsToIndices(List<String> selectors, String[] headers) {
    List<Integer> indices = new ArrayList<>();

    for (String selector : selectors) {
      // Try as column name first
      boolean found = false;
      for (int i = 0; i < headers.length; i++) {
        if (headers[i].equals(selector)) {
          indices.add(i);
          found = true;
          break;
        }
      }

      if (!found) {
        // Try as numeric index
        try {
          int index = Integer.parseInt(selector);
          if (index >= 0 && index < headers.length) {
            indices.add(index);
            found = true;
          }
        } catch (NumberFormatException e) {
          // Not a valid number
        }
      }

      if (!found) {
        throw new IllegalArgumentException("Column not found: " + selector);
      }
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
  private List<Integer> parseNumericColumnSelectors(List<String> selectors, int totalColumns) {
    List<Integer> indices = new ArrayList<>();

    for (String selector : selectors) {
      try {
        int index = Integer.parseInt(selector);
        if (index < 0 || index >= totalColumns) {
          throw new IllegalArgumentException("Column index out of range: " + selector);
        }
        indices.add(index);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(
            "Invalid column index (must be numeric when no header): " + selector);
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
}
