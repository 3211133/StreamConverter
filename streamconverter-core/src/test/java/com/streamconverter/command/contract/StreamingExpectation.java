package com.streamconverter.command.contract;

/** Expected first-write behavior for a command under the streaming contract probe. */
enum StreamingExpectation {
  MUST_WRITE_BEFORE_INPUT_COMPLETE,
  EXEMPT_FROM_STREAMING_CONTRACT
}
