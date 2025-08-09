package com.streamConverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * DatabaseFetchRuleの統合テスト
 *
 * <p>H2インメモリデータベースを使用してDatabaseFetchRuleの実際のデータベース操作をテストします。
 */
public class DatabaseFetchRuleIntegrationTest {

  private static final String DB_URL_BASE = "jdbc:h2:mem:testdb";
  private String dbUrl;
  private Connection connection;

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

      // 商品テーブル
      stmt.execute(
          "CREATE TABLE products (id INT PRIMARY KEY, name VARCHAR(100), price DECIMAL(10,2))");
      stmt.execute("INSERT INTO products VALUES (1, 'Laptop', 999.99)");
      stmt.execute("INSERT INTO products VALUES (2, 'Mouse', 29.99)");
      stmt.execute("INSERT INTO products VALUES (3, 'Keyboard', 79.99)");

      // NULLデータを含むテーブル
      stmt.execute("CREATE TABLE test_nulls (id INT PRIMARY KEY, nullable_col VARCHAR(50))");
      stmt.execute("INSERT INTO test_nulls VALUES (1, 'not null')");
      stmt.execute("INSERT INTO test_nulls VALUES (2, NULL)");
    }
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (connection != null && !connection.isClosed()) {
      connection.close();
    }
  }

  @Test
  @DisplayName("基本的なデータ取得テスト")
  public void testBasicDataFetch() {
    DatabaseFetchRule rule = new DatabaseFetchRule(dbUrl, "SELECT name FROM users WHERE id = ?");

    String result = rule.apply("1");
    assertEquals("John Doe", result);

    result = rule.apply("2");
    assertEquals("Jane Smith", result);

    result = rule.apply("3");
    assertEquals("Bob Johnson", result);
  }

  @Test
  @DisplayName("存在しないIDでの検索テスト")
  public void testNonExistentId() {
    DatabaseFetchRule rule = new DatabaseFetchRule(dbUrl, "SELECT name FROM users WHERE id = ?");

    String result = rule.apply("999");
    assertEquals("", result);
  }

  @Test
  @DisplayName("NULL値を含むデータの取得テスト")
  public void testNullValueFetch() {
    DatabaseFetchRule rule =
        new DatabaseFetchRule(dbUrl, "SELECT nullable_col FROM test_nulls WHERE id = ?");

    String result = rule.apply("1");
    assertEquals("not null", result);

    result = rule.apply("2");
    assertEquals("", result); // NULLは空文字列として返される
  }

  @Test
  @DisplayName("複数列を返すクエリのテスト")
  public void testMultipleColumns() {
    DatabaseFetchRule rule =
        new DatabaseFetchRule(dbUrl, "SELECT name, email FROM users WHERE id = ?");

    String result = rule.apply("1");
    assertEquals("John Doe", result); // 先頭列のみが返される
  }

  @Test
  @DisplayName("複数行を返すクエリのテスト")
  public void testMultipleRows() {
    DatabaseFetchRule rule = new DatabaseFetchRule(dbUrl, "SELECT name FROM users ORDER BY id");

    String result = rule.apply("dummy");
    assertEquals("John Doe", result); // 先頭行のみが返される
  }

  @Test
  @DisplayName("数値データの取得テスト")
  public void testNumericDataFetch() {
    DatabaseFetchRule rule =
        new DatabaseFetchRule(dbUrl, "SELECT price FROM products WHERE id = ?");

    String result = rule.apply("1");
    assertEquals("999.99", result);

    result = rule.apply("2");
    assertEquals("29.99", result);
  }

  @Test
  @DisplayName("パラメータなしクエリのテスト")
  public void testParameterlessQuery() {
    DatabaseFetchRule rule = new DatabaseFetchRule(dbUrl, "SELECT COUNT(*) FROM users");

    String result = rule.apply("ignored");
    assertEquals("3", result);
  }

  @Test
  @DisplayName("SQLインジェクション防止テスト")
  public void testSQLInjectionPrevention() {
    // 危険なクエリは初期化時に例外が発生する
    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule(dbUrl, "SELECT * FROM users; DROP TABLE users;");
        });

    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule(dbUrl, "INSERT INTO users VALUES (99, 'hacker', 'evil@hack.com')");
        });

    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule(dbUrl, "SELECT * FROM users UNION SELECT * FROM products");
        });
  }

  @Test
  @DisplayName("不正なデータベースURL拒否テスト")
  public void testInvalidDatabaseUrl() {
    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule("jdbc:evil:scheme://malicious", "SELECT * FROM users");
        });

    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule("file:///etc/passwd", "SELECT * FROM users");
        });
  }

  @Test
  @DisplayName("入力サニタイズテスト")
  public void testInputSanitization() {
    DatabaseFetchRule rule =
        new DatabaseFetchRule(dbUrl, "SELECT name FROM users WHERE name LIKE ?");

    // シングルクォートを含む入力
    String result = rule.apply("John's");
    // サニタイズされて検索されるが、マッチしないため空文字列
    assertEquals("", result);
  }

  @Test
  @DisplayName("大きなデータセットでのパフォーマンステスト")
  public void testLargeDatasetPerformance() throws Exception {
    // 大量のテストデータを作成
    try (Statement stmt = connection.createStatement()) {
      stmt.execute("CREATE TABLE large_table (id INT PRIMARY KEY, data VARCHAR(100))");

      for (int i = 1; i <= 1000; i++) {
        stmt.execute(String.format("INSERT INTO large_table VALUES (%d, 'Data %d')", i, i));
      }
    }

    DatabaseFetchRule rule =
        new DatabaseFetchRule(dbUrl, "SELECT data FROM large_table WHERE id = ?");

    long startTime = System.currentTimeMillis();

    // 複数回のクエリ実行
    for (int i = 1; i <= 100; i++) {
      String result = rule.apply(String.valueOf(i));
      assertEquals("Data " + i, result);
    }

    long endTime = System.currentTimeMillis();
    long duration = endTime - startTime;

    // パフォーマンス検証（100回のクエリが5秒以内に完了すること）
    assertTrue(
        duration < 5000,
        "100 queries should complete within 5 seconds, but took: " + duration + "ms");
  }

  @Test
  @DisplayName("接続エラーハンドリングテスト")
  public void testConnectionErrorHandling() {
    // 不正なJDBCドライバーURLを使用してエラーを発生させる
    // H2は存在しないデータベースでも自動作成するため、不正なドライバーを使用
    assertThrows(
        SecurityException.class,
        () -> {
          new DatabaseFetchRule("jdbc:invaliddriver://nonexistent", "SELECT 1");
        },
        "Should throw SecurityException for invalid database URL");
  }
}
