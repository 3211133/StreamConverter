package com.streamconverter.command.rule;

import com.streamconverter.StreamProcessingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * データベースからデータを取得するルール
 *
 * <p>このクラスは、データベースからデータを取得するためのルールを定義します。 具体的なデータベース接続やクエリ実行のロジックは、このクラスで実装されます。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // 基本的な使用例
 * DatabaseFetchRule rule = new DatabaseFetchRule(
 *     "jdbc:h2:mem:testdb",
 *     "SELECT name FROM users WHERE id = ?"
 * );
 * String result = rule.apply("123"); // ユーザーID 123 の名前を取得
 *
 * // JsonWalkerと組み合わせた使用例
 * JsonWalker command = JsonWalker.create(TreePath.fromJson("$.userId"), rule);
 * command.execute(inputStream, outputStream); // JSON中のuserIdでDBを検索して置換
 * }</pre>
 *
 * <p>セキュリティ機能:
 *
 * <ul>
 *   <li>SELECTクエリのみ許可（INSERT/UPDATE/DELETE等は禁止）
 *   <li>許可されたデータベーススキーマのみ接続可能
 * </ul>
 */
public class DatabaseFetchRule implements IRule {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseFetchRule.class);

  /** 許可されるデータベースURLスキーマ（テスト用のmockも含む） */
  private static final Pattern ALLOWED_DB_SCHEME_PATTERN =
      Pattern.compile(
          "^jdbc:(h2|hsqldb|sqlite|postgresql|mysql|mock):.*", Pattern.CASE_INSENSITIVE);

  private final String databaseUrl;
  private final String query;

  /**
   * コンストラクタ
   *
   * <p>データベースのURLとクエリを指定して、DatabaseFetchRuleのインスタンスを初期化します。
   * クエリは一意な結果を返すように設計されるべきです（例：DISTINCT、LIMIT句の使用など）。 セキュリティのため、URLとクエリの検証を実行します。
   *
   * @param databaseUrl データベースのURL（許可されたスキーマのみ）
   * @param query データベースに対するクエリ（SELECTクエリのみ許可）
   * @throws IllegalArgumentException 無効なパラメータが指定された場合
   * @throws SecurityException セキュリティ違反が検出された場合
   */
  public DatabaseFetchRule(String databaseUrl, String query) {
    Objects.requireNonNull(databaseUrl, "Database URL cannot be null");
    Objects.requireNonNull(query, "Query cannot be null");

    // データベースURLの検証
    this.databaseUrl = validateDatabaseUrl(databaseUrl.trim());

    // クエリの検証
    String trimmed = query.trim();
    long placeholderCount = trimmed.chars().filter(c -> c == '?').count();
    if (placeholderCount > 1) {
      throw new IllegalArgumentException(
          "Query must have at most one placeholder '?', found " + placeholderCount);
    }
    this.query = validateQuery(trimmed);

    logger.info(
        "DatabaseFetchRule initialized with secure validation - URL: {}, Query length: {}",
        this.databaseUrl,
        this.query.length());
  }

  /**
   * データベースURLを検証します（セキュリティ対策）
   *
   * @param url 検証対象のURL
   * @return 検証済みのURL
   * @throws SecurityException 不正なURLが検出された場合
   */
  private String validateDatabaseUrl(String url) {
    if (url.isEmpty()) {
      throw new IllegalArgumentException("Database URL cannot be empty");
    }

    // 許可されたスキーマのチェック
    if (!ALLOWED_DB_SCHEME_PATTERN.matcher(url).matches()) {
      throw new SecurityException(
          "Database URL uses unsupported or potentially dangerous scheme: " + url);
    }

    // 危険な文字列の検出
    if (url.contains("..") || url.contains("file:") || url.contains("javascript:")) {
      throw new SecurityException("Database URL contains potentially dangerous patterns: " + url);
    }

    logger.debug("Database URL validation passed: {}", url);
    return url;
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
    // プレースホルダー付きクエリに空/null 入力が来た場合は DB に接続せず拒否する。
    // PooledDatabaseFetchRule.bindParameters() と同じ挙動に揃え、未バインドのまま executeQuery が
    // 走って SQLException("Parameter #1 is not set" 相当) になるのを防ぐ。
    if (query.contains("?") && (input == null || input.isEmpty())) {
      logger.warn(
          "Query has a placeholder but input is null or empty. Rejecting to prevent unbound parameter.");
      return "";
    }

    try (Connection connection = DriverManager.getConnection(databaseUrl);
        PreparedStatement statement = connection.prepareStatement(query)) {

      // データベース接続
      logger.debug("データベースに接続: {}", databaseUrl);

      // 入力文字列をパラメータとして設定（クエリに「?」プレースホルダーがある場合）
      if (query.contains("?") && input != null && !input.isEmpty()) {
        statement.setString(1, input);
        logger.debug("Parameter set for prepared statement: length={}", input.length());
      }

      // クエリ実行
      logger.debug("クエリを実行: {}", query);
      try (ResultSet resultSet = statement.executeQuery()) {
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
      }
    } catch (SQLException e) {
      logger.error("データベース操作中にエラーが発生しました: {}", e.getMessage(), e);
      Throwable[] suppressed = e.getSuppressed();
      if (suppressed != null) {
        for (Throwable s : suppressed) {
          logger.error("クローズ中に追加のエラーが発生しました: {}", s.getMessage(), s);
        }
      }
      sneakyThrow(new StreamProcessingException("データベースフェッチに失敗しました: " + e.getMessage(), e));
      throw new AssertionError("unreachable");
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
    throw (T) t;
  }
}
