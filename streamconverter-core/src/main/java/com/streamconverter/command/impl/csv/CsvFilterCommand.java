package com.streamconverter.command.impl.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.path.CSVPath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
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
  private CsvFilterCommand(CSVPath columnSelector, boolean hasHeader) {
    this.combinedSelector = columnSelector;
    this.hasHeader = hasHeader;
  }

  /**
   * Factory method for CSV filtering with single typed column selector (assumes header exists).
   *
   * @param columnSelector the typed CSVPath to extract
   * @return a CsvFilterCommand instance
   * @throws IllegalArgumentException if columnSelector is null
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
   * @throws IllegalArgumentException if columnSelector is null
   */
  public static CsvFilterCommand create(CSVPath columnSelector, boolean hasHeader) {
    if (columnSelector == null) {
      throw new IllegalArgumentException("Column selector cannot be null");
    }
    return new CsvFilterCommand(columnSelector, hasHeader);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (CSVReader csvReader =
            new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        CSVWriter csvWriter =
            new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {

      List<Integer> columnIndices;

      String[] firstRow = csvReader.readNext();
      if (firstRow == null) {
        csvWriter.flush();
        return;
      }

      if (hasHeader) {
        columnIndices = mapColumnSelectorsToIndices(combinedSelector, firstRow);
        // Write filtered header
        writeFilteredRow(csvWriter, firstRow, columnIndices);
      } else {
        // No header — column selectors must be numeric indices
        columnIndices = parseNumericColumnSelectors(combinedSelector, firstRow.length);
        // Write filtered first data row
        writeFilteredRow(csvWriter, firstRow, columnIndices);
      }

      // Process remaining rows
      String[] row;
      while ((row = csvReader.readNext()) != null) {
        writeFilteredRow(csvWriter, row, columnIndices);
      }

      csvWriter.flush();
    } catch (CsvValidationException e) {
      throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
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
   * Write filtered row with only selected columns. Quotes are applied only when required by RFC
   * 4180 (fields containing commas, quotes, or newlines).
   *
   * @param csvWriter output writer
   * @param fields all field values
   * @param columnIndices indices of columns to include
   */
  private void writeFilteredRow(CSVWriter csvWriter, String[] fields, List<Integer> columnIndices) {
    String[] filteredRow = new String[columnIndices.size()];
    for (int i = 0; i < columnIndices.size(); i++) {
      int columnIndex = columnIndices.get(i);
      filteredRow[i] = columnIndex < fields.length ? fields[columnIndex] : "";
    }
    // applyQuotesToAll=false: only quote fields that contain delimiters or quotes
    csvWriter.writeNext(filteredRow, false);
  }
}
