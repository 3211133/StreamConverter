package com.streamconverter.security;

import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * XPathインジェクション攻撃を防ぐためのバリデータークラス
 *
 * <p>このクラスは、XPath式の安全性を検証し、 悪意のあるXPath式を検出・ブロックします。
 *
 * <p>主な機能:
 *
 * <ul>
 *   <li>XPathインジェクション攻撃パターンの検出
 *   <li>危険な関数の使用制限
 *   <li>特殊文字のエスケープ処理
 *   <li>XPath式の構造的検証
 *   <li>セキュリティ設定による動的制御
 * </ul>
 *
 * @since 1.0.0
 */
@SuppressWarnings("PMD.TooManyMethods")
// セキュリティ検証は「インジェクション・危険関数・外部参照・SQLライク・厳格モード」の
// 独立したチェック群で構成され、各チェックが固有の private Pattern 定数を参照している。
// チェック群は package-private クラスへ分離可能だが、単一クラスに集約することで
// 全パターンの見通しが保たれ、検証ロジックの変更漏れを防ぎやすい。
public class SecureXPathValidator {

  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  // XPathインジェクション攻撃パターン（エスケープクォートも含む）
  private static final Pattern XPATH_INJECTION_PATTERN =
      Pattern.compile(
          ".*(;|'\\s*or\\s*'|\"\\s*or\\s*\"|\\\\['\"\\\\]|\\b(union|drop|insert|delete)\\b).*",
          Pattern.CASE_INSENSITIVE);

  // 危険な関数パターン
  private static final Pattern DANGEROUS_FUNCTIONS_PATTERN =
      Pattern.compile(
          ".*\\b(document|unparsed-text|collection|doc|fn:doc|fn:collection|sql:execute)\\s*\\(.*",
          Pattern.CASE_INSENSITIVE);

  // 外部参照パターン
  private static final Pattern EXTERNAL_REFERENCE_PATTERN =
      Pattern.compile(".*(http://|https://|file://|ftp://|\\.\\./).*", Pattern.CASE_INSENSITIVE);

  // SQLインジェクション類似パターン
  private static final Pattern SQL_LIKE_INJECTION_PATTERN =
      Pattern.compile(
          ".*(union|select|insert|update|delete|drop|create|alter|exec|execute)\\s+.*",
          Pattern.CASE_INSENSITIVE);

  // 厳格モード用の論理演算子トークン検出パターン
  // XPath の名前（QName）に許容される文字 [\p{L}\p{N}_:.-] を境界に用いることで、
  // brand / colors / notes・and-node / pre:or-value / not.item のような
  // 「演算子の部分文字列を含むだけの正当な名前」を演算子と誤検知しないようにする。
  // CASE_INSENSITIVE は意図的に付けない（XPath の論理演算子は小文字キーワード固定）。
  private static final String XPATH_NAME_BOUNDARY = "[\\p{L}\\p{N}_:.\\-]";
  private static final Pattern OPERATOR_AND_PATTERN =
      Pattern.compile("(?<!" + XPATH_NAME_BOUNDARY + ")and(?!" + XPATH_NAME_BOUNDARY + ")");
  private static final Pattern OPERATOR_OR_PATTERN =
      Pattern.compile("(?<!" + XPATH_NAME_BOUNDARY + ")or(?!" + XPATH_NAME_BOUNDARY + ")");
  private static final Pattern OPERATOR_NOT_PATTERN =
      Pattern.compile("(?<!" + XPATH_NAME_BOUNDARY + ")not(?!" + XPATH_NAME_BOUNDARY + ")");

  private SecureXPathValidator() {
    // ユーティリティクラスのため、インスタンス化を禁止
  }

