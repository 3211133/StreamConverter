package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.context.PipelineContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class MdcPropagatingRuleTest {

  @AfterEach
  void cleanup() {
    PipelineContext.clear();
    MDC.clear();
  }

  @Test
  void applyPutsValueIntoSharedContext() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    MdcPropagatingRule rule = new MdcPropagatingRule("orderId");
    String result = rule.apply("ORD-001");

    assertEquals("ORD-001", result);
    assertEquals("ORD-001", PipelineContext.getShared("orderId"));
    assertEquals("ORD-001", MDC.get("orderId"));
  }

  @Test
  void applyReturnsInputUnchanged() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    MdcPropagatingRule rule = new MdcPropagatingRule("key");
    String input = "test-value";
    String result = rule.apply(input);

    assertSame(input, result);
  }

  @Test
  void applyWithNullInputThrowsException() {
    MdcPropagatingRule rule = new MdcPropagatingRule("key");

    assertThrows(IllegalArgumentException.class, () -> rule.apply(null));
  }

  @Test
  void constructorWithNullKeyThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> new MdcPropagatingRule(null));
  }

  @Test
  void applyWithoutPipelineContextIsNoOp() {
    // PipelineContext未設定時でもapplyは例外なく動作する
    MdcPropagatingRule rule = new MdcPropagatingRule("key");
    String result = rule.apply("value");

    assertEquals("value", result);
    assertNull(PipelineContext.getShared("key"));
  }

  @Test
  void toStringContainsKey() {
    MdcPropagatingRule rule = new MdcPropagatingRule("orderId");
    assertEquals("MdcPropagatingRule{mdcKey='orderId'}", rule.toString());
  }

  @Test
  void equalsSameKey() {
    MdcPropagatingRule rule1 = new MdcPropagatingRule("key");
    MdcPropagatingRule rule2 = new MdcPropagatingRule("key");

    assertEquals(rule1, rule2);
    assertEquals(rule1.hashCode(), rule2.hashCode());
  }

  @Test
  void equalsDifferentKey() {
    MdcPropagatingRule rule1 = new MdcPropagatingRule("key1");
    MdcPropagatingRule rule2 = new MdcPropagatingRule("key2");

    assertNotEquals(rule1, rule2);
  }
}
