package com.streamConverter.mock;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * H2インメモリデータベースを使用したモックデータベースサービス
 *
 * <p>データベース依存テストを環境非依存にするためのモック実装。 H2インメモリデータベースを使用して、実際のデータベース操作をシミュレート。
 */
public class MockDatabaseService {

  private static final Logger logger = LoggerFactory.getLogger(MockDatabaseService.class);
  private final HikariDataSource dataSource;
  private final AtomicBoolean isClosed = new AtomicBoolean(false);
  private final String databaseName;

  /** デフォルトコンストラクタ */
  public MockDatabaseService() {
    this("testdb");
  }

  /**
   * 指定データベース名でモックデータベースを作成
   *
   * @param databaseName データベース名
   */
  public MockDatabaseService(String databaseName) {
    this.databaseName = databaseName;
    this.dataSource = createDataSource();
    logger.info("MockDatabaseService initialized with database: {}", databaseName);
  }

  /**
   * データソースを取得
   *
   * @return データソース
   */
  public DataSource getDataSource() {
    if (isClosed.get()) {
      throw new IllegalStateException("Database service is closed");
    }
    return dataSource;
  }

  /**
   * データベース接続を取得
   *
   * @return データベース接続
   * @throws SQLException SQL例外
   */
  public Connection getConnection() throws SQLException {
    if (isClosed.get()) {
      throw new SQLException("Database service is closed");
    }
    return dataSource.getConnection();
  }

