package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.logging.MDCContext;
import com.streamconverter.logging.MDCInitializer;
import com.streamconverter.path.CSVPath;
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

/** MdcSetupRuleが兄弟スレッド（並列実行されるコマンド）間で 正しくMDC値を共有できるかを検証するテスト */
class MdcSetupRuleCrossThreadTest {

  private static final List<String> capturedUserIds = new CopyOnWriteArrayList<>();

  @BeforeEach
  void setUp() {
    MDCInitializer.initialize();
    MDC.clear();
    MDCContext.clearShared();
    capturedUserIds.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    MDCContext.clearShared();
  }

  @Test
  void testMdcSetupRuleValueAvailableInSubsequentCommands() throws IOException {
    // CSV: userId列を抽出してMDCに設定
    String csvData = "userId,name\nUSER-12345,Alice\n";

    // Command1: CSVからuserIdを抽出してMDCに設定
    IStreamCommand extractCommand =
        CsvNavigateCommand.create(new CSVPath("userId"), new MdcSetupRule("userId"));

    // Command2: MDCからuserIdを取得（抽出されたデータをキャプチャするだけ）
    IStreamCommand captureCommand1 = new UserIdCapturingCommand("Capture1");

    // Command3: さらにMDCからuserIdを取得
    IStreamCommand captureCommand2 = new UserIdCapturingCommand("Capture2");

    StreamConverter converter =
        StreamConverter.create(extractCommand, captureCommand1, captureCommand2);

    ByteArrayInputStream input = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream output = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(input, output);

    // 全コマンド成功
    assertEquals(3, results.size());
    assertTrue(results.stream().allMatch(CommandResult::isSuccess));

    // Command2とCommand3でMDC値が取得できているか確認
    assertEquals(2, capturedUserIds.size(), "Both capture commands should have captured userId");

    // MdcSetupRuleで設定された値が後続のコマンドで取得できる
    assertEquals(
        "USER-12345", capturedUserIds.get(0), "Command2 should have userId from MdcSetupRule");
    assertEquals(
        "USER-12345", capturedUserIds.get(1), "Command3 should have userId from MdcSetupRule");
  }

  /** MDCからuserIdを取得してキャプチャするコマンド */
  private static class UserIdCapturingCommand implements IStreamCommand {
    private final String name;

    public UserIdCapturingCommand(String name) {
      this.name = name;
    }

    @Override
    public void execute(InputStream input, OutputStream output) throws IOException {
      // データをそのまま通過（これにより前のコマンドの処理を待つ）
      input.transferTo(output);

      // データ処理後にMDCからuserIdを取得
      // （前のコマンドがMdcSetupRuleで設定した値が利用可能）
      String userId = MDC.get("userId");

      if (userId != null) {
        capturedUserIds.add(userId);
      }
    }
  }
}
