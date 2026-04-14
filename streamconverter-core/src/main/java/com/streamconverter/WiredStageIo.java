package com.streamconverter;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Immutable IO wiring for one command stage in the pipeline.
 *
 * <p>Each stage knows the connected input/output streams and the intermediate pipe it owns when it
 * is not the terminal stage.
 */
final class WiredStageIo {
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private final AbortablePipedStream ownedPipe;

  WiredStageIo(InputStream input, OutputStream output, AbortablePipedStream pipe) {
    this.inputStream = input;
    this.outputStream = output;
    this.ownedPipe = pipe;
  }

  /** Returns the input stream connected to this stage. */
  InputStream input() {
    return inputStream;
  }

  /** Returns the output stream connected to this stage. */
  OutputStream output() {
    return outputStream;
  }

  /** Returns the intermediate pipe for this stage, or {@code null} for the terminal stage. */
  AbortablePipedStream pipe() {
    return ownedPipe;
  }
}
