package com.streamConverter.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * キャッシュ管理サービス
 *
 * <p>バリデーション結果と変換設定のキャッシュ管理を提供します。
 *
 * <p>主要機能:
 *
 * <ul>
 *   <li>バリデーション結果の自動キャッシュ
 *   <li>変換設定の自動キャッシュ
 *   <li>スキーマ情報の自動キャッシュ
 *   <li>キャッシュの手動削除・更新
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Service
public class CacheService {

  private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

  /**
   * バリデーション結果をキャッシュします。
   *
   * <p>キー生成戦略:
   *
   * <ul>
   *   <li>データ内容のハッシュ値をキーとして使用
   *   <li>同一データの重複バリデーションを防止
   * </ul>
   *
   * @param data バリデーション対象データ
   * @param format データフォーマット (JSON, XML, CSV)
   * @param schema バリデーションスキーマ
   * @return バリデーション結果 (true: 有効, false: 無効)
   */
  @Cacheable(
      value = "validationResults",
      cacheManager = "validationCacheManager",
      key = "#data.hashCode() + '_' + #format + '_' + #schema.hashCode()")
  public boolean validateAndCache(String data, String format, String schema) {
    logger.info("Performing validation for format: {} (cache miss)", format);

    // 実際のバリデーション処理をここに実装
    // 現在はデモとして常にtrueを返す
    boolean isValid = performActualValidation(data, format, schema);

    logger.info("Validation completed - result: {}, cached for future use", isValid);
    return isValid;
  }

  /**
   * 変換設定をキャッシュします。
   *
   * <p>パイプライン設定の重複計算を防止し、レスポンス時間を改善します。
   *
   * @param inputFormat 入力フォーマット
   * @param outputFormat 出力フォーマット
   * @param extractionPath 抽出パス
   * @return 変換設定オブジェクト
   */
  @Cacheable(
      value = "transformConfigs",
      cacheManager = "transformCacheManager",
      key = "#inputFormat + '_' + #outputFormat + '_' + #extractionPath")
  public TransformConfig getTransformConfig(
      String inputFormat, String outputFormat, String extractionPath) {
    logger.info("Building transform config for {}→{} (cache miss)", inputFormat, outputFormat);

    TransformConfig config = buildTransformConfig(inputFormat, outputFormat, extractionPath);

    logger.info("Transform config built and cached");
    return config;
  }

  /**
   * スキーマ情報をキャッシュします。
   *
   * @param schemaContent スキーマの内容
   * @param schemaType スキーマタイプ (JSON_SCHEMA, XSD, CSV_HEADER)
   * @return パースされたスキーマオブジェクト
   */
  @Cacheable(
      value = "schemaCache",
      key =
          "#schemaType + '_' + T(java.security.MessageDigest).getInstance('SHA-256').digest(#schemaContent.getBytes()).toString()")
  public Object getSchemaObject(String schemaContent, String schemaType) {
    logger.info("Parsing schema of type: {} (cache miss)", schemaType);

    Object schemaObject = parseSchema(schemaContent, schemaType);

    logger.info("Schema parsed and cached");
    return schemaObject;
  }

  /**
   * バリデーションキャッシュを削除します。
   *
   * @param data 削除対象データ
   * @param format データフォーマット
   * @param schema バリデーションスキーマ
   */
  @CacheEvict(
      value = "validationResults",
      cacheManager = "validationCacheManager",
      key = "#data.hashCode() + '_' + #format + '_' + #schema.hashCode()")
  public void evictValidationCache(String data, String format, String schema) {
    logger.info("Evicted validation cache for format: {}", format);
  }

  /** すべてのバリデーションキャッシュを削除します。 */
  @CacheEvict(
      value = "validationResults",
      cacheManager = "validationCacheManager",
      allEntries = true)
  public void evictAllValidationCache() {
    logger.info("Evicted all validation cache entries");
  }

  /**
   * 変換設定キャッシュを削除します。
   *
   * @param inputFormat 入力フォーマット
   * @param outputFormat 出力フォーマット
   * @param extractionPath 抽出パス
   */
  @CacheEvict(
      value = "transformConfigs",
      cacheManager = "transformCacheManager",
      key = "#inputFormat + '_' + #outputFormat + '_' + #extractionPath")
  public void evictTransformCache(String inputFormat, String outputFormat, String extractionPath) {
    logger.info("Evicted transform cache for {}→{}", inputFormat, outputFormat);
  }

  /** すべてのキャッシュを削除します。 */
  @CacheEvict(
      value = {"validationResults", "transformConfigs", "schemaCache"},
      allEntries = true)
  public void evictAllCaches() {
    logger.info("Evicted all cache entries");
  }

  /**
   * 実際のバリデーション処理を実行します。
   *
   * @param data バリデーション対象データ
   * @param format データフォーマット
   * @param schema バリデーションスキーマ
   * @return バリデーション結果
   */
  private boolean performActualValidation(String data, String format, String schema) {
    // 実際のバリデーション処理の実装
    // 現在はデモとしてデータ長をチェック
    return data != null && !data.trim().isEmpty() && data.length() > 5;
  }

  /**
   * 変換設定を構築します。
   *
   * @param inputFormat 入力フォーマット
   * @param outputFormat 出力フォーマット
   * @param extractionPath 抽出パス
   * @return 構築された変換設定
   */
  private TransformConfig buildTransformConfig(
      String inputFormat, String outputFormat, String extractionPath) {
    TransformConfig config = new TransformConfig();
    config.setInputFormat(inputFormat);
    config.setOutputFormat(outputFormat);
    config.setExtractionPath(extractionPath);
    config.setCreatedAt(System.currentTimeMillis());
    return config;
  }

  /**
   * スキーマをパースします。
   *
   * @param schemaContent スキーマの内容
   * @param schemaType スキーマタイプ
   * @return パースされたスキーマオブジェクト
   */
  private Object parseSchema(String schemaContent, String schemaType) {
    // 実際のスキーマパース処理の実装
    // 現在はデモとして文字列をそのまま返す
    return schemaType + ":" + schemaContent.substring(0, Math.min(100, schemaContent.length()));
  }

  /** 変換設定クラス */
  public static class TransformConfig {
    private String inputFormat;
    private String outputFormat;
    private String extractionPath;
    private long createdAt;

    // Getters and Setters
    public String getInputFormat() {
      return inputFormat;
    }

    public void setInputFormat(String inputFormat) {
      this.inputFormat = inputFormat;
    }

    public String getOutputFormat() {
      return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
      this.outputFormat = outputFormat;
    }

    public String getExtractionPath() {
      return extractionPath;
    }

    public void setExtractionPath(String extractionPath) {
      this.extractionPath = extractionPath;
    }

    public long getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(long createdAt) {
      this.createdAt = createdAt;
    }

    @Override
    public String toString() {
      return String.format(
          "TransformConfig{inputFormat='%s', outputFormat='%s', extractionPath='%s', createdAt=%d}",
          inputFormat, outputFormat, extractionPath, createdAt);
    }
  }
}
