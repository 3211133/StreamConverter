package com.streamconverter.security;

import com.streamconverter.config.SecurityConfigurationManager;
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
public class SecureXPathValidator {

  private static final Logger logger = LoggerFactory.getLogger(SecureXPathValidator.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  private static final SecurityConfigurationManager securityConfig =
      SecurityConfigurationManager.getInstance();

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
    if (xpath == null || xpath.trim().isEmpty()) {
      throw new IllegalArgumentException("TreePath expression cannot be null or empty");
    }

    // XPath検証が無効な場合はスキップ
    if (!securityConfig.isXPathValidationEnabled()) {
      logger.debug("TreePath validation is disabled");
      return;
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
    if (securityConfig.isXPathStrictModeEnabled()) {
      validateStrictMode(trimmedXpath);
    }

    securityLogger.debug("TreePath validation passed: {}", sanitizeForLogging(trimmedXpath));
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

    securityLogger.debug(
        "TreePath sanitized: {} -> {}", sanitizeForLogging(xpath), sanitizeForLogging(sanitized));

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
    // 厳格モードでの追加検証

    // 長すぎるXPath式を拒否
    if (xpath.length() > 1000) {
      securityLogger.warn(
          "TreePath expression too long in strict mode: {} characters", xpath.length());
      throw new SecurityException("TreePath expression exceeds maximum length in strict mode");
    }

    // 深いネストを拒否
    long nestingLevel = xpath.chars().filter(ch -> ch == '[').count();
    if (nestingLevel > 10) {
      securityLogger.warn(
          "TreePath expression has too deep nesting in strict mode: {} levels", nestingLevel);
      throw new SecurityException("TreePath expression has excessive nesting in strict mode");
    }

    // 複雑な演算子の組み合わせを制限
    if (xpath.contains("and") && xpath.contains("or") && xpath.contains("not")) {
      securityLogger.warn(
          "Complex operator combination in TreePath in strict mode: {}", sanitizeForLogging(xpath));
      throw new SecurityException("Complex operator combinations not allowed in strict mode");
    }

    // ワイルドカードの過度な使用を制限
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

  /** セキュリティ設定の現在の状態をログに出力します */
  public static void logSecurityStatus() {
    if (securityConfig.isSecurityLoggingEnabled()) {
      securityLogger.info("=== TreePath Security Configuration Status ===");
      securityLogger.info(
          "TreePath Validation Enabled: {}", securityConfig.isXPathValidationEnabled());
      securityLogger.info(
          "TreePath Strict Mode Enabled: {}", securityConfig.isXPathStrictModeEnabled());
      securityLogger.info("Production Environment: {}", securityConfig.isProductionEnvironment());
      securityLogger.info("===========================================");
    }
  }
}
