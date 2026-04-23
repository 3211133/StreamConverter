package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@DisplayName("SqlQueryUtils.validateQuery のテスト")
class SqlQueryUtilsTest {

  private static final Logger logger = LoggerFactory.getLogger(SqlQueryUtilsTest.class);

  @Test
  @DisplayName("空文字列は IllegalArgumentException")
  void testEmptyQueryRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> SqlQueryUtils.validateQuery("", logger));
  }

  @Test
  @DisplayName("SELECT 以外のクエリは SecurityException")
  void testNonSelectRejected() {
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery("INSERT INTO users VALUES (1, 'x')", logger));
  }

  @Test
  @DisplayName("セミコロンによる複数文は SecurityException")
  void testMultipleStatementRejected() {
    assertThrows(
        SecurityException.class,
        () -> SqlQueryUtils.validateQuery("SELECT 1; DROP TABLE users", logger));
  }

  @Test
  @DisplayName("正常な SELECT クエリは通過する")
  void testValidSelectPasses() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery("SELECT name FROM users WHERE id = ?", logger));
  }

  @Test
  @DisplayName("updates_count カラムを含むクエリは誤検知されない")
  void testUpdatesCountColumnPasses() {
    assertDoesNotThrow(
        () -> SqlQueryUtils.validateQuery(
            "SELECT updates_count, reunion_id FROM events WHERE id = ?", logger));
  }
}
