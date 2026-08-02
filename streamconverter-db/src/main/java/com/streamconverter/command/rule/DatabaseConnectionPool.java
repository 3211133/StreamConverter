package com.streamconverter.command.rule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * データベース接続プールの実装
 *
 * <p>DatabaseFetchRuleで使用するための軽量なコネクションプール実装です。 接続の再利用によりパフォーマンスを向上させ、リソース使用量を最適化します。
 *
 * <p>機能:
 *
 * <ul>
 *   <li>接続プールサイズの設定可能
 *   <li>接続タイムアウトの設定
 *   <li>自動的な接続検証と回復
 *   <li>スレッドセーフな設計
 *   <li>適切なリソース管理
 * </ul>
 */
public class DatabaseConnectionPool {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionPool.class);

  private final String databaseUrl;
  private final int maxPoolSize;
  private final long connectionTimeoutMs;
  private final BlockingQueue<Connection> connectionPool;
  private final AtomicInteger activeConnections = new AtomicInteger(0);
  private final AtomicBoolean isShutdown = new AtomicBoolean(false);

  /**
   * デフォルト設定でコネクションプールを初期化
   *
   * @param databaseUrl データベースURL
   */
  public DatabaseConnectionPool(String databaseUrl) {
    this(databaseUrl, 5, 30_000); // デフォルト: 最大5接続、30秒タイムアウト
  }

  /**
   * 設定可能なコネクションプールを初期化
   *
   * @param databaseUrl データベースURL
   * @param maxPoolSize 最大プールサイズ
   * @param connectionTimeoutMs 接続タイムアウト（ミリ秒）
   */
  public DatabaseConnectionPool(String databaseUrl, int maxPoolSize, long connectionTimeoutMs) {
    this.databaseUrl = databaseUrl;
    this.maxPoolSize = maxPoolSize;
    this.connectionTimeoutMs = connectionTimeoutMs;
    this.connectionPool = new ArrayBlockingQueue<>(maxPoolSize);

    logger.info(
        "DatabaseConnectionPool initialized - URL: {}, MaxPoolSize: {}, TimeoutMs: {}",
        databaseUrl,
        maxPoolSize,
        connectionTimeoutMs);
  }

  /**
   * プールから接続を取得
   *
   * @return データベース接続
   * @throws SQLException 接続取得に失敗した場合
   */
  public Connection getConnection() throws SQLException {
    if (isShutdown.get()) {
      throw new SQLException("Connection pool is shutdown");
    }

    Connection connection = tryReuseFromPool();
    if (connection == null) {
      connection = tryCreateNewConnection();
    }
    if (connection == null) {
      connection = waitForConnection();
    }
    if (connection != null) {
      return new PooledConnection(connection, this);
    }

    throw new SQLException("Failed to get connection within timeout");
  }

  /**
   * プールから既存の接続の再利用を試みる。無効な接続はクローズして破棄する。
   *
   * @return 再利用可能な接続、取得できなければ null
   */
  private Connection tryReuseFromPool() {
    Connection connection = connectionPool.poll();
    if (connection == null) {
      return null;
    }
    if (isConnectionValid(connection)) {
      logger.debug("Reused connection from pool");
      return connection;
    }

    try {
      connection.close();
    } catch (SQLException e) {
      logger.warn("Failed to close invalid connection: {}", e.getMessage());
    }
    activeConnections.decrementAndGet();
    return null;
  }

  /**
   * プールサイズ制限内であれば新しい接続を作成する。
   *
   * @return 新規作成した接続、プールサイズ制限に達していれば null
   * @throws SQLException 接続の作成に失敗した場合
   */
  private Connection tryCreateNewConnection() throws SQLException {
    if (activeConnections.get() >= maxPoolSize) {
      return null;
    }

    try {
      Connection connection = DriverManager.getConnection(databaseUrl);
      activeConnections.incrementAndGet();
      logger.debug("Created new connection - Active connections: {}", activeConnections.get());
      return connection;
    } catch (SQLException e) {
      logger.error("Failed to create new connection: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * プールが満杯の場合、接続が返却されるかタイムアウトするまで待機する。
   *
   * @return 待機の結果得られた接続、タイムアウトすれば null
   * @throws SQLException 待機中に割り込まれた場合
   */
  private Connection waitForConnection() throws SQLException {
    try {
      Connection connection = connectionPool.poll(connectionTimeoutMs, TimeUnit.MILLISECONDS);
      if (connection == null) {
        return null;
      }
      if (isConnectionValid(connection)) {
        logger.debug("Got connection after waiting");
        return connection;
      }

      // 無効な接続をクローズして再試行
      try {
        connection.close();
      } catch (SQLException e) {
        logger.warn("Failed to close invalid waited connection: {}", e.getMessage());
      }
      activeConnections.decrementAndGet();
      return null;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLException("Interrupted while waiting for connection", e);
    }
  }

  /**
   * 接続をプールに返却
   *
   * @param connection 返却する接続
   */
  void returnConnection(Connection connection) {
    if (isShutdown.get() || !isConnectionValid(connection)) {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.warn("Failed to close returned connection: {}", e.getMessage());
      } finally {
        activeConnections.decrementAndGet();
      }
      return;
    }

    if (connectionPool.offer(connection)) {
      logger.debug("Returned connection to pool");
    } else {
      // プールが満杯の場合は接続をクローズ
      try {
        connection.close();
        logger.debug("Closed excess connection - Active connections: {}", activeConnections.get());
      } catch (SQLException e) {
        logger.warn("Failed to close excess connection: {}", e.getMessage());
      } finally {
        activeConnections.decrementAndGet();
      }
    }
  }

  /**
   * 接続の有効性を検証
   *
   * @param connection 検証する接続
   * @return 有効な場合true
   */
  private boolean isConnectionValid(Connection connection) {
    try {
      return connection != null && !connection.isClosed() && connection.isValid(1);
    } catch (SQLException e) {
      return false;
    }
  }

  /**
   * プールの現在の統計情報を取得
   *
   * @return 統計情報文字列
   */
  public String getPoolStats() {
    return String.format(
        "ConnectionPool[active=%d, pooled=%d, max=%d]",
        activeConnections.get(), connectionPool.size(), maxPoolSize);
  }

  /** プールをシャットダウンし、全ての接続をクローズ */
  public void shutdown() {
    isShutdown.set(true);
    logger.info("Shutting down connection pool");

    // プール内の全接続をクローズ
    Connection connection;
    while ((connection = connectionPool.poll()) != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.warn("Failed to close connection during shutdown: {}", e.getMessage());
      }
    }

    logger.info("Connection pool shutdown completed - Final stats: {}", getPoolStats());
  }
}
