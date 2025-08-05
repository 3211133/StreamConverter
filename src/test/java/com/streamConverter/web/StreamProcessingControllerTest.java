package com.streamConverter.web;

import static org.junit.jupiter.api.Assertions.*;

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
    String csvData = "name,age,city\nJohn,30,NYC\nJane,25,LA\n";
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
              assertTrue(responseString.contains("John") || responseString.contains("Jane"));
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
              assertNotNull(responseBody);
              String responseString = new String(responseBody, StandardCharsets.UTF_8);
              assertTrue(responseString.contains("John"));
            });
  }

  @Test
  @DisplayName("Pipeline processing endpoint test")
  void testPipelineProcessingEndpoint() {
    String csvData = "name,age,city\nJohn,30,NYC\nJane,25,LA\n";
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
              assertNotNull(responseBody);
              // Pipeline processing should return some processed data
              assertTrue(responseBody.length > 0);
            });
  }

  @Test
  @DisplayName("Invalid pipeline configuration test")
  void testInvalidPipelineConfiguration() {
    String csvData = "name,age,city\nJohn,30,NYC\n";
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
