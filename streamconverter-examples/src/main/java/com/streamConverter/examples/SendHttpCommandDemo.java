package com.streamConverter.examples;

import com.streamConverter.StreamConverter;
import com.streamConverter.command.IStreamCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;
import com.streamConverter.command.rule.PassThroughRule;
import com.streamConverter.path.TreePath;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * SendHttpCommandの実用例デモ
 *
 * <p>このデモでは、SendHttpCommandを使用して実際のHTTP通信を行い、 様々なシナリオでの動作を確認します。
 */
public class SendHttpCommandDemo {

  public static void main(String[] args) {
    System.out.println("=== SendHttpCommand実用例デモ ===\n");

    try {
      // シナリオ1: 基本的なHTTP POST送信
      demonstrateBasicHttpPost();

      // シナリオ2: JSONデータの送信
      demonstrateJsonPost();

      // シナリオ3: パイプライン処理でのHTTP送信
      demonstratePipelineHttpSend();

      // シナリオ4: 異なるエンドポイントでのテスト
      demonstrateDifferentEndpoints();

      // シナリオ5: エラーハンドリング
      demonstrateErrorHandling();

    } catch (Exception e) {
      System.err.println("デモ中にエラーが発生しました: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /** シナリオ1: 基本的なHTTP POST送信 */
  private static void demonstrateBasicHttpPost() throws Exception {
    System.out.println("--- シナリオ1: 基本的なHTTP POST送信 ---");

    SendHttpCommand httpCommand = new SendHttpCommand("https://httpbin.org/post");

    String testData = "Hello from StreamConverter SendHttpCommand!";
    System.out.println("📤 送信データ: " + testData);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    httpCommand.execute(inputStream, outputStream);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    System.out.println("📥 レスポンス長: " + response.length() + " bytes");
    System.out.println("✨ レスポンス抜粋: " + extractJsonField(response, "data"));
    System.out.println();
  }

  /** シナリオ2: JSONデータの送信 */
  private static void demonstrateJsonPost() throws Exception {
    System.out.println("--- シナリオ2: JSONデータの送信 ---");

    SendHttpCommand httpCommand = new SendHttpCommand("https://httpbin.org/post");

    String jsonData =
        """
        {
            "user": "test-user",
            "action": "send-http-demo",
            "timestamp": %d,
            "data": {
                "message": "StreamConverter HTTP送信テスト",
                "version": "1.0"
            }
        }
        """
            .formatted(System.currentTimeMillis());

    System.out.println("📤 送信JSON:");
    System.out.println(jsonData);

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(jsonData.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    httpCommand.execute(inputStream, outputStream);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    System.out.println(
        "✨ HTTPヘッダーチェック - Content-Type: " + extractJsonField(response, "Content-Type"));
    String dataField = extractJsonField(response, "data");
    System.out.println(
        "✨ 送信されたデータの確認: "
            + (dataField.length() > 50 ? dataField.substring(0, 50) + "..." : dataField));
    System.out.println();
  }

  /** シナリオ3: パイプライン処理でのHTTP送信 */
  private static void demonstratePipelineHttpSend() throws Exception {
    System.out.println("--- シナリオ3: パイプライン処理でのHTTP送信 ---");

    // JSON変換 → HTTP送信のパイプライン
    JsonNavigateCommand jsonCommand =
        JsonNavigateCommand.create(TreePath.fromJson("$.message"), new PassThroughRule());
    SendHttpCommand httpCommand = new SendHttpCommand("https://httpbin.org/post");

    String originalJson =
        """
        {
            "id": "MSG-001",
            "message": "パイプライン処理テスト: この部分だけをHTTP送信します",
            "metadata": {
                "source": "StreamConverter Pipeline Demo",
                "timestamp": %d
            }
        }
        """
            .formatted(System.currentTimeMillis());

    System.out.println("📄 元のJSON:");
    System.out.println(originalJson);

    // StreamConverterでパイプライン実行
    StreamConverter converter =
        new StreamConverter(new IStreamCommand[] {jsonCommand, httpCommand});

    ByteArrayInputStream inputStream =
        new ByteArrayInputStream(originalJson.getBytes(StandardCharsets.UTF_8));
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    converter.run(inputStream, outputStream);

    String response = outputStream.toString(StandardCharsets.UTF_8);
    System.out.println("✨ パイプライン結果: message部分のみがHTTP送信され、レスポンスを受信");
    System.out.println("📋 HTTPレスポンス長: " + response.length() + " bytes");
    System.out.println();
  }

  /** シナリオ4: 異なるエンドポイントでのテスト */
  private static void demonstrateDifferentEndpoints() throws Exception {
    System.out.println("--- シナリオ4: 異なるエンドポイントでのテスト ---");

    // httpbin.org の異なるエンドポイントをテスト
    String[] endpoints = {
      "https://httpbin.org/post", "https://httpbin.org/put", "https://httpbin.org/patch"
    };

    String testData =
        """
        {
            "test": "endpoint-comparison",
            "timestamp": %d
        }
        """
            .formatted(System.currentTimeMillis());

    System.out.println("📤 共通送信データ: " + testData.trim());

    for (String endpoint : endpoints) {
      try {
        System.out.println("\n🔗 エンドポイント: " + endpoint);

        // SendHttpCommandは常にPOSTを使用するため、
        // 実際にはサーバー側でエンドポイントごとの処理の違いを確認
        SendHttpCommand httpCommand = new SendHttpCommand(endpoint);

        ByteArrayInputStream inputStream =
            new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        httpCommand.execute(inputStream, outputStream);

        String response = outputStream.toString(StandardCharsets.UTF_8);
        System.out.println("   ✅ 成功 - レスポンス長: " + response.length() + " bytes");

      } catch (Exception e) {
        System.out.println("   ❌ エラー: " + e.getMessage());
      }
    }
    System.out.println();
  }

  /** シナリオ5: エラーハンドリング */
  private static void demonstrateErrorHandling() throws Exception {
    System.out.println("--- シナリオ5: エラーハンドリング ---");

    // 存在しないエンドポイントへの送信テスト
    System.out.println("🧪 404エラーのテスト:");
    try {
      SendHttpCommand httpCommand = new SendHttpCommand("https://httpbin.org/status/404");

      ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      httpCommand.execute(inputStream, outputStream);
      System.out.println("   ❓ 予期せず成功しました");

    } catch (Exception e) {
      System.out.println("   ✅ 期待通りエラー: " + e.getMessage());
    }

    // 500エラーのテスト
    System.out.println("\n🧪 500エラーのテスト:");
    try {
      SendHttpCommand httpCommand = new SendHttpCommand("https://httpbin.org/status/500");

      ByteArrayInputStream inputStream = new ByteArrayInputStream("test".getBytes());
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      httpCommand.execute(inputStream, outputStream);
      System.out.println("   ❓ 予期せず成功しました");

    } catch (Exception e) {
      System.out.println("   ✅ 期待通りエラー: " + e.getMessage());
    }

    System.out.println("\n💡 SendHttpCommandは適切にHTTPエラーステータスを検出し、");
    System.out.println("   IOExceptionとして報告します。");
  }

  /** JSONレスポンスから特定のフィールドを抽出する簡単なユーティリティ */
  private static String extractJsonField(String json, String fieldName) {
    try {
      // 簡単なJSONパース（実用的ではないが、デモ用）
      String searchPattern = "\"" + fieldName + "\":";
      int startIndex = json.indexOf(searchPattern);
      if (startIndex == -1) {
        return "(フィールドが見つかりません)";
      }

      startIndex += searchPattern.length();

      // 値の開始位置を見つける
      while (startIndex < json.length()
          && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\t')) {
        startIndex++;
      }

      if (startIndex >= json.length()) {
        return "(値が見つかりません)";
      }

      // 文字列値の場合
      if (json.charAt(startIndex) == '"') {
        startIndex++; // 開始の引用符をスキップ
        int endIndex = json.indexOf('"', startIndex);
        if (endIndex == -1) {
          return "(終了引用符が見つかりません)";
        }
        return json.substring(startIndex, endIndex);
      }

      // 数値やbooleanの場合
      int endIndex = startIndex;
      while (endIndex < json.length()
          && json.charAt(endIndex) != ','
          && json.charAt(endIndex) != '}'
          && json.charAt(endIndex) != ']'
          && json.charAt(endIndex) != '\n') {
        endIndex++;
      }

      return json.substring(startIndex, endIndex).trim();

    } catch (Exception e) {
      return "(パースエラー: " + e.getMessage() + ")";
    }
  }
}
