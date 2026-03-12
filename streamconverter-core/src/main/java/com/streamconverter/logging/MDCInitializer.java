package com.streamconverter.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
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

  private MDCInitializer() {}

  /**
   * Installs {@link InheritableMDCAdapter} into both the SLF4J {@link MDC} class and the Logback
   * {@code LoggerContext} (if Logback is present on the classpath).
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect after the
   * first call. It is also thread-safe; concurrent calls are serialized.
   *
   * <p><strong>Note:</strong> Any MDC values already set in the current thread at the time of this
   * call are migrated into the new adapter so that they are not lost. However, values set in other
   * threads are not migrated. For best results, call this method at application startup before any
   * MDC values are set.
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

    // Capture existing MDC state BEFORE replacing the adapter so it can be migrated
    Map<String, String> existingContext = MDC.getCopyOfContextMap();

    // Reuse an already-installed adapter if SLF4J side is ready; otherwise create a new one
    InheritableMDCAdapter adapter =
        slf4jReady ? (InheritableMDCAdapter) currentAdapter : new InheritableMDCAdapter();

    // Replace the MDC_ADAPTER field in SLF4J MDC via reflection
    // (MDC.setMDCAdapter is package-private in SLF4J 2.x)
    if (!slf4jReady) {
      try {
        Field mdcAdapterField = MDC.class.getDeclaredField("MDC_ADAPTER");
        mdcAdapterField.setAccessible(true);
        mdcAdapterField.set(null, (MDCAdapter) adapter);
      } catch (ReflectiveOperationException | RuntimeException e) {
        throw new IllegalStateException(
            "Failed to install InheritableMDCAdapter into SLF4J MDC", e);
      }
    }

    // Install into Logback LoggerContext via reflection (avoids hard compile-time dependency)
    if (!logbackReady) {
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

    // Migrate existing MDC state from the previous adapter into the new one
    if (!slf4jReady && existingContext != null) {
      adapter.setContextMap(existingContext);
    }
  }

  /**
   * Returns {@code true} if the Logback {@code LoggerContext} already has an {@link
   * InheritableMDCAdapter} installed, or if Logback is not on the classpath.
   */
  private static boolean isLogbackAlreadyInstalled(MDCAdapter currentSlf4jAdapter) {
    try {
      Class<?> loggerContextClass = Class.forName("ch.qos.logback.classic.LoggerContext");
      ILoggerFactory factory = LoggerFactory.getILoggerFactory();
      if (!loggerContextClass.isInstance(factory)) {
        return true; // not Logback — nothing to install
      }
      Method getMDCAdapter = loggerContextClass.getMethod("getMDCAdapter");
      MDCAdapter logbackAdapter = (MDCAdapter) getMDCAdapter.invoke(factory);
      // Both sides must use the same InheritableMDCAdapter instance
      return logbackAdapter instanceof InheritableMDCAdapter
          && logbackAdapter == currentSlf4jAdapter;
    } catch (ClassNotFoundException ignored) {
      return true; // Logback not on classpath
    } catch (ReflectiveOperationException | RuntimeException ignored) {
      return false;
    }
  }
}
