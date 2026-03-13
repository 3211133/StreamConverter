package com.streamconverter.command.impl.csv;

import com.streamconverter.command.AbstractStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.TreePath;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/**
 * CSV Navigate Command Class
 *
 * <p>This class implements command for targeted CSV transformation using column selectors. It
 * identifies specific columns using column names or indices and applies IRule transformations to
 * those columns while preserving the overall CSV structure.
 */
public class CsvNavigateCommand extends AbstractStreamCommand {

  private final CSVPath columnSelector;
  private final IRule rule;
  private int columnIndex = -1;

  /**
   * Constructor for CSV navigation with column selector and transformation rule.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if columnSelector or rule is null
   * @deprecated Use {@link #CsvNavigateCommand(CSVPath, IRule)} instead
   */
  @Deprecated
  private CsvNavigateCommand(String columnSelector, IRule rule) {
    this.columnSelector = CSVPath.of(columnSelector);
    this.rule = rule;
  }

  /**
   * Constructor for CSV navigation with typed column selector and transformation rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  private CsvNavigateCommand(CSVPath columnSelector, IRule rule) {
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
  private CsvNavigateCommand(TreePath treePath, IRule rule) {
    this.columnSelector = CSVPath.of(treePath.toString());
    this.rule = rule;
  }

  /**
   * Factory method for creating a CSV navigation command with explicit rule specification. This
   * method makes the intention explicit: extract data from the specified column and apply the given
   * transformation rule.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvNavigateCommand that extracts the specified column with the given rule
   * @throws IllegalArgumentException if rule is null
   * @deprecated Use {@link #create(CSVPath, IRule)} instead
   */
  @Deprecated
  public static CsvNavigateCommand create(String columnSelector, IRule rule) {
    if (columnSelector == null) {
      throw new IllegalArgumentException("Column selector cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new CsvNavigateCommand(columnSelector, rule);
  }

  /**
   * Factory method for creating a CSV navigation command with typed column selector and rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvNavigateCommand that extracts the specified column with the given rule
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  public static CsvNavigateCommand create(CSVPath columnSelector, IRule rule) {
    if (columnSelector == null) {
      throw new IllegalArgumentException("Column selector cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new CsvNavigateCommand(columnSelector, rule);
  }

  /**
   * Factory method for creating a CSV navigation command with TreePath compatibility.
   *
   * @param treePath the TreePath representing column selector
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvNavigateCommand that extracts the specified column with the given rule
   * @throws IllegalArgumentException if treePath or rule is null
   */
  public static CsvNavigateCommand create(TreePath treePath, IRule rule) {
    if (treePath == null) {
      throw new IllegalArgumentException("TreePath cannot be null");
    }
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    return new CsvNavigateCommand(treePath, rule);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      // Apply rule to specific column while preserving structure
      applyRuleToColumn(reader, writer);
    }
  }

  /** Apply transformation rule to specific column while preserving CSV structure */
  private void applyRuleToColumn(BufferedReader reader, Writer writer) throws IOException {
    String headerLine = reader.readLine();
    if (headerLine == null) {
      return; // Empty input
    }

    String[] headers = parseCSVLine(headerLine);

    // Determine column index if selector is provided
    columnIndex = resolveColumnIndex(headers, columnSelector);
    if (columnIndex == -1) {
      throw new IllegalArgumentException("Column not found: " + columnSelector.toString());
    }

    // Write header (unchanged)
    writer.write(headerLine);
    writer.write(System.lineSeparator());

    // Process data rows
    String line;
    while ((line = reader.readLine()) != null) {
      String[] values = parseCSVLine(line);

      if (columnIndex < values.length) {
        // Apply rule to target column only
        values[columnIndex] = rule.apply(values[columnIndex]);
      }

      // Write entire row with transformed column (with proper CSV escaping)
      writer.write(formatCsvRow(values));
      writer.write(System.lineSeparator());
    }
    writer.flush();
  }

  private String[] parseCSVLine(String line) {
    return line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
  }

  /** Format CSV row with proper escaping */
  private String formatCsvRow(String[] values) {
    StringBuilder row = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      if (i > 0) {
        row.append(",");
      }
      row.append(escapeCsvValue(values[i]));
    }
    return row.toString();
  }

  /** Escape CSV value according to CSV standards */
  private String escapeCsvValue(String value) {
    if (value == null) {
      return "";
    }

    // Check if value needs escaping (contains comma, quote, or newline)
    if (value.contains(",")
        || value.contains("\"")
        || value.contains("\n")
        || value.contains("\r")) {
      // Escape quotes by doubling them and wrap entire value in quotes
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    return value;
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
