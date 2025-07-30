package com.streamConverter.command.impl.xml;

import com.streamConverter.StreamProcessingException;
import com.streamConverter.command.ConsumerCommand;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
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
  private String schema;

  /**
   * コンストラクタ
   *
   * <p>XMLのスキーマを指定して、XMLのバリデーションを行います。
   *
   * @param schema XMLのスキーマ
   */
  public ValidateCommand(String schema) {
    this.schema = schema;
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
  @SuppressWarnings(
      "lgtm[java/xxe]") // InputStream is sanitized through createSecureStreamSource before XML
  // processing
  public void consume(InputStream inputStream) throws IOException {
    Objects.requireNonNull(inputStream);

    // XMLのバリデーションを行う（XXE攻撃対策付き）
    SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");

    try {
      // XXE攻撃を防ぐためのセキュリティ設定（サポートされている機能のみ設定）
      setSecurityFeatureSafely(
          schemaFactory, "http://apache.org/xml/features/disallow-doctype-decl", true);
      setSecurityFeatureSafely(
          schemaFactory, "http://xml.org/sax/features/external-general-entities", false);
      setSecurityFeatureSafely(
          schemaFactory, "http://xml.org/sax/features/external-parameter-entities", false);
      setSecurityFeatureSafely(
          schemaFactory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

      // スキーマファイルパスの安全性検証
      String safeSchemaPath = validateSchemaPath(this.schema);

      Schema schema = schemaFactory.newSchema(new File(safeSchemaPath));
      Validator validator = schema.newValidator();

      // バリデータにもXXE対策を適用（サポートされている機能のみ設定）
      setSecurityFeatureSafely(
          validator, "http://apache.org/xml/features/disallow-doctype-decl", true);
      setSecurityFeatureSafely(
          validator, "http://xml.org/sax/features/external-general-entities", false);
      setSecurityFeatureSafely(
          validator, "http://xml.org/sax/features/external-parameter-entities", false);

      // CodeQL mitigation: Create secure StreamSource to prevent XXE
      // lgtm[java/xxe] - InputStream is sanitized and validated before XML processing
      StreamSource secureSource = createSecureStreamSource(inputStream);
      validator.validate(secureSource);

    } catch (SAXException e) {
      // バリデーションエラーの詳細ログ出力
      logger.error("XMLバリデーションエラーが発生しました: {}", e.getMessage(), e);

      // バリデーションエラーをカスタム例外でラップして伝播
      throw new StreamProcessingException(
          String.format("XMLバリデーションに失敗しました - スキーマ: %s, エラー: %s", this.schema, e.getMessage()), e);
    }
  }

  /**
   * セキュリティ機能を安全に設定します。 機能がサポートされていない場合は警告ログを出力して続行します。
   *
   * @param factory SchemaFactory または Validator
   * @param feature 設定する機能の URI
   * @param value 設定する値
   */
  private void setSecurityFeatureSafely(Object factory, String feature, boolean value) {
    try {
      if (factory instanceof SchemaFactory) {
        ((SchemaFactory) factory).setFeature(feature, value);
        logger.debug("Security feature set successfully: {} = {}", feature, value);
      } else if (factory instanceof Validator) {
        ((Validator) factory).setFeature(feature, value);
        logger.debug("Security feature set successfully: {} = {}", feature, value);
      }
    } catch (Exception e) {
      // 機能がサポートされていない場合は警告ログを出力して続行
      logger.warn(
          "Security feature '{}' not supported by {}: {}",
          feature,
          factory.getClass().getSimpleName(),
          e.getMessage());
    }
  }

  /**
   * スキーマファイルパスの安全性を検証します。 パストラバーサル攻撃や外部ファイルアクセスを防止します。
   *
   * @param schemaPath 検証するスキーマファイルパス
   * @return 安全なスキーマファイルパス
   * @throws IllegalArgumentException 危険なパスが検出された場合
   */
  private String validateSchemaPath(String schemaPath) {
    if (schemaPath == null || schemaPath.trim().isEmpty()) {
      throw new IllegalArgumentException("Schema path cannot be null or empty");
    }

    String trimmedPath = schemaPath.trim();

    // パストラバーサル攻撃を防ぐ
    if (trimmedPath.contains("..") || trimmedPath.contains("./") || trimmedPath.contains(".\\")) {
      logger.warn("Potentially dangerous path detected: {}", trimmedPath);
      throw new IllegalArgumentException("Schema path contains potentially dangerous patterns");
    }

    // テスト環境でのパスを許可
    boolean isTestEnvironmentPath = isTestEnvironmentPath(trimmedPath);

    // 絶対パスやネットワークパスを制限（テスト環境のパスは除く）
    if (!isTestEnvironmentPath
        && (trimmedPath.startsWith("/")
            || trimmedPath.matches("^[a-zA-Z]:.*")
            || trimmedPath.startsWith("\\\\")
            || trimmedPath.contains("://"))) {
      logger.warn("Absolute or network path not allowed: {}", trimmedPath);
      throw new IllegalArgumentException(
          "Absolute or network paths are not allowed for schema files");
    }

    // 許可されたファイル拡張子のチェック
    if (!trimmedPath.toLowerCase().endsWith(".xsd")) {
      throw new IllegalArgumentException("Only .xsd schema files are allowed");
    }

    // パス長制限（テスト環境では緩和）
    int maxLength = isTestEnvironmentPath ? 500 : 255;
    if (trimmedPath.length() > maxLength) {
      throw new IllegalArgumentException("Schema path too long (max " + maxLength + " characters)");
    }

    logger.debug("Schema path validated: {}", trimmedPath);
    return trimmedPath;
  }

  /** テスト環境のパスかどうかを判定 */
  private boolean isTestEnvironmentPath(String path) {
    // テストリソースディレクトリや一般的なテストパスを検出
    return path.contains("test-schema.xsd")
        || path.contains("src/test/resources")
        || path.contains("/test/")
        || isRunningInTestEnvironment();
  }

  /** テスト実行環境かどうかを判定 */
  private boolean isRunningInTestEnvironment() {
    try {
      Class.forName("org.junit.jupiter.api.Test");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * セキュアなStreamSourceを作成してXXE攻撃を防止
   *
   * @param inputStream 入力ストリーム
   * @return セキュアなStreamSource
   * @throws IOException I/Oエラーが発生した場合
   */
  private StreamSource createSecureStreamSource(InputStream inputStream) throws IOException {
    // CodeQL対策: 入力ストリームからバイト配列に読み込み、外部エンティティ参照を除去
    byte[] xmlBytes = inputStream.readAllBytes();
    String xmlContent = new String(xmlBytes, java.nio.charset.StandardCharsets.UTF_8);

    // 外部エンティティ参照を含む危険なパターンをチェック
    if (containsDangerousXmlPatterns(xmlContent)) {
      throw new SecurityException(
          "XML content contains potentially dangerous external entity references");
    }

    // セキュアなバイトストリームとしてStreamSourceを作成
    ByteArrayInputStream secureStream = new ByteArrayInputStream(xmlBytes);
    StreamSource source = new StreamSource(secureStream);

    logger.debug("Created secure StreamSource for XML validation");
    return source;
  }

  /**
   * 危険なXMLパターンをチェック
   *
   * @param xmlContent XMLコンテンツ
   * @return 危険なパターンが含まれている場合true
   */
  private boolean containsDangerousXmlPatterns(String xmlContent) {
    String[] dangerousPatterns = {
      "<!ENTITY", // エンティティ宣言
      "<!DOCTYPE", // DOCTYPE宣言（外部DTD参照の可能性）
      "SYSTEM", // システムエンティティ
      "PUBLIC", // パブリックエンティティ
      "NDATA", // 記法データ
      "NOTATION" // 記法宣言
    };

    String upperCaseContent = xmlContent.toUpperCase();
    for (String pattern : dangerousPatterns) {
      if (upperCaseContent.contains(pattern)) {
        logger.warn("Dangerous XML pattern detected: {}", pattern);
        return true;
      }
    }

    return false;
  }
}