  /**
   * テストテーブルを作成
   *
   * @throws SQLException SQL例外
   */
  public void createTestTables() throws SQLException {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement()) {

      // ユーザーテーブル
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS users (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            username VARCHAR(255) NOT NULL UNIQUE,
            email VARCHAR(255) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // 製品テーブル
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS products (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            name VARCHAR(255) NOT NULL,
            price DECIMAL(10,2) NOT NULL,
            category VARCHAR(100),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
          )
          """);

      // 注文テーブル
      stmt.execute(
          """
          CREATE TABLE IF NOT EXISTS orders (
            id BIGINT PRIMARY KEY AUTO_INCREMENT,
            user_id BIGINT NOT NULL,
            product_id BIGINT NOT NULL,
            quantity INTEGER NOT NULL,
            total_price DECIMAL(10,2) NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id),
            FOREIGN KEY (product_id) REFERENCES products(id)
          )
          """);

      logger.info("Test tables created successfully");
    }
  }

  /**
   * テストデータを挿入
   *
   * @throws SQLException SQL例外
   */
  public void insertTestData() throws SQLException {
    try (Connection conn = getConnection()) {

      // ユーザーデータ
      try (PreparedStatement stmt =
          conn.prepareStatement("INSERT INTO users (username, email) VALUES (?, ?)")) {

        String[][] users = {
          {"alice", "alice@example.com"},
          {"bob", "bob@example.com"},
          {"charlie", "charlie@example.com"}
        };

        for (String[] user : users) {
          stmt.setString(1, user[0]);
          stmt.setString(2, user[1]);
          stmt.addBatch();
        }
        stmt.executeBatch();
      }

      // 製品データ
      try (PreparedStatement stmt =
          conn.prepareStatement("INSERT INTO products (name, price, category) VALUES (?, ?, ?)")) {

        Object[][] products = {
          {"Laptop", 999.99, "Electronics"},
          {"Mouse", 29.99, "Electronics"},
          {"Book", 19.99, "Books"},
          {"Coffee", 4.99, "Food"}
        };

        for (Object[] product : products) {
          stmt.setString(1, (String) product[0]);
          stmt.setBigDecimal(2, new java.math.BigDecimal(product[1].toString()));
          stmt.setString(3, (String) product[2]);
          stmt.addBatch();
        }
        stmt.executeBatch();
      }

      // 注文データ
      try (PreparedStatement stmt =
          conn.prepareStatement(
              "INSERT INTO orders (user_id, product_id, quantity, total_price) VALUES (?, ?, ?, ?)")) {

        Object[][] orders = {
          {1L, 1L, 1, 999.99},
          {1L, 2L, 2, 59.98},
          {2L, 3L, 1, 19.99},
          {3L, 4L, 3, 14.97}
        };

        for (Object[] order : orders) {
          stmt.setLong(1, (Long) order[0]);
          stmt.setLong(2, (Long) order[1]);
          stmt.setInt(3, (Integer) order[2]);
          stmt.setBigDecimal(4, new java.math.BigDecimal(order[3].toString()));
          stmt.addBatch();
        }
        stmt.executeBatch();
      }

      logger.info("Test data inserted successfully");
    }
  }

  /**
   * 全データを削除
   *
   * @throws SQLException SQL例外
   */
  public void clearAllData() throws SQLException {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement()) {

      // 外部キー制約のため順序に注意
      stmt.execute("DELETE FROM orders");
      stmt.execute("DELETE FROM products");
      stmt.execute("DELETE FROM users");

      logger.info("All test data cleared");
    }
  }

  /**
   * テーブルを削除
   *
   * @throws SQLException SQL例例
   */
  public void dropTestTables() throws SQLException {
    try (Connection conn = getConnection();
        Statement stmt = conn.createStatement()) {

      // 外部キー制約のため順序に注意
      stmt.execute("DROP TABLE IF EXISTS orders");
      stmt.execute("DROP TABLE IF EXISTS products");
      stmt.execute("DROP TABLE IF EXISTS users");

      logger.info("Test tables dropped");
    }
  }

  /**
   * クエリ実行（SELECT）
   *
   * @param sql SQLクエリ
   * @param params パラメータ
   * @return 結果リスト
   * @throws SQLException SQL例外
   */
  public List<Map<String, Object>> executeQuery(String sql, Object... params) throws SQLException {
    List<Map<String, Object>> results = new ArrayList<>();

    try (Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      // パラメータ設定
      for (int i = 0; i < params.length; i++) {
        stmt.setObject(i + 1, params[i]);
      }

      try (ResultSet rs = stmt.executeQuery()) {
        int columnCount = rs.getMetaData().getColumnCount();

        while (rs.next()) {
          Map<String, Object> row = new HashMap<>();
          for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnName(i);
            Object value = rs.getObject(i);
            row.put(columnName, value);
          }
          results.add(row);
        }
      }
    }

    return results;
  }

  /**
   * 更新実行（INSERT/UPDATE/DELETE）
   *
   * @param sql SQLクエリ
   * @param params パラメータ
   * @return 影響行数
   * @throws SQLException SQL例外
   */
  public int executeUpdate(String sql, Object... params) throws SQLException {
    try (Connection conn = getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      // パラメータ設定
      for (int i = 0; i < params.length; i++) {
        stmt.setObject(i + 1, params[i]);
      }

      return stmt.executeUpdate();
    }
  }

  /**
   * データベースの状態を確認
   *
   * @return データベース情報
   */
  public DatabaseInfo getDatabaseInfo() {
    try (Connection conn = getConnection()) {
      DatabaseInfo info = new DatabaseInfo();
      info.databaseName = databaseName;
      info.isConnected = !conn.isClosed();
      info.url = conn.getMetaData().getURL();
      info.activeConnections = dataSource.getHikariPoolMXBean().getActiveConnections();
      info.totalConnections = dataSource.getHikariPoolMXBean().getTotalConnections();

      return info;
    } catch (SQLException e) {
      logger.error("Failed to get database info", e);
      DatabaseInfo info = new DatabaseInfo();
      info.databaseName = databaseName;
      info.isConnected = false;
      info.error = e.getMessage();
      return info;
    }
  }

  /** モックデータベースサービスを停止 */
  public void close() {
    if (isClosed.compareAndSet(false, true)) {
      try {
        // テーブル削除を試行（失敗しても継続）
        dropTestTables();
      } catch (Exception e) {
        logger.warn("Failed to drop test tables during close", e);
      }

      try {
        // データソースを確実に閉じる
        if (dataSource != null && !dataSource.isClosed()) {
          dataSource.close();
        }
      } catch (Exception e) {
        logger.warn("Failed to close data source", e);
      }

      logger.info("MockDatabaseService closed");
    }
  }

  /**
   * HikariCPデータソースを作成
   *
   * @return データソース
   */
  private HikariDataSource createDataSource() {
    HikariConfig config = new HikariConfig();

    // H2インメモリデータベース設定
    config.setJdbcUrl("jdbc:h2:mem:" + databaseName + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
    config.setDriverClassName("org.h2.Driver");
    config.setUsername("sa");
    config.setPassword("");

    // 接続プール設定
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(30000);
    config.setIdleTimeout(600000);
    config.setMaxLifetime(1800000);

    // テスト用最適化
    config.setAutoCommit(true);
    config.setConnectionTestQuery("SELECT 1");
    config.setPoolName("MockDatabasePool-" + databaseName);

    return new HikariDataSource(config);
  }

  /** データベース情報DTO */
  public static class DatabaseInfo {
    public String databaseName;
    public boolean isConnected;
    public String url;
    public int activeConnections;
    public int totalConnections;
    public String error;

    @Override
    public String toString() {
      if (error != null) {
        return String.format("DatabaseInfo{name='%s', error='%s'}", databaseName, error);
      }
      return String.format(
          "DatabaseInfo{name='%s', connected=%s, url='%s', active=%d, total=%d}",
          databaseName, isConnected, url, activeConnections, totalConnections);
    }
  }
}
