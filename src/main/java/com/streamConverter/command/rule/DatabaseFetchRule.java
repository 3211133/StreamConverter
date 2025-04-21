package com.streamConverter.command.rule;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * データベースからデータを取得するルール
 *
 * <p>このクラスは、データベースからデータを取得するためのルールを定義します。 具体的なデータベース接続やクエリ実行のロジックは、このクラスで実装されます。
 */
class DatabaseFetchRule implements IRule {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseFetchRule.class);
  private String databaseUrl;
  private String query;

  /**
   * コンストラクタ
   *
   * <p>データベースのURLとクエリを指定して、DatabaseFetchRuleのインスタンスを初期化します。
   * クエリは一意な結果を返すように設計されるべきです（例：DISTINCT、LIMIT句の使用など）。
   *
   * @param databaseUrl データベースのURL
   * @param query データベースに対するクエリ
   */
  public DatabaseFetchRule(String databaseUrl, String query) {
    this.databaseUrl = databaseUrl;
    this.query = query;
  }

  /**
   * ルールの適用を実行します。
   *
   * <p>このメソッドは、ストリーム変換の際にルールを適用するために使用されます。 データベースからデータを取得するロジックを実装します。 結果セットの先頭行・先頭列の値を返却します。
   * 結果が1行1列でない場合は警告をログに出力します。
   *
   * @param input 変換対象の文字列（クエリパラメータとして使用）
   * @return String output クエリ結果の先頭値、または空文字列（結果がない場合）
   */
  @Override
  public String apply(String input) {
    Connection connection = null;
    PreparedStatement statement = null;
    ResultSet resultSet = null;

    try {
      // データベース接続
      logger.debug("データベースに接続: {}", databaseUrl);
      connection = DriverManager.getConnection(databaseUrl);

      // クエリの準備
      statement = connection.prepareStatement(query);

      // 入力文字列をパラメータとして設定（クエリに「?」プレースホルダーがある場合）
      if (query.contains("?") && input != null && !input.isEmpty()) {
        statement.setString(1, input);
      }

      // クエリ実行
      logger.debug("クエリを実行: {}", query);
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
        return ""; // NULLの場合は空文字列を返す
      }

      // 結果が理想的（1行1列）かどうかをログに記録
      if (columnCount == 1 && !hasMoreRows) {
        logger.info("データベースから単一値を取得しました: {}", value);
      } else {
        logger.info("データベースから先頭値を取得しました: {}", value);
      }

      return value;

    } catch (SQLException e) {
      logger.error("データベース操作中にエラーが発生しました: {}", e.getMessage(), e);
      return "ERROR: " + e.getMessage();
    } finally {
      // リソースのクローズ
      closeResources(resultSet, statement, connection);
    }
  }

  /**
   * データベースリソースを安全にクローズします。
   *
   * @param resultSet 結果セット
   * @param statement プリペアドステートメント
   * @param connection データベース接続
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
        connection.close();
      } catch (SQLException e) {
        logger.warn("Connectionのクローズ中にエラーが発生しました: {}", e.getMessage());
      }
    }
  }
}
