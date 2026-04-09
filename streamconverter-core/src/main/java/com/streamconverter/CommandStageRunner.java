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

  interface AsyncRunner {
    CompletableFuture<Void> runAsync(Runnable runnable);
  }

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

  private void abortAllPipes(List<AbortablePipedStream> pipes) {
    for (AbortablePipedStream pipe : pipes) {
      pipe.abort();
    }
  }
}
