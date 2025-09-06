package com.streamconverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.command.impl.csv.CsvValidateCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * 複数コマンドを束にした複雑なパイプライン処理の例
 *
 * <p>以下のような処理フローを実装します： 1. バリデータコマンド（入力データ検証） 2. MDC設定コマンド（ログコンテキスト設定） 3. DB変換コマンド（データベース変換処理） 4.
 * 通信コマンド（外部API呼び出し） 5. バリデータコマンド（レスポンス検証） 6. DB逆変換コマンド（逆変換処理）
 */
public class ComplexPipelineExample {
  private static final Logger logger = LoggerFactory.getLogger(ComplexPipelineExample.class);

  /**
   * メインメソッド
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    logger.info("🔧 Complex Pipeline Processing Example");
    logger.info("=====================================\n");

    try {
      demonstrateComplexPipeline();
    } catch (Exception e) {
      logger.error("Complex pipeline demonstration failed: {}", e.getMessage(), e);
    }
  }

  /** 複雑なパイプライン処理のデモンストレーション */
  private static void demonstrateComplexPipeline() throws IOException {
    // サンプル入力データ（CSV形式）
    String inputCsvData =
        """
        id,name,email,department
        1,John Doe,john@example.com,Engineering
        2,Jane Smith,jane@example.com,Marketing
        3,Bob Johnson,bob@example.com,Sales
        """;

    logger.info("📋 Input Data:");
    logger.info(inputCsvData);

    // 複数コマンドを束にしたパイプライン構築
    IStreamCommand[] complexPipeline = {
      // 1. 入力バリデータコマンド
      createInputValidator(),

      // 2. MDC設定コマンド（ログコンテキスト）
      createMdcSetupCommand(),

      // 3. DB変換コマンド（データベース変換処理）
      createDbTransformCommand(),

      // 4. 通信コマンド（外部API呼び出し）
      createCommunicationCommand(),

      // 5. レスポンスバリデータコマンド
      createResponseValidator(),

      // 6. DB逆変換コマンド
      createDbReverseTransformCommand()
    };

    // パイプライン実行
    StreamConverter converter = StreamConverter.create(complexPipeline);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(inputCsvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("🚀 Executing complex pipeline with {} commands...", complexPipeline.length);

    long startTime = System.currentTimeMillis();
    converter.run(inputStream, outputStream);
    long endTime = System.currentTimeMillis();

    // 結果出力
    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info("✅ Pipeline completed successfully in {} ms", endTime - startTime);
    logger.info("📤 Final Result:");
    logger.info(result);

    logger.info("\n" + "=".repeat(60));
  }

  /** 入力バリデータコマンドを作成 */
  private static IStreamCommand createInputValidator() {
    logger.info("🔍 Creating input validator command");

    // CSV必須カラムを定義
    String[] requiredColumns = {"id", "name", "email", "department"};

    // CsvValidateCommandを使用して入力検証
    return new CsvValidateCommand(requiredColumns);
  }

  /** MDC設定コマンドを作成 */
  private static IStreamCommand createMdcSetupCommand() {
    logger.info("📝 Creating MDC setup command");

    return new IStreamCommand() {
      @Override
      public void execute(java.io.InputStream inputStream, java.io.OutputStream outputStream)
          throws IOException {
        logger.info("Setting up MDC context");

        // MDCにコンテキスト情報を設定
        MDC.put("requestId", "REQ-" + System.currentTimeMillis());
        MDC.put("pipelineStage", "mdc-setup");
        MDC.put("processType", "complex-pipeline");

        logger.info("MDC context configured successfully");

        // データをそのまま次のコマンドに渡す
        inputStream.transferTo(outputStream);

        logger.info("MDC setup command completed");
      }
    };
  }

  /** DB変換コマンドを作成 */
  private static IStreamCommand createDbTransformCommand() {
    logger.info("🔄 Creating DB transform command");

    return new SampleStreamCommand("db-transform") {
      @Override
      public void executeInternal(
          java.io.InputStream inputStream, java.io.OutputStream outputStream) throws IOException {
        MDC.put("pipelineStage", "db-transform");
        logger.info("Executing database transformation");

        // 実際のDB変換処理をシミュレート
        super.executeInternal(inputStream, outputStream);

        logger.info("Database transformation completed");
      }
    };
  }

  /** 通信コマンドを作成 */
  private static IStreamCommand createCommunicationCommand() {
    logger.info("🌐 Creating communication command");

    return new SampleStreamCommand("http-communication") {
      @Override
      public void executeInternal(
          java.io.InputStream inputStream, java.io.OutputStream outputStream) throws IOException {
        MDC.put("pipelineStage", "communication");
        logger.info("Executing external API communication");

        // 実際のHTTP通信をシミュレート
        // 本来であればSendHttpCommandを使用
        super.executeInternal(inputStream, outputStream);

        logger.info("External API communication completed");
      }
    };
  }

  /** レスポンスバリデータコマンドを作成 */
  private static IStreamCommand createResponseValidator() {
    logger.info("✅ Creating response validator command");

    return new SampleStreamCommand("response-validator") {
      @Override
      public void executeInternal(
          java.io.InputStream inputStream, java.io.OutputStream outputStream) throws IOException {
        MDC.put("pipelineStage", "response-validation");
        logger.info("Validating API response");

        // レスポンスバリデーション処理をシミュレート
        super.executeInternal(inputStream, outputStream);

        logger.info("Response validation completed");
      }
    };
  }

  /** DB逆変換コマンドを作成 */
  private static IStreamCommand createDbReverseTransformCommand() {
    logger.info("🔙 Creating DB reverse transform command");

    return new SampleStreamCommand("db-reverse-transform") {
      @Override
      public void executeInternal(
          java.io.InputStream inputStream, java.io.OutputStream outputStream) throws IOException {
        MDC.put("pipelineStage", "db-reverse-transform");
        logger.info("Executing database reverse transformation");

        // DB逆変換処理をシミュレート
        super.executeInternal(inputStream, outputStream);

        logger.info("Database reverse transformation completed");

        // MDCクリーンアップ
        MDC.clear();
        logger.info("MDC context cleared");
      }
    };
  }
}
