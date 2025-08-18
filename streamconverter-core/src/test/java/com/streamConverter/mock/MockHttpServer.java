package com.streamConverter.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WireMockベースのHTTPモックサーバー
 *
 * <p>ネットワーク依存テストを環境非依存にするためのモックサーバー。 httpbin.orgなどの外部APIをモック化して安定したテスト環境を提供。
 */
public class MockHttpServer {

  private static final Logger logger = LoggerFactory.getLogger(MockHttpServer.class);
  private final WireMockServer wireMockServer;
  private final AtomicBoolean isStarted = new AtomicBoolean(false);

  /** デフォルトポートでMockHttpServerを作成 */
  public MockHttpServer() {
    this(0); // 自動ポート割り当て
  }

  /**
   * 指定ポートでMockHttpServerを作成
   *
   * @param port ポート番号（0の場合は自動割り当て）
   */
  public MockHttpServer(int port) {
    this.wireMockServer = new WireMockServer(options().port(port).httpDisabled(false));
  }

  /** サーバーを起動 */
  public void start() {
    if (isStarted.compareAndSet(false, true)) {
      wireMockServer.start();
      logger.info("MockHttpServer started on port: {}", getPort());
    }
  }

  /** サーバーを停止 */
  public void stop() {
    if (isStarted.compareAndSet(true, false)) {
      wireMockServer.stop();
      logger.info("MockHttpServer stopped");
    }
  }

  /**
   * サーバーのポート番号を取得
   *
   * @return ポート番号
   */
  public int getPort() {
    return wireMockServer.port();
  }

  /**
   * サーバーのベースURLを取得
   *
   * @return ベースURL
   */
  public String getBaseUrl() {
    return "http://localhost:" + getPort();
  }

  /** httpbin.org /post エンドポイントのモックを設定 */
  public void setupHttpbinPostMock() {
    wireMockServer.stubFor(
        post(urlEqualTo("/post"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(createHttpbinPostResponse())));
  }

  /** 並列処理検証用のストリーミングレスポンスモック */
  public void setupParallelProcessingMock() {
    // 即座にレスポンス開始する並列処理シミュレーション（極小遅延で高速チャンク送信）
    wireMockServer.stubFor(
        post(urlEqualTo("/post"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("Transfer-Encoding", "chunked")
                    .withFixedDelay(1) // 1msの極小遅延で即座に開始
                    .withChunkedDribbleDelay(5, 10) // 5-10ms間隔で超高速チャンク送信
                    .withBody(createStreamingResponse())));
  }

  /** 存在しないホストのシミュレーション */
  public void setupNonExistentHostMock() {
    wireMockServer.stubFor(
        any(urlMatching("/.*")).willReturn(aResponse().withStatus(404).withBody("Host not found")));
  }

  /** 大容量データレスポンスのモック */
  public void setupLargeDataResponseMock() {
    wireMockServer.stubFor(
        post(urlEqualTo("/post"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(createLargeDataResponse())));
  }

  /** 遅延レスポンスのモック（タイムアウトテスト用） */
  public void setupDelayedResponseMock(int delayMs) {
    wireMockServer.stubFor(
        post(urlEqualTo("/post"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(createHttpbinPostResponse())
                    .withFixedDelay(delayMs)));
  }

  /** エラーレスポンスのモック */
  public void setupErrorResponseMock(int statusCode) {
    wireMockServer.stubFor(
        post(urlEqualTo("/post"))
            .willReturn(
                aResponse()
                    .withStatus(statusCode)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"error\": \"Mock error response\"}")));
  }

  /** 全てのモック設定をリセット */
  public void resetMocks() {
    wireMockServer.resetAll();
  }

  /**
   * リクエストが送信されたかを検証
   *
   * @param requestPattern リクエストパターン
   */
  public void verifyRequest(RequestPatternBuilder requestPattern) {
    wireMockServer.verify(requestPattern);
  }

  private String createHttpbinPostResponse() {
    return """
        {
          "args": {},
          "data": "test-data-placeholder",
          "files": {},
          "form": {},
          "headers": {
            "Content-Type": "application/json",
            "Host": "httpbin.org"
          },
          "json": null,
          "origin": "127.0.0.1",
          "url": "http://httpbin.org/post"
        }
        """;
  }

  private String createLargeDataResponse() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"args\": {},\n");
    sb.append("  \"data\": \"");

    // 大容量データを模擬（約100KB）
    String largeData = "A".repeat(100 * 1024);
    sb.append(largeData);

    sb.append("\",\n");
    sb.append("  \"files\": {},\n");
    sb.append("  \"form\": {},\n");
    sb.append("  \"headers\": {\n");
    sb.append("    \"Content-Type\": \"application/json\",\n");
    sb.append("    \"Host\": \"httpbin.org\"\n");
    sb.append("  },\n");
    sb.append("  \"json\": null,\n");
    sb.append("  \"origin\": \"127.0.0.1\",\n");
    sb.append("  \"url\": \"http://httpbin.org/post\"\n");
    sb.append("}");

    return sb.toString();
  }

  private String createStreamingResponse() {
    // 並列処理検証用の大きなストリーミングレスポンス
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"status\": \"streaming\",\n");
    sb.append("  \"data\": \"");

    // 約10KBの応答データでストリーミング効果を確認
    String streamingData = "S".repeat(10 * 1024);
    sb.append(streamingData);

    sb.append("\",\n");
    sb.append("  \"parallel\": true,\n");
    sb.append("  \"timestamp\": \"").append(java.time.Instant.now()).append("\"\n");
    sb.append("}");

    return sb.toString();
  }

  /** AutoCloseableサポート */
  public void close() {
    stop();
  }
}
