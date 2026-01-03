package com.streamconverter.context;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * MDCスレッド間同期のテスト
 *
 * <p>以下のシナリオを検証:
 *
 * <ol>
 *   <li>親スレッドでのMDC.put() → 全ての子スレッドに伝播
 *   <li>子スレッドでのsetSharedContext() → 他の子スレッドに同期
 *   <li>子スレッド終了時 → 親スレッドに同期
 * </ol>
 */
class MDCThreadSynchronizationTest {

  private static final Logger LOG = LoggerFactory.getLogger(MDCThreadSynchronizationTest.class);

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  /**
   * テスト1: 親スレッドのMDC.put() → 全ての子スレッドに伝播
   *
   * <p>親スレッドで直接MDC.put()した値が、子スレッド（各Command）のMDCに反映されることを確認
   */
  @Test
  void testParentMDCPropagationToChildThreads() throws IOException {
    // 親スレッドでMDCに業務固有の値を設定
    MDC.put("requestId", "REQ-PARENT-123");
    MDC.put("userId", "PARENT_USER");

    ExecutionContext context = ExecutionContext.create();

    // 子スレッドで親のMDC値を直接assertするコマンド
    IStreamCommand childCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            // 子スレッド内でMDC値を直接検証
            String requestId = MDC.get("requestId");
            String userId = MDC.get("userId");

            LOG.info("Child thread sees: requestId={}, userId={}", requestId, userId);

            // Command内でassertion
            assertEquals("REQ-PARENT-123", requestId, "Child thread should see parent's requestId");
            assertEquals("PARENT_USER", userId, "Child thread should see parent's userId");

            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    StreamConverter converter = StreamConverter.createWithContext(context, childCommand);

    byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(testData);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // テスト実行（Command内でassertionが行われる）
    converter.run(input, output);
  }

