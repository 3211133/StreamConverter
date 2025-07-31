package com.streamConverter.examples;

import com.streamConverter.ContextAwareStreamConverter;
import com.streamConverter.command.IContextAwareStreamCommand;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * コンテキスト伝播機能のデモンストレーション
 *
 * <p>この例では、ExecutionContextとContextAwareStreamConverterを使用して マルチスレッド環境でのMDCコンテキスト伝播を実演します。
 */
public class ContextPropagationDemo {

  private static final Logger logger = LoggerFactory.getLogger(ContextPropagationDemo.class);

  /**
   * メインメソッド
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    logger.info("🔗 Context Propagation Demonstration");
    logger.info("===================================\n");

    try {
      // デモ1: 基本的なコンテキスト伝播
      demonstrateBasicContextPropagation();
      Thread.sleep(1000);

      // デモ2: カスタムコンテキストを使用した伝播
      demonstrateCustomContextPropagation();
      Thread.sleep(1000);

      // デモ3: 既存コマンドとコンテキスト対応コマンドの混在
      demonstrateMixedCommandTypes();
      Thread.sleep(1000);

      // デモ4: コンテキスト情報の活用
      demonstrateContextUsage();

    } catch (Exception e) {
      logger.error("Context propagation demonstration failed: {}", e.getMessage(), e);
    }
  }

  /** デモ1: 基本的なコンテキスト伝播 */
  private static void demonstrateBasicContextPropagation() throws IOException {
    logger.info("🔗 Demo 1: Basic Context Propagation");

    String testData = "id,name,department\n1,Alice,Engineering\n2,Bob,Marketing\n";

    // 基本的なパイプライン
    IStreamCommand validateCommand = new SampleStreamCommand("validator");
    IStreamCommand transformCommand = new SampleStreamCommand("transformer");
    IStreamCommand outputCommand = new SampleStreamCommand("output");

    // ContextAwareStreamConverterで実行
    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(validateCommand, transformCommand, outputCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing pipeline with automatic context propagation...");
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 1 completed. ExecutionId: {}", converter.getExecutionContext().getExecutionId());
    logger.info("Output length: {} characters\n", result.length());
  }

  /** デモ2: カスタムコンテキストを使用した伝播 */
  private static void demonstrateCustomContextPropagation() throws IOException {
    logger.info("🎯 Demo 2: Custom Context Propagation");

    // カスタムExecutionContextを作成
    ExecutionContext customContext =
        ExecutionContext.builder()
            .globalContext("requestId", "REQ-12345")
            .globalContext("userId", "user789")
            .globalContext("sessionId", "session456")
            .userContext("businessUnit", "finance")
            .userContext("priority", "high")
            .build();

    String testData = "transaction,amount,currency\n1,100.50,USD\n2,75.25,EUR\n";

    // コンテキスト対応コマンドを作成
    IContextAwareStreamCommand enrichmentCommand = createDataEnrichmentCommand();
    IContextAwareStreamCommand auditCommand = createAuditCommand();

    // カスタムコンテキストでコンバーター作成
    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(customContext, enrichmentCommand, auditCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing pipeline with custom context...");
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 2 completed. Custom ExecutionId: {}",
        converter.getExecutionContext().getExecutionId());
    logger.info("Output length: {} characters\n", result.length());
  }

  /** デモ3: 既存コマンドとコンテキスト対応コマンドの混在 */
  private static void demonstrateMixedCommandTypes() throws IOException {
    logger.info("🔀 Demo 3: Mixed Command Types");

    String testData = "product,price,category\nLaptop,999.99,Electronics\nBook,19.99,Education\n";

    // 既存のコマンドとコンテキスト対応コマンドを混在
    IStreamCommand legacyCommand = new SampleStreamCommand("legacy-processor");
    IContextAwareStreamCommand contextCommand = createProductAnalysisCommand();
    IStreamCommand anotherLegacyCommand = new SampleStreamCommand("legacy-formatter");

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(legacyCommand, contextCommand, anotherLegacyCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing mixed command pipeline...");
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 3 completed. Mixed ExecutionId: {}",
        converter.getExecutionContext().getExecutionId());
    logger.info("Output length: {} characters\n", result.length());
  }

  /** デモ4: コンテキスト情報の活用 */
  private static void demonstrateContextUsage() throws IOException {
    logger.info("📊 Demo 4: Context Information Usage");

    String testData =
        "metrics,value,timestamp\nCPU,75.5,2024-01-01T12:00:00Z\nMemory,60.2,2024-01-01T12:01:00Z\n";

    // 実行時情報を追跡するExecutionContext
    ExecutionContext trackingContext =
        ExecutionContext.builder()
            .globalContext("environment", "production")
            .globalContext("region", "us-east-1")
            .userContext("monitoringLevel", "detailed")
            .build();

    IContextAwareStreamCommand metricsProcessorCommand = createMetricsProcessorCommand();
    IContextAwareStreamCommand alertingCommand = createAlertingCommand();

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(
            trackingContext, metricsProcessorCommand, alertingCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing context-aware monitoring pipeline...");
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info(
        "Demo 4 completed. Tracking ExecutionId: {}",
        converter.getExecutionContext().getExecutionId());
    logger.info("Output length: {} characters", result.length());
    logger.info("All demonstrations completed successfully! 🎉");
  }

  /** データ拡充コマンドを作成 */
  private static IContextAwareStreamCommand createDataEnrichmentCommand() {
    return new IContextAwareStreamCommand() {
      private final Logger commandLogger = LoggerFactory.getLogger("DataEnrichment");

      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {

        // コンテキスト情報を取得
        String requestId = context.getGlobalContext("requestId");
        String userId = context.getGlobalContext("userId");
        String businessUnit = context.getUserContext("businessUnit");

        commandLogger.info(
            "Starting data enrichment for request: {}, user: {}, business unit: {}",
            requestId,
            userId,
            businessUnit);

        // データ処理をシミュレート
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        // コンテキストに処理結果を記録
        context.setUserContext("enrichmentStatus", "completed");
        context.setUserContext("enrichedRecords", "2");

        commandLogger.info(
            "Data enrichment completed. Records enriched: {}",
            context.getUserContext("enrichedRecords"));
      }
    };
  }

  /** 監査コマンドを作成 */
  private static IContextAwareStreamCommand createAuditCommand() {
    return new IContextAwareStreamCommand() {
      private final Logger commandLogger = LoggerFactory.getLogger("AuditCommand");

      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {

        String executionId = context.getExecutionId();
        String enrichmentStatus = context.getUserContext("enrichmentStatus");
        String enrichedRecords = context.getUserContext("enrichedRecords");

        commandLogger.info(
            "Audit checkpoint - ExecutionId: {}, Previous status: {}, Records: {}",
            executionId,
            enrichmentStatus,
            enrichedRecords);

        // データ処理
        inputStream.transferTo(outputStream);

        // 監査情報を記録
        context.setUserContext("auditCompleted", "true");
        context.setUserContext("auditTimestamp", String.valueOf(System.currentTimeMillis()));

        commandLogger.info("Audit completed for execution: {}", executionId);
      }
    };
  }

  /** 商品分析コマンドを作成 */
  private static IContextAwareStreamCommand createProductAnalysisCommand() {
    return new IContextAwareStreamCommand() {
      private final Logger commandLogger = LoggerFactory.getLogger("ProductAnalysis");

      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {

        int sequence = context.getCurrentCommandSequence();
        commandLogger.info("Performing product analysis (sequence: {})", sequence);

        // データ処理
        inputStream.transferTo(outputStream);

        // 分析結果をコンテキストに保存
        context.setUserContext("analysisCompleted", "true");
        context.setUserContext("productCategories", "Electronics,Education");

        commandLogger.info(
            "Product analysis completed. Categories found: {}",
            context.getUserContext("productCategories"));
      }
    };
  }

  /** メトリクス処理コマンドを作成 */
  private static IContextAwareStreamCommand createMetricsProcessorCommand() {
    return new IContextAwareStreamCommand() {
      private final Logger commandLogger = LoggerFactory.getLogger("MetricsProcessor");

      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {

        String environment = context.getGlobalContext("environment");
        String region = context.getGlobalContext("region");
        String monitoringLevel = context.getUserContext("monitoringLevel");

        commandLogger.info(
            "Processing metrics for environment: {}, region: {}, monitoring: {}",
            environment,
            region,
            monitoringLevel);

        // メトリクス処理をシミュレート
        inputStream.transferTo(outputStream);

        // 処理結果をコンテキストに記録
        context.setUserContext("metricsProcessed", "2");
        context.setUserContext("highPriorityAlerts", "0");

        commandLogger.info(
            "Metrics processing completed. Processed: {}, Alerts: {}",
            context.getUserContext("metricsProcessed"),
            context.getUserContext("highPriorityAlerts"));
      }
    };
  }

  /** アラート生成コマンドを作成 */
  private static IContextAwareStreamCommand createAlertingCommand() {
    return new IContextAwareStreamCommand() {
      private final Logger commandLogger = LoggerFactory.getLogger("AlertingCommand");

      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {

        String executionId = context.getExecutionId();
        String metricsProcessed = context.getUserContext("metricsProcessed");
        String environment = context.getGlobalContext("environment");

        commandLogger.info(
            "Generating alerts for execution: {}, environment: {}, metrics count: {}",
            executionId,
            environment,
            metricsProcessed);

        // アラート処理
        inputStream.transferTo(outputStream);

        // アラート結果を記録
        context.setUserContext("alertsGenerated", "0");
        context.setUserContext("alertingCompleted", "true");

        commandLogger.info(
            "Alerting completed. Generated {} alerts for execution: {}",
            context.getUserContext("alertsGenerated"),
            executionId);
      }
    };
  }
}
