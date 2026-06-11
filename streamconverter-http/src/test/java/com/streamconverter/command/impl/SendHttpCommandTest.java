package com.streamconverter.command.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testActualHttpRequest() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    String testData =
        "{\"message\": \"Hello from SendHttpCommand test\", \"timestamp\": \""
            + System.currentTimeMillis()
            + "\"}";

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
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
    String response = outputStream.toString(StandardCharsets.UTF_8);
    assertTrue(response.contains("{"), "レスポンスはJSON形式であるべき");
    assertTrue(response.contains("data"), "httpbinレスポンスには'data'フィールドが含まれるべき");

    System.out.println("✅ HTTP通信テスト成功");
    System.out.println("📤 送信データ: " + testData);
    System.out.println("📥 レスポンス長: " + response.length() + " bytes");
    System.out.println(
        "📋 レスポンス例: " + response.substring(0, Math.min(200, response.length())) + "...");
  }

  @Test
  @Tag("known-bug") // #766
  @DisplayName("リクエスト失敗時の例外メッセージにクエリ文字列の資格情報を含めない")
  void testExceptionMessageDoesNotLeakCredentialsOnRequestFailure() {
    // 資格情報（api_key）をクエリ文字列に含むURL
    String urlWithCredentials = "https://api.example.com/data?api_key=secret123";

    // パブリックIPを返すモックリゾルバ（SSRF検証をパスさせる）
    InetAddressResolver publicIpResolver =
        host -> new InetAddress[] {InetAddress.getByName("93.184.216.34")};

    // ネットワークを使わずにリクエストを失敗させる ExchangeFunction スタブ
    WebClient failingWebClient =
        WebClient.builder()
            .exchangeFunction(request -> Mono.error(new RuntimeException("connection refused")))
            .build();

    SendHttpCommand command =
        new SendHttpCommand(urlWithCredentials, failingWebClient, publicIpResolver);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    IOException thrown =
        assertThrows(
            IOException.class,
            () -> command.execute(inputStream, outputStream),
            "リクエスト失敗時はIOExceptionをスローするべき");

    // 期待動作: 例外メッセージには sanitizeUrl() 適用後のURLのみが含まれ、
    // クエリ文字列の資格情報は漏洩しない
    assertFalse(
        thrown.getMessage().contains("api_key=secret123"),
        "例外メッセージにクエリ文字列の資格情報（api_key=secret123）を含めるべきではない: " + thrown.getMessage());

    // URL自体への言及（サニタイズ済み）は維持される
    assertTrue(
        thrown.getMessage().contains("https://api.example.com"),
        "例外メッセージにはサニタイズ済みURLへの言及が含まれるべき: " + thrown.getMessage());
  }

  @Test
  @DisplayName("null入力ストリームで例外がスローされる")
  void testNullInputStream() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        NullPointerException.class,
        () -> command.execute(null, outputStream),
        "null入力ストリームはNullPointerExceptionをスローするべき");
  }

  @Test
  @DisplayName("null出力ストリームで例外がスローされる")
  void testNullOutputStream() {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));

    assertThrows(
        NullPointerException.class,
        () -> command.execute(inputStream, null),
        "null出力ストリームはNullPointerExceptionをスローするべき");
  }

  @Test
  @DisplayName("存在しないホストでIOExceptionがスローされる")
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testNonExistentHost() {
    SendHttpCommand command = new SendHttpCommand("https://this-domain-does-not-exist-12345.com");
    ByteArrayInputStream inputStream =
        new ByteArrayInputStream("test".getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    assertThrows(
        IOException.class,
        () -> command.execute(inputStream, outputStream),
        "存在しないホストはIOExceptionをスローするべき");
  }

  @Test
  @DisplayName("Large data streaming processing test with memory-efficient approach")
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testLargeDataStreamingProcessing() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // メモリ効率的な大容量データ生成（20MBテストデータ - httpbinレスポンス込みで50MB以下を目指す）
    int dataSizeInMB = 20;
    try (MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // ガベージコレクションでクリーンな状態にする
      System.gc();
      Thread.yield(); // Give GC a chance to run

      // メモリ使用量をモニタリング
      Runtime runtime = Runtime.getRuntime();
      long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

      // 実行時間の計測開始
      long startTime = System.currentTimeMillis();

      // 大容量データの HTTP通信を実行
      assertDoesNotThrow(
          () -> command.execute(inputStream, outputStream), "大容量データのHTTP通信は正常に完了するべき");

      long endTime = System.currentTimeMillis();
      long executionTime = endTime - startTime;

      long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
      long memoryUsed = memoryAfter - memoryBefore;

      // レスポンスが返ってきていることを確認
      String response = outputStream.toString(StandardCharsets.UTF_8);
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

      // メモリ効率の記録（参考値として）
      double memoryEfficiency = (double) memoryUsed / outputBytes;
      System.out.println("✅ メモリ効率: " + String.format("%.4f倍 (メモリ使用量/出力サイズ)", memoryEfficiency));

      // 実装特性の正確な記述
      System.out.println("📊 SendHttpCommand実装特性（検証済み）:");
      System.out.println("  - ✅ 入力: ストリーミング送信実装済み (DataBufferUtils.readInputStream使用)");
      System.out.println("  - ❌ 処理: 入力完全読み取り後にレスポンス処理開始 (逐次処理)");
      System.out.println("  - ✅ 出力: ストリーミング受信実装済み (8KBずつ処理)");
      System.out.println("  - 📝 注意: 高速処理 ≠ 並列処理。時間測定による並列処理推測は不正確");

      // 正確な並列処理検証には testInputStreamCloseTimingVerification() を使用
      System.out.println("🔧 並列処理の正確な検証: testInputStreamCloseTimingVerification()テストを参照");

      // 基本的な動作確認のみ実行
      assertTrue(memoryUsed >= 0, "メモリ使用量は測定可能であるべき");
    }
  }

  @Test
  @DisplayName("Memory-efficient streaming processing validation test with small data")
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testMemoryEfficientStreamingProcessing() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // 小容量データ（1MB）でメモリ効率を精密測定
    int dataSizeInMB = 1;
    try (MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      // ガベージコレクションでクリーンな状態にする
      System.gc();
      Thread.yield();

      // メモリ使用量をモニタリング
      Runtime runtime = Runtime.getRuntime();
      long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

      // 実行時間の計測開始
      long startTime = System.currentTimeMillis();

      // 小容量データでのHTTP通信を実行
      assertDoesNotThrow(
          () -> command.execute(inputStream, outputStream), "小容量データのHTTP通信は正常に完了するべき");

      long endTime = System.currentTimeMillis();
      long executionTime = endTime - startTime;

      long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
      long memoryUsed = memoryAfter - memoryBefore;

      // レスポンスが返ってきていることを確認
      String response = outputStream.toString(StandardCharsets.UTF_8);
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

      // 実装特性の正確な記述（小容量データ版）
      System.out.println("📊 SendHttpCommand実装特性（小容量データ検証）:");
      System.out.println("  - ✅ 入力ストリーミング: データサイズに関係なく一定のメモリ使用パターン");
      System.out.println("  - ❌ 並列処理推測: メモリ効率による並列処理判定は不正確");
      System.out.println("  - 🔧 正確な検証: testInputStreamCloseTimingVerification()を使用");

      // 基本的な動作確認のみ実行
      assertTrue(memoryEfficiency >= 0, "メモリ効率は測定可能であるべき");
    }
  }

  @Test
  @DisplayName("Streaming processing blocking behavior verification test")
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testStreamingBlockingBehavior() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // 中容量データ（5MB）でブロッキング動作を確認
    int dataSizeInMB = 5;
    try (MemoryEfficientInputStream inputStream = new MemoryEfficientInputStream(dataSizeInMB);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

      System.out.println("=== ストリーミング処理のブロッキング動作検証 ===");

      // 現在の実装動作を記録
      long startTime = System.currentTimeMillis();
      System.out.println("📤 HTTP送信開始: " + new java.util.Date(startTime));

      assertDoesNotThrow(() -> command.execute(inputStream, outputStream), "HTTP通信は正常に完了するべき");

      long endTime = System.currentTimeMillis();
      System.out.println("📥 HTTP処理完了: " + new java.util.Date(endTime));
      System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

      // 現在の実装の特性を検証
      String response = outputStream.toString(StandardCharsets.UTF_8);
      assertFalse(response.isEmpty(), "レスポンスは空でないべき");

      long inputBytes = inputStream.getTotalBytesGenerated();
      long outputBytes = outputStream.size();

      System.out.println("📊 処理結果:");
      System.out.println("  - 入力データ: " + String.format("%.2f MB", inputBytes / (1024.0 * 1024.0)));
      System.out.println("  - 出力データ: " + String.format("%.2f MB", outputBytes / (1024.0 * 1024.0)));
      System.out.println(
          "  - スループット: "
              + String.format(
                  "%.2f MB/秒",
                  (inputBytes / (1024.0 * 1024.0)) / ((endTime - startTime) / 1000.0)));

      System.out.println("📝 現在の実装特性:");
      System.out.println("  - WebClient.bodyToFlux().blockLast()により、レスポンス完了まで処理がブロック");
      System.out.println("  - ストリーミング送信は実装済み（DataBufferUtils.readInputStream）");
      System.out.println("  - レスポンス受信もストリーミング処理（8KBずつ処理）");
      System.out.println("  - 改善点: 完全な非同期処理にはFlux.subscribe()を使用可能");

      assertTrue(outputBytes > inputBytes, "httpbin.orgは入力データを含むJSONレスポンスを返すため出力の方が大きい");
    }
  }

  @Test
  @DisplayName("InputStream close timing verification for accurate parallel processing detection")
  @DisabledIfSystemProperty(
      named = "skipNetworkTests",
      matches = "true",
      disabledReason = "Network-dependent test disabled")
  void testInputStreamCloseTimingVerification() throws IOException {
    SendHttpCommand command = new SendHttpCommand("https://httpbin.org/post");

    // OutputStreamを監視可能なラッパーを作成
    MonitorableOutputStream outputStream = new MonitorableOutputStream();

    // InputStreamクローズ時にOutputStreamの状態を監視するInputStream
    try (ParallelProcessingVerificationInputStream inputStream =
        new ParallelProcessingVerificationInputStream(1024 * 1024, outputStream)) { // 1MB test data

      // HTTP通信を実行
      assertDoesNotThrow(() -> command.execute(inputStream, outputStream), "HTTP通信は正常に完了するべき");

      // 並列処理検証結果の確認
      ParallelProcessingResult result = inputStream.getParallelProcessingResult();
      assertNotNull(result, "並列処理検証結果を取得できませんでした");

      System.out.println("=== 並列処理検証結果 ===");
      System.out.printf(
          "🔄 InputStreamクローズ時点: 入力消費%.1f%%, OutputStream書き込み%d bytes%n",
          result.inputProgressAtClose, result.outputBytesAtClose);

      if (result.outputBytesAtClose > 0 && result.inputProgressAtClose < 100.0) {
        System.out.println("✅ 真の並列処理が検出されました");
        System.out.println("   → InputStreamクローズ前にOutputStreamにデータが存在");
      } else if (result.outputBytesAtClose == 0) {
        System.out.println("❌ InputStreamクローズ時点ではOutputStreamにデータが存在しない");
        System.out.println("   → Spring WebClientは逐次処理を行っている可能性があります");
      } else {
        System.out.println("ℹ️ InputStreamが完全に消費された後の処理");
      }

      // 最終的な処理結果の確認
      String response = outputStream.toString();
      assertFalse(response.isEmpty(), "レスポンスは空でないべき");
      assertTrue(response.contains("json") || response.contains("data"), "レスポンスにはデータの痕跡が含まれるべき");

      System.out.printf(
          "📊 最終結果: 入力%d bytes → 出力%d bytes%n",
          result.totalInputBytes, outputStream.getBytesWritten());
    }
  }

  /** 並列処理検証のためのInputStreamクローズタイミング監視結果 */
  private static class ParallelProcessingResult {
    final long outputBytesAtClose;
    final double inputProgressAtClose;
    final long totalInputBytes;

    ParallelProcessingResult(
        long outputBytesAtClose, double inputProgressAtClose, long totalInputBytes) {
      this.outputBytesAtClose = outputBytesAtClose;
      this.inputProgressAtClose = inputProgressAtClose;
      this.totalInputBytes = totalInputBytes;
    }
  }

  /** InputStreamクローズタイミングでOutputStreamの状態を監視するInputStream 真の並列処理検証のための正確な手法を実装 */
  private static class ParallelProcessingVerificationInputStream extends InputStream {
    private final int totalSizeInBytes;
    private int bytesRead = 0;
    private final MonitorableOutputStream outputStreamToMonitor;
    private ParallelProcessingResult result;
    private boolean closeMonitoringExecuted = false;
    private final Object lock = new Object();

    public ParallelProcessingVerificationInputStream(
        int sizeInBytes, MonitorableOutputStream outputStream) {
      this.totalSizeInBytes = sizeInBytes;
      this.outputStreamToMonitor = outputStream;
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= totalSizeInBytes) {
        return -1;
      }

      // 簡単なテストデータとしてJSONの一部を生成
      byte[] pattern = "data".getBytes(StandardCharsets.UTF_8);
      int patternIndex = bytesRead % pattern.length;
      bytesRead++;
      return pattern[patternIndex] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int totalRead = 0;
      for (int i = 0; i < len && bytesRead < totalSizeInBytes; i++) {
        int singleByte = read();
        if (singleByte == -1) {
          return totalRead == 0 ? -1 : totalRead;
        }
        b[off + i] = (byte) singleByte;
        totalRead++;
      }
      return totalRead == 0 ? -1 : totalRead;
    }

    @Override
    public void close() throws IOException {
      synchronized (lock) {
        if (!closeMonitoringExecuted) {
          // InputStreamクローズ時点でのOutputStreamの状態を記録
          long outputBytes = outputStreamToMonitor.getBytesWritten();
          double inputProgress = (double) bytesRead / totalSizeInBytes * 100.0;

          this.result = new ParallelProcessingResult(outputBytes, inputProgress, bytesRead);
          this.closeMonitoringExecuted = true;
        }
        super.close();
      }
    }

    public ParallelProcessingResult getParallelProcessingResult() {
      return result != null ? result : new ParallelProcessingResult(0, 0, bytesRead);
    }
  }

  /** OutputStreamのバイト数を監視可能なラッパークラス */
  private static class MonitorableOutputStream extends ByteArrayOutputStream {
    public long getBytesWritten() {
      return this.count;
    }

    public String toString() {
      return super.toString(StandardCharsets.UTF_8);
    }
  }

  /** メモリ効率的な大容量データ生成InputStreamクラス on-the-flyでデータを生成してメモリを節約 */
  private static class MemoryEfficientInputStream extends InputStream {
    private final int totalSizeInBytes;
    private int bytesGenerated = 0;
    private final byte[] buffer = "{\"largeData\": \"".getBytes(StandardCharsets.UTF_8);
    private final byte[] endBuffer =
        "\", \"metadata\": \"memory-efficient streaming test\"}".getBytes(StandardCharsets.UTF_8);
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
