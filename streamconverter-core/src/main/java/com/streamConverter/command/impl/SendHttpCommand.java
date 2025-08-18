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
    this(url, false);
  }

  /**
   * テスト用コンストラクタ（ローカルホストアクセス許可オプション付き）
   *
   * @param url 送信先のURL
   * @param allowLocalhost ローカルホストアクセスを許可するかどうか（テスト用）
   * @throws IllegalArgumentException URLが無効な場合
   */
  public SendHttpCommand(String url, boolean allowLocalhost) {
    super();
    this.url = validateAndSanitizeUrl(url, allowLocalhost);

    // HttpClient configuration optimized for HTTP/1.1 parallel processing
    HttpClient httpClient =
        HttpClient.create()
            .protocol(reactor.netty.http.HttpProtocol.HTTP11) // HTTP/1.1 with chunked encoding
            .responseTimeout(Duration.ofSeconds(30))
            .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .option(io.netty.channel.ChannelOption.SO_SNDBUF, 256) // 256バイト送信バッファ
            .option(io.netty.channel.ChannelOption.SO_RCVBUF, 256) // 256バイト受信バッファ
            .option(io.netty.channel.ChannelOption.TCP_NODELAY, true) // Nagleアルゴリズム無効化
            .keepAlive(false) // Disable keep-alive for simpler chunked processing
            .wiretap(true); // Enable wire-level logging

    this.webClient =
        WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .codecs(
                configurer ->
                    configurer.defaultCodecs().maxInMemorySize(512)) // 512バイトの極小バッファで並列処理を強制
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
    return validateAndSanitizeUrl(url, false);
  }

  /**
   * URLの検証とサニタイゼーションを行う（ローカルホスト許可オプション付き）
   *
   * @param url 検証するURL
   * @param allowLocalhost ローカルホストアクセスを許可するかどうか
   * @return 検証済みURL
   * @throws IllegalArgumentException URLが無効な場合
   */
  private String validateAndSanitizeUrl(String url, boolean allowLocalhost) {
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

      // ローカルホストや内部IPアドレスへのアクセスを防ぐ（テスト時は許可）
      if (!allowLocalhost && (isLocalhost(host) || isPrivateIpAddress(host))) {
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
      final java.util.concurrent.CompletableFuture<Void> completionFuture =
          new java.util.concurrent.CompletableFuture<>();
      final java.util.concurrent.atomic.AtomicReference<Throwable> errorRef =
          new java.util.concurrent.atomic.AtomicReference<>();

      // 並列処理ライブラリ要件実装：InputStreamクローズ前にOutputStreamへデータ流出
      // 背景読み込みスレッドで小さなチャンクを即座に送信し、レスポンスストリーミングを並列実行
      java.util.concurrent.ExecutorService backgroundExecutor =
          java.util.concurrent.Executors.newSingleThreadExecutor(
              r -> {
                Thread t = new Thread(r, "background-input-reader");
                t.setDaemon(true);
                return t;
              });

      // 背景でInputStreamからデータを読み取り、即座に送信開始
      java.util.concurrent.BlockingQueue<byte[]> dataQueue =
          new java.util.concurrent.LinkedBlockingQueue<>();
      final java.util.concurrent.atomic.AtomicBoolean inputComplete =
          new java.util.concurrent.atomic.AtomicBoolean(false);

      // 背景スレッドでInputStreamを小さなチャンクで読み取り
      java.util.concurrent.Future<?> readerTask =
          backgroundExecutor.submit(
              () -> {
                try (java.io.BufferedInputStream bufferedInput =
                    new java.io.BufferedInputStream(inputStream, 128)) {
                  byte[] buffer = new byte[128]; // 128バイトの極小チャンクで即座に処理開始
                  int bytesRead;
                  while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                    byte[] chunk = java.util.Arrays.copyOf(buffer, bytesRead);
                    dataQueue.offer(chunk);

                    // 各チャンク後にわずかに待機（並列処理を促進）
                    try {
                      Thread.sleep(0, 500000); // 0.5ms待機でHTTPレスポンス開始を促進
                    } catch (InterruptedException e) {
                      Thread.currentThread().interrupt();
                      break;
                    }
                  }
                } catch (IOException e) {
                  errorRef.set(new RuntimeException("Input reading failed", e));
                } finally {
                  inputComplete.set(true);
                }
              });

      try {
        // リアクティブなFluxストリームを作成（背景読み取りと並列）
        reactor.core.publisher.Flux<org.springframework.core.io.buffer.DataBuffer> inputFlux =
            reactor.core.publisher.Flux.<org.springframework.core.io.buffer.DataBuffer>create(
                sink -> {
                  reactor.core.scheduler.Schedulers.boundedElastic()
                      .schedule(
                          () -> {
                            try {
                              while (!inputComplete.get() || !dataQueue.isEmpty()) {
                                byte[] chunk =
                                    dataQueue.poll(1, java.util.concurrent.TimeUnit.MILLISECONDS);
                                if (chunk != null) {
                                  org.springframework.core.io.buffer.DataBuffer dataBuffer =
                                      org.springframework.core.io.buffer.DefaultDataBufferFactory
                                          .sharedInstance
                                          .wrap(chunk);
                                  sink.next(dataBuffer);
                                }
                              }
                              sink.complete();
                            } catch (InterruptedException e) {
                              Thread.currentThread().interrupt();
                              sink.error(e);
                            } catch (Exception e) {
                              sink.error(e);
                            }
                          });
                },
                reactor.core.publisher.FluxSink.OverflowStrategy.BUFFER);

        // HTTP並列ストリーミング実行（背景入力読み取りと同時）
        webClient
            .post()
            .uri(url)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .header(HttpHeaders.USER_AGENT, "StreamConverter/1.0")
            .header(HttpHeaders.TRANSFER_ENCODING, "chunked")
            .body(BodyInserters.fromDataBuffers(inputFlux))
            .exchange()
            .flatMapMany(
                response -> {
                  if (!response.statusCode().is2xxSuccessful()) {
                    return reactor.core.publisher.Mono.error(
                        new RuntimeException(
                            String.format(
                                "HTTP request failed: status=%d, url=%s",
                                response.statusCode().value(), url)));
                  }
                  return response.bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class);
                })
            .subscribeOn(reactor.core.scheduler.Schedulers.parallel())
            .timeout(Duration.ofMinutes(5))
            .doOnNext(
                dataBuffer -> {
                  try {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    synchronized (outputStream) {
                      outputStream.write(bytes);
                      outputStream.flush(); // 即座にフラッシュして並列処理実現
                    }
                    totalBytesWritten[0] += bytes.length;
                  } catch (IOException e) {
                    errorRef.set(
                        new RuntimeException(
                            String.format(
                                "Failed to write response data (url=%s, bytesWritten=%d)",
                                url, totalBytesWritten[0]),
                            e));
                  } finally {
                    org.springframework.core.io.buffer.DataBufferUtils.release(dataBuffer);
                  }
                })
            .doOnComplete(
                () -> {
                  try {
                    outputStream.flush();
                    logger.info("HTTP response streaming completed successfully");
                    completionFuture.complete(null);
                  } catch (IOException e) {
                    errorRef.set(new RuntimeException("Failed to flush output stream", e));
                    completionFuture.completeExceptionally(e);
                  }
                })
            .doOnError(
                error -> {
                  errorRef.set(error);
                  completionFuture.completeExceptionally(error);
                })
            .subscribe(); // 非同期実行で並列処理実現

        // 完了を待機
        try {
          completionFuture.get(6, java.util.concurrent.TimeUnit.MINUTES);
        } finally {
          // Cleanup
          backgroundExecutor.shutdown();
          try {
            if (!backgroundExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
              backgroundExecutor.shutdownNow();
            }
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            backgroundExecutor.shutdownNow();
          }
        }
      } finally {
        backgroundExecutor.shutdown();
      }

      if (errorRef.get() != null) {
        Throwable error = errorRef.get();
        if (error instanceof RuntimeException) {
          throw (RuntimeException) error;
        } else if (error instanceof IOException) {
          throw (IOException) error;
        } else {
          throw new IOException("Unexpected error during HTTP processing", error);
        }
      }
    } catch (java.util.concurrent.TimeoutException e) {
      throw new IOException("HTTP request timeout: " + url, e);
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      } else if (cause instanceof IOException) {
        throw (IOException) cause;
      } else {
        throw new IOException("HTTP request failed: " + url, cause);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP request interrupted: " + url, e);
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
