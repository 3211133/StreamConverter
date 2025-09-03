package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * MDCのマルチスレッド環境での動作検証のための実装例
 *
 * <p>このクラスでは以下の検証を行います： 1. 単一スレッドでのMDC動作 2. 複数スレッドでのMDC分離性 3. StreamConverterのマルチスレッドパイプラインでのMDC継承
 * 4. Thread-Local性の確認
 */
public class MDCMultiThreadExample {
  private static final Logger logger = LoggerFactory.getLogger(MDCMultiThreadExample.class);

  /**
   * メインメソッド
   *
   * @param args コマンドライン引数
   */
  public static void main(String[] args) {
    logger.info("🧪 MDC Multi-Thread Behavior Test");
    logger.info("=================================\n");

    try {
      // テスト1: 単一スレッドでのMDC動作確認
      demonstrateSingleThreadMDC();
      Thread.sleep(1000); // ログ出力の区切りのため

      // テスト2: 複数スレッドでのMDC分離性確認
      demonstrateMultiThreadMDCIsolation();
      Thread.sleep(1000);

      // テスト3: StreamConverterパイプラインでのMDC動作確認
      demonstrateStreamConverterMDC();
      Thread.sleep(1000);

      // テスト4: MDCの継承性とクリーンアップ確認
      demonstrateMDCInheritanceAndCleanup();

    } catch (Exception e) {
      logger.error("MDC demonstration failed: {}", e.getMessage(), e);
    }
  }

  /** テスト1: 単一スレッドでのMDC動作確認 */
  private static void demonstrateSingleThreadMDC() {
    logger.info("📝 Test 1: Single Thread MDC Behavior");
    logger.info("Current thread: {}", Thread.currentThread().getName());

    // MDCにコンテキスト情報を設定
    MDC.put("testCase", "single-thread");
    MDC.put("userId", "user123");
    MDC.put("sessionId", "session456");

    logger.info("MDC values set for single thread");
    logger.debug("Debugging in single thread context");
    logger.warn("Warning message with MDC context");

    // MDCクリア
    MDC.clear();
    logger.info("MDC cleared - this log should not have context");
    logger.info("Test 1 completed\n");
  }

