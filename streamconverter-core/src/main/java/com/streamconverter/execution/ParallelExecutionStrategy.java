package com.streamconverter.execution;

import com.streamconverter.StreamProcessingException;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;
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
 * 仮想スレッドを使用した並列パイプライン実行戦略。
 *
 * <p>各コマンドを独立した仮想スレッドで実行し、{@link PipedInputStream}/{@link PipedOutputStream}
 * でコマンド間をストリーミング接続する。これにより大容量データをメモリに全て持つことなく処理できる。
 *
 * <p>これは {@code StreamConverter} のデフォルト実行戦略である。
 */
public final class ParallelExecutionStrategy implements ExecutionStrategy {

  private static final Logger LOG = LoggerFactory.getLogger(ParallelExecutionStrategy.class);

  /** AutoCloseableラッパーでExecutorServiceのリソース管理を改善 */
  private static final class AutoCloseableExecutorService implements AutoCloseable {
    private final ExecutorService executor;

    AutoCloseableExecutorService(ExecutorService executor) {
      this.executor = executor;
    }

    CompletableFuture<Void> runAsync(Runnable runnable) {
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

  @Override
  public void execute(
      List<IStreamCommand> commands,
      List<String> commandNames,
      InputStream inputStream,
      OutputStream outputStream,
      BufferPolicy bufferPolicy)
      throws IOException {

    PipelineContext pipelineContext = new PipelineContext();
    List<CompletableFuture<Void>> futures = new ArrayList<>();
    // CopyOnWriteArrayList: パイプライン構築中（メインスレッド）と
    // 失敗時のクローズ処理（ワーカースレッド）が並行して安全にアクセスできるようにする
    List<AutoCloseable> resources = new CopyOnWriteArrayList<>();

    try (AutoCloseableExecutorService executor =
        new AutoCloseableExecutorService(createOptimalExecutor())) {
      InputStream currentInput = inputStream;

      // パイプライン構築
      for (int i = 0; i < commands.size(); i++) {
        IStreamCommand command = commands.get(i);
        final String commandName = commandNames.get(i);
        final InputStream commandInput = currentInput;
        final OutputStream commandOutput;

        if (i == commands.size() - 1) {
          // 最後のコマンド
          commandOutput = outputStream;
        } else {
          // 中間コマンド: 次のコマンド用にPipedStreamペア作成
          PipedOutputStream pipedOut = new PipedOutputStream();
          PipedInputStream pipedIn =
              new PipedInputStream(pipedOut, bufferPolicy.getBufferSizeBytes());
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
                    command.execute(commandInput, commandOutput);

                    if (commandOutput instanceof PipedOutputStream) {
                      commandOutput.close();
                    }

                  } catch (StreamProcessingException e) {
                    closePipedOutputIfNeeded(commandOutput);
                    throw e;
                  } catch (IOException | RuntimeException e) {
                    closePipedOutputIfNeeded(commandOutput);
                    throw new StreamProcessingException(
                        "Command execution failed: " + commandName + " - " + e.getMessage(), e);
                  } catch (Error e) {
                    closePipedOutputIfNeeded(commandOutput);
                    throw e;
                  } finally {
                    PipelineContext.clear();
                    MDC.clear();
                  }
                });

        futures.add(future);
      }

      // パイプライン構築完了後に exceptionally ハンドラを登録する
      for (CompletableFuture<Void> future : futures) {
        future.exceptionally(
            t -> {
              closeResources(resources);
              return null;
            });
      }

      // すべてのタスクの完了を待機
      for (int i = 0; i < futures.size(); i++) {
        try {
          futures.get(i).get();
        } catch (ExecutionException e) {
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
      closeResources(resources);
    }
  }

  private ExecutorService createOptimalExecutor() {
    ThreadFactory threadFactory = Thread.ofVirtual().name("stream-converter-", 0).factory();
    return Executors.newThreadPerTaskExecutor(threadFactory);
  }

  private static void closePipedOutputIfNeeded(OutputStream commandOutput) {
    if (commandOutput instanceof PipedOutputStream) {
      try {
        commandOutput.close();
      } catch (IOException ignored) {
        // ignored
      }
    }
  }

  private Throwable findFirstFailureCause(
      List<CompletableFuture<Void>> futures, Throwable fallback) {
    Throwable ioFallback = null;
    for (CompletableFuture<Void> f : futures) {
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

  private static boolean isPipeBrokenCause(Throwable cause) {
    if (cause instanceof IOException && isPipedStreamIOException((IOException) cause)) {
      return true;
    }
    if (cause instanceof StreamProcessingException) {
      Throwable inner = cause.getCause();
      return inner instanceof IOException && isPipedStreamIOException((IOException) inner);
    }
    return false;
  }

  private static boolean isPipedStreamIOException(IOException e) {
    for (StackTraceElement frame : e.getStackTrace()) {
      String cls = frame.getClassName();
      if (cls.equals("java.io.PipedInputStream") || cls.equals("java.io.PipedOutputStream")) {
        return true;
      }
    }
    return false;
  }

  private void cancelRemainingFutures(List<CompletableFuture<Void>> futures, int fromIndex) {
    for (int j = fromIndex; j < futures.size(); j++) {
      futures.get(j).cancel(true);
    }
  }

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
