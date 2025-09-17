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
}
