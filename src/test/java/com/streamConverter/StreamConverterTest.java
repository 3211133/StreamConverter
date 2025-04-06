package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StreamConverter Test")
class StreamConverterTest {

  private IStreamCommand[] validCommands;
  private String testInput;

  @BeforeEach
  void setUp() {
    // テスト前の準備
    validCommands =
        new IStreamCommand[] {new SampleStreamCommand("test1"), new SampleStreamCommand("test2")};
    testInput = "Hello, StreamConverter!";
  }

  @Test
  @DisplayName("Constructor Normal Case: Valid Command Array")
  void testConstructorWithValidCommandArray() {
    // 配列コンストラクタのテスト
    assertDoesNotThrow(
        () -> {
          new StreamConverter(validCommands);
        });
  }

  @Test
  @DisplayName("Constructor Normal Case: Valid Command List")
  void testConstructorWithValidCommandList() {
    // リストコンストラクタのテスト
    List<IStreamCommand> commandList = new ArrayList<>();
    commandList.add(new SampleStreamCommand("test1"));

    assertDoesNotThrow(
        () -> {
          new StreamConverter(commandList);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Null Command Array")
  void testConstructorWithNullCommandArray() {
    // nullコマンド配列でのコンストラクタテスト
    IStreamCommand[] nullCommands = null;

    assertThrows(
        NullPointerException.class,
        () -> {
          new StreamConverter(nullCommands);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Null Command List")
  void testConstructorWithNullCommandList() {
    // nullコマンドリストでのコンストラクタテスト
    List<IStreamCommand> nullCommandList = null;

    assertThrows(
        NullPointerException.class,
        () -> {
          new StreamConverter(nullCommandList);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Empty Command Array")
  void testConstructorWithEmptyCommandArray() {
    // 空のコマンド配列でのコンストラクタテスト
    IStreamCommand[] emptyCommands = new IStreamCommand[0];

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new StreamConverter(emptyCommands);
        });
  }

  @Test
  @DisplayName("Constructor Error Case: Empty Command List")
  void testConstructorWithEmptyCommandList() {
    // 空のコマンドリストでのコンストラクタテスト
    List<IStreamCommand> emptyCommandList = new ArrayList<>();

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new StreamConverter(emptyCommandList);
        });
  }

  @Test
  @DisplayName("Run Normal Case: Input/Output Stream Processing")
  void testRunWithValidStreams() throws IOException {
    // 正常系のrunメソッドテスト
    StreamConverter converter = new StreamConverter(validCommands);

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      List<Object> result = converter.run(inputStream, outputStream);

      // 結果の検証
      assertNotNull(result);
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("Run Error Case: Null Input Stream")
  void testRunWithNullInputStream() {
    // null入力ストリームでのrunメソッドテスト
    StreamConverter converter = new StreamConverter(validCommands);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(null, outputStream);
        });
  }

  @Test
  @DisplayName("Run Error Case: Null Output Stream")
  void testRunWithNullOutputStream() {
    // null出力ストリームでのrunメソッドテスト
    StreamConverter converter = new StreamConverter(validCommands);
    InputStream inputStream = new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(inputStream, null);
        });
  }

  @Test
  @DisplayName("Multiple Commands Integration Test")
  void testMultipleCommands() throws IOException {
    // 複数コマンドを使用した場合のテスト
    IStreamCommand[] commands =
        new IStreamCommand[] {
          new SampleStreamCommand("1"), new SampleStreamCommand("2"), new SampleStreamCommand("3")
        };

    StreamConverter converter = new StreamConverter(commands);

    try (InputStream inputStream =
            new ByteArrayInputStream(testInput.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      converter.run(inputStream, outputStream);

      // 結果の検証 - SampleStreamCommandは単純にコピーするだけなので、入力と同じ出力になるはず
      assertEquals(testInput, outputStream.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  @DisplayName("large file size test")
  @Disabled
  void testLargeFileSize() throws IOException {
    // 大きなファイルサイズのテスト
  }
}
