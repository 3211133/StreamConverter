package com.streamconverter.command.impl.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.ConsumerCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CSVデータのバリデーションを行うコマンドクラス
 *
 * <p>CSVデータの構造とデータ妥当性を検証します。以下の項目をチェックします：
 *
 * <ul>
 *   <li>必須カラムの存在
 *   <li>ヘッダー行の妥当性
 *   <li>データ行の整合性
 *   <li>重複ヘッダーの検出
 * </ul>
 *
 * <p>使用例:
 *
 * <pre>
 * String[] requiredColumns = {"id", "name", "email"};
 * CsvValidateCommand validator = CsvValidateCommand.create(requiredColumns);
 * validator.consume(csvInputStream);
 * </pre>
 */
public class CsvValidateCommand extends ConsumerCommand {
  private static final Logger LOGGER = LoggerFactory.getLogger(CsvValidateCommand.class);

  private final Set<String> requiredColumns;
  private final boolean hasHeader;
  private final int maxErrorsToReport;

  /**
   * 必須カラムを指定するコンストラクタ（ヘッダー行ありと仮定）
   *
   * @param requiredColumns 必須カラム名の配列
   * @throws IllegalArgumentException 必須カラムがnullの場合
   */
  private CsvValidateCommand(final String... requiredColumns) {
    this(true, 10, requiredColumns);
  }

  /**
   * 詳細設定を指定するコンストラクタ
   *
   * @param hasHeader ヘッダー行の存在フラグ
   * @param maxErrorsToReport 報告する最大エラー数
   * @param requiredColumns 必須カラム名の配列
   */
  private CsvValidateCommand(
      final boolean hasHeader, final int maxErrorsToReport, final String... requiredColumns) {
    super();
    this.hasHeader = hasHeader;
    this.maxErrorsToReport = Math.max(1, maxErrorsToReport);

    this.requiredColumns = new HashSet<>();
    for (final String column : requiredColumns) {
      if (column != null && !column.isBlank()) {
        this.requiredColumns.add(column.trim());
      }
    }

    if (requiredColumns.length == 0) {
      LOGGER.info("No required columns specified, column validation will be skipped");
    } else {
      LOGGER.info("Required columns: {}", this.requiredColumns);
    }
  }

  /**
   * 必須カラムを指定してCsvValidateCommandを作成（ヘッダー行ありと仮定）
   *
   * @param requiredColumns 必須カラム名の配列
   * @return a CsvValidateCommand instance
   * @throws IllegalArgumentException 必須カラムがnullの場合
   */
  public static CsvValidateCommand create(final String... requiredColumns) {
    if (requiredColumns == null) {
      throw new IllegalArgumentException("Required columns cannot be null");
    }
    return new CsvValidateCommand(requiredColumns);
  }

  /**
   * 詳細設定を指定してCsvValidateCommandを作成
   *
   * @param hasHeader ヘッダー行の存在フラグ
   * @param maxErrorsToReport 報告する最大エラー数
   * @param requiredColumns 必須カラム名の配列
   * @return a CsvValidateCommand instance
   * @throws IllegalArgumentException requiredColumnsがnullの場合
   */
  public static CsvValidateCommand create(
      final boolean hasHeader, final int maxErrorsToReport, final String... requiredColumns) {
    if (requiredColumns == null) {
      throw new IllegalArgumentException("Required columns cannot be null");
    }
    return new CsvValidateCommand(hasHeader, maxErrorsToReport, requiredColumns);
  }

