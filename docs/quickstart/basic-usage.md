# StreamConverter クイックスタート

このガイドでは、StreamConverterの基本的な使用方法を説明します。

## 前提条件

- Java 17 以上
- Gradle 8.0 以上（プロジェクトに含まれています）

## 基本的な使用例

### 1. シンプルなCSVデータ抽出

```java
import com.streamConverter.StreamConverter;
import com.streamConverter.command.impl.csv.CsvNavigateCommand;

// CSVファイルから特定の列を抽出
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("name")  // "name"列を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### 2. JSON データの変換

```java
import com.streamConverter.command.impl.json.JsonNavigateCommand;

// JSONから特定のパスの値を抽出
IStreamCommand[] pipeline = {
    new JsonNavigateCommand("$.user.email")  // JSONPathで値を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### 3. パイプライン処理

```java
// 複数の処理を連結
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),           // CSV から製品名を抽出
    new SendHttpCommand("http://api.example.com"),   // API に送信
    new JsonNavigateCommand("$.result")              // レスポンスから結果を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);
```

## コンテキスト機能（推奨）

### 実行追跡とログ連携

```java
import com.streamConverter.context.ExecutionContext;

// 実行コンテキストを作成
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

// コンテキスト付きで実行
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);

// 実行結果の確認
for (CommandResult result : results) {
    System.out.println("Command: " + result.getCommandName() +
                      ", Duration: " + result.getDurationMs() + "ms" +
                      ", Success: " + result.isSuccess());
}
```

### MDC（Mapped Diagnostic Context）の利点

- **トレーサビリティ**: リクエストIDでログを追跡
- **パフォーマンス測定**: 各コマンドの実行時間を自動記録
- **エラー追跡**: 実行コンテキスト付きエラー情報
- **マルチスレッド対応**: スレッド間でのコンテキスト伝播

## 実行方法

### プロジェクトのビルド

```bash
# プロジェクトをビルド
./gradlew build

# サンプルコードの実行
./gradlew runQuickStart
```

### サンプルコードの場所

- `streamconverter-examples/src/main/java/com/streamConverter/examples/QuickStart.java`
- `streamconverter-examples/src/main/java/com/streamConverter/examples/AutoLoggingDemo.java`
- `streamconverter-examples/src/main/java/com/streamConverter/examples/ContextPropagationDemo.java`

## 利用可能なコマンド概要

| カテゴリ | コマンド | 用途 |
|----------|----------|------|
| **データ抽出** | `CsvNavigateCommand` | CSV特定列の抽出 |
| | `JsonNavigateCommand` | JSON特定パスの抽出 |
| | `XmlNavigateCommand` | XML特定要素の抽出 |
| **データ変換** | `CharacterConvertCommand` | 文字エンコーディング変換 |
| | `LineEndingNormalizeCommand` | 改行コード正規化 |
| **通信** | `SendHttpCommand` | HTTP API呼び出し |
| **検証** | `CsvValidateCommand` | CSV構造検証 |
| | `JsonValidateCommand` | JSONスキーマ検証 |
| | `ValidateCommand` | XMLスキーマ検証 |

詳細なコマンド一覧は [コマンドカタログ](../features/RULES_CATALOG.md) を参照してください。

## エラーハンドリング

```java
try {
    List<CommandResult> results = converter.run(inputStream, outputStream);

    // 実行結果をチェック
    for (CommandResult result : results) {
        if (!result.isSuccess()) {
            System.err.println("Command failed: " + result.getCommandName());
            System.err.println("Error: " + result.getErrorMessage());
        }
    }
} catch (Exception e) {
    System.err.println("Pipeline execution failed: " + e.getMessage());
    e.printStackTrace();
}
```

## 次のステップ

1. **[システムアーキテクチャ](../handbook/architecture.md)** - 設計思想を理解
2. **[Web API](../handbook/web-api.md)** - REST APIとしての使用方法
3. **[ログ機能](../handbook/logging.md)** - 詳細なログ設定
4. **[Docker化](../deployment/docker.md)** - 環境一貫性の確保

## トラブルシューティング

### よくある問題

**メモリ不足エラー**
```bash
# JVMヒープサイズを増加
export JAVA_OPTS="-Xms1g -Xmx2g"
./gradlew runQuickStart
```

**文字エンコーディング問題**
```java
// 明示的に文字エンコーディングを指定
new CharacterConvertCommand("UTF-8", "Shift_JIS")
```

**HTTP接続エラー**
```java
// タイムアウト設定付きHTTPコマンド
new SendHttpCommand("http://api.example.com", 30000) // 30秒タイムアウト
```

## サポート

- [GitHub Issues](https://github.com/3211133/StreamConverter/issues) - バグ報告や機能要求
- [ドキュメント索引](../INDEX.md) - 詳細なドキュメント一覧
- [コントリビューションガイド](../../CONTRIBUTING.md) - 開発参加方法