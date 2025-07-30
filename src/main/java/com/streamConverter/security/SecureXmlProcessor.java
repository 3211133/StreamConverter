package com.streamConverter.security;

import java.io.StringReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * セキュアなXML処理クラス
 *
 * <p>DocumentBuilderを使用せず、StAXベースでXML処理を行うことで XXE攻撃を根本的に防止します。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>StAXによるストリーミング処理（DOMを生成しない）
 *   <li>XPath式に基づく値抽出（手動実装）
 *   <li>外部エンティティ参照の完全無効化
 *   <li>XML爆弾検出と制限
 * </ul>
 *
 * @author StreamConverter Security Team
 * @version 1.0.0
 * @since 2025-07-30
 */
@Component
public class SecureXmlProcessor {

  private static final Logger logger = LoggerFactory.getLogger(SecureXmlProcessor.class);

  private final SecureXmlConfiguration secureXmlConfig;
  private final XmlResourceLimiter resourceLimiter;
  private final SecureXPathValidator xpathValidator;

  // StAX処理制限
  private static final int MAX_ELEMENT_DEPTH = 100;
  private static final int MAX_ELEMENT_COUNT = 10000;
  private static final int MAX_ATTRIBUTE_COUNT = 100;

  /**
   * SecureXmlProcessorのコンストラクタ。
   *
   * <p>セキュアなXML処理に必要な設定とリミッター、XPathバリデーターを初期化します。
   */
  public SecureXmlProcessor() {
    this.secureXmlConfig = new SecureXmlConfiguration();
    this.resourceLimiter = new XmlResourceLimiter();
    this.xpathValidator = new SecureXPathValidator();
  }

  /**
   * 安全なXPath値抽出（DocumentBuilderを使用しない）
   *
   * @param xmlContent XML文字列
   * @param xpathExpression XPath式
   * @return 抽出された値
   * @throws Exception セキュリティ違反またはXML処理エラーの場合
   */
  public String extractValueSafely(String xmlContent, String xpathExpression) throws Exception {
    logger.debug("Starting secure XML value extraction");

    // XPath式の検証
    String validatedXPath = xpathValidator.validateAndSanitizeXPath(xpathExpression);

    // XML爆弾検出
    resourceLimiter.detectXmlBombs(xmlContent);

    // セキュアなXMLInputFactoryを作成
    XMLInputFactory factory = secureXmlConfig.createSecureXMLInputFactory();

    // StAXリーダーでXMLを安全に処理
    try (StringReader stringReader = new StringReader(xmlContent)) {
      XMLStreamReader reader = factory.createXMLStreamReader(stringReader);

      try {
        return extractValueWithStAX(reader, validatedXPath);
      } finally {
        reader.close();
      }
    }
  }

  /**
   * XML検証（DocumentBuilderを使用しない軽量検証）
   *
   * @param xmlContent 検証対象のXML
   * @return 検証結果（true: 有効, false: 無効）
   */
  public boolean validateXmlStructure(String xmlContent) {
    logger.debug("Starting lightweight XML validation");

    try {
      // XML爆弾検出
      resourceLimiter.detectXmlBombs(xmlContent);

      // セキュアなXMLInputFactoryを作成
      XMLInputFactory factory = secureXmlConfig.createSecureXMLInputFactory();

      // StAXで構造検証
      try (StringReader stringReader = new StringReader(xmlContent)) {
        XMLStreamReader reader = factory.createXMLStreamReader(stringReader);

        int depth = 0;
        int elementCount = 0;
        boolean hasRootElement = false;

        while (reader.hasNext()) {
          int event = reader.next();
          elementCount++;

          // リソース制限チェック
          if (depth > MAX_ELEMENT_DEPTH) {
            logger.warn("XML depth exceeds limit: {}", depth);
            return false;
          }
          if (elementCount > MAX_ELEMENT_COUNT) {
            logger.warn("XML element count exceeds limit: {}", elementCount);
            return false;
          }

          switch (event) {
            case XMLStreamConstants.START_ELEMENT:
              depth++;
              if (depth == 1) {
                hasRootElement = true;
              }

              // 属性数制限
              if (reader.getAttributeCount() > MAX_ATTRIBUTE_COUNT) {
                logger.warn("Attribute count exceeds limit: {}", reader.getAttributeCount());
                return false;
              }
              break;

            case XMLStreamConstants.END_ELEMENT:
              depth--;
              break;

            default:
              // その他のイベントは検証対象外
              break;
          }
        }

        boolean isValid = hasRootElement && depth == 0;
        logger.debug("XML structure validation result: {}", isValid);
        return isValid;
      }

    } catch (Exception e) {
      logger.warn("XML validation failed: {}", e.getMessage());
      return false;
    }
  }

