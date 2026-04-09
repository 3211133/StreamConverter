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
      PipelinePlan plan,
      AsyncRunner asyncRunner,
      PipelineContext pipelineContext) {
    List<CompletableFuture<Void>> futures = new ArrayList<>(plan.stageIos().size());
    for (int i = 0; i < plan.stageIos().size(); i++) {
      futures.add(
          startStage(
              commands.get(i), plan.stageIos().get(i), asyncRunner, pipelineContext, plan.pipes()));
    }
    return futures;
  }

  private CompletableFuture<Void> startStage(
      IStreamCommand command,
      WiredStageIo stageIo,
      AsyncRunner asyncRunner,
      PipelineContext pipelineContext,
      List<AbortablePipedStream> pipes) {
    return asyncRunner.runAsync(
        () -> {
          PipelineContext.set(pipelineContext);
          try {
            executeStage(command, stageIo, pipes);
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
      IStreamCommand command, WiredStageIo stageIo, List<AbortablePipedStream> pipes) {
    String commandLabel = resolveCommandLabel(command);
    try {
      command.execute(stageIo.input(), stageIo.output());

      if (stageIo.pipe() != null) {
        try {
          stageIo.output().close();
        } catch (IOException closeEx) {
          abortAllPipes(pipes);
          throw new StreamProcessingException(
              "Failed to close output stream of command: " + commandLabel, closeEx);
        }
      }

    } catch (StreamProcessingException e) {
      abortAllPipes(pipes);
      throw e;
    } catch (IOException | RuntimeException e) {
      abortAllPipes(pipes);
      throw new StreamProcessingException(
          "Command execution failed: " + commandLabel + " - " + e.getMessage(), e);
    } catch (Error e) {
      abortAllPipes(pipes);
      throw e;
    }
  }

  /** Derives a best-effort command label for diagnostics without storing parallel name state. */
  private String resolveCommandLabel(IStreamCommand command) {
    Class<?> implClass = command.getClass();
    if (implClass.isSynthetic()) {
      return "IStreamCommand";
    }
    String simpleName = implClass.getSimpleName();
    return simpleName.isEmpty() ? "IStreamCommand" : simpleName;
  }

  /** Aborts every intermediate pipe so dependent stages stop waiting on stream activity. */
  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream pipe : pipes) {
      pipe.abort();
    }
  }
}
