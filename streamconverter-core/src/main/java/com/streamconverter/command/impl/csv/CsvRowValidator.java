package com.streamconverter.command.impl.csv;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Validates CSV headers and data rows, collecting error messages up to a configurable limit. */
final class CsvRowValidator {

  private static final Logger logger = LoggerFactory.getLogger(CsvRowValidator.class);

  private final Set<String> requiredColumns;
  private final int maxErrorsToReport;

  CsvRowValidator(Set<String> requiredColumns, int maxErrorsToReport) {
    this.requiredColumns = requiredColumns;
    this.maxErrorsToReport = maxErrorsToReport;
  }

  void validateHeaders(String[] headers, List<String> errors) {
    if (headers == null || headers.length == 0) {
      addError(errors, "Header row is empty");
      return;
    }
    checkDuplicateHeaders(headers, errors);
    checkRequiredColumns(headers, errors);
    logger.debug("Header validation completed - {} columns found", headers.length);
  }

  private void checkDuplicateHeaders(String[] headers, List<String> errors) {
    Set<String> seen = new HashSet<>();
    Set<String> duplicates = new HashSet<>();
    for (String header : headers) {
      if (header == null || header.isBlank()) {
        addError(errors, "Header contains empty or null column");
        continue;
      }
      String trimmed = header.trim();
      if (!seen.add(trimmed)) {
        duplicates.add(trimmed);
      }
    }
    if (!duplicates.isEmpty()) {
      addError(errors, "Duplicate column headers: " + duplicates);
    }
  }

  private void checkRequiredColumns(String[] headers, List<String> errors) {
    if (requiredColumns.isEmpty()) {
      return;
    }
    Set<String> headerNames = new HashSet<>();
    for (String header : headers) {
      if (header != null) {
        headerNames.add(header.trim());
      }
    }
    Set<String> missing = new HashSet<>(requiredColumns);
    missing.removeAll(headerNames);
    if (!missing.isEmpty()) {
      addError(errors, "Missing required columns: " + missing);
    }
  }

  void validateDataRow(String[] row, int rowNum, String[] headers, List<String> errors) {
    int expectedColumnCount = headers != null ? headers.length : -1;

    if (row == null) {
      addError(errors, String.format("Data row %d: null row", rowNum));
      return;
    }

    if (expectedColumnCount > 0 && row.length != expectedColumnCount) {
      addError(
          errors,
          String.format(
              "Data row %d has inconsistent number of columns (expected %d, found %d)",
              rowNum, expectedColumnCount, row.length));
      return;
    }

    boolean isEmptyRow = true;
    for (String cell : row) {
      if (cell != null && !cell.isBlank()) {
        isEmptyRow = false;
        break;
      }
    }

    if (isEmptyRow) {
      addError(errors, String.format("Data row %d: Empty data row", rowNum));
    }
  }

  private void addError(List<String> errors, String error) {
    if (errors.size() < maxErrorsToReport) {
      errors.add(error);
    }
  }
}
