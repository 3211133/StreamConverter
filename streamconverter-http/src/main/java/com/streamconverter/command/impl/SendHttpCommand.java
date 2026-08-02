package com.streamconverter.command.impl;

import com.streamconverter.UncheckedStreamException;
import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** 指定された通信先にOutputStreamを送信するコマンドクラス。 */
public class SendHttpCommand implements IStreamCommand {

  private static final Logger logger = LoggerFactory.getLogger(SendHttpCommand.class);

  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int MAX_ERROR_BODY_SIZE = 1024 * 1024;
  private static final int STREAMING_CHUNK_SIZE = 8192;

  private static final InetAddressResolver DEFAULT_RESOLVER = InetAddress::getAllByName;

  private final String url;
  private final WebClient webClient;
  private final SsrfUrlValidator ssrfUrlValidator;

  /**
   * デフォルトコンストラクタ
   *
   * @param url 送信先のURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  public SendHttpCommand(String url) {
    this(url, createDefaultWebClient(), DEFAULT_RESOLVER);
  }

  /**
   * カスタム {@link WebClient} を使用するコンストラクタ。
   *
   * <p>主にテストや特殊なHTTPクライアント設定のために使用する。URLの検証は通常コンストラクタと同様に適用する。
   *
   * @param url 送信先のURL
   * @param webClient 使用するWebClient
   */
  public SendHttpCommand(String url, WebClient webClient) {
    this(url, webClient, DEFAULT_RESOLVER);
  }

  SendHttpCommand(String url, WebClient webClient, InetAddressResolver resolver) {
    this.ssrfUrlValidator = new SsrfUrlValidator(resolver);
    this.url = ssrfUrlValidator.validateAndSanitizeUrl(url);
    this.webClient = Objects.requireNonNull(webClient, "webClient must not be null");
  }

  private static WebClient createDefaultWebClient() {
    // Simple HttpClient configuration for Netty 4.1.123.Final compatibility
    HttpClient httpClient =
        HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
            .keepAlive(false); // Disable keep-alive to avoid connection pool issues

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(
            configurer ->
                // 1 MB limit for error response bodies (used by onStatus bodyToMono).
                // The streaming response path (bodyToFlux) bypasses this buffer entirely.
                configurer.defaultCodecs().maxInMemorySize(MAX_ERROR_BODY_SIZE))
        .build();
  }

  /**
   * ストリームを指定されたURLに送信します。
   *
   * <p>SSRF防御のため、送信直前にURLのホスト名を再度DNS解決し、プライベートIPへの変化を検出した場合は {@link IOException} をスローします（DNS
   * rebinding対策）。カスタム {@link WebClient} を注入する場合は、 接続再利用（keep-alive / connection
   * pool）を無効にしてください。接続再利用が有効な場合、 この再検証をパスしても既存接続がプライベートIPへ転送されるリスクが残ります。
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException DNS rebindingを検出した場合、または入出力エラーが発生した場合
   */
  // AvoidCatchingGenericException: WebClient / Reactor は下位の失敗を任意の RuntimeException として
  // 送出するため、コマンド境界でまとめて捕捉し、URLをサニタイズした IOException へ変換する必要がある。
  // 握り潰さず cause を保持して再スローするため、IStreamCommand.withLogging と同種の正当な境界catch。
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    Objects.requireNonNull(outputStream, "outputStream must not be null");

    ssrfUrlValidator.revalidateHostForSsrf(url);

    String safeUrl = sanitizeUrl(url);
    logger.info("Sending HTTP POST request to: {}", safeUrl);

