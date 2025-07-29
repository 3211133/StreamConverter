package com.streamConverter.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * キャッシュ設定クラス
 *
 * <p>Caffeineキャッシュを使用したバリデーション結果と変換設定のキャッシング機能を提供します。
 *
 * <p>キャッシュ戦略:
 *
 * <ul>
 *   <li>validationCache: バリデーション結果をキャッシュ (TTL: 30分, 最大1000エントリ)
 *   <li>transformCache: 変換設定をキャッシュ (TTL: 1時間, 最大500エントリ)
 *   <li>schemaCache: スキーマ情報をキャッシュ (TTL: 1時間, 最大100エントリ)
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Configuration
@EnableCaching
public class CacheConfig {

  /**
   * Caffeineベースのキャッシュマネージャーを構成します。
   *
   * <p>設定詳細:
   *
   * <ul>
   *   <li>初期容量: 100エントリ
   *   <li>最大サイズ: 1000エントリ
   *   <li>TTL: 30分後に自動削除
   *   <li>統計情報: 有効（メトリクス監視用）
   * </ul>
   *
   * @return 設定済みCaffeineCacheManager
   */
  @Bean
  @Primary
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    // Caffeineキャッシュの基本設定
    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .initialCapacity(100)
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .recordStats() // メトリクス収集を有効化
        );

    // キャッシュ名の事前定義
    cacheManager.setCacheNames(
        java.util.Arrays.asList("validationCache", "transformCache", "schemaCache"));

    return cacheManager;
  }

  /**
   * バリデーション専用キャッシュマネージャーを構成します。
   *
   * <p>バリデーション処理は頻繁に実行されるため、専用の設定で最適化:
   *
   * <ul>
   *   <li>最大サイズ: 2000エントリ
   *   <li>TTL: 15分（短めに設定）
   *   <li>アクセス後削除: 10分
   * </ul>
   *
   * @return バリデーション専用CacheManager
   */
  @Bean("validationCacheManager")
  public CacheManager validationCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .initialCapacity(200)
            .maximumSize(2000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .expireAfterAccess(Duration.ofMinutes(10))
            .recordStats());

    cacheManager.setCacheNames(java.util.Arrays.asList("validationResults", "schemaValidations"));

    return cacheManager;
  }

  /**
   * 変換設定専用キャッシュマネージャーを構成します。
   *
   * <p>変換設定は変更頻度が低いため、長期キャッシュ:
   *
   * <ul>
   *   <li>最大サイズ: 500エントリ
   *   <li>TTL: 1時間
   *   <li>アクセス後削除: 30分
   * </ul>
   *
   * @return 変換設定専用CacheManager
   */
  @Bean("transformCacheManager")
  public CacheManager transformCacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();

    cacheManager.setCaffeine(
        Caffeine.newBuilder()
            .initialCapacity(50)
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(1))
            .expireAfterAccess(Duration.ofMinutes(30))
            .recordStats());

    cacheManager.setCacheNames(java.util.Arrays.asList("transformConfigs", "pipelineConfigs"));

    return cacheManager;
  }
}
