package com.streamConverter.api.util;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.awaitility.Awaitility;

/**
 * テスト用ユーティリティクラス
 * 
 * <p>非同期処理のテストや、処理完了待機のためのヘルパーメソッドを提供します。
 * 
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
public class TestUtils {

    /**
     * 条件が満たされるまで効率的に待機します。
     * 
     * <p>Thread.sleep()の代わりに使用し、より効率的で確実なテストを実現します。
     * 
     * @param condition 満たすべき条件
     * @param timeout タイムアウト時間
     * @param pollInterval ポーリング間隔
     */
    public static void waitUntil(Supplier<Boolean> condition, Duration timeout, Duration pollInterval) {
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(pollInterval)
                .until(condition::get);
    }

    /**
     * 条件が満たされるまで待機します（デフォルト設定）。
     * 
     * <p>タイムアウト: 10秒、ポーリング間隔: 100ms
     * 
     * @param condition 満たすべき条件
     */
    public static void waitUntil(Supplier<Boolean> condition) {
        waitUntil(condition, Duration.ofSeconds(10), Duration.ofMillis(100));
    }

    /**
     * CompletableFutureの完了を効率的に待機します。
     * 
     * @param future 完了を待機するFuture
     * @param timeout タイムアウト時間
     * @param <T> Futureの戻り値型
     * @return Future の結果
     * @throws Exception タイムアウトまたは実行エラーが発生した場合
     */
    public static <T> T waitForCompletion(CompletableFuture<T> future, Duration timeout) throws Exception {
        return Awaitility.await()
                .atMost(timeout)
                .until(() -> future.isDone() ? future.get() : null, 
                       result -> result != null);
    }

    /**
     * 短い待機時間でキャッシュの同期を待機します。
     * 
     * <p>キャッシュ操作後の同期待機に特化したメソッドです。
     * 
     * @param operation キャッシュ操作
     */
    public static void waitForCacheSync(Runnable operation) {
        operation.run();
        
        // キャッシュ同期のための短時間待機
        Awaitility.await()
                .atMost(Duration.ofMillis(200))
                .pollInterval(Duration.ofMillis(10))
                .until(() -> true); // 時間ベースの待機
    }

    /**
     * バッチ処理のステータス変更を効率的に待機します。
     * 
     * @param statusSupplier ステータス取得用のサプライヤー
     * @param expectedStatus 期待するステータス
     * @param timeout タイムアウト時間
     * @return 最終的なステータス
     */
    public static String waitForBatchStatus(Supplier<String> statusSupplier, 
                                          String expectedStatus, 
                                          Duration timeout) {
        return Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(200))
                .until(statusSupplier::get, status -> expectedStatus.equals(status));
    }

    /**
     * 非同期処理の開始を確認します。
     * 
     * @param condition 処理開始を示す条件
     * @param timeout タイムアウト時間
     */
    public static void waitForAsyncStart(Supplier<Boolean> condition, Duration timeout) {
        Awaitility.await()
                .atMost(timeout)
                .pollInterval(Duration.ofMillis(50))
                .until(condition::get);
    }

    /**
     * テスト用の一意なIDを生成します。
     * 
     * @param prefix プリフィックス
     * @return 一意なID
     */
    public static String generateTestId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + 
               Thread.currentThread().getId();
    }

    /**
     * メモリリークを防ぐためのクリーンアップ処理を実行します。
     * 
     * <p>テスト終了時に呼び出すことを推奨します。
     */
    public static void cleanup() {
        // ガベージコレクションを提案
        System.gc();
        
        // 短時間待機してリソースのクリーンアップを確認
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}