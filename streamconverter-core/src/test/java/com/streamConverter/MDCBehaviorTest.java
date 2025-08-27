package com.streamConverter;

import static com.streamConverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.streamConverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * MDCのマルチスレッド環境での動作をテストするクラス
 *
 * <p>このテストクラスでは以下の観点を検証します： 1. MDCのThread-Local性 2. スレッド間でのMDC分離 3. StreamConverterでのMDC動作 4.
 * MDCの継承と非継承の挙動
 */
class MDCBehaviorTest {

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    // テスト用のログアペンダーを設定
    logger = (Logger) LoggerFactory.getLogger(MDCBehaviorTest.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // MDCをクリア
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    // MDCをクリア
    MDC.clear();

    // アペンダーを削除
    if (logger != null && listAppender != null) {
      logger.detachAppender(listAppender);
    }
  }

  @Test
  void testMDCThreadLocalBehavior() throws InterruptedException {
    // メインスレッドでMDCを設定
    MDC.put("mainThread", "main-value");
    MDC.put("testId", "thread-local-test");

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> childThreadValue = new AtomicReference<>();

    // 別スレッドでMDCの値を確認
    Thread childThread =
        new Thread(
            () -> {
              try {
                // 子スレッドでMDCの値を確認（継承されないはず）
                String inheritedValue = MDC.get("mainThread");
                childThreadValue.set(inheritedValue);

                // 子スレッドで独自のMDCを設定
                MDC.put("childThread", "child-value");
                MDC.put("testId", "child-test");

                logger.info("Child thread logging");

              } finally {
                latch.countDown();
              }
            });

    childThread.start();
    boolean awaitResult = latch.await(5, TimeUnit.SECONDS);
    assertTrue(awaitResult, "Latch await should complete within timeout");

    // メインスレッドのMDCは変更されていないはず
    assertEquals("main-value", MDC.get("mainThread"));
    assertEquals("thread-local-test", MDC.get("testId"));

    // 子スレッドではMDCが継承されていないはず
    assertNull(childThreadValue.get(), "Child thread should not inherit MDC values");

    logger.info("Main thread logging");
  }

  @Test
  void testMDCIsolationBetweenThreads() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(3);
    CountDownLatch latch = new CountDownLatch(3);
    AtomicInteger conflictCount = new AtomicInteger(0);

    // 3つのスレッドで同じキーに異なる値を設定
    for (int i = 1; i <= 3; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              MDC.put("threadId", "thread-" + threadId);
              MDC.put("sharedKey", "value-" + threadId);

              // 少し待ってから値を確認
              Thread.sleep(50);

              String actualThreadId = MDC.get("threadId");
              String actualSharedValue = MDC.get("sharedKey");

              // 設定した値と異なる場合は競合が発生
              if (!("thread-" + threadId).equals(actualThreadId)
                  || !("value-" + threadId).equals(actualSharedValue)) {
                conflictCount.incrementAndGet();
              }

              logger.info("Thread {} completed", threadId);

            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              MDC.clear();
              latch.countDown();
            }
          });
    }

    boolean awaitResult = latch.await(10, TimeUnit.SECONDS);
    assertTrue(awaitResult, "Latch await should complete within timeout");
    executor.shutdown();

    // スレッド間でMDCの競合が発生していないことを確認
    assertEquals(0, conflictCount.get(), "MDC values should be isolated between threads");
  }

  @Test
  void testStreamConverterMDCBehavior() throws IOException {
    String testData = createTestData("test,data,content", "1,2,3", "4,5,6");

    // MDC設定コマンド
    IStreamCommand mdcCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream inputStream, OutputStream outputStream)
              throws IOException {
            MDC.put("requestId", "REQ-123");
            MDC.put("command", "mdc-setup");
            logger.info("MDC setup in pipeline");
            inputStream.transferTo(outputStream);
          }
        };

    // MDC確認コマンド
    IStreamCommand verifyCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream inputStream, OutputStream outputStream)
              throws IOException {
            String requestId = MDC.get("requestId");
            String command = MDC.get("command");

            // マルチスレッド環境ではMDCが引き継がれない可能性がある
            logger.info("Verify command - requestId: {}, command: {}", requestId, command);

            MDC.put("command", "verify");
            inputStream.transferTo(outputStream);
          }
        };

    StreamConverter converter = StreamConverter.create(mdcCommand, verifyCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);

    // 結果の検証
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);

    // ログメッセージの確認
    List<ILoggingEvent> logEvents = listAppender.list;
    assertTrue(logEvents.size() > 0, "Should have log events");

    // MDC付きのログイベントがあることを確認
    boolean hasMDCLog =
        logEvents.stream()
            .anyMatch(
                event -> event.getMDCPropertyMap() != null && !event.getMDCPropertyMap().isEmpty());

    assertTrue(hasMDCLog, "Should have at least one log event with MDC properties");
  }

  @Test
  void testMDCCleanupInPipeline() throws IOException {
    String testData = createTestData("cleanup,test", "a,b");

    IStreamCommand setupCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream inputStream, OutputStream outputStream)
              throws IOException {
            MDC.put("stage", "setup");
            MDC.put("cleanup-test", "should-be-cleared");
            logger.info("Setup stage");
            inputStream.transferTo(outputStream);
          }
        };

    IStreamCommand cleanupCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream inputStream, OutputStream outputStream)
              throws IOException {
            logger.info("Before cleanup");
            inputStream.transferTo(outputStream);

            // MDCをクリア
            MDC.clear();
            logger.info("After cleanup - MDC cleared");
          }
        };

    StreamConverter converter = StreamConverter.create(setupCommand, cleanupCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);

    // パイプライン実行後、メインスレッドのMDCは影響されていないはず
    String remainingValue = MDC.get("cleanup-test");
    assertNull(remainingValue, "Main thread MDC should not be affected by pipeline cleanup");
  }

  @Test
  void testMDCPerformanceImpact() throws IOException, InterruptedException {
    String testData =
        createTestData(
            java.util.stream.Stream.concat(
                    java.util.stream.Stream.of("performance,test,data"),
                    java.util.stream.Stream.generate(() -> "row,data,value").limit(1000))
                .toArray(String[]::new));

    // MDCなしでの実行時間測定
    long startTime = System.currentTimeMillis();
    runSimplePipeline(testData, false);
    long timeWithoutMDC = System.currentTimeMillis() - startTime;

    Thread.sleep(100); // 測定間の間隔

    // MDCありでの実行時間測定
    startTime = System.currentTimeMillis();
    runSimplePipeline(testData, true);
    long timeWithMDC = System.currentTimeMillis() - startTime;

    logger.info("Performance - Without MDC: {}ms, With MDC: {}ms", timeWithoutMDC, timeWithMDC);

    // MDCのオーバーヘッドが過度でないことを確認（10倍以内）
    assertTrue(
        timeWithMDC < timeWithoutMDC * 10,
        "MDC overhead should be reasonable: " + timeWithMDC + "ms vs " + timeWithoutMDC + "ms");
  }

  private void runSimplePipeline(String testData, boolean useMDC) throws IOException {
    IStreamCommand command =
        new IStreamCommand() {
          @Override
          public void execute(InputStream inputStream, OutputStream outputStream)
              throws IOException {
            if (useMDC) {
              MDC.put("performance-test", "enabled");
              MDC.put("timestamp", String.valueOf(System.currentTimeMillis()));
            }

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
              outputStream.write(buffer, 0, bytesRead);
            }

            if (useMDC) {
              MDC.clear();
            }
          }
        };

    StreamConverter converter = StreamConverter.create(command);
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);
  }
}
