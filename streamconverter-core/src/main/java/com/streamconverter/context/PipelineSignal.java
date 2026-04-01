package com.streamconverter.context;

/**
 * パイプライン内のコマンド間で送受信できるシグナルの基底型。
 *
 * <p>前段コマンドが {@link SignalChannel#send(PipelineSignal)} でシグナルを送信し、 後段コマンドが処理ループ内で {@link
 * SignalChannel#poll()} を呼び出すことで、 ブロッキングなしに割り込み型の動作変更を実現する。
 *
 * <p>sealed interface により、受信側は switch 式でシグナル種別を網羅的に処理できる。 シグナルが届いていない場合（{@code poll()} が空の Optional
 * を返す）は通常処理継続とみなす。
 *
 * <p>パイプライン全体の中断が必要な場合は、シグナルではなく {@link com.streamconverter.StreamProcessingException} を直接スローすること。
 *
 * <p>使用例（後段コマンドの処理ループ内）:
 *
 * <pre>{@code
 * channel.poll().ifPresent(signal -> {
 *     switch (signal) {
 *         case PipelineSignal.Skip s -> log.info("Skipping: {}", s.reason());
 *     }
 * });
 * }</pre>
 *
 * @see SignalChannel
 */
public sealed interface PipelineSignal permits PipelineSignal.Skip {

  /**
   * 後続処理のスキップを要求するシグナル。
   *
   * <p>受信したコマンドは処理をスキップし、入力データをそのまま出力に流すなど、 軽量な代替動作に切り替えることを想定している。
   *
   * @param reason スキップ理由（ログ出力・デバッグ用）
   */
  record Skip(String reason) implements PipelineSignal {}
}
