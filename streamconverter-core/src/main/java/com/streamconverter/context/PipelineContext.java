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

  /** パイプラインコンテキストを新規作成する。 */
  public PipelineContext() {}

  private static final ThreadLocal<PipelineContext> HOLDER = new ThreadLocal<>();

  private final ConcurrentHashMap<String, String> sharedValues = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SignalChannel> signalChannels = new ConcurrentHashMap<>();

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

  /**
   * 指定IDのSignalChannelを事前登録する。
   *
   * <p>{@link com.streamconverter.StreamConverter#run(java.io.InputStream, java.io.OutputStream,
   * PipelineContext)} 呼び出し前に、コマンド間で共有するチャネルを確立するために使用する。 同じIDで複数回呼んだ場合は既存のチャネルを返す。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * PipelineContext ctx = new PipelineContext();
   * SignalChannel ch = ctx.prepareSignalChannel("validation");
   *
   * StreamConverter.create(
   *     new FilterCommand(ch),    // 前段: ch.send(new PipelineSignal.Skip("..."))
   *     new TransformCommand(ch)  // 後段: ch.poll() で割り込み確認
   * ).run(input, output, ctx);
   * }</pre>
   *
   * @param channelId チャネルID
   * @return 新規または既存の SignalChannel
   * @throws IllegalArgumentException channelId が null の場合
   */
  public SignalChannel prepareSignalChannel(String channelId) {
    if (channelId == null) {
      throw new IllegalArgumentException("channelId must not be null");
    }
    return signalChannels.computeIfAbsent(channelId, SignalChannel::new);
  }

  /**
   * 現在のスレッドに紐づくPipelineContextから、指定IDのSignalChannelを取得する。
   *
   * <p>PipelineContext未設定のスレッドから呼ばれた場合、またはチャネルが未登録の場合は {@code null} を返す。
   *
   * @param channelId チャネルID
   * @return SignalChannel。未設定または未登録の場合は null
   * @throws IllegalArgumentException channelId が null の場合
   */
  public static SignalChannel getSignalChannel(String channelId) {
    if (channelId == null) {
      throw new IllegalArgumentException("channelId must not be null");
    }
    PipelineContext ctx = HOLDER.get();
    if (ctx == null) {
      return null;
    }
    return ctx.signalChannels.get(channelId);
  }
}
