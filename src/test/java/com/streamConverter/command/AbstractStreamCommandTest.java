package com.streamConverter.command;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractStreamCommandのテスト")
class AbstractStreamCommandTest {

  // テスト用の具象クラス
  private static class TestStreamCommand extends AbstractStreamCommand {
    private boolean executed = false;

    @Override
    public void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
      executed = true;
      // 単純に入力を出力にコピー
      inputStream.transferTo(outputStream);
    }

    public boolean isExecuted() {
      return executed;
    }
  }

  @Test
  @DisplayName("コンストラクタのテスト")
  void testConstructor() {
    // コンストラクタのテスト
    AbstractStreamCommand command = new TestStreamCommand();
    assertNotNull(command);
  }

  @Test
  @DisplayName("execute実装のテスト")
  void testExecuteImplementation() throws IOException {
    // executeメソッドの実装テスト
    TestStreamCommand command = new TestStreamCommand();
    String testInput = "Test input for AbstractStreamCommand";

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // executeが呼び出されたことを確認
      assertTrue(command.isExecuted());

      // 出力が入力と同じであることを確認
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("IStreamCommandインターフェース実装のテスト")
  void testIStreamCommandImplementation() {
    // IStreamCommandインターフェースの実装テスト
    AbstractStreamCommand command = new TestStreamCommand();
    assertTrue(command instanceof IStreamCommand);
  }
}
