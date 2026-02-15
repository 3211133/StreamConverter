package com.streamconverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("StreamConverter Test")
class StreamConverterTest {

  private IStreamCommand[] validCommands;
  private String testInput;

  @BeforeEach
  void setUp() {
    // テスト前の準備
    validCommands =
        new IStreamCommand[] {(in, out) -> in.transferTo(out), (in, out) -> in.transferTo(out)};
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
    commandList.add((in, out) -> in.transferTo(out));

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

      List<CommandResult> result = converter.run(inputStream, outputStream);

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
          (in, out) -> in.transferTo(out),
          (in, out) -> in.transferTo(out),
          (in, out) -> in.transferTo(out)
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
  @DisplayName("large data memory efficiency test - cross-platform adaptive")
  @EnabledOnOs(OS.LINUX)
  void testLargeDataMemoryEfficiency() throws IOException {
    // プラットフォーム適応型メモリ効率性テスト
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // プラットフォーム/環境に応じたテストサイズ調整
    long testDataSize = Math.min(maxMemory / 10, 50 * 1024 * 1024); // ヒープの10%または50MB
    long maxMemoryThresholdMB = testDataSize / (1024 * 1024) * 2; // テストデータの2倍まで許可

    long initialMemory = runtime.totalMemory() - runtime.freeMemory();

    // 大容量データのストリーム生成（実際のファイルを作らずにメモリ効率的に）
    InputStream largeInputStream =
        new InputStream() {
          private long bytesRead = 0;
          private final byte[] pattern =
              "Large file test data pattern for memory efficiency testing.\n"
                  .getBytes(StandardCharsets.UTF_8);
          private int patternIndex = 0;

          @Override
          public int read() throws IOException {
            if (bytesRead >= testDataSize) {
              return -1; // EOF
            }
            int data = pattern[patternIndex] & 0xFF;
            patternIndex = (patternIndex + 1) % pattern.length;
            bytesRead++;
            return data;
          }
        };

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    StreamConverter converter =
        StreamConverter.create((in, out) -> in.transferTo(out), (in, out) -> in.transferTo(out));

    // メモリ使用量監視しながら実行
    long startTime = System.currentTimeMillis();
    converter.run(largeInputStream, outputStream);
    long endTime = System.currentTimeMillis();

    // メモリ使用量チェック
    runtime.gc(); // ガベージコレクション実行
    long finalMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsedMB = (finalMemory - initialMemory) / (1024 * 1024);

    // プラットフォーム適応型アサーション
    assertTrue(
        memoryUsedMB <= maxMemoryThresholdMB,
        "Memory usage should be <= " + maxMemoryThresholdMB + "MB, but was " + memoryUsedMB + "MB");

    // 処理時間をデータサイズに比例して調整（1MBあたり1秒、最大30秒）
    long maxProcessingTimeMs = Math.min((testDataSize / (1024 * 1024)) * 1000, 30000);
    long processingTimeMs = endTime - startTime;
    assertTrue(
        processingTimeMs <= maxProcessingTimeMs,
        String.format(
            "Processing time should be <= %dms for %dMB data, but was %dms",
            maxProcessingTimeMs, testDataSize / (1024 * 1024), processingTimeMs));

    // 出力サイズが入力サイズと一致することを確認
    assertEquals(testDataSize, outputStream.size(), "Output size should match input size");
  }
}
