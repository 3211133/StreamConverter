package com.streamconverter.execution;

/**
 * パイプライン実行時のエラーハンドリングポリシー。
 *
 * <p>sealed interface により、許可されたポリシー実装のみが存在することを保証する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // 即時失敗（デフォルト）
 * StreamConverter converter = StreamConverter.create(
 *     ErrorPolicy.failFast(), command1, command2);
 *
 * // リトライあり
 * StreamConverter converter = StreamConverter.create(
 *     ErrorPolicy.retry(3, 100), command1, command2);
 * }</pre>
 */
public sealed interface ErrorPolicy
    permits ErrorPolicy.FailFast, ErrorPolicy.Skip, ErrorPolicy.Retry {

  /**
   * デフォルトのエラーポリシー（即時失敗）を返す。
   *
   * @return FailFast ポリシー
   */
  static ErrorPolicy failFast() {
    return new FailFast();
  }

  /**
   * エラー発生時にそのコマンドをスキップするポリシーを返す。
   *
   * @return Skip ポリシー
   */
  static ErrorPolicy skip() {
    return new Skip();
  }

  /**
   * エラー発生時にリトライするポリシーを返す。
   *
   * @param maxRetries 最大リトライ回数（1以上）
   * @param delayMs リトライ間隔（ミリ秒）
   * @return Retry ポリシー
   * @throws IllegalArgumentException maxRetries が 1 未満、または delayMs が負の場合
   */
  static ErrorPolicy retry(int maxRetries, long delayMs) {
    return new Retry(maxRetries, delayMs);
  }

  /**
   * 最初のエラーで即座に失敗するポリシー（現行動作、後方互換）。
   *
   * <p>例外はラップされずにそのまま伝播する。
   */
  record FailFast() implements ErrorPolicy {}

  /**
   * エラー発生時にそのコマンドの出力をスキップしてパイプラインを継続するポリシー。
   *
   * <p>注意: スキップにより後続コマンドへの入力が空になる可能性がある。
   */
  record Skip() implements ErrorPolicy {}

  /**
   * エラー発生時に指定回数リトライするポリシー。
   *
   * @param maxRetries 最大リトライ回数
   * @param delayMs リトライ前の待機時間（ミリ秒）
   */
  record Retry(int maxRetries, long delayMs) implements ErrorPolicy {
    public Retry {
      if (maxRetries < 1) {
        throw new IllegalArgumentException("maxRetries must be at least 1, but was: " + maxRetries);
      }
      if (delayMs < 0) {
        throw new IllegalArgumentException("delayMs must be non-negative, but was: " + delayMs);
      }
    }
  }
}
