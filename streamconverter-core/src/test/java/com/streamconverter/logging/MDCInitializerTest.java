package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/** Unit tests for {@link MDCInitializer}. */
class MDCInitializerTest {

  private MDCAdapter originalAdapter;

  @BeforeEach
  void saveOriginalAdapter() throws Exception {
    originalAdapter = MDC.getMDCAdapter();
  }

  @AfterEach
  void restoreOriginalAdapter() throws Exception {
    setMdcAdapter(originalAdapter);
    MDC.clear();
  }

  private static void setMdcAdapter(MDCAdapter adapter) throws Exception {
    Field field = MDC.class.getDeclaredField("MDC_ADAPTER");
    field.setAccessible(true);
    field.set(null, adapter);
  }

  @Test
  void initialize_installsInheritableMDCAdapter() {
    MDCInitializer.initialize();
    assertInstanceOf(InheritableMDCAdapter.class, MDC.getMDCAdapter());
  }

  @Test
  void initialize_isIdempotent() {
    MDCInitializer.initialize();
    MDCAdapter first = MDC.getMDCAdapter();
    MDCInitializer.initialize();
    MDCAdapter second = MDC.getMDCAdapter();
    assertSame(first, second, "Second call should return without replacing the adapter");
  }

  @Test
  void initialize_migratesExistingContext() {
    // Set values in the current (original) adapter before initializing
    MDC.put("key1", "value1");
    MDC.put("key2", "value2");

    MDCInitializer.initialize();

    // Values set before initialization must survive in the new adapter
    assertEquals("value1", MDC.get("key1"));
    assertEquals("value2", MDC.get("key2"));
  }

  @Test
  void initialize_withNoExistingContext_doesNotThrow() {
    MDC.clear();
    assertDoesNotThrow(MDCInitializer::initialize);
    assertInstanceOf(InheritableMDCAdapter.class, MDC.getMDCAdapter());
  }

  @Test
  void initialize_preservesMapContents() {
    MDC.put("traceId", "TRACE-42");

    MDCInitializer.initialize();

    Map<String, String> ctx = MDC.getCopyOfContextMap();
    assertNotNull(ctx);
    assertEquals("TRACE-42", ctx.get("traceId"));
  }
}
