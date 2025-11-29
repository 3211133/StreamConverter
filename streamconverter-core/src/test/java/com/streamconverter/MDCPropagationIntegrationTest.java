package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.logging.MDCContext;
import com.streamconverter.logging.MDCInitializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** コマンド間でMDC値が正しく伝播・同期されているかを検証するテスト */
class MDCPropagationIntegrationTest {

  // 各コマンドで観測されたMDC値を記録
  private static final List<CapturedMDC> capturedMDCValues = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() {
    MDCInitializer.initialize();

    // MDCを完全にクリア（現在のスレッドのMDC値をクリア）
    MDC.clear();

    // 共有コンテキストもクリア
    MDCContext.clearShared();

    // テストデータをクリア
    capturedMDCValues.clear();

    // 前のテストから継承された可能性のある値を明示的に削除
    MDC.remove("commandSequence");
    MDC.remove("executionId");
    MDC.remove("stage");
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    MDCContext.clearShared();
  }

  @Test
  void testMDCPropagationAcrossMultipleCommands() throws IOException {
    // 親スレッドでMDC値を設定
    MDC.put("requestId", "REQ-12345");
    MDC.put("userId", "USER-999");

    // 3つのコマンドを作成（それぞれMDC値をキャプチャ）
    IStreamCommand command1 = new MDCCapturingCommand("Command1");
    IStreamCommand command2 = new MDCCapturingCommand("Command2");
    IStreamCommand command3 = new MDCCapturingCommand("Command3");

    StreamConverter converter = StreamConverter.create(command1, command2, command3);

    String testData = "test data";
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // パイプライン実行
    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 全コマンドが成功
    assertEquals(3, results.size());
    assertTrue(results.stream().allMatch(CommandResult::isSuccess));

    // 全コマンドでMDC値がキャプチャされている
    assertEquals(3, capturedMDCValues.size());

    // 各コマンドで親スレッドのMDC値が伝播していることを確認
    for (CapturedMDC captured : capturedMDCValues) {
      assertEquals(
          "REQ-12345",
          captured.requestId,
          captured.commandName + " should have requestId from parent thread");
      assertEquals(
          "USER-999",
          captured.userId,
          captured.commandName + " should have userId from parent thread");
      assertNotNull(
          captured.executionId,
          captured.commandName + " should have executionId set by StreamConverter");
      assertNotNull(
          captured.commandSequence, captured.commandName + " should have commandSequence");
      assertNotNull(captured.stage, captured.commandName + " should have stage");
    }

    // executionIdは全コマンドで同じ
    String firstExecutionId = capturedMDCValues.get(0).executionId;
    assertTrue(
        capturedMDCValues.stream().allMatch(c -> c.executionId.equals(firstExecutionId)),
        "All commands should share the same executionId");

    // commandSequenceは1, 2, 3と増加
    assertEquals("1", capturedMDCValues.get(0).commandSequence);
    assertEquals("2", capturedMDCValues.get(1).commandSequence);
    assertEquals("3", capturedMDCValues.get(2).commandSequence);

    // stageにはコマンド名とシーケンスが含まれる
    assertTrue(capturedMDCValues.get(0).stage.contains("MDCCapturingCommand"));
    assertTrue(capturedMDCValues.get(1).stage.contains("MDCCapturingCommand"));
    assertTrue(capturedMDCValues.get(2).stage.contains("MDCCapturingCommand"));
  }

  @Test
  void testMDCIsolationBetweenPipelineRuns() throws IOException {
    // 1回目の実行
    MDC.put("runId", "RUN-1");

    IStreamCommand command1 = new MDCCapturingCommand("Run1-Command");
    StreamConverter converter1 = StreamConverter.create(command1);

    ByteArrayInputStream input1 =
        new ByteArrayInputStream("data1".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output1 = new ByteArrayOutputStream();
    converter1.run(input1, output1);

    assertEquals(1, capturedMDCValues.size());
    assertEquals("RUN-1", capturedMDCValues.get(0).runId);

    capturedMDCValues.clear();
    MDC.clear();

    // 2回目の実行（異なるMDC値）
    MDC.put("runId", "RUN-2");

    IStreamCommand command2 = new MDCCapturingCommand("Run2-Command");
    StreamConverter converter2 = StreamConverter.create(command2);

    ByteArrayInputStream input2 =
        new ByteArrayInputStream("data2".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output2 = new ByteArrayOutputStream();
    converter2.run(input2, output2);

    assertEquals(1, capturedMDCValues.size());
    assertEquals("RUN-2", capturedMDCValues.get(0).runId);
  }

  /** MDC値をキャプチャするテスト用コマンド */
  private static class MDCCapturingCommand implements IStreamCommand {
    private final String name;

    public MDCCapturingCommand(String name) {
      this.name = name;
    }

    @Override
    public void execute(InputStream input, OutputStream output) throws IOException {
      // 現在のスレッドのMDC値をキャプチャ
      CapturedMDC captured = new CapturedMDC();
      captured.commandName = name;
      captured.requestId = MDC.get("requestId");
      captured.userId = MDC.get("userId");
      captured.runId = MDC.get("runId");
      captured.executionId = MDC.get("executionId");
      captured.commandSequence = MDC.get("commandSequence");
      captured.stage = MDC.get("stage");

      capturedMDCValues.add(captured);

      // データをそのまま通過
      input.transferTo(output);
    }
  }

  /** キャプチャされたMDC値 */
  private static class CapturedMDC {
    String commandName;
    String requestId;
    String userId;
    String runId;
    String executionId;
    String commandSequence;
    String stage;

    @Override
    public String toString() {
      return String.format(
          "CapturedMDC{command=%s, requestId=%s, userId=%s, runId=%s, executionId=%s, sequence=%s, stage=%s}",
          commandName, requestId, userId, runId, executionId, commandSequence, stage);
    }
  }
}
