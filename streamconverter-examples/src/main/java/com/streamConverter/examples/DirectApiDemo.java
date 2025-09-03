package com.streamConverter.examples;

import com.streamConverter.CommandResult;
import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.impl.xml.XmlNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.path.CSVPath;
import com.streamConverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 直接的なAPI使用のデモンストレーション
 *
 * <p>StreamConverterとEnhancedCommandFactoryを使用した 処理パイプラインの構築方法を示します。
 * 流暢なAPIの代替として、より直接的で明示的なアプローチを紹介します。
 */
public class DirectApiDemo {

  private static final Logger logger = LoggerFactory.getLogger(DirectApiDemo.class);

  /**
   * 直接APIデモのメインメソッド
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    logger.info("🔧 StreamConverter - Direct API Demo");
    logger.info("====================================\n");

    try {
      // Demo 1: Basic CSV processing
      basicCsvProcessingDemo();

      // Demo 2: JSON processing
      jsonProcessingDemo();

      // Demo 3: XML processing
      xmlProcessingDemo();

      // Demo 4: Complex pipeline
      complexPipelineDemo();

    } catch (Exception e) {
      logger.error("Demo failed: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** Demo 1: 基本的なCSV処理 */
  private static void basicCsvProcessingDemo() throws IOException {
    logger.info("📊 Basic CSV Processing");
    logger.info("========================");

    String employeeData =
        "employee_id,name,department,salary\n"
            + "E001,John Smith,Engineering,75000\n"
            + "E002,Jane Doe,Marketing,65000\n"
            + "E003,Bob Johnson,Engineering,80000";

    // CSV名前抽出パイプライン
    IStreamCommand csvCommand = new CsvNavigateCommand(new CSVPath("name"), new PassThroughRule());
    StreamConverter converter = new StreamConverter(new IStreamCommand[] {csvCommand});

    String result = processString(converter, employeeData);

    logger.info("📋 Employee names extracted:");
    logger.info(result);
    logger.info("");
  }

  /** Demo 2: JSON処理 */
  private static void jsonProcessingDemo() throws IOException {
    logger.info("🔍 JSON Processing");
    logger.info("===================");

    String jsonData =
        """
        {
            "user": {
                "name": "Alice Johnson",
                "email": "alice@example.com",
                "preferences": {
                    "theme": "dark",
                    "notifications": true
                }
            }
        }
        """;

    // JSONパス抽出
    IStreamCommand jsonCommand =
        new JsonNavigateCommand(TreePath.fromJsonPath("$.user.name"), new PassThroughRule());
    StreamConverter converter = new StreamConverter(new IStreamCommand[] {jsonCommand});

    String result = processString(converter, jsonData);

    logger.info("👤 User name extracted:");
    logger.info(result);
    logger.info("");
  }

  /** Demo 3: XML処理 */
  private static void xmlProcessingDemo() throws IOException {
    logger.info("📄 XML Processing");
    logger.info("==================");

    String xmlData =
        """
        <?xml version="1.0" encoding="UTF-8"?>
        <product>
            <id>P001</id>
            <name>Laptop Computer</name>
            <category>Electronics</category>
            <price currency="USD">999.99</price>
        </product>
        """;

    // XML要素抽出
    IStreamCommand xmlCommand =
        new XmlNavigateCommand(TreePath.fromXmlPath("//name"), new PassThroughRule());
    StreamConverter converter = new StreamConverter(new IStreamCommand[] {xmlCommand});

    String result = processString(converter, xmlData);

    logger.info("🛒 Product name extracted:");
    logger.info(result);
    logger.info("");
  }

  /** Demo 4: 複合パイプライン */
  private static void complexPipelineDemo() throws IOException {
    logger.info("🔗 Complex Pipeline");
    logger.info("===================");

    String csvData =
        "id,data\n"
            + "1,'{\"message\":\"Hello World\",\"status\":\"active\"}'\n"
            + "2,'{\"message\":\"Goodbye\",\"status\":\"inactive\"}'";

    // 複合パイプライン: CSV -> JSON抽出
    IStreamCommand[] commands = {
      new CsvNavigateCommand(new CSVPath("data"), new PassThroughRule()),
      new JsonNavigateCommand(TreePath.fromJsonPath("$.message"), new PassThroughRule())
    };

    StreamConverter converter = new StreamConverter(commands);
    String result = processString(converter, csvData);

    logger.info("💬 Messages extracted from CSV->JSON pipeline:");
    logger.info(result);
    logger.info("");
  }

  /**
   * 文字列データを処理してStreamConverterで変換
   *
   * @param converter StreamConverterインスタンス
   * @param inputData 入力文字列データ
   * @return 処理結果の文字列
   * @throws IOException I/O例外
   */
  private static String processString(StreamConverter converter, String inputData)
      throws IOException {
    try (ByteArrayInputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      List<CommandResult> results = converter.run(inputStream, outputStream);
      CommandResult result = results.get(0);

      if (result.isSuccess()) {
        return outputStream.toString(StandardCharsets.UTF_8);
      } else {
        logger.warn("Processing failed: {}", result.getErrorMessage());
        return "Processing failed: " + result.getErrorMessage();
      }
    }
  }
}
