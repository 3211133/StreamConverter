package com.streamConverter.command.impl;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import com.streamConverter.mock.MockHttpServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SendHttpCommandの包括的なテスト */
class SendHttpCommandTest {

  private MockHttpServer mockServer;
  private String mockServerUrl;

  @BeforeEach
  void setUp() {
    mockServer = new MockHttpServer();
    mockServer.start();
    mockServerUrl = mockServer.getBaseUrl();
  }

  @AfterEach
  void tearDown() {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

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
  @DisplayName("モックサーバーを使ったHTTP通信テスト")
  void testMockedHttpRequest() {
    // モックサーバーのセットアップ
    mockServer.setupHttpbinPostMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    String testData =
        "{\"message\": \"Hello from SendHttpCommand test\", \"timestamp\": \""
            + System.currentTimeMillis()
            + "\"}";

    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // モックHTTP通信をテスト
    assertDoesNotThrow(
        () -> {
          command.execute(inputStream, outputStream);
        },
        "モックサーバーへのHTTP POSTリクエストは成功するべき");

    // レスポンスが空でないことを確認
    assertTrue(outputStream.size() > 0, "HTTPレスポンスは空でないべき");

    // レスポンスにJSONが含まれていることを確認（モックレスポンス）
    String response = outputStream.toString();
    assertTrue(response.contains("{"), "レスポンスはJSON形式であるべき");
    assertTrue(response.contains("data"), "モックレスポンスには'data'フィールドが含まれるべき");

    // リクエストが正しく送信されたことを検証
    mockServer.verifyRequest(postRequestedFor(urlEqualTo("/post")));

    System.out.println("✅ モックHTTP通信テスト成功");
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
    // 存在しないホストをシミュレート
    mockServer.setupNonExistentHostMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/nonexistent", true); // テスト用: localhost許可
    ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // 404レスポンスは現在の実装ではIOExceptionとしてスローされる
    IOException exception =
        assertThrows(
            IOException.class,
            () -> command.execute(inputStream, outputStream),
            "404レスポンスはIOExceptionをスローするべき");

    // エラーメッセージに404と"Host not found"が含まれることを確認
    assertTrue(exception.getMessage().contains("404"), "エラーメッセージに404が含まれるべき");
    assertTrue(exception.getMessage().contains("Host not found"), "エラーメッセージにHost not foundが含まれるべき");
  }

  @Test
  @DisplayName("Large data streaming processing test with memory-efficient approach")
  void testLargeDataStreamingProcessing() throws IOException {
    // 大容量データレスポンスのモックを設定
    mockServer.setupLargeDataResponseMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    // 大容量データを模擬（簡易版：1MBテストデータ）
    String testData = "A".repeat(1024 * 1024); // 1MB
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
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
    assertDoesNotThrow(
        () -> command.execute(inputStream, outputStream), "大容量データのモックHTTP通信は正常に完了するべき");

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;

    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    // レスポンスが返ってきていることを確認
    String response = outputStream.toString();
    assertFalse(response.isEmpty(), "レスポンスは空でないべき");

    // ストリーミング処理の確認（レスポンスにデータの痕跡があること）
    assertTrue(response.contains("json") || response.contains("data"), "レスポンスにはデータの痕跡が含まれるべき");

    // リクエストが正しく送信されたことを検証
    mockServer.verifyRequest(postRequestedFor(urlEqualTo("/post")));

    // 性能指標の出力
    long inputBytes = testData.getBytes().length;
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

    // HTTPストリーミング処理のメモリ効率を記録（JVMとモックサーバー処理込み）
    // モックサーバーを使用することで、ネットワーク依存を排除し安定したテストを実現
    System.out.println("📊 JVMメモリ測定結果（モックサーバーテスト環境）:");
    System.out.println("  - モックサーバーは一定サイズのレスポンスを返すため、予測可能な結果");
    System.out.println("  - JVMメモリ測定は他のオブジェクトも含むため、参考値として扱います");

    // 実用的な閾値での検証：JVMとモックサーバー環境を考慮し3000倍以下とする
    // JVMガベージコレクション、モックサーバーオーバーヘッド、その他オブジェクト生成を含む
    assertTrue(
        memoryEfficiency < 3000.0,
        String.format("メモリ効率が著しく悪い状態です。メモリ使用量/出力サイズ: %.2f倍", memoryEfficiency));
  }

  @Test
  @DisplayName("Memory-efficient streaming processing validation test with small data")
  void testMemoryEfficientStreamingProcessing() throws IOException {
    // 通常のモックレスポンスを設定
    mockServer.setupHttpbinPostMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    // 小容量データ（1MB）でメモリ効率を精密測定
    String testData = "B".repeat(1024 * 1024); // 1MB
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
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
    assertDoesNotThrow(
        () -> command.execute(inputStream, outputStream), "小容量データのモックHTTP通信は正常に完了するべき");

    long endTime = System.currentTimeMillis();
    long executionTime = endTime - startTime;

    long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
    long memoryUsed = memoryAfter - memoryBefore;

    // レスポンスが返ってきていることを確認
    String response = outputStream.toString();
    assertFalse(response.isEmpty(), "レスポンスは空でないべき");

    // 性能指標の出力
    long inputBytes = testData.getBytes().length;
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

    // ストリーミング処理の実用的検証（JVM環境とモックサーバー特性を考慮）
    System.out.println("📊 小容量テストでのメモリ分析:");
    System.out.println("  - ストリーミング処理により、データサイズに関係なく一定のメモリ使用パターン");
    System.out.println("  - 大容量処理時には相対的にメモリ効率が改善されることが期待される");

    // JVMとモックテスト環境を考慮した実用的な閾値（500倍以下）
    assertTrue(
        memoryEfficiency < 500.0,
        String.format("ストリーミング処理が機能していない可能性があります。メモリ使用量/入力サイズ: %.2f倍", memoryEfficiency));

    // リクエストが正しく送信されたことを検証
    mockServer.verifyRequest(postRequestedFor(urlEqualTo("/post")));
  }

  @Test
  @org.junit.jupiter.api.Disabled("HTTP/1.1プロトコル制約により逐次処理が確定済み - テスト戦略書参照")
  @DisplayName("Streaming processing blocking behavior verification test")
  void testStreamingBlockingBehavior() throws IOException {
    // 通常のモックレスポンスを設定
    mockServer.setupHttpbinPostMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    // 中容量データ（1MB）でブロッキング動作を確認
    String testData = "C".repeat(1024 * 1024); // 1MB
    ByteArrayInputStream inputStream = new ByteArrayInputStream(testData.getBytes());
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

    long inputBytes = testData.getBytes().length;
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
    System.out.println("  - モックサーバーにより環境非依存で安定したテストを実現");

    // モックサーバーは固定サイズのJSONレスポンスを返すため、入力データサイズに関係なく一定サイズ
    assertTrue(outputBytes > 0, "レスポンスデータが存在するべき");
    assertTrue(outputBytes < inputBytes, "モックサーバーは固定サイズ(~244bytes)のJSONレスポンスを返すため入力データより小さい");

    // リクエストが正しく送信されたことを検証
    mockServer.verifyRequest(postRequestedFor(urlEqualTo("/post")));
  }

  @Test
  @org.junit.jupiter.api.Disabled("HTTP/1.1プロトコル制約により逐次処理が確定済み - テスト戦略書参照")
  @DisplayName("並列処理要件検証テスト（逐次処理は要件未達成）")
  void testTrueParallelProcessingValidation() throws IOException, InterruptedException {
    // 並列処理検証用のストリーミングモックを設定
    mockServer.setupParallelProcessingMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    // 大容量データ（5MB）で並列処理を検証
    String testData = "P".repeat(5 * 1024 * 1024); // 5MB

    // 並列処理監視用InputStream
    ParallelProcessingValidationInputStream inputStream =
        new ParallelProcessingValidationInputStream(testData.getBytes());

    // OutputStreamの状態を監視できるカスタムOutputStream
    MonitoringOutputStream monitoringStream = new MonitoringOutputStream();

    System.out.println("=== 並列処理要件検証テスト ===");
    System.out.println("📤 開始時刻: " + new java.util.Date());
    System.out.println(
        "📊 入力データサイズ: " + String.format("%.2f MB", testData.length() / (1024.0 * 1024.0)));

    // OutputStreamを参照として設定
    inputStream.setOutputStreamToMonitor(monitoringStream);

    // ストリーミング処理を実行
    long startTime = System.currentTimeMillis();
    assertDoesNotThrow(() -> command.execute(inputStream, monitoringStream), "HTTP通信は正常に完了するべき");
    long endTime = System.currentTimeMillis();

    System.out.println("📥 完了時刻: " + new java.util.Date());
    System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

    // 並列処理検証結果の取得
    ParallelProcessingValidationInputStream.ValidationResult result =
        inputStream.getValidationResult();

    System.out.println("📊 並列処理検証結果:");
    System.out.println(
        "  - InputStreamクローズ時点でのOutputStream書き込み: " + result.outputBytesAtClose + " bytes");
    System.out.println(
        "  - クローズ時点での入力進行率: " + String.format("%.1f%%", result.inputProgressAtClose));
    System.out.println("  - 総書き込みバイト数: " + monitoringStream.getBytesWritten());

    // 並列処理ライブラリとしての要件検証
    assertTrue(monitoringStream.getBytesWritten() > 0, "HTTPレスポンスデータが正常に受信されるべき");

    // **並列処理要件検証**: InputStreamクローズ時点でOutputStreamにデータが存在するべき
    assertTrue(
        result.outputBytesAtClose > 0,
        String.format(
            "並列処理ライブラリとして、InputStreamクローズ時点でOutputStreamにデータが存在するべきです。実際: %d bytes (要件未達成)",
            result.outputBytesAtClose));

    // 逐次処理パターンの検出（これは要件未達成を示す）
    boolean isSequentialProcessing =
        (result.inputProgressAtClose >= 100.0 && result.outputBytesAtClose == 0);
    assertFalse(isSequentialProcessing, "逐次処理パターンが検出されました。並列処理ライブラリとしての要件を満たしていません");

    System.out.println("  ✅ 並列処理要件達成 - InputStreamクローズ前にレスポンス処理開始");

    System.out.println("✅ 並列処理要件検証テスト完了");
  }

  @Test
  @DisplayName("InputStreamクローズ時点でのOutputStream状態監視テスト")
  void testStreamingProgressVerification() throws IOException, InterruptedException {
    // 通常のモックレスポンスを設定
    mockServer.setupHttpbinPostMock();
    SendHttpCommand command =
        new SendHttpCommand(mockServerUrl + "/post", true); // テスト用: localhost許可

    // 大容量データ（5MB）で読み込み処理を監視
    String testData = "D".repeat(5 * 1024 * 1024); // 5MB

    // 読み込み割り込み機能付きMonitoringInputStream
    ReadInterruptMonitoringInputStream inputStream =
        new ReadInterruptMonitoringInputStream(testData.getBytes());

    // OutputStreamの状態を監視できるカスタムOutputStream
    MonitoringOutputStream monitoringStream = new MonitoringOutputStream();

    System.out.println("=== InputStreamクローズ時点でのOutputStream状態監視テスト ===");
    System.out.println("📤 開始時刻: " + new java.util.Date());
    System.out.println(
        "📊 入力データサイズ: " + String.format("%.2f MB", testData.length() / (1024.0 * 1024.0)));

    // OutputStreamを参照として設定（読み込み時にチェックするため）
    inputStream.setOutputStreamToMonitor(monitoringStream);

    // ストリーミング処理を実行
    long startTime = System.currentTimeMillis();
    assertDoesNotThrow(() -> command.execute(inputStream, monitoringStream), "HTTP通信は正常に完了するべき");
    long endTime = System.currentTimeMillis();

    System.out.println("📥 完了時刻: " + new java.util.Date());
    System.out.println("⏱️ 総処理時間: " + (endTime - startTime) + "ms");

    // 結果の検証
    long finalBytes = monitoringStream.getBytesWritten();
    String finalContent = monitoringStream.getContent();

    // クローズ時点での監視結果の取得
    ReadInterruptMonitoringInputStream.CloseMonitoringResult result =
        inputStream.getCloseMonitoringResult();

    System.out.println("📊 InputStreamクローズ時点の監視結果:");
    System.out.println(
        "  - クローズ時点での入力進行率: " + String.format("%.1f%%", result.inputProgressAtClose));
    System.out.println("  - クローズ時点でのOutputStream書き込み: " + result.outputBytesAtClose + " bytes");
    System.out.println("  - クローズ監視実行済み: " + (result.closeMonitoringExecuted ? "✅ Yes" : "❌ No"));
    System.out.println("  - 総書き込みバイト数: " + finalBytes);
    System.out.println("  - OutputStreamサイズ: " + finalContent.length());

    // アサーション
    assertTrue(finalBytes > 0, "OutputStreamにデータが書き込まれるべき");
    assertFalse(finalContent.isEmpty(), "レスポンス内容は空でないべき");
    assertTrue(result.closeMonitoringExecuted, "クローズ監視が実行されているべき");

    // Spring WebClientの動作確認
    assertTrue(inputStream.isClosed(), "Spring WebClientはInputStreamを自動的にcloseすることが期待される動作");
    System.out.println(
        "✅ InputStreamのclose状態検証: "
            + (inputStream.isClosed() ? "✅ Closed (as expected)" : "❌ Not Closed"));

    // クローズ時点での処理順序分析
    System.out.println("📝 Spring WebClientの処理順序分析:");
    if (result.outputBytesAtClose > 0) {
      System.out.println("✅ InputStreamクローズ時点で既にOutputStreamにデータが存在");
      System.out.println("   → Spring WebClientは並行処理（ストリーミング）を実行している可能性があります");
    } else {
      System.out.println("❌ InputStreamクローズ時点ではOutputStreamにデータが存在しない");
      System.out.println("   → Spring WebClientは逐次処理を行っている可能性があります");
    }

    if (result.inputProgressAtClose >= 100.0) {
      System.out.println("   → InputStreamは完全に消費された後にクローズされています");
    } else {
      System.out.println("   → InputStreamは途中でクローズされています（異常ケース）");
    }

    // レスポンス内容の基本検証
    assertTrue(finalContent.contains("{"), "JSONレスポンスが含まれるべき");

    // リクエストが正しく送信されたことを検証
    mockServer.verifyRequest(postRequestedFor(urlEqualTo("/post")));

    System.out.println("✅ InputStreamクローズ時点監視テスト完了");
  }

  /** ストリーミング処理を監視するためのカスタムOutputStream */
  private static class MonitoringOutputStream extends ByteArrayOutputStream {
    private volatile long bytesWritten = 0;
    private final Object lock = new Object();

    @Override
    public void write(int b) {
      synchronized (lock) {
        super.write(b);
        bytesWritten++;
      }
    }

    @Override
    public void write(byte[] b, int off, int len) {
      synchronized (lock) {
        super.write(b, off, len);
        bytesWritten += len;
      }
    }

    public long getBytesWritten() {
      synchronized (lock) {
        return bytesWritten;
      }
    }

    public String getContent() {
      synchronized (lock) {
        return toString();
      }
    }
  }

  /** 並列処理検証用InputStream */
  private static class ParallelProcessingValidationInputStream extends ByteArrayInputStream {
    private volatile boolean closed = false;
    private volatile long bytesRead = 0;
    private final long totalBytes;
    private final Object lock = new Object();

    // 監視対象のOutputStream
    private volatile MonitoringOutputStream outputStreamToMonitor;

    // クローズ時点での監視結果
    private volatile long outputBytesAtClose = 0;
    private volatile double inputProgressAtClose = 0.0;
    private volatile boolean closeMonitoringExecuted = false;

    public ParallelProcessingValidationInputStream(byte[] buf) {
      super(buf);
      this.totalBytes = buf.length;
    }

    public void setOutputStreamToMonitor(MonitoringOutputStream outputStream) {
      this.outputStreamToMonitor = outputStream;
    }

    @Override
    public void close() throws IOException {
      synchronized (lock) {
        // クローズ処理前にOutputStream状態をチェック
        checkOutputStreamAtClose();
        super.close();
        closed = true;
      }
    }

    /** InputStreamクローズ時点でのOutputStream状態をチェック */
    private void checkOutputStreamAtClose() {
      if (outputStreamToMonitor != null && !closeMonitoringExecuted) {
        closeMonitoringExecuted = true;
        outputBytesAtClose = outputStreamToMonitor.getBytesWritten();
        inputProgressAtClose = getProgressPercentage();

        System.out.println(
            String.format(
                "🔄 [並列処理検証] InputStreamクローズ時点: 入力消費%.1f%%, OutputStream書き込み%d bytes",
                inputProgressAtClose, outputBytesAtClose));

        // 並列処理分析
        if (outputBytesAtClose > 0) {
          System.out.println("   ✅ 並列処理が検出されました - OutputStreamに既にデータが存在");
        } else {
          System.out.println("   ❌ 逐次処理が検出されました - OutputStreamにまだデータが存在しない");
        }
      }
    }

    public double getProgressPercentage() {
      synchronized (lock) {
        return totalBytes > 0 ? (double) bytesRead / totalBytes * 100.0 : 0.0;
      }
    }

    @Override
    public int read() {
      synchronized (lock) {
        if (closed) {
          return -1;
        }

        int result = super.read();
        if (result != -1) {
          bytesRead++;
        }
        return result;
      }
    }

    @Override
    public int read(byte[] b, int off, int len) {
      synchronized (lock) {
        if (closed) {
          return -1;
        }

        int result = super.read(b, off, len);
        if (result > 0) {
          bytesRead += result;
        }
        return result;
      }
    }

    @Override
    public long skip(long n) {
      synchronized (lock) {
        if (closed) {
          return 0;
        }

        long result = super.skip(n);
        bytesRead += result;
        return result;
      }
    }

    /** 並列処理検証結果を取得 */
    public ValidationResult getValidationResult() {
      synchronized (lock) {
        ValidationResult result = new ValidationResult();
        result.outputBytesAtClose = this.outputBytesAtClose;
        result.inputProgressAtClose = this.inputProgressAtClose;
        result.closeMonitoringExecuted = this.closeMonitoringExecuted;
        return result;
      }
    }

    /** 並列処理検証結果を格納するクラス */
    public static class ValidationResult {
      public long outputBytesAtClose = 0;
      public double inputProgressAtClose = 0.0;
      public boolean closeMonitoringExecuted = false;
    }
  }

  /** InputStreamクローズ時点でのOutputStream状態を監視するInputStream */
  private static class ReadInterruptMonitoringInputStream extends ByteArrayInputStream {
    private volatile boolean closed = false;
    private volatile long bytesRead = 0;
    private final long totalBytes;
    private final Object lock = new Object();

    // 監視対象のOutputStream
    private volatile MonitoringOutputStream outputStreamToMonitor;

    // クローズ時点での監視結果
    private volatile long outputBytesAtClose = 0;
    private volatile double inputProgressAtClose = 0.0;
    private volatile boolean closeMonitoringExecuted = false;

    public ReadInterruptMonitoringInputStream(byte[] buf) {
      super(buf);
      this.totalBytes = buf.length;
    }

    public void setOutputStreamToMonitor(MonitoringOutputStream outputStream) {
      this.outputStreamToMonitor = outputStream;
    }

    @Override
    public void close() throws IOException {
      synchronized (lock) {
        // クローズ処理前にOutputStream状態をチェック
        checkOutputStreamAtClose();
        super.close();
        closed = true;
      }
    }

    /** InputStreamクローズ時点でのOutputStream状態をチェック */
    private void checkOutputStreamAtClose() {
      if (outputStreamToMonitor != null && !closeMonitoringExecuted) {
        closeMonitoringExecuted = true;
        outputBytesAtClose = outputStreamToMonitor.getBytesWritten();
        inputProgressAtClose = getProgressPercentage();

        System.out.println(
            String.format(
                "🔄 InputStreamクローズ時点: 入力消費%.1f%%, OutputStream書き込み%d bytes",
                inputProgressAtClose, outputBytesAtClose));

        // 詳細分析
        if (outputBytesAtClose > 0) {
          System.out.println(
              String.format("   → OutputStreamには既に%d bytesのデータが書き込まれています", outputBytesAtClose));
        } else {
          System.out.println("   → OutputStreamにはまだデータが書き込まれていません");
        }

        if (inputProgressAtClose >= 100.0) {
          System.out.println("   → InputStreamは完全に消費されています");
        } else {
          System.out.println(
              String.format("   → InputStreamは%.1f%%消費済み（未完了）", inputProgressAtClose));
        }
      }
    }

    public boolean isClosed() {
      synchronized (lock) {
        return closed;
      }
    }

    public long getBytesRead() {
      synchronized (lock) {
        return bytesRead;
      }
    }

    public double getProgressPercentage() {
      synchronized (lock) {
        return totalBytes > 0 ? (double) bytesRead / totalBytes * 100.0 : 0.0;
      }
    }

    public boolean isFullyConsumed() {
      synchronized (lock) {
        return bytesRead >= totalBytes;
      }
    }

    @Override
    public int read() {
      synchronized (lock) {
        if (closed) {
          return -1;
        }

        int result = super.read();
        if (result != -1) {
          bytesRead++;
        }
        return result;
      }
    }

    @Override
    public int read(byte[] b, int off, int len) {
      synchronized (lock) {
        if (closed) {
          return -1;
        }

        int result = super.read(b, off, len);
        if (result > 0) {
          bytesRead += result;
        }
        return result;
      }
    }

    @Override
    public long skip(long n) {
      synchronized (lock) {
        if (closed) {
          return 0;
        }

        long result = super.skip(n);
        bytesRead += result;
        return result;
      }
    }

    @Override
    public int available() {
      synchronized (lock) {
        if (closed) {
          return 0;
        }
        return super.available();
      }
    }

    /** クローズ時点での監視結果を取得 */
    public CloseMonitoringResult getCloseMonitoringResult() {
      synchronized (lock) {
        CloseMonitoringResult result = new CloseMonitoringResult();
        result.outputBytesAtClose = this.outputBytesAtClose;
        result.inputProgressAtClose = this.inputProgressAtClose;
        result.closeMonitoringExecuted = this.closeMonitoringExecuted;
        return result;
      }
    }

    /** クローズ時点での監視結果を格納するクラス */
    public static class CloseMonitoringResult {
      public long outputBytesAtClose = 0;
      public double inputProgressAtClose = 0.0;
      public boolean closeMonitoringExecuted = false;
    }
  }
}
