package com.streamConverter.command.impl.csv;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.path.CSVPath;
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

  // Deprecated fields for backward compatibility
  @Deprecated private final String legacyColumnSelector;

  /**
   * Constructor for CSV navigation with column selector and transformation rule.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if rule is null
   * @deprecated Use {@link #CsvNavigateCommand(CSVPath, IRule)} instead
   */
  @Deprecated
  public CsvNavigateCommand(String columnSelector, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.legacyColumnSelector = columnSelector;
    this.columnSelector = columnSelector != null ? new CSVPath(columnSelector) : null;
    this.rule = rule;
  }

  /**
   * Constructor for CSV navigation with typed column selector and transformation rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if rule is null
   */
  public CsvNavigateCommand(CSVPath columnSelector, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.columnSelector = columnSelector;
    this.legacyColumnSelector = columnSelector != null ? columnSelector.toString() : null;
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
    return new CsvNavigateCommand(columnSelector, rule);
  }

  /**
   * Factory method for creating a CSV navigation command with typed column selector and rule.
   *
   * @param columnSelector the typed CSVPath to select column
   * @param rule the transformation rule to apply to selected column data
   * @return a CsvNavigateCommand that extracts the specified column with the given rule
   * @throws IllegalArgumentException if rule is null
   */
  public static CsvNavigateCommand create(CSVPath columnSelector, IRule rule) {
    return new CsvNavigateCommand(columnSelector, rule);
  }

  /**
   * Factory method for creating a CSV navigation command that processes all columns with explicit
   * rule specification. This method makes the intention explicit: process all CSV data with the
   * given transformation rule.
   *
   * @param rule the transformation rule to apply to all CSV data
   * @return a CsvNavigateCommand that processes all columns with the given rule
   * @throws IllegalArgumentException if rule is null
   */
  public static CsvNavigateCommand createForAll(IRule rule) {
    return new CsvNavigateCommand((CSVPath) null, rule);
  }

  @Override
  protected String getCommandDetails() {
    String selectorInfo =
        columnSelector != null
            ? String.format("columnSelector='%s'", columnSelector.toString())
            : "all columns";
    return String.format(
        "CsvNavigateCommand(%s, rule='%s')", selectorInfo, rule.getClass().getSimpleName());
  }

  @Override
  protected void executeInternal(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    try (BufferedReader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {

      if (columnSelector == null) {
        // Apply rule to entire CSV content
        applyRuleToEntireCsv(reader, writer);
      } else {
        // Apply rule to specific column while preserving structure
        applyRuleToColumn(reader, writer);
      }
    }
  }

  /** Apply transformation rule to entire CSV content using streaming approach */
  private void applyRuleToEntireCsv(BufferedReader reader, Writer writer) throws IOException {
    String line;
    boolean isFirstLine = true;

    // Stream through CSV lines and apply rule to each line
    while ((line = reader.readLine()) != null) {
      if (!isFirstLine) {
        writer.write(System.lineSeparator());
      }

      // Apply rule to each line individually to avoid loading entire CSV
      String transformedLine = rule.apply(line);
      writer.write(transformedLine);

      isFirstLine = false;
    }

    writer.flush();
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

  // This method is now deprecated as CSVPath handles index resolution
  @Deprecated
  private int findColumnIndex(String[] headers, String selector) {
    // Try to find by column name
    for (int i = 0; i < headers.length; i++) {
      if (headers[i].trim().equalsIgnoreCase(selector.trim())) {
        return i;
      }
    }

    // Try to parse as column index
    try {
      int index = Integer.parseInt(selector);
      if (index >= 0 && index < headers.length) {
        return index;
      }
    } catch (NumberFormatException e) {
      // Not a number, ignore
    }

    return -1;
  }

  /** Resolve column index using CSVPath */
  private int resolveColumnIndex(String[] headers, CSVPath csvPath) {
    String selector = csvPath.toString();

    // Try to parse as numeric index first
    try {
      int index = Integer.parseInt(selector);
      return (index >= 0 && index < headers.length) ? index : -1;
    } catch (NumberFormatException e) {
      // Not numeric, treat as column name
      for (int i = 0; i < headers.length; i++) {
        if (headers[i].trim().equalsIgnoreCase(selector.trim())) {
          return i;
        }
      }
      return -1;
    }
  }
}
