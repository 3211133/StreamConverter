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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        new SendHttpCommand("https://example.com/post", createImmediateResponseWebClient("accepted"));
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

  static WebClient createImmediateResponseWebClient(String responseBody) {
    DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
    ExchangeFunction exchangeFunction =
        request -> {
          return Mono.just(
              ClientResponse.create(HttpStatus.OK)
                  .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN_VALUE)
                  .body(Flux.just(bufferFactory.wrap(responseBody.getBytes(StandardCharsets.UTF_8))))
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
}
