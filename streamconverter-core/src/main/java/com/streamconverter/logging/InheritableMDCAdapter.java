package com.streamconverter.logging;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.spi.MDCAdapter;

/**
 * InheritableThreadLocalを使用したMDCAdapter実装
 *
 * <p>このアダプターは、親スレッドのMDC値を子スレッド（Platform Thread、Virtual Thread共に）に自動的に継承します。
 * LogbackのデフォルトMDCAdapterは通常のThreadLocalを使用するため、子スレッドにMDC値が伝播しませんが、
 * このアダプターはInheritableThreadLocalを使用することで、親スレッドの値を子スレッドに引き継ぎます。
 *
 * <p><b>使用方法:</b>
 *
 * <pre>{@code
 * // LoggerContext取得
 * LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
 *
 * // カスタムMDCAdapterを設定
 * loggerContext.setMDCAdapter(new InheritableMDCAdapter());
 * }</pre>
 *
 * <p><b>注意事項:</b>
 *
 * <ul>
 *   <li>この設定は、アプリケーション起動時、最初のログ出力より前に行う必要があります
 *   <li>SLF4JのMDC.put()を呼ぶと、このアダプターを経由してMDC値が設定されます
 *   <li>子スレッドは親スレッドのMDC値のコピーを受け取ります（親の変更は子に影響しません）
 * </ul>
 *
 * <p><b>設計上の注意:</b>
 *
 * <ul>
 *   <li>Logbackはバージョン1.1.5以降、意図的にInheritableThreadLocalを削除しました（LOGBACK-624）
 *   <li>理由: スレッドプール環境でのMDC値の混入を防ぐため
 *   <li>本実装はVirtual Thread環境での利便性を優先した設計です
 * </ul>
 *
 * @see org.slf4j.MDC
 * @since 1.0
 */
public class InheritableMDCAdapter implements MDCAdapter {

  /**
   * InheritableThreadLocalを使用してMDC値を保持
   *
   * <p>子スレッド生成時に親スレッドの値がコピーされます
   */
  private final InheritableThreadLocal<Map<String, String>> inheritableThreadLocal =
      new InheritableThreadLocal<Map<String, String>>() {
        @Override
        protected Map<String, String> childValue(Map<String, String> parentValue) {
          if (parentValue == null) {
            return null;
          }
          // 防御的コピー: 子スレッドは親の値のコピーを受け取る
          return new HashMap<>(parentValue);
        }
      };

  /**
   * MDCに値を設定します
   *
   * @param key MDCキー
   * @param val MDC値
   */
  @Override
  public void put(String key, String val) {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }

    Map<String, String> map = inheritableThreadLocal.get();
    if (map == null) {
      map = new HashMap<>();
      inheritableThreadLocal.set(map);
    }
    map.put(key, val);
  }

  /**
   * MDCから値を取得します
   *
   * <p>スレッドローカルの値を優先し、存在しない場合は共有コンテキストを確認します。 これにより、MdcSetupRuleなどで他のスレッドが設定した値も自動的に利用可能になります。
   *
   * @param key MDCキー
   * @return MDC値、存在しない場合はnull
   */
  @Override
  public String get(String key) {
    // まずスレッドローカルの値を確認
    Map<String, String> map = inheritableThreadLocal.get();
    String value = null;
    if (map != null) {
      value = map.get(key);
    }
    // スレッドローカルに値がない場合のみ共有コンテキストを確認
    // ただし、StreamConverter管理のキー（executionId, commandSequence, stage）は除外
    if (value == null && !isStreamConverterManagedKey(key)) {
      value = MDCContext.getShared().get(key);
    }
    return value;
  }

  /**
   * StreamConverterが管理するMDCキーかどうかを判定します
   *
   * <p>これらのキーはスレッドローカルでのみ管理され、共有コンテキストからは取得しません。
   *
   * @param key MDCキー
   * @return StreamConverter管理のキーの場合true
   */
  private boolean isStreamConverterManagedKey(String key) {
    return "executionId".equals(key) || "commandSequence".equals(key) || "stage".equals(key);
  }

  /**
   * MDCから特定のキーを削除します
   *
   * @param key 削除するMDCキー
   */
  @Override
  public void remove(String key) {
    Map<String, String> map = inheritableThreadLocal.get();
    if (map != null) {
      map.remove(key);
    }
  }

  /**
   * MDCの全ての値をクリアします
   *
   * <p><b>重要:</b> スレッドプール環境では、スレッド処理完了時に必ずこのメソッドを呼び出してください。 さもないと、次にプールから取得したスレッドに前回のMDC値が残ってしまいます。
   */
  @Override
  public void clear() {
    Map<String, String> map = inheritableThreadLocal.get();
    if (map != null) {
      map.clear();
      inheritableThreadLocal.remove();
    }
  }

  /**
   * MDCの全ての値のコピーを取得します
   *
   * @return MDC値のコピー、MDCが空の場合はnull
   */
  @Override
  public Map<String, String> getCopyOfContextMap() {
    Map<String, String> map = inheritableThreadLocal.get();
    if (map == null) {
      return null;
    }
    return new HashMap<>(map);
  }

  /**
   * MDCに複数の値を一括設定します
   *
   * <p>既存のMDC値は全てクリアされ、新しい値で置き換えられます。
   *
   * @param contextMap 設定するMDC値のMap
   */
  @Override
  public void setContextMap(Map<String, String> contextMap) {
    inheritableThreadLocal.set(new HashMap<>(contextMap));
  }

  /**
   * プッシュ操作（スタック構造のサポート）
   *
   * <p>この実装ではスタック構造をサポートしていません。
   *
   * @param key プッシュするキー（未サポート）
   * @param value プッシュする値（未サポート）
   */
  @Override
  public void pushByKey(String key, String value) {
    // スタック構造は未サポート
    // 必要に応じて実装可能
  }

  /**
   * ポップ操作（スタック構造のサポート）
   *
   * <p>この実装ではスタック構造をサポートしていません。
   *
   * @param key ポップするキー（未サポート）
   * @return 常にnull
   */
  @Override
  public String popByKey(String key) {
    // スタック構造は未サポート
    return null;
  }

  /**
   * スタック値の取得操作
   *
   * <p>この実装ではスタック構造をサポートしていません。
   *
   * @param key 取得するキー（未サポート）
   * @return 常に空のDeque
   */
  @Override
  public java.util.Deque<String> getCopyOfDequeByKey(String key) {
    // スタック構造は未サポート
    return new java.util.ArrayDeque<>();
  }

  /**
   * スタックのクリア操作
   *
   * <p>この実装ではスタック構造をサポートしていません。
   *
   * @param key クリアするキー（未サポート）
   */
  @Override
  public void clearDequeByKey(String key) {
    // スタック構造は未サポート
  }
}
