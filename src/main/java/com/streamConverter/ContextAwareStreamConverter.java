package com.streamConverter;

import com.streamConverter.command.ContextPropagatingDecorator;
import com.streamConverter.command.IContextAwareStreamCommand;
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
 * コンテキスト対応のStreamConverter
 *
 * <p>ExecutionContextを使用してマルチスレッド環境での MDCコンテキスト伝播を実現するStreamConverterの拡張版
 */
public class ContextAwareStreamConverter {

  private static final Logger logger = LoggerFactory.getLogger(ContextAwareStreamConverter.class);
  private static final int DEFAULT_BUFFER_SIZE = 64 * 1024; // 64KB buffer

  private final List<IContextAwareStreamCommand> commands;
  private final ExecutionContext executionContext;

  /**
   * コンストラクタ
   *
   * @param commands 実行するコマンドのリスト
   * @param context 実行コンテキスト
   */
  public ContextAwareStreamConverter(
      List<IContextAwareStreamCommand> commands, ExecutionContext context) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("commands cannot be empty");
    }
    this.commands = new ArrayList<>(commands);
    this.executionContext = Objects.requireNonNull(context, "context cannot be null");
  }

  /**
   * ファクトリメソッド：既存のコマンドからコンテキスト対応コンバーターを作成
   *
   * @param commands 実行するコマンド（可変長引数）
   * @return ContextAwareStreamConverterインスタンス
   */
  public static ContextAwareStreamConverter create(IStreamCommand... commands) {
    return create(ExecutionContext.create(), commands);
  }

  /**
   * ファクトリメソッド：カスタムコンテキストでコンバーターを作成
   *
   * @param context 実行コンテキスト
   * @param commands 実行するコマンド（可変長引数）
   * @return ContextAwareStreamConverterインスタンス
   */
  public static ContextAwareStreamConverter create(
      ExecutionContext context, IStreamCommand... commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands cannot be empty");
    }

    List<IContextAwareStreamCommand> contextAwareCommands = new ArrayList<>();
    for (IStreamCommand command : commands) {
      if (command instanceof IContextAwareStreamCommand) {
        contextAwareCommands.add((IContextAwareStreamCommand) command);
      } else {
        // 既存のコマンドをデコレータでラップ
        contextAwareCommands.add(new ContextPropagatingDecorator(command));
      }
    }

    return new ContextAwareStreamConverter(contextAwareCommands, context);
  }

  /**
   * ファクトリメソッド：リストからコンバーターを作成
   *
   * @param commands 実行するコマンドのリスト
   * @return ContextAwareStreamConverterインスタンス
   */
  public static ContextAwareStreamConverter create(List<IStreamCommand> commands) {
    return create(ExecutionContext.create(), commands);
  }

  /**
   * ファクトリメソッド：カスタムコンテキストでリストからコンバーターを作成
   *
   * @param context 実行コンテキスト
   * @param commands 実行するコマンドのリスト
   * @return ContextAwareStreamConverterインスタンス
   */
  public static ContextAwareStreamConverter create(
      ExecutionContext context, List<IStreamCommand> commands) {
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.isEmpty()) {
      throw new IllegalArgumentException("commands cannot be empty");
    }

    List<IContextAwareStreamCommand> contextAwareCommands = new ArrayList<>();
    for (IStreamCommand command : commands) {
      if (command instanceof IContextAwareStreamCommand) {
        contextAwareCommands.add((IContextAwareStreamCommand) command);
      } else {
        contextAwareCommands.add(new ContextPropagatingDecorator(command));
      }
    }

    return new ContextAwareStreamConverter(contextAwareCommands, context);
  }

  /**
   * ストリーム変換を実行
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @return 各コマンドの実行結果リスト
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> run(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(inputStream, "inputStream cannot be null");
    Objects.requireNonNull(outputStream, "outputStream cannot be null");

    // 実行コンテキストをMDCに適用
    executionContext.applyToMDCWithStage("pipeline-start");

    logger.info(
        "Starting ContextAware StreamConverter with {} commands (executionId: {})",
        commands.size(),
        executionContext.getExecutionId());

    if (commands.size() == 1) {
      // 単一コマンドの場合は直接実行
      return executeSingleCommand(inputStream, outputStream);
    } else {
      // 複数コマンドの場合は並列パイプライン実行
      return executeMultipleCommands(inputStream, outputStream);
    }
  }

  /** 単一コマンドの実行 */
  private List<CommandResult> executeSingleCommand(
      InputStream inputStream, OutputStream outputStream) throws IOException {

    IContextAwareStreamCommand command = commands.get(0);
    logger.info("Executing single command: {}", command.getClass().getSimpleName());

    long startTime = System.currentTimeMillis();
    java.time.Instant startInstant = java.time.Instant.now();

    try {
      command.execute(inputStream, outputStream, executionContext);

      long endTime = System.currentTimeMillis();
      java.time.Instant endInstant = java.time.Instant.now();

      List<CommandResult> results = new ArrayList<>();
      results.add(
          CommandResult.success(
              command.getClass().getSimpleName(),
              endTime - startTime,
              0L, // 現在の実装では入力バイト数は取得困難
              0L, // 現在の実装では出力バイト数は取得困難
              startInstant,
              endInstant));

      return results;

    } catch (Exception e) {
      long endTime = System.currentTimeMillis();
      java.time.Instant endInstant = java.time.Instant.now();

      List<CommandResult> results = new ArrayList<>();
      results.add(
          CommandResult.failure(
              command.getClass().getSimpleName(),
              endTime - startTime,
              e.getMessage(),
              startInstant,
              endInstant));

      throw e; // 例外は再スロー
    }
  }

  /** 複数コマンドの並列実行 */
  private List<CommandResult> executeMultipleCommands(
      InputStream inputStream, OutputStream outputStream) throws IOException {

    ExecutorService executor = createOptimalExecutor();
    List<CompletableFuture<CommandResult>> futures = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    try {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < commands.size(); i++) {
        IContextAwareStreamCommand command = commands.get(i);
        logger.info(
            "Setting up command {} of {}: {}",
            i + 1,
            commands.size(),
            command.getClass().getSimpleName());

        final InputStream commandInput = currentInput;
        final OutputStream commandOutput;

        if (i == commands.size() - 1) {
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

        // 各コマンドを非同期実行（同じExecutionContextを共有）
        final ExecutionContext commandContext = executionContext;
        CompletableFuture<CommandResult> future =
            CompletableFuture.supplyAsync(
                () -> {
                  long startTime = System.currentTimeMillis();
                  java.time.Instant startInstant = java.time.Instant.now();

                  try {
                    command.execute(commandInput, commandOutput, commandContext);

                    // 中間の PipedOutputStream は実行完了後にクローズする必要がある
                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }

                    long endTime = System.currentTimeMillis();
                    java.time.Instant endInstant = java.time.Instant.now();

                    return CommandResult.success(
                        command.getClass().getSimpleName(),
                        endTime - startTime,
                        0L, // 入力バイト数
                        0L, // 出力バイト数
                        startInstant,
                        endInstant);

                  } catch (IOException e) {
                    logger.error(
                        "Command execution failed: {} - {}",
                        command.getClass().getSimpleName(),
                        e.getMessage(),
                        e);

                    long endTime = System.currentTimeMillis();
                    java.time.Instant endInstant = java.time.Instant.now();

                    return CommandResult.failure(
                        command.getClass().getSimpleName(),
                        endTime - startTime,
                        e.getMessage(),
                        startInstant,
                        endInstant);
                  }
                },
                executor);

        futures.add(future);
      }

      // すべてのタスクの完了を待機
      List<CommandResult> results = new ArrayList<>();
      for (CompletableFuture<CommandResult> future : futures) {
        try {
          CommandResult result = future.get(60, TimeUnit.SECONDS);
          results.add(result);

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

      logger.info(
          "All commands completed successfully (executionId: {})",
          executionContext.getExecutionId());
      return results;

    } finally {
      // リソースクリーンアップ
      shutdownExecutor(executor);
      closeResources(resources);
    }
  }

  /** 最適なExecutorServiceを作成 */
  private ExecutorService createOptimalExecutor() {
    int availableCores = Runtime.getRuntime().availableProcessors();
    int optimalSize = Math.min(commands.size(), Math.max(2, availableCores));
    return Executors.newFixedThreadPool(optimalSize);
  }

  /** ExecutorServiceを安全にシャットダウン */
  private void shutdownExecutor(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        logger.warn("Executor did not terminate gracefully, forcing shutdown");
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          logger.error("Executor did not terminate after forced shutdown");
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
    }
  }

  /** リソースを安全にクローズ */
  private void closeResources(List<AutoCloseable> resources) {
    for (AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (Exception e) {
        logger.warn("Failed to close resource: {}", e.getMessage());
      }
    }
  }

  /**
   * 実行コンテキストを取得
   *
   * @return 実行コンテキスト
   */
  public ExecutionContext getExecutionContext() {
    return executionContext;
  }
}
