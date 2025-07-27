package com.streamConverter.command.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.ConsumerCommand;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSONスキーマバリデーションを行うコマンドクラス
 *
 * <p>JSONスキーマファイルを使用してJSONデータのバリデーションを実行します。 バリデーションエラーが発生した場合は、詳細なエラー情報とともに例外をスローします。
 *
 * <p>使用例:
 *
 * <pre>
 * JsonValidateCommand validator = new JsonValidateCommand("schema/user.json");
 * validator.consume(jsonInputStream);
 * </pre>
 */
public class JsonValidateCommand extends ConsumerCommand {
  private static final Logger logger = LoggerFactory.getLogger(JsonValidateCommand.class);

  private final String schemaPath;
  private final ObjectMapper objectMapper;
  private final JsonSchemaFactory schemaFactory;

  /**
   * コンストラクタ
   *
   * @param schemaPath JSONスキーマファイルのパス
   * @throws IllegalArgumentException スキーマパスがnullまたは空の場合
   */
  public JsonValidateCommand(String schemaPath) {
    this.schemaPath = validateSchemaPath(schemaPath);
    this.objectMapper = new ObjectMapper();
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
  }

  /** スキーマパスの検証 */
  private String validateSchemaPath(String path) {
    if (path == null) {
      throw new IllegalArgumentException("Schema path cannot be null");
    }
    String trimmedPath = path.trim();
    if (trimmedPath.isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }
    return trimmedPath;
  }

  /**
   * JSONバリデーションを実行します
   *
   * @param inputStream 検証対象のJSONデータを含む入力ストリーム
   * @throws IOException I/Oエラーが発生した場合
   * @throws StreamProcessingException JSONバリデーションエラーが発生した場合
   */
  @Override
  public void consume(InputStream inputStream) throws IOException {
    if (inputStream == null) {
      throw new NullPointerException("InputStream cannot be null");
    }

    logger.info("Starting JSON validation with schema: {}", schemaPath);

    try {
      // スキーマファイルの読み込み
      JsonSchema schema = loadSchema();

      // JSONデータの読み込み
      JsonNode jsonNode;
      try {
        jsonNode = objectMapper.readTree(inputStream);
      } catch (Exception e) {
        throw new StreamProcessingException("Failed to parse JSON input", e);
      }

      if (jsonNode == null) {
        throw new StreamProcessingException("Input stream is empty or contains no valid JSON data");
      }

      logger.debug("JSON data loaded successfully, validating against schema");

      // バリデーション実行
      Set<ValidationMessage> validationMessages = schema.validate(jsonNode);

      if (validationMessages.isEmpty()) {
        logger.info("JSON validation completed successfully");
      } else {
        handleValidationErrors(validationMessages);
      }

    } catch (StreamProcessingException e) {
      // 既にラップされた例外はそのまま再スロー
      throw e;
    } catch (Exception e) {
      logger.error("JSON validation failed: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format(
              "JSON validation failed - schema: %s, error: %s", schemaPath, e.getMessage()),
          e);
    }
  }

  /** JSONスキーマを読み込み */
  private JsonSchema loadSchema() throws StreamProcessingException {
    try {
      File schemaFile = new File(schemaPath);
      if (!schemaFile.exists()) {
        throw new StreamProcessingException("Schema file not found: " + schemaPath);
      }

      if (!schemaFile.canRead()) {
        throw new StreamProcessingException("Schema file is not readable: " + schemaPath);
      }

      JsonNode schemaNode;
      try {
        schemaNode = objectMapper.readTree(schemaFile);
      } catch (Exception e) {
        throw new StreamProcessingException("Invalid schema file format: " + schemaPath, e);
      }

      if (schemaNode == null) {
        throw new StreamProcessingException("Schema file is empty: " + schemaPath);
      }

      return schemaFactory.getSchema(schemaNode);

    } catch (StreamProcessingException e) {
      throw e;
    } catch (Exception e) {
      throw new StreamProcessingException("Failed to load JSON schema from: " + schemaPath, e);
    }
  }

  /** バリデーションエラーの処理 */
  private void handleValidationErrors(Set<ValidationMessage> validationMessages) {
    StringBuilder errorBuilder = new StringBuilder();
    errorBuilder
        .append("JSON validation failed with ")
        .append(validationMessages.size())
        .append(" error(s):");

    int errorCount = 0;
    for (ValidationMessage message : validationMessages) {
      errorBuilder.append("\n  ").append(++errorCount).append(". ");
      errorBuilder.append("Path: ").append(message.getInstanceLocation());
      errorBuilder.append(" - ").append(message.getMessage());

      // ログに詳細を出力
      logger.error(
          "JSON validation error - Path: {}, Message: {}",
          message.getInstanceLocation(),
          message.getMessage());
    }

    String errorMessage = errorBuilder.toString();
    logger.error("JSON validation summary: {}", errorMessage);

    throw new StreamProcessingException(
        String.format("JSON validation failed - schema: %s, errors: %s", schemaPath, errorMessage));
  }

  /**
   * スキーマパスを取得
   *
   * @return スキーマファイルのパス
   */
  public String getSchemaPath() {
    return schemaPath;
  }
}
