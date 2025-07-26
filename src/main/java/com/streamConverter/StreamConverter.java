package com.streamConverter;

import com.streamConverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
  private static final Logger log = LoggerFactory.getLogger(StreamConverter.class);
  private static final int DEFAULT_BUFFER_SIZE = 64 * 1024; // 64KB buffer
  private List<IStreamCommand> commands;

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
   * 非同期並列処理でストリームを変換する。 メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @return 各コマンドの実行結果リスト
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public List<CommandResult> run(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    log.info("Starting StreamConverter with {} commands", commands.size());

    if (this.commands.size() == 1) {
      // 単一コマンドの場合は直接実行（メモリ効率最優先）
      IStreamCommand command = this.commands.get(0);
      log.info("Executing single command: {}", command.getClass().getSimpleName());
      long startTime = System.currentTimeMillis();
      java.time.Instant startInstant = java.time.Instant.now();
      try {
        command.execute(inputStream, outputStream);
        long endTime = System.currentTimeMillis();
        java.time.Instant endInstant = java.time.Instant.now();

        List<CommandResult> results = new ArrayList<>();
        results.add(
            CommandResult.success(
                command.getClass().getSimpleName(),
                endTime - startTime,
                0L, // 入力バイト数は現在の実装では取得困難
                0L, // 出力バイト数は現在の実装では取得困難
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

    // 複数コマンドの場合はPipedStreamで並行処理
    ExecutorService executor = createOptimalExecutor();
    List<Future<?>> futures = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    try {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        log.info(
            "Executing command {} of {}: {}",
            i + 1,
            commands.size(),
            command.getClass().getSimpleName());

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

        // 各コマンドを非同期実行
        Future<?> future =
            executor.submit(
                () -> {
                  try {
                    command.execute(commandInput, commandOutput);
                    // 中間の PipedOutputStream は実行完了後にクローズする必要がある
                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }
                  } catch (IOException e) {
                    log.error(
                        "Command execution failed: {} - {}",
                        command.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
                    throw new StreamProcessingException(
                        "Command execution failed: " + command.getClass().getSimpleName(), e);
                  }
                });
        futures.add(future);
      }

      // すべてのタスクの完了を待機
      List<CommandResult> result = new ArrayList<>();
      for (int i = 0; i < futures.size(); i++) {
        Future<?> future = futures.get(i);
        try {
          // タイムアウト付きで待機（デッドロック防止）
          future.get(60, TimeUnit.SECONDS);
          // 現在の実装では詳細な実行結果を取得できないため、ダミーの成功結果を作成
          result.add(
              CommandResult.success(
                  commands.get(i).getClass().getSimpleName(),
                  0L, // 実行時間は現在取得できない
                  0L, // 入力バイト数
                  0L, // 出力バイト数
                  java.time.Instant.now(), // ダミーの開始時間
                  java.time.Instant.now() // ダミーの終了時間
                  ));
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause instanceof StreamProcessingException spe) {
            throw spe;
          } else if (cause instanceof RuntimeException re) {
            Throwable rootCause = re.getCause();
            if (rootCause instanceof IOException ioe) {
              throw ioe;
            }
            throw re;
          }
          throw new StreamProcessingException("Unexpected error during command execution", cause);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Command execution was interrupted", e);
        } catch (TimeoutException e) {
          throw new IOException("Command execution timed out after 60 seconds", e);
        }
      }

      log.info("All commands completed successfully");
      return result;

    } finally {
      // リソースクリーンアップ
      shutdownExecutor(executor);
      closeResources(resources);
    }
  }

  /**
   * ExecutorServiceを安全にシャットダウンする
   *
   * @param executor シャットダウン対象のExecutorService
   */
  private void shutdownExecutor(ExecutorService executor) {
    executor.shutdown();
    try {
      if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
        log.warn("Executor did not terminate gracefully, forcing shutdown");
        executor.shutdownNow();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
          log.error("Executor did not terminate after forced shutdown");
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      executor.shutdownNow();
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
        log.warn("Failed to close resource: {}", e.getMessage());
      }
    }
  }
}
