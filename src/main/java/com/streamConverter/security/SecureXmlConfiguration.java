package com.streamConverter.security;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
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

    logger.debug("Configuring secure DocumentBuilderFactory");

    try {
      // セキュア処理の有効化
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

      // XXE攻撃防止の包括的設定
      setSecureFeature(factory, DISALLOW_DOCTYPE_DECL, true);
      setSecureFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
      setSecureFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
      setSecureFeature(factory, LOAD_EXTERNAL_DTD, false);
      setSecureFeature(factory, LOAD_DTD_GRAMMAR, false);
      setSecureFeature(factory, LOAD_EXTERNAL_SUBSET, false);

      // XInclude無効化
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);

      // 外部参照の完全無効化
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

      // リソース制限の適用
      applyResourceLimits(factory);

      logger.info("Secure DocumentBuilderFactory configured successfully");

    } catch (Exception e) {
      logger.error("Failed to configure secure DocumentBuilderFactory", e);
      throw new ParserConfigurationException(
          "Could not configure secure XML parser: " + e.getMessage());
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

    // XXE攻撃防止
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

    // リソース制限（利用可能な場合のみ）
    setPropertySafely(factory, "javax.xml.stream.maxEntityCount", 1);
    setPropertySafely(factory, "com.sun.xml.internal.stream.XMLInputFactory.maxEntityCount", 1);

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

    // XXE攻撃防止設定
    setSecureFeature(factory, DISALLOW_DOCTYPE_DECL, true);
    setSecureFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
    setSecureFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
    setSecureFeature(factory, LOAD_EXTERNAL_DTD, false);

    // 外部参照無効化
    setPropertySafely(factory, XMLConstants.ACCESS_EXTERNAL_DTD, "");
    setPropertySafely(factory, XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

    logger.info("Secure SchemaFactory configured successfully");
    return factory;
  }

  /**
   * Validatorにセキュリティ設定を適用
   *
   * @param validator 設定対象のValidator
   */
  public void configureSecureValidator(Validator validator) {
    logger.debug("Configuring secure Validator");

    // XXE攻撃防止設定
    setSecureFeature(validator, DISALLOW_DOCTYPE_DECL, true);
    setSecureFeature(validator, EXTERNAL_GENERAL_ENTITIES, false);
    setSecureFeature(validator, EXTERNAL_PARAMETER_ENTITIES, false);

    logger.debug("Secure Validator configured successfully");
  }

  /**
   * DocumentBuilderFactoryにリソース制限を適用
   *
   * @param factory 設定対象のDocumentBuilderFactory
   */
  private void applyResourceLimits(DocumentBuilderFactory factory) {
    try {
      // JDK 8u45以降で利用可能なリソース制限
      factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxOccur", 100);
      factory.setAttribute("http://www.oracle.com/xml/jaxp/properties/maxElementDepth", 100);
      factory.setAttribute(
          "http://www.oracle.com/xml/jaxp/properties/totalEntitySizeLimit",
          10 * 1024 * 1024); // 10MB
      factory.setAttribute(
          "http://www.oracle.com/xml/jaxp/properties/maxEntityExpansionLimit", 100);

      logger.debug("Resource limits applied successfully");

    } catch (Exception e) {
      // JDKバージョンによっては利用できない場合があるため警告のみ
      logger.warn(
          "Could not apply some XML resource limits (may not be supported): {}", e.getMessage());
    }
  }

  /**
   * セキュリティ機能を安全に設定
   *
   * @param factory 設定対象のオブジェクト
   * @param feature 機能名
   * @param value 設定値
   */
  private void setSecureFeature(Object factory, String feature, boolean value) {
    try {
      if (factory instanceof DocumentBuilderFactory) {
        ((DocumentBuilderFactory) factory).setFeature(feature, value);
      } else if (factory instanceof SchemaFactory) {
        ((SchemaFactory) factory).setFeature(feature, value);
      } else if (factory instanceof Validator) {
        ((Validator) factory).setFeature(feature, value);
      }
      logger.debug("Security feature set: {} = {}", feature, value);

    } catch (Exception e) {
      logger.warn(
          "Could not set security feature '{}' on {}: {}",
          feature,
          factory.getClass().getSimpleName(),
          e.getMessage());
    }
  }

  /**
   * プロパティを安全に設定
   *
   * @param factory 設定対象のオブジェクト
   * @param property プロパティ名
   * @param value 設定値
   */
  private void setPropertySafely(Object factory, String property, Object value) {
    try {
      if (factory instanceof XMLInputFactory) {
        ((XMLInputFactory) factory).setProperty(property, value);
      } else if (factory instanceof SchemaFactory) {
        ((SchemaFactory) factory).setProperty(property, value);
      }
      logger.debug("Property set: {} = {}", property, value);

    } catch (Exception e) {
      logger.warn(
          "Could not set property '{}' on {}: {}",
          property,
          factory.getClass().getSimpleName(),
          e.getMessage());
    }
  }
}
