package com.streamconverter.command.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.ConsumerCommand;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jsfr.json.JsonSurfer;
import org.jsfr.json.JsonSurferJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JsonSurfer事前検証とJSON Schema検証を組み合わせた2段階検証コマンド
 *
 * <p><strong>重要な実装上の注意:</strong><br>
 * このクラスは入力ストリーム全体をメモリに読み込みます（{@code readAllBytes()}）。 「ストリーミング」という名前ですが、実際には全データをメモリバッファリングします。
 *
 * <p><strong>検証アプローチ:</strong><br>
 * 1. 入力ストリーム全体をバイト配列に読み込み（メモリバッファリング）<br>
 * 2. JsonSurferによる事前検証（構造確認、特定フィールドチェック）<br>
 * 3. JSON Schema検証（DOM構築が必要）
 *
 * <p><strong>メモリ使用について:</strong><br>
 * 全データをメモリに読み込むため、大容量ファイルを処理する場合は十分なヒープメモリが必要です。 {@link JsonValidateCommand}と同等のメモリを消費します。
 *
 * <p><strong>JsonValidateCommandとの違い:</strong><br>
 * JsonSurferによる事前検証を追加で実行しますが、JSON Schema検証は両方で同じです。 事前検証は {@code id}、{@code name}
 * フィールドのチェックに限定されており、 汎用的なスキーマには対応していません。
 *
 * <p><strong>技術的制約:</strong><br>
 * JSON Schema検証は構造全体の検証が必要なため（例: 配列の要素数制約、相互参照）、 完全なストリーミング処理は技術的に不可能です。
 *
 * <p>使用例:
 *
 * <pre>
 * JsonStreamingValidateCommand validator =
 *     JsonStreamingValidateCommand.create("schema/user.json");
 * validator.consume(jsonInputStream);
 * </pre>
 *
 * @see JsonValidateCommand より単純なJSON Schema検証（事前検証なし）
 */
public class JsonStreamingValidateCommand extends ConsumerCommand {
  private static final Logger logger = LoggerFactory.getLogger(JsonStreamingValidateCommand.class);

