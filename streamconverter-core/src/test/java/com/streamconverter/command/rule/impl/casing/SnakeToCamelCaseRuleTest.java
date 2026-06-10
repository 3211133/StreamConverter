package com.streamconverter.command.rule.impl.casing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for SnakeToCamelCaseRule transformation logic. */
class SnakeToCamelCaseRuleTest {

  @Test
  void testDefaultConfiguration() {
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.create();

    assertEquals("userName", rule.apply("user_name"));
    assertEquals("firstName", rule.apply("first_name"));
    assertEquals("xmlHttpRequest", rule.apply("xml_http_request"));
  }

  @ParameterizedTest
  @CsvSource({
    "user_name, userName",
    "first_name, firstName",
    "user_account_id, userAccountId",
    "xml_http_request, xmlHttpRequest",
    "id, id",
    "a, a",
    "some_very_long_variable_name, someVeryLongVariableName"
  })
  void testBasicConversions(String input, String expected) {
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.create();
    assertEquals(expected, rule.apply(input));
  }

  @Test
  void testNullAndEmptyInputs() {
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.create();

    assertNull(rule.apply(null));
    assertEquals("", rule.apply(""));
  }

  @Test
  void testPascalCase() {
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.createPascalCase();

    assertEquals("UserName", rule.apply("user_name"));
  }

  @Test
  @Tag("known-bug") // #758
  @DisplayName("Preserve leading underscore when preserveUnderscores is enabled")
  void testPreserveLeadingUnderscore() {
    // Arrange - preserveUnderscores=true は先頭・末尾のアンダースコアを保持する設定
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.builder().preserveUnderscores(true).build();

    // Act & Assert - 先頭アンダースコアは保持されたまま camelCase 変換されるべき
    assertEquals("_userName", rule.apply("_user_name"));
  }

  @Test
  @Tag("known-bug") // #758
  @DisplayName("Preserve both leading and trailing underscores when preserveUnderscores is enabled")
  void testPreserveLeadingAndTrailingUnderscores() {
    // Arrange
    SnakeToCamelCaseRule rule = SnakeToCamelCaseRule.builder().preserveUnderscores(true).build();

    // Act & Assert - 先頭・末尾の両方が保持されたまま camelCase 変換されるべき
    assertEquals("_userName_", rule.apply("_user_name_"));
  }
}
