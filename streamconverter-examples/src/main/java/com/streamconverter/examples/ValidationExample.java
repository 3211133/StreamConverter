package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvValidateCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * バリデーション機能のデモンストレーション
 *
 * <p>JSON、XML、CSVの各形式に対するバリデーション機能と、 ValidationDecoratorの使用方法を示します。
 */
public class ValidationExample {
  private static final Logger logger = LoggerFactory.getLogger(ValidationExample.class);

  /**
   * メインメソッド
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    logger.info("🔍 Validation Feature Demonstration");
    logger.info("====================================\n");

    try {
      // JSON バリデーションのデモ
      demonstrateJsonValidation();

      // CSV バリデーションのデモ
      demonstrateCsvValidation();

      // バリデーション結果の確認デモ
      demonstrateValidationResult();

    } catch (Exception e) {
      logger.error("Validation demonstration failed: {}", e.getMessage(), e);
    }
  }

  /** JSONバリデーションのデモ */
  private static void demonstrateJsonValidation() throws IOException {
    logger.info("📄 JSON Validation Demonstration");
    logger.info("================================");

    // 有効なJSONデータ
    String validJsonData =
        """
        {
          "name": "John Doe",
          "age": 30,
          "email": "john@example.com"
        }
        """;

    // 無効なJSONデータ（フィールド不足）
    String invalidJsonData =
        """
        {
          "name": "Jane Doe"
        }
        """;

    logger.info("📥 Valid JSON data processing...");
    try {
      processWithCsvValidation(validJsonData, "Valid JSON");
      logger.info("✅ Valid JSON processed successfully");
    } catch (Exception e) {
      logger.error("❌ Valid JSON processing failed: {}", e.getMessage());
    }

    logger.info("\n📥 Invalid JSON data processing...");
    try {
      processWithCsvValidation(invalidJsonData, "Invalid JSON");
      logger.info("✅ Invalid JSON processed successfully (unexpected!)");
    } catch (Exception e) {
      logger.info("❌ Invalid JSON processing failed as expected: {}", e.getMessage());
    }

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  /** CSVバリデーションのデモ */
  private static void demonstrateCsvValidation() throws IOException {
    logger.info("📊 CSV Validation Demonstration");
    logger.info("===============================");

    // 有効なCSVデータ
    String validCsvData =
        """
        id,name,email
        1,John Doe,john@example.com
        2,Jane Smith,jane@example.com
        """;

    // 無効なCSVデータ（必須カラム不足）
    String invalidCsvData =
        """
        id,name
        1,John Doe
        2,Jane Smith
        """;

    String[] requiredColumns = {"id", "name", "email"};

    logger.info("📥 Valid CSV data processing...");
    try {
      processWithCsvValidation(validCsvData, requiredColumns, "Valid CSV");
      logger.info("✅ Valid CSV processed successfully");
    } catch (Exception e) {
      logger.error("❌ Valid CSV processing failed: {}", e.getMessage());
    }

    logger.info("\n📥 Invalid CSV data processing...");
    try {
      processWithCsvValidation(invalidCsvData, requiredColumns, "Invalid CSV");
      logger.info("✅ Invalid CSV processed successfully (unexpected!)");
    } catch (Exception e) {
      logger.info("❌ Invalid CSV processing failed as expected: {}", e.getMessage());
    }

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  /** バリデーション結果の詳細確認デモ */
  private static void demonstrateValidationResult() throws IOException {
    logger.info("📋 Validation Result Demonstration");
    logger.info("==================================");

    String csvData =
        """
        id,name,email
        1,John Doe,john@example.com
        """;

    String[] requiredColumns = {"id", "name", "email"};

    // CsvValidateCommandを直接使用してバリデーション実行
    CsvValidateCommand csvValidator = CsvValidateCommand.create(requiredColumns);
    IStreamCommand dataProcessor = (in, out) -> in.transferTo(out);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
      // バリデーション実行
      ByteArrayInputStream csvValidationInputStream =
          new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
      csvValidator.consume(csvValidationInputStream);
      logger.info("📊 CSV validation completed successfully");

      // データ処理実行
      dataProcessor.execute(inputStream, outputStream);
      logger.info("✅ Data processing completed successfully");

    } catch (Exception e) {
      logger.error("❌ Validation or processing failed: {}", e.getMessage());
    }

    logger.info("\n" + "=".repeat(50) + "\n");
  }

  /** CSVバリデーション付きでデータを処理 */
  private static void processWithCsvValidation(String data, String description) throws IOException {
    processWithCsvValidation(data, new String[0], description);
  }

  /** CSVバリデーション付きでデータを処理 */
  private static void processWithCsvValidation(
      String data, String[] requiredColumns, String description) throws IOException {
    logger.debug("Processing {}: {}", description, data.substring(0, Math.min(50, data.length())));

    CsvValidateCommand csvValidator = CsvValidateCommand.create(requiredColumns);
    IStreamCommand processor = (in, out) -> in.transferTo(out);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // パイプラインでバリデーションと処理を実行
    StreamConverter converter = StreamConverter.create(csvValidator, processor);
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.debug("Processing result: {}", result.trim());
  }
}
