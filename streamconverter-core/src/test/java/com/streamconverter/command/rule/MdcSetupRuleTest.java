package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.logging.MDCContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Tests for MdcSetupRule that sets extracted values to MDCContext. */
class MdcSetupRuleTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
    MDCContext.clear();
    MDCContext.clearShared();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
    MDCContext.clear();
    MDCContext.clearShared();
  }

  @Test
  void testMdcSetupRuleSetsValueToMDCContext() {
    MdcSetupRule rule = new MdcSetupRule("userId");

    // Ruleを適用
    String result = rule.apply("USER12345");

    // MDCContextに値が設定されていることを確認
    assertEquals("USER12345", MDCContext.get().get("userId"));

    // 返り値は変更されずそのまま返される
    assertEquals("USER12345", result);
  }

  @Test
  void testMdcSetupRuleReturnsValueUnchanged() {
    MdcSetupRule rule = new MdcSetupRule("requestId");

    String input = "REQ-98765";
    String result = rule.apply(input);

    // 値は変更されずそのまま返される
    assertEquals(input, result);
  }

  @Test
  void testMdcSetupRuleWithNullValue() {
    MdcSetupRule rule = new MdcSetupRule("optional");

    // 初期値を設定
    rule.apply("initialValue");
    assertEquals("initialValue", MDCContext.get().get("optional"));

    // null値を適用
    String result = rule.apply(null);

    // MDCContextからは削除される
    assertNull(MDCContext.get().get("optional"));

    // 返り値もnull
    assertNull(result);
  }

  @Test
  void testMdcSetupRuleOverwritesValue() {
    MdcSetupRule rule = new MdcSetupRule("status");

    // 初回設定
    rule.apply("initial");
    assertEquals("initial", MDCContext.get().get("status"));

    // 値を上書き
    rule.apply("updated");
    assertEquals("updated", MDCContext.get().get("status"));
  }

  @Test
  void testMdcSetupRuleWithEmptyString() {
    MdcSetupRule rule = new MdcSetupRule("emptyField");

    String result = rule.apply("");

    // 空文字列も設定される
    assertEquals("", MDCContext.get().get("emptyField"));
    assertEquals("", result);
  }

  @Test
  void testMdcSetupRuleConstructorValidation() {
    // nullキーは例外
    assertThrows(NullPointerException.class, () -> new MdcSetupRule(null));
  }

  @Test
  void testMultipleMdcSetupRulesWithDifferentKeys() {
    MdcSetupRule userIdRule = new MdcSetupRule("userId");
    MdcSetupRule requestIdRule = new MdcSetupRule("requestId");

    // それぞれのRuleで値を設定
    userIdRule.apply("USER123");
    requestIdRule.apply("REQ-456");

    // 両方ともMDCContextに設定されている
    assertEquals("USER123", MDCContext.get().get("userId"));
    assertEquals("REQ-456", MDCContext.get().get("requestId"));
  }
}
