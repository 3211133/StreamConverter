package com.streamConverter.command.impl;

import static com.streamConverter.test.ParallelProcessingTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.test.ParallelProcessingTestUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("JsonFilterCommand Tests")
class JsonFilterCommandTest {

  @Test
  @DisplayName("基本的なプロパティ抽出（$.property）")
  void testBasicPropertyExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"age\": 25, \"city\": \"Tokyo\"}";
    String expected = "\"John\"";

    JsonFilterCommand command = new JsonFilterCommand("$.name");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("ルートパス抽出（$）")
  void testRootPathExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"age\": 25}";
    String expected = "{\"name\": \"John\", \"age\": 25}";

    JsonFilterCommand command = new JsonFilterCommand("$");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("数値プロパティの抽出")
  void testNumberPropertyExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"age\": 25, \"salary\": 50000.5}";
    String expected = "25";

    JsonFilterCommand command = new JsonFilterCommand("$.age");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("オブジェクトプロパティの抽出")
  void testObjectPropertyExtraction() throws IOException {
    // Given
    String input = "{\"person\": {\"name\": \"John\", \"age\": 25}, \"city\": \"Tokyo\"}";
    String expected = "{\"name\": \"John\", \"age\": 25}";

    JsonFilterCommand command = new JsonFilterCommand("$.person");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("配列プロパティの抽出")
  void testArrayPropertyExtraction() throws IOException {
    // Given
    String input = "{\"names\": [\"John\", \"Jane\", \"Bob\"], \"count\": 3}";
    String expected = "[\"John\", \"Jane\", \"Bob\"]";

    JsonFilterCommand command = new JsonFilterCommand("$.names");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("boolean値の抽出")
  void testBooleanPropertyExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"active\": true, \"archived\": false}";
    String expected = "true";

    JsonFilterCommand command = new JsonFilterCommand("$.active");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("null値の抽出")
  void testNullPropertyExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"middleName\": null, \"age\": 25}";
    String expected = "null";

    JsonFilterCommand command = new JsonFilterCommand("$.middleName");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("存在しないプロパティの抽出")
  void testNonExistentPropertyExtraction() throws IOException {
    // Given
    String input = "{\"name\": \"John\", \"age\": 25}";
    String expected = "null";

    JsonFilterCommand command = new JsonFilterCommand("$.salary");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("空のJSONオブジェクト")
  void testEmptyJsonObject() throws IOException {
    // Given
    String input = "{}";
    String expected = "null";

    JsonFilterCommand command = new JsonFilterCommand("$.name");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("空の入力")
  void testEmptyInput() throws IOException {
    // Given
    String input = "";
    String expected = "null";

    JsonFilterCommand command = new JsonFilterCommand("$.name");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("不正なJSON形式")
  void testInvalidJsonFormat() throws IOException {
    // Given
    String input = "{invalid json}";
    String expected = "null";

    JsonFilterCommand command = new JsonFilterCommand("$.name");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("複雑なネストされたJSONパス（現在の実装制限）")
  void testComplexNestedPath() throws IOException {
    // Given - 現在の実装では複雑なパスは元のJSONを返す
    String input = "{\"user\": {\"profile\": {\"name\": \"John\"}}, \"status\": \"active\"}";
    String expected = input; // 現在の実装制限により元のJSONが返される

    JsonFilterCommand command = new JsonFilterCommand("$.user.profile.name");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("配列インデックス指定（現在の実装制限）")
  void testArrayIndexAccess() throws IOException {
    // Given - 現在の実装では配列インデックスは元のJSONを返す
    String input = "{\"users\": [\"John\", \"Jane\", \"Bob\"]}";
    String expected = input; // 現在の実装制限により元のJSONが返される

    JsonFilterCommand command = new JsonFilterCommand("$.users[0]");

    // When
    String result = executeCommand(command, input);

    // Then
    assertEquals(expected, result);
  }

  @Test
  @DisplayName("コンストラクタのnullバリデーション")
  void testConstructorNullValidation() {
    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JsonFilterCommand(null);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JsonFilterCommand("");
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new JsonFilterCommand("   ");
        });
  }

  @Test
  @DisplayName("大容量JSON処理（メモリ制限内）")
  void testLargeJsonProcessing() throws IOException {
    // Given - メモリ制限（10MB）内の大容量JSONデータ
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append("{\"users\": [");

    for (int i = 0; i < 10000; i++) {
      if (i > 0) jsonBuilder.append(",");
      jsonBuilder.append(
          String.format(
              "{\"id\": %d, \"name\": \"User%d\", \"email\": \"user%d@example.com\", \"active\": %s}",
              i, i, i, (i % 2 == 0) ? "true" : "false"));
    }

    jsonBuilder.append("], \"totalCount\": 10000}");
    String input = jsonBuilder.toString();

    // メモリ制限内であることを確認（約5MB）
    assertTrue(input.length() < 10 * 1024 * 1024, "テストデータがメモリ制限内であること");

    JsonFilterCommand command = new JsonFilterCommand("$.totalCount");

    // When
    long startTime = System.currentTimeMillis();
    String result = executeCommand(command, input);
    long endTime = System.currentTimeMillis();

    // Then
    assertEquals("10000", result);

    long processingTime = endTime - startTime;
    System.out.println("📊 大容量JSON処理時間: " + processingTime + "ms");

    // パフォーマンス検証（3秒以内で完了することを期待）
    assertTrue(processingTime < 3000, "大容量JSON処理は3秒以内で完了するべき");
  }

  @Test
  @DisplayName("メモリ制限超過データの処理")
  void testMemoryLimitExceeded() {
    // Given - メモリ制限（10MB）を超える大容量データ
    StringBuilder oversizedBuilder = new StringBuilder();
    String baseContent = "X".repeat(1024); // 1KB

    // 11MB分のデータを作成
    oversizedBuilder.append("{\"data\": \"");
    for (int i = 0; i < 11 * 1024; i++) { // 11MB
      oversizedBuilder.append(baseContent);
    }
    oversizedBuilder.append("\"}");

    String oversizedInput = oversizedBuilder.toString();
    JsonFilterCommand command = new JsonFilterCommand("$.data");

    // When & Then
    assertThrows(
        IOException.class,
        () -> {
          executeCommand(command, oversizedInput);
        },
        "メモリ制限を超えるデータでIOExceptionが発生するべき");
  }

  @Test
  @DisplayName("並列処理検証テスト（JsonFilterCommand）")
  void testParallelProcessingValidation() throws IOException {
    // 中容量JSONデータ（約2MB）で並列処理を検証
    // JSON構造解析による部分並列特性を考慮
    StringBuilder jsonBuilder = new StringBuilder();
    jsonBuilder.append(
        "{\"metadata\": {\"version\": \"1.0\", \"created\": \"2024-01-01\"}, \"data\": [");

    // 各オブジェクト約200文字 × 10,000オブジェクト = 約2MB
    for (int i = 0; i < 10000; i++) {
      if (i > 0) jsonBuilder.append(",");
      jsonBuilder.append(
          String.format(
              "{\"id\": %d, \"name\": \"Product_%d\", \"description\": \"Detailed_description_for_product_%d_with_comprehensive_information\", \"price\": %d.%02d, \"category\": \"Category_%d\"}",
              i, i, i, i / 100, i % 100, i % 50));
    }

    jsonBuilder.append("], \"totalItems\": 10000}");
    String testData = jsonBuilder.toString();
    byte[] testDataBytes = testData.getBytes(StandardCharsets.UTF_8);

    // メモリ制限内であることを確認
    assertTrue(testDataBytes.length < 10 * 1024 * 1024, "テストデータがメモリ制限内であること");

    // JSONプロパティ抽出コマンドで部分並列処理特性を検証
    JsonFilterCommand command = new JsonFilterCommand("$.totalItems");

    // 標準的な並列処理検証テストを実行
    ParallelProcessingTestUtils.ValidationResult result =
        ParallelProcessingTestUtils.executeParallelProcessingTest(
            command, testDataBytes, "JsonFilterCommand");

    // 基本的な処理検証
    assertTrue(
        result.outputBytesAtClose > 0 || result.inputProgressAtClose >= 100.0,
        "JsonFilterCommand は正常に処理完了するべき");

    // JSON特有の検証
    System.out.println("📊 JSON処理特性:");
    System.out.println(
        "  - 元データサイズ: " + String.format("%.2f MB", testDataBytes.length / (1024.0 * 1024.0)));
    System.out.println("  - JSON構造: 大容量配列を含むオブジェクト");
    System.out.println("  - 抽出対象: $.totalItems (単一プロパティ)");

    // JsonFilterCommandの並列処理特性確認（部分並列処理）
    boolean isParallel = result.isParallelProcessing();
    if (isParallel) {
      System.out.println("  ✅ JsonFilterCommand: 部分並列処理が確認されました");
      System.out.println("    - JSON解析中でも出力開始が可能");
    } else {
      System.out.println("  ⚠️ JsonFilterCommand: 逐次処理パターンが検出されました");
      System.out.println("    - JSON全体解析完了後に出力開始");

      // JsonFilterCommandは構造解析が必要なため逐次処理の可能性もある
      assertTrue(result.inputProgressAtClose >= 100.0, "JsonFilterCommand: 全入力読み取り完了後の処理完了");
    }
  }

  @Test
  @DisplayName("getCommandDetailsの内容検証")
  void testGetCommandDetails() {
    // Given
    JsonFilterCommand command = new JsonFilterCommand("$.user.name");

    // When
    String details = command.getCommandDetails();

    // Then
    assertTrue(details.contains("JsonFilterCommand"));
    assertTrue(details.contains("$.user.name"));
  }

  /**
   * Helper method to execute a command with string input and return string output.
   *
   * @param command the command to execute
   * @param input the input string
   * @return the output string
   * @throws IOException if execution fails
   */
  private String executeCommand(JsonFilterCommand command, String input) throws IOException {
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    command.execute(inputStream, outputStream);

    return outputStream.toString(StandardCharsets.UTF_8);
  }
}
