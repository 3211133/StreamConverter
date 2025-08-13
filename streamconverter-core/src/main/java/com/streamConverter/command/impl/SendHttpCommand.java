/**
 * Copyright (c) 2023, Stream Converter Project All rights reserved.
 * 指定された通信先にOutputStreamを送信するコマンドクラス。
 *
 * <p>このクラスは、指定された通信先にOutputStreamを送信するためのコマンドを実装します。 ストリームを使用して、データを送信します。 送信先のURLはコンストラクタで指定されます。
 * 送信先のURLは、HTTP POSTリクエストを使用してデータを送信します。 送信先のURLは、HTTPまたはHTTPSで始まる必要があります。
 * 送信先のURLは、コンストラクタで指定されたURLに基づいて決定されます。
 *
 * <p>このクラスは、ストリーム変換のコマンドを実装するための抽象クラスを拡張しています。 ストリーム変換のコマンドは、ストリームを使用してデータを変換するためのものです。
 *
 * <p>レスポンスを受信してOutputStreamに書き込むことができます。
 */
package com.streamConverter.command.impl;

import com.google.common.net.InetAddresses;
import com.streamConverter.command.AbstractStreamCommand;
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
public class SendHttpCommand extends AbstractStreamCommand {

  private static final Logger logger = LoggerFactory.getLogger(SendHttpCommand.class);

  private String url;
  private final WebClient webClient;

  /**
   * デフォルトコンストラクタ
   *
   * @param url 送信先のURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  public SendHttpCommand(String url) {
    super();
    this.url = validateAndSanitizeUrl(url);

    // Simple HttpClient configuration for Netty 4.1.118.Final compatibility
    HttpClient httpClient =
        HttpClient.create()
            .responseTimeout(Duration.ofSeconds(30))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .keepAlive(false); // Disable keep-alive to avoid connection pool issues

    this.webClient =
        WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(
                configurer ->
                    configurer.defaultCodecs().maxInMemorySize(-1)) // Unlimited for streaming
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

  /** プライベートIPアドレスかどうかを判定する（Guava使用） */
  private boolean isPrivateIpAddress(String host) {
    try {
      // GuavaのInetAddressesを使用してIPアドレスを解析
      InetAddress address = InetAddresses.forString(host);
      // RFC 1918準拠のプライベートアドレス判定
      return address.isSiteLocalAddress() || address.isLoopbackAddress();
    } catch (IllegalArgumentException e) {
      // IPアドレス形式でない場合（ホスト名など）はfalseを返す
      return false;
    }
  }

  /**
   * ストリームを指定されたURLに送信します。
   *
   * @param inputStream 入力ストリーム
   * @param outputStream 出力ストリーム
   * @throws IOException 入出力エラーが発生した場合
   */
  @Override
  protected void _execute(InputStream inputStream, OutputStream outputStream) throws IOException {
    Objects.requireNonNull(inputStream, "inputStream must not be null");
    Objects.requireNonNull(outputStream, "outputStream must not be null");

    logger.info("Sending HTTP POST request to: {}", url);

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
                      8192))) // 8KB chunks for memory efficiency
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
                                      response.statusCode().value(), url, errorBody))))
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
                          url, totalBytesWritten[0]),
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
          .blockLast(); // Intentionally synchronous: AbstractStreamCommand interface requires
      // blocking execution
      // for compatibility with existing command pipeline. Alternative: use subscribe()
      // with CompletableFuture for true async, but would break command interface contract.

    } catch (RuntimeException e) {
      // WebClient error responses are wrapped in RuntimeException
      String errorMessage = "HTTP request failed: " + url + " - " + e.getMessage();
      logger.error(errorMessage, e);
      throw new IOException(errorMessage, e);
    } catch (Exception e) {
      String errorMessage = "HTTP request failed: " + url;
      logger.error(errorMessage, e);
      throw new IOException(errorMessage, e);
    }
  }
}
