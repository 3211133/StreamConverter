package com.streamconverter.metrics;

import java.util.Objects;
import org.slf4j.Logger;

/**
 * SLF4J ロガーを使用してメトリクスをログ出力する {@link Metrics} 実装。
 *
 * <p>コマンドの開始・終了・エラー・バイト数を INFO/WARN レベルでログ出力する。 本番環境での観測可能性向上や開発時のデバッグに使用する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * Logger metricsLogger = LoggerFactory.getLogger("com.example.metrics");
 * Metrics metrics = new LoggingMetrics(metricsLogger);
 *
 * IStreamCommand command = rawCommand.withLogging(logger, "MyCommand", metrics);
 * }</pre>
 */
public final class LoggingMetrics implements Metrics {

  private final Logger logger;

  /**
   * 指定ロガーを使用する LoggingMetrics を作成する。
   *
   * @param logger ログ出力先のロガー
   * @throws NullPointerException logger が null の場合
   */
  public LoggingMetrics(Logger logger) {
    this.logger = Objects.requireNonNull(logger, "logger must not be null");
  }

  @Override
  public void recordCommandStart(String commandName) {
    if (logger.isInfoEnabled()) {
      logger.info("[metrics] command.start name={}", commandName);
    }
  }

  @Override
  public void recordCommandEnd(String commandName, long durationMs) {
    if (logger.isInfoEnabled()) {
      logger.info("[metrics] command.end name={} durationMs={}", commandName, durationMs);
    }
  }

  @Override
  public void recordError(String commandName, Throwable t) {
    logger.warn("[metrics] command.error name={} error={}", commandName, t.getMessage(), t);
  }

  @Override
  public void recordBytesProcessed(long bytes) {
    if (logger.isDebugEnabled()) {
      logger.debug("[metrics] bytes.processed count={}", bytes);
    }
  }
}
