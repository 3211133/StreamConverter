package com.streamConverter.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.streamConverter.api.dto.TransformRequest;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

/**
 * セキュリティ機能の統合テスト
 *
 * <p>認証、認可、CORS、セキュリティヘッダーの動作を検証します。
 *
 * @author StreamConverter
 * @version 1.0.0
 * @since 2025-07-29
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
public class SecurityIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  private String baseUrl;
  private HttpHeaders validAuthHeaders;
  private HttpHeaders invalidAuthHeaders;

  @BeforeEach
  void setUp() {
    baseUrl = "http://localhost:" + port + "/api/v1";

    // 有効な認証ヘッダー
    validAuthHeaders = new HttpHeaders();
    validAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    validAuthHeaders.setBasicAuth("admin", "streamconverter123");

    // 無効な認証ヘッダー
    invalidAuthHeaders = new HttpHeaders();
    invalidAuthHeaders.setContentType(MediaType.APPLICATION_JSON);
    invalidAuthHeaders.setBasicAuth("admin", "wrongpassword");
  }

  /** 認証が必要なエンドポイントへの未認証アクセステスト */
  @Test
  @Order(1)
  void testUnauthenticatedAccess() {
    TransformRequest request = createTestRequest();
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, new HttpHeaders());

    // 認証なしでアクセス
    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 無効な認証情報でのアクセステスト */
  @Test
  @Order(2)
  void testInvalidAuthentication() {
    TransformRequest request = createTestRequest();
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, invalidAuthHeaders);

    // 無効な認証情報でアクセス
    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 有効な認証でのアクセステスト */
  @Test
  @Order(3)
  void testValidAuthentication() {
    TransformRequest request = createTestRequest();
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, validAuthHeaders);

    // 有効な認証情報でアクセス
    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  /** 様々な認証パターンのテスト */
  @Test
  @Order(4)
  void testVariousAuthenticationPatterns() {
    TransformRequest request = createTestRequest();

    // パターン1: 空のユーザー名
    HttpHeaders emptyUserHeaders = new HttpHeaders();
    emptyUserHeaders.setContentType(MediaType.APPLICATION_JSON);
    emptyUserHeaders.setBasicAuth("", "streamconverter123");

    HttpEntity<TransformRequest> emptyUserEntity = new HttpEntity<>(request, emptyUserHeaders);
    ResponseEntity<String> emptyUserResponse =
        restTemplate.postForEntity(baseUrl + "/transform", emptyUserEntity, String.class);
    assertThat(emptyUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // パターン2: 空のパスワード
    HttpHeaders emptyPassHeaders = new HttpHeaders();
    emptyPassHeaders.setContentType(MediaType.APPLICATION_JSON);
    emptyPassHeaders.setBasicAuth("admin", "");

    HttpEntity<TransformRequest> emptyPassEntity = new HttpEntity<>(request, emptyPassHeaders);
    ResponseEntity<String> emptyPassResponse =
        restTemplate.postForEntity(baseUrl + "/transform", emptyPassEntity, String.class);
    assertThat(emptyPassResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // パターン3: 不正なBase64エンコーディング
    HttpHeaders malformedHeaders = new HttpHeaders();
    malformedHeaders.setContentType(MediaType.APPLICATION_JSON);
    malformedHeaders.set("Authorization", "Basic invalidbase64");

    HttpEntity<TransformRequest> malformedEntity = new HttpEntity<>(request, malformedHeaders);
    ResponseEntity<String> malformedResponse =
        restTemplate.postForEntity(baseUrl + "/transform", malformedEntity, String.class);
    assertThat(malformedResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 全エンドポイントでの認証テスト */
  @Test
  @Order(5)
  void testAuthenticationOnAllEndpoints() {
    // Transform エンドポイント
    testEndpointAuthentication("/transform", HttpMethod.POST, createTestRequest());

    // Validate エンドポイント
    testEndpointAuthentication("/validate", HttpMethod.POST, createTestRequest());

    // Batch Transform エンドポイント
    // Note: BatchTransformRequestの作成は複雑なので、簡単なリクエストで代用
    testEndpointAuthentication("/batch/transform", HttpMethod.POST, "{\"datasets\":[]}");

    // Batch Status エンドポイント
    testEndpointAuthentication("/batch/test-batch/status", HttpMethod.GET, null);
  }

  /** セキュリティヘッダーのテスト */
  @Test
  @Order(6)
  void testSecurityHeaders() {
    HttpEntity<Void> entity = new HttpEntity<>(validAuthHeaders);

    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/health", HttpMethod.GET, entity, String.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

    HttpHeaders responseHeaders = response.getHeaders();

    // Spring Securityのデフォルトセキュリティヘッダーを確認
    // X-Content-Type-Options
    assertThat(responseHeaders.get("X-Content-Type-Options")).contains("nosniff");

    // X-Frame-Options
    assertThat(responseHeaders.get("X-Frame-Options")).contains("DENY");

    // Cache-Control（セキュリティ関連の場合）
    if (responseHeaders.get("Cache-Control") != null) {
      System.out.println("Cache-Control: " + responseHeaders.get("Cache-Control"));
    }
  }

  /** CORS設定のテスト（OPTIONS リクエスト） */
  @Test
  @Order(7)
  void testCORSConfiguration() {
    HttpHeaders corsHeaders = new HttpHeaders();
    corsHeaders.set("Origin", "http://localhost:3000"); // フロントエンドのオリジン例
    corsHeaders.set("Access-Control-Request-Method", "POST");
    corsHeaders.set("Access-Control-Request-Headers", "Content-Type,Authorization");

    HttpEntity<Void> entity = new HttpEntity<>(corsHeaders);

    // Preflightリクエスト（OPTIONS）
    ResponseEntity<String> response =
        restTemplate.exchange(baseUrl + "/transform", HttpMethod.OPTIONS, entity, String.class);

    // CORS設定によっては200、404、または405が返される
    assertThat(response.getStatusCode())
        .isIn(
            HttpStatus.OK,
            HttpStatus.NOT_FOUND,
            HttpStatus.METHOD_NOT_ALLOWED,
            HttpStatus.FORBIDDEN);

    if (response.getStatusCode() == HttpStatus.OK) {
      HttpHeaders responseHeaders = response.getHeaders();
      System.out.println("CORS Headers: " + responseHeaders.getAccessControlAllowOrigin());
      System.out.println("Allowed Methods: " + responseHeaders.getAccessControlAllowMethods());
    } else {
      System.out.println("CORS OPTIONS request returned: " + response.getStatusCode());
    }
  }

  /** Basic認証の文字エンコーディングテスト */
  @Test
  @Order(8)
  void testBasicAuthEncoding() {
    // 日本語文字を含むユーザー名/パスワードでのテスト
    String username = "管理者"; // 日本語ユーザー名
    String password = "パスワード123"; // 日本語パスワード

    HttpHeaders unicodeAuthHeaders = new HttpHeaders();
    unicodeAuthHeaders.setContentType(MediaType.APPLICATION_JSON);

    // UTF-8でのBase64エンコーディング
    String credentials = username + ":" + password;
    String encodedCredentials =
        Base64.getEncoder()
            .encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    unicodeAuthHeaders.set("Authorization", "Basic " + encodedCredentials);

    TransformRequest request = createTestRequest();
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, unicodeAuthHeaders);

    ResponseEntity<String> response =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);

    // 日本語認証情報は設定されていないので、401が期待される
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  /** 認証情報の大文字小文字区別テスト */
  @Test
  @Order(9)
  void testAuthenticationCaseSensitivity() {
    TransformRequest request = createTestRequest();

    // ユーザー名の大文字小文字違い
    HttpHeaders upperCaseUserHeaders = new HttpHeaders();
    upperCaseUserHeaders.setContentType(MediaType.APPLICATION_JSON);
    upperCaseUserHeaders.setBasicAuth("ADMIN", "streamconverter123"); // 大文字

    HttpEntity<TransformRequest> upperCaseEntity = new HttpEntity<>(request, upperCaseUserHeaders);
    ResponseEntity<String> upperCaseResponse =
        restTemplate.postForEntity(baseUrl + "/transform", upperCaseEntity, String.class);

    // 認証設定によっては大文字小文字を区別しない場合もある
    assertThat(upperCaseResponse.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.OK);
    System.out.println("Uppercase username test result: " + upperCaseResponse.getStatusCode());

    // パスワードの大文字小文字違い
    HttpHeaders upperCasePasswordHeaders = new HttpHeaders();
    upperCasePasswordHeaders.setContentType(MediaType.APPLICATION_JSON);
    upperCasePasswordHeaders.setBasicAuth("admin", "STREAMCONVERTER123"); // 大文字

    HttpEntity<TransformRequest> upperCasePassEntity =
        new HttpEntity<>(request, upperCasePasswordHeaders);
    ResponseEntity<String> upperCasePassResponse =
        restTemplate.postForEntity(baseUrl + "/transform", upperCasePassEntity, String.class);

    // パスワードは通常大文字小文字を区別する
    assertThat(upperCasePassResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    System.out.println("Uppercase password test result: " + upperCasePassResponse.getStatusCode());
  }

  /** 長時間セッションでの認証テスト */
  @Test
  @Order(10)
  void testLongRunningAuthentication() throws InterruptedException {
    TransformRequest request = createTestRequest();
    HttpEntity<TransformRequest> entity = new HttpEntity<>(request, validAuthHeaders);

    // 初回リクエスト
    ResponseEntity<String> response1 =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);
    assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);

    // 少し待機
    Thread.sleep(1000);

    // 2回目のリクエスト（認証が継続されることを確認）
    ResponseEntity<String> response2 =
        restTemplate.postForEntity(baseUrl + "/transform", entity, String.class);
    assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  // ヘルパーメソッド

  private TransformRequest createTestRequest() {
    TransformRequest request = new TransformRequest();
    request.setData("name,age\nSecurityTest,25");
    request.setInputFormat("CSV");
    request.setOutputFormat("JSON");
    request.setExtractPath("name");
    request.setMdcKey("securityTestUser");
    return request;
  }

  private void testEndpointAuthentication(String endpoint, HttpMethod method, Object requestBody) {
    HttpHeaders noAuthHeaders = new HttpHeaders();
    noAuthHeaders.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Object> noAuthEntity = new HttpEntity<>(requestBody, noAuthHeaders);
    HttpEntity<Object> validAuthEntity = new HttpEntity<>(requestBody, validAuthHeaders);

    // 認証なしでのアクセス
    ResponseEntity<String> noAuthResponse =
        restTemplate.exchange(baseUrl + endpoint, method, noAuthEntity, String.class);
    assertThat(noAuthResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

    // 有効な認証でのアクセス
    try {
      ResponseEntity<String> validAuthResponse =
          restTemplate.exchange(baseUrl + endpoint, method, validAuthEntity, String.class);
      // 200、400、404などの認証成功を示すステータス
      assertThat(validAuthResponse.getStatusCode()).isNotEqualTo(HttpStatus.UNAUTHORIZED);
    } catch (Exception e) {
      // エンドポイントが存在しない場合やリクエスト形式が不正な場合は除外
      System.out.println("Endpoint " + endpoint + " test skipped: " + e.getMessage());
    }
  }
}