  /**
   * CSVバリデーションを実行します
   *
   * @param inputStream 検証対象のCSVデータを含む入力ストリーム
   * @throws IOException I/Oエラーが発生した場合
   * @throws StreamProcessingException CSVバリデーションエラーが発生した場合
   */
  @Override
  public void consume(final InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");

    LOGGER.info(
        "Starting CSV validation - hasHeader: {}, requiredColumns: {}",
        hasHeader,
        requiredColumns.size());

    List<String> validationErrors = new ArrayList<>();

    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        CSVReader csvReader = new CSVReader(reader)) {

      List<String[]> allRows = csvReader.readAll();

      if (allRows.isEmpty()) {
        throw new StreamProcessingException("CSV validation failed: CSV file is empty");
      }

      LOGGER.debug("Read {} rows from CSV", allRows.size());

      String[] headers = null;
      int dataStartRow = 0;

      if (hasHeader) {
        headers = allRows.get(0);
        dataStartRow = 1;
        validateHeaders(headers, validationErrors);
      }

      // Check if CSV has only header row (no data rows)
      if (hasHeader && allRows.size() == 1) {
        validationErrors.add("CSV file contains only header, no data rows found");
      }

      validateDataRows(allRows, dataStartRow, headers, validationErrors);

      if (!validationErrors.isEmpty()) {
        handleValidationErrors(validationErrors);
      }

      LOGGER.info("CSV validation completed successfully");

    } catch (CsvException e) {
      LOGGER.error("CSV parsing error: {}", e.getMessage(), e);
      throw new StreamProcessingException("Failed to parse CSV: " + e.getMessage(), e);
    } catch (StreamProcessingException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error("CSV validation failed: {}", e.getMessage(), e);
      throw new StreamProcessingException("Failed to parse CSV: " + e.getMessage(), e);
    }
  }

  /** ヘッダー行のバリデーション */
  private void validateHeaders(String[] headers, List<String> errors) {
    if (headers == null || headers.length == 0) {
      errors.add("Header row is empty");
      return;
    }

    // 重複ヘッダーチェック
    Set<String> headerSet = new HashSet<>();
    Set<String> duplicates = new HashSet<>();

    for (String header : headers) {
      if (header == null || header.trim().isEmpty()) {
        errors.add("Header contains empty or null column");
        continue;
      }

      String trimmedHeader = header.trim();
      if (!headerSet.add(trimmedHeader)) {
        duplicates.add(trimmedHeader);
      }
    }

    if (!duplicates.isEmpty()) {
      errors.add("Duplicate column headers: " + duplicates);
    }

    // 必須カラムの存在チェック
    if (!requiredColumns.isEmpty()) {
      Set<String> headerNames = new HashSet<>();
      for (String header : headers) {
        if (header != null) {
          headerNames.add(header.trim());
        }
      }

      Set<String> missingColumns = new HashSet<>(requiredColumns);
      missingColumns.removeAll(headerNames);

      if (!missingColumns.isEmpty()) {
        errors.add("Missing required columns: " + missingColumns);
      }
    }

    LOGGER.debug("Header validation completed - {} columns found", headers.length);
  }

  /** データ行のバリデーション */
  private void validateDataRows(
      List<String[]> allRows, int startRow, String[] headers, List<String> errors) {
    int expectedColumnCount = headers != null ? headers.length : -1;
    int validRows = 0;
    int errorRows = 0;

    for (int i = startRow; i < allRows.size(); i++) {
      String[] row = allRows.get(i);
      int rowNumber = i + 1; // 1-based row number

      if (row == null) {
        addError(errors, String.format("Row %d: null row", rowNumber));
        errorRows++;
        continue;
      }

      // カラム数チェック
      if (expectedColumnCount > 0 && row.length != expectedColumnCount) {
        int dataRowNumber = i + 1 - startRow; // Data row number (1-based, excluding header)
        addError(
            errors,
            String.format(
                "Row %d has inconsistent number of columns (expected %d, found %d)",
                dataRowNumber, expectedColumnCount, row.length));
        errorRows++;
        continue;
      }

      // 空行チェック
      boolean isEmptyRow = true;
      for (String cell : row) {
        if (cell != null && !cell.trim().isEmpty()) {
          isEmptyRow = false;
          break;
        }
      }

      if (isEmptyRow) {
        addError(errors, String.format("Row %d: Empty data row", rowNumber));
        errorRows++;
        continue;
      }

      validRows++;
    }

    LOGGER.debug("Data validation completed - {} valid rows, {} error rows", validRows, errorRows);

    if (validRows == 0 && startRow < allRows.size()) {
      errors.add("No valid data rows found");
    }
  }

  /** エラーメッセージを追加（最大数制限あり） */
  private void addError(List<String> errors, String error) {
    if (errors.size() < maxErrorsToReport) {
      errors.add(error);
    } else if (errors.size() == maxErrorsToReport) {
      errors.add("... and more errors (limit reached)");
    }
  }

  /** バリデーションエラーの処理 */
  private void handleValidationErrors(List<String> errors) {
    StringBuilder errorBuilder = new StringBuilder();
    errorBuilder.append("CSV validation failed with ").append(errors.size()).append(" error(s):");

    for (int i = 0; i < errors.size(); i++) {
      errorBuilder.append("\n  ").append(i + 1).append(". ").append(errors.get(i));
      LOGGER.error("CSV validation error {}: {}", i + 1, errors.get(i));
    }

    String errorMessage = errorBuilder.toString();
    LOGGER.error("CSV validation summary: {}", errorMessage);

    // エラーメッセージが長すぎる場合は切り詰める（可読性向上のため）
    String finalErrorMessage = errorMessage;
    if (errorMessage.length() > 1000) {
      finalErrorMessage = errorMessage.substring(0, 997) + "...";
      LOGGER.warn(
          "Error message truncated due to length (original: {} chars)", errorMessage.length());
    }

    throw new StreamProcessingException("CSV validation failed: " + finalErrorMessage);
  }

  /**
   * 必須カラムを取得
   *
   * @return 必須カラムのセット
   */
  public Set<String> getRequiredColumns() {
    return new HashSet<>(requiredColumns);
  }

  /**
   * ヘッダー行の存在フラグを取得
   *
   * @return ヘッダー行ありの場合true
   */
  public boolean hasHeaderRow() {
    return hasHeader;
  }

  /**
   * 最大エラー報告数を取得
   *
   * @return 最大エラー報告数
   */
  public int getMaxErrorsToReport() {
    return maxErrorsToReport;
  }
}
