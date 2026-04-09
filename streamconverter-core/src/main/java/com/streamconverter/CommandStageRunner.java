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
   * Starts every stage defined by the plan and returns their completion futures.
   *
   * <p>Each stage receives the same pipeline context snapshot, while per-stage cleanup is handled
   * by {@link #startStage(PipelinePlan.StageSpec, AsyncRunner, PipelineContext, List)}.
   */
  List<CompletableFuture<Void>> startAll(
      PipelinePlan plan, AsyncRunner asyncRunner, PipelineContext pipelineContext) {
    List<CompletableFuture<Void>> futures = new ArrayList<>(plan.stageSpecs().size());
    for (PipelinePlan.StageSpec stageSpec : plan.stageSpecs()) {
      futures.add(startStage(stageSpec, asyncRunner, pipelineContext, plan.pipes()));
    }
    return futures;
  }

  private CompletableFuture<Void> startStage(
      PipelinePlan.StageSpec stageSpec,
      AsyncRunner asyncRunner,
      PipelineContext pipelineContext,
      List<AbortablePipedStream> pipes) {
    return asyncRunner.runAsync(
        () -> {
          PipelineContext.set(pipelineContext);
          try {
            executeStage(stageSpec, pipes);
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
  private void executeStage(PipelinePlan.StageSpec stageSpec, List<AbortablePipedStream> pipes) {
    IStreamCommand command = stageSpec.command();
    String commandName = stageSpec.commandName();

    try {
      command.execute(stageSpec.input(), stageSpec.output());

      if (stageSpec.pipe() != null) {
        try {
          stageSpec.output().close();
        } catch (IOException closeEx) {
          abortAllPipes(pipes);
          throw new StreamProcessingException(
              "Failed to close output stream of command: " + commandName, closeEx);
        }
      }

    } catch (StreamProcessingException e) {
      abortAllPipes(pipes);
      throw e;
    } catch (IOException | RuntimeException e) {
      abortAllPipes(pipes);
      throw new StreamProcessingException(
          "Command execution failed: " + commandName + " - " + e.getMessage(), e);
    } catch (Error e) {
      abortAllPipes(pipes);
      throw e;
    }
  }

  /** Aborts every intermediate pipe so dependent stages stop waiting on stream activity. */
  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream pipe : pipes) {
      pipe.abort();
    }
  }
}
