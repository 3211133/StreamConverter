package com.streamConverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * PooledDatabaseFetchRuleの統合テスト
 *
 * <p>H2インメモリデータベースとHikariCP接続プールを使用してPooledDatabaseFetchRuleの パフォーマンスと並行処理能力をテストします。
 */
@DisabledOnOs({OS.WINDOWS, OS.MAC}) // 一時的にWindows/macOS環境では無効化
public class PooledDatabaseFetchRuleIntegrationTest {

  private static final String DB_URL_BASE = "jdbc:h2:mem:pooltest";
  private String dbUrl;
  private Connection connection;
  private HikariConnectionPoolConfig connectionPool;

  @BeforeEach
  public void setUp() throws Exception {
    // 各テスト毎に一意のデータベースURLを生成
    dbUrl = DB_URL_BASE + System.nanoTime() + ";DB_CLOSE_DELAY=-1";

    // H2インメモリデータベースに接続
    connection = DriverManager.getConnection(dbUrl);

    // テスト用テーブルとデータを作成
    try (Statement stmt = connection.createStatement()) {
      // ユーザーテーブル
      stmt.execute("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50), email VARCHAR(100))");
      stmt.execute("INSERT INTO users VALUES (1, 'John Doe', 'john@example.com')");
      stmt.execute("INSERT INTO users VALUES (2, 'Jane Smith', 'jane@example.com')");
      stmt.execute("INSERT INTO users VALUES (3, 'Bob Johnson', 'bob@example.com')");

      // パフォーマンステスト用の大きなテーブル
      stmt.execute("CREATE TABLE performance_test (id INT PRIMARY KEY, data VARCHAR(100))");
      for (int i = 1; i <= 1000; i++) {
        stmt.execute(String.format("INSERT INTO performance_test VALUES (%d, 'Data %d')", i, i));
      }
    }

    // HikariCP接続プールを初期化
    connectionPool = new HikariConnectionPoolConfig(dbUrl, 5, Duration.ofSeconds(30));
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (connectionPool != null) {
      connectionPool.close();
    }
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  @DisplayName("基本的なプール対応データ取得テスト")
  public void testBasicPooledDataFetch() {
    PooledDatabaseFetchRule rule =
        new PooledDatabaseFetchRule(connectionPool, "SELECT name FROM users WHERE id = ?");

    String result = rule.apply("1");
    assertEquals("John Doe", result);

    result = rule.apply("2");
    assertEquals("Jane Smith", result);

    result = rule.apply("3");
    assertEquals("Bob Johnson", result);
  }

  @Test
  @DisplayName("プール統計情報取得テスト")
  public void testPoolStats() {
    PooledDatabaseFetchRule rule =
        new PooledDatabaseFetchRule(connectionPool, "SELECT name FROM users WHERE id = ?");

    String stats = rule.getPoolStats();
    assertTrue(stats.contains("HikariCP"));
    assertTrue(stats.contains("active="));
    assertTrue(stats.contains("idle="));
    assertTrue(stats.contains("total="));
  }

  @Test
  @DisplayName("プール対応パフォーマンステスト")
  public void testPooledPerformance() {
    PooledDatabaseFetchRule rule =
        new PooledDatabaseFetchRule(
            connectionPool, "SELECT data FROM performance_test WHERE id = ?");

    long startTime = System.currentTimeMillis();

    // 大量のクエリ実行（プールの恩恵を確認）
    for (int i = 1; i <= 500; i++) {
      String result = rule.apply(String.valueOf(i));
      assertEquals("Data " + i, result);
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // プール使用により高速化されていること（500回のクエリが3秒以内）
    assertTrue(
        duration < 3000,
        "500 pooled queries should complete within 3 seconds, but took: " + duration + "ms");
  }

  @Test
  @DisplayName("並行アクセステスト")
  public void testConcurrentAccess() throws InterruptedException {
    PooledDatabaseFetchRule rule =
        new PooledDatabaseFetchRule(
            connectionPool, "SELECT data FROM performance_test WHERE id = ?");

    int numberOfThreads = 10;
    int queriesPerThread = 50;
    ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
    CountDownLatch latch = new CountDownLatch(numberOfThreads);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger errorCount = new AtomicInteger(0);

    for (int t = 0; t < numberOfThreads; t++) {
      final int threadId = t;
      executor.submit(
          () -> {
            try {
              for (int i = 1; i <= queriesPerThread; i++) {
                int dataId = (threadId * queriesPerThread + i) % 1000 + 1;
                String result = rule.apply(String.valueOf(dataId));

                if (result.equals("Data " + dataId)) {
                  successCount.incrementAndGet();
                } else if (result.startsWith("ERROR")) {
                  errorCount.incrementAndGet();
                }
              }
            } finally {
              latch.countDown();
            }
          });
    }

    // 全スレッドの完了を待機
    assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");

    executor.shutdown();

    // 結果検証
    int expectedSuccessCount = numberOfThreads * queriesPerThread;
    assertEquals(expectedSuccessCount, successCount.get(), "All concurrent queries should succeed");
    assertEquals(0, errorCount.get(), "No errors should occur in concurrent access");
  }

  @Test
  @DisplayName("プール枯渇時の動作テスト")
  public void testPoolExhaustion() throws InterruptedException {
    // 小さなプールを作成（最大2接続）
    HikariConnectionPoolConfig smallPool =
        new HikariConnectionPoolConfig(dbUrl, 2, Duration.ofSeconds(1));

    try {
      PooledDatabaseFetchRule rule =
          new PooledDatabaseFetchRule(smallPool, "SELECT data FROM performance_test WHERE id = ?");

      ExecutorService executor = Executors.newFixedThreadPool(5);
      CountDownLatch latch = new CountDownLatch(5);
      AtomicInteger completedTasks = new AtomicInteger(0);

      // プールサイズを超える並行タスクを実行
      for (int i = 0; i < 5; i++) {
        final int taskId = i + 1;
        executor.submit(
            () -> {
              try {
                String result = rule.apply(String.valueOf(taskId));
                if (!result.startsWith("ERROR")) {
                  completedTasks.incrementAndGet();
                }
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(10, TimeUnit.SECONDS), "All tasks should complete or timeout");
      assertTrue(completedTasks.get() > 0, "Some tasks should complete even with pool exhaustion");

      executor.shutdown();
    } finally {
      smallPool.close();
    }
  }

  @Test
  @DisplayName("プールvs非プールパフォーマンス比較テスト")
  public void testPoolVsNonPoolPerformance() {
    // 非プール版
    DatabaseFetchRule nonPooledRule =
        new DatabaseFetchRule(dbUrl, "SELECT data FROM performance_test WHERE id = ?");

    // プール版
    PooledDatabaseFetchRule pooledRule =
        new PooledDatabaseFetchRule(
            connectionPool, "SELECT data FROM performance_test WHERE id = ?");

    int queryCount = 100;

    // 非プール版のパフォーマンス測定
    long nonPooledStart = System.currentTimeMillis();
    for (int i = 1; i <= queryCount; i++) {
      nonPooledRule.apply(String.valueOf(i));
    }
    long nonPooledDuration = System.currentTimeMillis() - nonPooledStart;

    // プール版のパフォーマンス測定
    long pooledStart = System.currentTimeMillis();
    for (int i = 1; i <= queryCount; i++) {
      pooledRule.apply(String.valueOf(i));
    }
    long pooledDuration = System.currentTimeMillis() - pooledStart;

    // プール版の方が高速であることを確認
    assertTrue(
        pooledDuration <= nonPooledDuration,
        String.format(
            "Pooled version (%dms) should be faster than or equal to non-pooled version (%dms)",
            pooledDuration, nonPooledDuration));

    if (pooledDuration == 0) {
      System.out.printf(
          "Performance comparison - Non-pooled: %dms, Pooled: %dms (improvement: infinite or unmeasurable)%n",
          nonPooledDuration, pooledDuration);
    } else {
      System.out.printf(
          "Performance comparison - Non-pooled: %dms, Pooled: %dms (%.1fx improvement)%n",
          nonPooledDuration, pooledDuration, (double) nonPooledDuration / pooledDuration);
    }
  }

  @Test
  @DisplayName("プールシャットダウン後の動作テスト")
  public void testPoolShutdownBehavior() {
    HikariConnectionPoolConfig testPool =
        new HikariConnectionPoolConfig(dbUrl, 3, Duration.ofSeconds(5));

    PooledDatabaseFetchRule rule =
        new PooledDatabaseFetchRule(testPool, "SELECT data FROM performance_test WHERE id = ?");

    // 正常動作確認
    String result = rule.apply("1");
    assertEquals("Data 1", result);

    // プールをシャットダウン
    testPool.close();

    // シャットダウン後はエラーになることを確認
    result = rule.apply("2");
    assertTrue(result.startsWith("ERROR"), "Should return error after pool shutdown");
  }
}