  /**
   * XPath式の安全性を検証します
   *
   * @param xpath 検証対象のXPath式
   * @throws SecurityException XPath式が安全でない場合
   * @throws IllegalArgumentException xpath引数がnullまたは空の場合
   */
  public static void validateXPath(String xpath) {
    if (xpath == null || xpath.isBlank()) {
      throw new IllegalArgumentException("TreePath expression cannot be null or empty");
    }

    String trimmedXpath = xpath.trim();

    // 基本的なインジェクションパターンチェック
    validateBasicInjectionPatterns(trimmedXpath);

    // 危険な関数の使用チェック
    validateDangerousFunctions(trimmedXpath);

    // 外部参照の検出
    validateExternalReferences(trimmedXpath);

    // SQLインジェクション類似パターンの検出
    validateSqlLikeInjection(trimmedXpath);

    // 厳格モードでの追加検証
    validateStrictMode(trimmedXpath);

    if (securityLogger.isDebugEnabled()) {
      securityLogger.debug("TreePath validation passed: {}", sanitizeForLogging(trimmedXpath));
    }
  }

  /**
   * XPath式をサニタイズします（エスケープ処理）
   *
   * @param xpath サニタイズ対象のXPath式
   * @return サニタイズされたXPath式
   */
  public static String sanitizeXPath(String xpath) {
    if (xpath == null) {
      return null;
    }

    String sanitized = xpath;

    // 危険な文字を除去（エスケープではなく除去）
    sanitized = sanitized.replaceAll("[';\"]", "");
    sanitized = sanitized.replace(";", "");

    // 改行コードの除去
    sanitized = sanitized.replace("\\r", "");
    sanitized = sanitized.replace("\\n", " ");

    // 連続する空白の正規化
    sanitized = sanitized.replaceAll("\\s+", " ");

    if (securityLogger.isDebugEnabled()) {
      securityLogger.debug(
          "TreePath sanitized: {} -> {}", sanitizeForLogging(xpath), sanitizeForLogging(sanitized));
    }

    return sanitized;
  }

  /**
   * XPath式が安全かどうかを判定します（例外を投げない版）
   *
   * @param xpath 検証対象のXPath式
   * @return 安全な場合true、そうでない場合false
   */
  public static boolean isXPathSafe(String xpath) {
    try {
      validateXPath(xpath);
      return true;
    } catch (SecurityException | IllegalArgumentException e) {
      securityLogger.warn("Unsafe TreePath detected: {}", sanitizeForLogging(xpath));
      return false;
    }
  }

  /**
   * 許可されたXPath関数のリストを取得します
   *
   * @return 許可された関数の配列
   */
  public static String[] getAllowedXPathFunctions() {
    return new String[] {
      "text()",
      "node()",
      "position()",
      "last()",
      "count()",
      "name()",
      "local-name()",
      "namespace-uri()",
      "boolean()",
      "number()",
      "string()",
      "string-length()",
      "concat()",
      "substring()",
      "substring-before()",
      "substring-after()",
      "normalize-space()",
      "translate()",
      "contains()",
      "starts-with()",
      "sum()",
      "floor()",
      "ceiling()",
      "round()"
    };
  }

  // ===========================================
  // Private Helper Methods
  // ===========================================

  private static void validateBasicInjectionPatterns(String xpath) {
    if (XPATH_INJECTION_PATTERN.matcher(xpath).matches()) {
      securityLogger.warn(
          "Basic TreePath injection pattern detected: {}", sanitizeForLogging(xpath));
      throw new SecurityException(
          "Potentially malicious TreePath expression detected: basic injection pattern");
    }
  }

  private static void validateDangerousFunctions(String xpath) {
    if (DANGEROUS_FUNCTIONS_PATTERN.matcher(xpath).matches()) {
      securityLogger.warn("Dangerous TreePath function detected: {}", sanitizeForLogging(xpath));
      throw new SecurityException(
          "Potentially malicious TreePath expression detected: dangerous function usage");
    }
  }

  private static void validateExternalReferences(String xpath) {
    if (EXTERNAL_REFERENCE_PATTERN.matcher(xpath).matches()) {
      securityLogger.warn("External reference in TreePath detected: {}", sanitizeForLogging(xpath));
      throw new SecurityException(
          "Potentially malicious TreePath expression detected: external reference");
    }
  }

