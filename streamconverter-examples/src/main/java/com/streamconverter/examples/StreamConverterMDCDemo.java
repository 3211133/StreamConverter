package com.streamconverter.examples;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.context.PipelineContext;
import com.streamconverter.path.CSVPath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * StreamConverterのMDC伝搬デモ
 *
 * <p>このデモでは、親スレッドで設定したMDC値がStreamConverter配下の 全コマンドに自動的に伝搬されることを示します。
 */
public class StreamConverterMDCDemo {
  private static final Logger logger = LoggerFactory.getLogger(StreamConverterMDCDemo.class);

  /**
   * Main method to demonstrate StreamConverter MDC propagation.
   *
   * @param args command line arguments (not used)
   * @throws IOException if file operations fail
   */
  public static void main(String[] args) throws IOException {
    logger.info("StreamConverter MDC Propagation Demo");

    demonstrateBasicMDC();
    demonstrateMultipleCommandsMDC();
    demonstrateChildToChildPropagation();

    logger.info("All demonstrations completed successfully!");
  }

  /** デモ1: 基本的なMDC伝搬 */
  private static void demonstrateBasicMDC() throws IOException {
    logger.info("Demo 1: Basic MDC Propagation");

    // 親スレッドでMDCを設定
    MDC.put("requestId", "REQ-DEMO-456");
    MDC.put("userId", "demo-user");

    try {
      IStreamCommand command = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(command);

      String testData = "id,name,department\n1,Alice,Engineering\n2,Bob,Marketing\n";
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      logger.info("Executing pipeline with MDC propagation...");
      converter.run(inputStream, outputStream);

      String result = outputStream.toString(StandardCharsets.UTF_8);
      logger.info("Demo 1 completed. Output length: {} characters", result.length());
    } finally {
      MDC.clear();
    }
  }

  /** デモ2: コマンド間MDC伝搬（PipelineContext） */
  private static void demonstrateChildToChildPropagation() throws IOException {
    logger.info("Demo 2: Child-to-Child MDC Propagation via PipelineContext");

    MDC.put("requestId", "REQ-CHILD-001");

    try {
      // Command A: ストリームデータからIDを抽出してPipelineContextに設定
      IStreamCommand extractId =
          (in, out) -> {
            byte[] data = in.readAllBytes();
            String content = new String(data, StandardCharsets.UTF_8);

            // 冒頭行からIDを抽出（シミュレーション）
            String orderId = content.lines().findFirst().orElse("UNKNOWN");
            PipelineContext.putShared("orderId", orderId);
            logger.info("Command A: extracted orderId from stream");

            out.write(data);
          };

      // Command B: TurboFilter経由でorderIdが自動的にMDCに反映される
      IStreamCommand process =
          (in, out) -> {
            // ログ出力時にTurboFilterがPipelineContextの共有値をMDCにsyncする
            // → このログにorderId が自動的に含まれる
            logger.info("Command B: processing with orderId automatically in MDC");
            in.transferTo(out);
          };

      StreamConverter converter = StreamConverter.create(extractId, process);

      String testData = "ORD-2026-0001\nitem1,100\nitem2,200\n";
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      converter.run(inputStream, outputStream);
      logger.info("Demo 2 completed. Output size: {} bytes", outputStream.size());
    } finally {
      MDC.clear();
    }
  }

  /** デモ3: 複数コマンドでのMDC伝搬 */
  private static void demonstrateMultipleCommandsMDC() throws IOException {
    logger.info("Demo 3: Multiple Commands with MDC Propagation");

    MDC.put("requestId", "REQ-MULTI-789");

    try {
      String testData = "product,price,category\nLaptop,999.99,Electronics\nBook,19.99,Education\n";

      // 複数のコマンドでパイプライン構築
      IStreamCommand validator = (in, out) -> in.transferTo(out);
      CsvNavigateCommand extractor =
          CsvNavigateCommand.create(new CSVPath("product"), new PassThroughRule());
      IStreamCommand formatter = (in, out) -> in.transferTo(out);

      StreamConverter converter = StreamConverter.create(validator, extractor, formatter);

      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      logger.info("Executing multi-command pipeline with MDC propagation...");
      converter.run(inputStream, outputStream);

      String result = outputStream.toString(StandardCharsets.UTF_8);
      logger.info("Demo 3 completed. Final output: {}", result.trim());
    } finally {
      MDC.clear();
    }
  }
}
