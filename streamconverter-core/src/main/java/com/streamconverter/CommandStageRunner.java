package com.streamconverter;

import com.streamconverter.command.IStreamCommand;
import com.streamconverter.context.PipelineContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.MDC;

/** Starts all stage commands for a prepared {@link PipelinePlan}. */
final class CommandStageRunner {

  /** Abstraction over async task submission so stage startup stays executor-agnostic. */
  @FunctionalInterface
  interface AsyncRunner {
    /**
     * Submits a stage task for asynchronous execution.
     *
     * @param runnable stage work to execute
     * @return future representing the stage completion
     */
    CompletableFuture<Void> runAsync(Runnable runnable);
  }

  /**
   * Starts every stage defined by the command list and IO plan, then returns their completion
   * futures.
   *
   * <p>Each stage receives the same pipeline context snapshot, while per-stage cleanup is handled
   * by {@link #startStage(IStreamCommand, String, WiredStageIo, AsyncRunner, PipelineContext,
   * List)}.
   */
  List<CompletableFuture<Void>> startAll(
      List<IStreamCommand> commands,
      List<String> commandLabels,
      PipelinePlan plan,
      AsyncRunner asyncRunner) {
    PipelineContext pipelineContext = new PipelineContext();
    List<CompletableFuture<Void>> futures = new ArrayList<>(plan.stageIos().size());
    for (int i = 0; i < plan.stageIos().size(); i++) {
      futures.add(
          startStage(
              commands.get(i),
              commandLabels.get(i),
              plan.stageIos().get(i),
              asyncRunner,
              pipelineContext,
              plan.pipes()));
    }
    return futures;
  }

  private CompletableFuture<Void> startStage(
      IStreamCommand command,
      String commandLabel,
      WiredStageIo stageIo,
      AsyncRunner asyncRunner,
      PipelineContext pipelineContext,
      List<AbortablePipedStream> pipes) {
    return asyncRunner.runAsync(
        () -> {
          PipelineContext.set(pipelineContext);
          try {
            executeStage(command, commandLabel, stageIo, pipes);
          } finally {
            PipelineContext.clear();
            MDC.clear();
          }
        });
  }

  /**
   * Executes a single stage and translates failures into pipeline-level exceptions.
   *
   * <p>Intermediate stage outputs are closed on success to signal EOF to the downstream stage. Any
   * failure aborts all pipes so blocked readers and writers are released promptly.
   */
  private void executeStage(
      IStreamCommand command,
      String commandLabel,
      WiredStageIo stageIo,
      List<AbortablePipedStream> pipes) {
    try {
      command.execute(stageIo.input(), stageIo.output());
      closeStageOutput(stageIo);
    } catch (Throwable throwable) {
      abortAllPipes(pipes);
      throw toStageFailure(throwable, commandLabel);
    }
  }

  /**
   * Closes intermediate stage output to signal EOF to the downstream command.
   *
   * @throws StreamProcessingException if the output cannot be closed cleanly
   */
  private void closeStageOutput(WiredStageIo stageIo) throws IOException {
    if (stageIo.pipe() == null) {
      return;
    }
    try {
      stageIo.output().close();
    } catch (IOException closeEx) {
      throw new InternalSystemException("ステージ出力ストリームのクローズに失敗しました", closeEx);
    }
  }

  /**
   * Classifies a stage failure per the exception policy (docs/reference/EXCEPTION_POLICY.md) and
   * returns it as an unchecked throwable suitable for the async stage boundary.
   *
   * <p>The propagated exception keeps its original type: input-data and I/O failures travel as
   * their own {@link IOException} subtype (carried across the {@code Runnable} boundary by {@link
   * UncheckedStreamException}), and runtime exceptions — implementation bugs by classification —
   * are returned as-is. The failing command is recorded as a suppressed {@link StageFailureContext}
   * instead of being embedded in a wrapper message.
   *
   * @throws Error when the stage failed with an unrecoverable JVM error
   */
  private RuntimeException toStageFailure(Throwable throwable, String commandLabel) {
    if (throwable instanceof Error error) {
      throw error;
    }
    Throwable failure =
        throwable instanceof UncheckedStreamException carrier ? carrier.getCause() : throwable;
    failure.addSuppressed(new StageFailureContext(commandLabel));
    if (failure instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    if (failure instanceof IOException ioException) {
      return new UncheckedStreamException(ioException);
    }
    return new UncheckedStreamException(new InternalSystemException("コマンド実行に失敗しました", failure));
  }

  /** Aborts every intermediate pipe so dependent stages stop waiting on stream activity. */
  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream pipe : pipes) {
      pipe.abort();
    }
  }
}
