package com.streamconverter;

import static com.streamconverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import com.streamconverter.logging.MDCInitializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/** Integration test for StreamConverter with MDC propagation via InheritableMDCAdapter */
class StreamConverterMDCIntegrationTest {

  private static MDCAdapter originalSlf4jAdapter;
  private static MDCAdapter originalLogbackAdapter;
  private static Map<String, String> originalMdcContents;

  @BeforeAll
  static void installInheritableMDCAdapter() throws Exception {
    originalSlf4jAdapter = MDC.getMDCAdapter();
    originalLogbackAdapter = getLogbackAdapter();
    originalMdcContents = MDC.getCopyOfContextMap();
    MDCInitializer.initialize();
  }

  @AfterAll
  static void restoreOriginalAdapters() throws Exception {
    setSlf4jAdapter(originalSlf4jAdapter);
    setLogbackAdapter(originalLogbackAdapter);
    MDC.clear();
    if (originalMdcContents != null) {
      MDC.getMDCAdapter().setContextMap(originalMdcContents);
    }
  }

  private static MDCAdapter getLogbackAdapter() {
    try {
      Class<?> lc = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (lc.isInstance(factory)) {
        return (MDCAdapter) lc.getMethod("getMDCAdapter").invoke(factory);
      }
    } catch (Exception ignored) {
      // Logback not on classpath
    }
    return null;
  }

  private static void setSlf4jAdapter(MDCAdapter adapter) throws Exception {
    Field field = MDC.class.getDeclaredField("MDC_ADAPTER");
    field.setAccessible(true);
    field.set(null, adapter);
  }

  private static void setLogbackAdapter(MDCAdapter adapter) {
    if (adapter == null) return;
    try {
      Class<?> lc = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (lc.isInstance(factory)) {
        Method set = lc.getMethod("setMDCAdapter", MDCAdapter.class);
        set.invoke(factory, adapter);
      }
    } catch (Exception ignored) {
      // Logback not on classpath
    }
  }

  private static final Logger LOG =
      LoggerFactory.getLogger(StreamConverterMDCIntegrationTest.class);

  @Test
  void testMDCPropagation() throws IOException {
    // 親スレッドでMDCを設定
    MDC.put("requestId", "REQ-TEST-123");

    AtomicReference<String> workerMdc = new AtomicReference<>();

    try {
      IStreamCommand command =
          (in, out) -> {
            // ワーカースレッドで MDC の伝播を確認
            workerMdc.set(MDC.get("requestId"));
            in.transferTo(out);
          };
      StreamConverter converter = StreamConverter.create(command);

      String testData = createTestData("test,data", "1,value1", "2,value2");
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      converter.run(inputStream, outputStream);

      // ワーカースレッドに MDC が伝播されたことを確認
      assertEquals("REQ-TEST-123", workerMdc.get());

      // 出力データが正しく処理されたことを確認
      String result = outputStream.toString(StandardCharsets.UTF_8);
      assertEquals(testData, result);
    } finally {
      MDC.clear();
    }
  }

  @Test
  void testMultipleCommandsMDC() throws IOException {
    // 複数コマンドでのMDC機能テスト
    MDC.put("userId", "testuser");

    try {
      IStreamCommand command1 = (in, out) -> in.transferTo(out);
      IStreamCommand command2 = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(command1, command2);

      String testData = createTestData("multi,command,test", "x,y,z");
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      converter.run(inputStream, outputStream);

      // 出力データが正しく処理されたことを確認
      String result = outputStream.toString(StandardCharsets.UTF_8);
      assertEquals(testData, result);
    } finally {
      MDC.clear();
    }
  }

