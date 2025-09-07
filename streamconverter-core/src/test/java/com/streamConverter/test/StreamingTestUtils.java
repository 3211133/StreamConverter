package com.streamconverter.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Utility classes for streaming behavior verification tests. These classes help monitor read/write
 * operations during stream processing to verify that commands process data incrementally rather
 * than loading everything into memory.
 */
public class StreamingTestUtils {

  /**
   * Custom InputStream that tracks read operations for streaming behavior verification. Monitors
   * when bytes are read and when the stream is fully consumed.
   */
  public static class TrackingInputStream extends ByteArrayInputStream {
    private long fullyReadTime = -1;
    private final int totalBytes;
    private int bytesRead = 0;

    public TrackingInputStream(byte[] buf) {
      super(buf);
      this.totalBytes = buf.length;
    }

    @Override
    public int read() {
      int result = super.read();
      if (result != -1) {
        bytesRead++;
      } else if (fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return result;
    }

    @Override
    public int read(byte[] b, int off, int len) {
      int bytesActuallyRead = super.read(b, off, len);
      if (bytesActuallyRead > 0) {
        bytesRead += bytesActuallyRead;
      }
      if (bytesActuallyRead == -1 && fullyReadTime == -1) {
        fullyReadTime = System.nanoTime();
      }
      return bytesActuallyRead;
    }

    @Override
    public byte[] readAllBytes() {
      // Some commands like JsonValidateCommand use readAllBytes(), so we need to track this
      byte[] result = super.readAllBytes();
      if (result.length > 0) {
        bytesRead += result.length;
        // If we read all bytes and reached the end, mark as fully read
        if (available() == 0) {
          fullyReadTime = System.nanoTime();
        }
      }
      return result;
    }

    public boolean isFullyRead() {
      return fullyReadTime != -1;
    }

    public long getFullyReadTime() {
      return fullyReadTime;
    }

    public int getBytesRead() {
      return bytesRead;
    }

    public int getTotalBytes() {
      return totalBytes;
    }

    public double getReadProgress() {
      return totalBytes > 0 ? (double) bytesRead / totalBytes : 0.0;
    }
  }

  /**
   * Custom OutputStream that monitors write operations and timing. Records when the first write
   * occurs to verify streaming behavior.
   */
  public static class MonitoringOutputStream extends ByteArrayOutputStream {
    private long firstWriteTime = -1;
    private boolean hasWriteOccurred = false;

    @Override
    public void write(int b) {
      recordFirstWrite();
      super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      recordFirstWrite();
      super.write(b, off, len);
    }

    private void recordFirstWrite() {
      if (!hasWriteOccurred) {
        firstWriteTime = System.nanoTime();
        hasWriteOccurred = true;
      }
    }

    public boolean hasWriteOccurred() {
      return hasWriteOccurred;
    }

    public long getFirstWriteTime() {
      return firstWriteTime;
    }

    public String getContent() {
      return toString(StandardCharsets.UTF_8);
    }

    public String getContent(String encoding) {
      try {
        return toString(encoding);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException("Unsupported encoding: " + encoding, e);
      }
    }
  }

  /**
   * Functional interface to run code under test with provided streams that may throw IOException.
   */
  @FunctionalInterface
  public interface IOStreamConsumer {
    void accept(java.io.InputStream in, java.io.OutputStream out) throws java.io.IOException;
  }

  /**
   * InputStream that triggers an assertion once half of the input has been read, ensuring that
   * output has already been produced by that time. Also fails fast if readAllBytes() is invoked to
   * discourage non-streaming implementations in code under test.
   */
  public static class HalfwayAssertingInputStream extends ByteArrayInputStream {
    private final MonitoringOutputStream monitoringOutput;
    private final int totalBytes;
    private int bytesRead = 0;
    private boolean halfwayAsserted = false;

    public HalfwayAssertingInputStream(byte[] data, MonitoringOutputStream monitoringOutput) {
      super(data);
      this.monitoringOutput = monitoringOutput;
      this.totalBytes = data.length;
    }

    @Override
    public synchronized int read() {
      int r = super.read();
      if (r != -1) {
        bytesRead++;
        maybeAssertAtHalfway();
      }
      return r;
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) {
      int n = super.read(b, off, len);
      if (n > 0) {
        bytesRead += n;
        maybeAssertAtHalfway();
      }
      return n;
    }

    @Override
    public byte[] readAllBytes() {
      throw new AssertionError(
          "readAllBytes() must not be used in streaming tests (use incremental reads)");
    }

    private void maybeAssertAtHalfway() {
      if (!halfwayAsserted && totalBytes > 0 && bytesRead >= totalBytes / 2) {
        halfwayAsserted = true;
        org.junit.jupiter.api.Assertions.assertTrue(
            monitoringOutput.size() > 0, "OutputStream should have emitted data by halfway point");
      }
    }

    public boolean isHalfwayAsserted() {
      return halfwayAsserted;
    }
  }

  /**
   * Convenience runner: wires {@link HalfwayAssertingInputStream} and {@link
   * MonitoringOutputStream} and executes the provided consumer. Asserts that output is emitted by
   * halfway point and returns the final output content for optional further checks.
   */
  public static String runWithHalfwayAssertion(byte[] data, IOStreamConsumer runner)
      throws java.io.IOException {
    MonitoringOutputStream out = new MonitoringOutputStream();
    HalfwayAssertingInputStream in = new HalfwayAssertingInputStream(data, out);
    runner.accept(in, out);
    // Ensure the stream processing actually progressed to (or beyond) halfway for non-trivial data
    if (data.length > 0) {
      org.junit.jupiter.api.Assertions.assertTrue(
          in.isHalfwayAsserted(), "Halfway assertion should have been triggered during reading");
    }
    return out.getContent();
  }

  /**
   * Convenience runner: uses {@link TrackingInputStream} and {@link MonitoringOutputStream}, then
   * asserts that some output was written before the input was fully consumed. This is a slightly
   * relaxed streaming assertion suitable for commands that may buffer modestly but still stream
   * output progressively.
   */
  public static String runWithStreamingAssertion(byte[] data, IOStreamConsumer runner)
      throws java.io.IOException {
    TrackingInputStream in = new TrackingInputStream(data);
    MonitoringOutputStream out = new MonitoringOutputStream();
    runner.accept(in, out);
    org.junit.jupiter.api.Assertions.assertTrue(out.hasWriteOccurred(), "Output should be written");
    // If timing is available, ensure first write occurred before or by full read
    if (data.length > 0 && out.getFirstWriteTime() > 0 && in.getFullyReadTime() > 0) {
      org.junit.jupiter.api.Assertions.assertTrue(
          out.getFirstWriteTime() <= in.getFullyReadTime(),
          "First output should occur before or by the time input is fully read");
    }
    return out.getContent();
  }
}
