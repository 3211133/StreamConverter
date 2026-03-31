package com.streamconverter.security;

import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XML処理のセキュリティ設定を提供するユーティリティクラス
 *
 * <p>このクラスは、XXE（XML外部エンティティ）攻撃や その他のXML関連のセキュリティ脆弱性を防ぐため、 安全なXML処理設定を提供します。
 *
 * <p>主な機能:
 *
 * <ul>
 *   <li>XXE攻撃防止のためのDocumentBuilderFactory設定
 *   <li>安全なXMLInputFactory設定
 *   <li>安全なTransformerFactory設定
 *   <li>安全なSchemaFactory設定
 *   <li>セキュリティ設定の動的調整
 * </ul>
 *
 * @since 1.0.0
 */
public class SecureXmlConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(SecureXmlConfiguration.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  private SecureXmlConfiguration() {
    // ユーティリティクラスのため、インスタンス化を禁止
  }

  /**
   * XXE攻撃を防ぐように設定されたDocumentBuilderFactoryを作成します
   *
   * @return 安全に設定されたDocumentBuilderFactory
   * @throws ParserConfigurationException 設定に失敗した場合
   */
  public static DocumentBuilderFactory createSecureDocumentBuilderFactory()
      throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    // XXE攻撃防止設定（設定失敗はXXE脆弱性のまま継続するため例外をスロー）
    // DOCTYPE宣言を無効化
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    securityLogger.debug("DOCTYPE declarations disabled");

    // 外部一般エンティティを無効化
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
    // 外部パラメータエンティティを無効化
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    // 外部DTDロードを無効化
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    securityLogger.debug("External entities disabled");

    // JAXP標準の外部リソースアクセス制限
    // setAttribute は未サポート属性時に IllegalArgumentException を投げる可能性があるため
    // ParserConfigurationException にラップして fail-fast を維持する
    try {
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
    } catch (IllegalArgumentException e) {
      ParserConfigurationException pce =
          new ParserConfigurationException(
              "Failed to configure secure XML external access attributes");
      pce.initCause(e);
      throw pce;
    }

    // 追加のセキュリティ設定
    factory.setNamespaceAware(true);
    factory.setValidating(false);

    // XMLリーダーのセキュリティ制限
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);

    securityLogger.info("Secure DocumentBuilderFactory created with XXE protection");
    return factory;
  }

  /**
   * 安全に設定されたDocumentBuilderを作成します
   *
   * @return 安全に設定されたDocumentBuilder
   * @throws ParserConfigurationException 設定に失敗した場合
   */
  public static DocumentBuilder createSecureDocumentBuilder() throws ParserConfigurationException {
    DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
    return factory.newDocumentBuilder();
  }

  /**
   * XXE攻撃を防ぐように設定されたXMLInputFactoryを作成します
   *
   * @return 安全に設定されたXMLInputFactory
   */
  public static XMLInputFactory createSecureXMLInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();

    // 外部エンティティの処理を無効化
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

    // 追加のセキュリティ設定
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
    factory.setProperty(XMLInputFactory.IS_VALIDATING, false);

    securityLogger.info("Secure XMLInputFactory created with XXE protection");
    return factory;
  }

  /**
   * 安全に設定されたTransformerFactoryを作成します
   *
   * <p>XXE攻撃防止設定（設定失敗はXXE脆弱性のまま継続するため例外をスロー）
   *
   * @return 安全に設定されたTransformerFactory
   * @throws IllegalStateException セキュリティ設定の適用に失敗した場合
   */
  public static TransformerFactory createSecureTransformerFactory() {
    TransformerFactory factory = TransformerFactory.newInstance();

    try {
      // XMLTransform攻撃を防ぐための設定
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      // 機能制限
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to configure secure TransformerFactory: XXE protection could not be applied", e);
    }

    securityLogger.info("Secure TransformerFactory created with XXE protection");
    return factory;
  }

  /**
   * 安全に設定されたSchemaFactoryを作成します
   *
   * <p>XXE攻撃防止設定（設定失敗はXXE脆弱性のまま継続するため例外をスロー）
   *
   * @return 安全に設定されたSchemaFactory
   * @throws IllegalStateException セキュリティ設定の適用に失敗した場合
   */
  public static SchemaFactory createSecureSchemaFactory() {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

    try {
      // 外部リソースアクセスを制限
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      // セキュアプロセシング機能を有効化
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Failed to configure secure SchemaFactory: XXE protection could not be applied", e);
    }

    securityLogger.info("Secure SchemaFactory created with XXE protection");
    return factory;
  }

  /**
   * ストリーム処理用に安全なDocumentBuilderを作成します。
   *
   * <p>引数の {@code inputStream} はnullチェックにのみ使用され、実際のXML解析はこのメソッド内では行いません。 返された {@code
   * DocumentBuilder} を使って呼び出し元が解析を行います。
   *
   * @param inputStream nullチェック対象のInputStream（nullの場合は {@link IllegalArgumentException} をスロー）
   * @return ストリーム処理用に安全に設定されたDocumentBuilder
   * @throws ParserConfigurationException XML設定エラーが発生した場合
   * @throws IllegalArgumentException inputStreamがnullの場合
   */
  public static DocumentBuilder createSecureDocumentBuilderForStream(InputStream inputStream)
      throws ParserConfigurationException {

    // 入力検証
    if (inputStream == null) {
      throw new IllegalArgumentException("InputStream cannot be null");
    }

    DocumentBuilder builder = createSecureDocumentBuilder();

    // エラーハンドラーの設定（セキュリティ上の理由により詳細エラー情報を制限）
    builder.setErrorHandler(new SecurityAwareErrorHandler());

    securityLogger.debug("Secure DocumentBuilder created for InputStream processing");
    return builder;
  }
}

