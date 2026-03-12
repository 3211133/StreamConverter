package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/** Unit tests for {@link MDCInitializer}. */
class MDCInitializerTest {

  private MDCAdapter originalSlf4jAdapter;
  private MDCAdapter originalLogbackAdapter;

  @BeforeEach
  void saveAdapters() throws Exception {
    originalSlf4jAdapter = MDC.getMDCAdapter();
    originalLogbackAdapter = getLogbackAdapter();
  }

  @AfterEach
  void restoreAdapters() throws Exception {
    setSlf4jAdapter(originalSlf4jAdapter);
    setLogbackAdapter(originalLogbackAdapter);
    MDC.clear();
  }

  // --- reflection helpers ---

  private static void setSlf4jAdapter(MDCAdapter adapter) throws Exception {
    Field field = MDC.class.getDeclaredField("MDC_ADAPTER");
    field.setAccessible(true);
    field.set(null, adapter);
  }

  private static MDCAdapter getLogbackAdapter() {
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (loggerContextClass.isInstance(factory)) {
        Method get = loggerContextClass.getMethod("getMDCAdapter");
        return (MDCAdapter) get.invoke(factory);
      }
    } catch (Exception ignored) {
      // Logback not on classpath
    }
    return null;
  }

  private static void setLogbackAdapter(MDCAdapter adapter) {
    if (adapter == null) return;
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (loggerContextClass.isInstance(factory)) {
        Method set = loggerContextClass.getMethod("setMDCAdapter", MDCAdapter.class);
        set.invoke(factory, adapter);
      }
    } catch (Exception ignored) {
      // Logback not on classpath
    }
  }

  // --- tests ---

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
    assertSame(first, second, "Second call should not replace the adapter");
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

  @Test
  void initialize_alsoUpdatesLogbackLoggerContext() {
    MDCInitializer.initialize();
    MDCAdapter logbackAdapter = getLogbackAdapter();
    if (logbackAdapter == null) {
      return; // Logback not on classpath — skip
    }
    assertInstanceOf(
        InheritableMDCAdapter.class,
        logbackAdapter,
        "Logback LoggerContext should also use InheritableMDCAdapter");
  }
}
