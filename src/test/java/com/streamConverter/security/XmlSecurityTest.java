package com.streamConverter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.validation.SchemaFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * XMLセキュリティ機能のテストクラス
 *
 * <p>XXE攻撃、XML爆弾攻撃、XPathインジェクション攻撃に対する防御機能をテストします。
 *
 * @author StreamConverter Security Team
 * @version 1.0.0
 * @since 2025-07-30
 */
class XmlSecurityTest {

  private SecureXmlConfiguration secureXmlConfig;
  private XmlResourceLimiter resourceLimiter;
  private SecureXPathValidator xpathValidator;

  @BeforeEach
  void setUp() {
    secureXmlConfig = new SecureXmlConfiguration();
    resourceLimiter = new XmlResourceLimiter();
    xpathValidator = new SecureXPathValidator();
  }

  @Test
  @DisplayName("XXE攻撃の防御テスト")
  void testXXEAttackPrevention() {
    String maliciousXml =
        """
        <?xml version="1.0"?>
        <!DOCTYPE foo [
          <!ENTITY xxe SYSTEM "/etc/passwd">
        ]>
        <root>&xxe;</root>
        """;

    assertThrows(
        SecurityException.class,
        () -> {
          resourceLimiter.detectXmlBombs(maliciousXml);
        },
        "XXE攻撃パターンが検出されるべきです");
  }

  @Test
  @DisplayName("XML爆弾攻撃の防御テスト")
  void testXmlBombPrevention() {
    String xmlBomb =
        """
        <?xml version="1.0"?>
        <!DOCTYPE lolz [
          <!ENTITY lol "lol">
          <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
          <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
        ]>
        <lolz>&lol3;</lolz>
        """;

    assertThrows(
        SecurityException.class,
        () -> {
          resourceLimiter.detectXmlBombs(xmlBomb);
        },
        "XML爆弾攻撃パターンが検出されるべきです");
  }

