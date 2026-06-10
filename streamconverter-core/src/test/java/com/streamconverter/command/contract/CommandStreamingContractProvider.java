package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;
import java.io.IOException;

/**
 * Provides the command instance and probe input used by the streaming contract tests.
 *
 * <p>The probe intentionally blocks the input stream after an initial chunk and observes whether
 * the command starts writing output before the remaining input is released. Providers therefore
 * need to supply:
 *
 * <ul>
 *   <li>a real command instance
 *   <li>sample input representative of the command's expected data format
 *   <li>a classification that reflects confirmed behavior, not assumptions
 * </ul>
 */
interface CommandStreamingContractProvider {

  IStreamCommand createCommand() throws IOException;

  byte[] sampleInput();

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