  /** テスト2: 複数スレッドでのMDC分離性確認 */
  private static void demonstrateMultiThreadMDCIsolation() throws InterruptedException {
    logger.info("🔀 Test 2: Multi-Thread MDC Isolation");

    ExecutorService executor = Executors.newFixedThreadPool(3);
    CountDownLatch latch = new CountDownLatch(3);

    // 3つの異なるスレッドでMDCを設定
    for (int i = 1; i <= 3; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              // 各スレッドで異なるMDC値を設定
              MDC.put("testCase", "multi-thread-isolation");
              MDC.put("threadId", "thread-" + threadId);
              MDC.put("taskId", "task-" + threadId + "-" + System.currentTimeMillis());

              logger.info("Thread {} started with MDC context", threadId);

              // 処理時間をシミュレート
              Thread.sleep(100 + (threadId * 50));

              logger.info("Thread {} processing data", threadId);
              logger.warn("Thread {} encountered warning", threadId);
              logger.info("Thread {} completed", threadId);

              // MDCクリア
              MDC.clear();
              logger.info("Thread {} MDC cleared", threadId);

            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              logger.error("Thread {} interrupted", threadId);
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();
    logger.info("Test 2 completed\n");
  }

  /** テスト3: StreamConverterパイプラインでのMDC動作確認 */
  private static void demonstrateStreamConverterMDC() throws IOException {
    logger.info("🔄 Test 3: StreamConverter Pipeline MDC Behavior");

    // テストデータ
    String testData = "id,name,value\n1,test1,100\n2,test2,200\n";

    // MDC設定コマンドを作成
    IStreamCommand mdcSetupCommand = createMDCSetupCommand("pipeline-test");
    IStreamCommand processingCommand1 = createMDCProcessingCommand("stage1");
    IStreamCommand processingCommand2 = createMDCProcessingCommand("stage2");
    IStreamCommand mdcCleanupCommand = createMDCCleanupCommand();

    // パイプライン作成
    StreamConverter converter =
        StreamConverter.create(
            mdcSetupCommand, processingCommand1, processingCommand2, mdcCleanupCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    logger.info("Executing StreamConverter pipeline with MDC");
    converter.run(inputStream, outputStream);

    String result = outputStream.toString(StandardCharsets.UTF_8);
    logger.info("Pipeline result length: {} characters", result.length());
    logger.info("Test 3 completed\n");
  }

  /** テスト4: MDCの継承性とクリーンアップ確認 */
  private static void demonstrateMDCInheritanceAndCleanup() throws InterruptedException {
    logger.info("🧹 Test 4: MDC Inheritance and Cleanup");

    // メインスレッドでMDCを設定
    MDC.put("testCase", "inheritance-test");
    MDC.put("mainThreadId", Thread.currentThread().getName());
    MDC.put("parentContext", "main-context");

    logger.info("Main thread MDC setup complete");

    ExecutorService executor = Executors.newSingleThreadExecutor();
    CountDownLatch latch = new CountDownLatch(1);

    executor.submit(
        () -> {
          try {
            // 子スレッドでMDCの状態を確認
            logger.info("Child thread started - checking MDC inheritance");

            String inheritedTestCase = MDC.get("testCase");
            if (inheritedTestCase != null) {
              logger.warn("UNEXPECTED: Child thread inherited MDC value: {}", inheritedTestCase);
            } else {
              logger.info("EXPECTED: Child thread has clean MDC context");
            }

            // 子スレッドで独自のMDCを設定
            MDC.put("testCase", "child-thread-test");
            MDC.put("childThreadId", Thread.currentThread().getName());

            logger.info("Child thread set its own MDC context");
            logger.debug("Child thread debug message");

            // MDCクリア
            MDC.clear();
            logger.info("Child thread cleared its MDC");

          } finally {
            latch.countDown();
          }
        });

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    // メインスレッドのMDCが影響を受けていないかチェック
    logger.info("Back in main thread - checking MDC state");
    String mainTestCase = MDC.get("testCase");
    if (mainTestCase != null) {
      logger.info("Main thread MDC preserved: {}", mainTestCase);
    } else {
      logger.warn("Main thread MDC was affected by child thread");
    }

    MDC.clear();
    logger.info("Test 4 completed - all MDC cleared");
  }

  /** MDC設定用のコマンドを作成 */
  private static IStreamCommand createMDCSetupCommand(String contextId) {
    return new IStreamCommand() {
      @Override
      public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // MDCコンテキストを設定
        MDC.put("requestId", "REQ-" + System.currentTimeMillis());
        MDC.put("contextId", contextId);
        MDC.put("threadName", Thread.currentThread().getName());
        MDC.put("pipelineStage", "setup");

        logger.info("MDC setup command executed - context established");

        // データをそのまま次のステージに渡す
        inputStream.transferTo(outputStream);
      }
    };
  }

  /** MDC処理用のコマンドを作成 */
  private static IStreamCommand createMDCProcessingCommand(String stageName) {
    return new IStreamCommand() {
      @Override
      public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        // 現在のMDC状態を確認
        String requestId = MDC.get("requestId");
        // ステージ固有の情報を追加
        MDC.put("pipelineStage", stageName);
        MDC.put("stageThreadName", Thread.currentThread().getName());

        if (requestId != null) {
          logger.info("Processing stage {} with inherited requestId: {}", stageName, requestId);
        } else {
          logger.warn("Processing stage {} without inherited requestId", stageName);
        }

        logger.info("Stage {} processing data", stageName);

        // 簡単な処理をシミュレート
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        logger.info("Stage {} completed", stageName);
      }
    };
  }

  /** MDCクリーンアップ用のコマンドを作成 */
  private static IStreamCommand createMDCCleanupCommand() {
    return new IStreamCommand() {
      @Override
      public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
        logger.info("MDC cleanup command started");

        // データを最終出力に渡す
        inputStream.transferTo(outputStream);

        // MDCをクリア
        MDC.clear();
        logger.info("MDC cleanup command completed - all context cleared");
      }
    };
  }
}
