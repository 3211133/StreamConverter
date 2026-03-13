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
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);

    // パイプライン内コマンド間で共有値を伝搬するためのコンテキスト
    PipelineContext pipelineContext = new PipelineContext();

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

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < this.commands.size(); i++) {
        IStreamCommand command = this.commands.get(i);
        final String commandName = this.commandNames.get(i);
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
        CompletableFuture<Void> future =
            executor.runAsync(
                () -> {
                  PipelineContext.set(pipelineContext);

                  try {
                    // コマンド実行（ロギングはコンストラクタ時にwithLogging()でラップ済み）
                    command.execute(commandInput, commandOutput);

                    // 中間の PipedOutputStream は実行完了後にクローズする必要がある
                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }

                  } catch (StreamProcessingException e) {
                    if (commandOutput instanceof PipedOutputStream) {
                      try {
                        commandOutput.close();
                      } catch (IOException ignored) {
                        // ignored
                      }
                    }
                    // Already a StreamProcessingException — re-throw as-is to avoid double-wrapping
                    throw e;
                  } catch (IOException | RuntimeException e) {
                    if (commandOutput instanceof PipedOutputStream) {
                      try {
                        commandOutput.close();
                      } catch (IOException ignored) {
                        // ignored
                      }
                    }
                    throw new StreamProcessingException(
                        "Command execution failed: " + commandName + " - " + e.getMessage(), e);
                  } catch (Error e) {
                    if (commandOutput instanceof PipedOutputStream) {
                      try {
                        commandOutput.close();
                      } catch (IOException ignored) {
                        // ignored
                      }
                    }
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
              closeResources(resources);
              return null;
            });
      }

      // すべてのタスクの完了を待機。
      // 前段コマンドの待機中に後段が失敗していれば exceptionally ハンドラがパイプをクローズし、
      // 前段の書き込みブロックを IOException で即解放する。
      for (int i = 0; i < futures.size(); i++) {
        try {
          futures.get(i).get();
        } catch (ExecutionException e) {
          // findFirstFailureCause より先に cancel すると、後段 future が isCancelled() になり
          // 根本原因が取得できなくなるため、先に根本原因を特定してからキャンセルする
          Throwable cause = findFirstFailureCause(futures, e.getCause());
          cancelRemainingFutures(futures, i + 1);
          if (cause instanceof Error err) throw err;
          if (cause instanceof StreamProcessingException spe) throw spe;
          if (cause instanceof IOException ioe) throw ioe;
          if (cause instanceof RuntimeException re) throw re;
          throw new StreamProcessingException("Unexpected error during command execution", cause);
        } catch (InterruptedException e) {
          cancelRemainingFutures(futures, i);
          Thread.currentThread().interrupt();
          throw new StreamProcessingException("Pipeline execution was interrupted", e);
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

  /**
   * 完了済みの futures から最も根本的な失敗原因を取得する。
   *
   * <p>パイプ破損による二次的な IOException（PipedInputStream/PipedOutputStream 起因）を避け、
   * 本来の原因（後段コマンドの失敗など）を優先して返す。 既に完了済みの future のみを走査し、ブロックしない。 非パイプ系の失敗が見つかれば即座に返し、全てパイプ系または未完了の場合は
   * fallback を返す。
   */
  private Throwable findFirstFailureCause(
      List<CompletableFuture<Void>> futures, Throwable fallback) {
    Throwable ioFallback = null;
    for (CompletableFuture<Void> f : futures) {
      // 未完了またはキャンセル済みの future はスキップする
      if (!f.isDone() || f.isCancelled()) {
        continue;
      }
      if (f.isCompletedExceptionally()) {
        try {
          f.get();
        } catch (ExecutionException ee) {
          Throwable cause = ee.getCause();
          if (!isPipeBrokenCause(cause)) {
            return cause;
          }
          if (ioFallback == null) {
            ioFallback = cause;
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          return ie;
        }
      }
    }
    return ioFallback != null ? ioFallback : fallback;
  }

  /**
   * パイプ破損による二次的な失敗かどうかを判定する。
   *
   * <p>後段コマンドの失敗に起因して前段が PipedOutputStream/PipedInputStream の IOException で失敗する場合、その例外はラップされた
   * StreamProcessingException として現れる。 ロケール依存のメッセージ文字列ではなく、スタックトレースのクラス名で判定する。
   */
  private static boolean isPipeBrokenCause(Throwable cause) {
    // 直接の cause が pipe 系 IO エラー
    if (cause instanceof IOException && isPipedStreamIOException((IOException) cause)) {
      return true;
    }
    // StreamProcessingException にラップされた pipe 系エラー
    if (cause instanceof StreamProcessingException) {
      Throwable inner = cause.getCause();
      return inner instanceof IOException && isPipedStreamIOException((IOException) inner);
    }
    return false;
  }

  /**
   * IOException が PipedInputStream/PipedOutputStream から送出されたものかを判定する。
   *
   * <p>JDK のロケールに依存しないようにスタックトレースのクラス名で判定する。
   */
  private static boolean isPipedStreamIOException(IOException e) {
    for (StackTraceElement frame : e.getStackTrace()) {
      String cls = frame.getClassName();
      if (cls.equals("java.io.PipedInputStream") || cls.equals("java.io.PipedOutputStream")) {
        return true;
      }
    }
    return false;
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
