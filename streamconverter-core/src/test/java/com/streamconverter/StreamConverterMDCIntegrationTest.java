package com.streamconverter;

import static com.streamconverter.test.TestUtils.createTestData;
import static org.junit.jupiter.api.Assertions.*;

import com.streamconverter.command.IStreamCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** Integration test for StreamConverter with MDC propagation */
class StreamConverterMDCIntegrationTest {

  @Test
  void testMDCPropagation() throws IOException {
    // 親スレッドでMDCを設定
    MDC.put("requestId", "REQ-TEST-123");

    try {
      IStreamCommand command = (in, out) -> in.transferTo(out);
      StreamConverter converter = StreamConverter.create(command);

      String testData = createTestData("test,data", "1,value1", "2,value2");
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
    } finally {
      MDC.clear();
    }
  }

  @Test
  void testMultipleCommandsMDC() throws IOException {
    // 複数コマンドでのMDC機能テスト
    MDC.put("userId", "testuser");

    try {
      IStreamCommand command1 = (in, out) -> in.transferTo(out);
      IStreamCommand command2 = (in, out) -> in.transferTo(out);
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

      // 出力データが正しく処理されたことを確認
      String result = outputStream.toString(StandardCharsets.UTF_8);
      assertEquals(testData, result);
    } finally {
      MDC.clear();
    }
  }
}
