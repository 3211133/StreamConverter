package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
  private final CommandStageRunner commandStageRunner;
  private final PipelineCompletionMonitor pipelineCompletionMonitor;
  private final PipelineFailureHandler pipelineFailureHandler;
  private final PipelineWiring pipelineWiring;
  private List<IStreamCommand> commands;

  private StreamConverter(List<IStreamCommand> commands) {
    this.commands = wrapWithLogging(commands);
    this.commandStageRunner = new CommandStageRunner();
    this.pipelineFailureHandler = new PipelineFailureHandler();
    this.pipelineCompletionMonitor = new PipelineCompletionMonitor();
    this.pipelineWiring = new PipelineWiring(DEFAULT_BUFFER_SIZE);
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
    Objects.requireNonNull(commands, "commands cannot be null");
    if (commands.length == 0) {
      throw new IllegalArgumentException("commands is empty.");
    }
    return new StreamConverter(List.of(commands));
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
    return new StreamConverter(List.copyOf(commands));
  }

  /** コマンドリストの各コマンドに withLogging をあらかじめ適用して返す。 ラッピングはコンストラクト時に1度だけ行われ、実行ごとのオーバーヘッドを排除する。 */
  private static List<IStreamCommand> wrapWithLogging(List<IStreamCommand> commands) {
    List<IStreamCommand> wrapped = new ArrayList<>(commands.size());
    for (IStreamCommand command : commands) {
      wrapped.add(command.withLogging(LOG, command.commandName()));
    }
    return wrapped;
  }

  static boolean isPipeAbortedCause(Throwable cause) {
    return PipelineFailureHandler.isPipeAbortedCause(cause);
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
   * 非同期並列処理でストリームを変換する。 メモリ効率を重視し、PipedStreamを使用して大容量ファイルに対応。
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public void run(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    if (LOG.isInfoEnabled()) {
      LOG.info("Starting StreamConverter with {} commands", commands.size());
    }

    executeCommands(inputStream, outputStream);

    if (LOG.isInfoEnabled()) {
      LOG.info("Completed StreamConverter pipeline");
    }
  }

  /** コマンド（単一または複数）を並列実行 */
  private void executeCommands(InputStream inputStream, OutputStream outputStream)
      throws IOException {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    PipelinePlan plan = pipelineWiring.build(commands.size(), inputStream, outputStream);
    List<AutoCloseable> resources = new ArrayList<>(plan.resources());

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      futures.addAll(commandStageRunner.startAll(commands, plan, executor::runAsync));

      // パイプライン構築完了後に exceptionally ハンドラを登録する。
      // これにより resources リストへの add が全て終わった後にワーカーからクローズが呼ばれることが保証される。
      // いずれかのコマンドが失敗したら、すべてのパイプをクローズして
      // ブロック中の書き込みを IOException で即座に解放する。
      for (CompletableFuture<Void> future : futures) {
        future.exceptionally(
            t -> {
              closeResources(resources);
              return null;
            });
      }

      try {
        // 全タスクの完了を待機してから根本原因を収集する。
        // abort() による物理クローズで各コマンドが順不同で完了するため、
        // 全完了後に走査することでレースコンディションを回避する。
        pipelineCompletionMonitor.await(futures);
      } catch (ExecutionException e) {
        pipelineFailureHandler.rethrowExecutionFailure(e, futures);
      }

      if (LOG.isInfoEnabled()) {
        LOG.info("All commands completed successfully");
      }

    } finally {
      // リソースクリーンアップ
      closeResources(resources);
    }
  }

  /**
   * Closes resources in order, logging and continuing if individual closes fail.
   *
   * @param resources resources associated with the current pipeline execution
   */
  private void closeResources(List<AutoCloseable> resources) {
    for (AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (Exception e) {
        LOG.warn("Failed to close resource [{}]", resource.getClass().getSimpleName(), e);
      }
    }
  }
}
