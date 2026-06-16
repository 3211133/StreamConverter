package com.streamconverter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for aggregated independent failures from converter-level pipeline execution.
 *
 * <p>When a pipeline runs multiple commands or stages in parallel, multiple independent failures
 * can occur simultaneously. This exception wraps all independent failures (末端型のみ) and delivers them
 * to the main layer as a single exception.
 *
 * <p>Each individual failure is a concrete subtype of {@link StreamProcessingException}: {@link
 * UserInputException}, {@link ExternalTransientException}, {@link ExternalPermanentException}, or
 * {@link InternalSystemException}.
 *
 * <p><strong>Contract: No nested aggregates.</strong> The failures list never contains another
 * {@code AggregatedStreamProcessingException}. The converter layer must flatten nested aggregates
 * by extracting their underlying failures.
 *
 * <p>Main-layer usage example:
 *
 * <pre>{@code
 * try {
 *   converter.execute(...);
 * } catch (StreamProcessingException e) {
 *   // Safely extract all failures
 *   List<StreamProcessingException> failures =
 *       (e instanceof AggregatedStreamProcessingException agg)
 *         ? agg.getAllFailures()
 *         : List.of(e);
 *
 *   // Prioritize user-input failures for display
 *   failures.stream()
 *       .filter(f -> f instanceof UserInputException)
 *       .findFirst()
 *       .or(() -> Optional.of(failures.get(0)))
 *       .ifPresent(f -> showUser(f.getUserMessage()));
 *
 *   // Log all failures for diagnosis
 *   failures.forEach(f -> log.error("pipeline failure", f));
 * }
 * }</pre>
 *
 * <p>Extends {@link StreamProcessingException}, and therefore {@link IOException}, so that callers
 * of {@link com.streamconverter.command.IStreamCommand#execute} can handle all stream-level
 * failures with a single {@code catch (IOException)} block.
 *
 * <p>See docs/reference/EXCEPTION_POLICY.md for the full exception classification policy.
 *
 * @see StreamProcessingException
 * @see UserInputException
 * @see ExternalTransientException
 * @see ExternalPermanentException
 * @see InternalSystemException
 */
public final class AggregatedStreamProcessingException extends StreamProcessingException {
  private static final long serialVersionUID = 1L;

  private final List<StreamProcessingException> failures;

  /**
   * Constructs an AggregatedStreamProcessingException from a list of independent failures.
   *
   * <p><strong>Precondition: failures is non-empty and contains no nested
   * AggregatedStreamProcessingException instances.</strong>
   *
   * @param failures non-empty list of independent {@link StreamProcessingException} failures (末端型:
   *     {@link UserInputException}, {@link ExternalTransientException}, {@link
   *     ExternalPermanentException}, or {@link InternalSystemException})
   * @throws IllegalArgumentException if failures is empty or contains nested aggregates
   */
  public AggregatedStreamProcessingException(List<StreamProcessingException> failures) {
    super(
        "Pipeline encountered " + failures.size() + " failure(s)",
        failures.isEmpty() ? null : failures.get(0));

    // Validation
    if (failures.isEmpty()) {
      throw new IllegalArgumentException("failures list cannot be empty");
    }
    for (StreamProcessingException failure : failures) {
      if (failure instanceof AggregatedStreamProcessingException) {
        throw new IllegalArgumentException(
            "nested AggregatedStreamProcessingException not allowed; converter must flatten");
      }
    }

    this.failures = Collections.unmodifiableList(new ArrayList<>(failures));
  }

  /**
   * Returns all independent failures that occurred during pipeline execution.
   *
   * <p><strong>Order preservation:</strong> Failures are listed in command order (upstream to
   * downstream). Within a single command stage, order among multiple simultaneous failures is
   * implementation-dependent. For deterministic ordering (e.g., by timestamp), main-layer code
   * should sort the list.
   *
   * <p><strong>Contents:</strong> Never contains {@code AggregatedStreamProcessingException}
   * (converted by converter layer). Contains only末端型 (concrete notification classes).
   *
   * @return unmodifiable list of independent failures (never empty, never contains aggregates)
   */
  public List<StreamProcessingException> getAllFailures() {
    return failures;
  }

  /**
   * Returns the base default user message.
   *
   * <p><strong>NOTE:</strong> This is an interim implementation pending decision on issue #797 (未決
   * 4.3.2). Currently returns the base {@link StreamProcessingException} default message.
   *
   * <p>Main-layer code should <strong>not</strong> rely on this method for user display. Instead,
   * call {@link #getAllFailures()}, filter for a representative failure (e.g., prioritizing {@link
   * UserInputException}), and use that failure's {@code getUserMessage()}. This pattern is
   * demonstrated in the class documentation.
   *
   * @return {@link StreamProcessingException#DEFAULT_USER_MESSAGE}
   */
  @Override
  @SuppressWarnings("PMD.UselessOverridingMethod")
  public String getUserMessage() {
    return super.getUserMessage();
  }
}