/**
 * セキュリティを考慮したXMLエラーハンドラー。
 *
 * <p>詳細なエラー情報の外部漏洩を防ぐため、外部向けメッセージには一般的な内容のみを使用する。 内部ログには行・列情報を記録し、デバッグを可能にする。
 *
 * <p>このクラスは {@link
 * SecureXmlConfiguration#createSecureDocumentBuilderForStream(java.io.InputStream)}
 * 内部での使用を想定してpackage-privateとしており、外部から直接インスタンス化されることを想定していない。
 */
class SecurityAwareErrorHandler implements org.xml.sax.ErrorHandler {
  private static final Logger logger = LoggerFactory.getLogger(SecurityAwareErrorHandler.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  /**
   * XML解析の警告を処理する。
   *
   * <p>セキュリティ上の理由から詳細は抑制し、DEBUGレベルでのみ記録する。
   *
   * @param exception 発生した警告の詳細
   */
  @Override
  public void warning(org.xml.sax.SAXParseException exception) {
    logger.debug("XML parsing warning (details suppressed for security)");
    securityLogger.warn("XML parsing warning detected during secure processing");
  }

  /**
   * XML解析の回復可能エラーを処理する。
   *
   * <p>セキュリティコンテキストでは回復可能エラーも失敗として扱う（不正XMLを後続処理に渡さない）。 内部ログには行・列情報を記録するが、外部エラーメッセージには詳細を含めない。
   *
   * @param exception 発生したエラーの詳細
   * @throws org.xml.sax.SAXException 常にスローし、XML処理を中断する
   */
  @Override
  public void error(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
    logger.warn(
        "XML parsing error at line {}, column {}",
        exception.getLineNumber(),
        exception.getColumnNumber());
    securityLogger.warn("XML parsing error detected during secure processing");
    throw new org.xml.sax.SAXException("XML processing failed: parsing error detected", exception);
  }

  /**
   * XML解析の致命的エラーを処理する。
   *
   * <p>セキュリティ制約違反とみなし、詳細情報を外部に漏らさずに処理を中断する。
   *
   * @param exception 発生した致命的エラーの詳細
   * @throws org.xml.sax.SAXException 常にスローし、XML処理を中断する
   */
  @Override
  public void fatalError(org.xml.sax.SAXParseException exception) throws org.xml.sax.SAXException {
    securityLogger.error("Fatal XML parsing error detected during secure processing");
    throw new org.xml.sax.SAXException("XML processing failed due to security constraints");
  }
}
