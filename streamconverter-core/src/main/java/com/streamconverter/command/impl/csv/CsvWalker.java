package com.streamconverter.command.impl.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.streamconverter.UncheckedStreamException;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.CSVPath;
import com.streamconverter.path.IColumnSelector;
import com.streamconverter.path.TreePath;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * CSV Walker
 *
 * <p>This class implements command for targeted CSV transformation using column selectors. It
 * identifies specific columns using column names or indices and applies IRule transformations to
 * those columns while preserving the overall CSV structure.
 */
public class CsvWalker implements IStreamCommand {

  private final IColumnSelector columnSelector;
  private final IRule rule;

  /**
   * Constructor for CSV navigation with typed column selector and transformation rule.
   *
   * @param columnSelector the typed IColumnSelector to select columns
   * @param rule the transformation rule to apply to selected columns
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  private CsvWalker(IColumnSelector columnSelector, IRule rule) {
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
   * @param columnSelector the typed IColumnSelector to select columns
   * @param rule the transformation rule to apply to selected columns
   * @return a CsvWalker that transforms values in the specified columns using the given rule
   * @throws IllegalArgumentException if columnSelector or rule is null
   */
  public static CsvWalker create(IColumnSelector columnSelector, IRule rule) {
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
   * @return a CsvWalker that transforms values in the specified column using the given rule
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
    // opencsv の readNext() は下位ストリームの IOException を EOF として握りつぶすため、
    // FaultReportingReader を介して close 時に記録済み障害を必ず再スローする（#784）
    try (FaultReportingReader reader = FaultReportingReader.forUtf8(inputStream);
        CSVReader csvReader = new CSVReader(reader);
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

  private void applyRuleToColumn(CSVReader csvReader, CSVWriter csvWriter)
      throws IOException, CsvValidationException {
    String[] headers = csvReader.readNext();
    if (headers == null) {
      return; // Empty input
    }

    List<Integer> indices = columnSelector.resolve(headers);
    if (indices.isEmpty()) {
      throw new IOException("Column not found: " + columnSelector.toString());
    }

    // Write header (unchanged); applyQuotesToAll=false: only quote when RFC 4180 requires
    csvWriter.writeNext(headers, false);

    // Process data rows
    String[] row;
    while ((row = csvReader.readNext()) != null) {
      for (int idx : indices) {
        if (idx < row.length) {
          row[idx] = applyRule(row[idx], idx);
        }
      }
      // applyQuotesToAll=false: only quote fields that contain delimiters or quotes
      csvWriter.writeNext(row, false);
    }
    csvWriter.flush();
  }

  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.PreserveStackTrace"})
  // IRule.apply() declares no checked exceptions. Rule-layer I/O failures arrive wrapped in the
  // UncheckedStreamException carrier and are unwrapped at this command boundary (#741); the cause
  // already records the rule-site stack trace, so discarding the carrier loses no diagnostics.
  // Any other RuntimeException is wrapped as IOException so the caller's error-handling path is
  // not bypassed.
  private String applyRule(String value, int columnIndex) throws IOException {
    try {
      return rule.apply(value);
    } catch (UncheckedStreamException carrier) {
      throw carrier.getCause();
    } catch (RuntimeException ruleEx) {
      throw new IOException("Rule application failed at column index " + columnIndex, ruleEx);
    }
  }
}
