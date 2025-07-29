package com.streamConverter.api.service;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.context.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** TransformServiceの単体テスト */
class TransformServiceTest {

  private TransformService transformService;

  @BeforeEach
  void setUp() {
    transformService = new TransformService();
    MDC.clear();
  }

  @Test
  @DisplayName("JSON値抽出が正常に動作する")
  void testJsonValueExtraction() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "JSON",
            "JSON",
            null,
            "$.user.id",
            "userId",
            null,
            "{\"user\":{\"id\":\"12345\",\"name\":\"John\"}}");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.transform(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
    assertEquals(context.getExecutionId(), response.getExecutionId());
    assertNotNull(response.getData());
    assertTrue(response.getInputSize() > 0);
    assertTrue(response.getOutputSize() > 0);

    // 抽出された値を確認
    assertTrue(response.getExtractedValues().containsKey("userId"));
    assertEquals("12345", response.getExtractedValues().get("userId"));
  }

  @Test
  @DisplayName("XML値抽出が正常に動作する")
  void testXmlValueExtraction() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "XML",
            "XML",
            null,
            "//user/@id",
            "userId",
            null,
            "<root><user id=\"12345\"><name>John</name></user></root>");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.transform(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
    assertTrue(response.getExtractedValues().containsKey("userId"));
    assertEquals("12345", response.getExtractedValues().get("userId"));
  }

  @Test
  @DisplayName("CSV値抽出が正常に動作する")
  void testCsvValueExtraction() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "CSV",
            "CSV",
            null,
            "user_id",
            "userId",
            null,
            "user_id,name,email\n12345,John,john@example.com\n67890,Jane,jane@example.com");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.transform(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
    assertTrue(response.getExtractedValues().containsKey("userId"));
    assertEquals("12345", response.getExtractedValues().get("userId"));
  }

  @Test
  @DisplayName("値抽出なしの場合も正常に動作する")
  void testTransformWithoutExtraction() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest("JSON", "JSON", null, null, null, null, "{\"message\":\"hello\"}");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.transform(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
    assertTrue(response.getExtractedValues().isEmpty());
  }

  @Test
  @DisplayName("JSONバリデーションが正常に動作する")
  void testJsonValidation() throws Exception {
    // Given - 一時的なスキーマファイルを作成
    String schemaPath = createTempJsonSchema();
    TransformRequest request =
        new TransformRequest(
            "JSON", null, null, null, null, schemaPath, "{\"user\":{\"id\":\"12345\"}}");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.validate(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
    assertEquals("Validation successful", response.getData());
  }

  @Test
  @DisplayName("CSVバリデーションが正常に動作する")
  void testCsvValidation() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "CSV",
            null,
            null,
            null,
            null,
            "user_id,name,email", // 必須カラムをカンマ区切りで指定
            "user_id,name,email\n12345,John,john@example.com");

    ExecutionContext context = ExecutionContext.create();

    // When
    TransformResponse response = transformService.validate(request, context);

    // Then
    assertEquals("SUCCESS", response.getStatus());
  }

  @Test
  @DisplayName("無効なデータ形式の場合例外が発生する")
  void testInvalidInputFormat() {
    // Given
    TransformRequest request =
        new TransformRequest("INVALID", "JSON", null, null, null, null, "test data");

    ExecutionContext context = ExecutionContext.create();

    // When & Then
    assertThrows(Exception.class, () -> transformService.transform(request, context));
  }

  @Test
  @DisplayName("バリデーションスキーマが未指定の場合例外が発生する")
  void testValidationWithoutSchema() {
    // Given
    TransformRequest request =
        new TransformRequest("JSON", null, null, null, null, null, "{\"test\":\"data\"}");

    ExecutionContext context = ExecutionContext.create();

    // When & Then
    assertThrows(Exception.class, () -> transformService.validate(request, context));
  }

  @Test
  @DisplayName("nullリクエストの場合例外が発生する")
  void testNullRequest() {
    // Given
    ExecutionContext context = ExecutionContext.create();

    // When & Then
    assertThrows(NullPointerException.class, () -> transformService.transform(null, context));
  }

  @Test
  @DisplayName("nullコンテキストの場合例外が発生する")
  void testNullContext() {
    // Given
    TransformRequest request =
        new TransformRequest("JSON", "JSON", null, null, null, null, "{\"test\":\"data\"}");

    // When & Then
    assertThrows(NullPointerException.class, () -> transformService.transform(request, null));
  }

  /** テスト用の一時的なJSONスキーマファイルを作成 */
  private String createTempJsonSchema() {
    try {
      java.io.File tempFile = java.io.File.createTempFile("test-schema", ".json");
      tempFile.deleteOnExit();

      String schema =
          "{"
              + "\"$schema\": \"http://json-schema.org/draft-07/schema#\","
              + "\"type\": \"object\","
              + "\"properties\": {"
              + "\"user\": {"
              + "\"type\": \"object\","
              + "\"properties\": {"
              + "\"id\": {\"type\": \"string\"}"
              + "}"
              + "}"
              + "}"
              + "}";

      java.nio.file.Files.write(tempFile.toPath(), schema.getBytes());
      return tempFile.getAbsolutePath();
    } catch (Exception e) {
      throw new RuntimeException("Failed to create temp schema file", e);
    }
  }
}
