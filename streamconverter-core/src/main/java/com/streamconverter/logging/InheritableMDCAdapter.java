package com.streamconverter.logging;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.spi.MDCAdapter;

/**
 * MDCAdapter implementation that propagates MDC context to child threads via
 * InheritableThreadLocal.
 *
 * <p>The standard {@link ch.qos.logback.classic.util.LogbackMDCAdapter} uses plain {@link
 * ThreadLocal}, which does not propagate to child threads. This implementation uses {@link
 * InheritableThreadLocal} so that virtual threads (and platform threads) spawned from a parent
 * automatically inherit a copy of the parent's MDC context map.
 *
 * <p>Use {@link MDCInitializer#initialize()} at application startup (before the first log call) to
 * install this adapter.
 */
public class InheritableMDCAdapter implements MDCAdapter {

  /** Constructs a new InheritableMDCAdapter. */
  public InheritableMDCAdapter() {}

  private final InheritableThreadLocal<Map<String, String>> tlm =
      new InheritableThreadLocal<>() {
        @Override
        protected Map<String, String> childValue(Map<String, String> parentValue) {
          return parentValue == null ? null : new HashMap<>(parentValue);
        }
      };

  private final InheritableThreadLocal<Map<String, Deque<String>>> tlmDeque =
      new InheritableThreadLocal<>() {
        @Override
        protected Map<String, Deque<String>> childValue(Map<String, Deque<String>> parentValue) {
          if (parentValue == null) return null;
          Map<String, Deque<String>> copy = new HashMap<>();
          for (Map.Entry<String, Deque<String>> entry : parentValue.entrySet()) {
            copy.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
          }
          return copy;
        }
      };

  @Override
  public void put(String key, String val) {
    if (key == null) throw new IllegalArgumentException("key cannot be null");
    Map<String, String> map = tlm.get();
    if (map == null) {
      map = new HashMap<>();
      tlm.set(map);
    }
    map.put(key, val);
  }

  @Override
  public String get(String key) {
    Map<String, String> map = tlm.get();
    return (map != null && key != null) ? map.get(key) : null;
  }

  @Override
  public void remove(String key) {
    Map<String, String> map = tlm.get();
    if (map != null) {
      map.remove(key);
      if (map.isEmpty()) {
        tlm.remove();
      }
    }
  }

  @Override
  public void clear() {
    tlm.remove();
    tlmDeque.remove();
  }

  @Override
  public Map<String, String> getCopyOfContextMap() {
    Map<String, String> map = tlm.get();
    return (map != null) ? new HashMap<>(map) : null;
  }

  @Override
  public void setContextMap(Map<String, String> contextMap) {
    if (contextMap == null) {
      tlm.remove();
    } else {
      tlm.set(new HashMap<>(contextMap));
    }
  }

  @Override
  public void pushByKey(String key, String value) {
    if (key == null) return;
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) {
      map = new HashMap<>();
      tlmDeque.set(map);
    }
    map.computeIfAbsent(key, k -> new ArrayDeque<>()).push(value);
  }

  @Override
  public String popByKey(String key) {
    if (key == null) return null;
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) return null;
    Deque<String> deque = map.get(key);
    return (deque != null) ? deque.pop() : null;
  }

  @Override
  public Deque<String> getCopyOfDequeByKey(String key) {
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) return null;
    Deque<String> deque = map.get(key);
    return (deque != null) ? new ArrayDeque<>(deque) : null;
  }

  @Override
  public void clearDequeByKey(String key) {
    if (key == null) return;
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) return;
    Deque<String> deque = map.get(key);
    if (deque != null) {
      deque.clear();
    }
  }
}
