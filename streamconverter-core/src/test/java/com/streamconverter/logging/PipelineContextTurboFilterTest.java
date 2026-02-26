package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.spi.FilterReply;
import com.streamconverter.context.PipelineContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class PipelineContextTurboFilterTest {

  private final PipelineContextTurboFilter filter = new PipelineContextTurboFilter();

  @AfterEach
  void cleanup() {
    PipelineContext.clear();
    MDC.clear();
  }

  @Test
  void decideWithContextSyncsSharedValuesToMDC() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);
    PipelineContext.putShared("orderId", "ORD-123");

    // MDCをクリアしてTurboFilter経由でsyncされることを確認
    MDC.clear();
    assertNull(MDC.get("orderId"));

    FilterReply reply = filter.decide(null, null, Level.INFO, "msg", null, null);

    assertEquals(FilterReply.NEUTRAL, reply);
    assertEquals("ORD-123", MDC.get("orderId"));
  }

  @Test
  void decideWithoutContextReturnsNeutral() {
    // PipelineContext未設定でも例外なくNEUTRALを返す
    FilterReply reply = filter.decide(null, null, Level.INFO, "msg", null, null);

    assertEquals(FilterReply.NEUTRAL, reply);
  }

  @Test
  void decideWithEmptyContextReturnsNeutral() {
    PipelineContext ctx = new PipelineContext();
    PipelineContext.set(ctx);

    FilterReply reply = filter.decide(null, null, Level.DEBUG, "msg", null, null);

    assertEquals(FilterReply.NEUTRAL, reply);
  }
}
