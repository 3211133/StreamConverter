package com.streamconverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

class SendHttpCommandWebClientTest {

  private static final Duration TIMEOUT = Duration.ofSeconds(2);
  private static final Duration INPUT_RELEASE_TIMEOUT = Duration.ofSeconds(5);

  @Test
  void executeWritesResponseFromInjectedWebClient() throws Exception {
    SendHttpCommand command =
        new SendHttpCommand("https://example.com/post", createEchoWebClient("ack:"));

    ByteArrayOutputStream output = new ByteArrayOutputStream();
    command.execute(
        new ByteArrayInputStream("hello-stream".getBytes(StandardCharsets.UTF_8)), output);

    assertEquals("ack:hello-stream", output.toString(StandardCharsets.UTF_8));
  }

  @Test
  void executeStartsResponseOnlyAfterRequestBodyCompletes() throws Exception {
    SendHttpCommand command =
        new SendHttpCommand("https://example.com/post", createEchoWebClient("ack:"));
    BlockingInputStream input =
        new BlockingInputStream("hello-streaming-body".getBytes(StandardCharsets.UTF_8), 5);
    SignalingOutputStream output = new SignalingOutputStream();

    try (var executor = Executors.newSingleThreadExecutor()) {
      Future<?> future = executor.submit(() -> runCommand(command, input, output));

      assertFalse(
          output.awaitFirstWrite(TIMEOUT),
          "レスポンスはリクエストボディ完了前には書き出されないはず");

      input.releaseRemainingInput();
      future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      assertTrue(output.awaitFirstWrite(TIMEOUT), "入力解放後はレスポンスが書き出されるはず");
      assertEquals("ack:hello-streaming-body", output.toString(StandardCharsets.UTF_8));
    }
  }

  @Test
  void executeCanWriteResponseBeforeRequestBodyCompletesWhenServerRespondsEarly() throws Exception {
    SendHttpCommand command =
        new SendHttpCommand(
            "https://example.com/post", createRespondAfterFirstChunkWebClient("accepted"));
    BlockingInputStream input =
        new BlockingInputStream("hello-streaming-body".getBytes(StandardCharsets.UTF_8), 5);
    SignalingOutputStream output = new SignalingOutputStream();

    try (var executor = Executors.newSingleThreadExecutor()) {
      Future<?> future = executor.submit(() -> runCommand(command, input, output));

      assertTrue(output.awaitFirstWrite(TIMEOUT), "早期レスポンスなら入力完了前に書き出せるはず");

      input.releaseRemainingInput();
      future.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

      assertEquals("accepted", output.toString(StandardCharsets.UTF_8));
    }
  }

