package com.streamconverter.metrics;

/**
 * パイプライン実行の観測可能性（Observability）インターフェース。
 *
 * <p>コマンドの開始・終了・エラー・バイト数処理を記録するフックを提供する。 デフォルト実装は {@link NoOpMetrics}（何もしない）。 SLF4J ベースの実装は {@link
 * LoggingMetrics} を参照。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // ログベースのメトリクス
 * Metrics metrics = new LoggingMetrics(LoggerFactory.getLogger("pipeline.metrics"));
 *
 * // StreamConverter に適用（IStreamCommand.withLogging 経由）
 * IStreamCommand command = rawCommand.withLogging(logger, "MyCommand", metrics);
 * }</pre>
 */
public interface Metrics {

  /**
   * コマンド開始を記録する。
   *
   * @param commandName コマンド名
   */
  void recordCommandStart(String commandName);

  /**
   * コマンド正常終了を記録する。
   *
   * @param commandName コマンド名
   * @param durationMs 実行時間（ミリ秒）
   */
  void recordCommandEnd(String commandName, long durationMs);

  /**
   * コマンドエラーを記録する。
   *
   * @param commandName コマンド名
   * @param t 発生した例外
   */
  void recordError(String commandName, Throwable t);

  /**
   * 処理バイト数を記録する。
   *
   * @param bytes 処理したバイト数
   */
  void recordBytesProcessed(long bytes);
}