  /**
   * テスト2: 子スレッドのsetSharedContext() → 他の子スレッドに同期
   *
   * <p>複数のコマンドが並列実行される際に、あるコマンドが設定したsharedContextの値が 他の並列実行中のコマンドから参照できることを確認
   */
  @Test
  void testChildSharedContextSynchronizationAcrossThreads() throws IOException {
    ExecutionContext context = ExecutionContext.create();

    CountDownLatch command1Started = new CountDownLatch(1);
    CountDownLatch command2CanProceed = new CountDownLatch(1);

    // Command1: sharedContextにuserIdを設定
    IStreamCommand command1 =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            // sharedContextに値を設定
            ctx.setSharedContext("userId", "USER_FROM_CMD1");
            LOG.info("Command1 set userId to sharedContext");

            command1Started.countDown();

            try {
              // Command2が値を読み取るまで待機
              command2CanProceed.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    // Command2: Command1が設定したsharedContextの値を読み取って検証
    IStreamCommand command2 =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            try {
              // Command1が値を設定するまで待機
              command1Started.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            // sharedContextから値を取得して直接検証
            String userId = ctx.getSharedContext("userId");
            LOG.info("Command2 sees userId from sharedContext: {}", userId);

            // Command内でassertion
            assertEquals(
                "USER_FROM_CMD1",
                userId,
                "Command2 should see userId set by Command1 via sharedContext");

            command2CanProceed.countDown();
            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    StreamConverter converter = StreamConverter.createWithContext(context, command1, command2);

    byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(testData);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // テスト実行（Command2内でassertionが行われる）
    converter.run(input, output);
  }

  /**
   * テスト3: 子スレッド終了時 → 親スレッドに同期
   *
   * <p>子スレッド（Command）が実行中にsetSharedContext()で設定した値が、 StreamConverter.run()完了後の親スレッドのMDCに反映されることを確認
   */
  @Test
  void testChildSharedContextSynchronizationToParentAfterCompletion() throws IOException {
    ExecutionContext context = ExecutionContext.create();

    // 子スレッドでsharedContextに値を設定するコマンド
    IStreamCommand childCommand =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            // XMLやJSONから抽出した想定でsharedContextに設定
            ctx.setSharedContext("extractedUserId", "USER_EXTRACTED_123");
            ctx.setSharedContext("extractedSessionId", "SESSION_XYZ");

            LOG.info("Child set extractedUserId and extractedSessionId to sharedContext");

            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    StreamConverter converter = StreamConverter.createWithContext(context, childCommand);

    byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(testData);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // 子スレッド実行前の親スレッドMDC状態
    assertNull(MDC.get("extractedUserId"));
    assertNull(MDC.get("extractedSessionId"));

    converter.run(input, output);

    // 検証: 子スレッドが設定したsharedContextの値が親スレッドのMDCに同期されている
    assertEquals("USER_EXTRACTED_123", MDC.get("extractedUserId"));
    assertEquals("SESSION_XYZ", MDC.get("extractedSessionId"));

    LOG.info(
        "Parent thread after completion: extractedUserId={}, extractedSessionId={}",
        MDC.get("extractedUserId"),
        MDC.get("extractedSessionId"));
  }

  /**
   * テスト4: 統合シナリオ - 親→子→子間→親の全てのMDC同期
   *
   * <p>実際の業務シナリオに近い統合テスト:
   *
   * <ol>
   *   <li>親スレッドでMDC.put("requestId", "REQ-001")
   *   <li>Command1が親のrequestIdを参照しながら、XMLからuserIdを抽出してsharedContextに設定
   *   <li>Command2が親のrequestIdとCommand1が設定したuserIdの両方を参照
   *   <li>親スレッドがCommand終了後にuserIdを参照できる
   * </ol>
   */
  @Test
  void testIntegratedMDCSynchronizationScenario() throws IOException {
    // 1. 親スレッドでMDC設定
    MDC.put("requestId", "REQ-INTEGRATED-001");

    ExecutionContext context = ExecutionContext.create();

    CountDownLatch command1Completed = new CountDownLatch(1);

    // Command1: XMLパース想定 - userIdを抽出してsharedContextに設定
    IStreamCommand command1 =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            // Command内で親のrequestIdを直接検証
            String requestId = MDC.get("requestId");
            LOG.info("Command1: requestId={}, set userId", requestId);

            assertEquals(
                "REQ-INTEGRATED-001", requestId, "Command1 should see parent's requestId in MDC");

            // XMLから抽出した想定でsharedContextに設定
            ctx.setSharedContext("userId", "USER_EXTRACTED_456");

            command1Completed.countDown();
            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    // Command2: 親のrequestIdとCommand1が設定したuserIdの両方を参照して検証
    IStreamCommand command2 =
        new IStreamCommand() {
          @Override
          public void execute(InputStream in, OutputStream out, ExecutionContext ctx)
              throws IOException {
            try {
              // Command1が完了するまで待機
              command1Completed.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }

            // Command内で親のrequestIdを直接検証
            String requestId = MDC.get("requestId");
            assertEquals(
                "REQ-INTEGRATED-001", requestId, "Command2 should see parent's requestId in MDC");

            // Command内でCommand1が設定したuserIdを直接検証
            String userId = ctx.getSharedContext("userId");
            assertEquals(
                "USER_EXTRACTED_456", userId, "Command2 should see userId set by Command1");

            LOG.info("Command2: requestId={}, userId={}", requestId, userId);

            in.transferTo(out);
          }

          @Override
          public void execute(InputStream in, OutputStream out) throws IOException {
            throw new UnsupportedOperationException("Should use ExecutionContext version");
          }
        };

    StreamConverter converter = StreamConverter.createWithContext(context, command1, command2);

    byte[] testData = "test data".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream input = new ByteArrayInputStream(testData);
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    // テスト実行（Command内でassertionが行われる）
    converter.run(input, output);

    // 親スレッドがCommand1が設定したuserIdを参照できることを検証
    assertEquals(
        "USER_EXTRACTED_456", MDC.get("userId"), "Parent should see userId after completion");

    LOG.info(
        "Parent thread final state: requestId={}, userId={}",
        MDC.get("requestId"),
        MDC.get("userId"));
  }
}
