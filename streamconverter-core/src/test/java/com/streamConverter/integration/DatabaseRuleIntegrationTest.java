package com.streamConverter.integration;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.DatabaseFetchRule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DatabaseFetchRuleとNavigateCommandsの統合テスト 実際のDBデータを使用してJSON/CSVの値を置換する動作を検証 */
// Re-enabled for cross-platform testing with improved H2 configuration
class DatabaseRuleIntegrationTest {

  private static final String DB_URL =
      "jdbc:h2:mem:integrationtest_"
          + System.currentTimeMillis()
          + ";DB_CLOSE_DELAY=-1;MODE=REGULAR;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";

  @BeforeEach
  void setUp() throws SQLException {
    // テスト用データベースの初期化
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      // テストテーブルの作成
      conn.prepareStatement("DROP TABLE IF EXISTS users").execute();
      conn.prepareStatement("DROP TABLE IF EXISTS products").execute();

      conn.prepareStatement(
              """
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    department VARCHAR(50)
                )
                """)
          .execute();

      conn.prepareStatement(
              """
                CREATE TABLE products (
                    code VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(100),
                    price DECIMAL(10,2),
                    category VARCHAR(50)
                )
                """)
          .execute();

      // テストデータの挿入
      PreparedStatement userStmt =
          conn.prepareStatement(
              "INSERT INTO users (id, name, email, department) VALUES (?, ?, ?, ?)");
      userStmt.setInt(1, 1001);
      userStmt.setString(2, "田中太郎");
      userStmt.setString(3, "tanaka@example.com");
      userStmt.setString(4, "開発部");
      userStmt.execute();

      userStmt.setInt(1, 1002);
      userStmt.setString(2, "佐藤花子");
      userStmt.setString(3, "sato@example.com");
      userStmt.setString(4, "営業部");
      userStmt.execute();

      PreparedStatement productStmt =
          conn.prepareStatement(
              "INSERT INTO products (code, name, price, category) VALUES (?, ?, ?, ?)");
      productStmt.setString(1, "P001");
      productStmt.setString(2, "高性能ノートPC");
      productStmt.setBigDecimal(3, new java.math.BigDecimal("120000.00"));
      productStmt.setString(4, "PC");
      productStmt.execute();

      productStmt.setString(1, "P002");
      productStmt.setString(2, "ワイヤレスマウス");
      productStmt.setBigDecimal(3, new java.math.BigDecimal("2980.00"));
      productStmt.setString(4, "周辺機器");
      productStmt.execute();
    }
  }

  @Test
  @DisplayName("JSON内のユーザーIDを名前に変換")
  void testJsonUserIdToNameConversion() throws IOException {
    // DatabaseFetchRuleの作成
    DatabaseFetchRule dbRule = new DatabaseFetchRule(DB_URL, "SELECT name FROM users WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command = new JsonNavigateCommand("$.userId", dbRule);

    // テスト用JSON
    String inputJson =
        """
            {
                "orderId": "ORD-001",
                "userId": "1001",
                "amount": 120000,
                "status": "pending"
            }
            """;

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    String result = output.toString();

    // 結果検証（テストデバッグ情報付き）
    System.out.println("JSON変換結果: " + result);
    assertTrue(result.contains("田中太郎"), "ユーザーIDが名前に変換されているべき");
    assertFalse(result.contains("1001"), "元のユーザーIDは残っていないべき");
    assertTrue(result.contains("ORD-001"), "他のフィールドは変更されないべき");
  }

  @Test
  @DisplayName("CSV内の商品コードを商品名に変換")
  void testCsvProductCodeToNameConversion() throws IOException {
    // DatabaseFetchRuleの作成
    DatabaseFetchRule dbRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM products WHERE code = ?");

    // CsvNavigateCommandの作成
    CsvNavigateCommand command = new CsvNavigateCommand("product_code", dbRule);

    // テスト用CSV
    String inputCsv =
        """
            order_id,customer_id,product_code,quantity,total
            ORD-001,1001,P001,1,120000
            ORD-002,1002,P002,2,5960
            """;

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputCsv.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    String result = output.toString();

    // 結果検証（テストデバッグ情報付き）
    System.out.println("CSV変換結果: " + result);
    assertTrue(result.contains("高性能ノートPC"), "商品コードが商品名に変換されているべき");
    assertTrue(result.contains("ワイヤレスマウス"), "商品コードが商品名に変換されているべき");
    assertFalse(result.contains("P001"), "元の商品コードは残っていないべき");
    assertFalse(result.contains("P002"), "元の商品コードは残っていないべき");
    assertTrue(result.contains("ORD-001"), "他のフィールドは変更されないべき");
  }

  @Test
  @DisplayName("存在しないIDの処理")
  void testNonExistentIdHandling() throws IOException {
    // DatabaseFetchRuleの作成
    DatabaseFetchRule dbRule = new DatabaseFetchRule(DB_URL, "SELECT name FROM users WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command = new JsonNavigateCommand("$.userId", dbRule);

    // 存在しないユーザーIDを含むJSON
    String inputJson =
        """
            {
                "orderId": "ORD-003",
                "userId": "9999",
                "amount": 50000
            }
            """;

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    String result = output.toString();

    // 結果検証（存在しないIDの場合は空文字列になる）
    assertTrue(result.contains("\"userId\":\"\""), "存在しないIDは空文字列になるべき");
    assertTrue(result.contains("ORD-003"), "他のフィールドは変更されないべき");
  }

  @Test
  @DisplayName("複数レコードのバッチ処理")
  void testMultipleRecordBatchProcessing() throws IOException {
    // DatabaseFetchRuleの作成
    DatabaseFetchRule dbRule =
        new DatabaseFetchRule(DB_URL, "SELECT department FROM users WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command = new JsonNavigateCommand("$.userId", dbRule);

    // 複数のユーザーIDを含むJSON配列
    String inputJson =
        """
            [
                {"orderId": "ORD-001", "userId": "1001", "amount": 10000},
                {"orderId": "ORD-002", "userId": "1002", "amount": 20000},
                {"orderId": "ORD-003", "userId": "1001", "amount": 15000}
            ]
            """;

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    String result = output.toString();

    // 結果検証（テストデバッグ情報付き）
    System.out.println("バッチ処理結果: " + result);
    assertTrue(result.contains("開発部"), "ユーザー1001の部署が変換されているべき");
    assertTrue(result.contains("営業部"), "ユーザー1002の部署が変換されているべき");
    // 開発部が2回出現する（ORD-001とORD-003で同じユーザー）
    assertEquals(2, result.split("開発部").length - 1, "開発部が2回出現するべき");
  }

  @Test
  @DisplayName("数値データの処理")
  void testNumericDataProcessing() throws IOException {
    // DatabaseFetchRuleの作成（価格を取得）
    DatabaseFetchRule dbRule =
        new DatabaseFetchRule(DB_URL, "SELECT price FROM products WHERE code = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command = new JsonNavigateCommand("$.productCode", dbRule);

    // テスト用JSON
    String inputJson =
        """
            {
                "itemId": "ITEM-001",
                "productCode": "P001",
                "quantity": 2
            }
            """;

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    String result = output.toString();

    // 結果検証
    assertTrue(result.contains("120000"), "商品価格が取得されているべき");
    assertFalse(result.contains("P001"), "元の商品コードは残っていないべき");
  }
}
