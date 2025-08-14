package com.streamConverter.benchmark;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.charaCode.CharacterConvertCommand;
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
  @DisplayName("文字コード変換の環境適応型省メモリテスト")
  void testCharacterConversionMemoryEfficiency() throws IOException {
    logger.info("=== Character Conversion Environment-Adaptive Memory Test ===");

    // 環境適応型テストデータサイズ計算
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long calculatedSize =
        Math.min(
            Math.max(maxMemory / 20, 5L * 1024 * 1024),
            100L * 1024 * 1024); // ヒープの5%（最小5MB、最大100MB）

    // 型安全性: Integer.MAX_VALUE以下であることを保証
    if (calculatedSize > Integer.MAX_VALUE) {
      throw new IllegalStateException(
          "Calculated test data size exceeds integer range: " + calculatedSize + " bytes");
    }

    int dataSize = (int) calculatedSize;

    logger.info(
        "Max heap: {}MB, Test data size: {}MB", maxMemory / 1024 / 1024, dataSize / 1024 / 1024);

    // 複雑パイプラインの文字コード変換部分のみテスト
    IStreamCommand[] pipeline = {
      new CharacterConvertCommand("UTF-8", "UTF-16"), new CharacterConvertCommand("UTF-16", "UTF-8")
    };

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

    logger.info("=== 環境適応型省メモリテスト結果 ===");
    logger.info(
        "Data size: {}MB, Memory used: {}MB", dataSize / 1024 / 1024, memoryUsed / 1024 / 1024);

    // 環境適応型メモリ要件：データサイズの10倍以下のメモリ使用（文字コード変換での文字化け対策バッファを考慮）
    long maxAcceptableMemory = dataSize * 10; // データサイズの10倍制限
    logger.info("Max acceptable (10x data): {}MB", maxAcceptableMemory / 1024 / 1024);

    Assertions.assertTrue(
        memoryUsed < maxAcceptableMemory,
        String.format(
            "設計原理違反: 文字コード変換でメモリを使いすぎています。Memory usage: %dMB > %dMB (10x %dMB data)",
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
