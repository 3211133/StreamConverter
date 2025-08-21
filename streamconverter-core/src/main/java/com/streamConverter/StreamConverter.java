package com.streamConverter;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ストリーム変換クラス。
 *
 * <p>ストリームを変換するクラス。ストリームを変換するコマンドを指定して、ストリームを変換する。
 *
 * <p>ストリームを変換するコマンドは、IStreamCommandインターフェースを実装したクラスである必要がある。
 */
public class StreamConverter {

  /** AutoCloseableラッパーでExecutorServiceのリソース管理を改善 */
  private static class AutoCloseableExecutorService implements AutoCloseable {
    private final ExecutorService executor;

    public AutoCloseableExecutorService(ExecutorService executor) {
      this.executor = executor;
    }

    public CompletableFuture<CommandResult> supplyAsync(
        java.util.function.Supplier<CommandResult> supplier) {
      return CompletableFuture.supplyAsync(supplier, executor);
    }

    @Override
    public void close() {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
          LOG.warn("Executor did not terminate gracefully, forcing shutdown");
          executor.shutdownNow();
          if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            LOG.error("Executor did not terminate after forced shutdown");
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        executor.shutdownNow();
      }
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(StreamConverter.class);
  private static final int DEFAULT_BUFFER_SIZE = 64 * 1024; // 64KB buffer
  private List<IStreamCommand> commands;
  private ExecutionContext defaultContext;

  /**
   * Constructs a StreamConverter with the specified array of commands.
   *
   * @param commands the array of commands to be executed in sequence
   * @throws NullPointerException if commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public StreamConverter(IStreamCommand[] commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    this.commands = List.of(commands);
  }

  /**
   * Constructs a StreamConverter with the specified list of commands.
   *
   * @param commands the list of commands to be executed in sequence
   * @throws NullPointerException if commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public StreamConverter(List<IStreamCommand> commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("commands is empty.");
    }
    this.commands = new ArrayList<>(commands); // Defensive copy
  }

  /**
   * Creates a StreamConverter with the specified array of commands.
   *
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter create(IStreamCommand... commands) {
    return new StreamConverter(commands);
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
    return new StreamConverter(commands);
  }

  /**
   * Creates a StreamConverter with a custom ExecutionContext and specified commands. All subsequent
   * runs will use the provided context for MDC synchronization.
   *
   * @param context the execution context to use for MDC and logging
   * @param commands the array of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if context or commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter createWithContext(
      ExecutionContext context, IStreamCommand... commands) {
    Objects.requireNonNull(context, "context cannot be null");
    StreamConverter converter = new StreamConverter(commands);
    converter.defaultContext = context;
    return converter;
  }

  /**
   * Creates a StreamConverter with a custom ExecutionContext and specified commands list. All
   * subsequent runs will use the provided context for MDC synchronization.
   *
   * @param context the execution context to use for MDC and logging
   * @param commands the list of commands to be executed in sequence
   * @return a new StreamConverter instance
   * @throws NullPointerException if context or commands is null
   * @throws IllegalArgumentException if commands is empty
   */
  public static StreamConverter createWithContext(
      ExecutionContext context, List<IStreamCommand> commands) {
    Objects.requireNonNull(context, "context cannot be null");
    StreamConverter converter = new StreamConverter(commands);
    converter.defaultContext = context;
    return converter;
  }

  /**
   * Creates an optimal executor service based on available system resources and command count.
   *
   * @return an optimally configured ExecutorService
   */
  private ExecutorService createOptimalExecutor() {
    int availableCores = Runtime.getRuntime().availableProcessors();
    int optimalSize = Math.min(this.commands.size(), Math.max(2, availableCores));
    return Executors.newFixedThreadPool(optimalSize);
  }

