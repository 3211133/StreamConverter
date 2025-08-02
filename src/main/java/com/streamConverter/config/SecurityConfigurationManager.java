package com.streamConverter.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 本番環境向けセキュリティ設定管理クラス
 *
 * <p>このクラスは、本番環境での安全な運用を支援するため、 セキュリティ関連の設定値を一元管理します。
 *
 * <p>主な機能:
 *
 * <ul>
 *   <li>XML外部エンティティ攻撃防止設定の管理
 *   <li>XPathインジェクション防止設定の管理
 *   <li>パストラバーサル攻撃防止設定の管理
 *   <li>入力値検証設定の管理
 *   <li>監査ログ設定の管理
 * </ul>
 *
 * @since 1.0.0
 */
public class SecurityConfigurationManager {

  private static final Logger logger = LoggerFactory.getLogger(SecurityConfigurationManager.class);
  private static final Logger securityLogger =
      LoggerFactory.getLogger("com.streamConverter.security");

  private static SecurityConfigurationManager instance;
  private final Properties securityConfig;
  private final String activeProfile;

  private SecurityConfigurationManager() {
    this.activeProfile = determineActiveProfile();
    this.securityConfig = loadSecurityConfiguration();
    logSecurityConfigurationStatus();
  }

  /**
   * SecurityConfigurationManagerのシングルトンインスタンスを取得します
   *
   * @return SecurityConfigurationManagerインスタンス
   */
  public static synchronized SecurityConfigurationManager getInstance() {
    if (instance == null) {
      instance = new SecurityConfigurationManager();
    }
    return instance;
  }

  /**
   * アクティブなプロファイルを取得します
   *
   * @return アクティブプロファイル名
   */
  public String getActiveProfile() {
    return activeProfile;
  }

  /**
   * 本番環境かどうかを判定します
   *
   * @return 本番環境の場合true、そうでない場合false
   */
  public boolean isProductionEnvironment() {
    return "prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile);
  }

  // ===========================================
  // XML Security Settings
  // ===========================================

  /**
   * XML外部エンティティの無効化設定を取得します
   *
   * @return XML外部エンティティを無効化する場合true
   */
  public boolean isXmlExternalEntitiesDisabled() {
    return getBooleanProperty("security.xml.disable-external-entities", true);
  }

  /**
   * XMLドキュメントタイプ宣言の無効化設定を取得します
   *
   * @return ドキュメントタイプ宣言を無効化する場合true
   */
  public boolean isXmlDoctypeDeclarationsDisabled() {
    return getBooleanProperty("security.xml.disable-doctype-declarations", true);
  }

  /**
   * 外部DTD読み込みの無効化設定を取得します
   *
   * @return 外部DTD読み込みを無効化する場合true
   */
  public boolean isLoadExternalDtdDisabled() {
    return getBooleanProperty("security.xml.load-external-dtd", false);
  }

  // ===========================================
  // XPath Security Settings
  // ===========================================

  /**
   * XPath検証の有効化設定を取得します
   *
   * @return XPath検証を有効化する場合true
   */
  public boolean isXPathValidationEnabled() {
    return getBooleanProperty("security.xpath.validation.enabled", true);
  }

  /**
   * XPath厳格モードの設定を取得します
   *
   * @return XPath厳格モードが有効な場合true
   */
  public boolean isXPathStrictModeEnabled() {
    return getBooleanProperty("security.xpath.strict-mode", true);
  }

  // ===========================================
  // Path Traversal Prevention
  // ===========================================

  /**
   * パストラバーサル防止の設定を取得します
   *
   * @return パストラバーサル防止が有効な場合true
   */
  public boolean isPathTraversalPreventionEnabled() {
    return getBooleanProperty("security.path-traversal.prevention", true);
  }

  /**
   * 親ディレクトリ参照許可の設定を取得します
   *
   * @return 親ディレクトリ参照を許可する場合true
   */
  public boolean isParentReferencesAllowed() {
    return getBooleanProperty("security.path-traversal.allow-parent-references", false);
  }

  /**
   * ファイルアクセスのワークスペース制限設定を取得します
   *
   * @return ワークスペースへの制限が有効な場合true
   */
  public boolean isFileAccessRestrictedToWorkspace() {
    return getBooleanProperty("security.file-access.restrict-to-workspace", true);
  }

  // ===========================================
  // Input Validation Settings
  // ===========================================

  /**
   * 最大ファイルサイズ制限を取得します
   *
   * @return 最大ファイルサイズ（バイト）
   */
  public long getMaxFileSize() {
    String maxSizeStr = getStringProperty("security.input.max-file-size", "100MB");
    return parseFileSize(maxSizeStr);
  }

