package com.streamconverter.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
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
 * <p><strong>Important:</strong> {@code InheritableMDCAdapter} is backed by {@link
 * java.lang.InheritableThreadLocal}. In environments that reuse threads (for example, servlet
 * containers or {@link java.util.concurrent.ExecutorService} thread pools), MDC values inherited
 * for one logical request can unintentionally be visible to a subsequent, unrelated request running
 * on the same thread unless MDC is reliably cleared. This can lead to log correlation IDs, user
 * identifiers, or other contextual data "leaking" across requests.
 *
 * <p>When using {@link MDCInitializer} together with thread pools, ensure that each task clears MDC
 * in a {@code finally} block (for example, by calling {@link MDC#clear()} or removing the keys it
 * set), or wrap submitted {@link Runnable}/{@link java.util.concurrent.Callable} instances so that
 * MDC is captured, applied for the task execution, and then cleared afterwards.
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

  private static final Logger LOGGER = LoggerFactory.getLogger(MDCInitializer.class);

  private MDCInitializer() {}

  /**
   * Installs {@link InheritableMDCAdapter} into both the SLF4J {@link MDC} class and the Logback
   * {@code LoggerContext} (if Logback is present on the classpath).
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect after the
   * first call. It is also thread-safe; concurrent calls are serialized.
   *
   * <p><strong>Note:</strong> The key-value map of the current thread's MDC context is migrated
   * into the new adapter. However, deque-based state ({@code pushByKey}/{@code popByKey}) and
   * values set in other threads are not migrated. For best results, call this method at application
   * startup before any MDC values are set.
   *
   * @throws IllegalStateException if the adapter cannot be installed via reflection
   */
  public static synchronized void initialize() {
    MDCAdapter currentAdapter = MDC.getMDCAdapter();
    boolean slf4jReady = currentAdapter instanceof InheritableMDCAdapter;
    boolean logbackReady = isLogbackAlreadyInstalled(currentAdapter);

    if (slf4jReady && logbackReady) {
      return;
    }

    Map<String, String> existingContext = MDC.getCopyOfContextMap();
    InheritableMDCAdapter adapter =
        slf4jReady ? (InheritableMDCAdapter) currentAdapter : new InheritableMDCAdapter();

    if (!slf4jReady) {
      installIntoSlf4j(adapter);
    }
    if (!logbackReady) {
      installIntoLogback(adapter);
    }
    if (!slf4jReady && existingContext != null) {
      adapter.setContextMap(existingContext);
    }
  }

  /**
   * Installs the adapter into the SLF4J {@link MDC} class via reflection.
   *
   * <p>{@code MDC.setMDCAdapter()} is package-private in SLF4J 2.x, so reflection is the only way
   * to replace the adapter from outside the {@code org.slf4j} package.
   */
  @SuppressWarnings({"PMD.AvoidAccessibilityAlteration", "PMD.AvoidCatchingGenericException"})
  private static void installIntoSlf4j(InheritableMDCAdapter adapter) {
    // MDC.setMDCAdapter() is package-private in SLF4J 2.x; reflection is the only available path.
    // RuntimeException は Field.set() が SecurityException 以外をスローする可能性に備えた catch。
    try {
      Field mdcAdapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
      mdcAdapterField.setAccessible(true);
      mdcAdapterField.set(null, (MDCAdapter) adapter);
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new IllegalStateException("Failed to install InheritableMDCAdapter into SLF4J MDC", e);
    }
  }

  /**
   * Installs the adapter into the Logback {@code LoggerContext} via reflection, if Logback is
   * present on the classpath.
   */
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private static void installIntoLogback(InheritableMDCAdapter adapter) {
    // ReflectiveOperationException subtypes vary by JDK version; Exception covers all cases.
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (loggerContextClass.isInstance(factory)) {
        Method setMDCAdapter = loggerContextClass.getMethod("setMDCAdapter", MDCAdapter.class);
        setMDCAdapter.invoke(factory, adapter);
      }
    } catch (ClassNotFoundException ignored) {
      // Logback is not on the classpath; nothing to do
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new IllegalStateException(
          "Failed to install InheritableMDCAdapter into Logback LoggerContext", e);
    }
  }

  /**
   * Returns {@code true} if the Logback {@code LoggerContext} already has an {@link
   * InheritableMDCAdapter} installed, or if Logback is not on the classpath.
   */
  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.CompareObjectsWithEquals"})
  private static boolean isLogbackAlreadyInstalled(MDCAdapter currentSlf4jAdapter) {
    // ReflectiveOperationException subtypes vary by JDK version; Exception covers all cases.
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (!loggerContextClass.isInstance(factory)) {
        return true; // not Logback — nothing to install
      }
      Method getMDCAdapter = loggerContextClass.getMethod("getMDCAdapter");
      MDCAdapter logbackAdapter = (MDCAdapter) getMDCAdapter.invoke(factory);
      // == checks instance identity: SLF4J and Logback must share the exact same adapter object,
      // not just equal values. equals() would incorrectly return true for distinct instances.
      return logbackAdapter instanceof InheritableMDCAdapter
          && logbackAdapter == currentSlf4jAdapter;
    } catch (ClassNotFoundException ignored) {
      return true; // Logback not on classpath
    } catch (ReflectiveOperationException | RuntimeException e) {
      LOGGER.warn("Failed to check Logback MDC adapter; will attempt reinstall", e);
      return false;
    }
  }
}