  @Test
  void testChildToChildMDCPropagation() throws IOException {
    // Command AでputSharedした値がCommand BのログでMDCに反映されることを検証
    MDC.put("requestId", "REQ-PROPAGATION-001");

    AtomicReference<String> capturedOrderId = new AtomicReference<>();
    CountDownLatch orderIdSet = new CountDownLatch(1);

    try {
      // Command A: ストリームデータから"orderId"を抽出してPipelineContextに設定
      IStreamCommand commandA =
          (in, out) -> {
            byte[] data = in.readAllBytes();
            String content = new String(data, StandardCharsets.UTF_8);

            // ストリームデータからorderIdを抽出（シミュレーション）
            String orderId = "ORD-" + content.substring(0, Math.min(3, content.length()));
            PipelineContext.putShared("orderId", orderId);
            LOG.info("Command A: extracted orderId={}", orderId);

            orderIdSet.countDown();
            out.write(data);
          };

      // Command B: ログ出力時にTurboFilter経由でorderIdがMDCに反映される
      IStreamCommand commandB =
          (in, out) -> {
            try {
              orderIdSet.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            // TurboFilter経由でMDCにsyncされるため、ログ出力時にorderIdが反映される
            // ここでは直接MDCを確認（TurboFilterがsyncToMDCを呼ぶのと同等の検証）
            PipelineContext.syncToMDC();
            capturedOrderId.set(MDC.get("orderId"));
            LOG.info("Command B: orderId from MDC={}", MDC.get("orderId"));

            in.transferTo(out);
          };

      StreamConverter converter = StreamConverter.create(commandA, commandB);

      String testData = createTestData("ABC,data", "1,value1");
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      converter.run(inputStream, outputStream);

      // Command AでputSharedした値がCommand Bで取得できたことを検証
      assertEquals("ORD-ABC", capturedOrderId.get());

      // パイプライン終了後にPipelineContextがクリアされていることを検証
      assertNull(PipelineContext.getShared("orderId"));
    } finally {
      MDC.clear();
    }
  }

  @Test
  void testPipelineContextDoesNotAffectCommandsNotUsingIt() throws IOException {
    // PipelineContextを使わないCommandに影響がないことを検証
    MDC.put("requestId", "REQ-NOOP-001");

    try {
      IStreamCommand simpleCommand = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(simpleCommand);

      String testData = createTestData("simple,data");
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      converter.run(inputStream, outputStream);

      assertEquals(testData, outputStream.toString(StandardCharsets.UTF_8));
    } finally {
      MDC.clear();
    }
  }

  @Test
  void testMDCNotPropagatedWithoutInitializer() throws Exception {
    // InheritableMDCAdapter を外した標準アダプターに一時切り替えて、
    // MDCInitializer 未インストール相当の状態 (SLF4J + Logback 両側) を再現する
    MDCAdapter savedSlf4j = MDC.getMDCAdapter();
    MDCAdapter savedLogback = getLogbackAdapter();

    MDCAdapter plainAdapter = new ch.qos.logback.classic.util.LogbackMDCAdapter();
    setSlf4jAdapter(plainAdapter);
    setLogbackAdapter(plainAdapter);

    MDC.put("requestId", "REQ-NOPROP-001");

    AtomicReference<String> workerMdc = new AtomicReference<>("NOT_SET");
    try {
      IStreamCommand command =
          (in, out) -> {
            workerMdc.set(MDC.get("requestId"));
            in.transferTo(out);
          };
      StreamConverter converter = StreamConverter.create(command);

      String testData = createTestData("data");
      converter.run(
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8)),
          new ByteArrayOutputStream());

      // 標準アダプターでは子スレッドに MDC が伝搬しない
      assertNull(workerMdc.get(), "MDC should NOT propagate without InheritableMDCAdapter");
    } finally {
      setSlf4jAdapter(savedSlf4j);
      setLogbackAdapter(savedLogback);
      MDC.clear();
    }
  }

  @Test
  void testAnonymousClassCommandNameInException() {
    // 匿名クラスで実装したコマンドが失敗したとき、例外メッセージに "IStreamCommand" が含まれる
    IStreamCommand failingCommand =
        new IStreamCommand() {
          @Override
          public void execute(java.io.InputStream in, java.io.OutputStream out) throws IOException {
            throw new IOException("intentional failure");
          }
        };

    StreamConverter converter = StreamConverter.create(failingCommand);

    StreamProcessingException ex =
        assertThrows(
            StreamProcessingException.class,
            () ->
                converter.run(
                    new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayOutputStream()));

    assertTrue(
        ex.getMessage().contains("IStreamCommand"),
        "Exception message should contain 'IStreamCommand' for anonymous class, but was: "
            + ex.getMessage());
  }
}
