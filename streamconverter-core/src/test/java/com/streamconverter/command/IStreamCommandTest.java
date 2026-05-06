package com.streamconverter.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

@DisplayName("IStreamCommand#withLogging のテスト")
class IStreamCommandTest {

  private static class ConcreteTestCommand implements IStreamCommand {
    @Override
    public void execute(InputStream in, OutputStream out) throws IOException {
      in.transferTo(out);
    }
  }

  private Logger logger;

  @BeforeEach
  void setUp() {
    logger = mock(Logger.class);
    when(logger.isInfoEnabled()).thenReturn(true);
    when(logger.isErrorEnabled()).thenReturn(true);
  }

  @Test
  @DisplayName("正常実行時: 開始・完了ログが出力される")
  void testWithLogging_successfulExecution() throws IOException {
    IStreamCommand command = (in, out) -> in.transferTo(out);
    IStreamCommand wrapped = command.withLogging(logger);

    try (InputStream in = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        OutputStream out = new ByteArrayOutputStream()) {
      wrapped.execute(in, out);
    }

    verify(logger).info(eq("Starting command: {}"), anyString());
    verify(logger).info(eq("Completed command: {} ({}ms)"), anyString(), anyLong());
    verifyNoMoreInteractions(logger);
  }

  @Test
  @DisplayName("IOException 発生時: エラーログを出力して再スロー")
  void testWithLogging_ioException() {
    IOException cause = new IOException("test IO error");
    IStreamCommand command =
        (in, out) -> {
          throw cause;
        };
    IStreamCommand wrapped = command.withLogging(logger);

    IOException thrown =
        assertThrows(
            IOException.class,
            () ->
                wrapped.execute(
                    new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));

    assertSame(cause, thrown);
    verify(logger).info(eq("Starting command: {}"), anyString());
    verify(logger).error(eq("Failed command: {} - {}"), anyString(), anyString(), eq(cause));
  }

  @Test
  @DisplayName("RuntimeException 発生時: エラーログを出力して再スロー")
  void testWithLogging_runtimeException() {
    RuntimeException cause = new IllegalStateException("test runtime error");
    IStreamCommand command =
        (in, out) -> {
          throw cause;
        };
    IStreamCommand wrapped = command.withLogging(logger);

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () ->
                wrapped.execute(
                    new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));

    assertSame(cause, thrown);
    verify(logger).info(eq("Starting command: {}"), anyString());
    verify(logger).error(eq("Failed command: {} - {}"), anyString(), anyString(), eq(cause));
  }

  @Test
  @DisplayName("Error 発生時: エラーログを出力して再スロー")
  void testWithLogging_error() {
    Error cause = new AssertionError("test fatal error");
    IStreamCommand command =
        (in, out) -> {
          throw cause;
        };
    IStreamCommand wrapped = command.withLogging(logger);

    Error thrown =
        assertThrows(
            Error.class,
            () ->
                wrapped.execute(
                    new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream()));

    assertSame(cause, thrown);
    verify(logger).info(eq("Starting command: {}"), anyString());
    verify(logger)
        .error(eq("Fatal error in command: {} - {}"), anyString(), anyString(), eq(cause));
  }

  @Test
  @DisplayName("通常クラス実装: クラス名がログに使われる")
  void testWithLogging_concreteClass_usesClassName() throws IOException {
    // 名前付き具象クラスでラップ → getSimpleName() が使われる
    IStreamCommand concreteCommand = new ConcreteTestCommand();
    IStreamCommand wrapped = concreteCommand.withLogging(logger);

    try (InputStream in = new ByteArrayInputStream(new byte[0]);
        OutputStream out = new ByteArrayOutputStream()) {
      wrapped.execute(in, out);
    }

    // ログに "IStreamCommand" ではなく実クラス名が含まれることを確認
    verify(logger)
        .info(eq("Starting command: {}"), argThat((String name) -> !"IStreamCommand".equals(name)));
  }

  @Test
  @DisplayName("lambda実装: 合成クラス名の代わりに 'IStreamCommand' がログに使われる")
  void testWithLogging_lambda_usesFallbackName() throws IOException {
    // lambda → isSynthetic() == true → フォールバック名 "IStreamCommand"
    IStreamCommand lambda = (in, out) -> in.transferTo(out);
    IStreamCommand wrapped = lambda.withLogging(logger);

    try (InputStream in = new ByteArrayInputStream(new byte[0]);
        OutputStream out = new ByteArrayOutputStream()) {
      wrapped.execute(in, out);
    }

    verify(logger).info("Starting command: {}", "IStreamCommand");
  }

  @Test
  @DisplayName("withLogging(Logger, String): 指定したコマンド名がログに使われる")
  void testWithLoggingExplicitName() throws IOException {
    IStreamCommand command = (in, out) -> in.transferTo(out);
    IStreamCommand wrapped = command.withLogging(logger, "MyCustomCommand");

    try (InputStream in = new ByteArrayInputStream(new byte[0]);
        OutputStream out = new ByteArrayOutputStream()) {
      wrapped.execute(in, out);
    }

    verify(logger).info("Starting command: {}", "MyCustomCommand");
    verify(logger).info(eq("Completed command: {} ({}ms)"), eq("MyCustomCommand"), anyLong());
  }
}
