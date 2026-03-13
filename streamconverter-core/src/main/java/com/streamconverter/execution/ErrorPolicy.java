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
 * // リトライあり（Source/Sink オーバーロードのみ対応）
 * StreamConverter converter = StreamConverter.create(
 *     ErrorPolicy.retry(3, 100), command1, command2);
 * converter.run(Source.ofFile(inputPath), Sink.toFile(outputPath));
 * }</pre>
 */
public sealed interface ErrorPolicy permits ErrorPolicy.FailFast, ErrorPolicy.Retry {

  /**
   * デフォルトのエラーポリシー（即時失敗）を返す。
   *
   * @return FailFast ポリシー
   */
  static ErrorPolicy failFast() {
    return new FailFast();
  }

  /**
   * エラー発生時にリトライするポリシーを返す。
   *
   * <p><strong>注意:</strong> Retry は {@link com.streamconverter.StreamConverter#run(
   * com.streamconverter.io.Source, com.streamconverter.io.Sink)} でのみ有効。 {@link
   * com.streamconverter.StreamConverter#run(java.io.InputStream, java.io.OutputStream)} に渡した場合は
   * {@link UnsupportedOperationException} をスローする（InputStream は巻き戻せないため）。
   *
   * @param maxRetries 最大リトライ回数（1以上）
   * @param delayMs リトライ間隔（ミリ秒、0以上）
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
