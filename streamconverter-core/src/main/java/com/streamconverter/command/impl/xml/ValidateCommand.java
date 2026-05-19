package com.streamconverter.command.impl.xml;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.ConsumerCommand;
import com.streamconverter.security.SecureXmlConfiguration;
import com.streamconverter.util.ClasspathResourceValidator;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
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

  private final String schemaPath;
  private final Schema schema;

  /**
   * コンストラクタ
   *
   * <p>クラスパスからXMLスキーマを読み込み、バリデーションコマンドを作成します。
   *
   * <p>セキュリティ: ClassLoaderはクラスパス内でパス正規化を行います（例: "hoge/../fuga" → "fuga"）。
   * ただし、クラスパス境界外へのアクセスは不可能です（"../etc/passwd" → リソース未発見）。
   *
   * @param schemaPath クラスパスリソース識別子（例: "schemas/test.xsd", "test-schema.xsd"）
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   */
  private ValidateCommand(String schemaPath, Schema schema) {
    this.schemaPath = schemaPath;
    this.schema = schema;
  }

  /**
   * Factory method for creating a ValidateCommand.
   *
   * @param schemaPath クラスパスリソース識別子（例: "schemas/test.xsd", "test-schema.xsd"）
   * @return a ValidateCommand instance
   * @throws NullPointerException スキーマパスがnullの場合
   * @throws IllegalArgumentException スキーマパスが空の場合
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   */
  public static ValidateCommand create(String schemaPath) throws IOException {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    if (schemaPath.isBlank()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }
    String normalizedPath = normalizeClasspathPath(schemaPath);
    Schema schema = loadSchemaFromClasspath(normalizedPath);
    return new ValidateCommand(normalizedPath, schema);
  }

  /**
   * クラスパスリソース識別子を正規化します
   *
   * <p>先頭のスラッシュはClassLoader互換性のため除去されます。
   *
   * @param inputPath 入力されたクラスパス識別子
   * @return 正規化されたクラスパス識別子
   */
  private static String normalizeClasspathPath(String inputPath) {
    String trimmed = inputPath.trim();
    // Remove leading slash for ClassLoader compatibility
    if (trimmed.startsWith("/")) {
      trimmed = trimmed.substring(1);
    }
    return trimmed;
  }

  /**
   * セキュアにスキーマをロードします
   *
   * @param validatedPath 検証済みのスキーマパス
   * @return ロードされたSchemaオブジェクト
   * @throws StreamProcessingException スキーマロードに失敗した場合
   */
  private static Schema loadSchemaFromClasspath(String validatedPath) throws IOException {
    try {
      // セキュアなSchemaFactoryの作成（新しいセキュリティインフラを使用）
      SchemaFactory factory = SecureXmlConfiguration.createSecureSchemaFactory();

      // クラスパスからスキーマをロード（パストラバーサル不要・JAR対応）
      URL schemaUrl = ClasspathResourceValidator.getResourceUrl(validatedPath);

      Schema loadedSchema = factory.newSchema(schemaUrl);
      logger.info("XML Schema loaded successfully from: {}", validatedPath);
      securityLogger.info("Secure XML schema loading completed for: {}", validatedPath);

      return loadedSchema;

    } catch (SAXException | IllegalArgumentException e) {
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
   * @throws StreamProcessingException XXE防止設定の適用失敗またはXMLバリデーションエラーが発生した場合
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
   * @throws SAXException セキュリティ設定に失敗した場合（XXE脆弱性のまま継続しないためスロー）
   */
  private void configureSecureValidator(Validator validator) throws SAXException {
    // XXE攻撃防止設定（設定失敗はXXE脆弱性のまま継続するため例外をスロー）
    validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

    // 外部リソースアクセスを無効化
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
    validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    logger.debug("Secure XML processing features configured for Validator");
  }
}
