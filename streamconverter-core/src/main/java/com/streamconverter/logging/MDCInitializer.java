package com.streamconverter.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * Installs {@link InheritableMDCAdapter} so that MDC context is automatically propagated to child
 * threads (including virtual threads) without manual copying.
 *
 * <p>Call {@link #initialize()} once at application startup, <em>before</em> the first log
 * statement, to replace the default Logback MDC adapter. After initialization, MDC values set in a
 * parent thread are automatically visible in any thread it spawns.
 *
 * <p>If {@link InheritableMDCAdapter} is not installed (i.e., {@link #initialize()} was not
 * called), MDC context will not propagate to child threads. Each thread will have an independent,
 * empty MDC context.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     MDCInitializer.initialize();
 *     // ... rest of application startup
 * }
 * }</pre>
 */
public final class MDCInitializer {

  private MDCInitializer() {}

  /**
   * Installs {@link InheritableMDCAdapter} into both the SLF4J {@link MDC} class and the Logback
   * {@code LoggerContext} (if Logback is present on the classpath).
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect after the
   * first call.
   *
   * @throws IllegalStateException if the SLF4J MDC adapter field cannot be replaced via reflection
   */
  public static synchronized void initialize() {
    if (MDC.getMDCAdapter() instanceof InheritableMDCAdapter) {
      return;
    }

    InheritableMDCAdapter adapter = new InheritableMDCAdapter();

    // Replace the MDC_ADAPTER field in SLF4J MDC via reflection
    // (MDC.setMDCAdapter is package-private in SLF4J 2.x)
    try {
      Field mdcAdapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
      mdcAdapterField.setAccessible(true);
      mdcAdapterField.set(null, (MDCAdapter) adapter);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException("Failed to install InheritableMDCAdapter into SLF4J MDC", e);
    }

    // Install into Logback LoggerContext via reflection (avoids hard compile-time dependency)
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (loggerContextClass.isInstance(factory)) {
        Method setMDCAdapter = loggerContextClass.getMethod("setMDCAdapter", MDCAdapter.class);
        setMDCAdapter.invoke(factory, adapter);
      }
    } catch (ClassNotFoundException ignored) {
      // Logback is not on the classpath; nothing to do
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
          "Failed to install InheritableMDCAdapter into Logback LoggerContext", e);
    }
  }
}
