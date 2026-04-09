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

  Void cleanupOnAsyncFailure(List<AutoCloseable> resources) {
    try {
      closeResources(resources);
    } catch (RuntimeException ex) {
      logger.error("Unexpected error during resource cleanup on pipeline failure", ex);
    }
    return null;
  }

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

  void handleInterrupted(List<CompletableFuture<Void>> futures, int fromIndex) {
    cancelRemainingFutures(futures, fromIndex);
    Thread.currentThread().interrupt();
    throw new StreamProcessingException("Pipeline execution was interrupted");
  }

  void closeResources(List<AutoCloseable> resources) {
    for (AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (Exception e) {
        logger.warn("Failed to close resource [{}]", resource.getClass().getSimpleName(), e);
      }
    }
  }

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

  static boolean isPipeAbortedCause(Throwable cause) {
    if (cause instanceof PipeAbortedException) {
      return true;
    }
    if (cause instanceof StreamProcessingException) {
      return cause.getCause() instanceof PipeAbortedException;
    }
    return false;
  }

  private void cancelRemainingFutures(List<CompletableFuture<Void>> futures, int fromIndex) {
    for (int i = fromIndex; i < futures.size(); i++) {
      futures.get(i).cancel(true);
    }
  }
}
