package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;

/**
 * Provides the command instance and probe input used by the streaming contract tests.
 *
 * <p>The probe intentionally blocks the input stream after an initial chunk and observes whether
 * the command starts writing output before the remaining input is released. Providers therefore
 * need to supply:
 *
 * <ul>
 *   <li>a real command instance
 *   <li>sample input large enough to exercise the command beyond its first read
 *   <li>a first-chunk size that leaves meaningful unread input behind for the probe
 *   <li>a classification that reflects confirmed behavior, not assumptions
 * </ul>
 */
interface CommandStreamingContractProvider {

  IStreamCommand createCommand();

  byte[] sampleInput();

  int firstChunkSize();

  StreamingExpectation expectation();

  /** Required for every non-compliant classification so future readers know why it is not green. */
  default String exemptionReason() {
    return "";
  }

  /** Rare escape hatch for commands that cannot be executed by this in-process probe. */
  default boolean supportsProbeExecution() {
    return true;
  }

  /** Required when {@link #supportsProbeExecution()} is false. */
  default String probeSkipReason() {
    return "";
  }
}
