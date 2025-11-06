package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.context.ExecutionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Tests for MdcSetupRule that sets extracted values to shared context. */
class MdcSetupRuleTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testMdcSetupRuleSetsValueToSharedContext() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "userId");

    // Ruleを適用
    String result = rule.apply("USER12345");

    // 共有コンテキストに値が設定されていることを確認
    assertEquals("USER12345", context.getSharedContext("userId"));

    // 返り値は変更されずそのまま返される
    assertEquals("USER12345", result);
  }

  @Test
  void testMdcSetupRuleReturnsValueUnchanged() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "requestId");

    String input = "REQ-98765";
    String result = rule.apply(input);

    // 値は変更されずそのまま返される
    assertEquals(input, result);
  }

  @Test
  void testMdcSetupRuleWithNullValue() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "optional");

    // null値を適用
    String result = rule.apply(null);

    // 共有コンテキストからは削除される
    assertNull(context.getSharedContext("optional"));

    // 返り値もnull
    assertNull(result);
  }

  @Test
  void testMdcSetupRuleOverwritesValue() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "status");

    // 初回設定
    rule.apply("initial");
    assertEquals("initial", context.getSharedContext("status"));

    // 値を上書き
    rule.apply("updated");
    assertEquals("updated", context.getSharedContext("status"));
  }

  @Test
  void testMdcSetupRuleWithEmptyString() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "emptyField");

    String result = rule.apply("");

    // 空文字列も設定される
    assertEquals("", context.getSharedContext("emptyField"));
    assertEquals("", result);
  }

  @Test
  void testMdcSetupRuleConstructorValidation() {
    ExecutionContext context = ExecutionContext.create();

    // nullコンテキストは例外
    assertThrows(NullPointerException.class, () -> new MdcSetupRule(null, "key"));

    // nullキーは例外
    assertThrows(NullPointerException.class, () -> new MdcSetupRule(context, null));
  }

  @Test
  void testMdcSetupRuleIntegrationWithApplyToMDC() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule rule = new MdcSetupRule(context, "userId");

    // 値を設定
    rule.apply("USER99999");

    // applyToMDC()を呼ぶとMDCに反映される
    context.applyToMDC();

    // MDCに値が反映されている
    assertEquals("USER99999", MDC.get("userId"));
  }

  @Test
  void testMultipleMdcSetupRulesWithDifferentKeys() {
    ExecutionContext context = ExecutionContext.create();
    MdcSetupRule userIdRule = new MdcSetupRule(context, "userId");
    MdcSetupRule requestIdRule = new MdcSetupRule(context, "requestId");

    // それぞれのRuleで値を設定
    userIdRule.apply("USER123");
    requestIdRule.apply("REQ-456");

    // 両方とも共有コンテキストに設定されている
    assertEquals("USER123", context.getSharedContext("userId"));
    assertEquals("REQ-456", context.getSharedContext("requestId"));

    // applyToMDC()で両方ともMDCに反映される
    context.applyToMDC();
    assertEquals("USER123", MDC.get("userId"));
    assertEquals("REQ-456", MDC.get("requestId"));
  }
}
