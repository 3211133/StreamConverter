package com.streamConverter.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IStreamCommandのログ出力デコレーター
 *
 * <p>このクラスは既存のIStreamCommandをラップして、詳細なログ出力機能を追加します。 デコレーターパターンを使用することで、既存のコマンドを変更せずに
 * 追加のログ機能を提供できます。
 *
 * <p>使用例:
 *
 * <pre>
 * IStreamCommand originalCommand = new CsvNavigateCommand("name");
 * IStreamCommand loggedCommand = new LoggingDecorator(originalCommand);
 * loggedCommand.execute(inputStream, outputStream);
 * </pre>
 */
public class LoggingDecorator implements IStreamCommand {
  private final IStreamCommand delegate;
  private final Logger log;
  private final String commandName;

  /**
   * コンストラクタ
   *
   * @param delegate ラップ対象のコマンド
   */
  public LoggingDecorator(IStreamCommand delegate) {
    this.delegate = delegate;
    this.commandName = delegate.getClass().getSimpleName();
    this.log = LoggerFactory.getLogger(delegate.getClass());
  }

  /**
   * カスタムログ名を指定するコンストラクタ
   *
   * @param delegate ラップ対象のコマンド
   * @param loggerName ログ名
   */
  public LoggingDecorator(IStreamCommand delegate, String loggerName) {
    this.delegate = delegate;
    this.commandName = delegate.getClass().getSimpleName();
    this.log = LoggerFactory.getLogger(loggerName);
  }

  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    long startTime = System.currentTimeMillis();

    // AbstractStreamCommandの場合は重複ログを避けるため、簡潔なログ出力
    boolean isAbstractCommand = delegate instanceof AbstractStreamCommand;

    if (isAbstractCommand) {
      // AbstractStreamCommandの場合は追加的な情報のみログ出力
      log.info("=== Enhanced logging for {} ===", commandName);
    } else {
      // 非AbstractStreamCommandの場合は詳細なログ出力
      log.info("=== {} Execution Started ===", commandName);
    }

    log.debug("Input stream type: {}", inputStream.getClass().getSimpleName());
    log.debug("Output stream type: {}", outputStream.getClass().getSimpleName());
    log.debug("Available input bytes: {}", safeAvailable(inputStream));

    // スレッド情報
    Thread currentThread = Thread.currentThread();
    log.debug("Executing on thread: {} (ID: {})", currentThread.getName(), currentThread.getId());

    // システムリソース情報
    Runtime runtime = Runtime.getRuntime();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();
    long usedMemory = totalMemory - freeMemory;

    log.debug(
        "Memory before execution: used={}KB, free={}KB, total={}KB",
        usedMemory / 1024,
        freeMemory / 1024,
        totalMemory / 1024);

    try {
      // 実際のコマンド実行
      delegate.execute(inputStream, outputStream);

      // 成功時のログ
      long duration = System.currentTimeMillis() - startTime;

      if (isAbstractCommand) {
        // AbstractStreamCommandの場合は追加的な情報のみ
        log.info("=== Enhanced logging completed for {} ({} ms) ===", commandName, duration);
      } else {
        // 非AbstractStreamCommandの場合は詳細なログ
        log.info("=== {} Execution Completed Successfully ({} ms) ===", commandName, duration);
      }

      // 実行後のメモリ情報
      long usedMemoryAfter = (runtime.totalMemory() - runtime.freeMemory());
      long memoryDiff = usedMemoryAfter - usedMemory;

      log.debug(
          "Memory after execution: used={}KB (diff: {}KB)",
          usedMemoryAfter / 1024,
          memoryDiff / 1024);

      // パフォーマンス分析
      analyzePerformance(duration, memoryDiff);

    } catch (Exception e) {
      // 例外発生時のログ
      long duration = System.currentTimeMillis() - startTime;

      if (isAbstractCommand) {
        // AbstractStreamCommandの場合は追加的な情報のみ
        log.error("=== Enhanced logging failed for {} ({} ms) ===", commandName, duration);
      } else {
        // 非AbstractStreamCommandの場合は詳細なログ
        log.error("=== {} Execution Failed ({} ms) ===", commandName, duration);
      }

      log.error("Exception type: {}", e.getClass().getSimpleName());
      log.error("Exception message: {}", e.getMessage());

      // スタックトレースの最初の数行のみを記録（デバッグレベル）
      if (log.isDebugEnabled()) {
        StackTraceElement[] stackTrace = e.getStackTrace();
        log.debug("Stack trace (first 5 lines):");
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
          log.debug("  at {}", stackTrace[i]);
        }
      }

      throw e;
    }
  }

  /**
   * パフォーマンス分析を実行
   *
   * @param duration 実行時間（ミリ秒）
   * @param memoryDiff メモリ使用量の変化（バイト）
   */
  private void analyzePerformance(long duration, long memoryDiff) {
    // パフォーマンス警告
    if (duration > 10000) { // 10秒以上
      log.warn("PERFORMANCE: {} took {} ms - consider optimization", commandName, duration);
    } else if (duration > 5000) { // 5秒以上
      log.info("PERFORMANCE: {} took {} ms - slower than expected", commandName, duration);
    }

    // メモリ使用量警告
    long memoryDiffMB = memoryDiff / 1024 / 1024;
    if (memoryDiffMB > 100) { // 100MB以上
      log.warn("MEMORY: {} used {} MB - high memory consumption", commandName, memoryDiffMB);
    } else if (memoryDiffMB > 50) { // 50MB以上
      log.info("MEMORY: {} used {} MB - moderate memory consumption", commandName, memoryDiffMB);
    }

    // 効率性分析
    if (duration > 1000 && memoryDiffMB > 10) {
      log.info(
          "EFFICIENCY: {} may benefit from optimization (time: {}ms, memory: {}MB)",
          commandName,
          duration,
          memoryDiffMB);
    }
  }

  /**
   * InputStreamのavailable()メソッドを安全に呼び出し
   *
   * @param inputStream 入力ストリーム
   * @return 利用可能バイト数（取得失敗時は-1）
   */
  private int safeAvailable(InputStream inputStream) {
    try {
      return inputStream.available();
    } catch (IOException e) {
      log.debug("Failed to get available bytes: {}", e.getMessage());
      return -1;
    }
  }

  /**
   * ラップされているコマンドを取得
   *
   * @return ラップされているコマンド
   */
  public IStreamCommand getDelegate() {
    return delegate;
  }

  /**
   * コマンド名を取得
   *
   * @return コマンド名
   */
  public String getCommandName() {
    return commandName;
  }
}
