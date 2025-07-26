# StreamConverter

**Version**: 1.0.0-SNAPSHOT | [📚 Documentation](docs/) | [🔗 Javadoc](https://3211133.github.io/StreamConverter/javadoc/)

大容量ファイルのストリーム処理を効率的に行うためのJavaライブラリです。メモリ使用量を抑えながら、複数の処理を連結するパイプライン型アーキテクチャを提供します。

## ✨ 主要機能

### 🔄 パイプライン処理
- **コマンドパターン**: 処理単位をコマンドとして分離し、柔軟な組み合わせが可能
- **メモリ効率**: ストリーミング処理により大容量ファイルも少ないメモリで処理
- **並行処理**: 複数コマンドを並行実行してスループットを向上

### 📊 自動ログ機能
- **統合ログ**: 全コマンドの実行状況を自動記録
- **パフォーマンス測定**: 実行時間とメモリ使用量の自動追跡
- **エラー追跡**: 詳細なスタックトレースと実行コンテキスト

### 🔧 豊富なコマンド
- **データ抽出**: CSV、JSON、XMLからの値抽出
- **形式変換**: 文字エンコーディング、XML変換
- **通信**: HTTP API呼び出し
- **バリデーション**: XMLスキーマ検証

## 🚀 クイックスタート

### 基本的な使用方法

```java
import com.streamConverter.StreamConverter;
import com.streamConverter.command.impl.*;

// CSV → HTTP API → JSON 処理パイプライン
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),      // CSV から製品名を抽出
    new SendHttpCommand("http://api.example.com"), // API に送信
    new JsonNavigateCommand("$.result")         // レスポンスから結果を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### 自動ログ機能付き

```java
import com.streamConverter.command.CommandFactory;

// ログ機能を自動追加
IStreamCommand loggedCommand = CommandFactory.createWithLogging(
    CsvNavigateCommand.class, "productName"
);

StreamConverter converter = StreamConverter.create(loggedCommand);
```

## 📋 利用可能なコマンド

| カテゴリ | コマンド | 用途 | 使用例 |
|----------|----------|------|--------|
| **データ抽出** | `CsvNavigateCommand` | CSV フィールド抽出 | `new CsvNavigateCommand("name")` |
| | `JsonNavigateCommand` | JSON パス抽出 | `new JsonNavigateCommand("$.user.id")` |
| | `XmlNavigateCommand` | XPath 抽出 | `new XmlNavigateCommand("//item/@id")` |
| **変換** | `convert` | 文字エンコーディング変換 | `new convert("UTF-8", "Shift_JIS")` |
| | `xml.ConvertCommand` | XSLT 変換 | `new ConvertCommand("style.xsl")` |
| **通信** | `SendHttpCommand` | HTTP リクエスト | `new SendHttpCommand("http://api.example.com")` |
| **検証** | `xml.ValidateCommand` | XML スキーマ検証 | `new ValidateCommand("schema.xsd")` |

## 📖 詳細ドキュメント

- **[📚 ドキュメント一覧](docs/)** - 全ドキュメントのインデックス
- **[🏗️ コマンドアーキテクチャ](docs/COMMAND_ARCHITECTURE.md)** - 設計思想と拡張方法
- **[📝 自動ログ機能](docs/AUTO_LOGGING.md)** - ログ機能の詳細と設定
- **[🔢 バージョン管理](docs/VERSION_MANAGEMENT.md)** - サポートバージョンとポリシー
- **[🛡️ セキュリティ](SECURITY.md)** - セキュリティポリシーと脆弱性報告

## 🎯 使用例

詳細な使用例は以下のサンプルコードを参照してください：

- **[QuickStart.java](src/main/java/com/streamConverter/examples/QuickStart.java)** - 基本的な使用方法
- **[AutoLoggingDemo.java](src/main/java/com/streamConverter/examples/AutoLoggingDemo.java)** - ログ機能のデモ
- **[DataProcessingExamples.java](src/main/java/com/streamConverter/examples/DataProcessingExamples.java)** - 実用的な処理例
- **[EnterpriseIntegrationPatterns.java](src/main/java/com/streamConverter/examples/EnterpriseIntegrationPatterns.java)** - エンタープライズパターン

## 🛠️ ビルドとテスト

```bash
# ビルド
./gradlew build

# テスト実行
./gradlew test

# コードスタイル適用
./gradlew spotlessApply

# サンプル実行
./gradlew runQuickStart
./gradlew runAutoLoggingDemo
```

## 開発ガイドライン

### コミットルール

このプロジェクトでは [Conventional Commits](https://www.conventionalcommits.org/) 仕様に従ったコミットメッセージを使用しています。詳細は [CONTRIBUTING.md](doc/CONTRIBUTING.md) を参照してください。

### コードスタイル
このプロジェクトでは [Spotless](https://github.com/diffplug/spotless) を使用してコードスタイルを統一しています。コードスタイルを適用するには：

```bash
./gradlew spotlessApply
```
