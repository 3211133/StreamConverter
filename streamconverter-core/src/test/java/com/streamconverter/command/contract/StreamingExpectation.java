package com.streamconverter.command.contract;

/**
 * Classification of a command's conformance to the streaming contract.
 *
 * <p>These values describe confirmed state, not provisional guesses.
 *
 * <ul>
 *   <li>{@link #STREAMING_COMPLIANT}: the probe was executed and observed output before the input
 *       completed
 *   <li>{@link #ALLOWED_FULL_BUFFERING}: the command is intentionally non-streaming and that
 *       behavior is explicitly allowed by project policy
 *   <li>{@link #KNOWN_STREAMING_VIOLATION}: the probe was executed and the command failed the
 *       streaming expectation
 * </ul>
 *
 * <p>{@code KNOWN_STREAMING_VIOLATION} must not be used as shorthand for "not verified yet".
 */
enum StreamingExpectation {
  STREAMING_COMPLIANT,
  ALLOWED_FULL_BUFFERING,
  KNOWN_STREAMING_VIOLATION
}
