package com.streamconverter.examples;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.SampleStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.path.CSVPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StreamConverterのMDC機能統合デモ
 *
 * <p>このデモでは、StreamConverterに統合されたMDC機能により、デフォルトでMDC同期が有効になることを示します。
 */
public class StreamConverterMDCDemo {
  private static final Logger logger = LoggerFactory.getLogger(StreamConverterMDCDemo.class);

  /**
   * Main method to demonstrate StreamConverter MDC integration.
   *
   * @param args command line arguments (not used)
   * @throws IOException if file operations fail
   */
  public static void main(String[] args) throws IOException {
    logger.info("🚀 StreamConverter MDC Integration Demo");

    demonstrateAutomaticMDC();
    demonstrateCustomContext();
    demonstrateMultipleCommandsMDC();

    logger.info("✅ All demonstrations completed successfully!");
  }

  /** デモ1: 自動MDC生成 */
  private static void demonstrateAutomaticMDC() throws IOException {
    logger.info("📊 Demo 1: Automatic MDC Generation");

    // 既存のAPIを使用 - MDCが自動的に有効化される
    SampleStreamCommand command = new SampleStreamCommand("auto-mdc");
    StreamConverter converter = StreamConverter.create(command);

    String testData = "id,name,department\n1,Alice,Engineering\n2,Bob,Marketing\n";
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing pipeline with automatic MDC...");
    List<CommandResult> results = converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 1 completed. Results: {} commands, Output length: {} characters\n",
        results.size(),
        result.length());
  }

  /** デモ2: カスタムMDCコンテキストでの処理 */
  private static void demonstrateCustomContext() throws IOException {
    logger.info("🎯 Demo 2: Custom MDC Context");

    // カスタムMDC値を作成
    java.util.Map<String, String> mdcValues = new java.util.HashMap<>();
    mdcValues.put("requestId", "REQ-DEMO-456");
    mdcValues.put("userId", "demo-user");
    mdcValues.put("sessionId", "session-789");
    mdcValues.put("businessUnit", "development");
    mdcValues.put("priority", "high");

    String testData = "transaction,amount,currency\n1,100.50,USD\n2,75.25,EUR\n";

    // カスタムMDC値でコンバーター作成
    SampleStreamCommand enrichmentCommand = new SampleStreamCommand("enrichment");
    SampleStreamCommand auditCommand = new SampleStreamCommand("audit");
    StreamConverter converter =
        StreamConverter.createWithMDC(mdcValues, enrichmentCommand, auditCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing pipeline with custom MDC...");
    List<CommandResult> results = converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info("Demo 2 completed. Results: {} commands", results.size());
    logger.info("Output length: {} characters\n", result.length());
  }

  /** デモ3: 複数コマンドでのMDC同期 */
  private static void demonstrateMultipleCommandsMDC() throws IOException {
    logger.info("🔀 Demo 3: Multiple Commands with MDC Synchronization");

    String testData = "product,price,category\nLaptop,999.99,Electronics\nBook,19.99,Education\n";

    // 複数のコマンドでパイプライン構築
    SampleStreamCommand validator = new SampleStreamCommand("validator");
    CsvNavigateCommand extractor =
        CsvNavigateCommand.create(new CSVPath("product"), new PassThroughRule());
    SampleStreamCommand formatter = new SampleStreamCommand("formatter");

    StreamConverter converter = StreamConverter.create(validator, extractor, formatter);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing multi-command pipeline with MDC synchronization...");
    List<CommandResult> results = converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info("Demo 3 completed. Results: {} commands executed", results.size());
    logger.info("Final output: {}", result.trim());
    logger.info("All commands completed with MDC synchronization\n");
  }
}
