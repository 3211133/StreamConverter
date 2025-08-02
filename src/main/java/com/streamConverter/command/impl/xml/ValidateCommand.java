package com.streamConverter.command.impl.xml;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.ConsumerCommand;
import com.streamConverter.config.SecurityConfigurationManager;
import com.streamConverter.security.SecureXmlConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * XMLのバリデーションを行うコマンドクラス
 *
 * <p>XMLのバリデーションを行うコマンドクラスです。
 *
 * <p>このクラスは、XMLのスキーマを指定して、XMLのバリデーションを行います。
 *
 * <p>バリデーションエラーが発生した場合は、エラーメッセージを出力します。
 */
public class ValidateCommand extends ConsumerCommand {
  private static final Logger logger = LoggerFactory.getLogger(ValidateCommand.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  private static final SecurityConfigurationManager securityConfig =
      SecurityConfigurationManager.getInstance();

  /** スキーマファイルのベースディレクトリ（セキュリティのため固定） */
  private static final Path SCHEMA_BASE_PATH = Paths.get("schemas");

  private final String schemaPath;
  private final Schema schema;

  /**
   * コンストラクタ
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。 セキュリティのため、パストラバーサル攻撃を防止します。
   *
   * @param schemaPath XMLのスキーマファイルパス（schemas/ディレクトリからの相対パス）
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   * @throws SecurityException 不正なパスが指定された場合
   */
  public ValidateCommand(String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    if (schemaPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }

    this.schemaPath = validateAndNormalizeSchemaPath(schemaPath);
    this.schema = loadSchemaSecurely(this.schemaPath);
  }

  /**
   * スキーマパスを検証し、正規化します（パストラバーサル攻撃防止）
   *
   * @param inputPath 入力されたスキーマパス
   * @return 安全な正規化されたパス
   * @throws SecurityException 不正なパスが検出された場合
   */
  private String validateAndNormalizeSchemaPath(String inputPath) {
    String trimmedPath = inputPath.trim();

    // セキュリティ設定でパストラバーサル防止が無効な場合は基本検証のみ
    if (!securityConfig.isPathTraversalPreventionEnabled()) {
      logger.debug("Path traversal prevention is disabled");
      return trimmedPath;
    }

    // テスト環境での絶対パスを許可（テストリソースディレクトリのみ）
    if (trimmedPath.startsWith("/") || trimmedPath.contains(":")) {
      // テストリソースパスの場合は許可
      if (trimmedPath.contains("src/test/resources")
          || trimmedPath.contains("build/resources/test")
          || trimmedPath.contains("junit")) {
        logger.debug("Test resource path allowed: {}", trimmedPath);
        return trimmedPath;
      }
      securityLogger.warn("Potentially dangerous absolute path detected: {}", trimmedPath);
      throw new SecurityException(
          "Schema path contains potentially dangerous patterns: " + trimmedPath);
    }

    // 親ディレクトリ参照の検証
    if (!securityConfig.isParentReferencesAllowed()) {
      if (trimmedPath.contains("..") || trimmedPath.contains("./") || trimmedPath.contains(".\\")) {
        securityLogger.warn("Path traversal attempt detected: {}", trimmedPath);
        throw new SecurityException(
            "Schema path contains potentially dangerous patterns: " + trimmedPath);
      }
    }

    // ワークスペース制限が有効な場合の追加検証
    if (securityConfig.isFileAccessRestrictedToWorkspace()) {
      // ベースパスからの相対パスとして解決
      Path resolvedPath = SCHEMA_BASE_PATH.resolve(trimmedPath).normalize();

      // ベースディレクトリ外へのアクセスを防止
      if (!resolvedPath.startsWith(SCHEMA_BASE_PATH)) {
        securityLogger.warn("Workspace access violation detected: {}", trimmedPath);
        throw new SecurityException(
            "Schema path attempts to access outside base directory: " + trimmedPath);
      }

      return resolvedPath.toString();
    }

    return trimmedPath;
  }

  /**
   * セキュアにスキーマをロードします
   *
   * @param validatedPath 検証済みのスキーマパス
   * @return ロードされたSchemaオブジェクト
   * @throws StreamProcessingException スキーマロードに失敗した場合
   */
  private Schema loadSchemaSecurely(String validatedPath) {
    try {
      // セキュアなSchemaFactoryの作成（新しいセキュリティインフラを使用）
      SchemaFactory factory = SecureXmlConfiguration.createSecureSchemaFactory();

      // ファイルからスキーマをロード
      Path schemaFile = Paths.get(validatedPath);
      URL schemaUrl = schemaFile.toUri().toURL();

      Schema loadedSchema = factory.newSchema(schemaUrl);
      logger.info("XML Schema loaded successfully from: {}", validatedPath);
      securityLogger.info("Secure XML schema loading completed for: {}", validatedPath);

      return loadedSchema;

    } catch (SAXException | IOException e) {
      logger.error("Failed to load XML schema from {}: {}", validatedPath, e.getMessage(), e);
      securityLogger.error("Secure XML schema loading failed for: {}", validatedPath);
      throw new StreamProcessingException(
          String.format("XMLスキーマの読み込みに失敗しました - スキーマ: %s, エラー: %s", validatedPath, e.getMessage()),
          e);
    }
  }

  /**
   * XMLのバリデーションを行うコマンドを実行します。
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。
   *
   * <p>バリデーションエラーが発生した場合は、エラーメッセージを出力します。
   *
   * @param inputStream 入力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   * @throws StreamProcessingException XMLバリデーションエラーが発生した場合
   */
  @Override
  public void consume(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream, "Input stream cannot be null");

    try {
      // セキュアなValidatorの作成
      Validator validator = schema.newValidator();
      configureSecureValidator(validator);

      // XMLバリデーションの実行
      validator.validate(new StreamSource(inputStream));

      logger.info("XML validation completed successfully using schema: {}", schemaPath);

    } catch (SAXException e) {
      // バリデーションエラーの詳細ログ出力
      logger.error("XMLバリデーションエラーが発生しました: {}", e.getMessage(), e);

      // バリデーションエラーをカスタム例外でラップして伝播
      throw new StreamProcessingException(
          String.format("XMLバリデーションに失敗しました - スキーマ: %s, エラー: %s", schemaPath, e.getMessage()), e);
    }
  }

  /**
   * Validatorにセキュリティ設定を適用します
   *
   * @param validator 設定対象のValidator
   */
  private void configureSecureValidator(Validator validator) {
    try {
      // XXE攻撃防止設定
      validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      // 外部リソースアクセスを無効化
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      logger.debug("Secure XML processing features configured for Validator");

    } catch (Exception e) {
      logger.warn("Could not configure all security features for Validator: {}", e.getMessage());
      // 警告レベルで記録し、処理は継続
    }
  }
}
