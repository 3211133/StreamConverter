package com.streamconverter.context;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 前段コマンドから後段コマンドへシグナルを送るスレッドセーフなチャネル。
 *
 * <p>{@link PipelineContext#prepareSignalChannel(String)} で取得したチャネルを 前段コマンドと後段コマンドのコンストラクタに渡して使用する。
 *
 * <p><b>設計方針:</b>
 *
 * <ul>
 *   <li>後段コマンドはブロッキング待機を行わず、処理ループを継続しながら {@link #poll()} でシグナルの有無を確認する（割り込み型）。
 *   <li>シグナルは上書き可能。後から送られた強いシグナルで以前のシグナルを上書きできる。
 *   <li>一度送られたシグナルは {@link #poll()} で何度でも確認できる（消費しない）。
 * </ul>
 *
 * <p>使用例（前段コマンド）:
 *
 * <pre>{@code
 * // 非表示フィルタ適用後にシグナルを送信
 * if (!isVisible(record)) {
 *     channel.send(new PipelineSignal.Skip("non-visible item filtered"));
 * }
 * }</pre>
 *
 * <p>使用例（後段コマンド）:
 *
 * <pre>{@code
 * // 処理ループ内でシグナルを確認（ブロッキングなし）
 * channel.poll().ifPresent(signal -> {
 *     switch (signal) {
 *         case PipelineSignal.Skip s -> { /* スキップ処理 *&#47; }
 *     }
 * });
 * }</pre>
 *
 * @see PipelineSignal
 * @see PipelineContext#prepareSignalChannel(String)
 */
public final class SignalChannel {

  private final AtomicReference<PipelineSignal> signal = new AtomicReference<>();
  private final String channelId;

  /**
   * パッケージプライベートコンストラクタ。
   *
   * <p>外部からの直接インスタンス化を防ぎ、{@link PipelineContext#prepareSignalChannel(String)} を 唯一の生成手段とするための設計。
   *
   * @param channelId チャネルID
   */
  SignalChannel(String channelId) {
    this.channelId = channelId;
  }

  /**
   * シグナルを送信する。
   *
   * <p>既にシグナルが設定されている場合は上書きする。 複数のシグナルが連続して送られた場合は最後のシグナルが有効になる。
   *
   * @param signal 送信するシグナル
   * @throws IllegalArgumentException signal が null の場合
   */
  public void send(PipelineSignal signal) {
    if (signal == null) {
      throw new IllegalArgumentException("signal must not be null");
    }
    this.signal.set(signal);
  }

  /**
   * シグナルを非ブロッキングで確認する。
   *
   * <p>シグナルが届いていれば返す。届いていなければ {@link Optional#empty()} を返す。 シグナルは消費されず、何度でも確認できる。
   *
   * @return シグナルが届いている場合はそれを含む Optional、未着の場合は empty
   */
  public Optional<PipelineSignal> poll() {
    return Optional.ofNullable(signal.get());
  }

  /**
   * シグナルをリセットする（未着状態に戻す）。
   *
   * <p>前段コマンドが複数回シグナルを送る場合、シグナルを処理済みにして 次の単位では「シグナルなし」状態に戻したい場合に使用する。
   *
   * <p>例: 明細1件ごとにシグナルの有無を判定するとき、 表示対象の明細を送る前に前段がリセットし、後段が正しく判定できるようにする。
   */
  public void reset() {
    signal.set(null);
  }

  /**
   * チャネルIDを返す。
   *
   * @return チャネルID
   */
  public String channelId() {
    return channelId;
  }
}
