package com.streamconverter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Waits for all pipeline stages to complete and handles wait-time interruption. */
final class PipelineCompletionMonitor {
  /**
   * Waits for every stage future to finish before failure causes are inspected.
   *
   * <p>If the waiting thread is interrupted, all unfinished stage futures are cancelled and the
   * interrupt status is restored before the pipeline fails.
   *
   * @param futures stage completion futures
   * @throws ExecutionException if any stage completed exceptionally
   */
  void await(List<CompletableFuture<Void>> futures) throws ExecutionException {
    try {
      CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
    } catch (InterruptedException interruptedException) {
      cancelRemainingFutures(futures);
      Thread.currentThread().interrupt();
      throw new StreamProcessingException("Pipeline execution was interrupted");
    }
  }

  /** Cancels unfinished stage futures after interruption. */
  private void cancelRemainingFutures(List<CompletableFuture<Void>> futures) {
    for (CompletableFuture<Void> future : futures) {
      future.cancel(true);
    }
  }
}
