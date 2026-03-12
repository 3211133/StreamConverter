package com.streamconverter.benchmark;

import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.charcode.CharacterConvertCommand;
import java.io.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 省メモリ効率の迅速テスト
 *
 * <p>complex pipelineの文字コード変換でメモリを大量消費する問題を特定・修正のための簡易テスト
 */
@DisplayName("省メモリ効率迅速テスト")
@EnabledOnOs(OS.LINUX)
class MemoryEfficiencyQuickTest {

  private static final Logger logger = LoggerFactory.getLogger(MemoryEfficiencyQuickTest.class);

  @Test
  @DisplayName("文字コード変換の環境適応型省メモリテスト")
  void testCharacterConversionMemoryEfficiency() throws IOException {
    logger.info("=== Character Conversion Environment-Adaptive Memory Test ===");

    // 環境適応型テストデータサイズ計算（long型完全対応）
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long dataSize =
        Math.min(
            Math.max(maxMemory / 20, 5L * 1024 * 1024),
            100L * 1024 * 1024); // ヒープの5%（最小5MB、最大100MB）

    logger.info(
        "Max heap: {}MB, Test data size: {}MB", maxMemory / 1024 / 1024, dataSize / 1024 / 1024);

    // 複雑パイプラインの文字コード変換部分のみテスト
    IStreamCommand[] pipeline = {
      CharacterConvertCommand.create("UTF-8", "UTF-16"),
      CharacterConvertCommand.create("UTF-16", "UTF-8")
    };

    // テスト実行
    System.gc();
    long beforeMemory = runtime.totalMemory() - runtime.freeMemory();

    try (InputStream input = LargeDataGenerator.createLargeDataStream("CSV", dataSize);
        OutputStream output = new NullOutputStream()) {

      StreamConverter converter = StreamConverter.create(pipeline);
      converter.run(input, output);
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

  // LargeDataInputStreamを削除 - LargeDataGenerator.createLargeDataStream()に統合完了
  // 技術的負債解消: int制限を解除し、既存のlong対応実装を利用
}
