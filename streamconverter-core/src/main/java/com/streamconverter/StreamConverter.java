package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.execution.BufferPolicy;
import com.streamconverter.execution.ErrorPolicy;
import com.streamconverter.execution.ExecutionStrategy;
import com.streamconverter.execution.MemoryBudget;
import com.streamconverter.execution.ParallelExecutionStrategy;
import com.streamconverter.io.Sink;
import com.streamconverter.io.Source;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ストリーム変換クラス。
 *
 * <p>ストリームを変換するクラス。ストリームを変換するコマンドを指定して、ストリームを変換する。
 *
 * <p>ストリームを変換するコマンドは、IStreamCommandインターフェースを実装したクラスである必要がある。
 *
 * <p>親スレッドのMDCコンテキストは、{@link com.streamconverter.logging.InheritableMDCAdapter}が
 * インストールされている場合、各コマンドの仮想スレッドに自動的に伝搬される。{@link
 * com.streamconverter.logging.MDCInitializer#initialize()}が呼ばれていない場合、各ワーカースレッドは
 * 独立した空のMDCコンテキストを持ち、親スレッドのMDC値は伝搬されない。
 *
 * <p>実行戦略は {@link ExecutionStrategy} で抽象化されており、デフォルトは仮想スレッドによる並列実行 ({@link
 * ParallelExecutionStrategy}) である。逐次実行が必要な場合は {@link
 * com.streamconverter.execution.SequentialExecutionStrategy} を使用する。
 */
public class StreamConverter {

  private static final Logger LOG = LoggerFactory.getLogger(StreamConverter.class);

  private final List<IStreamCommand> commands;
  private final List<String> commandNames;
  private final ExecutionStrategy executionStrategy;
  private final MemoryBudget memoryBudget;
  private final ErrorPolicy errorPolicy;

  private StreamConverter(
      List<IStreamCommand> commands,
      ExecutionStrategy executionStrategy,
      MemoryBudget memoryBudget,
      ErrorPolicy errorPolicy) {
    this.commandNames = commands.stream().map(StreamConverter::resolveCommandName).toList();
    this.commands = wrapWithLogging(commands, this.commandNames);
    this.executionStrategy = executionStrategy;
    this.memoryBudget = memoryBudget;
    this.errorPolicy = errorPolicy;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // ファクトリメソッド
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Creates a StreamConverter with the specified array of commands.
   *
   * <p>デフォルトの {@link ParallelExecutionStrategy} と {@link MemoryBudget#defaultBudget()}、 {@link
   * ErrorPolicy#failFast()} を使用する。
   *
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(IStreamCommand... commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(
        List.of(commands),
        new ParallelExecutionStrategy(),
        MemoryBudget.defaultBudget(),
        ErrorPolicy.failFast());
  }

  /**
   * Creates a StreamConverter with the specified list of commands.
   *
   * @param commands the list of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(List<IStreamCommand> commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(
        List.copyOf(commands),
        new ParallelExecutionStrategy(),
        MemoryBudget.defaultBudget(),
        ErrorPolicy.failFast());
  }

  /**
   * Creates a StreamConverter with a custom {@link ExecutionStrategy}.
   *
   * @param strategy the execution strategy to use
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if strategy or commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(ExecutionStrategy strategy, IStreamCommand... commands) {
    Objects.requireNonNull(strategy, "strategy cannot be null");
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(
        List.of(commands), strategy, MemoryBudget.defaultBudget(), ErrorPolicy.failFast());
  }

  /**
   * Creates a StreamConverter with a custom {@link MemoryBudget}.
   *
   * @param memoryBudget the memory budget configuration
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if memoryBudget or commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(MemoryBudget memoryBudget, IStreamCommand... commands) {
    Objects.requireNonNull(memoryBudget, "memoryBudget cannot be null");
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(
        List.of(commands), new ParallelExecutionStrategy(), memoryBudget, ErrorPolicy.failFast());
  }

  /**
   * Creates a StreamConverter with a custom {@link ErrorPolicy}.
   *
   * @param errorPolicy the error handling policy
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if errorPolicy or commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(ErrorPolicy errorPolicy, IStreamCommand... commands) {
    Objects.requireNonNull(errorPolicy, "errorPolicy cannot be null");
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(
        List.of(commands),
        new ParallelExecutionStrategy(),
        MemoryBudget.defaultBudget(),
        errorPolicy);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // ヘルパーメソッド
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * 元のコマンドクラスから人が読めるコマンド名を解決する。 ラムダ（synthetic）と匿名クラス（getSimpleName が空文字）は "IStreamCommand"
   * にフォールバックする。
   */
  private static String resolveCommandName(IStreamCommand command) {
    Class<?> cls = command.getClass();
    if (cls.isSynthetic()) {
      return "IStreamCommand";
    }
    String simpleName = cls.getSimpleName();
    return simpleName.isEmpty() ? "IStreamCommand" : simpleName;
  }

  /** コマンドリストの各コマンドに withLogging をあらかじめ適用して返す。 ラッピングはコンストラクト時に1度だけ行われ、実行ごとのオーバーヘッドを排除する。 */
  private static List<IStreamCommand> wrapWithLogging(
      List<IStreamCommand> commands, List<String> names) {
    List<IStreamCommand> wrapped = new ArrayList<>(commands.size());
    for (int i = 0; i < commands.size(); i++) {
      wrapped.add(commands.get(i).withLogging(LOG, names.get(i)));
    }
    return wrapped;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // 実行
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Source と Sink を使用してストリームを変換する。
   *
   * <p>Source と Sink のストリームはこのメソッド内で開かれ、処理完了後にクローズされる。 {@link ErrorPolicy#retry(int, long)}
   * を使用している場合、失敗時に Source を再オープンしてリトライする。
   *
   * @param source 入力ソース
   * @param sink 出力シンク
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   * @throws NullPointerException source または sink が null の場合
   */
  public void run(Source source, Sink sink) throws IOException {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(sink, "sink must not be null");
    if (errorPolicy instanceof ErrorPolicy.Retry retry) {
      runWithRetry(source, sink, retry);
    } else {
      try (InputStream inputStream = source.open();
          OutputStream outputStream = sink.open()) {
        runCore(inputStream, outputStream);
      }
    }
  }

  /**
   * 非同期並列処理でストリームを変換する。 メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。
   *
   * <p><strong>注意:</strong> {@link ErrorPolicy#retry(int, long)} とともに使用する場合は、 InputStream は巻き戻せないため
   * {@link UnsupportedOperationException} をスローする。 代わりに {@link #run(Source, Sink)} を使用すること。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   * @throws UnsupportedOperationException ErrorPolicy.Retry が設定されている場合
   */
  public void run(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);
    if (errorPolicy instanceof ErrorPolicy.Retry) {
      throw new UnsupportedOperationException(
          "ErrorPolicy.Retry requires run(Source, Sink) — InputStream cannot be rewound for retry."
              + " Use StreamConverter.run(Source, Sink) instead.");
    }
    runCore(inputStream, outputStream);
  }

  /** Source を再オープンしながらリトライするコア実装 */
  private void runWithRetry(Source source, Sink sink, ErrorPolicy.Retry retry) throws IOException {
    IOException lastException = null;
    BufferPolicy bufferPolicy = memoryBudget.getBufferPolicy();
    for (int attempt = 0; attempt <= retry.maxRetries(); attempt++) {
      try (InputStream inputStream = source.open();
          OutputStream outputStream = sink.open()) {
        if (LOG.isInfoEnabled()) {
          LOG.info("Starting StreamConverter with {} commands", commands.size());
        }
        executionStrategy.execute(commands, commandNames, inputStream, outputStream, bufferPolicy);
        if (LOG.isInfoEnabled()) {
          LOG.info("Completed StreamConverter pipeline");
        }
        return;
      } catch (IOException e) {
        lastException = e;
        if (attempt < retry.maxRetries()) {
          if (LOG.isWarnEnabled()) {
            LOG.warn(
                "Command execution failed (attempt {}/{}), retrying in {}ms: {}",
                attempt + 1,
                retry.maxRetries() + 1,
                retry.delayMs(),
                e.getMessage());
          }
          if (retry.delayMs() > 0) {
            try {
              Thread.sleep(retry.delayMs());
            } catch (InterruptedException ie) {
              Thread.currentThread().interrupt();
              throw new StreamProcessingException("Retry interrupted", ie);
            }
          }
        }
      }
    }
    throw lastException;
  }

  /** エラーポリシーなしで実行する内部コア */
  private void runCore(InputStream inputStream, OutputStream outputStream) throws IOException {
    if (LOG.isInfoEnabled()) {
      LOG.info("Starting StreamConverter with {} commands", commands.size());
    }
    BufferPolicy bufferPolicy = memoryBudget.getBufferPolicy();
    executionStrategy.execute(commands, commandNames, inputStream, outputStream, bufferPolicy);
    if (LOG.isInfoEnabled()) {
      LOG.info("Completed StreamConverter pipeline");
    }
  }
}
