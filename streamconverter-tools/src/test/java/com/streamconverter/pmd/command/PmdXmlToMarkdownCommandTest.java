package com.streamconverter.pmd.command;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.pmd.PmdViolation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class PmdXmlToMarkdownCommandTest {
  private static final String MARKDOWN_HEADER = "# PMD Code Quality Analysis Report\n\n";

  private static final PmdViolation VIOLATION_A =
      new PmdViolation(
          "streamconverter-core/Foo.java",
          10,
          "UnusedVariable",
          "Best Practices",
          3,
          "Avoid unused variables.",
          "Foo",
          "bar",
          "x");
  private static final PmdViolation VIOLATION_B =
      new PmdViolation(
          "streamconverter-db/Dao.java",
          5,
          "LongMethod",
          "Design",
          2,
          "Method too long.",
          "Dao",
          "find",
          "");

  @Test
  void outputContainsHeader() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of(VIOLATION_A, VIOLATION_B)), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("# PMD Code Quality Analysis Report"));
    assertTrue(report.contains("Total Violations**: 2"));
  }

  @Test
  void outputContainsRuleNames() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of(VIOLATION_A, VIOLATION_B)), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("UnusedVariable"));
    assertTrue(report.contains("LongMethod"));
  }

  @Test
  void emptyInput_producesEmptyReport() throws Exception {
    var output = new ByteArrayOutputStream();
    new PmdXmlToMarkdownCommand().execute(serialize(List.of()), output);

    String report = output.toString(StandardCharsets.UTF_8);
    assertTrue(report.contains("Total Violations**: 0"));
  }

  @Test
  void writesHeaderBeforeInputCompletes() throws Exception {
    RecordingWriter recordingWriter = new RecordingWriter(new StringWriter());
    BlockingInputStream input =
        new BlockingInputStream(serializeBytes(List.of(VIOLATION_A, VIOLATION_B)), 8);

    PmdXmlToMarkdownCommand command =
        new PmdXmlToMarkdownCommand() {
          @Override
          Writer createWriter(OutputStream output) {
            return recordingWriter;
          }
        };

    try (var executor = Executors.newSingleThreadExecutor()) {
      Future<?> future =
          executor.submit(
              () -> {
                try {
                  command.execute(input, new ByteArrayOutputStream());
                } catch (IOException e) {
                  throw new RuntimeException(e);
                }
              });

      assertTrue(
          recordingWriter.awaitHeaderWrite(1, TimeUnit.SECONDS),
          "header should be written before input completes");

      input.releaseRemainingInput();
      future.get(2, TimeUnit.SECONDS);
    }
  }

  private ByteArrayInputStream serialize(List<PmdViolation> violations) throws Exception {
    return new ByteArrayInputStream(serializeBytes(violations));
  }

  private byte[] serializeBytes(List<PmdViolation> violations) throws Exception {
    var baos = new ByteArrayOutputStream();
    try (var oos = new ObjectOutputStream(baos)) {
      for (var v : violations) {
        oos.writeObject(v);
      }
    }
    return baos.toByteArray();
  }

  private static final class BlockingInputStream extends InputStream {
    private final byte[] data;
    private final int firstChunkSize;
    private final CountDownLatch releaseRemainingInput = new CountDownLatch(1);
    private int index;

    private BlockingInputStream(byte[] data, int firstChunkSize) {
      this.data = data;
      this.firstChunkSize = firstChunkSize;
    }

    @Override
    public int read() throws IOException {
      byte[] singleByte = new byte[1];
      int read = read(singleByte, 0, 1);
      return read == -1 ? -1 : singleByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
      if (index >= data.length) {
        return -1;
      }
      if (index >= firstChunkSize) {
        try {
          if (!releaseRemainingInput.await(2, TimeUnit.SECONDS)) {
            throw new IOException("Timed out waiting to release remaining input");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting to release remaining input", e);
        }
      }

      int upperBound = index < firstChunkSize ? firstChunkSize : data.length;
      int bytesToCopy = Math.min(len, upperBound - index);
      System.arraycopy(data, index, buffer, off, bytesToCopy);
      index += bytesToCopy;
      return bytesToCopy;
    }

    private void releaseRemainingInput() {
      releaseRemainingInput.countDown();
    }
  }

  private static final class RecordingWriter extends FilterWriter {
    private final CountDownLatch headerWrite = new CountDownLatch(1);

    private RecordingWriter(Writer out) {
      super(out);
    }

    @Override
    public void write(String str) throws IOException {
      if (MARKDOWN_HEADER.equals(str)) {
        headerWrite.countDown();
      }
      super.write(str);
    }

    private boolean awaitHeaderWrite(long timeout, TimeUnit unit) throws InterruptedException {
      return headerWrite.await(timeout, unit);
    }
  }
}
