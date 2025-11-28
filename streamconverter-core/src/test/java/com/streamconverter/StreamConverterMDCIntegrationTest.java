package com.streamconverter;

import static com.streamconverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.impl.SampleStreamCommand;
import com.streamconverter.logging.MDCInitializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Integration test for StreamConverter with MDC functionality */
class StreamConverterMDCIntegrationTest {

  @BeforeEach
  void setUp() {
    MDCInitializer.initialize();
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testAutomaticMDCGeneration() throws IOException {
    // 既存のAPIでMDC機能が自動的に有効化されることをテスト
    SampleStreamCommand command = new SampleStreamCommand("test");
    StreamConverter converter = StreamConverter.create(command);

    String testData = createTestData("test,data", "1,value1", "2,value2");
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(1, results.size());
    assertTrue(results.get(0).isSuccess());
    assertEquals("SampleStreamCommand", results.get(0).getCommandName());

    // 出力データが正しく処理されたことを確認
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testCustomExecutionContext() throws IOException {
    // カスタムMDC値を使用したテスト
    MDC.put("requestId", "REQ-TEST-123");
    MDC.put("userId", "testuser");
    MDC.put("testScope", "integration");

    SampleStreamCommand command = new SampleStreamCommand("contextTest");
    StreamConverter converter = StreamConverter.create(command);

    String testData = createTestData("custom,context,test", "a,b,c");
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(1, results.size());
    assertTrue(results.get(0).isSuccess());

    // 出力データが正しく処理されたことを確認
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testMultipleCommandsMDC() throws IOException {
    // 複数コマンドでのMDC機能テスト
    SampleStreamCommand command1 = new SampleStreamCommand("first");
    SampleStreamCommand command2 = new SampleStreamCommand("second");
    StreamConverter converter = StreamConverter.create(command1, command2);

    String testData = createTestData("multi,command,test", "x,y,z");
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 結果検証
    assertEquals(2, results.size());
    assertTrue(results.get(0).isSuccess());
    assertTrue(results.get(1).isSuccess());
    assertEquals("SampleStreamCommand", results.get(0).getCommandName());
    assertEquals("SampleStreamCommand", results.get(1).getCommandName());

    // 出力データが正しく処理されたことを確認
    String result = outputStream.toString(StandardCharsets.UTF_8);
    assertEquals(testData, result);
  }

  @Test
  void testContextPersistenceInFactory() throws IOException {
    // MDC値が複数回実行で保持されることをテスト
    MDC.put("persistenceTest", "factory");

    SampleStreamCommand command = new SampleStreamCommand("persistenceTest");
    StreamConverter converter = StreamConverter.create(command);

    // 複数回実行して同じコンテキストが使用されることを確認
    for (int i = 0; i < 3; i++) {
      String testData = createTestData("persistence,test," + i, "data,value," + i);
      ByteArrayInputStream inputStream =
          new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      List<CommandResult> results = converter.run(inputStream, outputStream);

      assertEquals(1, results.size());
      assertTrue(results.get(0).isSuccess());

      String result = outputStream.toString(StandardCharsets.UTF_8);
      assertEquals(testData, result);
    }
  }
}
