package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AbortablePipedStream")
class AbortablePipedStreamTest {

  private AbortablePipedStream pipe;

  @AfterEach
  void tearDown() throws IOException {
    if (pipe != null) {
      pipe.close();
    }
  }

  @Nested
  @DisplayName("コンストラクタ境界値")
  class ConstructorTest {

    @Test
    void constructorWithZeroBufferThrowsIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> new AbortablePipedStream(0));
    }

    @Test
    void constructorWithNegativeBufferThrowsIllegalArgumentException() {
      assertThrows(IllegalArgumentException.class, () -> new AbortablePipedStream(-1));
    }

    @Test
    void constructorWithSizeOneSucceeds() {
      assertDoesNotThrow(
          () -> {
            pipe = new AbortablePipedStream(1);
          });
    }
  }

  @Nested
  @DisplayName("outputStream() / inputStream() 二重呼び出しガード")
  class StreamAccessGuardTest {

    @Test
    void outputStreamSecondCallThrowsIllegalStateException() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream();
      assertThrows(IllegalStateException.class, () -> pipe.outputStream());
    }

    @Test
    void inputStreamSecondCallThrowsIllegalStateException() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.inputStream();
      assertThrows(IllegalStateException.class, () -> pipe.inputStream());
    }
  }

  @Nested
  @DisplayName("abort() 後の OutputStream 操作")
  class OutputStreamAbortBeforeTest {

    @Test
    void writeIntThrowsPipeAbortedAfterAbort() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      pipe.inputStream(); // 接続を確立するために必須
      pipe.abort();
      assertThrows(PipeAbortedException.class, () -> os.write(1));
    }

    @Test
    void writeByteArrayThrowsPipeAbortedAfterAbort() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      pipe.inputStream();
      pipe.abort();
      assertThrows(PipeAbortedException.class, () -> os.write(new byte[] {1}, 0, 1));
    }

    @Test
    void flushThrowsPipeAbortedAfterAbort() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      pipe.inputStream();
      pipe.abort();
      assertThrows(PipeAbortedException.class, os::flush);
    }
  }

  @Nested
  @DisplayName("OutputStream catch ブランチ（IOException → checkAborted）")
  class OutputStreamIOExceptionCatchTest {

    @Test
    @DisplayName("is.close() 後に abort してから write すると PipeAbortedException（catch 内 true）")
    void writeIntAfterInputCloseWithAbortThrowsPipeAbortedException() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      is.close();
      pipe.abort();
      assertThrows(PipeAbortedException.class, () -> os.write(1));
    }

    @Test
    @DisplayName(
        "is.close() 後に abort なしで write すると PipeAbortedException 以外の IOException（catch 内 false）")
    void writeIntAfterInputCloseWithoutAbortRethrowsIOException() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      is.close();
      IOException ex = assertThrows(IOException.class, () -> os.write(1));
      assertFalse(
          ex instanceof PipeAbortedException,
          "abort していないので PipeAbortedException ではなく通常の IOException であること");
    }

    @Test
    @DisplayName("is.close() 後に abort してから write(byte[]) すると PipeAbortedException")
    void writeByteArrayAfterInputCloseWithAbortThrowsPipeAbortedException() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      is.close();
      pipe.abort();
      assertThrows(PipeAbortedException.class, () -> os.write(new byte[] {1}, 0, 1));
    }

    @Test
    @DisplayName("is.close() 後に abort してから flush() すると PipeAbortedException")
    void flushAfterInputCloseWithAbortThrowsPipeAbortedException() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      is.close();
      pipe.abort();
      assertThrows(PipeAbortedException.class, os::flush);
    }
  }

  @Nested
  @DisplayName("abort() 後の InputStream 操作")
  class InputStreamAbortBeforeTest {

    @Test
    void readIntThrowsPipeAbortedAfterAbort() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream(); // 接続を確立するために必須
      InputStream is = pipe.inputStream();
      pipe.abort();
      assertThrows(PipeAbortedException.class, is::read);
    }

    @Test
    void readByteArrayThrowsPipeAbortedAfterAbort() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream();
      InputStream is = pipe.inputStream();
      pipe.abort();
      assertThrows(
          PipeAbortedException.class,
          () -> {
            int readCount = is.read(new byte[4], 0, 4);
            fail("Expected PipeAbortedException, but read returned " + readCount);
          });
    }
  }

  @Nested
  @DisplayName("InputStream: os.close() 後の動作")
  class InputStreamAfterOutputCloseTest {

    @Test
    @DisplayName("os.close() 後に read すると EOF（-1）を返す")
    void readIntAfterOutputCloseReturnsEOF() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      os.close();
      assertEquals(-1, is.read());
    }

    @Test
    @DisplayName("os.close() 後に read(byte[]) すると 0 または EOF を返す（例外なし）")
    void readByteArrayAfterOutputCloseReturnsNonNegative() throws IOException {
      pipe = new AbortablePipedStream(16);
      OutputStream os = pipe.outputStream();
      InputStream is = pipe.inputStream();
      os.close();
      int result = is.read(new byte[4], 0, 4);
      assertEquals(-1, result, "書き込み側がクローズされデータなしの場合は EOF(-1) を返す");
    }
  }

  @Nested
  @DisplayName("abort() と close() のライフサイクル")
  class LifecycleTest {

    @Test
    void abortIsIdempotent() throws IOException {
      pipe = new AbortablePipedStream(16);
      assertDoesNotThrow(
          () -> {
            pipe.abort();
            pipe.abort();
            pipe.abort();
          });
    }

    @Test
    void closeIsIdempotent() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream();
      pipe.inputStream();
      assertDoesNotThrow(
          () -> {
            pipe.close();
            pipe.close(); // 2回目: catch(IOException ignored) ブランチをカバー
          });
    }

    @Test
    void abortThenCloseIsValid() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream();
      pipe.inputStream();
      assertDoesNotThrow(
          () -> {
            pipe.abort();
            pipe.close();
          });
    }

    @Test
    void closeThenAbortIsValid() throws IOException {
      pipe = new AbortablePipedStream(16);
      pipe.outputStream();
      pipe.inputStream();
      assertDoesNotThrow(
          () -> {
            pipe.close();
            pipe.abort();
          });
    }
  }
}
