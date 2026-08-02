package com.streamconverter.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import org.h2.tools.Server; // H2のWebサーバー機能は依存関係の問題でコメントアウト

/**
 * H2データベースの中身を確認するためのツール
 *
 * <p>このツールでは以下の方法でH2データベースにアクセスできます： 1. H2 Web Console（ブラウザ経由） 2. 対話型SQLクライアント（コマンドライン） 3.
 * データベース構造の表示
 */
// SystemPrintln: 対話型コンソールツールのため、System.out/err は「ログ」ではなく利用者向けUIチャネルそのもの。
// Scanner で標準入力を読み、メニューと結果を標準出力へ返す REPL であり、Logger への置換は用途上不適切。
// なお診断情報（スタックトレース）はUIではないため、そちらは Logger へ送っている。
// AvoidCatchingGenericException: 操作の失敗で対話セッションを落とさないための最上位境界での捕捉。
// TooManyMethods: メニュー操作とデモデータ整備が1クラスに同居しているため。分割は別途検討する。
@SuppressWarnings({"PMD.SystemPrintln", "PMD.AvoidCatchingGenericException", "PMD.TooManyMethods"})
public final class DatabaseInspector {

  private static final Logger log = LoggerFactory.getLogger(DatabaseInspector.class);

  private static final String DEFAULT_DB_URL = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";

  private DatabaseInspector() {
    // ユーティリティクラスのためインスタンス化しない
  }

  public static void main(String[] args) {
    try {
      System.out.println("=== H2 Database Inspector ===\n");

      // デモ用データベースを初期化
      initializeDemoDatabase();

      // メニュー表示
      showMenu();

    } catch (Exception e) {
      // 利用者へはメッセージを、診断用のスタックトレースはログへ送る
      System.err.println("エラーが発生しました: " + e.getMessage());
      log.error("DatabaseInspector aborted", e);
    }
  }

  /** メニューを表示して操作を選択 */
  private static void showMenu() {
    Scanner scanner = new Scanner(System.in);

    while (true) {
      System.out.println("\n--- 操作メニュー ---");
      System.out.println("1. H2 Web Console を起動");
      System.out.println("2. テーブル一覧を表示");
      System.out.println("3. テーブルのデータを表示");
      System.out.println("4. カスタムSQLクエリを実行");
      System.out.println("5. データベース構造を表示");
      System.out.println("0. 終了");
      System.out.print("\n選択してください (0-5): ");

      try {
        int choice = scanner.nextInt();
        scanner.nextLine(); // 改行を消費

        switch (choice) {
          case 1:
            startWebConsole();
            break;
          case 2:
            showTables();
            break;
          case 3:
            showTableData(scanner);
            break;
          case 4:
            executeCustomQuery(scanner);
            break;
          case 5:
            showDatabaseStructure();
            break;
          case 0:
            System.out.println("終了します。");
            return;
          default:
            System.out.println("無効な選択です。0-5の番号を入力してください。");
        }
      } catch (Exception e) {
        System.out.println("エラー: " + e.getMessage());
        scanner.nextLine(); // エラー時の入力をクリア
      }
    }
  }

  /** H2 Web Consoleを起動 */
  private static void startWebConsole() {
    System.out.println("⚠️  Web Console機能は現在無効になっています");
    System.out.println("💡 代替案:");
    System.out.println("   - メニューの「2. テーブル一覧を表示」でテーブルを確認");
    System.out.println("   - メニューの「3. テーブルのデータを表示」でデータを確認");
    System.out.println("   - メニューの「4. カスタムSQLクエリを実行」でクエリを実行");
    System.out.println("   - QuickDatabaseLookupクラスも利用可能です");

    /*
    // H2 Web Server依存関係の問題により一時的に無効化
    try {
      Server webServer = Server.createWebServer("-webAllowOthers", "-webPort", "8082");
      webServer.start();

      System.out.println("✅ H2 Web Console が起動されました！");
      System.out.println("📖 ブラウザで以下のURLにアクセスしてください：");
      System.out.println("   http://localhost:8082");
      System.out.println("\n💡 接続情報:");
      System.out.println("   JDBC URL: " + DEFAULT_DB_URL);
      System.out.println("   User Name: (空白)");
      System.out.println("   Password: (空白)");
      System.out.println("\n⚠️  注意: このプロセスを終了するとWebコンソールも停止します");

    } catch (SQLException e) {
      System.err.println("Web Console の起動に失敗しました: " + e.getMessage());
    }
    */
  }

  /** テーブル一覧を表示 */
  private static void showTables() {
    try (Connection conn = DriverManager.getConnection(DEFAULT_DB_URL)) {
      String query =
          "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'";

      try (PreparedStatement stmt = conn.prepareStatement(query);
          ResultSet rs = stmt.executeQuery()) {

        System.out.println("\n📋 テーブル一覧:");
        while (rs.next()) {
          System.out.println("  - " + rs.getString("TABLE_NAME"));
        }
      }
    } catch (SQLException e) {
      System.err.println("テーブル一覧の取得に失敗しました: " + e.getMessage());
    }
  }

