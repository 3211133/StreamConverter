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

  /**
   * Waits for every stage future to finish before failure causes are inspected.
   *
   * <p>Interrupt handling is centralized in {@link PipelineFailureHandler} so cancellation and
   * thread interrupt restoration follow the same policy everywhere.
   *
   * @param futures stage completion futures
   * @throws ExecutionException if any stage completed exceptionally
   */
  void await(List<CompletableFuture<Void>> futures) throws ExecutionException {
    try {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
    } catch (InterruptedException interruptedException) {
      pipelineFailureHandler.handleInterrupted(futures, 0);
    }
  }
}
