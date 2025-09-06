package com.streamconverter.benchmark;

import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SampleStreamCommand;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

/**
 * 軽量な動作確認テスト
 *
 * <p>ベンチマークテストの軽量版として、基本的な動作を確認します。 大容量データは使用せず、小さなデータで機能が正常に動作することのみを検証します。
 */
@DisplayName("軽量動作確認テスト")
class QuickSmokeTest {

  @Test
  @DisplayName("基本的なパイプライン動作確認")
  void testBasicPipelineOperation() throws IOException {
    // 小さなテストデータ（1KB）
    String testData = "test,data,1\ntest,data,2\ntest,data,3\n".repeat(50);

    try (InputStream input = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      // シンプルなパイプライン
      StreamConverter converter =
          new StreamConverter(new IStreamCommand[] {new SampleStreamCommand("smoke-test")});

      // 実行
      var results = converter.run(input, output);

      // 基本的な動作確認
      assertNotNull(results);
      assertEquals(1, results.size());
      assertTrue(output.size() > 0);
    }
  }

  @Test
  @DisplayName("複数段階パイプライン動作確認")
  void testMultiStagePipeline() throws IOException {
    // 小さなテストデータ（2KB）
    String testData = "sample,pipeline,test\n".repeat(100);

    try (InputStream input = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      // 3段階パイプライン
      StreamConverter converter =
          new StreamConverter(
              new IStreamCommand[] {
                new SampleStreamCommand("stage1"),
                new SampleStreamCommand("stage2"),
                new SampleStreamCommand("stage3")
              });

      // 実行
      var results = converter.run(input, output);

      // 動作確認
      assertNotNull(results);
      assertEquals(3, results.size());
      assertTrue(output.size() > 0);
    }
  }

  @Test
  @DisplayName("エラーハンドリング動作確認")
  void testErrorHandling() throws IOException {
    try (InputStream input = new ByteArrayInputStream("test".getBytes());
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      StreamConverter converter =
          new StreamConverter(new IStreamCommand[] {new SampleStreamCommand("error-test")});

      // 正常実行できることを確認
      assertDoesNotThrow(() -> converter.run(input, output));
    }
  }
}
