package com.streamConverter.examples;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.ValueExtractionDecorator;
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
import org.slf4j.MDC;

/**
 * ValueExtractionDecoratorの使用例を示すデモクラス
 *
 * <p>JSON、XML、CSVの各形式からの値抽出とMDCコンテキスト連携を実演します。
 */
public class ValueExtractionDemo {
  private static final Logger logger = LoggerFactory.getLogger(ValueExtractionDemo.class);

  public static void main(String[] args) {
    logger.info("=== ValueExtractionDecorator Demo Start ===");

    try {
      // JSON形式からの値抽出デモ
      demonstrateJsonExtraction();

      // XML形式からの値抽出デモ
      demonstrateXmlExtraction();

      // CSV形式からの値抽出デモ
      demonstrateCsvExtraction();

      // 複数の値抽出を組み合わせたデモ
      demonstrateChainedExtraction();

    } catch (Exception e) {
      logger.error("Demo execution failed", e);
    } finally {
      MDC.clear();
    }

    logger.info("=== ValueExtractionDecorator Demo End ===");
  }

  /** JSON形式からの値抽出デモ */
  private static void demonstrateJsonExtraction() throws Exception {
    logger.info("--- JSON Value Extraction Demo ---");

    String jsonData =
        "{"
            + "\"user\":{"
            + "\"id\":\"U12345\","
            + "\"name\":\"John Doe\","
            + "\"email\":\"john@example.com\""
            + "},"
            + "\"items\":["
            + "{\"name\":\"Product A\",\"price\":100},"
            + "{\"name\":\"Product B\",\"price\":200}"
            + "]"
            + "}";

    // ユーザーIDを抽出するデコレータ
    IStreamCommand userIdExtractor =
        new ValueExtractionDecorator(new SampleStreamCommand(), "JSON", "$.user.id", "userId");

    // 最初の商品名を抽出するデコレータ
    IStreamCommand productExtractor =
        new ValueExtractionDecorator(userIdExtractor, "JSON", "$.items[0].name", "productName");

    executeWithContext(productExtractor, jsonData, "JSON extraction");
  }

  /** XML形式からの値抽出デモ */
  private static void demonstrateXmlExtraction() throws Exception {
    logger.info("--- XML Value Extraction Demo ---");

    String xmlData =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<order id=\"ORD12345\">"
            + "<customer>"
            + "<id>C67890</id>"
            + "<name>Jane Smith</name>"
            + "</customer>"
            + "<items>"
            + "<item id=\"I001\">Laptop</item>"
            + "<item id=\"I002\">Mouse</item>"
            + "</items>"
            + "</order>";

    // 注文IDを抽出
    IStreamCommand orderExtractor =
        new ValueExtractionDecorator(new SampleStreamCommand(), "XML", "//order/@id", "orderId");

    // 顧客IDを抽出
    IStreamCommand customerExtractor =
        new ValueExtractionDecorator(orderExtractor, "XML", "//customer/id/text()", "customerId");

    executeWithContext(customerExtractor, xmlData, "XML extraction");
  }

  /** CSV形式からの値抽出デモ */
  private static void demonstrateCsvExtraction() throws Exception {
    logger.info("--- CSV Value Extraction Demo ---");

    String csvData =
        "transaction_id,user_id,amount,currency\n"
            + "TXN001,U98765,250.00,USD\n"
            + "TXN002,U87654,150.50,EUR\n"
            + "TXN003,U76543,300.75,JPY";

    // トランザクションIDを抽出
    IStreamCommand transactionExtractor =
        new ValueExtractionDecorator(
            new SampleStreamCommand(), "CSV", "transaction_id", "transactionId");

    // ユーザーIDを抽出
    IStreamCommand userExtractor =
        new ValueExtractionDecorator(transactionExtractor, "CSV", "user_id", "userId");

    executeWithContext(userExtractor, csvData, "CSV extraction");
  }

  /** 複数の値抽出を組み合わせたデモ */
  private static void demonstrateChainedExtraction() throws Exception {
    logger.info("--- Chained Value Extraction Demo ---");

    String complexJsonData =
        "{"
            + "\"requestId\":\"REQ-2025-001\","
            + "\"api\":{"
            + "\"version\":\"v2.1\","
            + "\"endpoint\":\"/users/profile\""
            + "},"
            + "\"user\":{"
            + "\"id\":\"USR789\","
            + "\"profile\":{"
            + "\"email\":\"user@company.com\","
            + "\"department\":\"Engineering\""
            + "}"
            + "},"
            + "\"metadata\":{"
            + "\"timestamp\":\"2025-07-29T10:00:00Z\","
            + "\"source\":\"mobile-app\""
            + "}"
            + "}";

    // リクエストIDを抽出
    IStreamCommand requestIdExtractor =
        new ValueExtractionDecorator(
            new LoggingCommand("Processing complex request"), "JSON", "$.requestId", "requestId");

    // APIバージョンを抽出
    IStreamCommand apiVersionExtractor =
        new ValueExtractionDecorator(requestIdExtractor, "JSON", "$.api.version", "apiVersion");

    // ユーザーIDを抽出
    IStreamCommand userIdExtractor =
        new ValueExtractionDecorator(apiVersionExtractor, "JSON", "$.user.id", "userId");

    // 部署名を抽出
    IStreamCommand departmentExtractor =
        new ValueExtractionDecorator(
            userIdExtractor, "JSON", "$.user.profile.department", "department");

    executeWithContext(departmentExtractor, complexJsonData, "Chained extraction");
  }

  /** コマンドを実行コンテキスト付きで実行 */
  private static void executeWithContext(IStreamCommand command, String data, String description)
      throws Exception {
    ExecutionContext context = ExecutionContext.create();

    try (InputStream inputStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        OutputStream outputStream = new ByteArrayOutputStream()) {

      logger.info("Executing: {} with context ID: {}", description, context.getExecutionId());

      command.execute(inputStream, outputStream);

      // MDCから抽出された値を表示
      logger.info("Extracted values from MDC:");
      MDC.getCopyOfContextMap()
          .forEach(
              (key, value) -> {
                if (!key.equals("executionId")
                    && !key.equals("startTime")
                    && !key.equals("commandSequence")
                    && !key.equals("threadName")) {
                  logger.info("  {} = {}", key, value);
                }
              });

      logger.info("Output size: {} bytes", outputStream.toString().getBytes().length);

    } finally {
      MDC.clear();
    }
  }

  /** ログ出力用のシンプルなコマンド */
  private static class LoggingCommand implements IStreamCommand {
    private final String message;

    public LoggingCommand(String message) {
      this.message = message;
    }

    @Override
    public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
      logger.info("LoggingCommand: {}", message);

      // 入力をそのまま出力に転送
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }

      // MDCの内容もログ出力
      if (!MDC.getCopyOfContextMap().isEmpty()) {
        logger.info("Current MDC context: {}", MDC.getCopyOfContextMap());
      }
    }
  }
}