  @Test
  @DisplayName("XPathインジェクション攻撃の防御テスト")
  void testXPathInjectionPrevention() {
    String[] maliciousXPaths = {
      "//user[@id='' or '1'='1']", // SQLインジェクション様
      "document('/etc/passwd')", // 外部ファイルアクセス
      "//script[contains(@src,'evil.js')]", // スクリプトタグ
      "//user[contains(name(),system-property('user.name'))]", // システムプロパティアクセス
      "../../../etc/passwd", // パストラバーサル
      "javascript:alert('xss')" // JavaScriptインジェクション
    };

    for (String maliciousXPath : maliciousXPaths) {
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            xpathValidator.validateAndSanitizeXPath(maliciousXPath);
          },
          "悪意のあるXPath式が検出されるべきです: " + maliciousXPath);
    }
  }

  @Test
  @DisplayName("安全なXML処理の正常動作テスト")
  void testSafeXmlProcessing() {
    String safeXml =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <root>
          <user id="123">
            <name>Test User</name>
            <email>test@example.com</email>
          </user>
        </root>
        """;

    assertDoesNotThrow(
        () -> {
          resourceLimiter.detectXmlBombs(safeXml);
        },
        "安全なXMLは処理できるべきです");
  }

  @Test
  @DisplayName("安全なXPath式の正常動作テスト")
  void testSafeXPathProcessing() {
    String[] safeXPaths = {
      "//user/@id",
      "//user/name/text()",
      "/root/item/text()",
      "//product/name/text()",
      "/config/@setting"
    };

    for (String safeXPath : safeXPaths) {
      assertDoesNotThrow(
          () -> {
            String result = xpathValidator.validateAndSanitizeXPath(safeXPath);
            assertThat(result).isEqualTo(safeXPath);
          },
          "安全なXPath式は処理できるべきです: " + safeXPath);
    }
  }

  @Test
  @DisplayName("DocumentBuilderFactoryのセキュリティ設定テスト")
  void testSecureDocumentBuilderFactory() throws Exception {
    DocumentBuilderFactory factory = secureXmlConfig.createSecureDocumentBuilderFactory();

    // セキュリティ設定が適用されていることを確認
    assertThat(factory.getFeature("http://apache.org/xml/features/disallow-doctype-decl")).isTrue();
    assertThat(factory.getFeature("http://xml.org/sax/features/external-general-entities"))
        .isFalse();
    assertThat(factory.getFeature("http://xml.org/sax/features/external-parameter-entities"))
        .isFalse();
  }

  @Test
  @DisplayName("XMLInputFactoryのセキュリティ設定テスト")
  void testSecureXMLInputFactory() {
    XMLInputFactory factory = secureXmlConfig.createSecureXMLInputFactory();

    // セキュリティ設定が適用されていることを確認
    assertThat(factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES))
        .isEqualTo(false);
    assertThat(factory.getProperty(XMLInputFactory.SUPPORT_DTD)).isEqualTo(false);
  }

  @Test
  @DisplayName("SchemaFactoryのセキュリティ設定テスト")
  void testSecureSchemaFactory() {
    SchemaFactory factory = secureXmlConfig.createSecureSchemaFactory();

    // SchemaFactoryが正常に作成されることを確認
    assertThat(factory).isNotNull();
  }

  @Test
  @DisplayName("XMLサイズ制限テスト")
  void testXmlSizeLimit() throws Exception {
    // 制限を超える大きなXMLデータを作成
    StringBuilder largeXml = new StringBuilder("<?xml version=\"1.0\"?><root>");
    for (int i = 0; i < 100000; i++) {
      largeXml.append("<item>").append("data".repeat(100)).append("</item>");
    }
    largeXml.append("</root>");

    InputStream largeStream = new ByteArrayInputStream(largeXml.toString().getBytes());

    assertThrows(
        SecurityException.class,
        () -> {
          resourceLimiter.createLimitedInputStream(largeStream).readAllBytes();
        },
        "大きなXMLファイルは制限されるべきです");
  }

  @Test
  @DisplayName("XPath複雑さスコアテスト")
  void testXPathComplexityScore() {
    // 単純なXPath（低スコア）
    int simpleScore = xpathValidator.calculateComplexityScore("//user");
    assertThat(simpleScore).isLessThan(10);

    // 複雑なXPath（高スコア）
    String complexXPath = "//user[@id='123' and @type='admin']//profile[@active='true']/settings";
    int complexScore = xpathValidator.calculateComplexityScore(complexXPath);
    assertThat(complexScore).isGreaterThan(simpleScore);
  }

  @Test
  @DisplayName("ホワイトリストXPath式テスト")
  void testWhitelistedXPathExpressions() {
    // ホワイトリストに含まれるXPath式
    String[] whitelistedXPaths = {
      "//user/@id", "//user/name/text()", "/root/item/text()", "//data/value/text()"
    };

    for (String xpath : whitelistedXPaths) {
      assertThat(xpathValidator.isXPathInWhitelist(xpath))
          .withFailMessage("XPath式 '%s' はホワイトリストに含まれるべきです", xpath)
          .isTrue();
    }
  }

  @Test
  @DisplayName("過度に長いXPath式の拒否テスト")
  void testLongXPathRejection() {
    String longXPath = "//user".repeat(50); // 非常に長いXPath式

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          xpathValidator.validateAndSanitizeXPath(longXPath);
        },
        "過度に長いXPath式は拒否されるべきです");
  }

  @Test
  @DisplayName("エンティティ参照密度テスト")
  void testEntityReferenceDensity() {
    String highDensityXml =
        """
        <?xml version="1.0"?>
        <!DOCTYPE test [
          <!ENTITY ent "entity">
        ]>
        <root>&ent;&ent;&ent;&ent;&ent;&ent;&ent;&ent;&ent;&ent;&ent;&ent;</root>
        """;

    assertThrows(
        SecurityException.class,
        () -> {
          resourceLimiter.detectXmlBombs(highDensityXml);
        },
        "高密度のエンティティ参照は検出されるべきです");
  }

  @Test
  @DisplayName("入れ子エンティティテスト")
  void testNestedEntityDetection() {
    String nestedEntityXml =
        """
        <?xml version="1.0"?>
        <!DOCTYPE test [
          <!ENTITY outer "&inner;">
          <!ENTITY inner "nested entity">
        ]>
        <root>&outer;</root>
        """;

    assertThrows(
        SecurityException.class,
        () -> {
          resourceLimiter.detectXmlBombs(nestedEntityXml);
        },
        "入れ子エンティティは検出されるべきです");
  }
}
