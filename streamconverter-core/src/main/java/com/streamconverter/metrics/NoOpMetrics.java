package com.streamconverter.metrics;

/**
 * 何も記録しない {@link Metrics} のデフォルト実装。
 *
 * <p>メトリクス収集が不要な場合のデフォルト実装として使用する。 すべてのメソッドは何もしない（no-op）。
 *
 * <p>{@code StreamConverter} はデフォルトでこの実装を使用する。
 */
public final class NoOpMetrics implements Metrics {

  /** デフォルトの NoOpMetrics シングルトンインスタンス。 */
  public static final NoOpMetrics INSTANCE = new NoOpMetrics();

  /** インスタンス化を許可（シングルトン利用を推奨するが強制しない）。 */
  public NoOpMetrics() {}

  @Override
  public void recordCommandStart(String commandName) {
    // no-op
  }

  @Override
  public void recordCommandEnd(String commandName, long durationMs) {
    // no-op
  }

  @Override
  public void recordError(String commandName, Throwable t) {
    // no-op
  }

  @Override
  public void recordBytesProcessed(long bytes) {
    // no-op
  }
}
