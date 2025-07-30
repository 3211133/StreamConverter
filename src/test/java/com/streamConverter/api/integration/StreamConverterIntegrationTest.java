package com.streamConverter.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streamConverter.api.dto.BatchTransformRequest;
import com.streamConverter.api.dto.BatchTransformResponse;
import com.streamConverter.api.dto.TransformRequest;
import com.streamConverter.api.dto.TransformResponse;
import com.streamConverter.config.TestSecurityConfig;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

/**
 * StreamConverter WebAPI統合テスト
 *
 * <p>実際のサーバーを起動して全機能のエンドツーエンドテストを実行します。
 *
 * <p>テスト対象:
 *
 * <ul>
 *   <li>基本的なAPI機能（transform, validate, health）
 *   <li>バッチ処理機能（同期・非同期）
 *   <li>キャッシュ機能
 *   <li>認証・セキュリティ
 *   <li>エラーハンドリング
 * </ul>
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class StreamConverterIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  private String baseUrl;
  private HttpHeaders authHeaders;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port + "/api/v1";

    // Basic認証ヘッダーの設定
    authHeaders = new HttpHeaders();
    authHeaders.setContentType(MediaType.APPLICATION_JSON);
    authHeaders.setBasicAuth("admin", "streamconverter123");
  }

  /** ヘルスチェックエンドポイントのテスト */
  @Test
  @Order(1)
  void testHealthEndpoint() {
    // ヘルスエンドポイントは認証が必要な場合がある
    HttpEntity<Void> entity = new HttpEntity<>(authHeaders);
    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/health", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
    assertThat(response.getBody()).contains("\"service\":\"StreamConverter API\"");
  }

  /** 基本的な変換APIのテスト */
  @Test
  @Order(2)
  void testBasicTransformAPI() throws Exception {
    // テストデータ準備
    TransformRequest request = new TransformRequest();
    request.setData("name,age\nJohn,30\nJane,25");
    request.setInputFormat("CSV");
    request.setOutputFormat("JSON");
    request.setExtractPath("name");
    request.setMdcKey("userName");

    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, authHeaders);

    // API呼び出し
    ResponseEntity<TransformResponse> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, TransformResponse.class);

    // 結果検証
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getStatus()).isEqualTo("SUCCESS");
    assertThat(response.getBody().getExecutionId()).isNotNull();
    assertThat(response.getBody().getInputSize()).isEqualTo(24);
    assertThat(response.getBody().getExtractedValues()).containsKey("userName");
    assertThat(response.getBody().getExtractedValues().get("userName")).isEqualTo("John");
  }

  /** 認証エラーのテスト */
  @Test
  @Order(3)
  void testAuthenticationRequired() {
    TransformRequest request = new TransformRequest();
    request.setData("test data");
    request.setInputFormat("CSV");
    request.setOutputFormat("JSON");

    // 認証ヘッダーなしでリクエスト
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, new HttpHeaders());

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** バリデーションエラーのテスト */
  @Test
  @Order(4)
  void testValidationErrors() {
    // 必須フィールドが不足したリクエスト
    TransformRequest request = new TransformRequest();
    // dataフィールドを設定しない（必須フィールド）
    request.setInputFormat("CSV");
    request.setOutputFormat("JSON");

    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, authHeaders);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).contains("データは必須です");
  }

  /** 同期バッチ処理のテスト */
  @Test
  @Order(5)
  void testSynchronousBatchProcessing() throws Exception {
    // バッチリクエスト準備
    TransformRequest dataset1 = createTransformRequest("name,age\nAlice,25", "name", "user1");
    TransformRequest dataset2 = createTransformRequest("name,age\nBob,30", "age", "user2");

    BatchTransformRequest batchRequest = new BatchTransformRequest();
    batchRequest.setBatchId("TEST-SYNC-BATCH");
    batchRequest.setDatasets(Arrays.asList(dataset1, dataset2));
    batchRequest.setAsyncProcessing(false);
    batchRequest.setParallelism(2);
    batchRequest.setContinueOnError(true);

    HttpEntity<BatchTransformRequest> entity = new HttpEntity<>(batchRequest, authHeaders);

    // バッチAPI呼び出し
    ResponseEntity<BatchTransformResponse> response =
        restTemplate.postForEntity(
            baseUrl + "/batch/transform", entity, BatchTransformResponse.class);

    // 結果検証
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    BatchTransformResponse batchResponse = response.getBody();
    assertThat(batchResponse).isNotNull();
    assertThat(batchResponse.getBatchId()).isEqualTo("TEST-SYNC-BATCH");
    assertThat(batchResponse.getStatus()).isEqualTo("COMPLETED");
    assertThat(batchResponse.getTotalCount()).isEqualTo(2);
    assertThat(batchResponse.getSuccessCount()).isEqualTo(2);
    assertThat(batchResponse.getFailureCount()).isEqualTo(0);
    assertThat(batchResponse.getProgressPercentage()).isEqualTo(100.0);
    assertThat(batchResponse.getResults()).hasSize(2);

    // 個別結果の検証
    assertThat(batchResponse.getResults().get(0).getStatus()).isEqualTo("SUCCESS");
    assertThat(batchResponse.getResults().get(1).getStatus()).isEqualTo("SUCCESS");
  }

  /** 非同期バッチ処理のテスト */
  @Test
  @Order(6)
  void testAsynchronousBatchProcessing() throws Exception {
    // 非同期バッチリクエスト準備
    TransformRequest dataset = createTransformRequest("name,age\nCharlie,35", "name", "asyncUser");

    BatchTransformRequest batchRequest = new BatchTransformRequest();
    batchRequest.setBatchId("TEST-ASYNC-BATCH");
    batchRequest.setDatasets(Arrays.asList(dataset));
    batchRequest.setAsyncProcessing(true);
    batchRequest.setParallelism(1);

    HttpEntity<BatchTransformRequest> entity = new HttpEntity<>(batchRequest, authHeaders);

    // 非同期バッチAPI呼び出し
    ResponseEntity<BatchTransformResponse> submitResponse =
        restTemplate.postForEntity(
            baseUrl + "/batch/transform", entity, BatchTransformResponse.class);

    // 受付確認
    assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    BatchTransformResponse submitResult = submitResponse.getBody();
    assertThat(submitResult).isNotNull();
    assertThat(submitResult.getBatchId()).isEqualTo("TEST-ASYNC-BATCH");
    assertThat(submitResult.getStatus()).isEqualTo("SUBMITTED");
    assertThat(submitResult.getStatusUrl()).isEqualTo("/api/v1/batch/TEST-ASYNC-BATCH/status");

    // 処理完了まで待機（最大5秒）
    Thread.sleep(2000);

    // ステータス確認
    HttpEntity<Void> statusEntity = new HttpEntity<>(authHeaders);
    ResponseEntity<BatchTransformResponse> statusResponse =
        restTemplate.exchange(
            baseUrl + "/batch/TEST-ASYNC-BATCH/status",
            HttpMethod.GET,
            statusEntity,
            BatchTransformResponse.class);

    assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    BatchTransformResponse statusResult = statusResponse.getBody();
    assertThat(statusResult).isNotNull();
    assertThat(statusResult.getBatchId()).isEqualTo("TEST-ASYNC-BATCH");
    assertThat(statusResult.getStatus()).isIn("PROCESSING", "COMPLETED");

    // 完了している場合の詳細確認
    if ("COMPLETED".equals(statusResult.getStatus())) {
      assertThat(statusResult.getTotalCount()).isEqualTo(1);
      assertThat(statusResult.getSuccessCount()).isEqualTo(1);
      assertThat(statusResult.getResults()).hasSize(1);
      assertThat(statusResult.getResults().get(0).getExtractedValues())
          .containsEntry("asyncUser", "Charlie");
    }
  }

  /** 存在しないバッチのステータス確認テスト */
  @Test
  @Order(7)
  void testNonExistentBatchStatus() {
    HttpEntity<Void> entity = new HttpEntity<>(authHeaders);

    ResponseEntity<BatchTransformResponse> response =
        restTemplate.exchange(
            baseUrl + "/batch/NON-EXISTENT-BATCH/status",
            HttpMethod.GET,
            entity,
            BatchTransformResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
  }

  /** キャッシュ機能のテスト（同じリクエストの高速化確認） */
  @Test
  @Order(8)
  void testCachingPerformance() throws Exception {
    TransformRequest request =
        createTransformRequest("name,age\nCacheTest,40", "name", "cacheUser");
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, authHeaders);

    // 1回目の実行（キャッシュミス）
    long startTime1 = System.currentTimeMillis();
    ResponseEntity<TransformResponse> response1 =
        restTemplate.postForEntity(baseUrl + "/transform", entity, TransformResponse.class);
    long duration1 = System.currentTimeMillis() - startTime1;

    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 2回目の実行（キャッシュヒット期待）
    long startTime2 = System.currentTimeMillis();
    ResponseEntity<TransformResponse> response2 =
        restTemplate.postForEntity(baseUrl + "/transform", entity, TransformResponse.class);
    long duration2 = System.currentTimeMillis() - startTime2;

    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 両方成功し、同じ結果が得られることを確認
    assertThat(response1.getBody().getExtractedValues())
        .isEqualTo(response2.getBody().getExtractedValues());

    // キャッシュによる高速化の確認（2回目が1回目より速いか、少なくとも同程度）
    System.out.println("First request: " + duration1 + "ms, Second request: " + duration2 + "ms");
    assertThat(duration2).isLessThanOrEqualTo(duration1 + 50); // 50ms のマージン
  }

  /** 大量データでのバッチ処理テスト */
  @Test
  @Order(9)
  void testLargeBatchProcessing() throws Exception {
    // 10件のデータセットを作成
    List<TransformRequest> datasets =
        Arrays.asList(
            createTransformRequest("name,age\nUser1,20", "name", "user1"),
            createTransformRequest("name,age\nUser2,21", "name", "user2"),
            createTransformRequest("name,age\nUser3,22", "name", "user3"),
            createTransformRequest("name,age\nUser4,23", "name", "user4"),
            createTransformRequest("name,age\nUser5,24", "name", "user5"),
            createTransformRequest("name,age\nUser6,25", "name", "user6"),
            createTransformRequest("name,age\nUser7,26", "name", "user7"),
            createTransformRequest("name,age\nUser8,27", "name", "user8"),
            createTransformRequest("name,age\nUser9,28", "name", "user9"),
            createTransformRequest("name,age\nUser10,29", "name", "user10"));

    BatchTransformRequest batchRequest = new BatchTransformRequest();
    batchRequest.setBatchId("LARGE-BATCH-TEST");
    batchRequest.setDatasets(datasets);
    batchRequest.setAsyncProcessing(false);
    batchRequest.setParallelism(5); // 5並列で処理
    batchRequest.setContinueOnError(true);

    HttpEntity<BatchTransformRequest> entity = new HttpEntity<>(batchRequest, authHeaders);

    long startTime = System.currentTimeMillis();
    ResponseEntity<BatchTransformResponse> response =
        restTemplate.postForEntity(
            baseUrl + "/batch/transform", entity, BatchTransformResponse.class);
    long duration = System.currentTimeMillis() - startTime;

    // 結果検証
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    BatchTransformResponse result = response.getBody();
    assertThat(result).isNotNull();
    assertThat(result.getTotalCount()).isEqualTo(10);
    assertThat(result.getSuccessCount()).isEqualTo(10);
    assertThat(result.getFailureCount()).isEqualTo(0);
    assertThat(result.getResults()).hasSize(10);

    // 統計情報の確認
    assertThat(result.getStatistics()).containsKey("averageProcessingTime");
    assertThat(result.getStatistics()).containsKey("totalInputSize");
    assertThat(result.getStatistics()).containsKey("totalOutputSize");

    System.out.println("Large batch processing completed in " + duration + "ms");
    System.out.println("Average per item: " + (duration / 10.0) + "ms");
  }

  /** Actuatorエンドポイントのテスト */
  @Test
  @Order(10)
  void testActuatorEndpoints() {
    // Actuator health endpoint（認証あり）
    HttpEntity<Void> authEntity = new HttpEntity<>(authHeaders);
    ResponseEntity<String> healthResponse =
        restTemplate.exchange(
            "http://localhost:" + port + "/actuator/health",
            HttpMethod.GET,
            authEntity,
            String.class);

    assertThat(healthResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(healthResponse.getBody()).contains("\"status\":\"UP\"");

    // Actuator info endpoint（認証あり、利用可能な場合）
    ResponseEntity<String> infoResponse =
        restTemplate.exchange(
            "http://localhost:" + port + "/actuator/info",
            HttpMethod.GET,
            authEntity,
            String.class);

    // 設定によってはアクセスできない可能性があるので、404も許容
    assertThat(infoResponse.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.NOT_FOUND);
  }

  /** テスト用のTransformRequestを作成するヘルパーメソッド */
  private TransformRequest createTransformRequest(String data, String extractPath, String mdcKey) {
    TransformRequest request = new TransformRequest();
    request.setData(data);
    request.setInputFormat("CSV");
    request.setOutputFormat("JSON");
    request.setExtractPath(extractPath);
    request.setMdcKey(mdcKey);
    return request;
  }
}
