package com.streamConverter.examples;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DatabaseRuleDemoのテスト デモが正常に実行されることを確認 */
class DatabaseRuleDemoTest {

  @Test
  @DisplayName("DatabaseRuleDemoが正常に実行される")
  void testDemoRuns() {
    // デモのmainメソッドを呼び出して例外が発生しないことを確認
    assertDoesNotThrow(
        () -> {
          DatabaseRuleDemo.main(new String[] {});
        },
        "DatabaseRuleDemoは例外なく実行されるべき");
  }
}
