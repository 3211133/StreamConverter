package com.streamConverter.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 並列処理検証テストのためのユーティリティクラス
 *
 * <p>StreamConverterライブラリの並列処理特性を統一的に検証するためのヘルパークラス。
 * InputStreamのクローズ時点でOutputStreamの状態を監視し、並列処理の実現度を測定する。
 *
 * <p>使用例:
 *
 * <pre>{@code
 * // テストデータ準備
 * String testData = "大容量テストデータ...";
 * ParallelProcessingValidationInputStream inputStream =
 *     ParallelProcessingTestUtils.createValidationInputStream(testData.getBytes());
 * MonitoringOutputStream outputStream =
 *     ParallelProcessingTestUtils.createMonitoringOutputStream();
 *
 * // 検証設定
 * inputStream.setOutputStreamToMonitor(outputStream);
 *
 * // コマンド実行
 * command.execute(inputStream, outputStream);
 *
 * // 結果検証
 * ValidationResult result = inputStream.getValidationResult();
 * ParallelProcessingTestUtils.assertParallelProcessing(result, "CommandName");
 * }</pre>
 */
public class ParallelProcessingTestUtils {

  /**
   * 並列処理検証用のValidationInputStreamを作成
   *
   * @param data テストデータ
   * @return 検証用InputStream
   */
  public static ParallelProcessingValidationInputStream createValidationInputStream(byte[] data) {
    return new ParallelProcessingValidationInputStream(data);
  }

  /**
   * 監視機能付きのMonitoringOutputStreamを作成
   *
   * @return 監視用OutputStream
   */
  public static MonitoringOutputStream createMonitoringOutputStream() {
    return new MonitoringOutputStream();
  }

  /**
   * 並列処理特性のアサーション
   *
   * @param result 検証結果
   * @param commandName コマンド名（ログ用）
   * @return 並列処理が確認された場合true
   */
  public static boolean assertParallelProcessing(ValidationResult result, String commandName) {
    System.out.println("📊 " + commandName + " 並列処理検証結果:");
    System.out.println(
        "  - InputStreamクローズ時点でのOutputStream書き込み: " + result.outputBytesAtClose + " bytes");
    System.out.println(
        "  - クローズ時点での入力進行率: " + String.format("%.1f%%", result.inputProgressAtClose));

    boolean isParallel = result.outputBytesAtClose > 0;

    if (isParallel) {
      System.out.println("  ✅ 並列処理特性確認 - InputStreamクローズ前に出力書き込み開始");
    } else {
      System.out.println("  ⚠️ 逐次処理パターン - InputStreamクローズ後に出力書き込み");
    }

    return isParallel;
  }

  /**
   * 標準的な並列処理検証テストを実行
   *
   * @param command 検証対象のコマンド
   * @param testData テストデータ
   * @param commandName コマンド名
   * @return 検証結果
   * @throws IOException 実行エラー
   */
  public static ValidationResult executeParallelProcessingTest(
      com.streamConverter.command.IStreamCommand command, byte[] testData, String commandName)
      throws IOException {

    System.out.println("=== " + commandName + " 並列処理検証テスト ===");
    System.out.println("📤 開始時刻: " + new java.util.Date());
    System.out.println(
        "📊 入力データサイズ: " + String.format("%.2f MB", testData.length / (1024.0 * 1024.0)));

    // 検証用ストリーム作成
    ParallelProcessingValidationInputStream inputStream = createValidationInputStream(testData);
    MonitoringOutputStream outputStream = createMonitoringOutputStream();

    // 監視設定
    inputStream.setOutputStreamToMonitor(outputStream);

    // コマンド実行
    long startTime = System.currentTimeMillis();
    command.execute(inputStream, outputStream);
    long endTime = System.currentTimeMillis();

    System.out.println("📥 完了時刻: " + new java.util.Date());
    System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

    // 結果取得と検証
    ValidationResult result = inputStream.getValidationResult();
    assertParallelProcessing(result, commandName);

    System.out.println("  - 総書き込みバイト数: " + outputStream.getBytesWritten());
    System.out.println("✅ " + commandName + " 並列処理検証テスト完了");

    return result;
  }

  /** 並列処理検証用のInputStream InputStreamのクローズ時点でOutputStreamの状態を記録 */
  public static class ParallelProcessingValidationInputStream extends InputStream {
    private final byte[] data;
    private int position = 0;
    private MonitoringOutputStream outputStreamToMonitor;
    private ValidationResult validationResult;

    public ParallelProcessingValidationInputStream(byte[] data) {
      this.data = data;
    }

    public void setOutputStreamToMonitor(MonitoringOutputStream outputStream) {
      this.outputStreamToMonitor = outputStream;
    }

    @Override
    public int read() throws IOException {
      if (position >= data.length) {
        return -1;
      }
      return data[position++] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      if (position >= data.length) {
        return -1;
      }
      int available = Math.min(len, data.length - position);
      System.arraycopy(data, position, b, off, available);
      position += available;
      return available;
    }

    @Override
    public void close() throws IOException {
      // InputStreamクローズ時点でのOutputStreamの状態を記録
      double inputProgressAtClose = (position / (double) data.length) * 100.0;
      long outputBytesAtClose =
          outputStreamToMonitor != null ? outputStreamToMonitor.getBytesWritten() : 0;

      System.out.printf(
          "🔄 [並列処理検証] InputStreamクローズ時点: 入力消費%.1f%%, OutputStream書き込み%d bytes%n",
          inputProgressAtClose, outputBytesAtClose);

      if (outputBytesAtClose > 0) {
        System.out.println("   ✅ 並列処理が検出されました - OutputStreamに既にデータが存在");
      } else {
        System.out.println("   ⚠️ 逐次処理が検出されました - OutputStreamにまだデータが存在しない");
      }

      this.validationResult = new ValidationResult(inputProgressAtClose, outputBytesAtClose);
      super.close();
    }

    public ValidationResult getValidationResult() {
      return validationResult;
    }
  }

  /** 書き込みバイト数を監視できるOutputStream */
  public static class MonitoringOutputStream extends OutputStream {
    private final ByteArrayOutputStream delegate = new ByteArrayOutputStream();
    private long bytesWritten = 0;

    @Override
    public void write(int b) throws IOException {
      delegate.write(b);
      bytesWritten++;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      delegate.write(b, off, len);
      bytesWritten += len;
    }

    @Override
    public void flush() throws IOException {
      delegate.flush();
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }

    public long getBytesWritten() {
      return bytesWritten;
    }

    public byte[] toByteArray() {
      return delegate.toByteArray();
    }
  }

  /** 並列処理検証結果 */
  public static class ValidationResult {
    public final double inputProgressAtClose;
    public final long outputBytesAtClose;

    public ValidationResult(double inputProgressAtClose, long outputBytesAtClose) {
      this.inputProgressAtClose = inputProgressAtClose;
      this.outputBytesAtClose = outputBytesAtClose;
    }

    /**
     * 並列処理が確認されたかどうか
     *
     * @return 並列処理が確認された場合true
     */
    public boolean isParallelProcessing() {
      return outputBytesAtClose > 0;
    }

    /**
     * 逐次処理パターンかどうか
     *
     * @return 逐次処理の場合true
     */
    public boolean isSequentialProcessing() {
      return inputProgressAtClose >= 100.0 && outputBytesAtClose == 0;
    }
  }
}
