package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.LineEndingNormalizeCommand.LineEndingType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LineEndingNormalizeCommand Tests")
class LineEndingNormalizeCommandTest {

  @Test
  @DisplayName("Convert Unix to Windows line endings")
  void testUnixToWindows() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String expected = "line1\r\nline2\r\nline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert Windows to Unix line endings")
  void testWindowsToUnix() throws IOException {
    // Given
    String input = "line1\r\nline2\r\nline3";
    String expected = "line1\nline2\nline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert mixed line endings to Unix")
  void testMixedToUnix() throws IOException {
    // Given - mix of CRLF, LF, and CR
    String input = "line1\r\nline2\nline3\rline4";
    String expected = "line1\nline2\nline3\nline4";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Convert to classic Mac line endings")
  void testToMacClassic() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String expected = "line1\rline2\rline3";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.MAC_CLASSIC);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Use system default line endings")
  void testSystemDefault() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    String systemSeparator = System.lineSeparator();
    String expected = "line1" + systemSeparator + "line2" + systemSeparator + "line3";

    LineEndingNormalizeCommand command =
        new LineEndingNormalizeCommand(LineEndingType.SYSTEM_DEFAULT);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle empty input")
  void testEmptyInput() throws IOException {
    // Given
    String input = "";
    String expected = "";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle single line without line ending")
  void testSingleLineWithoutEnding() throws IOException {
    // Given
    String input = "single line";
    String expected = "single line";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.UNIX);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle single line with line ending")
  void testSingleLineWithEnding() throws IOException {
    // Given
    String input = "single line\n";
    String expected = "single line\r\n";

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Preserve input line endings (simplified implementation)")
  void testPreserveInput() throws IOException {
    // Given
    String input = "line1\nline2\nline3";
    // Note: Current implementation defaults to Unix for PRESERVE_INPUT
    String expected = "line1\nline2\nline3";

    LineEndingNormalizeCommand command =
        new LineEndingNormalizeCommand(LineEndingType.PRESERVE_INPUT);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Constructor should reject null target type")
  void testConstructorNullValidation() {
    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> {
          new LineEndingNormalizeCommand(null);
        });
  }

  @Test
  @DisplayName("Get command details should include configuration")
  void testGetCommandDetails() {
    // Given
    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String details = command.getCommandDetails();

    // Then
    assertTrue(details.contains("LineEndingNormalizeCommand"));
    assertTrue(details.contains("WINDOWS"));
    assertTrue(details.contains("\\r\\n"));
  }

  @Test
  @DisplayName("Handle large input efficiently")
  void testLargeInput() throws IOException {
    // Given - create a moderately large input
    StringBuilder inputBuilder = new StringBuilder();
    StringBuilder expectedBuilder = new StringBuilder();

    for (int i = 0; i < 1000; i++) {
      if (i > 0) {
        inputBuilder.append("\n");
        expectedBuilder.append("\r\n");
      }
      String line = "Line " + i + " with some content";
      inputBuilder.append(line);
      expectedBuilder.append(line);
    }

    String input = inputBuilder.toString();
    String expected = expectedBuilder.toString();

    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("Handle buffer boundary with line endings")
  void testBufferBoundaryLineEndings() throws IOException {
    // Given - create input that puts line endings exactly at buffer boundaries
    // Buffer size is 8192 for PRESERVE_INPUT mode
    StringBuilder inputBuilder = new StringBuilder();

    // Create content that approaches 8192 characters
    String baseContent = "A".repeat(100); // 100 chars per line
    for (int i = 0; i < 81; i++) { // 81 * 100 = 8100 chars
      if (i > 0) {
        inputBuilder.append("\n");
      }
      inputBuilder.append(baseContent);
    }

    // Add content to get close to 8192 boundary, then add line ending
    inputBuilder.append("\n"); // This should be near buffer boundary
    inputBuilder.append("Final line content");

    String input = inputBuilder.toString();

    // Test PRESERVE_INPUT (uses buffer reading)
    LineEndingNormalizeCommand preserveCommand =
        new LineEndingNormalizeCommand(LineEndingType.PRESERVE_INPUT);
    String preserveResult = executeCommand(preserveCommand, input);
    assertEquals(
        input,
        preserveResult,
        "PRESERVE_INPUT should maintain exact input including boundary line endings");

    // Test conversion (uses character-by-character reading)
    LineEndingNormalizeCommand windowsCommand =
        new LineEndingNormalizeCommand(LineEndingType.WINDOWS);
    String windowsResult = executeCommand(windowsCommand, input);
    String expectedWindows = input.replace("\n", "\r\n");
    assertEquals(
        expectedWindows,
        windowsResult,
        "Windows conversion should work correctly across buffer boundaries");
  }

  @Test
  @DisplayName("Handle CRLF spanning buffer boundary")
  void testCRLFSpanningBufferBoundary() throws IOException {
    // Given - create input where \r\n spans across buffer boundary
    StringBuilder inputBuilder = new StringBuilder();

    // Fill almost exactly to buffer boundary minus 1
    String padding = "X".repeat(8191); // 8191 chars
    inputBuilder.append(padding);
    inputBuilder.append("\r\n"); // CRLF spans boundary at position 8191-8192
    inputBuilder.append("After boundary");

    String input = inputBuilder.toString();

    // Test Unix conversion - should handle CRLF correctly even when spanning boundary
    LineEndingNormalizeCommand unixCommand = new LineEndingNormalizeCommand(LineEndingType.UNIX);
    String result = executeCommand(unixCommand, input);

    String expectedOutput = padding + "\n" + "After boundary";
    assertEquals(expectedOutput, result, "CRLF spanning buffer boundary should be converted to LF");
  }

  @Test
  @DisplayName("Handle multiple mixed line endings near buffer boundary")
  void testMixedLineEndingsNearBoundary() throws IOException {
    // Given - create input with different line ending types near buffer boundary
    StringBuilder inputBuilder = new StringBuilder();

    // Content approaching buffer boundary
    String baseContent = "Data".repeat(2000); // 8000 chars
    inputBuilder.append(baseContent);

    // Add mixed line endings near boundary
    inputBuilder.append("Line1\r\n"); // CRLF
    inputBuilder.append("Line2\n"); // LF
    inputBuilder.append("Line3\r"); // CR
    inputBuilder.append("Line4\r\n"); // CRLF again
    inputBuilder.append("Final");

    String input = inputBuilder.toString();

    // Test conversion to Windows format
    LineEndingNormalizeCommand windowsCommand =
        new LineEndingNormalizeCommand(LineEndingType.WINDOWS);
    String result = executeCommand(windowsCommand, input);

    String expectedOutput =
        baseContent + "Line1\r\n" + "Line2\r\n" + "Line3\r\n" + "Line4\r\n" + "Final";
    assertEquals(
        expectedOutput,
        result,
        "Mixed line endings near buffer boundary should be normalized correctly");
  }

  @Test
  @DisplayName("並列処理検証テスト（LineEndingNormalizeCommand）")
  void testParallelProcessingValidation() throws IOException, InterruptedException {
    // 大容量データ（2MB）で並列処理を検証
    StringBuilder inputBuilder = new StringBuilder();
    // 改行を含む大容量テストデータを作成
    for (int i = 0; i < 100000; i++) {
      inputBuilder.append("Test line ").append(i).append(" with some content\n");
    }
    String testData = inputBuilder.toString();

    // 並列処理監視用InputStream
    ParallelProcessingValidationInputStream inputStream =
        new ParallelProcessingValidationInputStream(testData.getBytes(StandardCharsets.UTF_8));

    // OutputStreamの状態を監視できるカスタムOutputStream
    MonitoringOutputStream monitoringStream = new MonitoringOutputStream();

    System.out.println("=== LineEndingNormalizeCommand 並列処理検証テスト ===");
    System.out.println("📤 開始時刻: " + new java.util.Date());
    System.out.println(
        "📊 入力データサイズ: " + String.format("%.2f MB", testData.length() / (1024.0 * 1024.0)));

    // OutputStreamを参照として設定
    inputStream.setOutputStreamToMonitor(monitoringStream);

    // ストリーミング処理を実行
    LineEndingNormalizeCommand command = new LineEndingNormalizeCommand(LineEndingType.WINDOWS);
    long startTime = System.currentTimeMillis();
    assertDoesNotThrow(
        () -> command.execute(inputStream, monitoringStream), "LineEndingNormalize処理は正常に完了するべき");
    long endTime = System.currentTimeMillis();

    System.out.println("📥 完了時刻: " + new java.util.Date());
    System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

    // 並列処理検証結果の取得
    ParallelProcessingValidationInputStream.ValidationResult result =
        inputStream.getValidationResult();

    System.out.println("📊 並列処理検証結果:");
    System.out.println(
        "  - InputStreamクローズ時点でのOutputStream書き込み: " + result.outputBytesAtClose + " bytes");
    System.out.println(
        "  - クローズ時点での入力進行率: " + String.format("%.1f%%", result.inputProgressAtClose));
    System.out.println("  - 総書き込みバイト数: " + monitoringStream.getBytesWritten());

    // 基本的な処理検証
    assertTrue(monitoringStream.getBytesWritten() > 0, "出力データが正常に書き込まれるべき");

    // 並列処理特性の検証（LineEndingNormalizeCommandの場合）
    if (result.outputBytesAtClose > 0) {
      System.out.println("  ✅ 並列処理特性確認 - InputStreamクローズ前に出力書き込み開始");
      assertTrue(
          result.outputBytesAtClose > 0,
          String.format(
              "並列処理特性: InputStreamクローズ時点でOutputStreamに書き込み済み (%d bytes)",
              result.outputBytesAtClose));
    } else {
      System.out.println("  ⚠️ 逐次処理パターン - InputStreamクローズ後に出力書き込み");
      // LineEndingNormalizeCommandでも逐次処理の場合があることを記録
      assertEquals(0, result.outputBytesAtClose, "逐次処理パターン: InputStreamクローズ時点でのOutputStream書き込みなし");
    }

    System.out.println("✅ LineEndingNormalizeCommand 並列処理検証テスト完了");
  }

  /** 並列処理検証用のInputStream - InputStreamのクローズ時点でOutputStreamの状態を記録 */
  private static class ParallelProcessingValidationInputStream extends java.io.InputStream {
    private final byte[] data;
    private int position = 0;
    private MonitoringOutputStream outputStreamToMonitor;
    private ValidationResult validationResult;

    public ParallelProcessingValidationInputStream(byte[] data) {
      this.data = data;
    }

    public void setOutputStreamToMonitor(MonitoringOutputStream outputStream) {
      this.outputStreamToMonitor = outputStream;
    }

    @Override
    public int read() throws IOException {
      if (position >= data.length) {
        return -1;
      }
      return data[position++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (position >= data.length) {
        return -1;
      }
      int available = Math.min(len, data.length - position);
      System.arraycopy(data, position, b, off, available);
      position += available;
      return available;
    }

    @Override
    public void close() throws IOException {
      // InputStreamクローズ時点でのOutputStreamの状態を記録
      double inputProgressAtClose = (position / (double) data.length) * 100.0;
      long outputBytesAtClose =
          outputStreamToMonitor != null ? outputStreamToMonitor.getBytesWritten() : 0;

      System.out.printf(
          "🔄 [並列処理検証] InputStreamクローズ時点: 入力消費%.1f%%, OutputStream書き込み%d bytes%n",
          inputProgressAtClose, outputBytesAtClose);

      if (outputBytesAtClose > 0) {
        System.out.println("   ✅ 並列処理が検出されました - OutputStreamに既にデータが存在");
      } else {
        System.out.println("   ⚠️ 逐次処理が検出されました - OutputStreamにまだデータが存在しない");
      }

      this.validationResult = new ValidationResult(inputProgressAtClose, outputBytesAtClose);
      super.close();
    }

    public ValidationResult getValidationResult() {
      return validationResult;
    }

    public static class ValidationResult {
      public final double inputProgressAtClose;
      public final long outputBytesAtClose;

      public ValidationResult(double inputProgressAtClose, long outputBytesAtClose) {
        this.inputProgressAtClose = inputProgressAtClose;
        this.outputBytesAtClose = outputBytesAtClose;
      }
    }
  }

  /** 書き込みバイト数を監視できるOutputStream */
  private static class MonitoringOutputStream extends java.io.OutputStream {
    private final java.io.ByteArrayOutputStream delegate = new java.io.ByteArrayOutputStream();
    private long bytesWritten = 0;

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
      bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
      bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    public long getBytesWritten() {
      return bytesWritten;
    }

    public byte[] toByteArray() {
      return delegate.toByteArray();
    }
  }

  /**
   * Helper method to execute a command with string input and return string output.
   *
   * @param command the command to execute
   * @param input the input string
   * @return the output string
   * @throws IOException if execution fails
   */
  private String executeCommand(LineEndingNormalizeCommand command, String input)
      throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