  /** 指定したテーブルのデータを表示 */
  private static void showTableData(Scanner scanner) {
    System.out.print("テーブル名を入力してください: ");
    String tableName = scanner.nextLine().trim();

    if (tableName.isEmpty()) {
      System.out.println("テーブル名が入力されていません。");
      return;
    }

    try (Connection conn = DriverManager.getConnection(DEFAULT_DB_URL)) {
      String query = "SELECT * FROM " + tableName;

      try (PreparedStatement stmt = conn.prepareStatement(query);
          ResultSet rs = stmt.executeQuery()) {

        System.out.println("\n📊 テーブル「" + tableName + "」のデータ:");
        printResultSet(rs);
      }
    } catch (SQLException e) {
      System.err.println("データの取得に失敗しました: " + e.getMessage());
    }
  }

  /** カスタムSQLクエリを実行 */
  private static void executeCustomQuery(Scanner scanner) {
    System.out.print("SQLクエリを入力してください: ");
    String query = scanner.nextLine().trim();

    if (query.isEmpty()) {
      System.out.println("クエリが入力されていません。");
      return;
    }

    try (Connection conn = DriverManager.getConnection(DEFAULT_DB_URL)) {
      try (PreparedStatement stmt = conn.prepareStatement(query)) {

        if (query.trim().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
          // SELECT文の場合
          try (ResultSet rs = stmt.executeQuery()) {
            System.out.println("\n🔍 クエリ結果:");
            printResultSet(rs);
          }
        } else {
          // INSERT, UPDATE, DELETE等の場合
          int affectedRows = stmt.executeUpdate();
          System.out.println("✅ 実行完了。影響を受けた行数: " + affectedRows);
        }
      }
    } catch (SQLException e) {
      System.err.println("クエリの実行に失敗しました: " + e.getMessage());
    }
  }

  /** データベース構造を表示 */
  private static void showDatabaseStructure() {
    try (Connection conn = DriverManager.getConnection(DEFAULT_DB_URL)) {
      // テーブルとカラム情報を取得
      String query =
          """
                SELECT
                    TABLE_NAME,
                    COLUMN_NAME,
                    DATA_TYPE,
                    IS_NULLABLE,
                    COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = 'PUBLIC'
                ORDER BY TABLE_NAME, ORDINAL_POSITION
                """;

      try (PreparedStatement stmt = conn.prepareStatement(query);
          ResultSet rs = stmt.executeQuery()) {

        System.out.println("\n🏗️  データベース構造:");

        String currentTable = "";
        while (rs.next()) {
          String tableName = rs.getString("TABLE_NAME");

          if (!tableName.equals(currentTable)) {
            if (!currentTable.isEmpty()) {
              System.out.println();
            }
            System.out.println("📋 テーブル: " + tableName);
            currentTable = tableName;
          }

          String columnName = rs.getString("COLUMN_NAME");
          String dataType = rs.getString("DATA_TYPE");
          String nullable = rs.getString("IS_NULLABLE");
          String defaultValue = rs.getString("COLUMN_DEFAULT");

          System.out.printf(
              "  ├─ %-15s %-15s %s%s%n",
              columnName,
              dataType,
              "YES".equals(nullable) ? "(NULL可)" : "(NOT NULL)",
              defaultValue != null ? " デフォルト:" + defaultValue : "");
        }
      }
    } catch (SQLException e) {
      System.err.println("データベース構造の取得に失敗しました: " + e.getMessage());
    }
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
    while (rs.next()) {
      for (int i = 1; i <= columnCount; i++) {
        String value = rs.getString(i);
        System.out.printf("%-20s", value != null ? value : "(NULL)");
      }
      System.out.println();
    }
  }

  /** デモ用データベースの初期化 */
  private static void initializeDemoDatabase() throws SQLException {
    try (Connection conn = DriverManager.getConnection(DEFAULT_DB_URL)) {
      // テーブルを作成してデータを挿入（既存の場合はスキップ）
      String checkTable =
          "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CUSTOMERS'";
      try (PreparedStatement stmt = conn.prepareStatement(checkTable);
          ResultSet rs = stmt.executeQuery()) {

        if (rs.next() && rs.getInt(1) > 0) {
          System.out.println("✅ デモデータベースは既に初期化済みです");
          return;
        }
      }

      // テーブル作成とデータ挿入（DatabaseRuleDemoと同じ）
      setupDemoData(conn);
      System.out.println("✅ デモデータベースを初期化しました");
    }
  }

  private static void setupDemoData(Connection conn) throws SQLException {
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
  }
}
