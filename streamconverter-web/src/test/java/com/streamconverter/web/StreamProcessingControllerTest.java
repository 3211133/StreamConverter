package com.streamconverter.web;

import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.test.TestUtils;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("StreamProcessingController Web API Test")
@org.junit.jupiter.api.Disabled(
    "Persistent Netty 4.1.123.Final compatibility issue: isExplicitNoPreferDirect() method not found in WebTestClient despite version alignment")
class StreamProcessingControllerTest {

  @Autowired private WebTestClient webTestClient;

  @Test
  @DisplayName("Health check endpoint test")
  void testHealthEndpoint() {
    webTestClient
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

    webTestClient
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

    webTestClient
        .post()
        .uri("/api/v1/stream/json/extract?jsonPath=name")
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

    webTestClient
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

              // Verify pipeline executed successfully (data passed through both commands)
              assertEquals(
                  csvData.trim(),
                  responseString.trim(),
                  "With pass-through commands, input should equal output");

              // Verify byte count matches expectation
              assertEquals(
                  csvData.getBytes(StandardCharsets.UTF_8).length,
                  responseBody.length,
                  "Response size should match input size for pass-through pipeline");
            });
  }

  @Test
  @DisplayName("Invalid pipeline configuration test")
  void testInvalidPipelineConfiguration() {
    String csvData = TestUtils.createTestData("name,age,city", "John,30,NYC", "");
    DataBuffer dataBuffer =
        new DefaultDataBufferFactory().wrap(csvData.getBytes(StandardCharsets.UTF_8));

    webTestClient
        .post()
        .uri("/api/v1/stream/process")
        .header("X-Pipeline-Config", "invalid:command")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(Flux.just(dataBuffer), DataBuffer.class)
        .exchange()
        .expectStatus()
        .is5xxServerError();
  }
}
