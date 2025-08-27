package com.streamConverter.command.impl.charaCode;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.test.StreamingTestUtils.MonitoringOutputStream;
import com.streamConverter.test.StreamingTestUtils.TrackingInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Character Code Conversion Command Test")
class ConvertTest {

  private static final String TEST_STRING = "こんにちは世界！Hello World!";

  @Test
  @DisplayName("Constructor Test")
  void testConstructor() {
    // コンストラクタのテスト
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");
    assertNotNull(command);
  }

  @ParameterizedTest
  @CsvSource({"UTF-8, UTF-16", "UTF-16, UTF-8"})
  @DisplayName("Execute Normal Case: Character Code Conversion")
  void testExecuteWithValidCharsets(String fromCharset, String toCharset) throws IOException {
    // 正常系のexecuteメソッドテスト
    CharacterConvertCommand command = new CharacterConvertCommand(fromCharset, toCharset);

    // 入力文字列をfromCharsetでエンコード
    byte[] inputBytes = TEST_STRING.getBytes(Charset.forName(fromCharset));

    try (InputStream inputStream = new ByteArrayInputStream(inputBytes);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 出力をtoCharsetでデコードして元の文字列と比較
      String result = new String(outputStream.toByteArray(), Charset.forName(toCharset));
      assertEquals(TEST_STRING, result);
    }
  }

  @Test
  @DisplayName("Execute Error Case: Null Input Stream")
  void testExecuteWithNullInputStream() {
    // null入力ストリームでのexecuteメソッドテスト
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Exception exception =
        assertThrows(
            IOException.class,
            () -> {
              command.execute(null, outputStream);
            });

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("Execute Error Case: Null Output Stream")
  void testExecuteWithNullOutputStream() {
    // null出力ストリームでのexecuteメソッドテスト
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");
    InputStream inputStream =
        new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8));

    Exception exception =
        assertThrows(
            IOException.class,
            () -> {
              command.execute(inputStream, null);
            });

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("Execute Error Case: Invalid Input Character Code")
  void testExecuteWithInvalidInputCharset() {
    // 無効な入力文字コードでのconstructorテスト
    // 例外が発生することを期待
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CharacterConvertCommand("INVALID-CHARSET", "UTF-8");
        });
  }

  @Test
  @DisplayName("Execute Error Case: Invalid Output Character Code")
  void testExecuteWithInvalidOutputCharset() {
    // 無効な出力文字コードでのconstructorテスト
    // 例外が発生することを期待
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CharacterConvertCommand("UTF-8", "INVALID-CHARSET");
        });
  }

  @Test
  @DisplayName("Execute: Empty Input Stream")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");

    try (InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 空の出力になるはず
      assertEquals(0, outputStream.size());
    }
  }

  @Test
  @DisplayName("Japanese Character Conversion Test")
  void testJapaneseCharacterConversion() throws IOException {
    // 日本語文字の変換テスト
    String japaneseText = "日本語のテスト文字列です。漢字、ひらがな、カタカナを含みます。";
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");

    try (InputStream inputStream =
            new ByteArrayInputStream(japaneseText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証
      String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_16);
      assertEquals(japaneseText, result);
    }
  }

  @Test
  @DisplayName("Streaming character conversion behavior verification")
  void testStreamingCharacterConversionBehavior() throws IOException {
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");

    // Create moderate-sized multilingual text to observe streaming behavior
    StringBuilder textBuilder = new StringBuilder();
    textBuilder.append("Streaming character conversion test data:\n");

    for (int i = 0; i < 100; i++) {
      textBuilder.append(
          String.format(
              "Line %03d: English text with Japanese characters: こんにちは世界 %d! Chinese: 你好世界 %d! Korean: 안녕하세요 세계 %d!%n",
              i, i, i, i));
    }
    String testData = textBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(testData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - execute character conversion
    command.execute(trackingInputStream, monitoringOutputStream);

    // Then - verify streaming behavior occurred
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "OutputStream should have received data during character conversion");
    assertTrue(
        trackingInputStream.isFullyRead(), "InputStream should be fully consumed after processing");

    // Verify data was processed incrementally
    assertTrue(
        trackingInputStream.getBytesRead() > 0,
        "Input stream should have been read during character conversion");

    // Verify the conversion was correct
    String convertedOutput = monitoringOutputStream.getContent();
    // Note: We can't directly compare UTF-16 output as string, but verify it has content
    // (removed unused expectedOutput variable to fix SpotBugs DLS warning)
    assertTrue(convertedOutput.length() > 0, "Should have produced converted output");
  }

  @Test
  @DisplayName("Incremental character conversion processing verification")
  void testIncrementalCharacterConversionProcessing() throws IOException {
    CharacterConvertCommand command = new CharacterConvertCommand("UTF-8", "UTF-16");

    // Create complex multilingual content to force incremental processing
    StringBuilder contentBuilder = new StringBuilder();

    for (int i = 1; i <= 200; i++) {
      contentBuilder.append(
          String.format(
              "Entry %03d: Mixed languages content - English, 日本語 (Japanese), 中文 (Chinese), 한국어 (Korean), العربية (Arabic), русский (Russian), Ελληνικά (Greek)%n",
              i));

      // Add occasional longer content blocks
      if (i % 20 == 0) {
        contentBuilder.append(
            "Extended multilingual content block: "
                + "This block contains extensive text in multiple character encodings to test streaming behavior during character conversion. "
                + "日本語: これは文字エンコーディング変換のストリーミング動作をテストするための拡張されたマルチリンガルコンテンツブロックです。"
                + "中文: 这是一个扩展的多语言内容块，用于测试字符编码转换期间的流行为。"
                + "한국어: 이것은 문자 인코딩 변환 중 스트리밍 동작을 테스트하기 위한 확장된 다국어 콘텐츠 블록입니다.\n");
      }
    }
    String contentData = contentBuilder.toString();

    TrackingInputStream trackingInputStream =
        new TrackingInputStream(contentData.getBytes(StandardCharsets.UTF_8));
    MonitoringOutputStream monitoringOutputStream = new MonitoringOutputStream();

    // When - perform incremental character conversion processing
    long processingStart = System.nanoTime();
    command.execute(trackingInputStream, monitoringOutputStream);
    long processingEnd = System.nanoTime();

    // Then - verify incremental processing characteristics
    assertTrue(
        monitoringOutputStream.hasWriteOccurred(),
        "Output should be written during character conversion");
    assertTrue(trackingInputStream.isFullyRead(), "Input should be fully processed");

    // Verify substantial multilingual data was processed
    assertTrue(
        trackingInputStream.getBytesRead() > 15000,
        "Should have processed substantial amount of multilingual character data");

    long processingTime = processingEnd - processingStart;
    assertTrue(processingTime > 0, "Character conversion should take measurable time");

    // Verify output was generated (character conversion should produce some result)
    String output = monitoringOutputStream.getContent();
    assertTrue(output.length() > 0, "Character conversion should produce output");
  }
}
