package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.CommandConfig;
import com.streamConverter.command.CommandFactory;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.LoggingDecorator;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.impl.xml.XmlNavigateCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自動ログ出力機能のデモンストレーション
 *
 * <p>このクラスは、自動ログ出力機能の使用方法を示すサンプルです。 以下の機能をデモンストレーションします： - AbstractStreamCommandの自動ログ出力 -
 * LoggingDecoratorによる詳細ログ出力 - CommandFactoryによるログ機能付きコマンド生成 - パイプライン全体の自動ログ出力
 */
public class AutoLoggingDemo {
  private static final Logger log = LoggerFactory.getLogger(AutoLoggingDemo.class);

  /**
   * アプリケーションのエントリーポイント。自動ログ機能のデモンストレーションを実行します。
   *
   * @param args コマンドライン引数（使用されません）
   */
  public static void main(String[] args) {
    log.info("🚀 Auto Logging Demo Started");
    log.info("=============================\n");

    try {
      // デモ1: 標準的な自動ログ出力
      demonstrateStandardLogging();

      // デモ2: 詳細ログ出力（LoggingDecorator）
      demonstrateDetailedLogging();

      // デモ3: CommandFactoryによるログ機能付きコマンド生成
      demonstrateCommandFactory();

      // デモ4: パイプライン全体の自動ログ出力
      demonstrateAutoLoggingPipeline();

      // デモ5: エラーハンドリングとログ出力
      demonstrateErrorLogging();

      log.info("✅ Auto Logging Demo Completed Successfully!");

    } catch (Exception e) {
      log.error("❌ Auto Logging Demo Failed: {}", e.getMessage(), e);
    }
  }

  /** デモ1: 標準的な自動ログ出力 */
  private static void demonstrateStandardLogging() throws IOException {
    log.info("📊 Demo 1: Standard Auto Logging");
    log.info("=================================");

    String csvData = "name,age,city\nAlice,25,Tokyo\nBob,30,Osaka\n";

    // 通常のコマンド実行 - AbstractStreamCommandの自動ログ出力が動作
    IStreamCommand csvCommand = new CsvNavigateCommand("name");
    String result = processData(csvData, csvCommand);

    log.info("Standard logging result: {}", result.trim());
    log.info("");
  }

  /** デモ2: 詳細ログ出力（LoggingDecorator） */
  private static void demonstrateDetailedLogging() throws IOException {
    log.info("🔍 Demo 2: Detailed Logging with LoggingDecorator");
    log.info("================================================");

    String jsonData = "{\"users\":[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":30}]}";

    // LoggingDecoratorでラップして詳細ログ出力
    IStreamCommand originalCommand = new JsonNavigateCommand("name");
    IStreamCommand detailedCommand = new LoggingDecorator(originalCommand);

    String result = processData(jsonData, detailedCommand);

    log.info("Detailed logging result: {}", result.trim());
    log.info("");
  }

  /** デモ3: CommandFactoryによるログ機能付きコマンド生成 */
  private static void demonstrateCommandFactory() throws IOException {
    log.info("🏭 Demo 3: Command Factory with Auto Logging");
    log.info("============================================");

    String xmlData =
        """
            <?xml version="1.0"?>
            <users>
                <user>
                    <name>Alice</name>
                    <age>25</age>
                </user>
                <user>
                    <name>Bob</name>
                    <age>30</age>
                </user>
            </users>
            """;

    // CommandFactoryで自動ログ機能付きコマンドを生成
    IStreamCommand factoryCommand =
        CommandFactory.createWithLogging(XmlNavigateCommand.class, "users/user/name");

    String result = processData(xmlData, factoryCommand);

    log.info("Factory-created command result: {}", result.trim());
    log.info("");
  }

  /** デモ4: パイプライン全体の自動ログ出力 */
  private static void demonstrateAutoLoggingPipeline() throws IOException {
    log.info("🔗 Demo 4: Auto Logging Pipeline");
    log.info("================================");

    String csvData = "id,name,status\n1,Alice,active\n2,Bob,inactive\n3,Charlie,active\n";

    // CommandFactoryでパイプライン全体を生成
    IStreamCommand[] pipeline =
        CommandFactory.createPipelineWithDetailedLogging(
            new CommandConfig(CsvNavigateCommand.class, "CSV name extraction", "name"));

    String result = processData(csvData, pipeline);

    log.info("Pipeline result: {}", result.trim());
    log.info("");
  }

  /** デモ5: エラーハンドリングとログ出力 */
  private static void demonstrateErrorLogging() {
    log.info("⚠️ Demo 5: Error Handling and Logging");
    log.info("====================================");

    try {
      // 無効なXMLデータでエラーを発生させる
      String invalidXml = "<?xml version=\"1.0\"?><invalid><unclosed>";

      // 詳細ログ付きでコマンドを作成
      IStreamCommand baseCommand =
          CommandFactory.createWithLogging(XmlNavigateCommand.class, "invalid/path");
      IStreamCommand xmlCommand = new LoggingDecorator(baseCommand);
      processData(invalidXml, xmlCommand);

    } catch (Exception e) {
      log.info("Expected error was caught and logged appropriately");
    }

    log.info("");
  }

  /** パフォーマンステスト用の大量データ処理 */
  private static void demonstratePerformanceLogging() throws IOException {
    log.info("⚡ Demo 6: Performance Logging");
    log.info("==============================");

    // 大量データを生成
    StringBuilder largeData = new StringBuilder();
    largeData.append("name,age,city\n");

    for (int i = 1; i <= 10000; i++) {
      largeData.append(String.format("User%d,%d,City%d\n", i, 20 + (i % 50), i % 100));
    }

    // 詳細ログ付きでコマンドを作成
    IStreamCommand baseCommand = CommandFactory.createWithLogging(CsvNavigateCommand.class, "name");
    IStreamCommand command = new LoggingDecorator(baseCommand);

    long startTime = System.currentTimeMillis();
    String result = processData(largeData.toString(), command);
    long duration = System.currentTimeMillis() - startTime;

    log.info("Large data processing completed in {} ms", duration);
    log.info("Result lines: {}", result.split("\n").length);
    log.info("");
  }

  /** データ処理のヘルパーメソッド（単一コマンド） */
  private static String processData(String inputData, IStreamCommand command) throws IOException {
    return processData(inputData, new IStreamCommand[] {command});
  }

  /** データ処理のヘルパーメソッド（パイプライン） */
  private static String processData(String inputData, IStreamCommand[] commands)
      throws IOException {
    try (InputStream inputStream =
            new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      StreamConverter converter = new StreamConverter(commands);
      converter.run(inputStream, outputStream);

      return outputStream.toString(StandardCharsets.UTF_8);
    }
  }
}
