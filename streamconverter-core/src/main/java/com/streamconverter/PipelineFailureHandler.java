package com.streamconverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;

/** Handles pipeline cleanup and failure translation for {@link StreamConverter}. */
final class PipelineFailureHandler {
  private final Logger logger;

  PipelineFailureHandler(Logger logger) {
    this.logger = logger;
  }

  /**
   * Best-effort cleanup hook for asynchronous stage failures.
   *
   * <p>This method is intended for use from future completion callbacks, so cleanup failures are
   * logged instead of being propagated back into the callback chain.
   */
  Void cleanupOnAsyncFailure(List<AutoCloseable> resources) {
    try {
      closeResources(resources);
    } catch (RuntimeException ex) {
      logger.error("Unexpected error during resource cleanup on pipeline failure", ex);
    }
    return null;
  }

  /**
   * Re-throws the primary execution failure after all stage futures have been inspected.
   *
   * <p>Secondary failures are attached as suppressed exceptions, while pipe-aborted failures are
   * treated as downstream consequences and filtered out.
   *
   * @param executionException failure reported by {@code CompletableFuture.allOf(...).get()}
   * @param futures completed stage futures to inspect
   * @throws IOException if the primary root cause is an I/O failure
   */
  void rethrowExecutionFailure(
      ExecutionException executionException, List<CompletableFuture<Void>> futures)
      throws IOException {
    List<Throwable> rootCauses = collectRootCauses(futures);
    if (rootCauses.isEmpty()) {
      throw new StreamProcessingException(
          "Unexpected error during command execution", executionException.getCause());
    }

    Throwable primary = rootCauses.get(0);
    for (int i = 1; i < rootCauses.size(); i++) {
      primary.addSuppressed(rootCauses.get(i));
    }

    if (primary instanceof Error err) {
      throw err;
    }
    if (primary instanceof StreamProcessingException spe) {
      throw spe;
    }
    if (primary instanceof IOException ioe) {
      throw ioe;
    }
    if (primary instanceof RuntimeException re) {
      throw re;
    }
    throw new StreamProcessingException("Unexpected error during command execution", primary);
  }

  /**
   * Cancels remaining futures, restores the interrupt flag, and fails the pipeline.
   *
   * @param futures stage futures to cancel
   * @param fromIndex first future index that should be cancelled
   */
  void handleInterrupted(List<CompletableFuture<Void>> futures, int fromIndex) {
    cancelRemainingFutures(futures, fromIndex);
    Thread.currentThread().interrupt();
    throw new StreamProcessingException("Pipeline execution was interrupted");
  }

  /**
   * Closes resources in order, logging and continuing if individual closes fail.
   *
   * @param resources resources associated with the current pipeline execution
   */
  void closeResources(List<AutoCloseable> resources) {
    for (AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (Exception e) {
        logger.warn("Failed to close resource [{}]", resource.getClass().getSimpleName(), e);
      }
    }
  }

  /**
   * Collects non-secondary failures from already completed stage futures.
   *
   * <p>Callers are expected to wait for all futures first so the collected failure set is stable.
   */
  private List<Throwable> collectRootCauses(List<CompletableFuture<Void>> futures) {
    List<Throwable> rootCauses = new ArrayList<>();
    for (CompletableFuture<Void> future : futures) {
      if (!future.isCompletedExceptionally()) {
        continue;
      }
      try {
        future.get();
      } catch (ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        if (!isPipeAbortedCause(cause)) {
          rootCauses.add(cause);
        }
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        rootCauses.add(interruptedException);
      }
    }
    return rootCauses;
  }

  /**
   * Returns whether the failure was caused by an upstream abort rather than by the stage itself.
   *
   * <p>{@link PipeAbortedException} marks secondary failures created when another stage already
   * failed and the pipeline actively closed its intermediate pipes.
   */
  static boolean isPipeAbortedCause(Throwable cause) {
    if (cause instanceof PipeAbortedException) {
      return true;
    }
    if (cause instanceof StreamProcessingException) {
      return cause.getCause() instanceof PipeAbortedException;
    }
    return false;
  }

  /** Cancels unfinished stage futures starting at the supplied index. */
  private void cancelRemainingFutures(List<CompletableFuture<Void>> futures, int fromIndex) {
    for (int i = fromIndex; i < futures.size(); i++) {
      futures.get(i).cancel(true);
    }
  }
}
