package com.streamConverter.command.impl;

import com.streamConverter.command.AbstractStreamCommand;
import com.streamConverter.command.rule.IRule;
import com.streamConverter.command.rule.PassThroughRule;
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

  private String columnSelector;
  private IRule rule;
  private int columnIndex = -1;

  /**
   * Constructor for CSV navigation with column selector and transformation rule.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   * @param rule the transformation rule to apply to selected column
   * @throws IllegalArgumentException if rule is null
   */
  public CsvNavigateCommand(String columnSelector, IRule rule) {
    if (rule == null) {
      throw new IllegalArgumentException("Rule cannot be null");
    }
    this.columnSelector = columnSelector;
    this.rule = rule;
  }

  /**
   * Constructor for CSV navigation with column selector using PassThroughRule.
   *
   * @param columnSelector the column name or index to select (e.g., "name", "2")
   */
  public CsvNavigateCommand(String columnSelector) {
    this(columnSelector, new PassThroughRule());
  }

  /** Default constructor - processes all columns with PassThroughRule. */
  public CsvNavigateCommand() {
    this(null, new PassThroughRule());
  }

  @Override
  protected String getCommandDetails() {
    if (columnSelector != null) {
      return String.format(
          "CsvNavigateCommand(columnSelector='%s', rule='%s')",
          columnSelector, rule.getClass().getSimpleName());
    } else {
      return String.format(
          "CsvNavigateCommand(all columns, rule='%s')", rule.getClass().getSimpleName());
    }
  }

  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
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

  /** Apply transformation rule to entire CSV content */
  private void applyRuleToEntireCsv(BufferedReader reader, Writer writer) throws IOException {
    StringBuilder csvBuilder = new StringBuilder();
    String line;

    // Read entire CSV content
    while ((line = reader.readLine()) != null) {
      csvBuilder.append(line).append(System.lineSeparator());
    }

    // Apply rule to entire content
    String transformedCsv = rule.apply(csvBuilder.toString());
    writer.write(transformedCsv);
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
    columnIndex = findColumnIndex(headers, columnSelector);
    if (columnIndex == -1) {
      throw new IllegalArgumentException("Column not found: " + columnSelector);
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

      // Write entire row with transformed column
      writer.write(String.join(",", values));
      writer.write(System.lineSeparator());
    }
    writer.flush();
  }

  private String[] parseCSVLine(String line) {
    return line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
  }

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
}
