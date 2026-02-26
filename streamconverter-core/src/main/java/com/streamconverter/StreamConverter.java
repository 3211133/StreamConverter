package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * ストリーム変換クラス。
 *
 * <p>ストリームを変換するクラス。ストリームを変換するコマンドを指定して、ストリームを変換する。
 *
 * <p>ストリームを変換するコマンドは、IStreamCommandインターフェースを実装したクラスである必要がある。
 *
 * <p>親スレッドのMDCコンテキストは、各コマンドの仮想スレッドに自動的に伝搬される。 呼び出し側で{@code MDC.put()}した値は、全コマンドのログに反映される。
 */
public class StreamConverter {

  /** AutoCloseableラッパーでExecutorServiceのリソース管理を改善 */
  private static class AutoCloseableExecutorService implements AutoCloseable {
    private final ExecutorService executor;

    public AutoCloseableExecutorService(ExecutorService executor) {
      this.executor = executor;
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
      return CompletableFuture.runAsync(runnable, executor);
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
    ThreadFactory threadFactory = Thread.ofVirtual().name("stream-converter-", 0).factory();
    return Executors.newThreadPerTaskExecutor(threadFactory);
  }

  /**
   * 非同期並列処理でストリームを変換する。 メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。 親スレッドのMDCコンテキストは各コマンドの仮想スレッドに自動伝搬される。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public void run(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // 親スレッドのMDCコンテキストをキャプチャ
    Map<String, String> parentMdc = MDC.getCopyOfContextMap();

    // パイプライン内コマンド間で共有値を伝搬するためのコンテキスト
    PipelineContext pipelineContext = new PipelineContext();

    if (LOG.isInfoEnabled()) {
      LOG.info("Starting StreamConverter with {} commands", commands.size());
    }

    executeCommands(inputStream, outputStream, parentMdc, pipelineContext);

    if (LOG.isInfoEnabled()) {
      LOG.info("Completed StreamConverter pipeline");
    }

    return results;
  }

  /** コマンド（単一または複数）をMDC伝搬付きで並列実行 */
  private void executeCommands(
      InputStream inputStream,
      OutputStream outputStream,
      Map<String, String> parentMdc,
      PipelineContext pipelineContext)
      throws IOException {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    List<AutoCloseable> resources = new ArrayList<>();

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        final int commandIndex = i;

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

        // 各コマンドを非同期実行（親MDCを伝搬）
        CompletableFuture<Void> future =
            executor.runAsync(
                () -> {
                  // 親スレッドのMDCを子スレッドに設定
                  if (parentMdc != null) {
                    MDC.setContextMap(parentMdc);
                  }
                  PipelineContext.set(pipelineContext);

                  if (LOG.isInfoEnabled()) {
                    LOG.info(
                        "Setting up command {} of {}: {}",
                        commandIndex + 1,
                        commands.size(),
                        command.getClass().getSimpleName());
                  }

                  try {
                    // コマンド実行
                    command.execute(commandInput, commandOutput);

                    // 中間の PipedOutputStream は実行完了後にクローズする必要がある
                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }

                    if (LOG.isInfoEnabled()) {
                      LOG.info("Completed command: {}", command.getClass().getSimpleName());
                    }

                  } catch (IOException e) {
                    LOG.error(
                        "Command execution failed: {} - {}",
                        command.getClass().getSimpleName(),
                        e.getMessage(),
                        e);
                    if (commandOutput instanceof PipedOutputStream) {
                      try {
                        commandOutput.close();
                      } catch (IOException ignored) {
                        // ignored
                      }
                    }
                    throw new StreamProcessingException(
                        "Command execution failed: "
                            + command.getClass().getSimpleName()
                            + " - "
                            + e.getMessage(),
                        e);
                  } finally {
                    PipelineContext.clear();
                    MDC.clear();
                  }
                });

        futures.add(future);
      }

      // すべてのタスクの完了を待機
      for (int i = 0; i < futures.size(); i++) {
        try {
          // タイムアウト付きで待機（デッドロック防止）
          futures.get(i).get(60, TimeUnit.SECONDS);

        } catch (ExecutionException e) {
          cancelRemainingFutures(futures, i + 1);
          Throwable cause = e.getCause();
          if (cause instanceof StreamProcessingException spe) throw spe;
          if (cause instanceof IOException ioe) throw ioe;
          if (cause instanceof RuntimeException re) throw re;
          throw new StreamProcessingException("Unexpected error during command execution", cause);
        } catch (InterruptedException e) {
          cancelRemainingFutures(futures, i + 1);
          Thread.currentThread().interrupt();
          throw new StreamProcessingException("Command execution was interrupted", e);
        } catch (TimeoutException e) {
          cancelRemainingFutures(futures, i + 1);
          throw new StreamProcessingException("Command execution timed out after 60 seconds", e);
        }
      }

      if (LOG.isInfoEnabled()) {
        LOG.info("All commands completed successfully");
      }

    } finally {
      // リソースクリーンアップ
      closeResources(resources);
    }
  }

  /** 失敗時に残りのfuturesをキャンセルする */
  private void cancelRemainingFutures(List<CompletableFuture<Void>> futures, int fromIndex) {
    for (int j = fromIndex; j < futures.size(); j++) {
      futures.get(j).cancel(true);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn("Failed to close resource: {}", e.getMessage());
        }
      }
    }
  }
}
