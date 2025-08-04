package com.streamConverter.command.rule;

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
    this(databaseUrl, 5, 30000); // デフォルト: 最大5接続、30秒タイムアウト
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

    // プールから既存の接続を試行
    Connection connection = connectionPool.poll();
    if (connection != null && isConnectionValid(connection)) {
      logger.debug("Reused connection from pool");
      return new PooledConnection(connection, this);
    }

    // 既存接続が無効な場合はクローズ
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        logger.warn("Failed to close invalid connection: {}", e.getMessage());
      }
    }

    // 新しい接続を作成（プールサイズ制限内）
    if (activeConnections.get() < maxPoolSize) {
      try {
        connection = DriverManager.getConnection(databaseUrl);
        activeConnections.incrementAndGet();
        logger.debug("Created new connection - Active connections: {}", activeConnections.get());
        return new PooledConnection(connection, this);
      } catch (SQLException e) {
        logger.error("Failed to create new connection: {}", e.getMessage());
        throw e;
      }
    }

    // プールが満杯の場合は待機
    try {
      connection = connectionPool.poll(connectionTimeoutMs, TimeUnit.MILLISECONDS);
      if (connection != null) {
        if (isConnectionValid(connection)) {
          logger.debug("Got connection after waiting");
          return new PooledConnection(connection, this);
        } else {
          // 無効な接続をクローズして再試行
          try {
            connection.close();
          } catch (SQLException e) {
            logger.warn("Failed to close invalid waited connection: {}", e.getMessage());
          }
          activeConnections.decrementAndGet();
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new SQLException("Interrupted while waiting for connection", e);
    }

    throw new SQLException("Failed to get connection within timeout");
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
        activeConnections.decrementAndGet();
      } catch (SQLException e) {
        logger.warn("Failed to close returned connection: {}", e.getMessage());
      }
      return;
    }

    if (!connectionPool.offer(connection)) {
      // プールが満杯の場合は接続をクローズ
      try {
        connection.close();
        activeConnections.decrementAndGet();
        logger.debug("Closed excess connection - Active connections: {}", activeConnections.get());
      } catch (SQLException e) {
        logger.warn("Failed to close excess connection: {}", e.getMessage());
      }
    } else {
      logger.debug("Returned connection to pool");
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

  /** プール管理されたConnection実装 */
  private static class PooledConnection implements Connection {
    private final Connection delegate;
    private final DatabaseConnectionPool pool;
    private boolean closed = false;

    PooledConnection(Connection delegate, DatabaseConnectionPool pool) {
      this.delegate = delegate;
      this.pool = pool;
    }

    @Override
    public void close() throws SQLException {
      if (!closed) {
        closed = true;
        pool.returnConnection(delegate);
      }
    }

    // Connection インターface のすべてのメソッドを delegate に委譲
    @Override
    public java.sql.Statement createStatement() throws SQLException {
      return delegate.createStatement();
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql) throws SQLException {
      return delegate.prepareStatement(sql);
    }

    @Override
    public java.sql.CallableStatement prepareCall(String sql) throws SQLException {
      return delegate.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
      return delegate.nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
      delegate.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      return delegate.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
      delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
      delegate.rollback();
    }

    @Override
    public boolean isClosed() throws SQLException {
      return closed || delegate.isClosed();
    }

    @Override
    public java.sql.DatabaseMetaData getMetaData() throws SQLException {
      return delegate.getMetaData();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
      delegate.setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
      return delegate.isReadOnly();
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
      delegate.setCatalog(catalog);
    }

    @Override
    public String getCatalog() throws SQLException {
      return delegate.getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
      delegate.setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
      return delegate.getTransactionIsolation();
    }

    @Override
    public java.sql.SQLWarning getWarnings() throws SQLException {
      return delegate.getWarnings();
    }

    @Override
    public void clearWarnings() throws SQLException {
      delegate.clearWarnings();
    }

    @Override
    public java.sql.Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException {
      return delegate.createStatement(resultSetType, resultSetConcurrency);
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(
        String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      return delegate.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public java.sql.CallableStatement prepareCall(
        String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      return delegate.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public java.util.Map<String, Class<?>> getTypeMap() throws SQLException {
      return delegate.getTypeMap();
    }

    @Override
    public void setTypeMap(java.util.Map<String, Class<?>> map) throws SQLException {
      delegate.setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
      delegate.setHoldability(holdability);
    }

    @Override
    public int getHoldability() throws SQLException {
      return delegate.getHoldability();
    }

    @Override
    public java.sql.Savepoint setSavepoint() throws SQLException {
      return delegate.setSavepoint();
    }

    @Override
    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
      return delegate.setSavepoint(name);
    }

    @Override
    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
      delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
      delegate.releaseSavepoint(savepoint);
    }

    @Override
    public java.sql.Statement createStatement(
        int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
      return delegate.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(
        String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
      return delegate.prepareStatement(
          sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public java.sql.CallableStatement prepareCall(
        String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException {
      return delegate.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
      return delegate.prepareStatement(sql, autoGeneratedKeys);
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException {
      return delegate.prepareStatement(sql, columnIndexes);
    }

    @Override
    public java.sql.PreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException {
      return delegate.prepareStatement(sql, columnNames);
    }

    @Override
    public java.sql.Clob createClob() throws SQLException {
      return delegate.createClob();
    }

    @Override
    public java.sql.Blob createBlob() throws SQLException {
      return delegate.createBlob();
    }

    @Override
    public java.sql.NClob createNClob() throws SQLException {
      return delegate.createNClob();
    }

    @Override
    public java.sql.SQLXML createSQLXML() throws SQLException {
      return delegate.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
      return delegate.isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value) throws java.sql.SQLClientInfoException {
      delegate.setClientInfo(name, value);
    }

    @Override
    public void setClientInfo(java.util.Properties properties)
        throws java.sql.SQLClientInfoException {
      delegate.setClientInfo(properties);
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
      return delegate.getClientInfo(name);
    }

    @Override
    public java.util.Properties getClientInfo() throws SQLException {
      return delegate.getClientInfo();
    }

    @Override
    public java.sql.Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      return delegate.createArrayOf(typeName, elements);
    }

    @Override
    public java.sql.Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      return delegate.createStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) throws SQLException {
      delegate.setSchema(schema);
    }

    @Override
    public String getSchema() throws SQLException {
      return delegate.getSchema();
    }

    @Override
    public void abort(java.util.concurrent.Executor executor) throws SQLException {
      delegate.abort(executor);
    }

    @Override
    public void setNetworkTimeout(java.util.concurrent.Executor executor, int milliseconds)
        throws SQLException {
      delegate.setNetworkTimeout(executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
      return delegate.getNetworkTimeout();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return delegate.isWrapperFor(iface);
    }
  }
}
