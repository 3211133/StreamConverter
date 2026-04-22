package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("SqlQueryUtils.validateQuery のSQLインジェクション検出テスト")
class SqlQueryUtilsTest {

  private static final Logger logger = LoggerFactory.getLogger(SqlQueryUtilsTest.class);

  // ---- #651 fix: find() + DOTALL で部分一致検出 ----

  @Test
  @DisplayName("UNION を含むクエリはブロックされる（matches()では通過していたケース）")
  void testUnionInjectionInMiddleOfQuery() {
    // matches() では "SELECT 1" の後に UNION があってもフルマッチしないためスルーされた。
    // find() では部分一致するので SecurityException をスロー。
    assertThrows(
        SecurityException.class,
        () ->
            SqlQueryUtils.validateQuery(
                "SELECT * FROM users WHERE id = 1 UNION SELECT password FROM admin", logger),
        "UNION を含むクエリはブロックされるべき");
  }

  @Test
  @DisplayName("大文字小文字混在の UNION もブロックされる")
  void testUnionCaseInsensitive() {
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery("SELECT 1 uNiOn SELECT 2", logger));
  }

  @Test
  @DisplayName("改行をまたぐ UNION もブロックされる（DOTALL フラグ）")
  void testUnionAcrossNewline() {
    // DOTALL フラグがないと . が改行にマッチしない。
    // 旧パターン ".*union.*" は DOTALL なしで改行をまたぐ場合にマッチしなかった。
    // find() + DOTALL で確実に検出できる。
    String multilineQuery = "SELECT id FROM users\nUNION\nSELECT password FROM admin";
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery(multilineQuery, logger),
        "改行をまたぐ UNION もブロックされるべき");
  }

  @Test
  @DisplayName("DROP を含むクエリはブロックされる")
  void testDropInjection() {
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery("SELECT 1; DROP TABLE users", logger));
  }

  @Test
  @DisplayName("正常な SELECT クエリは通過する")
  void testValidSelectQuery() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery("SELECT name FROM users WHERE id = ?", logger));
  }

  @Test
  @DisplayName("正常な SELECT クエリに LIKE や ORDER BY が含まれても通過する")
  void testSelectWithLikeAndOrderBy() {
    assertDoesNotThrow(
        () ->
            SqlQueryUtils.validateQuery(
                "SELECT name FROM users WHERE name LIKE ? ORDER BY id", logger));
  }
}
