package com.streamConverter.benchmark;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.charaCode.convert;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 省メモリ効率の迅速テスト
 *
 * <p>complex pipelineの文字コード変換でメモリを大量消費する問題を特定・修正のための簡易テスト
 */
@DisplayName("省メモリ効率迅速テスト")
class MemoryEfficiencyQuickTest {

  private static final Logger logger = LoggerFactory.getLogger(MemoryEfficiencyQuickTest.class);

  @Test
  @DisplayName("文字コード変換の省メモリテスト")
  void testCharacterConversionMemoryEfficiency() throws IOException {
    logger.info("=== Character Conversion Memory Test ===");

    int dataSize = 10 * 1024 * 1024; // 10MB

    // 複雑パイプラインの文字コード変換部分のみテスト
    IStreamCommand[] pipeline = {new convert("UTF-8", "UTF-16"), new convert("UTF-16", "UTF-8")};

    Runtime runtime = Runtime.getRuntime();

    // テスト実行
    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    try (InputStream input = new LargeDataInputStream(dataSize);
        OutputStream output = new NullOutputStream()) {

      StreamConverter converter = new StreamConverter(pipeline);
      List<CommandResult> results = converter.run(input, output);

      Assertions.assertNotNull(results);
      Assertions.assertEquals(pipeline.length, results.size());
    }

    System.gc();
    long afterMemory = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = Math.max(0, afterMemory - beforeMemory);

    logger.info(
        "Data size: {}MB, Memory used: {}MB", dataSize / 1024 / 1024, memoryUsed / 1024 / 1024);

    // 省メモリ要件：10MBデータで50MB以下のメモリ使用
    long maxAcceptableMemory = 50 * 1024 * 1024; // 50MB
    Assertions.assertTrue(
        memoryUsed < maxAcceptableMemory,
        String.format(
            "Character conversion memory usage too high: %dMB > %dMB (data: %dMB)",
            memoryUsed / 1024 / 1024, maxAcceptableMemory / 1024 / 1024, dataSize / 1024 / 1024));
  }

  /** 全ての出力を破棄するOutputStream */
  private static class NullOutputStream extends OutputStream {
    @Override
    public void write(int b) throws IOException {
      // データを破棄
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      // データを破棄
    }
  }

  /** 大容量データを生成するInputStream */
  private static class LargeDataInputStream extends InputStream {
    private final int totalSize;
    private int bytesRead = 0;
    private final byte[] pattern;
    private int patternIndex = 0;

    public LargeDataInputStream(int totalSize) {
      this.totalSize = totalSize;
      this.pattern =
          "StreamConverter,Character,Encoding,Test,1234567890,ABCDEF\n"
              .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= totalSize) {
        return -1;
      }

      byte b = pattern[patternIndex];
      patternIndex = (patternIndex + 1) % pattern.length;
      bytesRead++;
      return b & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (bytesRead >= totalSize) {
        return -1;
      }

      int remaining = totalSize - bytesRead;
      int toRead = Math.min(len, remaining);

      for (int i = 0; i < toRead; i++) {
        b[off + i] = pattern[patternIndex];
        patternIndex = (patternIndex + 1) % pattern.length;
      }

      bytesRead += toRead;
      return toRead;
    }
  }
}
