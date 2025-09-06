package com.streamconverter.examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * H2データベースの中身を素早く確認するためのシンプルなツール
 *
 * <p>このクラスは、DatabaseFetchRuleで使用されているデータベースの内容を プログラムから簡単に確認するための便利メソッドを提供します。
 */
public class QuickDatabaseLookup {

  private static final String DB_URL = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";

  public static void main(String[] args) {
    try {
      // デモデータベースを初期化
      initializeDemoDatabase();

      System.out.println("=== Quick Database Lookup ===\n");

      // 各テーブルの内容を表示
      System.out.println("📋 CUSTOMERS テーブル:");
      showTable("customers");

      System.out.println("\n📋 PRODUCTS テーブル:");
      showTable("products");

      System.out.println("\n📋 DEPARTMENTS テーブル:");
      showTable("departments");

      // 特定のクエリ実行例
      System.out.println("\n🔍 カスタムクエリ例:");
      executeQuery("SELECT name, email FROM customers WHERE id = 1001");

      System.out.println("\n🔍 JOIN クエリ例（仮想的な注文データ）:");
      executeQuery(
          """
                SELECT c.name as customer_name, p.name as product_name, p.price
                FROM customers c, products p
                WHERE c.id = 1001 AND p.code = 'LAPTOP001'
                """);

    } catch (Exception e) {
      System.err.println("エラーが発生しました: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** 指定したテーブルの全データを表示 */
  public static void showTable(String tableName) {
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      String query = "SELECT * FROM " + tableName;
      executeQuery(query);
    } catch (SQLException e) {
      System.err.println("テーブル " + tableName + " の表示に失敗: " + e.getMessage());
    }
  }

  /** SQLクエリを実行して結果を表示 */
  public static void executeQuery(String query) {
    try (Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement stmt = conn.prepareStatement(query);
        ResultSet rs = stmt.executeQuery()) {

      printResultSet(rs);

    } catch (SQLException e) {
      System.err.println("クエリの実行に失敗: " + e.getMessage());
      System.err.println("クエリ: " + query);
    }
  }

  /** 特定のIDの顧客情報を取得（DatabaseFetchRuleと同じ動作をテスト） */
  public static void lookupCustomer(int customerId) {
    System.out.println("🔍 顧客ID " + customerId + " の検索:");
    executeQuery("SELECT name FROM customers WHERE id = " + customerId);
  }

  /** 特定のコードの商品情報を取得（DatabaseFetchRuleと同じ動作をテスト） */
  public static void lookupProduct(String productCode) {
    System.out.println("🔍 商品コード " + productCode + " の検索:");
    executeQuery("SELECT name, price FROM products WHERE code = '" + productCode + "'");
  }

  /** データベースのテーブル一覧を表示 */
  public static void showTables() {
    System.out.println("📋 データベース内のテーブル一覧:");
    executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'");
  }

  /** ResultSetの内容を整形して表示 */
  private static void printResultSet(ResultSet rs) throws SQLException {
    ResultSetMetaData metaData = rs.getMetaData();
    int columnCount = metaData.getColumnCount();

    // ヘッダー行を表示
    for (int i = 1; i <= columnCount; i++) {
      System.out.printf("%-20s", metaData.getColumnName(i));
    }
    System.out.println();

    // 区切り線
    for (int i = 1; i <= columnCount; i++) {
      System.out.print("--------------------");
    }
    System.out.println();

    // データ行を表示
    int rowCount = 0;
    while (rs.next()) {
      for (int i = 1; i <= columnCount; i++) {
        String value = rs.getString(i);
        System.out.printf("%-20s", value != null ? value : "(NULL)");
      }
      System.out.println();
      rowCount++;
    }

    if (rowCount == 0) {
      System.out.println("(データがありません)");
    } else {
      System.out.println("(" + rowCount + " 行)");
    }
  }

  /** デモ用データベースの初期化（DatabaseRuleDemoと同じ） */
  private static void initializeDemoDatabase() throws SQLException {
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
      // テーブルが既に存在するかチェック
      String checkTable =
          "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CUSTOMERS'";
      try (PreparedStatement stmt = conn.prepareStatement(checkTable);
          ResultSet rs = stmt.executeQuery()) {

        if (rs.next() && rs.getInt(1) > 0) {
          return; // 既に初期化済み
        }
      }

      // テーブル作成
      conn.prepareStatement(
              """
                CREATE TABLE customers (
                    id INTEGER PRIMARY KEY,
                    name VARCHAR(100),
                    email VARCHAR(100),
                    phone VARCHAR(20)
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

      conn.prepareStatement(
              """
                CREATE TABLE departments (
                    code VARCHAR(10) PRIMARY KEY,
                    name VARCHAR(50),
                    manager VARCHAR(50)
                )
                """)
          .execute();

      // サンプルデータ挿入
      insertSampleData(conn);
    }
  }

  private static void insertSampleData(Connection conn) throws SQLException {
    // 顧客データ
    PreparedStatement customerStmt =
        conn.prepareStatement("INSERT INTO customers (id, name, email, phone) VALUES (?, ?, ?, ?)");

    customerStmt.setInt(1, 1001);
    customerStmt.setString(2, "田中太郎");
    customerStmt.setString(3, "tanaka@example.com");
    customerStmt.setString(4, "090-1234-5678");
    customerStmt.execute();

    customerStmt.setInt(1, 1002);
    customerStmt.setString(2, "佐藤花子");
    customerStmt.setString(3, "sato@example.com");
    customerStmt.setString(4, "090-9876-5432");
    customerStmt.execute();

    customerStmt.setInt(1, 1003);
    customerStmt.setString(2, "鈴木一郎");
    customerStmt.setString(3, "suzuki@example.com");
    customerStmt.setString(4, "090-5555-7777");
    customerStmt.execute();

    // 商品データ
    PreparedStatement productStmt =
        conn.prepareStatement(
            "INSERT INTO products (code, name, price, category) VALUES (?, ?, ?, ?)");

    productStmt.setString(1, "LAPTOP001");
    productStmt.setString(2, "高性能ビジネスノートPC");
    productStmt.setBigDecimal(3, new java.math.BigDecimal("98000.00"));
    productStmt.setString(4, "PC");
    productStmt.execute();

    productStmt.setString(1, "MOUSE001");
    productStmt.setString(2, "無線光学マウス");
    productStmt.setBigDecimal(3, new java.math.BigDecimal("2980.00"));
    productStmt.setString(4, "周辺機器");
    productStmt.execute();

    productStmt.setString(1, "MONITOR001");
    productStmt.setString(2, "24インチ液晶ディスプレイ");
    productStmt.setBigDecimal(3, new java.math.BigDecimal("24800.00"));
    productStmt.setString(4, "モニター");
    productStmt.execute();

    // 部署データ
    PreparedStatement deptStmt =
        conn.prepareStatement("INSERT INTO departments (code, name, manager) VALUES (?, ?, ?)");

    deptStmt.setString(1, "DEV");
    deptStmt.setString(2, "開発部");
    deptStmt.setString(3, "山田部長");
    deptStmt.execute();

    deptStmt.setString(1, "SALES");
    deptStmt.setString(2, "営業部");
    deptStmt.setString(3, "田村部長");
    deptStmt.execute();

    deptStmt.setString(1, "HR");
    deptStmt.setString(2, "人事部");
    deptStmt.setString(3, "川上部長");
    deptStmt.execute();
  }
}
