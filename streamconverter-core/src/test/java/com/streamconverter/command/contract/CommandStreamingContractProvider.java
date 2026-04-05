package com.streamconverter.command.contract;

import com.streamconverter.command.IStreamCommand;

/** Provides the command instance and probe input used by the streaming contract tests. */
interface CommandStreamingContractProvider {

  IStreamCommand createCommand();

  byte[] sampleInput();

  int firstChunkSize();

  StreamingExpectation expectation();

  default String exemptionReason() {
    return "";
  }
}
