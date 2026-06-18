package com.streamconverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** Translates stage failures into the exception surfaced by {@link StreamConverter}. */
final class PipelineFailureHandler {
  /**
   * Rethrows the pipeline failure after inspecting all completed stage futures.
   *
   * <p>Classified failures ({@link StreamProcessingException} subtypes) are aggregated into a
   * single {@link AggregatedStreamProcessingException} before being thrown. Unclassified failures
   * ({@link Error}, {@link RuntimeException}, raw {@link IOException}) propagate unchanged per
   * EXCEPTION_POLICY.md §4.1.1 A1: blanket-wrapping should-never-happen unchecked exceptions into
   * A-class is prohibited inside the library; only truly-unreachable guard sites may do so.
   *
   * <p>Pipe-aborted failures (secondary consequences of another stage's failure) are filtered out
   * regardless of classification.
   *
   * @param executionException failure reported by {@code CompletableFuture.allOf(...).get()}
   * @param futures completed stage futures to inspect
   * @throws AggregatedStreamProcessingException when all independent failures are classified
   * @throws IOException (and its subtypes) for unclassified or empty-collection fallback
   */
  void rethrowExecutionFailure(
      ExecutionException executionException, List<CompletableFuture<Void>> futures)
      throws IOException {
    CollectedFailures collected = collectIndependentFailures(futures);

    // Unclassified failures propagate unchanged (§4.1.1 open question).
    if (collected.unclassified != null) {
      throwRaw(collected.unclassified);
    }

    if (collected.classified.isEmpty()) {
      // No independent failures could be identified; fall back to the original cause.
      Throwable rawCause = unwrapCarrier(executionException.getCause());
      if (rawCause instanceof StreamProcessingException spe) {
        throw new AggregatedStreamProcessingException(List.of(spe));
      }
      throwRaw(rawCause);
      // Unreachable in practice; guards against an unknown Throwable subtype.
      throw new InternalSystemException("コマンド実行中に予期せぬエラーが発生しました", rawCause);
    }

    throw new AggregatedStreamProcessingException(collected.classified);
  }

  private static void throwRaw(Throwable cause) throws IOException {
    if (cause instanceof Error err) {
      throw err;
    }
    if (cause instanceof IOException ioe) {
      throw ioe;
    }
    if (cause instanceof RuntimeException re) {
      throw re;
    }
    // Truly-unreachable guard: Throwable that is neither Error, IOException, nor RuntimeException.
    // Per §4.1.1 A1, this site qualifies as a guard for an impossible Throwable subtype.
    throw new InternalSystemException("コマンド実行中に予期せぬエラーが発生しました", cause);
  }

  /** Holder for the two categories of independent failures found across stage futures. */
  private static final class CollectedFailures {
    final List<StreamProcessingException> classified = new ArrayList<>();

    /** The first unclassified failure encountered, or null if all were classified. */
    Throwable unclassified;
  }

  /**
   * Inspects all completed stage futures and separates failures into classified vs. unclassified.
   *
   * <p>Pipe-aborted secondary failures are filtered before categorization. Classified failures
   * ({@link StreamProcessingException} subtypes) are collected for aggregation. The first
   * unclassified failure short-circuits further collection and is returned as {@code
   * CollectedFailures#unclassified}.
   */
  private CollectedFailures collectIndependentFailures(List<CompletableFuture<Void>> futures) {
    CollectedFailures result = new CollectedFailures();
    for (CompletableFuture<Void> future : futures) {
      if (!future.isCompletedExceptionally()) {
        continue;
      }
      try {
        future.get();
      } catch (ExecutionException executionException) {
        Throwable cause = unwrapCarrier(executionException.getCause());
        if (isPipeAbortedCause(cause)) {
          continue;
        }
        if (cause instanceof StreamProcessingException spe) {
          result.classified.add(spe);
        } else {
          result.unclassified = cause;
          return result;
        }
      } catch (InterruptedException interruptedException) {
        Thread.currentThread().interrupt();
        result.classified.add(
            new InternalSystemException("コマンド実行中に予期せぬエラーが発生しました", interruptedException));
      }
    }
    return result;
  }

  /**
   * Unwraps the async-boundary carrier so callers see the original checked failure.
   *
   * <p>{@link CommandStageRunner} moves {@link IOException} failures across the {@code Runnable}
   * boundary inside an {@link UncheckedStreamException}; root-cause inspection must look at the
   * carried exception, not the carrier.
   */
  private static Throwable unwrapCarrier(Throwable cause) {
    return cause instanceof UncheckedStreamException carrier ? carrier.getCause() : cause;
  }

  /**
   * Returns whether the failure was caused by an upstream abort rather than by the stage itself.
   *
   * <p>{@link PipeAbortedException} marks secondary failures created when another stage already
   * failed and the pipeline actively closed its intermediate pipes.
   */
  static boolean isPipeAbortedCause(Throwable cause) {
    return cause instanceof PipeAbortedException
        || (cause instanceof StreamProcessingException
            && cause.getCause() instanceof PipeAbortedException);
  }
}
