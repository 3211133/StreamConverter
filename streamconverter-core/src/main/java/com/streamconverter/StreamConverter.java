package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
  private List<IStreamCommand> commands;
  private List<String> commandNames;

  private StreamConverter(List<IStreamCommand> commands) {
    this.commandNames = commands.stream().map(StreamConverter::resolveCommandName).toList();
    this.commands = wrapWithLogging(commands, this.commandNames);
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
    run(inputStream, outputStream, new PipelineContext());
  }

  /**
   * 外部で構築したPipelineContextを使用して、非同期並列処理でストリームを変換する。
   *
   * <p>コマンド間でシグナルを送受信する {@link com.streamconverter.context.SignalChannel} を使用する場合は、 事前に {@link
   * PipelineContext#prepareSignalChannel(String)} でチャネルを登録してから このメソッドを呼び出す。
   *
   * <p>使用例:
   *
   * <pre>{@code
   * PipelineContext ctx = new PipelineContext();
   * SignalChannel ch = ctx.prepareSignalChannel("validation");
   *
   * StreamConverter.create(
   *     new ValidatorCommand(ch),
   *     new TransformCommand(ch)
   * ).run(input, output, ctx);
   * }</pre>
   *
   * @param inputStream 処理対象の入力ストリーム
   * @param outputStream 処理結果を書き込む出力ストリーム
   * @param pipelineContext 使用するPipelineContext
   * @throws IOException ストリーム処理中にI/Oエラーが発生した場合
   */
  public void run(
      InputStream inputStream, OutputStream outputStream, PipelineContext pipelineContext)
      throws IOException {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);
    Objects.requireNonNull(pipelineContext);

    if (LOG.isInfoEnabled()) {
      LOG.info("Starting StreamConverter with {} commands", commands.size());
    }

    executeCommands(inputStream, outputStream, pipelineContext);

    if (LOG.isInfoEnabled()) {
      LOG.info("Completed StreamConverter pipeline");
    }
  }

  /** コマンド（単一または複数）を並列実行 */
  private void executeCommands(
      InputStream inputStream, OutputStream outputStream, PipelineContext pipelineContext)
      throws IOException {
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    // CopyOnWriteArrayList: パイプライン構築中（メインスレッド）と
    // 失敗時のクローズ処理（ワーカースレッド）が並行して安全にアクセスできるようにする
    List<AutoCloseable> resources = new CopyOnWriteArrayList<>();
    List<AbortablePipedStream> pipes = new CopyOnWriteArrayList<>();

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        final String commandName = this.commandNames.get(i);
        final InputStream commandInput = currentInput;
        final OutputStream commandOutput;

        final AbortablePipedStream pipe;
        if (i == this.commands.size() - 1) {
          // 最後のコマンド
          commandOutput = outputStream;
          pipe = null;
        } else {
          // 中間コマンド: 次のコマンド用にAbortablePipedStreamペア作成
          pipe = new AbortablePipedStream(DEFAULT_BUFFER_SIZE);
          resources.add(pipe);
          pipes.add(pipe);
          commandOutput = pipe.outputStream();
          currentInput = pipe.inputStream();
        }

        // 各コマンドを非同期実行
        CompletableFuture<Void> future =
            executor.runAsync(
                () -> {
                  PipelineContext.set(pipelineContext);

                  try {
                    // コマンド実行（ロギングはコンストラクタ時にwithLogging()でラップ済み）
                    command.execute(commandInput, commandOutput);

                    // 中間コマンドの出力側を閉じて後段コマンドに EOF を通知する
                    if (pipe != null) {
                      try {
                        commandOutput.close();
                      } catch (IOException closeEx) {
                        abortAllPipes(pipes);
                        throw new StreamProcessingException(
                            "Failed to close output stream of command: " + commandName, closeEx);
                      }
                    }

                  } catch (StreamProcessingException e) {
                    abortAllPipes(pipes);
                    // Already a StreamProcessingException — re-throw as-is to avoid double-wrapping
                    throw e;
                  } catch (IOException | RuntimeException e) {
                    abortAllPipes(pipes);
                    throw new StreamProcessingException(
                        "Command execution failed: " + commandName + " - " + e.getMessage(), e);
                  } catch (Error e) {
                    abortAllPipes(pipes);
                    throw e;
                  } finally {
                    PipelineContext.clear();
                    MDC.clear();
                  }
                });

        futures.add(future);
      }

      // パイプライン構築完了後に exceptionally ハンドラを登録する。
      // これにより resources リストへの add が全て終わった後にワーカーからクローズが呼ばれることが保証される。
      // いずれかのコマンドが失敗したら、すべてのパイプをクローズして
      // ブロック中の書き込みを IOException で即座に解放する。
      for (CompletableFuture<Void> future : futures) {
        future.exceptionally(
            t -> {
              try {
                closeResources(resources);
              } catch (RuntimeException ex) {
                LOG.error("Unexpected error during resource cleanup on pipeline failure", ex);
              }
              return null;
            });
      }

      // 全タスクの完了を待機してから根本原因を収集する。
      // abort() による物理クローズで各コマンドが順不同で完了するため、
      // 全完了後に走査することでレースコンディションを回避する。
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
      } catch (ExecutionException e) {
        // いずれかが失敗した場合は全件走査して根本原因を収集する
        List<Throwable> rootCauses = collectRootCauses(futures);
        if (rootCauses.isEmpty()) {
          // 全件 PipeAbortedException の場合（通常起こらない）
          throw new StreamProcessingException(
              "Unexpected error during command execution", e.getCause());
        }
        Throwable primary = rootCauses.get(0);
        for (int i = 1; i < rootCauses.size(); i++) {
          primary.addSuppressed(rootCauses.get(i));
        }
        if (primary instanceof Error err) throw err;
        if (primary instanceof StreamProcessingException spe) throw spe;
        if (primary instanceof IOException ioe) throw ioe;
        if (primary instanceof RuntimeException re) throw re;
        throw new StreamProcessingException("Unexpected error during command execution", primary);
      } catch (InterruptedException e) {
        cancelRemainingFutures(futures, 0);
        Thread.currentThread().interrupt();
        throw new StreamProcessingException("Pipeline execution was interrupted", e);
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
   * 全 futures から根本原因（{@link PipeAbortedException} でない失敗）を全件収集して返す。
   *
   * <p>呼び出し前に全 futures が完了済みであること。
   */
  private List<Throwable> collectRootCauses(List<CompletableFuture<Void>> futures) {
    List<Throwable> rootCauses = new ArrayList<>();
    for (CompletableFuture<Void> f : futures) {
      if (!f.isCompletedExceptionally()) {
        continue;
      }
      try {
        f.get();
      } catch (ExecutionException ee) {
        Throwable cause = ee.getCause();
        if (!isPipeAbortedCause(cause)) {
          rootCauses.add(cause);
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        rootCauses.add(ie);
      }
    }
    return rootCauses;
  }

  /**
   * 対向コマンドの異常終了に巻き込まれた二次的な失敗かどうかを判定する。
   *
   * <p>{@link PipeAbortedException} は {@link AbortablePipedStream#abort()} で設定される
   * 型付き例外であり、二次的失敗と明示的に識別できる。
   */
  static boolean isPipeAbortedCause(Throwable cause) {
    if (cause instanceof PipeAbortedException) {
      return true;
    }
    if (cause instanceof StreamProcessingException) {
      return cause.getCause() instanceof PipeAbortedException;
    }
    return false;
  }

  /**
   * すべてのパイプを abort して、ブロック中の read/write を {@link PipeAbortedException} で解放する。
   *
   * @param pipes abort 対象のパイプリスト
   */
  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream p : pipes) {
      p.abort();
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
        LOG.warn("Failed to close resource [{}]", resource.getClass().getSimpleName(), e);
      }
    }
  }
}
