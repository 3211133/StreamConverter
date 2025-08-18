package com.streamConverter.command.impl;

import static com.streamConverter.test.ParallelProcessingTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.test.ParallelProcessingTestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CsvFilterCommand Tests")
class CsvFilterCommandTest {

  @Test
  @DisplayName("基本的な列フィルタリング（列名指定）")
  void testBasicColumnFilteringByName() throws IOException {
    // Given
    String input = "name,age,city\nJohn,25,Tokyo\nJane,30,Osaka";
    String expected = "name,city\nJohn,Tokyo\nJane,Osaka";

    CsvFilterCommand command = new CsvFilterCommand("name", true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("基本的な列フィルタリング（インデックス指定）")
  void testBasicColumnFilteringByIndex() throws IOException {
    // Given
    String input = "name,age,city\nJohn,25,Tokyo\nJane,30,Osaka";
    String expected = "name,city\nJohn,Tokyo\nJane,Osaka";

    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("0", "2"), true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("複数列フィルタリング（列名指定）")
  void testMultipleColumnFilteringByName() throws IOException {
    // Given
    String input = "name,age,city,country\nJohn,25,Tokyo,Japan\nJane,30,Osaka,Japan";
    String expected = "name,city\nJohn,Tokyo\nJane,Osaka";

    List<String> columns = Arrays.asList("name", "city");
    CsvFilterCommand command = new CsvFilterCommand(columns, true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("ヘッダーなしCSVのフィルタリング")
  void testNoHeaderFiltering() throws IOException {
    // Given
    String input = "John,25,Tokyo\nJane,30,Osaka";
    String expected = "John,Tokyo\nJane,Osaka";

    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("0", "2"), false);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("クォート付きフィールドの処理")
  void testQuotedFields() throws IOException {
    // Given
    String input =
        "\"name\",\"age\",\"city\"\n\"John, Jr.\",25,\"Tokyo, Japan\"\n\"Jane\",30,\"Osaka\"";
    String expected = "\"name\",\"city\"\n\"John, Jr.\",\"Tokyo, Japan\"\n\"Jane\",\"Osaka\"";

    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("name", "city"), true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("空のCSVファイルの処理")
  void testEmptyFile() throws IOException {
    // Given
    String input = "";

    CsvFilterCommand command = new CsvFilterCommand("name", true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals("", result);
  }

  @Test
  @DisplayName("存在しない列名の指定")
  void testNonExistentColumn() {
    // Given
    String input = "name,age,city\nJohn,25,Tokyo";

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          CsvFilterCommand command = new CsvFilterCommand("salary", true);
          executeCommand(command, input);
        });
  }

  @Test
  @DisplayName("範囲外インデックスの指定")
  void testOutOfRangeIndex() {
    // Given
    String input = "name,age,city\nJohn,25,Tokyo";

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          CsvFilterCommand command = new CsvFilterCommand("5", true);
          executeCommand(command, input);
        });
  }

  @Test
  @DisplayName("不正なインデックス形式（ヘッダーなし）")
  void testInvalidIndexFormatNoHeader() {
    // Given
    String input = "John,25,Tokyo";

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          CsvFilterCommand command = new CsvFilterCommand("invalid", false);
          executeCommand(command, input);
        });
  }

  @Test
  @DisplayName("コンストラクタのnullバリデーション")
  void testConstructorNullValidation() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CsvFilterCommand((String) null, true);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CsvFilterCommand((List<String>) null, true);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new CsvFilterCommand("", true);
        });
  }

  @Test
  @DisplayName("部分的に欠損した行の処理")
  void testIncompleteRows() throws IOException {
    // Given
    String input = "name,age,city\nJohn,25,Tokyo\nJane,30\nBob,,Kyoto";
    String expected = "name,city\nJohn,Tokyo\nJane,\nBob,Kyoto";

    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("name", "city"), true);

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("大容量データの処理効率性")
  void testLargeDataProcessing() throws IOException {
    // Given - 大容量CSVデータの作成
    StringBuilder inputBuilder = new StringBuilder();
    StringBuilder expectedBuilder = new StringBuilder();

    inputBuilder.append("id,name,description,category,value\n");
    expectedBuilder.append("name,value\n");

    for (int i = 0; i < 10000; i++) {
      String row =
          String.format(
              "%d,Product%d,Description for product %d,Category%d,%d.%02d\n",
              i, i, i, i % 10, i / 100, i % 100);
      inputBuilder.append(row);

      String expectedRow = String.format("Product%d,%d.%02d\n", i, i / 100, i % 100);
      expectedBuilder.append(expectedRow);
    }

    String input = inputBuilder.toString();
    String expected = expectedBuilder.toString();

    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("name", "value"), true);

    // When
    long startTime = System.currentTimeMillis();
    String result = executeCommand(command, input);
    long endTime = System.currentTimeMillis();

    // Then
    assertEquals(expected, result);

    long processingTime = endTime - startTime;
    System.out.println("📊 大容量データ処理時間: " + processingTime + "ms");

    // パフォーマンス検証（5秒以内で完了することを期待）
    assertTrue(processingTime < 5000, "大容量データ処理は5秒以内で完了するべき");
  }

  @Test
  @DisplayName("並列処理検証テスト（CsvFilterCommand）")
  void testParallelProcessingValidation() throws IOException {
    // 大容量CSVデータ（約3MB）で並列処理を検証
    StringBuilder inputBuilder = new StringBuilder();
    inputBuilder.append("id,name,description,category,value,status,date\n");

    // 各行約150文字 × 20,000行 = 約3MB
    for (int i = 0; i < 20000; i++) {
      inputBuilder.append(
          String.format(
              "%d,Product_%d,Very_long_description_for_product_%d_with_detailed_information,Category_%d,%d.%02d,Active,2024-01-01\n",
              i, i, i, i % 100, i / 100, i % 100));
    }

    String testData = inputBuilder.toString();
    byte[] testDataBytes = testData.getBytes(StandardCharsets.UTF_8);

    // 複数列フィルタリングコマンドで並列処理特性を検証
    CsvFilterCommand command =
        new CsvFilterCommand(Arrays.asList("name", "category", "value"), true);

    // 標準的な並列処理検証テストを実行
    ParallelProcessingTestUtils.ValidationResult result =
        ParallelProcessingTestUtils.executeParallelProcessingTest(
            command, testDataBytes, "CsvFilterCommand");

    // 基本的な処理検証
    assertTrue(
        result.outputBytesAtClose > 0 || result.inputProgressAtClose >= 100.0,
        "CsvFilterCommand は正常に処理完了するべき");

    // CSV特有の検証
    System.out.println("📊 CSV処理特性:");
    System.out.println(
        "  - 元データサイズ: " + String.format("%.2f MB", testDataBytes.length / (1024.0 * 1024.0)));
    System.out.println(
        "  - 予想フィルタ後サイズ: 約" + String.format("%.1f%%", (3.0 / 7.0) * 100) + " (3列/7列)");

    // CsvFilterCommandの並列処理特性確認
    boolean isParallel = result.isParallelProcessing();
    if (isParallel) {
      System.out.println("  ✅ CsvFilterCommand: 行単位並列処理が確認されました");
    } else {
      System.out.println("  ⚠️ CsvFilterCommand: 逐次処理パターンが検出されました");
    }
  }

  @Test
  @DisplayName("getCommandDetailsの内容検証")
  void testGetCommandDetails() {
    // Given
    CsvFilterCommand command = new CsvFilterCommand(Arrays.asList("name", "age"), true);

    // When
    String details = command.getCommandDetails();

    // Then
    assertTrue(details.contains("CsvFilterCommand"));
    assertTrue(details.contains("name"));
    assertTrue(details.contains("age"));
    assertTrue(details.contains("hasHeader=true"));
  }

  /**
   * Helper method to execute a command with string input and return string output.
   *
   * @param command the command to execute
   * @param input the input string
   * @return the output string
   * @throws IOException if execution fails
   */
  private String executeCommand(CsvFilterCommand command, String input) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
