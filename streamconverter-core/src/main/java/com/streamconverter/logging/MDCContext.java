package com.streamconverter.logging;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * スレッド間MDC同期専用クラス
 *
 * <p>このクラスは、スレッド間でMDC値を共有するための軽量なThreadLocalコンテナです。
 * MDCTurboFilterと組み合わせて使用することで、ログ出力時に自動的にMDCに同期されます。
 *
 * <p><b>設計思想:</b>
 *
 * <ul>
 *   <li>単一責任: MDC同期のみに特化
 *   <li>シンプル: 3つの静的メソッドのみ
 *   <li>スレッドセーフ: ThreadLocalによるスレッドごとの独立管理
 * </ul>
 *
 * <p><b>使用例:</b>
 *
 * <pre>{@code
 * // MDC値の設定
 * Map<String, String> mdcValues = new HashMap<>();
 * mdcValues.put("userId", "USER123");
 * mdcValues.put("requestId", "REQ-456");
 * MDCContext.set(mdcValues);
 *
 * try {
 *   // ログ出力時に自動的にMDCに同期される
 *   log.info("Processing...");  // → [userId:USER123, requestId:REQ-456] Processing...
 * } finally {
 *   // メモリリーク防止のため必ずクリア
 *   MDCContext.clear();
 * }
 * }</pre>
 *
 * <p><b>ExecutionContextとの違い:</b>
 *
 * <ul>
 *   <li>MDCContext: MDC同期専用、シンプルなMap、約100行
 *   <li>ExecutionContext: パイプライン全体の実行管理、複雑な構造、約400行
 * </ul>
 *
 * <p><b>スレッド安全性:</b> ThreadLocalを使用しているため、各スレッドは独立したMDC値を持ちます。 異なるスレッドで同じキーを設定しても、互いに影響を与えません。
 *
 * @see com.streamconverter.logging.MDCTurboFilter
 * @since 1.0
 */
public class MDCContext {

  /**
   * スレッドローカルなMDC値のコンテナ
   *
   * <p>各スレッドは独立したMapインスタンスを持ちます。
   */
  private static final ThreadLocal<Map<String, String>> holder =
      ThreadLocal.withInitial(HashMap::new);

  /**
   * スレッド間で共有されるMDC値のコンテナ
   *
   * <p>MdcSetupRuleなどで抽出した値を全スレッドで共有するために使用します。 マルチスレッド環境でも安全にアクセスできるようConcurrentHashMapを使用しています。
   */
  private static final Map<String, String> sharedContext = new ConcurrentHashMap<>();

  /**
   * 現在のスレッドにMDC値を設定します
   *
   * <p>設定された値は、ログ出力時にMDCTurboFilterによって自動的にMDCに同期されます。
   *
   * @param values MDCに設定する値のMap。nullの場合は空のMapとして扱われます。
   */
  public static void set(Map<String, String> values) {
    if (values == null) {
      holder.set(new HashMap<>());
    } else {
      // 防御的コピーを作成
      holder.set(new HashMap<>(values));
    }
  }

  /**
   * 現在のスレッドのMDC値を取得します（スレッド固有値と共有値の両方を含む）
   *
   * <p>返されるMapは読み取り専用のコピーです。変更しても元のMDC値には影響しません。 スレッド固有の値と共有コンテキストの値が両方含まれます。
   *
   * @return 現在のスレッドのMDC値のコピー。
   */
  public static Map<String, String> get() {
    Map<String, String> result = new HashMap<>(sharedContext);
    result.putAll(holder.get()); // スレッド固有の値が共有値を上書き
    return Collections.unmodifiableMap(result);
  }

  /**
   * 現在のスレッドに単一のMDC値を追加または更新します
   *
   * <p>既存の値に新しいキーと値を追加します。同じキーが存在する場合は上書きされます。
   *
   * @param key MDCキー
   * @param value MDC値。nullの場合は該当キーを削除します。
   * @throws NullPointerException keyがnullの場合
   */
  public static void put(String key, String value) {
    if (key == null) {
      throw new NullPointerException("MDC key cannot be null");
    }

    Map<String, String> current = holder.get();
    if (value == null) {
      current.remove(key);
    } else {
      current.put(key, value);
    }
  }

  /**
   * 現在のスレッドから特定のMDC値を削除します
   *
   * @param key 削除するMDCキー
   */
  public static void remove(String key) {
    holder.get().remove(key);
  }

  /**
   * 共有コンテキストに値を設定します（全スレッドで共有される）
   *
   * <p>この値はすべてのスレッドから参照可能です。MdcSetupRuleで抽出した値など、 マルチスレッド環境で共有したい値を設定するために使用します。
   *
   * @param key MDCキー
   * @param value MDC値。nullの場合は該当キーを削除します。
   * @throws NullPointerException keyがnullの場合
   */
  public static void putShared(String key, String value) {
    if (key == null) {
      throw new NullPointerException("MDC key cannot be null");
    }
    if (value == null) {
      sharedContext.remove(key);
    } else {
      sharedContext.put(key, value);
    }
  }

  /**
   * 共有コンテキストから値を削除します
   *
   * @param key 削除するMDCキー
   */
  public static void removeShared(String key) {
    sharedContext.remove(key);
  }

  /**
   * 共有コンテキストの値を取得します（スレッド固有の値は含まない）
   *
   * <p>返されるMapは読み取り専用のコピーです。変更しても元の共有コンテキストには影響しません。
   *
   * @return 共有コンテキストの値のコピー
   */
  public static Map<String, String> getShared() {
    return Collections.unmodifiableMap(new HashMap<>(sharedContext));
  }

  /**
   * 共有コンテキストをクリアします（全スレッド共通の値をクリア）
   *
   * <p>パイプライン処理の終了時などに呼び出して、共有コンテキストをリセットします。
   */
  public static void clearShared() {
    sharedContext.clear();
  }

  /**
   * 現在のスレッドの全MDC値をクリアします
   *
   * <p>ThreadLocalからMapを削除し、メモリリークを防止します。 スレッドプールを使用している場合、スレッドの処理完了時に必ず呼び出してください。
   *
   * <p><b>重要:</b> このメソッドを呼び忘れると、スレッドプール環境でメモリリークが発生する可能性があります。 try-finallyブロックで確実に呼び出すことを推奨します。
   *
   * <p><b>注意:</b> このメソッドはスレッド固有の値のみをクリアします。共有コンテキストはクリアされません。
   *
   * <pre>{@code
   * try {
   *   MDCContext.set(values);
   *   // 処理
   * } finally {
   *   MDCContext.clear();  // 必ず呼び出す
   * }
   * }</pre>
   */
  public static void clear() {
    holder.remove();
  }

  /**
   * Private constructor to prevent instantiation.
   *
   * <p>このクラスは静的ユーティリティクラスであり、インスタンス化を禁止します。
   */
  private MDCContext() {
    throw new AssertionError("Utility class should not be instantiated");
  }
}
