package com.streamconverter.command.impl.csv;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.opencsv.CSVReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for FaultReportingReader. */
@DisplayName("FaultReportingReader Tests")
class FaultReportingReaderTest {

  @Test
  @DisplayName("read()で発生したIOExceptionは伝播し、記録もされること")
  void testReadSingleCharPropagatesAndRecordsFault() {
    IOException failure = new IOException("simulated failure on read()");
    FaultReportingReader reader = new FaultReportingReader(new AlwaysFailingReader(failure));

    IOException thrown = assertThrows(IOException.class, reader::read, "read()の例外がそのまま伝播すること");
    assertSame(failure, thrown, "read()がスローする例外は下位readerの例外と同一インスタンスであること");

    // The recorded fault must be rethrown (wrapped) on close, proving it was recorded.
    IOException closeFailure =
        assertThrows(IOException.class, reader::close, "記録されたfaultがclose()で再スローされること");
    assertSame(failure, closeFailure.getCause(), "close()がスローする例外のcauseは記録されたfaultであること");
  }

  @Test
  @DisplayName("read(char[],int,int)で発生したIOExceptionは伝播し、記録もされること")
  void testReadBufferPropagatesAndRecordsFault() {
    IOException failure = new IOException("simulated failure on read(char[],int,int)");
    FaultReportingReader reader = new FaultReportingReader(new AlwaysFailingReader(failure));
    char[] buf = new char[16];

    IOException thrown =
        assertThrows(
            IOException.class,
            () -> reader.read(buf, 0, buf.length),
            "read(char[],int,int)の例外がそのまま伝播すること");
    assertSame(failure, thrown, "read(char[],int,int)がスローする例外は下位readerの例外と同一インスタンスであること");

    IOException closeFailure =
        assertThrows(IOException.class, reader::close, "記録されたfaultがclose()で再スローされること");
    assertSame(failure, closeFailure.getCause(), "close()がスローする例外のcauseは記録されたfaultであること");
  }

  @Test
  @DisplayName("黙殺シナリオ: read失敗をEOF扱いした後でもclose()がfaultを伴う例外をスローすること")
  void testCloseReportsSuppressedFaultAfterCallerTreatsFailureAsEof() throws IOException {
    IOException failure = new IOException("simulated failure during read");
    FaultReportingReader reader = new FaultReportingReader(new AlwaysFailingReader(failure));
    char[] buf = new char[16];

    // Caller catches the IOException and treats it as EOF (the opencsv-like behaviour),
    // i.e. it does NOT rethrow.
    try {
      reader.read(buf, 0, buf.length);
    } catch (IOException ignored) {
      // simulate a caller that swallows the failure as if it were EOF
    }

    IOException closeFailure =
        assertThrows(IOException.class, reader::close, "黙殺されたfaultはclose()で新たな例外として検出されること");
    assertSame(failure, closeFailure.getCause(), "close()がスローする例外のcauseは記録されたfaultであること");
    assertTrue(
        closeFailure.getMessage().contains(failure.getMessage()),
        "close()の例外メッセージは記録されたfaultのメッセージを含むこと");
  }

  @Test
  @DisplayName("障害が発生しなければclose()は何もスローしないこと")
  void testCloseDoesNothingWhenNoFaultRecorded() throws IOException {
    FaultReportingReader reader = new FaultReportingReader(new StringReader("hello"));

    char[] buf = new char[16];
    int read = reader.read(buf, 0, buf.length);
    assertEquals(5, read, "正常なreaderからは期待した文字数が読み取れること");

    assertDoesNotThrow(reader::close, "障害が記録されていない場合、close()は例外をスローしないこと");
  }

  @Test
  @DisplayName("close()自体が失敗し、かつfault記録済みの場合、close失敗例外にfaultがsuppressedとして付くこと")
  void testCloseFailureSuppressesRecordedFault() {
    IOException readFailure = new IOException("simulated read failure");
    IOException closeFailure = new IOException("simulated close failure");
    FaultReportingReader reader =
        new FaultReportingReader(new AlwaysFailingReader(readFailure, closeFailure));

    assertThrows(IOException.class, reader::read, "read()の失敗が伝播すること");

    IOException thrown = assertThrows(IOException.class, reader::close, "close()自体の失敗が伝播すること");
    assertSame(closeFailure, thrown, "close()がスローする例外はclose失敗の例外そのものであること");

    Throwable[] suppressed = thrown.getSuppressed();
    assertEquals(1, suppressed.length, "close失敗例外には1件のsuppressed例外が付与されること");
    assertSame(readFailure, suppressed[0], "suppressedされた例外は記録済みのread失敗であること");
  }

