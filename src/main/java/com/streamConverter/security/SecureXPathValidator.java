package com.streamConverter.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * XPathセキュリティ検証クラス
 *
 * <p>XPathインジェクション攻撃を防ぐため、XPath式の厳格な検証とサニタイゼーションを行います。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>ホワイトリスト方式によるXPath式の検証
 *   <li>危険な関数・パターンの検出
 *   <li>XPath式の長さ・複雑さ制限
 *   <li>プリコンパイル済みパターンとの照合
 * </ul>
 *
 * @author StreamConverter Security Team
 * @version 1.0.0
 * @since 2025-07-30
 */
@Component
public class SecureXPathValidator {

  private static final Logger logger = LoggerFactory.getLogger(SecureXPathValidator.class);

  // 安全なXPath文字セット（text()関数対応）
  private static final Pattern SAFE_XPATH_PATTERN =
      Pattern.compile("^[a-zA-Z0-9_/\\[\\]@.\\-:()='\"\\s*]+$");

  // 危険な関数のセット
  private static final Set<String> DANGEROUS_FUNCTIONS =
      new HashSet<>(
          Arrays.asList(
              "document",
              "unparsed-text",
              "collection",
              "doc",
              "system-property",
              "current-date",
              "current-time",
              "generate-id",
              "format-number",
              "key",
              "id",
              "function",
              "import",
              "include"));

  // 危険なパターン
  private static final Set<String> DANGEROUS_PATTERNS =
      new HashSet<>(
          Arrays.asList(
              "javascript:",
              "file:",
              "http:",
              "https:",
              "ftp:",
              "entity",
              "external",
              "//script",
              "..",
              "normalize-space",
              "translate",
              "contains",
              "substring-before",
              "substring-after",
              "concat"));

  // 許可されたXPathパターン（正規表現）- より厳密なパターン
  private static final String[] ALLOWED_PATTERNS = {
    "^[a-zA-Z0-9_]+$", // 単純な要素名: element
    "^/[a-zA-Z0-9_/]+$", // 絶対パス: /root/element
    "^//[a-zA-Z0-9_]+$", // 子孫検索: //element
    "^/[a-zA-Z0-9_/]+/@[a-zA-Z0-9_]+$", // 属性: /root/element/@attr
    "^//[a-zA-Z0-9_]+/@[a-zA-Z0-9_]+$", // 子孫属性: //element/@attr
    "^/[a-zA-Z0-9_/]+/text\\(\\)$", // テキスト: /root/element/text()
    "^//[a-zA-Z0-9_/]+/text\\(\\)$", // 子孫テキスト: //element/text()
    "^/[a-zA-Z0-9_/]+\\[@[a-zA-Z0-9_]+='[a-zA-Z0-9_\\s]+'\\]$", // 条件付き:
    // /root/element[@attr='value']
    "^//[a-zA-Z0-9_]+\\[@[a-zA-Z0-9_]+='[a-zA-Z0-9_\\s]+'\\]$" // 子孫条件付き: //element[@attr='value']
  };

  // 事前承認済みXPath式のホワイトリスト
  private static final Set<String> WHITELISTED_EXPRESSIONS =
      new HashSet<>(
          Arrays.asList(
              "//user/@id",
              "//user/name/text()",
              "/root/item/text()",
              "/root/@version",
              "//item[@type='test']/text()",
              "/document/header/title/text()",
              "//data/value/text()",
              "/config/@setting",
              "//product/name/text()",
              "//order/@id",
              "/response/status/text()",
              "//error/message/text()"));

  /**
   * XPath式を検証・サニタイズ
   *
   * @param xpath 検証対象のXPath式
   * @return サニタイズされたXPath式
   * @throws IllegalArgumentException 無効または危険なXPath式の場合
   */
  public String validateAndSanitizeXPath(String xpath) {
    if (xpath == null || xpath.trim().isEmpty()) {
      throw new IllegalArgumentException("XPath expression cannot be null or empty");
    }

    String sanitized = xpath.trim();

    logger.debug("Validating XPath expression: {}", sanitized);

    // 基本的な制限チェック
    validateBasicConstraints(sanitized);

    // 文字セット検証
    validateCharacterSet(sanitized);

    // 危険なパターンの検出
    detectDangerousPatterns(sanitized);

    // 許可されたパターンとの照合
    validateAgainstAllowedPatterns(sanitized);

    logger.info("XPath expression validated successfully: {}", sanitized);
    return sanitized;
  }

  /**
   * XPath式がホワイトリストに含まれているかチェック
   *
   * @param xpath 検証するXPath式
   * @return ホワイトリストに含まれている場合true
   */
  public boolean isXPathInWhitelist(String xpath) {
    // 明示的なホワイトリストチェックを優先
    if (WHITELISTED_EXPRESSIONS.contains(xpath)) {
      logger.debug("XPath expression found in explicit whitelist: {}", xpath);
      return true;
    }

    // パターンマッチングでの厳密な検証
    boolean isValid = isValidXPathPattern(xpath);
    if (isValid) {
      logger.debug("XPath expression matches allowed pattern: {}", xpath);
    }
    return isValid;
  }

