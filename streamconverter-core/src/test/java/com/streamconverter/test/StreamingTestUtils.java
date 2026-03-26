package com.streamconverter.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
    private boolean readAllBytesCalled = false;

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
    public long transferTo(OutputStream out) throws IOException {
      // ByteArrayInputStream.transferTo() on Java 17+ uses System.arraycopy and bypasses
      // our read() overrides. Delegate through read(byte[], ...) so tracking still works.
      long transferred = 0;
      byte[] buffer = new byte[8192];
      int n;
      while ((n = this.read(buffer, 0, buffer.length)) != -1) {
        out.write(buffer, 0, n);
        transferred += n;
      }
      return transferred;
    }

    @Override
    public byte[] readAllBytes() {
      // Some commands use readAllBytes(), so we need to track this
      readAllBytesCalled = true;
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

    /** Returns {@code true} if {@link #readAllBytes()} was called on this stream. */
    public boolean wasReadAllBytesCalled() {
      return readAllBytesCalled;
    }

    public boolean isFullyRead() {
      // fullyReadTime is set when -1 is returned from read(), but some implementations
      // may not make that final -1 call. Fall back to bytesRead == totalBytes.
      return fullyReadTime != -1 || bytesRead >= totalBytes;
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
   * An extension of {@link MonitoringOutputStream} that captures how many bytes have been read from
   * a paired {@link TrackingInputStream} at the moment of the first write. This can be used to
   * verify that output is produced before the input stream is fully consumed (i.e., true streaming
   * behaviour).
   */
  public static class MidStreamMonitoringOutputStream extends MonitoringOutputStream {

    private final TrackingInputStream pairedInput;
    private int inputBytesReadAtFirstWrite = -1;

    /**
     * Create a monitoring output stream paired with the given input stream.
     *
     * @param pairedInput the input stream to observe at first-write time
     */
    public MidStreamMonitoringOutputStream(TrackingInputStream pairedInput) {
      this.pairedInput = pairedInput;
    }

    @Override
    public void write(int b) {
      captureSnapshot();
      super.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      captureSnapshot();
      super.write(b, off, len);
    }

    private void captureSnapshot() {
      if (inputBytesReadAtFirstWrite == -1) {
        inputBytesReadAtFirstWrite = pairedInput.getBytesRead();
      }
    }

    /**
     * Returns the number of bytes that had been read from the paired input stream at the time the
     * first byte was written to this output stream, or {@code -1} if no write has occurred yet.
     */
    public int getInputBytesReadAtFirstWrite() {
      return inputBytesReadAtFirstWrite;
    }

    /**
     * Returns {@code true} if the first write to this output stream occurred while the paired input
     * stream had not yet been fully consumed (i.e., true streaming behaviour).
     */
    public boolean wasFirstWriteBeforeFullyRead() {
      if (inputBytesReadAtFirstWrite < 0) {
        return false;
      }
      return inputBytesReadAtFirstWrite < pairedInput.getTotalBytes();
    }
  }
}
