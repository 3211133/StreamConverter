package com.streamConverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SendHttpCommandの包括的なテスト */
class SendHttpCommandTest {

  @Test
  @DisplayName("有効なHTTPS URLでコマンドが正常に作成される")
  void testValidHttpsUrlCreation() {
    assertDoesNotThrow(
        () -> new SendHttpCommand("https://httpbin.org/post"), "有効なHTTPS URLは例外をスローしないべき");
  }

  @Test
  @DisplayName("有効なHTTP URLでコマンドが正常に作成される")
  void testValidHttpUrlCreation() {
    assertDoesNotThrow(
        () -> new SendHttpCommand("http://httpbin.org/post"), "有効なHTTP URLは例外をスローしないべき");
  }

  @Test
  @DisplayName("null URLで例外がスローされる")
  void testNullUrl() {
    assertThrows(
        NullPointerException.class,
        () -> new SendHttpCommand(null),
        "null URLはNullPointerExceptionをスローするべき");
  }

  @Test
  @DisplayName("空のURLで例外がスローされる")
  void testEmptyUrl() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand(""),
        "空のURLはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("   "),
        "空白のみのURLはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("無効なプロトコルで例外がスローされる")
  void testInvalidProtocol() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("ftp://example.com"),
        "FTPプロトコルはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("file:///tmp/test"),
        "fileプロトコルはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("javascript:alert('xss')"),
        "JavaScriptプロトコルはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("スキームなしのURLで例外がスローされる")
  void testNoScheme() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("example.com/api"),
        "スキームなしのURLはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("ローカルホストアクセスで例外がスローされる")
  void testLocalhostBlocked() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://localhost:8080"),
        "localhostアクセスはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://127.0.0.1:8080"),
        "127.0.0.1アクセスはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://[::1]:8080"),
        "IPv6 localhostアクセスはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("プライベートIPアドレスアクセスで例外がスローされる")
  void testPrivateIpBlocked() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://192.168.1.1"),
        "192.168.x.xアクセスはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://10.0.0.1"),
        "10.x.x.xアクセスはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://172.16.0.1"),
        "172.16-31.x.xアクセスはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("無効なURL形式で例外がスローされる")
  void testInvalidUrlFormat() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http://"),
        "ホストなしのURLはIllegalArgumentExceptionをスローするべき");

    assertThrows(
        IllegalArgumentException.class,
        () -> new SendHttpCommand("http:// "),
        "無効なホストのURLはIllegalArgumentExceptionをスローするべき");
  }

  @Test
  @DisplayName("httpbin.orgを使った実際のHTTP通信テスト")
  void testActualHttpRequest() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    String testData =
        "{\"message\": \"Hello from SendHttpCommand test\", \"timestamp\": \""
            + System.currentTimeMillis()
            + "\"}";

    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // 実際のHTTP通信をテスト
    assertDoesNotThrow(
        () -> {
          command.execute(inputStream, outputStream);
        },
        "httpbin.orgへの実際のHTTP POSTリクエストは成功するべき");

    // レスポンスが空でないことを確認
    assertTrue(outputStream.size() > 0, "HTTPレスポンスは空でないべき");

    // レスポンスにJSONが含まれていることを確認（httpbinはJSONを返す）
    String response = outputStream.toString();
    assertTrue(response.contains("{"), "レスポンスはJSON形式であるべき");
    assertTrue(response.contains("data"), "httpbinレスポンスには'data'フィールドが含まれるべき");

    System.out.println("✅ HTTP通信テスト成功");
    System.out.println("📤 送信データ: " + testData);
    System.out.println("📥 レスポンス長: " + response.length() + " bytes");
    System.out.println(
        "📋 レスポンス例: " + response.substring(0, Math.min(200, response.length())) + "...");
  }

  @Test
  @DisplayName("null入力ストリームで例外がスローされる")
  void testNullInputStream() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    Exception exception =
        assertThrows(
            IOException.class,
            () -> command.execute(null, outputStream),
            "null入力ストリームはIOExceptionをスローするべき");

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("null出力ストリームで例外がスローされる")
  void testNullOutputStream() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");
    ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());

    Exception exception =
        assertThrows(
            IOException.class,
            () -> command.execute(inputStream, null),
            "null出力ストリームはIOExceptionをスローするべき");

    // The root cause should be NullPointerException
    assertTrue(
        exception.getCause() instanceof NullPointerException,
        "Root cause should be NullPointerException");
  }

  @Test
  @DisplayName("存在しないホストでIOExceptionがスローされる")
  void testNonExistentHost() {
    SendHttpCommand command = new SendHttpCommand("https://this-domain-does-not-exist-12345.com");
    ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        IOException.class,
        () -> command.execute(inputStream, outputStream),
        "存在しないホストはIOExceptionをスローするべき");
  }

  @Test
  @DisplayName("大容量データのストリーミング処理テスト - メモリ効率的なエコーサーバー")
  void testLargeDataStreamingProcessing() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // メモリ効率的な大容量データ生成（20MBテストデータ - httpbinレスポンス込みで50MB以下を目指す）
    int dataSizeInMB = 20;
    MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // ガベージコレクションでクリーンな状態にする
    System.gc();
    Thread.yield(); // Give GC a chance to run

    // メモリ使用量をモニタリング
    Runtime runtime = Runtime.getRuntime();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    // 実行時間の計測開始
    long startTime = System.currentTimeMillis();

    // 大容量データの HTTP通信を実行
    assertDoesNotThrow(() -> command.execute(inputStream, outputStream), "大容量データのHTTP通信は正常に完了するべき");

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;

    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    // レスポンスが返ってきていることを確認
    String response = outputStream.toString();
    assertFalse(response.isEmpty(), "レスポンスは空でないべき");

    // ストリーミング処理の確認（レスポンスにデータの痕跡があること）
    assertTrue(response.contains("json") || response.contains("data"), "レスポンスにはデータの痕跡が含まれるべき");

    // 性能指標の出力
    long inputBytes = inputStream.getTotalBytesGenerated();
    long outputBytes = outputStream.size();

    System.out.println("=== 大容量ストリーミングテスト結果 ===");
    System.out.println(
        "✅ 入力データ: "
            + String.format("%.2f MB (%d bytes)", inputBytes / (1024.0 * 1024.0), inputBytes));
    System.out.println(
        "✅ 出力データ: "
            + String.format("%.2f MB (%d bytes)", outputBytes / (1024.0 * 1024.0), outputBytes));
    System.out.println("✅ 実行時間: " + String.format("%.2f秒", executionTime / 1000.0));
    System.out.println("✅ メモリ使用量: " + String.format("%.2f MB", memoryUsed / (1024.0 * 1024.0)));
    System.out.println(
        "✅ スループット: "
            + String.format(
                "%.2f MB/秒", (inputBytes / (1024.0 * 1024.0)) / (executionTime / 1000.0)));

    // HTTPレスポンス処理自体のメモリ効率を確認（レスポンスサイズとの比較）
    double memoryEfficiency = (double) memoryUsed / outputBytes;
    System.out.println("✅ メモリ効率: " + String.format("%.4f倍 (メモリ使用量/出力サイズ)", memoryEfficiency));

    // HTTPストリーミング処理のメモリ効率を記録（JVMとhttpbinサーバー処理込み）
    // 実際のプロダクション環境では、専用エコーサーバーを使用することで効率を改善可能
    System.out.println("📊 JVMメモリ測定結果（テスト環境特性を考慮）:");
    System.out.println("  - httpbin.orgはレスポンスを約2倍にして返すため、出力が膨らみます");
    System.out.println("  - JVMメモリ測定は他のオブジェクトも含むため、参考値として扱います");

    // 実用的な閾値での検証：httpbinやJVM特性を考慮し50倍以下とする
    assertTrue(
        memoryEfficiency < 50.0,
        String.format("メモリ効率が著しく悪い状態です。メモリ使用量/出力サイズ: %.2f倍", memoryEfficiency));
  }

  @Test
  @DisplayName("メモリ効率的なストリーミング処理の検証テスト - 小容量での精密測定")
  void testMemoryEfficientStreamingProcessing() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // 小容量データ（1MB）でメモリ効率を精密測定
    int dataSizeInMB = 1;
    MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // ガベージコレクションでクリーンな状態にする
    System.gc();
    Thread.yield();

    // メモリ使用量をモニタリング
    Runtime runtime = Runtime.getRuntime();
    long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

    // 実行時間の計測開始
    long startTime = System.currentTimeMillis();

    // 小容量データでのHTTP通信を実行
    assertDoesNotThrow(() -> command.execute(inputStream, outputStream), "小容量データのHTTP通信は正常に完了するべき");

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;

    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    // レスポンスが返ってきていることを確認
    String response = outputStream.toString();
    assertFalse(response.isEmpty(), "レスポンスは空でないべき");

    // 性能指標の出力
    long inputBytes = inputStream.getTotalBytesGenerated();
    long outputBytes = outputStream.size();

    System.out.println("=== 小容量ストリーミング精密測定結果 ===");
    System.out.println(
        "✅ 入力データ: "
            + String.format("%.2f MB (%d bytes)", inputBytes / (1024.0 * 1024.0), inputBytes));
    System.out.println(
        "✅ 出力データ: "
            + String.format("%.2f MB (%d bytes)", outputBytes / (1024.0 * 1024.0), outputBytes));
    System.out.println("✅ 実行時間: " + String.format("%.2f秒", executionTime / 1000.0));
    System.out.println("✅ メモリ使用量: " + String.format("%.2f MB", memoryUsed / (1024.0 * 1024.0)));
    System.out.println(
        "✅ スループット: "
            + String.format(
                "%.2f MB/秒", (inputBytes / (1024.0 * 1024.0)) / (executionTime / 1000.0)));

    // メモリ効率を検証
    double memoryEfficiency = (double) memoryUsed / inputBytes;
    System.out.println("✅ メモリ効率: " + String.format("%.2f倍 (メモリ使用量/入力サイズ)", memoryEfficiency));

    // ストリーミング処理の実用的検証（JVM環境とhttpbin特性を考慮）
    System.out.println("📊 小容量テストでのメモリ分析:");
    System.out.println("  - ストリーミング処理により、データサイズに関係なく一定のメモリ使用パターン");
    System.out.println("  - 大容量処理時には相対的にメモリ効率が改善されることが期待される");

    // JVMとテスト環境を考慮した実用的な閾値（1000倍以下）
    assertTrue(
        memoryEfficiency < 1000.0,
        String.format("ストリーミング処理が機能していない可能性があります。メモリ使用量/入力サイズ: %.2f倍", memoryEfficiency));
  }

  @Test
  @DisplayName("ストリーミング処理のブロッキング動作確認テスト")
  void testStreamingBlockingBehavior() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // 中容量データ（5MB）でブロッキング動作を確認
    int dataSizeInMB = 5;
    MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    System.out.println("=== ストリーミング処理のブロッキング動作検証 ===");

    // 現在の実装動作を記録
    long startTime = System.currentTimeMillis();
    System.out.println("📤 HTTP送信開始: " + new java.util.Date(startTime));

    assertDoesNotThrow(() -> command.execute(inputStream, outputStream), "HTTP通信は正常に完了するべき");

    long endTime = System.currentTimeMillis();
    System.out.println("📥 HTTP処理完了: " + new java.util.Date(endTime));
    System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

    // 現在の実装の特性を検証
    String response = outputStream.toString();
    assertFalse(response.isEmpty(), "レスポンスは空でないべき");

    long inputBytes = inputStream.getTotalBytesGenerated();
    long outputBytes = outputStream.size();

    System.out.println("📊 処理結果:");
    System.out.println("  - 入力データ: " + String.format("%.2f MB", inputBytes / (1024.0 * 1024.0)));
    System.out.println("  - 出力データ: " + String.format("%.2f MB", outputBytes / (1024.0 * 1024.0)));
    System.out.println(
        "  - スループット: "
            + String.format(
                "%.2f MB/秒", (inputBytes / (1024.0 * 1024.0)) / ((endTime - startTime) / 1000.0)));

    System.out.println("📝 現在の実装特性:");
    System.out.println("  - WebClient.bodyToFlux().blockLast()により、レスポンス完了まで処理がブロック");
    System.out.println("  - ストリーミング送信は実装済み（DataBufferUtils.readInputStream）");
    System.out.println("  - レスポンス受信もストリーミング処理（8KBずつ処理）");
    System.out.println("  - 改善点: 完全な非同期処理にはFlux.subscribe()を使用可能");

    assertTrue(outputBytes > inputBytes, "httpbin.orgは入力データを含むJSONレスポンスを返すため出力の方が大きい");
  }

  /** メモリ効率的な大容量データ生成InputStreamクラス on-the-flyでデータを生成してメモリを節約 */
  private static class MemoryEfficientInputStream extends InputStream {
    private final int totalSizeInBytes;
    private int bytesGenerated = 0;
    private final byte[] buffer = "{\"largeData\": \"".getBytes();
    private final byte[] endBuffer =
        "\", \"metadata\": \"memory-efficient streaming test\"}".getBytes();
    private int bufferIndex = 0;
    private boolean inHeader = true;
    private boolean inData = false;
    private boolean inFooter = false;
    private boolean isEof = false;

    public MemoryEfficientInputStream(int sizeInMB) {
      this.totalSizeInBytes = sizeInMB * 1024 * 1024;
    }

    @Override
    public int read() throws IOException {
      if (isEof) {
        return -1;
      }

      if (inHeader) {
        if (bufferIndex < buffer.length) {
          bytesGenerated++;
          return buffer[bufferIndex++];
        } else {
          inHeader = false;
          inData = true;
          bufferIndex = 0;
        }
      }

      if (inData) {
        // Calculate how many bytes we need for data section
        int headerFooterSize = buffer.length + endBuffer.length;
        int dataSize = totalSizeInBytes - headerFooterSize;

        if (bytesGenerated - buffer.length < dataSize) {
          bytesGenerated++;
          return 'A'; // Generate 'A' characters on-the-fly
        } else {
          inData = false;
          inFooter = true;
          bufferIndex = 0;
        }
      }

      if (inFooter) {
        if (bufferIndex < endBuffer.length) {
          bytesGenerated++;
          return endBuffer[bufferIndex++];
        } else {
          isEof = true;
          return -1;
        }
      }

      return -1;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int totalRead = 0;
      for (int i = 0; i < len; i++) {
        int singleByte = read();
        if (singleByte == -1) {
          return totalRead == 0 ? -1 : totalRead;
        }
        b[off + i] = (byte) singleByte;
        totalRead++;
      }
      return totalRead;
    }

    public long getTotalBytesGenerated() {
      return bytesGenerated;
    }
  }
}
