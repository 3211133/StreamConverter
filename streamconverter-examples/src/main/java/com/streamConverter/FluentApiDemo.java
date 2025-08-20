package com.streamConverter.examples;

import com.streamconverter.CommandResult;
import com.streamConverter.api.StreamBuilder;
import com.streamConverter.api.Streams;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流暢なAPI（Fluent API）のデモンストレーション
 *
 * <p>新しいStreamBuilderとStreams APIの使用例を示します。 従来のコマンド配列による記述と比較して、より直感的で読みやすい
 * メソッドチェーンによる処理パイプラインの構築方法を紹介します。
 */
public class FluentApiDemo {

  private static final Logger logger = LoggerFactory.getLogger(FluentApiDemo.class);

  /**
   * Fluent APIデモのメインメソッド
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("🌊 StreamConverter - Fluent API Demo");
    logger.info("=====================================\n");

    try {
      // Demo 1: Basic StreamBuilder usage
      basicStreamBuilderDemo();

      // Demo 2: Data format specific APIs
      dataFormatSpecificDemo();

      // Demo 3: Complex pipeline construction
      complexPipelineDemo();

      // Demo 4: Conditional processing
      conditionalProcessingDemo();

      // Demo 5: Comparison with traditional approach
      comparisonDemo();

    } catch (Exception e) {
      logger.error("Demo failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Demo 1: 基本的なStreamBuilderの使用法 */
  private static void basicStreamBuilderDemo() throws IOException {
    logger.info("🔧 Basic StreamBuilder Usage");
    logger.info("==============================");

    String employeeData =
        "employee_id,name,department,salary\n"
            + "E001,John Smith,Engineering,75000\n"
            + "E002,Jane Doe,Marketing,65000\n"
            + "E003,Bob Johnson,Engineering,80000";

    // 流暢なAPIでCSV処理
    String result =
        StreamBuilder.create()
            .fromString(employeeData)
            .extractCsv("name")
            .process("name-formatter")
            .asString();

    logger.info("📋 Employee names extracted:");
    logger.info(result);

    // パイプライン情報を表示
    StreamBuilder pipeline =
        StreamBuilder.create()
            .fromString(employeeData)
            .extractCsv("department")
            .process("department-analyzer");

    logger.info("\n🔍 Pipeline structure:");
    logger.info(pipeline.getPipelineInfo());

    logger.info("Commands count: {}", pipeline.getCommandCount());
    logger.info("Is empty: {}", pipeline.isEmpty());

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demo 2: データフォーマット特化API */
  private static void dataFormatSpecificDemo() throws IOException {
    logger.info("📊 Data Format Specific APIs");
    logger.info("==============================");

    // JSON処理の例
    String jsonData =
        """
            {
                "user": {
                    "id": 123,
                    "name": "John Doe",
                    "email": "john@example.com",
                    "active": true
                },
                "timestamp": "2023-07-31T10:00:00Z"
            }
            """;

    logger.info("🟢 JSON Processing:");
    String jsonResult = Streams.json(jsonData).extract("user").format().asString();
    logger.info(jsonResult);

    // CSV処理の例
    String csvData =
        "product_id,name,price,category\n"
            + "P001,Laptop,999.99,Electronics\n"
            + "P002,Chair,149.99,Furniture";

    logger.info("\n🟡 CSV Processing:");
    String csvResult = Streams.csv(csvData).extract("price").process("price-formatter").asString();
    logger.info(csvResult);

    // XML処理の例
    String xmlData =
        """
            <?xml version="1.0"?>
            <configuration>
                <database>
                    <host>localhost</host>
                    <port>5432</port>
                </database>
            </configuration>
            """;

    logger.info("\n🟠 XML Processing:");
    String xmlResult =
        Streams.xml(xmlData).extract("configuration/database").process("config-parser").asString();
    logger.info(xmlResult);

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demo 3: 複雑なパイプライン構築 */
  private static void complexPipelineDemo() throws IOException {
    logger.info("⚙️ Complex Pipeline Construction");
    logger.info("==================================");

    String apiResponse =
        """
            {
                "status": "success",
                "data": {
                    "users": [
                        {"id": 1, "name": "John", "email": "john@example.com"},
                        {"id": 2, "name": "Jane", "email": "jane@example.com"}
                    ]
                }
            }
            """;

    // 複雑な処理パイプライン
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      List<CommandResult> results =
          StreamBuilder.create()
              .fromString(apiResponse)
              .formatJson() // JSONフォーマット
              .extractJson("data") // データ部分抽出
              .process("user-analyzer") // ユーザー分析
              .convertEncoding("UTF-8", "UTF-16") // エンコーディング変換
              .convertEncoding("UTF-16", "UTF-8") // 元に戻す
              .process("final-formatter") // 最終フォーマット
              .toStream(outputStream);

      logger.info("🔄 Complex pipeline executed successfully");
      logger.info("Commands executed: {}", results.size());
      logger.info(
          "Output preview: {}",
          outputStream.toString().substring(0, Math.min(100, outputStream.toString().length()))
              + "...");
    }

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demo 4: 条件付き処理 */
  private static void conditionalProcessingDemo() throws IOException {
    logger.info("🔀 Conditional Processing");
    logger.info("===========================");

    String configData =
        """
            {
                "environment": "production",
                "debug": false,
                "features": {
                    "logging": true,
                    "metrics": true
                }
            }
            """;

    boolean isProduction = true;
    boolean enableMetrics = true;

    // 条件付きパイプライン構築
    String result =
        StreamBuilder.create()
            .fromString(configData)
            .formatJson()
            .when(isProduction, builder -> builder.process("production-validator"))
            .extractJson("features")
            .when(enableMetrics, builder -> builder.process("metrics-collector"))
            .process("config-processor")
            .asString();

    logger.info("🎯 Conditional pipeline result:");
    logger.info(result);

    // パイプライン構築の情報を表示
    StreamBuilder conditionalPipeline =
        StreamBuilder.create()
            .fromString(configData)
            .formatJson()
            .when(false, builder -> builder.process("skipped-command")) // スキップされる
            .when(true, builder -> builder.process("executed-command")) // 実行される
            .extractJson("environment");

    logger.info("\n📋 Conditional pipeline structure:");
    logger.info(conditionalPipeline.getPipelineInfo());

    logger.info("\n" + "=".repeat(60) + "\n");
  }

  /** Demo 5: 従来のアプローチとの比較 */
  private static void comparisonDemo() throws IOException {
    logger.info("⚖️ Traditional vs Fluent API Comparison");
    logger.info("=========================================");

    String sampleData =
        """
            {
                "message": "Hello, StreamConverter!",
                "version": "2.0"
            }
            """;

    logger.info("📝 Traditional approach (verbose):");
    logger.info("   IStreamCommand[] commands = {");
    logger.info("       new JsonNavigateCommand(\"message\"),");
    logger.info("       new SampleStreamCommand(\"processor\")");
    logger.info("   };");
    logger.info("   StreamConverter converter = new StreamConverter(commands);");
    logger.info("   converter.run(inputStream, outputStream);");

    logger.info("\n✨ Fluent API approach (concise):");
    logger.info("   String result = Streams.json(data)");
    logger.info("       .extract(\"message\")");
    logger.info("       .process(\"processor\")");
    logger.info("       .asString();");

    // 実際に実行して結果を比較
    String fluentResult =
        Streams.json(sampleData).extract("message").process("message-processor").asString();

    logger.info("\n🎯 Fluent API result:");
    logger.info(fluentResult);

    logger.info("\n💡 Benefits of Fluent API:");
    logger.info("   ✓ More readable and intuitive");
    logger.info("   ✓ Type-safe method chaining");
    logger.info("   ✓ Conditional processing support");
    logger.info("   ✓ Built-in pipeline introspection");
    logger.info("   ✓ Reduced boilerplate code");

    logger.info("\n" + "=".repeat(60) + "\n");
  }
}
