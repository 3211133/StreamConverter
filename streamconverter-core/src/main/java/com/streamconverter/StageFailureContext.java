package com.streamconverter;

/**
 * Lightweight marker attached as a suppressed exception to a stage failure so the failing command
 * can be identified without changing the type or message of the propagated exception.
 *
 * <p>The exception policy (docs/reference/EXCEPTION_POLICY.md) requires the pipeline boundary to
 * propagate environmental I/O failures and implementation bugs with their original type. This
 * marker carries the command label that the previous wrapping behavior used to embed in the message
 * of a {@link StreamProcessingException}.
 *
 * <p>The stack trace is intentionally disabled: the marker only contributes its message and is
 * created on a failure path where the original exception already records the relevant frames.
 */
final class StageFailureContext extends Exception {
  private static final long serialVersionUID = 1L;

  StageFailureContext(String commandLabel) {
    super("Command execution failed: " + commandLabel, null, false, false);
  }
}
