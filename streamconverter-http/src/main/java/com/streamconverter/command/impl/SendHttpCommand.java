package com.streamconverter.command.impl;

import com.google.common.net.InetAddresses;
import com.streamconverter.command.IStreamCommand;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
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
  private final InetAddressResolver inetAddressResolver;

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
    this.inetAddressResolver = Objects.requireNonNull(resolver);
    this.url = validateAndSanitizeUrl(url);
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
   * URLの検証とサニタイゼーションを行う
   *
   * @param url 検証するURL
   * @return 検証済みURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  private String validateAndSanitizeUrl(String url) {
    Objects.requireNonNull(url, "URL cannot be null");

    String trimmedUrl = url.trim();
    if (trimmedUrl.isEmpty()) {
      throw new IllegalArgumentException("URL cannot be empty");
    }

    try {
      URI uri = new URI(trimmedUrl);
      String scheme = uri.getScheme();

      if (scheme == null) {
        throw new IllegalArgumentException("URL must have a scheme (http or https)");
      }

      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed");
      }

      String host = uri.getHost();
      if (host == null || host.trim().isEmpty()) {
        throw new IllegalArgumentException("URL must have a valid host");
      }

      // ローカルホストや内部IPアドレスへのアクセスを防ぐ
      if (isLocalhost(host) || isPrivateIpAddress(host)) {
        throw new IllegalArgumentException(
            "Access to localhost or private IP addresses is not allowed");
      }

      return trimmedUrl;
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URL format: " + e.getMessage(), e);
    }
  }

  /**
   * execute()直前にホスト名を再DNS解決してSSRF（DNS rebinding）を検出する。
   *
   * <p>リテラルIPはDNS rebindingの対象外なのでスキップする。ホスト名の場合のみ再解決を行い、 localhost判定またはプライベートIP判定に変化していれば {@link
   * IOException} をスローする。
   */
  private void revalidateHostForSsrf() throws IOException {
    try {
      URI uri = new URI(url);
      String host = uri.getHost();
      if (host == null || InetAddresses.isInetAddress(host)) {
        return;
      }
      if (isLocalhost(host)) {
        throw new IOException("DNS rebinding detected: host resolved to localhost: " + host);
      }
      InetAddress[] addresses = inetAddressResolver.getAllByName(host);
      for (InetAddress address : addresses) {
        if (isNonRoutable(address)) {
          throw new IOException(
              "DNS rebinding detected: host resolved to non-routable address: " + address);
        }
      }
    } catch (URISyntaxException | UnknownHostException e) {
      throw new IOException("SSRF revalidation failed: " + e.getMessage(), e);
    }
  }

  /** ローカルホストかどうかを判定する */
  private boolean isLocalhost(String host) {
    if (host == null) {
      return false;
    }

    // Handle IPv6 addresses with brackets
    String cleanHost =
        host.startsWith("[") && host.endsWith("]") ? host.substring(1, host.length() - 1) : host;

    return "localhost".equalsIgnoreCase(cleanHost)
        || "127.0.0.1".equals(cleanHost)
        || "::1".equals(cleanHost);
  }

  /** ホストがプライベートIPに解決されるかを判定する。 リテラルIPはGuavaで即解析し、ホスト名はDNS解決後に検査する。 解決不能なホスト名は例外をスローしてアクセスを拒否する。 */
  private boolean isPrivateIpAddress(String host) {
    // まずリテラルIPとして解析を試みる
    if (InetAddresses.isInetAddress(host)) {
      InetAddress address = InetAddresses.forString(host);
      return isNonRoutable(address);
    }
    // ホスト名: DNS解決して全アドレスを検査する
    try {
      InetAddress[] addresses = inetAddressResolver.getAllByName(host);
      for (InetAddress address : addresses) {
        if (isNonRoutable(address)) {
          return true;
        }
      }
      return false;
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException("Cannot resolve hostname: " + host, e);
    }
  }

  private static boolean isNonRoutable(InetAddress address) {
    return address.isSiteLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isAnyLocalAddress()
        || address.isMulticastAddress();
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
  @Override
  public void execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    Objects.requireNonNull(outputStream, "outputStream must not be null");

    revalidateHostForSsrf();

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
          .doOnNext(
              dataBuffer -> {
                // Ensure DataBuffer is always released, even if write fails
                try {
                  // ストリーミング処理：8KBずつレスポンスを処理
                  byte[] bytes = new byte[dataBuffer.readableByteCount()];
                  dataBuffer.read(bytes);
                  outputStream.write(bytes);
                  totalBytesWritten[0] += bytes.length;
                } catch (IOException e) {
                  throw new RuntimeException(
                      String.format(
                          "Failed to write response data to output stream (url=%s, bytesWritten=%d)",
                          sanitizeUrl(url), totalBytesWritten[0]),
                      e);
                } finally {
                  org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                }
              })
          .doOnComplete(
              () -> {
                try {
                  outputStream.flush();
                  logger.info("HTTP response streaming completed successfully");
                } catch (IOException e) {
                  throw new RuntimeException("Failed to flush output stream", e);
                }
              })
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
