package com.streamconverter.command.impl.csv;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
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
  private static final Logger logger = LoggerFactory.getLogger(CsvValidateCommand.class);

  private static final String ERROR_PREFIX = "CSV validation failed: ";
  private static final int MAX_MESSAGE_LENGTH = 1000;

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
    this.hasHeader = hasHeader;
    this.maxErrorsToReport = Math.max(1, maxErrorsToReport);

    this.requiredColumns = new HashSet<>();
    for (final String column : requiredColumns) {
      if (column != null && !column.isBlank()) {
        this.requiredColumns.add(column.trim());
      }
    }

    if (requiredColumns.length == 0) {
      logger.info("No required columns specified, column validation will be skipped");
    } else {
      logger.info("Required columns: {}", this.requiredColumns);
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

    logger.info(
        "Starting CSV validation - hasHeader: {}, requiredColumns: {}",
        hasHeader,
        requiredColumns.size());

    List<String> validationErrors = new ArrayList<>();
    CsvRowValidator rowValidator = new CsvRowValidator(requiredColumns, maxErrorsToReport);
    boolean empty = readAndValidate(inputStream, rowValidator, validationErrors);

    if (empty) {
      throw new StreamProcessingException(ERROR_PREFIX + "CSV file is empty");
    }
    if (!validationErrors.isEmpty()) {
      handleValidationErrors(validationErrors);
    }
    logger.info("CSV validation completed successfully");
  }

  private boolean readAndValidate(
      InputStream inputStream, CsvRowValidator rowValidator, List<String> validationErrors)
      throws IOException {
    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        CSVReader csvReader = new CSVReader(reader)) {

      String[] headers = null;
      if (hasHeader) {
        headers = csvReader.readNext();
        if (headers == null) {
          return true;
        }
        rowValidator.validateHeaders(headers, validationErrors);
      }

      return validateDataRows(csvReader, rowValidator, headers, validationErrors);

    } catch (CsvValidationException e) {
      logger.error("CSV parsing error: {}", e.getMessage(), e);
      throw new StreamProcessingException("Failed to parse CSV: " + e.getMessage(), e);
    } catch (IOException e) {
      logger.error("CSV validation failed: {}", e.getMessage(), e);
      throw new StreamProcessingException("Failed to parse CSV: " + e.getMessage(), e);
    }
  }

  private boolean validateDataRows(
      CSVReader csvReader, CsvRowValidator rowValidator, String[] headers, List<String> errors)
      throws IOException, CsvValidationException {
    String[] row;
    int rowNum = 1;
    boolean hasDataRows = false;

    while ((row = csvReader.readNext()) != null) {
      hasDataRows = true;
      rowValidator.validateDataRow(row, rowNum++, headers, errors);
    }

    if (hasHeader && !hasDataRows) {
      errors.add("CSV file contains only header, no data rows found");
    }
    return !hasHeader && !hasDataRows;
  }

  /** バリデーションエラーの処理 */
  private void handleValidationErrors(List<String> errors) throws IOException {
    StringBuilder errorBuilder = new StringBuilder();
    errorBuilder.append("CSV validation failed with ").append(errors.size()).append(" error(s):");

    for (int i = 0; i < errors.size(); i++) {
      errorBuilder.append("\n  ").append(i + 1).append(". ").append(errors.get(i));
      logger.error("CSV validation error {}: {}", i + 1, errors.get(i));
    }

    String errorMessage = errorBuilder.toString();
    logger.error("CSV validation summary: {}", errorMessage);

    int maxBodyLength = MAX_MESSAGE_LENGTH - ERROR_PREFIX.length();
    String body = errorMessage;
    if (errorMessage.length() > maxBodyLength) {
      body = errorMessage.substring(0, maxBodyLength - 3) + "...";
      logger.warn(
          "Error message truncated due to length (original: {} chars)", errorMessage.length());
    }

    throw new StreamProcessingException(ERROR_PREFIX + body);
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
