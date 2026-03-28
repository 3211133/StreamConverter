package com.streamconverter.command.rule;

import com.streamconverter.StreamProcessingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;
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

  /** SQLインジェクション攻撃を検出するパターン（SELECT以外の危険なSQL文） */
  private static final Pattern SQL_INJECTION_PATTERN =
      Pattern.compile(
          "(?i).*(union|insert|update|delete|drop|create|alter|exec|execute|sp_|xp_).*",
          Pattern.CASE_INSENSITIVE);

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
    this.query = validateQuery(query.trim());

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
    if (queryString.isEmpty()) {
      throw new IllegalArgumentException("Query cannot be empty");
    }

    // SELECTクエリのみ許可
    if (!queryString.trim().toLowerCase().startsWith("select")) {
      throw new SecurityException("Only SELECT queries are allowed: " + queryString);
    }

    // SQLインジェクション攻撃の検出
    if (SQL_INJECTION_PATTERN.matcher(queryString).matches()) {
      throw new SecurityException(
          "Query contains potentially dangerous SQL commands: " + queryString);
    }

    // セミコロンによる複数文の実行を防止
    if (queryString.contains(";") && !queryString.trim().endsWith(";")) {
      throw new SecurityException("Multiple SQL statements are not allowed: " + queryString);
    }

    logger.debug("Query validation passed, length: {}", queryString.length());
    return queryString;
  }

  /**
   * 入力パラメータをサニタイズします
   *
   * @param input サニタイズ対象の入力
   * @return サニタイズされた入力
   */
  private String sanitizeInput(String input) {
    if (input == null) {
      return null;
    }

    // 危険な文字の除去/エスケープ
    String sanitized =
        input
            .replace("'", "''") // シングルクォートのエスケープ
            .replace("--", "") // SQLコメントの除去
            .replace("/*", "") // ブロックコメント開始の除去
            .replace("*/", ""); // ブロックコメント終了の除去

    // 極端に長い入力の制限
    if (sanitized.length() > 1000) {
      logger.warn("Input parameter is extremely long, truncating: length={}", sanitized.length());
      sanitized = sanitized.substring(0, 1000);
    }

    return sanitized;
  }

  /**
   * ルールの適用を実行します。
   *
   * <p>コネクションプールから接続を取得してクエリを実行し、結果の先頭行・先頭列の値を返します。 接続はプールに自動的に返却されるため、高いパフォーマンスを実現します。
   *
   * @param input 変換対象の文字列（クエリパラメータとして使用）
   * @return クエリ結果の先頭値、または空文字列（結果がない場合）
   */
  @Override
  public String apply(String input) {
    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // プールから接続を取得
      logger.debug("Getting connection from pool: {}", connectionPool.getPoolStats());
      connection = connectionPool.getConnection();

      // クエリの準備
      statement = connection.prepareStatement(query);

      // 入力文字列をパラメータとして設定（クエリに「?」プレースホルダーがある場合）
      if (query.contains("?") && input != null && !input.isEmpty()) {
        String sanitizedInput = sanitizeInput(input);
        if (sanitizedInput == null || sanitizedInput.isEmpty()) {
          logger.warn(
              "Input parameter was sanitized to empty string. Rejecting input for security reasons. Original input: {}",
              input);
          return "";
        }

        statement.setString(1, sanitizedInput);
        logger.debug("Parameter set for prepared statement: length={}", sanitizedInput.length());
      }

      // クエリ実行
      logger.debug("Executing query with pooled connection: {}", query);
      resultSet = statement.executeQuery();

      // 結果の検証と処理
      ResultSetMetaData metaData = resultSet.getMetaData();
      int columnCount = metaData.getColumnCount();

      // 結果がない場合
      if (!resultSet.next()) {
        logger.warn("クエリ結果が空です。");
        return "";
      }

      // 列数の検証
      if (columnCount != 1) {
        logger.warn("クエリ結果が一列ではありません。列数: {}。先頭列の値を使用します。", columnCount);
      }

      // 先頭行の先頭列の値を取得
      String value = resultSet.getString(1);

      // 追加の行があるかチェック
      boolean hasMoreRows = resultSet.next();
      if (hasMoreRows) {
        logger.warn("クエリ結果が複数行あります。先頭行の値を使用します。");
      }

      // nullチェック
      if (value == null) {
        logger.info("クエリ結果の先頭値がNULLです。");
        return "";
      }

      // 結果が理想的（1行1列）かどうかをログに記録
      if (columnCount == 1 && !hasMoreRows) {
        logger.debug("データベースから単一値を取得しました（プール使用）: {}", value);
      } else {
        logger.debug("データベースから先頭値を取得しました（プール使用）: {}", value);
      }

      return value;

    } catch (SQLException e) {
      logger.error("プール接続でのデータベース操作中にエラーが発生しました: {}", e.getMessage(), e);
      throw new StreamProcessingException(
          "データベースフェッチに失敗しました: " + e.getMessage(), e);
    } finally {
      // リソースのクローズ（接続は自動的にプールに返却される）
      closeResources(resultSet, statement, connection);
    }
  }

  /**
   * データベースリソースを安全にクローズします。 接続はプールに自動的に返却されます。
   *
   * @param resultSet 結果セット
   * @param statement プリペアドステートメント
   * @param connection データベース接続（プールに返却される）
   */
  private void closeResources(
      ResultSet resultSet, PreparedStatement statement, Connection connection) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        logger.warn("ResultSetのクローズ中にエラーが発生しました: {}", e.getMessage());
      }
    }

    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        logger.warn("PreparedStatementのクローズ中にエラーが発生しました: {}", e.getMessage());
      }
    }

    if (connection != null) {
      try {
        connection.close(); // プールに返却される
      } catch (SQLException e) {
        logger.warn("Connection（プール返却）中にエラーが発生しました: {}", e.getMessage());
      }
    }
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
