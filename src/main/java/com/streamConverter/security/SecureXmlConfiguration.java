package com.streamConverter.security;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.SchemaFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * XMLセキュリティ設定を統一管理するクラス
 *
 * <p>XXE攻撃、XML爆弾攻撃、その他のXML関連脆弱性を防ぐための セキュアな設定を一元管理します。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>XXE攻撃防止のための包括的設定
 *   <li>XML爆弾攻撃防止のためのリソース制限
 *   <li>外部エンティティ参照の無効化
 *   <li>DTD処理の無効化
 * </ul>
 *
 * @author StreamConverter Security Team
 * @version 1.0.0
 * @since 2025-07-30
 */
@Component
public class SecureXmlConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(SecureXmlConfiguration.class);

  // セキュリティ設定定数
  private static final String DISALLOW_DOCTYPE_DECL =
      "http://apache.org/xml/features/disallow-doctype-decl";
  private static final String EXTERNAL_GENERAL_ENTITIES =
      "http://xml.org/sax/features/external-general-entities";
  private static final String EXTERNAL_PARAMETER_ENTITIES =
      "http://xml.org/sax/features/external-parameter-entities";
  private static final String LOAD_EXTERNAL_DTD =
      "http://apache.org/xml/features/nonvalidating/load-external-dtd";
  private static final String LOAD_DTD_GRAMMAR =
      "http://apache.org/xml/features/nonvalidating/load-dtd-grammar";
  private static final String LOAD_EXTERNAL_SUBSET =
      "http://apache.org/xml/features/nonvalidating/load-external-subset";

  /**
   * セキュアなDocumentBuilderFactoryを作成
   *
   * <p>XXE攻撃防止、リソース制限、外部参照無効化などの セキュリティ設定を適用したDocumentBuilderFactoryを返します。
   *
   * @return セキュリティ設定済みのDocumentBuilderFactory
   * @throws ParserConfigurationException パーサー設定エラーが発生した場合
   */
  public DocumentBuilderFactory createSecureDocumentBuilderFactory()
      throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);
    factory.setIgnoringComments(true);

    logger.debug("Configuring secure DocumentBuilderFactory");

    try {
      // XXE and other external entity attacks
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);

      logger.info("Secure DocumentBuilderFactory configured successfully");

    } catch (ParserConfigurationException e) {
      logger.error("Failed to configure secure DocumentBuilderFactory", e);
      // Re-throw the original exception to avoid losing context
      throw e;
    }

    return factory;
  }

  /**
   * セキュアなXMLInputFactoryを作成
   *
   * <p>ストリーミングXML処理用のセキュアな設定を適用した XMLInputFactoryを返します。
   *
   * @return セキュリティ設定済みのXMLInputFactory
   */
  public XMLInputFactory createSecureXMLInputFactory() {
    XMLInputFactory factory = XMLInputFactory.newInstance();

    logger.debug("Configuring secure XMLInputFactory");

    // Disable DTDs and external entities to prevent XXE
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

    // Note: IS_REPLACING_ENTITY_REFERENCES is not a standard property and can be ignored.
    // The two properties above are sufficient for XXE prevention in StAX parsers.

    logger.info("Secure XMLInputFactory configured successfully");
    return factory;
  }

  /**
   * セキュアなSchemaFactoryを作成
   *
   * <p>XML Schema検証用のセキュアな設定を適用した SchemaFactoryを返します。
   *
   * @return セキュリティ設定済みのSchemaFactory
   */
  public SchemaFactory createSecureSchemaFactory() {
    SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    logger.debug("Configuring secure SchemaFactory");
    try {
      // Prevent external entity access to block XXE
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      logger.info("Secure SchemaFactory configured successfully");
    } catch (Exception e) {
      // Some properties might not be supported by all JAXP implementations.
      // Log a warning but don't fail, as the most critical protections are in the parsers.
      logger.warn(
          "Could not set some properties on SchemaFactory (may not be supported): {}",
          e.getMessage());
    }
    return factory;
  }

  /**
   * セキュアなSchemaFactoryを設定します。
   *
   * @param factory 設定対象のSchemaFactory
   */
  public static void configureSchemaFactory(SchemaFactory factory) {
    try {
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      // 外部スキーマの参照を無効化
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      logger.info("Secure SchemaFactory configured successfully.");
    } catch (Exception e) {
      logger.error("Failed to configure secure SchemaFactory: {}", e.getMessage(), e);
    }
  }

  /**
   * セキュアなValidatorを設定します。
   *
   * @param validator 設定対象のValidator
   */
  public static void configureValidator(javax.xml.validation.Validator validator) {
    try {
      validator.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      logger.info("Secure Validator configured successfully.");
    } catch (org.xml.sax.SAXException e) {
      logger.error("Failed to configure secure Validator: {}", e.getMessage(), e);
    }
  }
}
