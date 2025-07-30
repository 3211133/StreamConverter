package com.streamConverter.command.impl.xml;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.ConsumerCommand;
import com.streamConverter.security.SecureXmlConfiguration;
import com.streamConverter.security.SecureXmlProcessor;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
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
  private final String schemaPath;
  private final Schema schema;
  private final SecureXmlProcessor secureXmlProcessor;

  /**
   * コンストラクタ
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。
   *
   * @param schemaPath XMLのスキーマファイルのパス
   * @throws StreamProcessingException スキーマファイルの読み込みに失敗した場合
   */
  public ValidateCommand(String schemaPath) {
    Objects.requireNonNull(schemaPath, "Schema path cannot be null");
    if (schemaPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be empty");
    }
    this.schemaPath = schemaPath;
    this.secureXmlProcessor = new SecureXmlProcessor();

    try {
      // セキュアなSchemaFactoryの作成
      SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      SecureXmlConfiguration.configureSchemaFactory(factory); // セキュリティ設定を適��

      // スキーマのロード
      URL schemaUrl = new File(schemaPath).toURI().toURL();
      this.schema = factory.newSchema(schemaUrl);
      logger.info("XML Schema loaded successfully from: {}", schemaPath);
    } catch (SAXException | IOException e) {
      logger.error("Failed to load XML schema from {}: {}", schemaPath, e.getMessage(), e);
      throw new StreamProcessingException(
          String.format("XMLスキーマの読み込みに失敗しました - スキーマ: %s, エラー: %s", schemaPath, e.getMessage()), e);
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
      // XML爆弾検出と基本的な構造検証 (SecureXmlProcessorを使用)
      String xmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      if (!secureXmlProcessor.validateXmlStructure(xmlContent)) {
        throw new StreamProcessingException(String.format("XML構造検証に失敗しました - スキーマ: %s", schemaPath));
      }

      // XSDスキーマバリデーション
      Validator validator = schema.newValidator();
      SecureXmlConfiguration.configureValidator(validator); // セキュリティ設定を適用

      try (InputStream validationInputStream =
          new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8))) {
        Source xmlSource = new StreamSource(validationInputStream);
        validator.validate(xmlSource);
      }

      logger.info("XML構造検証が成功しました - スキーマ: {}", schemaPath);

    } catch (SAXException e) {
      logger.error("XMLバリデーションエラーが発生しました: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format("XMLバリデーションに失敗しました - スキーマ: %s, エラー: %s", schemaPath, e.getMessage()), e);
    } catch (SecurityException e) {
      logger.error("XMLセキュリティ脅威を検出しました: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format("XMLセキュリティ検証に失敗しました - %s", e.getMessage()), e);
    } catch (IOException e) {
      logger.error("XML検証中にIOエラーが発生しました: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format("XML検証中にIOエラーが発生しました - スキーマ: %s, エラー: %s", schemaPath, e.getMessage()), e);
    } catch (Exception e) {
      logger.error("予期せぬXML検証エラーが発生しました: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          String.format("予期せぬXML検証エラーが発生しました - スキーマ: %s, エラー: %s", schemaPath, e.getMessage()), e);
    }
  }
}
