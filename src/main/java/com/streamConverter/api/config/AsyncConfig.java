package com.streamConverter.api.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 非同期処理設定クラス
 *
 * <p>バッチ処理や長時間実行タスクの非同期処理設定を提供します。
 *
 * <p>スレッドプール設定:
 *
 * <ul>
 *   <li>コアスレッド数: 5
 *   <li>最大スレッド数: 20
 *   <li>キューサイズ: 100
 *   <li>スレッド名プリフィックス: BatchProcessing-
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

  /**
   * バッチ処理用の非同期実行器を設定します。
   *
   * <p>バッチ処理に最適化されたスレッドプール設定:
   *
   * <ul>
   *   <li>コアプールサイズ: 5スレッド
   *   <li>最大プールサイズ: 20スレッド
   *   <li>キューキャパシティ: 100タスク
   *   <li>キープアライブ時間: 60秒
   * </ul>
   *
   * @return 設定済みThreadPoolTaskExecutor
   */
  @Bean(name = "batchTaskExecutor")
  public Executor batchTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    // 基本スレッドプール設定
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setKeepAliveSeconds(60);

    // スレッド名の設定
    executor.setThreadNamePrefix("BatchProcessing-");

    // 拒否ポリシーの設定（キューが満杯の場合は呼び出し元スレッドで実行）
    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());

    // シャットダウン時の設定
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);

    // 初期化
    executor.initialize();

    logger.info(
        "Batch task executor initialized - coreSize: {}, maxSize: {}, queueCapacity: {}",
        executor.getCorePoolSize(),
        executor.getMaxPoolSize(),
        executor.getQueueCapacity());

    return executor;
  }

  /**
   * 一般的な非同期処理用の実行器を設定します。
   *
   * <p>軽量タスク用の設定:
   *
   * <ul>
   *   <li>コアプールサイズ: 3スレッド
   *   <li>最大プールサイズ: 10スレッド
   *   <li>キューキャパシティ: 50タスク
   * </ul>
   *
   * @return 設定済みThreadPoolTaskExecutor
   */
  @Bean(name = "generalTaskExecutor")
  public Executor generalTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setKeepAliveSeconds(30);
    executor.setThreadNamePrefix("GeneralAsync-");

    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);

    executor.initialize();

    logger.info(
        "General task executor initialized - coreSize: {}, maxSize: {}, queueCapacity: {}",
        executor.getCorePoolSize(),
        executor.getMaxPoolSize(),
        executor.getQueueCapacity());

    return executor;
  }

  /**
   * 通知送信用の非同期実行器を設定します。
   *
   * <p>通知処理用の軽量設定:
   *
   * <ul>
   *   <li>コアプールサイズ: 2スレッド
   *   <li>最大プールサイズ: 5スレッド
   *   <li>キューキャパシティ: 25タスク
   * </ul>
   *
   * @return 設定済みThreadPoolTaskExecutor
   */
  @Bean(name = "notificationTaskExecutor")
  public Executor notificationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(25);
    executor.setKeepAliveSeconds(30);
    executor.setThreadNamePrefix("Notification-");

    executor.setRejectedExecutionHandler(
        new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(15);

    executor.initialize();

    logger.info(
        "Notification task executor initialized - coreSize: {}, maxSize: {}, queueCapacity: {}",
        executor.getCorePoolSize(),
        executor.getMaxPoolSize(),
        executor.getQueueCapacity());

    return executor;
  }
}
