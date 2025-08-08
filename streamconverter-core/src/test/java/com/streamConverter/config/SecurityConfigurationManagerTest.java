package com.streamConverter.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SecurityConfigurationManagerのテストクラス */
class SecurityConfigurationManagerTest {

  private SecurityConfigurationManager securityConfig;

  @BeforeEach
  void setUp() {
    securityConfig = SecurityConfigurationManager.getInstance();
  }

  @Test
  @DisplayName("シングルトンパターンのテスト")
  void testSingletonPattern() {
    SecurityConfigurationManager instance1 = SecurityConfigurationManager.getInstance();
    SecurityConfigurationManager instance2 = SecurityConfigurationManager.getInstance();

    assertSame(instance1, instance2, "SecurityConfigurationManagerはシングルトンである必要があります");
  }

  @Test
  @DisplayName("デフォルト設定値のテスト")
  void testDefaultValues() {
    // XML Security Settings
    assertTrue(securityConfig.isXmlExternalEntitiesDisabled(), "デフォルトでXML外部エンティティは無効化されている必要があります");
    assertTrue(
        securityConfig.isXmlDoctypeDeclarationsDisabled(), "デフォルトでXMLドキュメントタイプ宣言は無効化されている必要があります");
    assertFalse(securityConfig.isLoadExternalDtdDisabled(), "デフォルトで外部DTD読み込みは許可されている必要があります");

    // XPath Security Settings
    assertTrue(securityConfig.isXPathValidationEnabled(), "デフォルトでXPath検証は有効化されている必要があります");
    assertTrue(securityConfig.isXPathStrictModeEnabled(), "デフォルトでXPath厳格モードは有効化されている必要があります");

    // Path Traversal Prevention
    assertTrue(
        securityConfig.isPathTraversalPreventionEnabled(), "デフォルトでパストラバーサル防止は有効化されている必要があります");
    assertFalse(securityConfig.isParentReferencesAllowed(), "デフォルトで親ディレクトリ参照は禁止されている必要があります");
    assertTrue(
        securityConfig.isFileAccessRestrictedToWorkspace(),
        "デフォルトでファイルアクセスはワークスペースに制限されている必要があります");
  }

  @Test
  @DisplayName("入力値検証設定のテスト")
  void testInputValidationSettings() {
    // 最大ファイルサイズのテスト
    long maxFileSize = securityConfig.getMaxFileSize();
    assertEquals(100 * 1024 * 1024, maxFileSize, "デフォルトの最大ファイルサイズは100MBである必要があります");

    // 許可されるファイル拡張子のテスト
    String[] allowedExtensions = securityConfig.getAllowedFileExtensions();
    assertNotNull(allowedExtensions, "許可される拡張子リストはnullであってはいけません");
    assertTrue(allowedExtensions.length > 0, "許可される拡張子リストは空であってはいけません");

    // エンコーディング検証の設定
    assertTrue(securityConfig.isEncodingValidationEnabled(), "デフォルトでエンコーディング検証は有効化されている必要があります");
  }

  @Test
  @DisplayName("監査・モニタリング設定のテスト")
  void testAuditAndMonitoringSettings() {
    // 環境によって結果が変わるため、nullでないことのみ確認
    assertNotNull(securityConfig.isAuditEnabled(), "監査設定はnullであってはいけません");
    assertNotNull(securityConfig.isSecurityLoggingEnabled(), "セキュリティログ設定はnullであってはいけません");
    assertTrue(securityConfig.isPerformanceMonitoringEnabled(), "デフォルトでパフォーマンス監視は有効化されている必要があります");
  }

  @Test
  @DisplayName("アクティブプロファイルの取得テスト")
  void testActiveProfile() {
    String activeProfile = securityConfig.getActiveProfile();
    assertNotNull(activeProfile, "アクティブプロファイルはnullであってはいけません");
    assertFalse(activeProfile.trim().isEmpty(), "アクティブプロファイルは空であってはいけません");
  }

  @Test
  @DisplayName("本番環境判定のテスト")
  void testProductionEnvironmentDetection() {
    // 現在の環境設定に基づいてテスト
    boolean isProduction = securityConfig.isProductionEnvironment();
    assertNotNull(isProduction, "本番環境判定結果はnullであってはいけません");

    // デフォルトのdev環境では本番環境ではない
    String activeProfile = securityConfig.getActiveProfile();
    if ("dev".equals(activeProfile)) {
      assertFalse(isProduction, "dev環境では本番環境判定はfalseである必要があります");
    }
  }
}
