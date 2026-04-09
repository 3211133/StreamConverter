package com.streamconverter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Waits for all pipeline stages to complete and delegates failure handling. */
final class PipelineCompletionMonitor {
  private final PipelineFailureHandler pipelineFailureHandler;

  PipelineCompletionMonitor(PipelineFailureHandler pipelineFailureHandler) {
    this.pipelineFailureHandler = pipelineFailureHandler;
  }

  void await(List<CompletableFuture<Void>> futures) throws ExecutionException {
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
    } catch (InterruptedException interruptedException) {
      pipelineFailureHandler.handleInterrupted(futures, 0);
    }
  }
}
