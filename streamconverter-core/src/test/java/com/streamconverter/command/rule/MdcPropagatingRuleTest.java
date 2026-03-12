package com.streamconverter.command.rule;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.context.PipelineContext;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
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

    MdcPropagatingRule rule = MdcPropagatingRule.create("orderId");
    String result = rule.apply("ORD-001");

    assertEquals("ORD-001", result);
    assertEquals("ORD-001", PipelineContext.getShared("orderId"));
    assertEquals("ORD-001", MDC.get("orderId"));
  }

  @Test
  void applyReturnsInputUnchanged() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    MdcPropagatingRule rule = MdcPropagatingRule.create("key");
    String input = "test-value";
    String result = rule.apply(input);

    assertSame(input, result);
  }

  @Test
  void applyWithNullInputThrowsException() {
    MdcPropagatingRule rule = MdcPropagatingRule.create("key");

    assertThrows(IllegalArgumentException.class, () -> rule.apply(null));
  }

  @Test
  @DisplayName("create_nullKeyThrows")
  void create_nullKeyThrows() {
    assertThrows(IllegalArgumentException.class, () -> MdcPropagatingRule.create(null));
  }

  @Test
  @DisplayName("create_emptyKeyThrows")
  void create_emptyKeyThrows() {
    assertThrows(IllegalArgumentException.class, () -> MdcPropagatingRule.create(""));
  }

  @Test
  @DisplayName("create_returnsRuleThatStoresValueInPipelineContext")
  void create_returnsRuleThatStoresValueInPipelineContext() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    MdcPropagatingRule rule = MdcPropagatingRule.create("userId");
    rule.apply("user-123");

    assertEquals("user-123", PipelineContext.getShared("userId"));
    assertEquals("user-123", MDC.get("userId"));
  }

  @Test
  void applyWithoutPipelineContextIsNoOp() {
    MdcPropagatingRule rule = MdcPropagatingRule.create("key");
    String result = rule.apply("value");

    assertEquals("value", result);
    assertNull(PipelineContext.getShared("key"));
  }

  @Test
  void toStringContainsKey() {
    MdcPropagatingRule rule = MdcPropagatingRule.create("orderId");
    assertEquals("MdcPropagatingRule{mdcKey='orderId'}", rule.toString());
  }

  @Test
  void equalsSameKey() {
    MdcPropagatingRule rule1 = MdcPropagatingRule.create("key");
    MdcPropagatingRule rule2 = MdcPropagatingRule.create("key");

    assertEquals(rule1, rule2);
    assertEquals(rule1.hashCode(), rule2.hashCode());
  }

  @Test
  void equalsDifferentKey() {
    MdcPropagatingRule rule1 = MdcPropagatingRule.create("key1");
    MdcPropagatingRule rule2 = MdcPropagatingRule.create("key2");

    assertNotEquals(rule1, rule2);
  }

  @Test
  @DisplayName("MdcPropagatingRuleはPipelineContext経由で別スレッドでもsyncToMDC後に値を取得できる")
  void mdcPropagatingRulePropagatesViaPipelineContextAcrossThreads() throws Exception {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    MdcPropagatingRule.create("orderId").apply("ORD-999");

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> mdcBeforeSync = new AtomicReference<>();
    AtomicReference<String> mdcAfterSync = new AtomicReference<>();

    Thread child =
        new Thread(
            () -> {
              try {
                // InheritableMDCAdapter がインストールされていないため、子スレッドの MDC は空
                mdcBeforeSync.set(MDC.get("orderId"));
                // 同一 ctx を子スレッドに設定して syncToMDC で反映
                PipelineContext.set(ctx);
                PipelineContext.syncToMDC();
                mdcAfterSync.set(MDC.get("orderId"));
                PipelineContext.clear();
                MDC.clear();
              } finally {
                latch.countDown();
              }
            });
    child.start();
    assertTrue(latch.await(5, TimeUnit.SECONDS), "Child thread did not complete in time");

    assertNull(mdcBeforeSync.get(), "InheritableMDCAdapter 未インストール時は子スレッドの MDC は空");
    assertEquals("ORD-999", mdcAfterSync.get(), "PipelineContext.syncToMDC() 後は MDC に反映される");
  }

  @Test
  @DisplayName("PipelineContext 未設定スレッドで MDC.put を直接呼んでも後続スレッドへ伝搬しない")
  void rawMdcPutDoesNotPersistInPipelineContext() {
    MDC.put("rawKey", "raw-value");

    assertNull(
        PipelineContext.getShared("rawKey"), "MDC.put だけでは PipelineContext.sharedValues に入らない");

    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    assertNull(
        PipelineContext.getShared("rawKey"),
        "後から PipelineContext を設定しても MDC.put の値は sharedValues に届かない");
  }
}
