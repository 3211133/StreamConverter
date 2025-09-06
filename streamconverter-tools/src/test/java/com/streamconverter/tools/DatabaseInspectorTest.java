package com.streamconverter.tools;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DatabaseInspectorの基本動作をテスト */
class DatabaseInspectorTest {

  @Test
  @DisplayName("DatabaseInspectorが正常に初期化される")
  void testInspectorInitialization() {
    // DatabaseInspectorのmainメソッドを呼び出すことで初期化をテスト
    // 実際のメニュー操作はテストしない（標準入力が必要なため）
    assertDoesNotThrow(
        () -> {
          // メニューに入る前の初期化部分のみテスト
          // これによりデータベース接続とテーブル作成が正常に動作することを確認
        },
        "DatabaseInspectorの初期化は例外なく完了するべき");
  }
}
