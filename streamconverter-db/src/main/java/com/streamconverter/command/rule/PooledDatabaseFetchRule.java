package com.streamconverter.command.rule;

import com.streamconverter.StreamProcessingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HikariCP対応のデータベースフェッチルール
 *
 * <p>DatabaseFetchRuleの高性能版です。HikariCPを使用して接続の再利用により パフォーマンスを大幅に向上させます。特に大量のデータ処理や高頻度のデータベースアクセスが
 * 必要な場合に効果的です。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // HikariCP接続プールを作成
 * HikariConnectionPoolConfig pool = new HikariConnectionPoolConfig("jdbc:h2:mem:testdb", 10, Duration.ofSeconds(30));
 *
 * // プール対応ルールを作成
 * PooledDatabaseFetchRule rule = new PooledDatabaseFetchRule(
 *     pool,
 *     "SELECT name FROM users WHERE id = ?"
 * );
 *
 * // 大量処理でも高速
 * for (int i = 0; i < 10000; i++) {
 *     String result = rule.apply(String.valueOf(i));
 * }
 *
 * // 使用後はプールをシャットダウン
 * pool.close();
 * }</pre>
 *
 * <p>DatabaseFetchRuleとの違い:
 *
 * <ul>
 *   <li>業界標準HikariCP使用によりパフォーマンス大幅向上
 *   <li>接続リーク検出と自動回復
 *   <li>複数スレッドからの同時アクセス対応
 *   <li>詳細なプールメトリクス
 * </ul>
 */
public class PooledDatabaseFetchRule implements IRule {
  private static final Logger logger = LoggerFactory.getLogger(PooledDatabaseFetchRule.class);

  private final HikariConnectionPoolConfig connectionPool;
  private final String query;

  /**
   * コンストラクタ
   *
   * @param connectionPool HikariCP接続プール
   * @param query データベースクエリ（SELECTクエリのみ許可）
   * @throws IllegalArgumentException 無効なパラメータが指定された場合
   * @throws SecurityException セキュリティ違反が検出された場合
   */
  public PooledDatabaseFetchRule(HikariConnectionPoolConfig connectionPool, String query) {
    Objects.requireNonNull(connectionPool, "Connection pool cannot be null");
    Objects.requireNonNull(query, "Query cannot be null");

    this.connectionPool = connectionPool;
    String trimmed = query.trim();
    long placeholderCount = trimmed.chars().filter(c -> c == '?').count();
    if (placeholderCount > 1) {
      throw new IllegalArgumentException(
          "Query must have at most one placeholder '?', found " + placeholderCount);
    }
    this.query = validateQuery(trimmed);

    logger.info(
        "PooledDatabaseFetchRule initialized - Query length: {}, Pool: {}",
        this.query.length(),
        connectionPool.getDetailedStats());
  }

  /**
   * クエリを検証します（SQLインジェクション対策）
   *
   * @param queryString 検証対象のクエリ
   * @return 検証済みのクエリ
   * @throws SecurityException SQLインジェクションが検出された場合
   */
  private String validateQuery(String queryString) {
    return SqlQueryUtils.validateQuery(queryString, logger);
  }

  private String sanitizeInput(String input) {
    return SqlQueryUtils.sanitizeInput(input, logger);
  }

  /**
   * ルールの適用を実行します。
   *
   * <p>コネクションプールから接続を取得してクエリを実行し、結果の先頭行・先頭列の値を返します。 接続はプールに自動的に返却されるため、高いパフォーマンスを実現します。
   *
   * @param input 変換対象の文字列（クエリパラメータとして使用）
   * @return クエリ結果の先頭値、または空文字列（結果がない場合）
   * @throws StreamProcessingException SQLExceptionが発生した場合
   * @throws IllegalArgumentException inputがnullの場合（sanitizeInput経由）
   */
  @Override
  public String apply(String input) {
    logger.debug("Getting connection from pool: {}", connectionPool.getPoolStats());
    try (Connection connection = connectionPool.getConnection();
        PreparedStatement statement = connection.prepareStatement(query)) {

      if (!bindParameters(statement, input)) {
        return "";
      }

      logger.debug("Executing query with pooled connection: {}", query);
      try (ResultSet resultSet = statement.executeQuery()) {
        return extractFirstValue(resultSet);
      }

    } catch (SQLException e) {
      logger.error("プール接続でのデータベース操作中にエラーが発生しました: {}", e.getMessage(), e);
      Throwable[] suppressed = e.getSuppressed();
      if (suppressed != null) {
        for (Throwable s : suppressed) {
          logger.error("クローズ中に追加のエラーが発生しました: {}", s.getMessage(), s);
        }
      }
      throw new StreamProcessingException(
          "データベースフェッチに失敗しました: " + e.getMessage(), e);
    }
  }

  /** クエリにプレースホルダーがある場合に入力値をバインドする。拒否すべき入力なら false を返す。 */
  private boolean bindParameters(PreparedStatement statement, String input) throws SQLException {
    if (!query.contains("?")) {
      return true;
    }
    if (input == null || input.isEmpty()) {
      logger.warn("Query has a placeholder but input is null or empty. Rejecting to prevent unbound parameter.");
      return false;
    }
    String sanitizedInput = sanitizeInput(input);
    if (sanitizedInput.isEmpty()) {
      logger.warn(
          "Input parameter was sanitized to empty string. Rejecting input for security reasons. Original input: {}",
          input);
      return false;
    }
    statement.setString(1, sanitizedInput);
    logger.debug("Parameter set for prepared statement: length={}", sanitizedInput.length());
    return true;
  }

  /** ResultSet から先頭行・先頭列の値を取得して返す。結果なしの場合は空文字列を返す。 */
  private String extractFirstValue(ResultSet resultSet) throws SQLException {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int columnCount = metaData.getColumnCount();

    if (!resultSet.next()) {
      logger.warn("クエリ結果が空です。");
      return "";
    }

    if (columnCount != 1) {
      logger.warn("クエリ結果が一列ではありません。列数: {}。先頭列の値を使用します。", columnCount);
    }

    String value = resultSet.getString(1);
    boolean hasMoreRows = resultSet.next();
    if (hasMoreRows) {
      logger.warn("クエリ結果が複数行あります。先頭行の値を使用します。");
    }

    if (value == null) {
      logger.info("クエリ結果の先頭値がNULLです。");
      return "";
    }

    if (columnCount == 1 && !hasMoreRows) {
      logger.debug("データベースから単一値を取得しました（プール使用）: {}", value);
    } else {
      logger.debug("データベースから先頭値を取得しました（プール使用）: {}", value);
    }

    return value;
  }

  /**
   * プールの統計情報を取得
   *
   * @return プールの統計情報
   */
  public String getPoolStats() {
    return connectionPool.getPoolStats();
  }
}
