package com.streamConverter.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.api.service.TransformService;
import com.streamConverter.context.ExecutionContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** StreamConverterControllerの単体テスト */
@WebMvcTest(StreamConverterController.class)
class StreamConverterControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private TransformService transformService;

  @Test
  @DisplayName("ヘルスチェックエンドポイントが正常に動作する")
  void testHealthEndpoint() throws Exception {
    mockMvc
        .perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.service").value("StreamConverter API"));
  }

  @Test
  @WithMockUser
  @DisplayName("変換APIが正常に動作する")
  void testTransformEndpoint() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "JSON",
            "XML",
            "http://example.com/api",
            "$.user.id",
            "userId",
            null,
            "{\"user\":{\"id\":\"12345\",\"name\":\"John\"}}");

    Map<String, String> extractedValues = new HashMap<>();
    extractedValues.put("userId", "12345");

    TransformResponse expectedResponse =
        TransformResponse.success(
            "<user><id>12345</id><name>John</name></user>",
            "EXEC123",
            100L,
            42L,
            45L,
            extractedValues);

    when(transformService.transform(any(TransformRequest.class), any(ExecutionContext.class)))
        .thenReturn(expectedResponse);

    // When & Then
    mockMvc
        .perform(
            post("/api/v1/transform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.executionId").value("EXEC123"))
        .andExpect(jsonPath("$.data").value("<user><id>12345</id><name>John</name></user>"))
        .andExpect(jsonPath("$.extractedValues.userId").value("12345"));
  }

  @Test
  @WithMockUser
  @DisplayName("バリデーションAPIが正常に動作する")
  void testValidateEndpoint() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "JSON", null, null, null, null, "schema/user.json", "{\"user\":{\"id\":\"12345\"}}");

    TransformResponse expectedResponse =
        TransformResponse.success(
            "Validation successful", "EXEC456", 50L, 25L, 0L, new HashMap<>());

    when(transformService.validate(any(TransformRequest.class), any(ExecutionContext.class)))
        .thenReturn(expectedResponse);

    // When & Then
    mockMvc
        .perform(
            post("/api/v1/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("SUCCESS"))
        .andExpect(jsonPath("$.data").value("Validation successful"));
  }

  @Test
  @WithMockUser
  @DisplayName("無効なリクエストの場合400エラーが返される")
  void testInvalidRequest() throws Exception {
    // Given - 必須フィールドが不足しているリクエスト
    TransformRequest invalidRequest = new TransformRequest();
    invalidRequest.setInputFormat(""); // 空文字
    invalidRequest.setOutputFormat("INVALID"); // 無効な形式
    // dataフィールドがnull

    // When & Then
    mockMvc
        .perform(
            post("/api/v1/transform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
  }

  @Test
  @WithMockUser
  @DisplayName("サービスエラーの場合500エラーが返される")
  void testServiceError() throws Exception {
    // Given
    TransformRequest request =
        new TransformRequest(
            "JSON", "XML", null, null, null, null, "{\"user\":{\"id\":\"12345\"}}");

    when(transformService.transform(any(TransformRequest.class), any(ExecutionContext.class)))
        .thenThrow(new RuntimeException("Internal service error"));

    // When & Then
    mockMvc
        .perform(
            post("/api/v1/transform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isInternalServerError())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.status").value("ERROR"))
        .andExpect(jsonPath("$.errorMessage").exists());
  }

  @Test
  @DisplayName("認証なしの場合401エラーが返される")
  void testUnauthorizedAccess() throws Exception {
    TransformRequest request =
        new TransformRequest(
            "JSON", "XML", null, null, null, null, "{\"user\":{\"id\":\"12345\"}}");

    mockMvc
        .perform(
            post("/api/v1/transform")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }
}
