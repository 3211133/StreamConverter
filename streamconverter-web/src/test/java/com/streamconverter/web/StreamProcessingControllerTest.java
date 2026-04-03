package com.streamconverter.web;

import static com.streamconverter.test.TestUtils.assertEqualsIgnoreLineEndings;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.test.TestUtils;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("StreamProcessingController Web API Test")
class StreamProcessingControllerTest {

  @LocalServerPort private int port;

  private WebTestClient webTestClient() {
    return WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
  }

  @Test
  @DisplayName("Health check endpoint test")
  void testHealthEndpoint() {
    webTestClient()
        .get()
        .uri("/api/v1/stream/health")
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(String.class)
        .value(response -> assertTrue(response.contains("StreamConverter Web API is running")));
  }

  @Test
  @DisplayName("CSV extraction endpoint test")
  void testCsvExtractionEndpoint() {
    String csvData = TestUtils.createTestData("name,age,city", "John,30,NYC", "Jane,25,LA", "");
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap(csvData.getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/csv/extract?columnName=name")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .expectBody()
        .consumeWith(
            result -> {
              byte[] responseBody = result.getResponseBody();
              assertNotNull(responseBody);
              String responseString = new String(responseBody, StandardCharsets.UTF_8);

              // Verify the response contains the complete CSV structure
              assertTrue(
                  responseString.contains("name,age,city"), "Response should contain CSV headers");
              assertTrue(
                  responseString.contains("John,30,NYC"),
                  "Response should contain John's complete record");
              assertTrue(
                  responseString.contains("Jane,25,LA"),
                  "Response should contain Jane's complete record");

              // Verify proper line structure
              String[] lines = responseString.trim().split("\\r?\\n");
              assertEquals(3, lines.length, "Should have header + 2 data rows");
              assertEquals("name,age,city", lines[0], "Header should be preserved");

              // With PassThroughRule, the entire CSV structure is maintained
              assertTrue(
                  lines[1].equals("John,30,NYC") || lines[1].equals("Jane,25,LA"),
                  "First data row should be complete");
              assertTrue(
                  lines[2].equals("Jane,25,LA") || lines[2].equals("John,30,NYC"),
                  "Second data row should be complete");
            });
  }

  @Test
  @DisplayName("JSON extraction endpoint test")
  void testJsonExtractionEndpoint() {
    String jsonData = "{\"name\":\"John\",\"age\":30,\"city\":\"NYC\"}";
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap(jsonData.getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/json/extract?jsonPath=$.name")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .expectBody()
        .consumeWith(
            result -> {
              byte[] responseBody = result.getResponseBody();
              assertNotNull(responseBody, "Response body should not be null");
              String responseString = new String(responseBody, StandardCharsets.UTF_8);

              // JsonNavigateCommand with PassThroughRule preserves the entire JSON
              assertFalse(responseString.isEmpty(), "Response should not be empty");

              // Verify the complete JSON structure is preserved
              assertTrue(
                  responseString.contains("\"name\":\"John\""),
                  "JSON should contain the name field");
              assertTrue(
                  responseString.contains("\"age\":30"), "JSON should contain the age field");
              assertTrue(
                  responseString.contains("\"city\":\"NYC\""),
                  "JSON should contain the city field");

              // Verify it's valid JSON structure
              assertTrue(
                  responseString.startsWith("{") && responseString.endsWith("}"),
                  "Response should maintain JSON object structure");

              // Verify exact match with input (pass-through behavior)
              assertEquals(
                  jsonData,
                  responseString.trim(),
                  "With PassThroughRule, input JSON should equal output JSON");

              // Verify byte count
              assertEquals(
                  jsonData.getBytes(StandardCharsets.UTF_8).length,
                  responseBody.length,
                  "Response size should match input size");
            });
  }

  @Test
  @DisplayName("Pipeline processing endpoint test")
  void testPipelineProcessingEndpoint() {
    String csvData = TestUtils.createTestData("name,age,city", "John,30,NYC", "Jane,25,LA", "");
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap(csvData.getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "csv:name,process:validator")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isOk()
        .expectHeader()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .expectBody()
        .consumeWith(
            result -> {
              byte[] responseBody = result.getResponseBody();
              assertNotNull(responseBody, "Response body should not be null");
              String responseString = new String(responseBody, StandardCharsets.UTF_8);

              // Verify that pipeline processing maintains data integrity
              assertFalse(responseString.isEmpty(), "Processed data should not be empty");

              // Pipeline: csv:name -> process:validator
              // Both commands use pass-through logic, so original data should be preserved
              assertTrue(
                  responseString.contains("name,age,city"),
                  "CSV headers should be preserved through pipeline");
              assertTrue(
                  responseString.contains("John") && responseString.contains("Jane"),
                  "Original data content should be preserved");

              // Verify data structure integrity
              String[] lines = responseString.trim().split("\\r?\\n");
              assertTrue(
                  lines.length >= 2, "Should have at least header and data rows after processing");

              // Verify pipeline executed successfully (data passed through both commands).
              // Use line-ending-agnostic comparison: CsvWriter outputs \r\n (RFC 4180 §2) but
              // TestUtils.createTestData uses System.lineSeparator() which is \n on Unix / \r\n on Windows.
              assertEqualsIgnoreLineEndings(
                  csvData.trim(),
                  responseString.trim(),
                  "With pass-through commands, input should equal output");
            });
  }

  @Test
  @DisplayName("Invalid pipeline configuration returns 400")
  void testInvalidPipelineConfiguration() {
    String csvData = TestUtils.createTestData("name,age,city", "John,30,NYC", "");
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap(csvData.getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "invalid:command")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("Trailing comma produces empty command type and returns 400")
  void testTrailingCommaIsRejected() {
    // "csv:name," のように末尾にカンマがあると空のコマンド型セグメントが生まれる
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "csv:name,")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("Oversized pipeline configuration returns 400")
  void testOversizedPipelineConfigurationIsRejected() {
    String longConfig = "csv:" + "a".repeat(1000);
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", longConfig)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("Too many commands in pipeline configuration returns 400")
  void testTooManyCommandsIsRejected() {
    String manyCommands = "process:a,process:b,process:c,process:d,process:e,"
        + "process:f,process:g,process:h,process:i,process:j,process:k";
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", manyCommands)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("Exactly 10 commands in pipeline is accepted")
  void testExactlyMaxCommandsIsAccepted() {
    // 上限ちょうど10件は受け入れられることを確認（境界値テスト）
    String tenCommands = "process:a,process:b,process:c,process:d,process:e,"
        + "process:f,process:g,process:h,process:i,process:j";
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", tenCommands)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @DisplayName("Parameter of exactly max length is accepted")
  void testExactlyMaxParameterLengthIsAccepted() {
    // パラメータがちょうど500文字は受け入れられることを確認（境界値テスト）
    String maxParam = "process:" + "a".repeat(500);
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", maxParam)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isOk();
  }

  @Test
  @DisplayName("Parameter exceeding max length by 1 returns 400")
  void testOversizedParameterIsRejected() {
    // パラメータが500文字を超える場合（501文字）は拒否される
    String longParam = "csv:" + "a".repeat(501);
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", longParam)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("csv command with empty parameter returns 400")
  void testCsvWithEmptyParameterIsRejected() {
    // "csv:" のようにパラメータが空の場合は拒否される
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "csv:")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  @DisplayName("json command with empty parameter returns 400")
  void testJsonWithEmptyParameterIsRejected() {
    // "json:" のようにパラメータが空の場合は拒否される
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap("data".getBytes(StandardCharsets.UTF_8));

    webTestClient()
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "json:")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .isBadRequest();
  }
}
