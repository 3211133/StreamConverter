package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.streamConverter.command.IContextAwareStreamCommand;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/** ContextAwareStreamConverterのテスト */
class ContextAwareStreamConverterTest {

  private ListAppender<ILoggingEvent> listAppender;
  private Logger logger;

  @BeforeEach
  void setUp() {
    // テスト用のログアペンダーを設定
    logger = (Logger) LoggerFactory.getLogger(ContextAwareStreamConverter.class);
    listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // MDCをクリア
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    // MDCをクリア
    MDC.clear();

    // アペンダーを削除
    if (logger != null && listAppender != null) {
      logger.detachAppender(listAppender);
    }
  }

  @Test
  void testCreateWithVariousCommandTypes() {
    IStreamCommand legacyCommand = new SampleStreamCommand("legacy");
    IContextAwareStreamCommand contextCommand = createTestContextCommand("context");

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(legacyCommand, contextCommand);

    assertNotNull(converter);
    assertNotNull(converter.getExecutionContext());
  }

  @Test
  void testCreateWithCustomContext() {
    ExecutionContext customContext =
        ExecutionContext.builder()
            .globalContext("environment", "test")
            .userContext("priority", "high")
            .build();

    IStreamCommand command = new SampleStreamCommand("test");

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(customContext, command);

    assertEquals(customContext, converter.getExecutionContext());
    assertEquals("test", converter.getExecutionContext().getGlobalContext("environment"));
    assertEquals("high", converter.getExecutionContext().getUserContext("priority"));
  }

  @Test
  void testSingleCommandExecution() throws IOException {
    String testData = "test,data\n1,value1\n2,value2\n";

    AtomicBoolean contextReceived = new AtomicBoolean(false);
    AtomicReference<String> receivedExecutionId = new AtomicReference<>();

    IContextAwareStreamCommand testCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            contextReceived.set(true);
            receivedExecutionId.set(context.getExecutionId());
            inputStream.transferTo(outputStream);
          }
        };

    ContextAwareStreamConverter converter = ContextAwareStreamConverter.create(testCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(1, results.size());
    assertTrue(results.get(0).isSuccess());

    // コンテキストが正しく渡されたことを確認
    assertTrue(contextReceived.get());
    assertEquals(converter.getExecutionContext().getExecutionId(), receivedExecutionId.get());

    // 出力データ検証
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testMultipleCommandExecution() throws IOException {
    String testData = "multi,command,test\na,b,c\nd,e,f\n";

    AtomicReference<String> firstCommandExecutionId = new AtomicReference<>();
    AtomicReference<String> secondCommandExecutionId = new AtomicReference<>();

    IContextAwareStreamCommand firstCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            firstCommandExecutionId.set(context.getExecutionId());
            context.setUserContext("firstCommandCompleted", "true");
            inputStream.transferTo(outputStream);
          }
        };

    IContextAwareStreamCommand secondCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            secondCommandExecutionId.set(context.getExecutionId());
            String firstCompleted = context.getUserContext("firstCommandCompleted");
            assertEquals("true", firstCompleted, "Context should be propagated between commands");
            inputStream.transferTo(outputStream);
          }
        };

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(firstCommand, secondCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(2, results.size());
    assertTrue(results.get(0).isSuccess());
    assertTrue(results.get(1).isSuccess());

    // 同じExecutionIdが両方のコマンドに渡されたことを確認
    assertEquals(firstCommandExecutionId.get(), secondCommandExecutionId.get());
    assertEquals(converter.getExecutionContext().getExecutionId(), firstCommandExecutionId.get());

    // 出力データ検証
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testMDCContextPropagation() throws IOException {
    String testData = "mdc,test\n1,data\n";

    ExecutionContext customContext =
        ExecutionContext.builder()
            .globalContext("requestId", "REQ-MDC-TEST")
            .globalContext("userId", "testuser")
            .build();

    AtomicReference<String> capturedRequestId = new AtomicReference<>();
    AtomicReference<String> capturedUserId = new AtomicReference<>();

    IContextAwareStreamCommand mdcTestCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            // MDCから値を取得してテスト
            capturedRequestId.set(MDC.get("requestId"));
            capturedUserId.set(MDC.get("userId"));
            inputStream.transferTo(outputStream);
          }
        };

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(customContext, mdcTestCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);

    // MDCの値が正しく伝播されたことを確認
    assertEquals("REQ-MDC-TEST", capturedRequestId.get());
    assertEquals("testuser", capturedUserId.get());
  }

  @Test
  void testLegacyCommandIntegration() throws IOException {
    String testData = "legacy,integration,test\n1,2,3\n4,5,6\n";

    // 既存のSampleStreamCommandを使用
    IStreamCommand legacyCommand = new SampleStreamCommand("legacy-test");

    ContextAwareStreamConverter converter = ContextAwareStreamConverter.create(legacyCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(1, results.size());
    assertTrue(results.get(0).isSuccess());

    // 出力データ検証
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testCommandSequenceTracking() throws IOException {
    String testData = "sequence,test\n1,first\n2,second\n";

    AtomicReference<Integer> firstCommandSequence = new AtomicReference<>();
    AtomicReference<Integer> secondCommandSequence = new AtomicReference<>();

    IContextAwareStreamCommand firstCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            firstCommandSequence.set(context.getCurrentCommandSequence());
            inputStream.transferTo(outputStream);
          }
        };

    IContextAwareStreamCommand secondCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            secondCommandSequence.set(context.getCurrentCommandSequence());
            inputStream.transferTo(outputStream);
          }
        };

    ContextAwareStreamConverter converter =
        ContextAwareStreamConverter.create(firstCommand, secondCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);

    // シーケンス番号が正しく追跡されていることを確認
    assertNotNull(firstCommandSequence.get());
    assertNotNull(secondCommandSequence.get());
    assertTrue(firstCommandSequence.get() > 0);
    assertTrue(secondCommandSequence.get() > firstCommandSequence.get());
  }

  @Test
  void testEmptyCommandListThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ContextAwareStreamConverter.create();
        });
  }

  @Test
  void testNullInputValidation() {
    IStreamCommand command = new SampleStreamCommand("test");
    ContextAwareStreamConverter converter = ContextAwareStreamConverter.create(command);

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(null, new ByteArrayOutputStream());
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          converter.run(new ByteArrayInputStream("test".getBytes()), null);
        });
  }

  @Test
  void testErrorHandlingInCommand() throws IOException {
    String testData = "error,test\n";

    IContextAwareStreamCommand errorCommand =
        new IContextAwareStreamCommand() {
          @Override
          public void execute(
              InputStream inputStream, OutputStream outputStream, ExecutionContext context)
              throws IOException {
            throw new IOException("Simulated command error");
          }
        };

    ContextAwareStreamConverter converter = ContextAwareStreamConverter.create(errorCommand);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        IOException.class,
        () -> {
          converter.run(inputStream, outputStream);
        });
  }

  /** テスト用のコンテキスト対応コマンドを作成 */
  private IContextAwareStreamCommand createTestContextCommand(String name) {
    return new IContextAwareStreamCommand() {
      @Override
      public void execute(
          InputStream inputStream, OutputStream outputStream, ExecutionContext context)
          throws IOException {
        // シンプルなデータコピー
        inputStream.transferTo(outputStream);
      }

      @Override
      public String toString() {
        return "TestContextCommand{" + name + "}";
      }
    };
  }
}