  private static Void runCommand(
      SendHttpCommand command, InputStream input, ByteArrayOutputStream output) {
    try {
      command.execute(input, output);
      return null;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static WebClient createEchoWebClient(String responsePrefix) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ExchangeFunction exchangeFunction =
        request -> {
          AtomicReference<String> requestBody = new AtomicReference<>("");
          MockClientHttpRequest mockRequest = new MockClientHttpRequest(HttpMethod.POST, URI.create(request.url().toString()));
          mockRequest.setWriteHandler(
              body ->
                  DataBufferUtils.join(body)
                      .doOnNext(
                          dataBuffer -> {
                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(bytes);
                            requestBody.set(new String(bytes, StandardCharsets.UTF_8));
                            DataBufferUtils.release(dataBuffer);
                          })
                      .then());
          return request
              .writeTo(mockRequest, ExchangeStrategies.withDefaults())
              .then(Mono.fromSupplier(requestBody::get))
              .map(
                  body ->
                      ClientResponse.create(HttpStatus.OK)
                          .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                          .body(
                              Flux.just(
                                  bufferFactory.wrap(
                                      (responsePrefix + body).getBytes(StandardCharsets.UTF_8))))
                          .build());
        };
    return WebClient.builder().exchangeFunction(exchangeFunction).build();
  }

  static WebClient createRespondAfterFirstChunkWebClient(String responseBody) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ExchangeFunction exchangeFunction =
        request -> {
          MockClientHttpRequest mockRequest =
              new MockClientHttpRequest(HttpMethod.POST, URI.create(request.url().toString()));
          Sinks.One<Void> firstChunkReceived = Sinks.one();
          AtomicBoolean emitted = new AtomicBoolean(false);
          mockRequest.setWriteHandler(
              body ->
                  body.next()
                      .doOnNext(
                          dataBuffer -> {
                            try {
                              if (emitted.compareAndSet(false, true)) {
                                firstChunkReceived.tryEmitEmpty();
                              }
                            } finally {
                              DataBufferUtils.release(dataBuffer);
                            }
                          })
                      .then());
          request.writeTo(mockRequest, ExchangeStrategies.withDefaults()).subscribe();
          return firstChunkReceived
              .asMono()
              .thenReturn(
                  ClientResponse.create(HttpStatus.OK)
                      .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                      .body(
                          Flux.just(
                              bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8))))
                      .build());
        };
    return WebClient.builder().exchangeFunction(exchangeFunction).build();
  }

  private static final class BlockingInputStream extends InputStream {
    private final byte[] data;
    private final int firstChunkSize;
    private final CountDownLatch releaseRemainingInput = new CountDownLatch(1);
    private int index;

    private BlockingInputStream(byte[] data, int firstChunkSize) {
      this.data = data;
      this.firstChunkSize = firstChunkSize;
    }

    @Override
    public int read() throws IOException {
      byte[] singleByte = new byte[1];
      int read = read(singleByte, 0, 1);
      return read == -1 ? -1 : singleByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int off, int len) throws IOException {
      if (index >= data.length) {
        return -1;
      }

      if (index >= firstChunkSize) {
        try {
          if (!releaseRemainingInput.await(INPUT_RELEASE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new IOException("Timed out waiting to release remaining input");
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new IOException("Interrupted while waiting to release remaining input", e);
        }
      }

      int upperBound = index < firstChunkSize ? firstChunkSize : data.length;
      int bytesToCopy = Math.min(len, upperBound - index);
      System.arraycopy(data, index, buffer, off, bytesToCopy);
      index += bytesToCopy;
      return bytesToCopy;
    }

    private void releaseRemainingInput() {
      releaseRemainingInput.countDown();
    }
  }

  private static final class SignalingOutputStream extends ByteArrayOutputStream {
    private final CountDownLatch firstWriteLatch = new CountDownLatch(1);

    @Override
    public synchronized void write(int b) {
      firstWriteLatch.countDown();
      super.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
      if (len > 0) {
        firstWriteLatch.countDown();
      }
      super.write(b, off, len);
    }

    private boolean awaitFirstWrite(Duration timeout) throws InterruptedException {
      return firstWriteLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }
  }

  // ---- #657: エラーレスポンスボディを 256 文字に切り詰める ----

  @Test
  @DisplayName("エラーレスポンスボディが 256 文字を超える場合は切り詰められる（#657）")
  void testErrorBodyIsTruncatedToMaxLength() throws Exception {
    // 修正前: errorBody をそのままメッセージに埋め込む → 大きなボディが例外メッセージに入る
    // 修正後: 256 文字超は "...[truncated]" サフィックス付きで切り詰める
    String longBody = "E".repeat(300);
    WebClient errorClient = createErrorWebClient(500, longBody);
    SendHttpCommand command = new SendHttpCommand("https://example.com/post", errorClient);

    IOException ex =
        assertThrows(
            IOException.class,
            () ->
                command.execute(
                    new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayOutputStream()));

    String causeMessage = ex.getCause().getMessage();
    assertTrue(
        causeMessage.contains("...[truncated]"),
        "エラーメッセージに切り詰め表示が含まれるべき: " + causeMessage);
    assertFalse(
        causeMessage.contains(longBody),
        "エラーメッセージに完全な 300 文字ボディが含まれてはいけない");
  }

  @Test
  @DisplayName("エラーレスポンスボディが 256 文字以下の場合は切り詰めない（#657）")
  void testShortErrorBodyIsNotTruncated() throws Exception {
    String shortBody = "short error";
    WebClient errorClient = createErrorWebClient(400, shortBody);
    SendHttpCommand command = new SendHttpCommand("https://example.com/post", errorClient);

    IOException ex =
        assertThrows(
            IOException.class,
            () ->
                command.execute(
                    new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)),
                    new ByteArrayOutputStream()));

    String causeMessage = ex.getCause().getMessage();
    assertTrue(causeMessage.contains(shortBody), "短いエラーボディはそのまま含まれるべき");
    assertFalse(causeMessage.contains("...[truncated]"), "短いエラーボディは切り詰められないべき");
  }

  static WebClient createErrorWebClient(int statusCode, String responseBody) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    org.springframework.http.HttpStatus status =
        org.springframework.http.HttpStatus.valueOf(statusCode);
    ExchangeFunction exchangeFunction =
        request ->
            reactor.core.publisher.Mono.just(
                ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE,
                        org.springframework.http.MediaType.TEXT_PLAIN_VALUE)
                    .body(
                        Flux.just(
                            bufferFactory.wrap(
                                responseBody.getBytes(StandardCharsets.UTF_8))))
                    .build());
    return WebClient.builder().exchangeFunction(exchangeFunction).build();
  }
}
