package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("SampleStreamCommand Test")
class SampleStreamCommandTest {

  private String testInput;

  @BeforeEach
  void setUp() {
    testInput = "Hello, SampleStreamCommand!";
  }

  @Test
  @DisplayName("Default Constructor")
  void testDefaultConstructor() {
    // デフォルトコンストラクタのテスト
    SampleStreamCommand command = new SampleStreamCommand();
    assertEquals("default", command.toString().replaceAll(".*\\[id=(.*)\\]", "$1"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"test", "sample", "custom"})
  @DisplayName("Constructor with Argument")
  void testConstructorWithArgument(String id) {
    // 引数付きコンストラクタのテスト
    SampleStreamCommand command = new SampleStreamCommand(id);
    assertEquals(id, command.toString().replaceAll(".*\\[id=(.*)\\]", "$1"));
  }

  @Test
  @DisplayName("Execute Normal Case: Stream Copy")
  void testExecuteWithValidStreams() throws IOException {
    // 正常系のexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 入力がそのまま出力されるはず
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("Execute Error Case: Null Input Stream")
  void testExecuteWithNullInputStream() {
    // null入力ストリームでのexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");
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
    SampleStreamCommand command = new SampleStreamCommand("test");
    InputStream inputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NullPointerException.class,
        () -> {
          command.execute(inputStream, null);
        });
  }

  @Test
  @DisplayName("Execute: Empty Input Stream")
  void testExecuteWithEmptyInputStream() throws IOException {
    // 空の入力ストリームでのexecuteメソッドテスト
    SampleStreamCommand command = new SampleStreamCommand("test");

    try (InputStream inputStream = new ByteArrayInputStream(new byte[0]);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      command.execute(inputStream, outputStream);

      // 結果の検証 - 空の出力になるはず
      assertEquals("", outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("ToString Output Verification")
  void testToString() {
    // toStringメソッドのテスト
    String id = "testId";
    SampleStreamCommand command = new SampleStreamCommand(id);

    String expected = "SampleStreamCommand [id=" + id + "]";
    assertEquals(expected, command.toString());
  }
}
