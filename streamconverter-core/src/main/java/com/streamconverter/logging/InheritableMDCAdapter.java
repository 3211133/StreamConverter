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
 *
 * <p><b>Thread safety:</b> Each thread holds its own independent copy of the context map via {@link
 * InheritableThreadLocal}. Child threads receive a shallow copy (snapshot) of the parent's map at
 * creation time; subsequent changes in the parent are not reflected in already-running children,
 * and vice versa. Individual per-thread maps are not synchronized, which is safe as long as each
 * map is accessed only by its owning thread — which is the normal MDC usage pattern.
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

  /**
   * 現在のスレッドのMDCコンテキストにキーと値を設定する。
   *
   * <p>このメソッド呼び出し後に起動された子スレッドは、このエントリを自動継承する （{@link InheritableThreadLocal}
   * による）。ただし、既に起動済みの子スレッドには反映されない。
   *
   * @param key MDCキー名。nullの場合は {@link IllegalArgumentException} をスロー
   * @param val 設定する値
   * @throws IllegalArgumentException keyがnullの場合
   */
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

  /**
   * 現在のスレッドのMDCコンテキストから指定キーの値を取得する。
   *
   * @param key MDCキー名
   * @return 設定されている値。キーが存在しないかkeyがnullの場合はnull
   */
  @Override
  public String get(String key) {
    Map<String, String> map = tlm.get();
    return (map != null && key != null) ? map.get(key) : null;
  }

  /**
   * 現在のスレッドのMDCコンテキストから指定キーを削除する。
   *
   * <p>削除後にコンテキストが空になった場合は {@link InheritableThreadLocal} 自体を解放する。 Dequeスタック（{@link
   * #pushByKey}/{@link #popByKey}）には影響しない。 Dequeを含む全エントリのクリアには {@link #clear()} を使用すること。
   *
   * @param key 削除するMDCキー名
   */
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

  /**
   * 現在のスレッドのMDCコンテキスト（マップおよびDeque）をすべてクリアする。
   *
   * <p>{@link InheritableThreadLocal} のエントリを解放するため、このメソッド呼び出し後に 生成される子スレッドにはコンテキストが継承されない。
   * なお、既に起動済みの子スレッドが保持するコピーには影響しない。
   */
  @Override
  public void clear() {
    tlm.remove();
    tlmDeque.remove();
  }

  /**
   * 現在のスレッドのMDCコンテキストのコピーを返す。
   *
   * <p>返されたマップはスナップショットであり、以降の変更は反映されない。
   *
   * @return MDCコンテキストのコピー。コンテキストが未設定の場合はnull
   */
  @Override
  public Map<String, String> getCopyOfContextMap() {
    Map<String, String> map = tlm.get();
    return (map != null) ? new HashMap<>(map) : null;
  }

  /**
   * 現在のスレッドのMDCコンテキストを指定されたマップで置き換える。
   *
   * <p>nullを渡すとコンテキストをクリアする。子スレッドへの継承も更新後の内容になる。
   *
   * @param contextMap 新しいMDCコンテキスト。nullの場合はクリア
   */
  @Override
  public void setContextMap(Map<String, String> contextMap) {
    if (contextMap == null) {
      tlm.remove();
    } else {
      tlm.set(new HashMap<>(contextMap));
    }
  }

  /**
   * 指定キーのDequeスタックに値をプッシュする。
   *
   * <p>keyがnullの場合は何もしない。
   *
   * @param key Dequeのキー名
   * @param value プッシュする値
   */
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

  /**
   * 指定キーのDequeスタックから先頭の値をポップして返す。
   *
   * <p>keyがnullまたはDequeが存在しない場合はnullを返す。
   *
   * @param key Dequeのキー名
   * @return ポップした値。キーが存在しないかkeyがnullの場合はnull
   */
  @Override
  public String popByKey(String key) {
    if (key == null) return null;
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) return null;
    Deque<String> deque = map.get(key);
    return (deque != null) ? deque.pop() : null;
  }

  /**
   * 指定キーのDequeのコピーを返す。
   *
   * <p>返されたDequeはスナップショットであり、以降の変更は反映されない。
   *
   * @param key Dequeのキー名
   * @return Dequeのコピー。キーが存在しない場合はnull
   */
  @Override
  public Deque<String> getCopyOfDequeByKey(String key) {
    Map<String, Deque<String>> map = tlmDeque.get();
    if (map == null) return null;
    Deque<String> deque = map.get(key);
    return (deque != null) ? new ArrayDeque<>(deque) : null;
  }

  /**
   * 指定キーのDequeの全要素をクリアする。
   *
   * <p>keyがnullまたはDequeが存在しない場合は何もしない。
   *
   * @param key クリアするDequeのキー名
   */
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
