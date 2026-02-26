package com.streamconverter.context;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class PipelineContextTest {

  @AfterEach
  void cleanup() {
    PipelineContext.clear();
    MDC.clear();
  }

  @Test
  void putSharedAndGetShared() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    PipelineContext.putShared("orderId", "ORD-001");

    assertEquals("ORD-001", PipelineContext.getShared("orderId"));
    assertEquals("ORD-001", MDC.get("orderId"));
  }

  @Test
  void putSharedWithNullValueRemovesKey() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    PipelineContext.putShared("key", "value");
    assertEquals("value", PipelineContext.getShared("key"));

    PipelineContext.putShared("key", null);
    assertNull(PipelineContext.getShared("key"));
    assertNull(MDC.get("key"));
  }

  @Test
  void putSharedWithNullKeyThrowsException() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    assertThrows(IllegalArgumentException.class, () -> PipelineContext.putShared(null, "value"));
  }

  @Test
  void putSharedWithoutContextIsNoOp() {
    // PipelineContext未設定時はNPEにならずに何もしない
    assertDoesNotThrow(() -> PipelineContext.putShared("key", "value"));
    assertNull(PipelineContext.getShared("key"));
  }

  @Test
  void getSharedWithoutContextReturnsNull() {
    assertNull(PipelineContext.getShared("anyKey"));
  }

  @Test
  void syncToMDC() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    PipelineContext.putShared("key1", "val1");
    PipelineContext.putShared("key2", "val2");

    // MDCをクリアしてsyncToMDCで復元されることを確認
    MDC.clear();
    assertNull(MDC.get("key1"));

    PipelineContext.syncToMDC();

    assertEquals("val1", MDC.get("key1"));
    assertEquals("val2", MDC.get("key2"));
  }

  @Test
  void syncToMDCWithoutContextIsNoOp() {
    assertDoesNotThrow(PipelineContext::syncToMDC);
  }

  @Test
  void sharedValuesPropagateAcrossThreads() throws Exception {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    PipelineContext.putShared("threadTest", "shared-value");

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> otherThreadValue = new AtomicReference<>();

    Thread.ofVirtual()
        .start(
            () -> {
              PipelineContext.set(ctx); // 同一インスタンスを設定
              PipelineContext.syncToMDC();
              otherThreadValue.set(MDC.get("threadTest"));
              PipelineContext.clear();
              MDC.clear();
              latch.countDown();
            });

    latch.await();
    assertEquals("shared-value", otherThreadValue.get());
  }

  @Test
  void clearRemovesContextFromThread() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    PipelineContext.putShared("key", "val");

    PipelineContext.clear();

    assertNull(PipelineContext.getShared("key"));
    // 共有値自体はPipelineContextインスタンスに残っている（他スレッドから参照可能）
    assertEquals("val", ctx.getSharedValues().get("key"));
  }
}