  private static final SchemaRegistry DEFAULT_SCHEMA_REGISTRY =
      SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);

  private final String schemaPath;
  private final ObjectMapper objectMapper;
  private final SchemaRegistry schemaRegistry;
  private final JsonSurfer surfer;
  private volatile Schema cachedSchema;

  /**
   * JsonStreamingValidateCommandのファクトリメソッド。
   *
   * @param schemaPath JSONスキーマファイルのパス
   * @return 検証済みのスキーマパスに基づくJsonStreamingValidateCommand
   * @throws IllegalArgumentException スキーマパスがnullまたは空の場合
   */
  public static JsonStreamingValidateCommand create(String schemaPath) {
    return create(schemaPath, DEFAULT_SCHEMA_REGISTRY);
  }

  /**
   * カスタムSchemaRegistryを指定してJsonStreamingValidateCommandを生成します。
   *
   * @param schemaPath JSONスキーマファイルのパス
   * @param schemaRegistry 使用するSchemaRegistry
   * @return JsonStreamingValidateCommandのインスタンス
   * @throws IllegalArgumentException スキーマパスがnullまたは空の場合
   * @throws NullPointerException スキーマレジストリがnullの場合
   */
  public static JsonStreamingValidateCommand create(
      String schemaPath, SchemaRegistry schemaRegistry) {
    String validatedSchemaPath = validateSchemaPath(schemaPath);
    SchemaRegistry validatedRegistry =
        Objects.requireNonNull(schemaRegistry, "Schema registry cannot be null");
    return new JsonStreamingValidateCommand(validatedSchemaPath, validatedRegistry);
  }

  private JsonStreamingValidateCommand(String schemaPath, SchemaRegistry schemaRegistry) {
    this.schemaPath = schemaPath;
    this.schemaRegistry = schemaRegistry;
    this.objectMapper = new ObjectMapper();
    this.surfer = JsonSurferJackson.INSTANCE;

    // パフォーマンス改善: 遅延読み込みによりコンストラクタでのI/O操作を回避
    // スキーマの妥当性検証は最初の使用時に実行
  }

  /** スキーマパスの検証 */
  private static String validateSchemaPath(String path) {
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
   * JSONストリーミング検証を実行します
   *
   * @param inputStream 検証対象のJSONデータを含む入力ストリーム
   * @throws IOException I/Oエラーが発生した場合
   * @throws StreamProcessingException JSONバリデーションエラーが発生した場合
   */
  @Override
  public void consume(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");

    logger.info("Starting streaming JSON validation with schema: {}", schemaPath);

    try {
      // 入力ストリームをバッファリングして再利用可能にする
      byte[] inputBuffer;
      try {
        inputBuffer = inputStream.readAllBytes();
      } catch (IOException e) {
        throw new StreamProcessingException("Failed to buffer input stream for validation", e);
      }

      // Phase 1: JsonSurferによる高速ストリーミング事前検証
      try (java.io.ByteArrayInputStream streamingInputStream =
          new java.io.ByteArrayInputStream(inputBuffer)) {
        StreamingValidationResult streamingResult =
            performStreamingValidation(streamingInputStream);

        if (!streamingResult.isValid()) {
          throw new StreamProcessingException(
              String.format(
                  "JSON streaming validation failed: %s", streamingResult.getErrorMessage()));
        }

        logger.debug(
            "Streaming validation passed ({} elements), proceeding to schema validation",
            streamingResult.getElementCount());
      }

      // Phase 2: 事前検証通過時のみJSON Schema検証を実行
      try (java.io.ByteArrayInputStream schemaInputStream =
          new java.io.ByteArrayInputStream(inputBuffer)) {
        performSchemaValidation(schemaInputStream);
        logger.info("JSON validation completed successfully (streaming + schema validation)");
      }

    } catch (StreamProcessingException e) {
      throw e;
    } catch (Exception e) {
      logger.error("JSON streaming validation failed: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format(
              "JSON streaming validation failed - schema: %s, error: %s",
              schemaPath, e.getMessage()),
          e);
    }
  }

  /** JsonSurferを使った完全ストリーミング検証 */
  private StreamingValidationResult performStreamingValidation(InputStream inputStream) {
    AtomicBoolean isValid = new AtomicBoolean(true);
    AtomicInteger elementCount = new AtomicInteger(0);
    StringBuilder errorMessages = new StringBuilder();

    try {
      // 基本構造検証: ルートオブジェクトまたは配列の存在確認
      AtomicBoolean hasRootStructure = new AtomicBoolean(false);

      surfer
          .configBuilder()
          // ルートレベルの検証
          .bind(
              "$",
              (value, context) -> {
                hasRootStructure.set(true);
                logger.debug("Found root JSON structure");
              })
          // 配列要素の計測
          .bind(
              "$[*]",
              (value, context) -> {
                int count = elementCount.incrementAndGet();
                if (count % 1000 == 0) {
                  logger.debug("Processed {} array elements", count);
                }
              })
          // オブジェクトプロパティの基本検証例
          .bind(
              "$..id",
              (value, context) -> {
                if (value == null) {
                  isValid.set(false);
                  errorMessages.append("Found null id field; ");
                  logger.warn("Validation error: null id field");
                }
              })
          .bind(
              "$..name",
              (value, context) -> {
                if (value == null || value.toString().trim().isEmpty()) {
                  isValid.set(false);
                  errorMessages.append("Found empty name field; ");
                  logger.warn("Validation error: empty name field");
                }
              })
          // JsonSurferのエラーハンドリングは別途try-catchで実装
          .buildAndSurf(inputStream);

      if (!hasRootStructure.get()) {
        isValid.set(false);
        errorMessages.append("No valid JSON root structure found; ");
      }

      logger.debug("Streaming validation completed - elements processed: {}", elementCount.get());

    } catch (Exception e) {
      isValid.set(false);
      errorMessages.append("Streaming validation exception: ").append(e.getMessage());
      logger.error("Exception during streaming validation", e);
    }

    return new StreamingValidationResult(
        isValid.get(), errorMessages.toString(), elementCount.get());
  }

  /** JSONスキーマを遅延読み込み（スレッドセーフ） */
  private Schema loadSchema() throws StreamProcessingException {
    if (cachedSchema != null) {
      return cachedSchema;
    }

    synchronized (this) {
      if (cachedSchema != null) {
        return cachedSchema;
      }
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

        cachedSchema = schemaRegistry.getSchema(schemaNode);
        return cachedSchema;

      } catch (StreamProcessingException e) {
        throw e;
      } catch (Exception e) {
        throw new StreamProcessingException("Failed to load JSON schema from: " + schemaPath, e);
      }
    }
  }

  /** JSON Schema検証を実行 */
  private void performSchemaValidation(InputStream inputStream) throws StreamProcessingException {
    try {
      Schema schema = loadSchema();
      JsonNode jsonNode = objectMapper.readTree(inputStream);

      if (jsonNode == null) {
        throw new StreamProcessingException("Failed to parse JSON for schema validation");
      }

      List<Error> validationErrors = schema.validate(jsonNode);

      if (!validationErrors.isEmpty()) {
        StringBuilder errorBuilder = new StringBuilder();
        errorBuilder
            .append("JSON schema validation failed with ")
            .append(validationErrors.size())
            .append(" validation errors:");

        int errorCount = 0;
        for (Error error : validationErrors) {
          errorBuilder.append("\n  ").append(++errorCount).append(". ");
          String instanceLocation =
              error.getInstanceLocation() == null
                  ? "(unknown location)"
                  : error.getInstanceLocation().toString();
          errorBuilder.append("Path: ").append(instanceLocation);
          errorBuilder.append(" - ").append(error.getMessage());
        }

        throw new StreamProcessingException(errorBuilder.toString());
      }

      logger.debug("JSON schema validation completed successfully");

    } catch (StreamProcessingException e) {
      throw e;
    } catch (Exception e) {
      throw new StreamProcessingException("JSON schema validation failed: " + e.getMessage(), e);
    }
  }

  /**
   * スキーマパスを取得
   *
   * @return スキーマファイルのパス
   */
  public String getSchemaPath() {
    return schemaPath;
  }

  /** ストリーミング検証結果を保持するクラス */
  private static class StreamingValidationResult {
    private final boolean valid;
    private final String errorMessage;
    private final int elementCount;

    public StreamingValidationResult(boolean valid, String errorMessage, int elementCount) {
      this.valid = valid;
      this.errorMessage = errorMessage;
      this.elementCount = elementCount;
    }

    public boolean isValid() {
      return valid;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public int getElementCount() {
      return elementCount;
    }
  }
}
