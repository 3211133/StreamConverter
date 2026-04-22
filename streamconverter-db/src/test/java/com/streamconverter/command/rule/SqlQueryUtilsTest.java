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

  // ---- #651: matches() は . が改行にマッチしないため改行をまたぐ UNION をスルーする ----

  @Test
  @DisplayName("改行をまたぐ UNION はブロックされる（旧 matches() では通過していたケース）")
  void testUnionAcrossNewline() {
    // DOTALL なしの .* は改行にマッチしないため、matches() では検出できなかった。
    // find() への変更後は正しく SecurityException をスローする。
    String multilineAttack = "SELECT id FROM users\nUNION\nSELECT password FROM admin";
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery(multilineAttack, logger),
        "改行をまたぐ UNION もブロックされるべき");
  }

  @Test
  @DisplayName("改行 + コメント混入の UNION もブロックされる")
  void testUnionWithNewlineAndComment() {
    String attack = "SELECT id FROM users\n-- コメント\nUNION SELECT password FROM admin";
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery(attack, logger),
        "改行とコメントを含む UNION もブロックされるべき");
  }

  // ---- 正常系（修正後も通過することの確認） ----

  @Test
  @DisplayName("正常な SELECT クエリは通過する")
  void testValidSelectQuery() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery("SELECT name FROM users WHERE id = ?", logger));
  }

  @Test
  @DisplayName("LIKE や ORDER BY を含む正常クエリも通過する")
  void testSelectWithLikeAndOrderBy() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery(
            "SELECT name FROM users WHERE name LIKE ? ORDER BY id", logger));
  }

  @Test
  @DisplayName("カラム名に update/union 等を含む正常クエリは誤検知されない（\\b 単語境界）")
  void testColumnNamesContainingKeywordsAreNotFalsePositives() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery(
            "SELECT updates_count, reunion_id FROM events WHERE id = ?", logger),
        "updates_count や reunion_id は単語境界でキーワードと区別されるべき");
  }
}