    try {
      // Track total bytes written for better error reporting
      final long[] totalBytesWritten = {0L};

      // 大容量データに対応するため、ストリーミング処理を使用
      // WebClientでストリーミングレスポンスを処理
      webClient
          .post()
          .uri(url)
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
          .header(HttpHeaders.USER_AGENT, "StreamConverter/1.0")
          .body(
              BodyInserters.fromDataBuffers(
                  org.springframework.core.io.buffer.DataBufferUtils.readInputStream(
                      () -> inputStream,
                      org.springframework.core.io.buffer.DefaultDataBufferFactory.sharedInstance,
                      STREAMING_CHUNK_SIZE)))
          .retrieve()
          .onStatus(
              status -> !status.is2xxSuccessful(),
              response ->
                  response
                      .bodyToMono(String.class)
                      .defaultIfEmpty("")
                      .map(
                          errorBody ->
                              new RuntimeException(
                                  String.format(
                                      "HTTP request failed: status=%d, url=%s, response=%s",
                                      response.statusCode().value(),
                                      sanitizeUrl(url),
                                      truncate(errorBody, 256)))))
          .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class)
          .timeout(Duration.ofMinutes(5)) // 大容量データ処理のため5分に延長
          .doOnNext(dataBuffer -> writeChunk(dataBuffer, outputStream, totalBytesWritten))
          .doOnComplete(() -> flushResponse(outputStream))
          .blockLast(); // Intentionally synchronous: IStreamCommand interface requires
      // blocking execution
      // for compatibility with existing command pipeline. Alternative: use subscribe()
      // with CompletableFuture for true async, but would break command interface contract.

    } catch (RuntimeException e) {
      // WebClient error responses are wrapped in RuntimeException.
      // 例外メッセージには sanitizeUrl 適用後のURLのみを含め、e.getMessage() の連結は避ける。
      // WebClient の下位例外メッセージに生 URL が含まれるケース（クエリ文字列・資格情報）でも
      // 公開される IOException メッセージから漏洩しないようにするため。
      // 原因の詳細は cause として保持し、診断性を維持する。
      String errorMessage = "HTTP request failed: " + safeUrl;
      logger.error(errorMessage, e);
      throw new IOException(errorMessage, e);
    }
  }

  /**
   * レスポンスの1チャンクを出力ストリームへ書き出す。
   *
   * <p>{@code doOnNext} のコールバックはチェック例外を宣言できないため、{@link IOException} は {@link
   * UncheckedStreamException} に載せて搬送し、{@link #execute} の catch 節で {@link IOException} に戻す。
   */
  private void writeChunk(
      org.springframework.core.io.buffer.DataBuffer dataBuffer,
      OutputStream outputStream,
      long[] totalBytesWritten) {
    // Ensure DataBuffer is always released, even if write fails
    try {
      // ストリーミング処理：8KBずつレスポンスを処理
      byte[] bytes = new byte[dataBuffer.readableByteCount()];
      dataBuffer.read(bytes);
      outputStream.write(bytes);
      totalBytesWritten[0] += bytes.length;
    } catch (IOException e) {
      throw new UncheckedStreamException(
          new IOException(
              String.format(
                  "Failed to write response data to output stream (url=%s, bytesWritten=%d)",
                  sanitizeUrl(url), totalBytesWritten[0]),
              e));
    } finally {
      org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
    }
  }

  /**
   * ストリーミング完了時に出力ストリームをフラッシュする。
   *
   * <p>{@link #writeChunk} と同様に、{@link IOException} は {@link UncheckedStreamException} に載せて搬送する。
   */
  private void flushResponse(OutputStream outputStream) {
    try {
      outputStream.flush();
      logger.info("HTTP response streaming completed successfully");
    } catch (IOException e) {
      throw new UncheckedStreamException(new IOException("Failed to flush output stream", e));
    }
  }

  private static String truncate(String s, int maxLength) {
    if (s == null || s.length() <= maxLength) {
      return s;
    }
    return s.substring(0, maxLength) + "...[truncated]";
  }

  /** URLのクエリ文字列とユーザー情報を除去してログ・例外メッセージへの資格情報漏洩を防ぐ。 */
  static String sanitizeUrl(String rawUrl) {
    if (rawUrl == null) {
      return null;
    }
    try {
      URI uri = new URI(rawUrl);
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), null, null)
          .toString();
    } catch (URISyntaxException e) {
      // 解析不能な場合はスキームとホストのみ抽出を試みる
      int idx = rawUrl.indexOf('?');
      return idx >= 0 ? rawUrl.substring(0, idx) : rawUrl;
    }
  }
}
