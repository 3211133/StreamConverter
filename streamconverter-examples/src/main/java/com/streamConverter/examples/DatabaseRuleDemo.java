package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.DatabaseFetchRule;
import com.streamConverter.path.CSVPath;
import com.streamConverter.path.JSONPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * DatabaseFetchRuleの実用的な使用例デモンストレーション
 *
 * <p>このデモでは以下の実用的なシナリオを示します： 1. 顧客IDを顧客名に変換 2. 商品コードを商品名と価格に変換 3. 部署コードを部署名に変換 4.
 * エラーハンドリング（存在しないIDの処理）
 */
public class DatabaseRuleDemo {

  private static final String DB_URL = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";

  public static void main(String[] args) {
    try {
      // デモ用データベースの初期化
      setupDemoDatabase();

      System.out.println("=== DatabaseFetchRule実用例デモ ===\n");

      // シナリオ1: JSON内の顧客IDを顧客名に変換
      demonstrateCustomerIdConversion();

      // シナリオ2: CSV内の商品コードを商品名に変換
      demonstrateProductCodeConversion();

      // シナリオ3: 複数のIDを含むJSONの一括変換
      demonstrateBatchProcessing();

      // シナリオ4: StreamConverterと組み合わせた複雑な処理
      demonstrateComplexPipeline();

      // シナリオ5: エラーハンドリングの例
      demonstrateErrorHandling();

    } catch (Exception e) {
      System.err.println("デモ実行中にエラーが発生しました: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** デモ用のデータベースセットアップ */
  private static void setupDemoDatabase() throws SQLException {
    try (Connection conn = DriverManager.getConnection(DB_URL)) {
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

      // テストデータ挿入
      insertDemoData(conn);

      System.out.println("✅ デモ用データベースを初期化しました");
    }
  }

  private static void insertDemoData(Connection conn) throws SQLException {
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

  /** シナリオ1: JSON内の顧客IDを顧客名に変換 */
  private static void demonstrateCustomerIdConversion() throws Exception {
    System.out.println("--- シナリオ1: 顧客IDを顧客名に変換 ---");

    // DatabaseFetchRuleの作成
    DatabaseFetchRule customerRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM customers WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command =
        new JsonNavigateCommand(new JSONPath("$.customerId"), customerRule);

    // 変換前のJSON
    String inputJson =
        """
            {
                "orderId": "ORD-2024-001",
                "customerId": "1001",
                "orderDate": "2024-01-15",
                "items": [
                    {"productCode": "LAPTOP001", "quantity": 1}
                ],
                "totalAmount": 98000
            }
            """;

    System.out.println("📄 変換前のJSON:");
    System.out.println(inputJson);

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    System.out.println("✨ 変換後のJSON:");
    System.out.println(output.toString());
    System.out.println();
  }

  /** シナリオ2: CSV内の商品コードを商品名に変換 */
  private static void demonstrateProductCodeConversion() throws Exception {
    System.out.println("--- シナリオ2: 商品コードを商品名に変換 ---");

    // DatabaseFetchRuleの作成
    DatabaseFetchRule productRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM products WHERE code = ?");

    // CsvNavigateCommandの作成
    CsvNavigateCommand command = new CsvNavigateCommand(new CSVPath("product_code"), productRule);

    // 変換前のCSV
    String inputCsv =
        """
            order_id,customer_id,product_code,quantity,unit_price
            ORD-001,1001,LAPTOP001,1,98000
            ORD-002,1002,MOUSE001,2,2980
            ORD-003,1003,MONITOR001,1,24800
            """;

    System.out.println("📊 変換前のCSV:");
    System.out.println(inputCsv);

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputCsv.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    System.out.println("✨ 変換後のCSV:");
    System.out.println(output.toString());
    System.out.println();
  }

  /** シナリオ3: 複数のIDを含むJSONの一括変換 */
  private static void demonstrateBatchProcessing() throws Exception {
    System.out.println("--- シナリオ3: 複数顧客IDの一括変換 ---");

    // DatabaseFetchRuleの作成
    DatabaseFetchRule customerRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM customers WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command =
        new JsonNavigateCommand(new JSONPath("$.customerId"), customerRule);

    // 複数の注文を含むJSON
    String inputJson =
        """
            [
                {
                    "orderId": "ORD-001",
                    "customerId": "1001",
                    "amount": 98000
                },
                {
                    "orderId": "ORD-002",
                    "customerId": "1002",
                    "amount": 5960
                },
                {
                    "orderId": "ORD-003",
                    "customerId": "1003",
                    "amount": 24800
                }
            ]
            """;

    System.out.println("📄 変換前のJSON配列:");
    System.out.println(inputJson);

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    System.out.println("✨ 変換後のJSON配列:");
    System.out.println(output.toString());
    System.out.println();
  }

  /** シナリオ4: StreamConverterと組み合わせた複雑な処理 */
  private static void demonstrateComplexPipeline() throws Exception {
    System.out.println("--- シナリオ4: 複雑なパイプライン処理 ---");

    // 顧客ID変換ルール
    DatabaseFetchRule customerRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM customers WHERE id = ?");

    // 部署コード変換ルール
    DatabaseFetchRule deptRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM departments WHERE code = ?");

    // 複数段階の変換を組み合わせる
    JsonNavigateCommand customerCommand =
        new JsonNavigateCommand(new JSONPath("$.customerId"), customerRule);
    JsonNavigateCommand deptCommand = new JsonNavigateCommand(new JSONPath("$.deptCode"), deptRule);

    // StreamConverterで複数のコマンドを組み合わせ
    StreamConverter converter =
        new StreamConverter(new IStreamCommand[] {customerCommand, deptCommand});

    // 変換前のJSON
    String inputJson =
        """
            {
                "employeeId": "EMP-001",
                "customerId": "1001",
                "deptCode": "DEV",
                "projectName": "新システム開発",
                "status": "進行中"
            }
            """;

    System.out.println("📄 変換前のJSON:");
    System.out.println(inputJson);

    // パイプライン実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    converter.run(input, output);

    System.out.println("✨ パイプライン変換後のJSON:");
    System.out.println(output.toString());
    System.out.println();
  }

  /** シナリオ5: エラーハンドリングの例 */
  private static void demonstrateErrorHandling() throws Exception {
    System.out.println("--- シナリオ5: エラーハンドリング ---");

    // DatabaseFetchRuleの作成
    DatabaseFetchRule customerRule =
        new DatabaseFetchRule(DB_URL, "SELECT name FROM customers WHERE id = ?");

    // JsonNavigateCommandの作成
    JsonNavigateCommand command =
        new JsonNavigateCommand(new JSONPath("$.customerId"), customerRule);

    // 存在しない顧客IDを含むJSON
    String inputJson =
        """
            {
                "orderId": "ORD-9999",
                "customerId": "9999",
                "amount": 50000,
                "note": "存在しない顧客IDのテスト"
            }
            """;

    System.out.println("📄 存在しない顧客IDを含むJSON:");
    System.out.println(inputJson);

    // 変換実行
    ByteArrayInputStream input = new ByteArrayInputStream(inputJson.getBytes());
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    command.execute(input, output);

    System.out.println("✨ 変換結果（存在しないIDは空文字列になる）:");
    System.out.println(output.toString());

    System.out.println("💡 DatabaseFetchRuleは存在しないIDに対して空文字列を返し、");
    System.out.println("   システム全体の安定性を保ちます。");
    System.out.println();
  }
}