  /**
   * 許可されるファイル拡張子リストを取得します
   *
   * @return 許可される拡張子の配列
   */
  public String[] getAllowedFileExtensions() {
    String extensions =
        getStringProperty("security.input.allowed-file-extensions", ".json,.xml,.csv,.txt");
    return extensions.split(",");
  }

  /**
   * 文字エンコーディング検証の有効化設定を取得します
   *
   * @return エンコーディング検証が有効な場合true
   */
  public boolean isEncodingValidationEnabled() {
    return getBooleanProperty("security.input.validate-encoding", true);
  }

  // ===========================================
  // Audit and Monitoring Settings
  // ===========================================

  /**
   * 監査ログの有効化設定を取得します
   *
   * @return 監査ログが有効な場合true
   */
  public boolean isAuditEnabled() {
    return getBooleanProperty("audit.enabled", isProductionEnvironment());
  }

  /**
   * セキュリティログの有効化設定を取得します
   *
   * @return セキュリティログが有効な場合true
   */
  public boolean isSecurityLoggingEnabled() {
    return getBooleanProperty("logging.security.enabled", isProductionEnvironment());
  }

  /**
   * パフォーマンス監視の有効化設定を取得します
   *
   * @return パフォーマンス監視が有効な場合true
   */
  public boolean isPerformanceMonitoringEnabled() {
    return getBooleanProperty("monitoring.enabled", true);
  }

  // ===========================================
  // Private Helper Methods
  // ===========================================

  private String determineActiveProfile() {
    // 環境変数から取得
    String profile = System.getenv("STREAMCONVERTER_PROFILE");
    if (profile != null && !profile.trim().isEmpty()) {
      return profile.trim();
    }

    // システムプロパティから取得
    profile = System.getProperty("streamconverter.profiles.active");
    if (profile != null && !profile.trim().isEmpty()) {
      return profile.trim();
    }

    // デフォルトは development
    return "dev";
  }

  private Properties loadSecurityConfiguration() {
    Properties props = new Properties();

    // デフォルト設定を読み込み
    loadPropertiesFile(props, "application.properties");

    // プロファイル固有の設定を読み込み（存在する場合）
    if (activeProfile != null && !activeProfile.isEmpty()) {
      loadPropertiesFile(props, "application-" + activeProfile + ".properties");
    }

    return props;
  }

  private void loadPropertiesFile(Properties props, String filename) {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(filename)) {
      if (input != null) {
        props.load(input);
        logger.debug("Loaded security configuration from: {}", filename);
      } else {
        logger.debug("Configuration file not found: {}", filename);
      }
    } catch (IOException e) {
      logger.warn("Failed to load configuration file: {}", filename, e);
    }
  }

  private boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = securityConfig.getProperty(key);
    if (value != null) {
      return Boolean.parseBoolean(value.trim());
    }
    return defaultValue;
  }

  private String getStringProperty(String key, String defaultValue) {
    return securityConfig.getProperty(key, defaultValue);
  }

  private long parseFileSize(String sizeStr) {
    if (sizeStr == null || sizeStr.trim().isEmpty()) {
      return 100 * 1024 * 1024; // Default 100MB
    }

    sizeStr = sizeStr.trim().toUpperCase();
    long multiplier = 1;

    if (sizeStr.endsWith("KB")) {
      multiplier = 1024;
      sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
    } else if (sizeStr.endsWith("MB")) {
      multiplier = 1024 * 1024;
      sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
    } else if (sizeStr.endsWith("GB")) {
      multiplier = 1024 * 1024 * 1024;
      sizeStr = sizeStr.substring(0, sizeStr.length() - 2);
    }

    try {
      return Long.parseLong(sizeStr.trim()) * multiplier;
    } catch (NumberFormatException e) {
      logger.warn("Invalid file size format: {}, using default 100MB", sizeStr);
      return 100 * 1024 * 1024; // Default 100MB
    }
  }

  private void logSecurityConfigurationStatus() {
    if (isSecurityLoggingEnabled()) {
      securityLogger.info(
          "Security Configuration Manager initialized for profile: {}", activeProfile);
      securityLogger.info("Production environment: {}", isProductionEnvironment());
      securityLogger.info("XML external entities disabled: {}", isXmlExternalEntitiesDisabled());
      securityLogger.info("XPath validation enabled: {}", isXPathValidationEnabled());
      securityLogger.info(
          "Path traversal prevention enabled: {}", isPathTraversalPreventionEnabled());
      securityLogger.info("Audit logging enabled: {}", isAuditEnabled());
    }
  }
}