  /**
   * 非同期並列処理でストリームを変換する。 メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。 自動的にExecutionContextを生成してMDC同期を実現する。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @return 各コマンドの実行結果リスト
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> run(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    // デフォルトコンテキストがあればそれを使用、なければ自動生成
    ExecutionContext contextToUse =
        defaultContext != null ? defaultContext : ExecutionContext.create();
    return run(inputStream, outputStream, contextToUse);
  }

  /**
   * カスタムExecutionContextを使用してストリームを変換する。 マルチスレッド環境でのMDCコンテキスト伝播とログトレーサビリティを実現。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @param context 実行コンテキスト
   * @return 各コマンドの実行結果リスト
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> run(
      InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);
    Objects.requireNonNull(context, "context cannot be null");

    // パイプライン開始時にMDCコンテキストを設定
    context.applyToMDCWithStage("pipeline-start");

    LOG.info(
        "Starting StreamConverter with {} commands (executionId: {})",
        commands.size(),
        context.getExecutionId());

    // PipedStreamで並行処理（MDC対応）
    return executeMultipleCommandsWithMDC(inputStream, outputStream, context);
  }

  /** コマンド（単一または複数）をMDC同期付きで並列実行 */
  private List<CommandResult> executeMultipleCommandsWithMDC(
      InputStream inputStream, OutputStream outputStream, ExecutionContext context)
      throws IOException {
    List<CompletableFuture<CommandResult>> futures = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      InputStream currentInput = inputStream;

      // パイプライン構築（MDC対応）
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        final int commandIndex = i; // Lambda用のfinal変数

        final InputStream commandInput = currentInput;
        final OutputStream commandOutput;

        if (i == this.commands.size() - 1) {
          // 最後のコマンド
          commandOutput = outputStream;
        } else {
          // 中間コマンド: 次のコマンド用にPipedStreamペア作成
          PipedOutputStream pipedOut = new PipedOutputStream();
          PipedInputStream pipedIn = new PipedInputStream(pipedOut, DEFAULT_BUFFER_SIZE);
          resources.add(pipedOut);
          resources.add(pipedIn);
          commandOutput = pipedOut;
          currentInput = pipedIn;
        }

        // 各コマンドを非同期実行（MDC同期付き）
        CompletableFuture<CommandResult> future =
            executor.supplyAsync(
                () -> {
                  // スレッド固有のMDC設定
                  int sequence = context.getNextCommandSequence();
                  String stageName = command.getClass().getSimpleName() + "-" + sequence;
                  context.applyToMDCWithStage(stageName);

                  LOG.info(
                      "Setting up command {} of {}: {} (sequence: {})",
                      commandIndex + 1,
                      commands.size(),
                      command.getClass().getSimpleName(),
                      sequence);

                  long startTime = System.currentTimeMillis();
                  java.time.Instant startInstant = java.time.Instant.now();

                  try {
                    // コマンド実行（MDCは自動的に利用可能）
                    command.execute(commandInput, commandOutput);

                    // 中間の PipedOutputStream は実行完了後にクローズする必要がある
                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }

                    long endTime = System.currentTimeMillis();
                    java.time.Instant endInstant = java.time.Instant.now();

                    LOG.info(
                        "Completed command: {} (sequence: {})",
                        command.getClass().getSimpleName(),
                        sequence);

                    return CommandResult.success(
                        command.getClass().getSimpleName(),
                        endTime - startTime,
                        0L, // 入力バイト数
                        0L, // 出力バイト数
                        startInstant,
                        endInstant);

                  } catch (IOException e) {
                    long endTime = System.currentTimeMillis();
                    java.time.Instant endInstant = java.time.Instant.now();

                    LOG.error(
                        "Command execution failed: {} (sequence: {}) - {}",
                        command.getClass().getSimpleName(),
                        sequence,
                        e.getMessage(),
                        e);

                    return CommandResult.failure(
                        command.getClass().getSimpleName(),
                        endTime - startTime,
                        e.getMessage(),
                        startInstant,
                        endInstant);
                  }
                });

        futures.add(future);
      }

      // すべてのタスクの完了を待機
      List<CommandResult> results = new ArrayList<>();
      for (CompletableFuture<CommandResult> future : futures) {
        try {
          // タイムアウト付きで待機（デッドロック防止）
          CommandResult result = future.get(60, TimeUnit.SECONDS);
          results.add(result);

          // 失敗した場合は例外をスロー
          if (!result.isSuccess()) {
            throw new IOException("Command execution failed: " + result.getErrorMessage());
          }

        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof IOException) {
            throw (IOException) cause;
          } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
          }
          throw new IOException("Unexpected error during command execution", cause);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Command execution was interrupted", e);
        } catch (TimeoutException e) {
          throw new IOException("Command execution timed out after 60 seconds", e);
        }
      }

      LOG.info("All commands completed successfully (executionId: {})", context.getExecutionId());
      return results;

    } finally {
      // リソースクリーンアップ
      closeResources(resources);
    }
  }

  /**
   * 使用したリソースを安全にクローズする
   *
   * @param resources クローズ対象のリソースリスト
   */
  private void closeResources(List<AutoCloseable> resources) {
    for (AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (Exception e) {
        LOG.warn("Failed to close resource: {}", e.getMessage());
      }
    }
  }
}
