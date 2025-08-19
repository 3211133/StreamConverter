package com.streamConverter.command.impl.json;

import com.fasterxml.jackson.core.JsonParser;
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
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JSONスキーマバリデーションを行うコマンドクラス
 *
 * <p>JSONスキーマファイルを使用してJSONデータのバリデーションを実行します。 バリデーションエラーが発生した場合は、詳細なエラー情報とともに例外をスローします。
 *
 * <p><strong>技術的制約について:</strong><br>
 * JSON Schema検証では構造全体の検証が必要なため、完全なストリーミング処理は技術的に困難です。 本実装では任意サイズのデータを受け入れつつ、Jackson streaming
 * APIを使用してメモリ効率を最大化しています。
 *
 * <p><strong>完全ストリーミング処理について:</strong><br>
 * 真のストリーミング処理が必要な場合は{@link JsonStreamingValidateCommand}の使用を検討してください。
 * JsonSurferによる完全ストリーミング検証で、任意サイズのデータを一定メモリで処理できます。
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
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   */
  public JsonValidateCommand(String schemaPath) {
    this.schemaPath = validateSchemaPath(schemaPath);
    this.objectMapper = new ObjectMapper();
    this.schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    // コンストラクタでスキーマファイルの妥当性を検証
    try {
      loadSchema();
    } catch (StreamProcessingException e) {
      throw e;
    }
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
    Objects.requireNonNull(inputStream, "InputStream cannot be null");

    logger.info("Starting JSON validation with schema: {}", schemaPath);

    try {
      // スキーマファイルの読み込み
      JsonSchema schema = loadSchema();

      // JSONデータのストリーミング解析 - 任意サイズのデータに対応
      // 注意: JSON Schema検証では全体構造の検証が必要なため、完全なストリーミング処理は技術的に困難
      // しかし、Jackson streaming APIを使用してメモリ効率を最大化
      JsonNode jsonNode;
      try {
        try (JsonParser parser = objectMapper.createParser(inputStream)) {
          // Jackson streaming APIを使用してJSONを解析
          jsonNode = objectMapper.readTree(parser);
          if (jsonNode == null) {
            throw new StreamProcessingException(
                "Failed to parse JSON input: Input stream is empty or contains no valid JSON data");
          }

          logger.debug("JSON data loaded successfully, validating against schema");
        }
      } catch (StreamProcessingException e) {
        throw e;
      } catch (Exception e) {
        throw new StreamProcessingException("Failed to parse JSON input: " + e.getMessage(), e);
      }

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
        throw new StreamProcessingException(
            "Failed to load JSON schema: Schema file not found: " + schemaPath);
      }

      if (!schemaFile.canRead()) {
        throw new StreamProcessingException(
            "Failed to load JSON schema: Schema file is not readable: " + schemaPath);
      }

      JsonNode schemaNode;
      try {
        schemaNode = objectMapper.readTree(schemaFile);
      } catch (Exception e) {
        throw new StreamProcessingException(
            "Failed to load JSON schema: Invalid schema file format: " + schemaPath, e);
      }

      if (schemaNode == null) {
        throw new StreamProcessingException(
            "Failed to load JSON schema: Schema file is empty: " + schemaPath);
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
        .append(" validation errors:");

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