  /**
   * XPath式の複雑さを評価
   *
   * @param xpath 評価対象のXPath式
   * @return 複雑さスコア（低いほど安全）
   */
  public int calculateComplexityScore(String xpath) {
    int score = 0;

    // 長さによるスコア
    score += xpath.length() / 10;

    // 演算子の数
    score += countOccurrences(xpath, '[');
    score += countOccurrences(xpath, '(');
    score += countOccurrences(xpath, '@');

    // ワイルドカードの使用
    score += countOccurrences(xpath, '*') * 2;

    // スラッシュの数（階層の深さ）
    score += countOccurrences(xpath, '/');

    logger.debug("XPath complexity score: {} for expression: {}", score, xpath);
    return score;
  }

  /**
   * 基本的な制約をチェック
   *
   * @param xpath 検証対象のXPath式
   */
  private void validateBasicConstraints(String xpath) {
    // 長さ制限（DoS攻撃防止）
    if (xpath.length() > 150) {
      throw new IllegalArgumentException(
          String.format("XPath expression too long: %d characters (max 150)", xpath.length()));
    }

    // 複雑さ制限
    int complexityScore = calculateComplexityScore(xpath);
    if (complexityScore > 20) {
      throw new IllegalArgumentException(
          String.format("XPath expression too complex: score %d (max 20)", complexityScore));
    }

    // 空文字や不正な文字列のチェック
    if (xpath.contains("null") || xpath.contains("undefined")) {
      throw new IllegalArgumentException("XPath expression contains invalid null/undefined values");
    }
  }

  /**
   * 文字セット検証
   *
   * @param xpath 検証対象のXPath式
   */
  private void validateCharacterSet(String xpath) {
    if (!SAFE_XPATH_PATTERN.matcher(xpath).matches()) {
      logger.warn("XPath contains invalid characters: {}", xpath);
      throw new IllegalArgumentException("XPath expression contains invalid characters");
    }
  }

  /**
   * 危険なパターンの検出
   *
   * @param xpath 検証対象のXPath式
   */
  private void detectDangerousPatterns(String xpath) {
    String lowerXPath = xpath.toLowerCase();

    // 危険な関数の検出
    for (String dangerousFunction : DANGEROUS_FUNCTIONS) {
      if (lowerXPath.contains(dangerousFunction + "(")) {
        logger.warn("Dangerous function detected in XPath: {}", dangerousFunction);
        throw new IllegalArgumentException(
            "XPath expression contains dangerous function: " + dangerousFunction);
      }
    }

    // 危険なパターンの検出
    for (String dangerousPattern : DANGEROUS_PATTERNS) {
      if (lowerXPath.contains(dangerousPattern.toLowerCase())) {
        logger.warn("Dangerous pattern detected in XPath: {}", dangerousPattern);
        throw new IllegalArgumentException(
            "XPath expression contains dangerous pattern: " + dangerousPattern);
      }
    }

    // コメントインジェクション
    if (xpath.contains("/*") || xpath.contains("*/")) {
      throw new IllegalArgumentException("XPath expression contains comment syntax");
    }

    // SQLインジェクション様パターン
    if (lowerXPath.contains("union")
        || lowerXPath.contains("select")
        || lowerXPath.contains("drop")
        || lowerXPath.contains("or ")
        || lowerXPath.contains(" or")
        || lowerXPath.contains("and ")
        || lowerXPath.contains(" and")
        || lowerXPath.contains("'='")
        || lowerXPath.contains("\"=\"")) {
      throw new IllegalArgumentException("XPath expression contains SQL-like injection patterns");
    }
  }

  /**
   * 許可されたパターンとの照合
   *
   * @param xpath 検証対象のXPath式
   */
  private void validateAgainstAllowedPatterns(String xpath) {
    if (!isValidXPathPattern(xpath)) {
      logger.warn("XPath does not match any allowed pattern: {}", xpath);
      throw new IllegalArgumentException("XPath expression does not match allowed patterns");
    }
  }

  /**
   * XPath式が許可されたパターンに一致するかチェック
   *
   * @param xpath 検証するXPath式
   * @return 許可されたパターンに一致する場合true
   */
  private boolean isValidXPathPattern(String xpath) {
    for (String pattern : ALLOWED_PATTERNS) {
      if (xpath.matches(pattern)) {
        logger.debug("XPath matches allowed pattern '{}': {}", pattern, xpath);
        return true;
      }
    }
    return false;
  }

  /**
   * 文字列中の特定文字の出現回数をカウント
   *
   * @param text 検索対象の文字列
   * @param ch 検索する文字
   * @return 出現回数
   */
  private int countOccurrences(String text, char ch) {
    int count = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == ch) {
        count++;
      }
    }
    return count;
  }

  // 設定取得メソッド
  /**
   * ホワイトリストに登録されているXPath式のセットを取得します。
   *
   * @return ホワイトリストに登録されているXPath式の不変のセット
   */
  public Set<String> getWhitelistedExpressions() {
    return new HashSet<>(WHITELISTED_EXPRESSIONS);
  }

  /**
   * 危険な関数名のセットを取得します。
   *
   * @return 危険な関数名の不変のセット
   */
  public Set<String> getDangerousFunctions() {
    return new HashSet<>(DANGEROUS_FUNCTIONS);
  }

  /**
   * 許可されているXPathパターンの配列を取得します。
   *
   * @return 許可されているXPathパターンのクローン配列
   */
  public String[] getAllowedPatterns() {
    return ALLOWED_PATTERNS.clone();
  }
}
