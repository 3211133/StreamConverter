package com.streamconverter.examples;

import com.streamConverter.CommandResult;
import com.streamConverter.StreamConverter;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.context.ExecutionContext;
import com.streamConverter.path.CSVPath;
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

  /** デモ2: カスタムコンテキストでのMDC */
  private static void demonstrateCustomContext() throws IOException {
    logger.info("🎯 Demo 2: Custom ExecutionContext with MDC");

    // カスタムExecutionContextを作成
    ExecutionContext customContext =
        ExecutionContext.builder()
            .globalContext("requestId", "REQ-DEMO-456")
            .globalContext("userId", "demo-user")
            .globalContext("sessionId", "session-789")
            .userContext("businessUnit", "development")
            .userContext("priority", "high")
            .build();

    String testData = "transaction,amount,currency\n1,100.50,USD\n2,75.25,EUR\n";

    // カスタムコンテキストでコンバーター作成
    SampleStreamCommand enrichmentCommand = new SampleStreamCommand("enrichment");
    SampleStreamCommand auditCommand = new SampleStreamCommand("audit");
    StreamConverter converter =
        StreamConverter.createWithContext(customContext, enrichmentCommand, auditCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing pipeline with custom context...");
    List<CommandResult> results = converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 2 completed. ExecutionId: {}, Results: {} commands",
        customContext.getExecutionId(),
        results.size());
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
