package com.streamconverter.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SecureXPathValidatorのテストクラス */
class SecureXPathValidatorTest {

  @Test
  @DisplayName("正常なXPath式の検証テスト")
  void testValidXPathExpressions() {
    String[] validXPaths = {
      "/root/element",
      "//element[@id='test']",
      "//*[text()='value']",
      "count(//element)",
      "string-length(//text)",
      "substring(@attr, 1, 5)",
      "//element[position()=1]"
    };

    for (String xpath : validXPaths) {
      assertDoesNotThrow(
          () -> SecureXPathValidator.validateXPath(xpath), "正常なXPath式でエラーが発生しました: " + xpath);
      assertTrue(SecureXPathValidator.isXPathSafe(xpath), "正常なXPath式が安全でないと判定されました: " + xpath);
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "' or '1'='1",
        "\"; DROP TABLE users; --",
        "../../etc/passwd",
        "//element[contains(., '\\'malicious\\'')]"
      })
  @DisplayName("XPathインジェクション攻撃パターンの検出テスト")
  void testXPathInjectionDetection(String maliciousXPath) {
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(maliciousXPath),
        "XPathインジェクション攻撃パターンが検出されませんでした: " + maliciousXPath);

    assertFalse(
        SecureXPathValidator.isXPathSafe(maliciousXPath),
        "危険なXPath式が安全と判定されました: " + maliciousXPath);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "document('http://malicious.com')",
        "unparsed-text('file:///etc/passwd')",
        "sql:execute('DROP TABLE users')",
        "fn:doc('http://attacker.com/xml')"
      })
  @DisplayName("危険な関数の検出テスト")
  void testDangerousFunctionDetection(String dangerousXPath) {
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(dangerousXPath),
        "危険な関数が検出されませんでした: " + dangerousXPath);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://malicious.com/xpath",
        "https://attacker.com/",
        "file:///etc/passwd",
        "ftp://server.com/file",
        "../../../sensitive"
      })
  @DisplayName("外部参照の検出テスト")
  void testExternalReferenceDetection(String externalRefXPath) {
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(externalRefXPath),
        "外部参照が検出されませんでした: " + externalRefXPath);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "UNION SELECT * FROM users",
        "INSERT INTO table VALUES",
        "DROP TABLE users",
        "DELETE FROM sensitive_data"
      })
  @DisplayName("SQLインジェクション類似パターンの検出テスト")
  void testSqlLikeInjectionDetection(String sqlLikeXPath) {
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(sqlLikeXPath),
        "SQLインジェクション類似パターンが検出されませんでした: " + sqlLikeXPath);
  }

  @Test
  @DisplayName("XPath式のサニタイズテスト")
  void testXPathSanitization() {
    String originalXPath = "//element[@attr='value's with quotes\"]";
    String sanitized = SecureXPathValidator.sanitizeXPath(originalXPath);

    assertNotNull(sanitized, "サニタイズ結果はnullであってはいけません");
    assertFalse(sanitized.contains("'"), "サニタイズ後にシングルクォートが残っています");
    assertFalse(sanitized.contains("\""), "サニタイズ後にダブルクォートが残っています");
  }

  @Test
  @DisplayName("null入力の処理テスト")
  void testNullInput() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SecureXPathValidator.validateXPath(null),
        "null入力で例外が発生しませんでした");

    assertThrows(
        IllegalArgumentException.class,
        () -> SecureXPathValidator.validateXPath(""),
        "空文字列入力で例外が発生しませんでした");

    assertThrows(
        IllegalArgumentException.class,
        () -> SecureXPathValidator.validateXPath("   "),
        "空白文字のみの入力で例外が発生しませんでした");
  }

  @Test
  @DisplayName("許可されたXPath関数リストの取得テスト")
  void testAllowedXPathFunctions() {
    String[] allowedFunctions = SecureXPathValidator.getAllowedXPathFunctions();

    assertNotNull(allowedFunctions, "許可された関数リストはnullであってはいけません");
    assertTrue(allowedFunctions.length > 0, "許可された関数リストは空であってはいけません");

    // 基本的な関数が含まれていることを確認
    boolean containsText = false;
    boolean containsCount = false;
    for (String function : allowedFunctions) {
      if ("text()".equals(function)) containsText = true;
      if ("count()".equals(function)) containsCount = true;
    }

    assertTrue(containsText, "text()関数が許可された関数リストに含まれていません");
    assertTrue(containsCount, "count()関数が許可された関数リストに含まれていません");
  }

  @Test
  @DisplayName("長いXPath式の処理テスト（厳格モード）")
  void testLongXPathInStrictMode() {
    // 1000文字を超えるXPath式を生成
    StringBuilder longXPath = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      longXPath.append("//element").append(i).append("/");
    }

    // 厳格モードが有効な場合、長すぎるXPathは拒否される
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(longXPath.toString()),
        "長すぎるXPath式が受け入れられました");
  }

  @Test
  @DisplayName("複雑なネストの処理テスト（厳格モード）")
  void testDeepNestingInStrictMode() {
    // 深いネストを持つXPath式を生成
    StringBuilder deepNestedXPath = new StringBuilder("//element");
    for (int i = 0; i < 15; i++) {
      deepNestedXPath.append("[").append(i).append("]");
    }

    // 厳格モードが有効な場合、深いネストは拒否される
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(deepNestedXPath.toString()),
        "深いネストを持つXPath式が受け入れられました");
  }

  @Test
  @DisplayName("厳格モードで and/or/not の複合演算子を含むXPathは拒否される")
  void testStrictModeRejectsComplexOperatorCombinations() {
    String complexOperatorXPath = "//a[@x='1' and @y='2' or not(@z)]";

    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(complexOperatorXPath),
        "and/or/not の複合演算子を含むXPathが受け入れられました");
  }

  @Test
  @DisplayName("QName 風要素名（ハイフン・ドット・コロン）に and/or/not を含むパス式は通過する")
  void testQNameLikeElementNamesContainingOperatorSubstringsAreAccepted() {
    // XPath の Name には - . : も含まれるため、これらを境界に持つ要素名が
    // 単純な \b 境界判定では誤検知される。predicate 外限定の判定であることを検証する。
    String[] qnameLikePaths = {"and-node/orbit/notation-item", "pre:and/sponsor/not.item"};

    for (String xpath : qnameLikePaths) {
      assertDoesNotThrow(
          () -> SecureXPathValidator.validateXPath(xpath), "QName 風の正当なパス式でエラーが発生しました: " + xpath);
      assertTrue(SecureXPathValidator.isXPathSafe(xpath), "QName 風の正当なパス式が安全でないと判定されました: " + xpath);
    }
  }

  @Test
  @DisplayName("要素名・属性名そのものが and/or/not のパス式は通過する")
  void testElementOrAttributeNamesEqualToOperatorTokensAreAccepted() {
    // and / or / not は要素名・属性名として合法に使用できる。
    // predicate [...] 外に現れる and / or / not は常に Name の一部であり、
    // 演算子として解釈してはならない。
    String[] nameAsOperatorPaths = {"/root/and/or/not", "//a/or/@not"};

    for (String xpath : nameAsOperatorPaths) {
      assertDoesNotThrow(
          () -> SecureXPathValidator.validateXPath(xpath),
          "要素名・属性名が and/or/not のパス式でエラーが発生しました: " + xpath);
      assertTrue(
          SecureXPathValidator.isXPathSafe(xpath),
          "要素名・属性名が and/or/not のパス式が安全でないと判定されました: " + xpath);
    }
  }

  @Test
  @DisplayName("要素名に and/or/not を部分文字列として含む正当なパス式は検証を通過する")
  void testElementNamesContainingOperatorSubstringsAreAccepted() {
    // 演算子 and/or/not をトークンとして一切含まない単純な要素パス。
    // 要素名の一部（brand/colors/notes、operand/sponsor/notation）に
    // 演算子と同じ文字列が含まれるだけであり、検証を通過することが期待される。
    String[] legitimatePaths = {"brand/colors/notes", "operand/sponsor/notation"};

    for (String xpath : legitimatePaths) {
      assertDoesNotThrow(
          () -> SecureXPathValidator.validateXPath(xpath), "正当なパス式でエラーが発生しました: " + xpath);
      assertTrue(SecureXPathValidator.isXPathSafe(xpath), "正当なパス式が安全でないと判定されました: " + xpath);
    }
  }

  @Test
  @DisplayName("過度なワイルドカード使用の処理テスト（厳格モード）")
  void testExcessiveWildcardsInStrictMode() {
    String excessiveWildcards = "//*/*/*/*/*/*/*/*";

    // 厳格モードが有効な場合、過度なワイルドカード使用は拒否される
    assertThrows(
        SecurityException.class,
        () -> SecureXPathValidator.validateXPath(excessiveWildcards),
        "過度なワイルドカード使用が受け入れられました");
  }
}
