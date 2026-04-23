package com.streamconverter.command.impl.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * CSV Walker
 *
 * <p>This class implements command for targeted CSV transformation using column selectors. It
 * identifies specific columns using column names or indices and applies IRule transformations to
 * those columns while preserving the overall CSV structure.
 */
public class CsvWalker extends AbstractStreamCommand {

  private final CSVPath columnSelector;
  private final IRule rule;

  /**
   * Constructor for CSV navigation with typed column selector and transformation rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  private CsvWalker(CSVPath columnSelector, IRule rule) {
    this.columnSelector = columnSelector;
    this.rule = rule;
  }

  /**
   * Constructor for CSV navigation with TreePath (for compatibility).
   *
   * @param treePath the TreePath representing column selector
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if treePath or rule is null
   */
  private CsvWalker(TreePath treePath, IRule rule) {
    this.columnSelector = CSVPath.of(treePath.toString());
    this.rule = rule;
  }

  /**
   * Factory method for creating a CSV navigation command with typed column selector and rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvWalker that extracts the specified column with the given rule
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  public static CsvWalker create(CSVPath columnSelector, IRule rule) {
    if (columnSelector == null) {
      throw new IllegalArgumentException("Column selector cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new CsvWalker(columnSelector, rule);
  }

  /**
   * Factory method for creating a CSV walker with TreePath compatibility.
   *
   * @param treePath the TreePath representing column selector
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvWalker that extracts the specified column with the given rule
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public static CsvWalker create(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new CsvWalker(treePath, rule);
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
                // RFC 4180 §5: only doubled-quote escaping, no backslash escape
                CSVWriter.NO_ESCAPE_CHARACTER,
                // RFC4180_LINE_END (\r\n) per RFC 4180 §2.
                CSVWriter.RFC4180_LINE_END)) {

      applyRuleToColumn(csvReader, csvWriter);

    } catch (CsvValidationException e) {
      throw new IOException("Failed to parse CSV: " + e.getMessage(), e);
    }
  }

  /** Apply transformation rule to specific column while preserving CSV structure */
  private void applyRuleToColumn(CSVReader csvReader, CSVWriter csvWriter)
      throws IOException, CsvValidationException {
    String[] headers = csvReader.readNext();
    if (headers == null) {
      return; // Empty input
    }

    // Determine column index
    int columnIndex = resolveColumnIndex(headers, columnSelector);
    if (columnIndex == -1) {
      throw new IllegalArgumentException("Column not found: " + columnSelector.toString());
    }

    // Write header (unchanged); applyQuotesToAll=false: only quote when RFC 4180 requires
    csvWriter.writeNext(headers, false);

    // Process data rows
    String[] row;
    // Read and transform each data row until the CSV reader reaches EOF.
    while ((row = csvReader.readNext()) != null) {
      if (columnIndex < row.length) {
        // Apply rule to target column only
        row[columnIndex] = rule.apply(row[columnIndex]);
      }
      // applyQuotesToAll=false: only quote fields that contain delimiters or quotes
      csvWriter.writeNext(row, false);
    }
    csvWriter.flush();
  }

  /** Resolve column index using CSVPath matches() method */
  private int resolveColumnIndex(String[] headers, CSVPath csvPath) {
    for (int i = 0; i < headers.length; i++) {
      if (csvPath.matches(headers, i)) {
        return i;
      }
    }
    return -1; // Not found
  }
}