  /**
   * StAXを使用した安全なXPath値抽出
   *
   * @param reader XMLStreamReader
   * @param xpathExpression 検証済みXPath式
   * @return 抽出された値
   */
  private String extractValueWithStAX(XMLStreamReader reader, String xpathExpression)
      throws Exception {
    logger.debug("Extracting value with StAX for XPath: {}", xpathExpression);

    XPathInfo xpathInfo = parseSimpleXPath(xpathExpression);
    String[] pathElements = xpathInfo.pathElements;
    String attributeName = xpathInfo.attributeName;
    boolean isTextNode = xpathInfo.isTextNode;

    int depth = 0;
    int currentPathIndex = 0;
    boolean inTargetElement = false;
    StringBuilder textContent = new StringBuilder();
    int elementCount = 0;

    while (reader.hasNext()) {
      int event = reader.next();
      elementCount++;

      // リソース制限チェック
      if (depth > MAX_ELEMENT_DEPTH) {
        throw new SecurityException("XML element depth exceeds maximum: " + MAX_ELEMENT_DEPTH);
      }
      if (elementCount > MAX_ELEMENT_COUNT) {
        throw new SecurityException("XML element count exceeds maximum: " + MAX_ELEMENT_COUNT);
      }

      switch (event) {
        case XMLStreamConstants.START_ELEMENT:
          depth++;
          String elementName = reader.getLocalName();

          // パス照合
          if (currentPathIndex < pathElements.length
              && elementName.equals(pathElements[currentPathIndex])) {
            currentPathIndex++;
            if (currentPathIndex == pathElements.length) {
              inTargetElement = true;

              // ターゲット要素に到達した場合、属性を抽出
              if (attributeName != null) {
                String attributeValue = reader.getAttributeValue(null, attributeName);
                logger.debug("Extracted attribute value: {}", attributeValue);
                return attributeValue;
              }
            }
          } else if (inTargetElement) {
            // ターゲット要素内の子要素は無視
          } else {
            // パスが一致しない場合はリセット
            currentPathIndex = elementName.equals(pathElements[0]) ? 1 : 0;
          }

          // 属性数制限
          if (reader.getAttributeCount() > MAX_ATTRIBUTE_COUNT) {
            throw new SecurityException(
                "XML attribute count exceeds maximum: " + MAX_ATTRIBUTE_COUNT);
          }
          break;

        case XMLStreamConstants.CHARACTERS:
          if (inTargetElement && isTextNode) {
            textContent.append(reader.getText());
          }
          break;

        case XMLStreamConstants.END_ELEMENT:
          depth--;
          if (inTargetElement && depth == pathElements.length - 1) {
            // ターゲット要素の終了
            if (isTextNode) {
              String result = textContent.toString().trim();
              logger.debug("Successfully extracted text node value: {}", result);
              return result.isEmpty() ? null : result;
            } else {
              // 属性抽出の場合、既に返されているはずなので、ここでは何もしない
              // または、属性がない場合はnullを返す
              return null;
            }
          }
          if (currentPathIndex > 0) {
            currentPathIndex--;
          }
          break;

        default:
          // その他のイベントは無視
          break;
      }
    }

    logger.debug("XPath expression did not match any element or attribute: {}", xpathExpression);
    return null;
  }

  /**
   * 簡単なXPath式をパス要素に分解し、属性名やテキストノードの有無を判断
   *
   * <p>このメソッドは、XPath式を解析し、パス要素、属性名、およびテキストノードの有無を抽出します。
   * 現在、絶対パスのみをサポートし、子孫軸（//）はセキュリティ上の理由から許可されていません。
   *
   * @param xpath XPath式
   * @return XPathInfoオ���ジェクト
   * @throws IllegalArgumentException 無効なXPath式が指定された場合
   */
  private XPathInfo parseSimpleXPath(String xpath) {
    // 簡単な絶対パスのみサポート (/root/element/@attribute or /root/element/text())
    if (xpath.startsWith("//")) {
      throw new IllegalArgumentException("Descendant axis (//) not supported in secure mode");
    }

    if (!xpath.startsWith("/")) {
      throw new IllegalArgumentException("Only absolute XPath expressions supported");
    }

    String cleanPath = xpath;
    String attributeName = null;
    boolean isTextNode = false;

    if (xpath.endsWith("/text()")) {
      isTextNode = true;
      cleanPath = xpath.substring(0, xpath.length() - "/text()".length());
    } else if (xpath.contains("@")) {
      int atIndex = xpath.lastIndexOf('@');
      attributeName = xpath.substring(atIndex + 1);
      cleanPath = xpath.substring(0, atIndex);
    }

    // 先頭のスラッシュを除去して分割
    String pathWithoutSlash = cleanPath.substring(1);
    if (pathWithoutSlash.isEmpty()) {
      throw new IllegalArgumentException("Invalid XPath expression: empty path");
    }

    return new XPathInfo(pathWithoutSlash.split("/"), attributeName, isTextNode);
  }

  /**
   * XPath解析結果を保持する内部クラス
   *
   * <p>このクラスは、解析されたXPath式のパス要素、属性名、およびテキストノードの有無をカプセル化します。
   */
  private static class XPathInfo {
    final String[] pathElements;
    final String attributeName;
    final boolean isTextNode;

    /**
     * コンストラクタ
     *
     * @param pathElements XPathの要素パスの配列
     * @param attributeName 抽出対象の属性名（存在しない場合はnull）
     * @param isTextNode テキストノードを抽出するかどうか
     */
    XPathInfo(String[] pathElements, String attributeName, boolean isTextNode) {
      this.pathElements = pathElements;
      this.attributeName = attributeName;
      this.isTextNode = isTextNode;
    }
  }
}