  @Test
  @DisplayName("forUtf8(InputStream)はUTF-8コンテンツ(日本語含む)を正しく読めること")
  void testForUtf8ReadsUtf8Content() throws IOException {
    String content = "name,note\nAlice,こんにちは\n";
    ByteArrayInputStream input = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

    FaultReportingReader reader = FaultReportingReader.forUtf8(input);
    char[] buf = new char[256];
    int read;
    StringBuilder result = new StringBuilder();
    while ((read = reader.read(buf, 0, buf.length)) != -1) {
      result.append(buf, 0, read);
    }
    reader.close();

    assertEquals(content, result.toString(), "forUtf8で読み込んだ内容はUTF-8として元の文字列と一致すること");
  }

  @Test
  @DisplayName("opencsv実証: readNext()がnullを返して握りつぶしても、close()で記録済み障害がスローされること")
  void testOpenCsvSwallowsReadFailureButCloseDetectsIt() throws IOException {
    // The whole content fits in CSVReader's internal BufferedReader in a single underlying
    // read(char[],int,int) call, so the header row and the data row are both served from that
    // buffer without any further calls to the underlying reader.
    String csvData = "name,age\nAlice,30\n";
    IOException simulatedFailure = new IOException("simulated disk failure");
    FailAfterFirstReadReader failingReader =
        new FailAfterFirstReadReader(csvData, simulatedFailure);
    FaultReportingReader faultReportingReader = new FaultReportingReader(failingReader);

    IOException closeFailure =
        assertThrows(
            IOException.class,
            () -> {
              try (CSVReader csvReader = new CSVReader(faultReportingReader)) {
                // First call returns the header row, served from the initial buffer fill.
                String[] headers = csvReader.readNext();
                assertArrayEquals(new String[] {"name", "age"}, headers, "1行目のヘッダーは正常に読めること");

                // Second call returns the data row, also served from the initial buffer fill.
                String[] dataRow = csvReader.readNext();
                assertArrayEquals(new String[] {"Alice", "30"}, dataRow, "2行目のデータ行は正常に読めること");

                // Third call: the buffer is exhausted, so CSVReader's verifyReader check
                // (isClosed()) triggers another underlying read(char[],int,int), which throws.
                // opencsv swallows the IOException and reports it as a normal EOF (null).
                String[] next = csvReader.readNext();
                assertNull(next, "opencsvはIOExceptionをEOF(null)として握りつぶすこと");
              }
              // try-with-resources close() must surface the suppressed I/O failure.
            },
            "opencsvが握りつぶしたI/O障害はclose()で検出され、IOExceptionがスローされること");

    assertSame(simulatedFailure, closeFailure.getCause(), "close()でスローされる例外のcauseは記録されたI/O障害であること");
  }

  /** A {@link Reader} that always fails with a given {@link IOException} on every read call. */
  private static final class AlwaysFailingReader extends Reader {
    private final IOException readFailure;
    private final IOException closeFailure;

    AlwaysFailingReader(IOException readFailure) {
      this(readFailure, null);
    }

    AlwaysFailingReader(IOException readFailure, IOException closeFailure) {
      this.readFailure = readFailure;
      this.closeFailure = closeFailure;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      throw readFailure;
    }

    @Override
    public void close() throws IOException {
      if (closeFailure != null) {
        throw closeFailure;
      }
    }
  }

  /**
   * A {@link Reader} that returns CSV data on its first {@code read(char[], int, int)} call and
   * throws a simulated I/O failure on every subsequent call, mimicking a disk that fails partway
   * through a read.
   */
  private static final class FailAfterFirstReadReader extends Reader {
    private final String firstChunk;
    private final IOException subsequentFailure;
    private boolean firstReadDone;

    FailAfterFirstReadReader(String firstChunk, IOException subsequentFailure) {
      this.firstChunk = firstChunk;
      this.subsequentFailure = subsequentFailure;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
      if (!firstReadDone) {
        firstReadDone = true;
        char[] chars = firstChunk.toCharArray();
        int toCopy = Math.min(len, chars.length);
        System.arraycopy(chars, 0, cbuf, off, toCopy);
        return toCopy;
      }
      throw subsequentFailure;
    }

    @Override
    public void close() {
      // no-op
    }
  }
}
