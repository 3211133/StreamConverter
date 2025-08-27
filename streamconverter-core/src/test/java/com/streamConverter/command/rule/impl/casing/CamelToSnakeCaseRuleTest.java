package com.streamConverter.command.rule.impl.casing;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Tests for CamelToSnakeCaseRule transformation logic. */
class CamelToSnakeCaseRuleTest {

  @Test
  void testDefaultConfiguration() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.create();

    assertEquals("user_name", rule.apply("userName"));
    assertEquals("first_name", rule.apply("firstName"));
    assertEquals("xml_http_request", rule.apply("XMLHttpRequest"));
  }

  @ParameterizedTest
  @CsvSource({
    "userName, user_name",
    "firstName, first_name",
    "userAccountID, user_account_id",
    "XMLHttpRequest, xml_http_request",
    "ID, id",
    "HTTPSConnection, https_connection",
    "camelCase, camel_case",
    "PascalCase, pascal_case",
    "a, a",
    "A, a",
    "someVeryLongVariableName, some_very_long_variable_name"
  })
  void testBasicConversions(String input, String expected) {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.create();
    assertEquals(expected, rule.apply(input));
  }

  @Test
  void testNullAndEmptyInputs() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.create();

    assertNull(rule.apply(null));
    assertEquals("", rule.apply(""));
  }

  @Test
  void testPreserveUnderscores() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.builder().preserveUnderscores(true).build();

    assertEquals("_user_name_", rule.apply("_userName_"));
    assertEquals("user__name", rule.apply("user_Name"));
  }

  @Test
  void testCleanUpUnderscores() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.builder().preserveUnderscores(false).build();

    assertEquals("user_name", rule.apply("_userName_"));
    assertEquals("user_name", rule.apply("user__Name"));
    assertEquals("user_name", rule.apply("___user___Name___"));
  }

  @Test
  void testAcronymHandling() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.builder().handleAcronyms(true).build();

    assertEquals("xml_http_request", rule.apply("XMLHttpRequest"));
    assertEquals("json_api_response", rule.apply("JSONAPIResponse"));
    assertEquals("url_parser", rule.apply("URLParser"));
  }

  @Test
  void testNoAcronymHandling() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.builder().handleAcronyms(false).build();

    assertEquals("x_m_l_http_request", rule.apply("XMLHttpRequest"));
    assertEquals("j_s_o_n_a_p_i_response", rule.apply("JSONAPIResponse"));
  }

  @Test
  void testBuilderConfiguration() {
    CamelToSnakeCaseRule rule =
        CamelToSnakeCaseRule.builder().preserveUnderscores(true).handleAcronyms(false).build();

    assertEquals("_x_m_l_http_request_", rule.apply("_XMLHttpRequest_"));
  }

  @Test
  void testToString() {
    CamelToSnakeCaseRule rule =
        CamelToSnakeCaseRule.builder().preserveUnderscores(true).handleAcronyms(false).build();

    String str = rule.toString();
    assertTrue(str.contains("CamelToSnakeCaseRule"));
    assertTrue(str.contains("preserveUnderscores=true"));
    assertTrue(str.contains("handleAcronyms=false"));
  }

  @Test
  void testEdgeCases() {
    CamelToSnakeCaseRule rule = CamelToSnakeCaseRule.create();

    // Numbers
    assertEquals("user123", rule.apply("user123"));
    assertEquals("user123_name", rule.apply("user123Name"));

    // Special characters
    assertEquals("user@name", rule.apply("user@Name"));
    assertEquals("user-name", rule.apply("user-Name"));

    // Already snake_case
    assertEquals("already_snake_case", rule.apply("already_snake_case"));
  }
}
