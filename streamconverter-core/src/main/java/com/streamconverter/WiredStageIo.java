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
  private final InputStream input;
  private final OutputStream output;
  private final AbortablePipedStream pipe;

  WiredStageIo(InputStream input, OutputStream output, AbortablePipedStream pipe) {
    this.input = input;
    this.output = output;
    this.pipe = pipe;
  }

  /** Returns the input stream connected to this stage. */
  InputStream input() {
    return input;
  }

  /** Returns the output stream connected to this stage. */
  OutputStream output() {
    return output;
  }

  /** Returns the intermediate pipe for this stage, or {@code null} for the terminal stage. */
  AbortablePipedStream pipe() {
    return pipe;
  }
}
