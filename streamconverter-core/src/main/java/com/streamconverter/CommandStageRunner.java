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
      closeStageOutput(stageIo, commandLabel);
    } catch (Throwable throwable) {
      abortAllPipes(pipes);
      throw rethrowStageFailure(throwable, commandLabel);
    }
  }

  /**
   * Closes intermediate stage output to signal EOF to the downstream command.
   *
   * @throws StreamProcessingException if the output cannot be closed cleanly
   */
  private void closeStageOutput(WiredStageIo stageIo, String commandLabel) {
    if (stageIo.pipe() == null) {
      return;
    }
    try {
      stageIo.output().close();
    } catch (IOException closeEx) {
      throw new StreamProcessingException(
          "Failed to close output stream of command: " + commandLabel, closeEx);
    }
  }

  /**
   * Converts a stage failure into the exception type surfaced by the pipeline.
   *
   * @throws Error when the stage failed with an unrecoverable JVM error
   */
  private RuntimeException rethrowStageFailure(Throwable throwable, String commandLabel) {
    if (throwable instanceof StreamProcessingException streamProcessingException) {
      return streamProcessingException;
    }
    if (throwable instanceof IOException || throwable instanceof RuntimeException) {
      return new StreamProcessingException(
          "Command execution failed: " + commandLabel + " - " + throwable.getMessage(), throwable);
    }
    if (throwable instanceof Error error) {
      throw error;
    }
    return new StreamProcessingException("Command execution failed: " + commandLabel, throwable);
  }

  /** Aborts every intermediate pipe so dependent stages stop waiting on stream activity. */
  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream pipe : pipes) {
      pipe.abort();
    }
  }
}
