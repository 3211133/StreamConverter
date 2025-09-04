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

  private final CSVPath combinedSelector;
  private final boolean hasHeader;

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
    this.combinedSelector = columnSelector;
    Arrays.asList(columnSelector);
    Arrays.asList(columnSelector.toString());
    this.hasHeader = hasHeader;
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
    return new CsvFilterCommand(columnSelector, hasHeader);
  }

  @Override
  protected String getCommandDetails() {
    return String.format(
        "CsvFilterCommand(columns=%s, hasHeader=%s)", combinedSelector.toString(), hasHeader);
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
        columnIndices = mapColumnSelectorsToIndices(combinedSelector, headers);

        // Write filtered header
        writeFilteredRow(writer, firstRowFields, columnIndices);
        writer.write(System.lineSeparator());
      } else {
        // No header - column selectors must be numeric indices
        columnIndices = parseNumericColumnSelectors(combinedSelector, firstRowFields.length);

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
   * Map column selectors to column indices using Don't Ask Tell pattern
   *
   * @param csvPath the CSV path containing multiple selectors
   * @param headers array of header names
   * @return list of column indices
   * @throws IllegalArgumentException if no columns found
   */
  private List<Integer> mapColumnSelectorsToIndices(CSVPath csvPath, String[] headers) {
    List<Integer> indices = csvPath.findMatchingIndices(headers);
    if (indices.isEmpty()) {
      throw new IllegalArgumentException("No columns found for selector: " + csvPath.toString());
    }
    return indices;
  }

  /**
   * Parse numeric column selectors when no header exists using Don't Ask Tell pattern
   *
   * @param csvPath the CSV path containing column selectors
   * @param totalColumns total number of columns available
   * @return list of column indices
   * @throws IllegalArgumentException if no valid indices found
   */
  private List<Integer> parseNumericColumnSelectors(CSVPath csvPath, int totalColumns) {
    List<Integer> indices = csvPath.findMatchingIndices(totalColumns);
    if (indices.isEmpty()) {
      throw new IllegalArgumentException(
          "Column selector must be numeric when no header: " + csvPath.toString());
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
