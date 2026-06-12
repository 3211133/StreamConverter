package com.streamconverter.command.impl.csv;

import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * A {@link Reader} wrapper that records {@link IOException}s raised by read operations and rethrows
 * them when the reader is closed.
 *
 * <p>opencsv 5.x's {@code CSVReader.readNext()} does not propagate an {@link IOException} thrown by
 * the underlying {@link Reader}; instead it treats the failure as end-of-stream and returns {@code
 * null}, as if the input ended normally. This makes a genuine I/O failure indistinguishable from a
 * normal end-of-file from the caller's perspective.
 *
 * <p>This wrapper records the first {@link IOException} raised by any read operation. When the
 * reader is closed (for example, via try-with-resources), it rethrows a new {@link IOException}
 * wrapping the recorded failure, ensuring that a suppressed I/O failure is surfaced rather than
 * silently treated as a normal end-of-stream.
 */
final class FaultReportingReader extends FilterReader {

  private IOException fault;

  /**
   * Constructs a new FaultReportingReader wrapping the given reader.
   *
   * @param in the underlying reader
   */
  FaultReportingReader(Reader in) {
    super(in);
  }

  /**
   * Creates a FaultReportingReader that decodes the given input stream as UTF-8.
   *
   * @param in the underlying input stream
   * @return a FaultReportingReader wrapping a UTF-8 {@link InputStreamReader} over {@code in}
   */
  static FaultReportingReader forUtf8(InputStream in) {
    return new FaultReportingReader(new InputStreamReader(in, StandardCharsets.UTF_8));
  }

  @Override
  public int read() throws IOException {
    try {
      return super.read();
    } catch (IOException e) {
      recordFault(e);
      throw e;
    }
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    try {
      return super.read(cbuf, off, len);
    } catch (IOException e) {
      recordFault(e);
      throw e;
    }
  }

  @Override
  public long skip(long n) throws IOException {
    try {
      return super.skip(n);
    } catch (IOException e) {
      recordFault(e);
      throw e;
    }
  }

  @Override
  public boolean ready() throws IOException {
    try {
      return super.ready();
    } catch (IOException e) {
      recordFault(e);
      throw e;
    }
  }

  private void recordFault(IOException e) {
    if (fault == null) {
      fault = e;
    }
  }

  /**
   * Closes the underlying reader.
   *
   * <p>If a read operation previously failed with an {@link IOException}, that failure is rethrown
   * here as a new {@link IOException} wrapping the recorded fault, even if {@code super.close()}
   * itself completes normally. If {@code super.close()} also fails, the recorded fault (if any) is
   * attached to the close failure as a suppressed exception.
   *
   * <p>A recorded fault is reported only once: closing an already-closed reader does not rethrow
   * it. This keeps {@code close()} idempotent when the reader is registered both as its own
   * try-with-resources resource and as the delegate of a closeable consumer (such as {@code
   * CSVReader}) that closes it first.
   *
   * @throws IOException if the underlying reader fails to close, or if a read operation previously
   *     failed and was not propagated by the caller
   */
  @Override
  public void close() throws IOException {
    try {
      super.close();
    } catch (IOException closeFailure) {
      if (fault != null) {
        closeFailure.addSuppressed(fault);
        fault = null;
      }
      throw closeFailure;
    }
    if (fault != null) {
      IOException reported =
          new IOException(
              "I/O read failure was suppressed by a downstream consumer: " + fault.getMessage(),
              fault);
      fault = null;
      throw reported;
    }
  }
}
