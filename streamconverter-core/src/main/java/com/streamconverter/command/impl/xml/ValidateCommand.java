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
   * <p>セキュリティ: ClassLoaderはリソース名を完全一致で検索するため、パストラバーサル（..）は
   * 単に「存在しないリソース」として扱われます（ClasspathResourceValidator参照）。
   *
   * @param schemaPath クラスパスリソース識別子（例: "schemas/test.xsd", "test-schema.xsd"）
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   */
  public ValidateCommand(String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    if (schemaPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }

    // クラスパスリソース識別子として扱う（セキュリティはClasspathResourceValidatorが担保）
    this.schemaPath = normalizeClasspathPath(schemaPath);
    this.schema = loadSchemaFromClasspath(this.schemaPath);
  }

  /**
   * クラスパスリソース識別子を正規化します
   *
   * <p>先頭のスラッシュはClassLoader互換性のため除去されます。
   *
   * @param inputPath 入力されたクラスパス識別子
   * @return 正規化されたクラスパス識別子
   */
  private String normalizeClasspathPath(String inputPath) {
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
  private Schema loadSchemaFromClasspath(String validatedPath) {
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
