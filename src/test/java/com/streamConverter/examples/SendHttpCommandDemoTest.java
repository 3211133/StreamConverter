package com.streamConverter.examples;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SendHttpCommandDemoの動作テスト */
class SendHttpCommandDemoTest {

  @Test
  @DisplayName("SendHttpCommandDemoが正常に実行される")
  void testDemoRuns() {
    // デモのmainメソッドを呼び出して例外が発生しないことを確認
    assertDoesNotThrow(
        () -> {
          SendHttpCommandDemo.main(new String[] {});
        },
        "SendHttpCommandDemoは例外なく実行されるべき");
  }
}
