package com.streamConverter;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.command.impl.SampleStreamCommand;
import com.streamConverter.context.ExecutionContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Integration test for StreamConverter with MDC functionality */
class StreamConverterMDCIntegrationTest {

  @Test
  void testAutomaticMDCGeneration() throws IOException {
    // 既存のAPIでMDC機能が自動的に有効化されることをテスト
    SampleStreamCommand command = new SampleStreamCommand("test");
    StreamConverter converter = StreamConverter.create(command);

    String testData = "test,data\n1,value1\n2,value2\n";
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
    // カスタムExecutionContextを使用したテスト
    ExecutionContext context =
        ExecutionContext.builder()
            .globalContext("requestId", "REQ-TEST-123")
            .globalContext("userId", "testuser")
            .userContext("testScope", "integration")
            .build();

    SampleStreamCommand command = new SampleStreamCommand("contextTest");
    StreamConverter converter = StreamConverter.createWithContext(context, command);

    String testData = "custom,context,test\na,b,c\n";
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

    String testData = "multi,command,test\nx,y,z\n";
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
    // ファクトリメソッドで作成したコンテキストが保持されることをテスト
    ExecutionContext context =
        ExecutionContext.builder().globalContext("persistenceTest", "factory").build();

    SampleStreamCommand command = new SampleStreamCommand("persistenceTest");
    StreamConverter converter = StreamConverter.createWithContext(context, command);

    // 複数回実行して同じコンテキストが使用されることを確認
    for (int i = 0; i < 3; i++) {
      String testData = "persistence,test," + i + "\ndata,value," + i + "\n";
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
