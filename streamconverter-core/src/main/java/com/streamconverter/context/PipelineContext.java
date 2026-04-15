package com.streamconverter.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.MDC;

/**
 * パイプライン内でコマンド間の共有値を管理するコンテキスト。
 *
 * <p>パイプライン実行中に、あるコマンドが抽出した値（例: XMLヘッダのorderId）を 他のコマンドのログ出力に自動反映するための仕組みを提供する。
 *
 * <p>共有値は {@link com.streamconverter.logging.PipelineContextTurboFilter} により、
 * ログ出力直前にMDCへ自動的にマージされる。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // コマンド内でストリームデータから値を抽出してMDCに伝搬
 * PipelineContext.putShared("orderId", extractedOrderId);
 *
 * // 他のコマンドのログ出力時に自動的にMDCに反映される
 * }</pre>
 *
 * <p><b>MDC 関連クラスの全体像:</b>
 *
 * <ul>
 *   <li>{@link com.streamconverter.command.rule.MdcPropagatingRule} — ストリームから抽出した値を このコンテキスト経由で MDC
 *       に伝搬する Rule 実装。コマンドから MDC へ値を書き込む際の推奨手段。
 *   <li>{@link com.streamconverter.logging.PipelineContextTurboFilter} — ログ出力直前に {@link
 *       #syncToMDC()} を呼び出し、共有値を MDC へ自動反映する Logback TurboFilter。
 *   <li>{@link com.streamconverter.logging.MDCInitializer} — {@code InheritableMDCAdapter} を
 *       インストールし、MDC コンテキストを子スレッドへ自動継承させる。アプリ起動時に一度呼ぶ。
 * </ul>
 */
public final class PipelineContext {

  private static final ThreadLocal<PipelineContext> HOLDER = new ThreadLocal<>();

  private final Map<String, String> sharedValues;

  /** パイプラインコンテキストを新規作成する。 */
  public PipelineContext() {
    this.sharedValues = new ConcurrentHashMap<>();
  }

  /**
   * 共有値を設定し、呼び出しスレッドのMDCにも即座に反映する。
   *
   * <p>PipelineContextが未設定のスレッドから呼ばれた場合は何もしない。
   *
   * @param key MDCキー名
   * @param value 値。nullの場合はキーを削除する
   * @throws IllegalArgumentException keyがnullの場合
   */
  public static void putShared(String key, String value) {
    if (key == null) {
      throw new IllegalArgumentException("key must not be null");
    }
    PipelineContext ctx = HOLDER.get();
    if (ctx == null) {
      return;
    }
    if (value != null) {
      ctx.sharedValues.put(key, value);
      MDC.put(key, value);
    } else {
      ctx.sharedValues.remove(key);
      MDC.remove(key);
    }
  }

  /**
   * 共有値を取得する。
   *
   * @param key MDCキー名
   * @return 値。キーが存在しない場合またはPipelineContext未設定の場合はnull
   */
  public static String getShared(String key) {
    PipelineContext ctx = HOLDER.get();
    if (ctx == null) {
      return null;
    }
    return ctx.sharedValues.get(key);
  }

  /**
   * 全共有値を呼び出しスレッドのMDCにマージする。
   *
   * <p>PipelineContextが未設定の場合は何もしない。
   */
  public static void syncToMDC() {
    PipelineContext ctx = HOLDER.get();
    if (ctx == null) {
      return;
    }
    ctx.sharedValues.forEach(MDC::put);
  }

  /**
   * 現在のスレッドに紐づくPipelineContextを取得する。
   *
   * @return PipelineContext、未設定の場合はnull
   */
  static PipelineContext current() {
    return HOLDER.get();
  }

  /**
   * 共有値のスナップショットを返す。
   *
   * @return 共有値の不変ビュー
   */
  Map<String, String> getSharedValues() {
    return Collections.unmodifiableMap(sharedValues);
  }

  /**
   * 現在のスレッドにPipelineContextを設定する。
   *
   * <p>StreamConverterがパイプラインの各コマンドスレッドで呼び出す。
   *
   * @param context 設定するPipelineContext
   */
  public static void set(PipelineContext context) {
    HOLDER.set(context);
  }

  /** 現在のスレッドからPipelineContextをクリアする。 */
  public static void clear() {
    HOLDER.remove();
  }
}
