package com.streamConverter.command.rule;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HikariCP接続プール設定クラス
 *
 * <p>HikariCPを使用したデータベース接続プールの実装です。 DatabaseConnectionPoolの置き換えとして高性能で信頼性の高い接続プール管理を提供します。
 *
 * <p>機能:
 *
 * <ul>
 *   <li>業界標準のHikariCP接続プール
 *   <li>自動的な接続リーク検出
 *   <li>接続プールメトリクス
 *   <li>設定可能なプールサイズと接続タイムアウト
 *   <li>接続の検証と回復
 * </ul>
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // HikariCP接続プールの作成
 * HikariConnectionPoolConfig pool = new HikariConnectionPoolConfig("jdbc:h2:mem:testdb");
 *
 * // 接続の取得
 * try (Connection connection = pool.getConnection()) {
 *     // データベース操作
 * }
 *
 * // プールのシャットダウン
 * pool.close();
 * }</pre>
 */
public class HikariConnectionPoolConfig implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(HikariConnectionPoolConfig.class);

  private final HikariDataSource dataSource;
  private final String databaseUrl;

  /**
   * デフォルト設定で接続プールを初期化
   *
   * @param databaseUrl データベースURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  public HikariConnectionPoolConfig(String databaseUrl) {
    this(databaseUrl, 10, Duration.ofSeconds(30));
  }

  /**
   * カスタム設定で接続プールを初期化
   *
   * @param databaseUrl データベースURL
   * @param maximumPoolSize 最大プールサイズ
   * @param connectionTimeout 接続タイムアウト
   * @throws IllegalArgumentException パラメータが無効な場合
   */
  public HikariConnectionPoolConfig(
      String databaseUrl, int maximumPoolSize, Duration connectionTimeout) {
    Objects.requireNonNull(databaseUrl, "Database URL cannot be null");
    if (maximumPoolSize <= 0) {
      throw new IllegalArgumentException("Maximum pool size must be positive");
    }
    Objects.requireNonNull(connectionTimeout, "Connection timeout cannot be null");

    this.databaseUrl = databaseUrl;

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(databaseUrl);
    config.setMaximumPoolSize(maximumPoolSize);
    config.setMinimumIdle(Math.min(5, maximumPoolSize / 2)); // 最小接続数は最大値の半分または5
    config.setConnectionTimeout(connectionTimeout.toMillis());
    config.setIdleTimeout(Duration.ofMinutes(10).toMillis()); // アイドル接続のタイムアウト
    config.setMaxLifetime(Duration.ofMinutes(30).toMillis()); // 接続の最大生存時間
    config.setLeakDetectionThreshold(Duration.ofMinutes(2).toMillis()); // 接続リーク検出

    // 接続プールの名前を設定（デバッグ用）
    config.setPoolName("StreamConverter-Pool");

    // 接続検証クエリ（H2データベース用）
    if (databaseUrl.contains(":h2:")) {
      config.setConnectionTestQuery("SELECT 1");
    }

    // プロパティの設定
    config.addDataSourceProperty("cachePrepStmts", "true");
    config.addDataSourceProperty("prepStmtCacheSize", "250");
    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    this.dataSource = new HikariDataSource(config);

    logger.info(
        "HikariCP connection pool initialized - URL: {}, MaxPoolSize: {}, ConnectionTimeout: {}ms",
        databaseUrl,
        maximumPoolSize,
        connectionTimeout.toMillis());
  }

  /**
   * プールから接続を取得
   *
   * @return データベース接続
   * @throws SQLException 接続取得に失敗した場合
   */
  public Connection getConnection() throws SQLException {
    try {
      Connection connection = dataSource.getConnection();
      logger.debug("Connection obtained from HikariCP pool");
      return connection;
    } catch (SQLException e) {
      logger.error("Failed to get connection from HikariCP pool: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * プールの現在の統計情報を取得
   *
   * @return 統計情報文字列
   */
  public String getPoolStats() {
    if (dataSource.isClosed()) {
      return "HikariCP[CLOSED]";
    }

    return String.format(
        "HikariCP[active=%d, idle=%d, total=%d, waiting=%d]",
        dataSource.getHikariPoolMXBean().getActiveConnections(),
        dataSource.getHikariPoolMXBean().getIdleConnections(),
        dataSource.getHikariPoolMXBean().getTotalConnections(),
        dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
  }

  /**
   * プールの詳細統計情報を取得
   *
   * @return 詳細統計情報文字列
   */
  public String getDetailedStats() {
    if (dataSource.isClosed()) {
      return "HikariCP[CLOSED]";
    }

    var mxBean = dataSource.getHikariPoolMXBean();
    return String.format(
        "HikariCP Details[active=%d, idle=%d, total=%d, waiting=%d, url=%s]",
        mxBean.getActiveConnections(),
        mxBean.getIdleConnections(),
        mxBean.getTotalConnections(),
        mxBean.getThreadsAwaitingConnection(),
        databaseUrl);
  }

  /**
   * プールがクローズ済みかどうかを確認
   *
   * @return クローズ済みの場合true
   */
  public boolean isClosed() {
    return dataSource.isClosed();
  }

  /** プールをシャットダウンし、全ての接続をクローズ */
  @Override
  public void close() {
    if (!dataSource.isClosed()) {
      logger.info("Shutting down HikariCP connection pool");
      dataSource.close();
      logger.info("HikariCP connection pool shutdown completed");
    }
  }

  /**
   * DatabaseConnectionPoolとの互換性のためのメソッド
   *
   * @deprecated {@link #close()} を使用してください
   */
  @Deprecated
  public void shutdown() {
    close();
  }
}