  private static void validateSqlLikeInjection(String xpath) {
    if (SQL_LIKE_INJECTION_PATTERN.matcher(xpath).matches()) {
      securityLogger.warn(
          "SQL-like injection pattern in TreePath detected: {}", sanitizeForLogging(xpath));
      throw new SecurityException(
          "Potentially malicious TreePath expression detected: SQL-like injection pattern");
    }
  }

  private static void validateStrictMode(String xpath) {
    checkLength(xpath);
    checkNesting(xpath);
    checkOperators(xpath);
    checkWildcards(xpath);
  }

  private static void checkLength(String xpath) {
    if (xpath.length() > 1000) {
      securityLogger.warn(
          "TreePath expression too long in strict mode: {} characters", xpath.length());
      throw new SecurityException("TreePath expression exceeds maximum length in strict mode");
    }
  }

  private static void checkNesting(String xpath) {
    long nestingLevel = xpath.chars().filter(ch -> ch == '[').count();
    if (nestingLevel > 10) {
      securityLogger.warn(
          "TreePath expression has too deep nesting in strict mode: {} levels", nestingLevel);
      throw new SecurityException("TreePath expression has excessive nesting in strict mode");
    }
  }

  private static void checkOperators(String xpath) {
    // 論理演算子 and / or / not は predicate [...] 内でのみ式として意味を持つ。
    // predicate 外（要素名・属性名・ステップ）に現れる and / or / not は常に Name の一部であり、
    // 演算子として解釈してはならない（例: /root/and、brand/colors/notes、//a[@x='1']/or/@not）。
    // したがって predicate 内のテキストだけを対象に独立トークンとして検出することで、
    // 「より長い Name の部分文字列」と「Name そのものが and/or/not」の両ケースを誤検知から除外する。
    String inPredicate = extractPredicateText(xpath);
    if (OPERATOR_AND_PATTERN.matcher(inPredicate).find()
        && OPERATOR_OR_PATTERN.matcher(inPredicate).find()
        && OPERATOR_NOT_PATTERN.matcher(inPredicate).find()) {
      securityLogger.warn(
          "Complex operator combination in TreePath in strict mode: {}", sanitizeForLogging(xpath));
      throw new SecurityException("Complex operator combinations not allowed in strict mode");
    }
  }

  /**
   * XPath 式中の predicate [...] 内のテキストだけを連結して返す。 ネストした角括弧にも対応する。引用符内の角括弧は構文上不正で本来出現しないため、
   * 構造解析の単純さを優先して特別扱いしない。
   */
  private static String extractPredicateText(String xpath) {
    StringBuilder predicateText = new StringBuilder();
    int depth = 0;
    for (int i = 0; i < xpath.length(); i++) {
      char c = xpath.charAt(i);
      if (c == '[') {
        depth++;
      } else if (c == ']') {
        if (depth > 0) {
          depth--;
        }
      } else if (depth > 0) {
        predicateText.append(c);
      }
    }
    return predicateText.toString();
  }

  private static void checkWildcards(String xpath) {
    long wildcardCount = xpath.chars().filter(ch -> ch == '*').count();
    if (wildcardCount > 5) {
      securityLogger.warn(
          "Excessive wildcard usage in TreePath in strict mode: {} wildcards", wildcardCount);
      throw new SecurityException("Excessive wildcard usage not allowed in strict mode");
    }
  }

  private static String sanitizeForLogging(String xpath) {
    if (xpath == null) {
      return "null";
    }

    // ログ出力用のサニタイズ（機密情報や長すぎる文字列の対策）
    String sanitized = xpath;

    // 長すぎる場合は省略
    if (sanitized.length() > 100) {
      sanitized = sanitized.substring(0, 97) + "...";
    }

    // 制御文字を除去
    sanitized = sanitized.replaceAll("[\\p{Cntrl}\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}]", "");

    return sanitized;
  }
}
