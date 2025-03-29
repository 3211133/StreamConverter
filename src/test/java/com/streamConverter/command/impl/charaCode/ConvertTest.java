package com.streamConverter.command.impl.charaCode;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import org.junit.jupiter.api.Disabled;
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
    convert command = new convert("UTF-8", "UTF-16");
    assertNotNull(command);
  }

  @ParameterizedTest
  @CsvSource({"UTF-8, UTF-16", "UTF-16, UTF-8"})
  @DisplayName("Execute Normal Case: Character Code Conversion")
  void testExecuteWithValidCharsets(String fromCharset, String toCharset) throws IOException {
    // 正常系のexecuteメソッドテスト
    convert command = new convert(fromCharset, toCharset);

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
    convert command = new convert("UTF-8", "UTF-16");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        NullPointerException.class,
        () -> {
          command.execute(null, outputStream);
        });
  }

  @Test
  @DisplayName("Execute Error Case: Null Output Stream")
  void testExecuteWithNullOutputStream() {
    // null出力ストリームでのexecuteメソッドテスト
    convert command = new convert("UTF-8", "UTF-16");
    InputStream inputStream =
        new ByteArrayInputStream(TEST_STRING.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NullPointerException.class,
        () -> {
          command.execute(inputStream, null);
        });
  }

  @Test
  @DisplayName("Execute Error Case: Invalid Input Character Code")
  void testExecuteWithInvalidInputCharset() {
    // 無効な入力文字コードでのconstructorテスト
    // 例外が発生することを期待
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new convert("INVALID-CHARSET", "UTF-8");
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
          new convert("UTF-8", "INVALID-CHARSET");
        });
  }

  @Test
  @DisplayName("Execute: Empty Input Stream")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    convert command = new convert("UTF-8", "UTF-16");

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
    convert command = new convert("UTF-8", "UTF-16");

    try (InputStream inputStream =
            new ByteArrayInputStream(japaneseText.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証
      String result = new String(outputStream.toByteArray(), StandardCharsets.UTF_16);
      assertEquals(japaneseText, result);
    }
  }
}
