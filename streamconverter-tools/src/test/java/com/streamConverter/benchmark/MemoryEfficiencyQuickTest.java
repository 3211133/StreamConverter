package com.streamConverter.benchmark;

import com.streamConverter.*;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.charaCode.CharacterConvertCommand;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 省メモリ効率の迅速テスト
 *
 * <p>complex pipelineの文字コード変換でメモリを大量消費する問題を特定・修正のための簡易テスト
 */
@DisplayName("省メモリ効率迅速テスト - 環境非依存")
class MemoryEfficiencyQuickTest {

  private static final Logger logger = LoggerFactory.getLogger(MemoryEfficiencyQuickTest.class);

  @Test
  @DisplayName("文字コード変換の環境適応型省メモリテスト")
  void testCharacterConversionMemoryEfficiency() throws IOException {
    logger.info("=== Character Conversion Environment-Adaptive Memory Test ===");

    // 環境適応型テストデータサイズ計算（long型完全対応）
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();

    // プラットフォーム別の最適化設定
    String osName = System.getProperty("os.name").toLowerCase();
    double memoryRatio;
    long minSize;
    long maxSize;

    if (osName.contains("windows")) {
      // Windows: より保守的な設定
      memoryRatio = 0.03; // 3%
      minSize = 3L * 1024 * 1024; // 3MB
      maxSize = 50L * 1024 * 1024; // 50MB
    } else if (osName.contains("mac")) {
      // macOS: 中程度の設定
      memoryRatio = 0.04; // 4%
      minSize = 4L * 1024 * 1024; // 4MB
      maxSize = 75L * 1024 * 1024; // 75MB
    } else {
      // Linux/その他: より積極的な設定
      memoryRatio = 0.05; // 5%
      minSize = 5L * 1024 * 1024; // 5MB
      maxSize = 100L * 1024 * 1024; // 100MB
    }

    long dataSize = Math.min(Math.max((long) (maxMemory * memoryRatio), minSize), maxSize);

    logger.info(
        "Platform: {}, Max heap: {}MB, Test data size: {}MB (ratio: {}%)",
        osName, maxMemory / 1024 / 1024, dataSize / 1024 / 1024, (memoryRatio * 100));

    // 複雑パイプラインの文字コード変換部分のみテスト
    IStreamCommand[] pipeline = {
      new CharacterConvertCommand("UTF-8", "UTF-16"), new CharacterConvertCommand("UTF-16", "UTF-8")
    };

    // テスト実行
    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    try (InputStream input = LargeDataGenerator.createLargeDataStream("CSV", dataSize);
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

    // 環境適応型メモリ要件：プラットフォーム別の最適化（文字コード変換での文字化け対策バッファを考慮）
    double memoryMultiplier;

    if (osName.contains("windows")) {
      memoryMultiplier = 12.0; // Windows: 12倍制限（GC負荷を考慮）
    } else if (osName.contains("mac")) {
      memoryMultiplier = 11.0; // macOS: 11倍制限
    } else {
      memoryMultiplier = 10.0; // Linux/その他: 10倍制限
    }

    long maxAcceptableMemory = (long) (dataSize * memoryMultiplier);
    logger.info(
        "Max acceptable ({}x data): {}MB", memoryMultiplier, maxAcceptableMemory / 1024 / 1024);

    Assertions.assertTrue(
        memoryUsed < maxAcceptableMemory,
        String.format(
            "設計原理違反: 文字コード変換でメモリを使いすぎています。Memory usage: %dMB > %dMB (%.1fx %dMB data)",
            memoryUsed / 1024 / 1024,
            maxAcceptableMemory / 1024 / 1024,
            memoryMultiplier,
            dataSize / 1024 / 1024));
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

  // LargeDataInputStreamを削除 - LargeDataGenerator.createLargeDataStream()に統合完了
  // 技術的負債解消: int制限を解除し、既存のlong対応実装を利用
}
