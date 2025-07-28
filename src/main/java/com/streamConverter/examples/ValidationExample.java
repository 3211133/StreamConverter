package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.ValidationDecorator;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.validation.ValidationResult;
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

    // ValidationDecoratorを直接使用してValidationResultを取得
    SampleStreamCommand baseCommand = new SampleStreamCommand("validation-demo");
    ValidationDecorator decorator = new ValidationDecorator(baseCommand, requiredColumns);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    try {
      decorator.execute(inputStream, outputStream);

      ValidationResult result = decorator.getLastValidationResult();
      if (result != null) {
        logger.info("📊 Validation Result Details:");
        logger.info("   Type: {}", result.getValidationType());
        logger.info("   Valid: {}", result.isValid());
        logger.info("   Schema: {}", result.getSchemaPath());
        logger.info("   Execution Time: {} ms", result.getExecutionTimeMillis());
        logger.info("   Error Count: {}", result.getErrorCount());
        logger.info("   Warning Count: {}", result.getWarningCount());
        logger.info("   Validation Time: {}", result.getValidationTime());

        if (!result.getErrors().isEmpty()) {
          logger.info("   Errors:");
          for (int i = 0; i < result.getErrors().size(); i++) {
            logger.info("     {}. {}", i + 1, result.getErrors().get(i));
          }
        }
      }

      logger.info("✅ Validation result retrieved successfully");

    } catch (Exception e) {
      logger.error("❌ Validation result demonstration failed: {}", e.getMessage());
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

    SampleStreamCommand baseCommand = new SampleStreamCommand("csv-processor");
    ValidationDecorator decorator = new ValidationDecorator(baseCommand, requiredColumns);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    StreamConverter converter = StreamConverter.create(decorator);
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.debug("Processing result: {}", result.trim());
  }
}
