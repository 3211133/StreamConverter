package com.streamConverter.examples;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** QuickDatabaseLookupの動作テスト */
class QuickDatabaseLookupTest {

  @Test
  @DisplayName("QuickDatabaseLookupが正常に実行される")
  void testQuickLookupRuns() {
    // QuickDatabaseLookupのmainメソッドを呼び出して例外が発生しないことを確認
    assertDoesNotThrow(
        () -> {
          QuickDatabaseLookup.main(new String[] {});
        },
        "QuickDatabaseLookupは例外なく実行されるべき");
  }

  @Test
  @DisplayName("個別のメソッドが正常に動作する")
  void testIndividualMethods() {
    // 各メソッドが例外なく動作することを確認
    assertDoesNotThrow(
        () -> {
          QuickDatabaseLookup.showTables();
          QuickDatabaseLookup.lookupCustomer(1001);
          QuickDatabaseLookup.lookupProduct("LAPTOP001");
        },
        "個別のメソッドは例外なく実行されるべき");
  }
}
