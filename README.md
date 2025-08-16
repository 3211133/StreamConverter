# StreamConverter

**Version**: 1.2.0 | [📚 Documentation](docs/) | [🔗 Javadoc](https://3211133.github.io/StreamConverter/)

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
- **コンテキスト伝播**: マルチスレッド環境でのMDCコンテキスト管理

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

### コンテキスト対応処理

```java
import com.streamConverter.StreamConverter;
import com.streamConverter.context.ExecutionContext;

// カスタムコンテキストで実行追跡
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

StreamConverter converter = StreamConverter.createWithContext(
    context, csvCommand, httpCommand, jsonCommand
);
converter.run(inputStream, outputStream);
```

## 📋 利用可能なコマンド

| カテゴリ | コマンド | 用途 | 使用例 |
|----------|----------|------|--------|
| **データ変換** | `CsvNavigateCommand` | CSV 特定列の変換 | `new CsvNavigateCommand("name")` |
| | `JsonNavigateCommand` | JSON 特定パスの変換 | `new JsonNavigateCommand("$.user.id")` |
| | `XmlNavigateCommand` | XML 特定要素の変換 | `new XmlNavigateCommand("//item/@id")` |
| | `CharacterConvertCommand` | 文字エンコーディング変換 | `new CharacterConvertCommand("UTF-8", "Shift_JIS")` |
| | `LineEndingNormalizeCommand` | 改行コード正規化 | `new LineEndingNormalizeCommand(LineEndingType.UNIX)` |
| | `xml.ConvertCommand` | XSLT 変換 | `new ConvertCommand("style.xsl")` |
| **フィルタリング** | `CsvFilterCommand` | CSV 行フィルタリング | `new CsvFilterCommand(predicate)` |
| | `JsonFilterCommand` | JSON 要素フィルタリング | `new JsonFilterCommand(jsonPath, predicate)` |
| | `XmlFilterCommand` | XML 要素フィルタリング | `new XmlFilterCommand(xpath, predicate)` |
| **通信** | `SendHttpCommand` | HTTP リクエスト | `new SendHttpCommand("http://api.example.com")` |
| **検証** | `CsvValidateCommand` | CSV 構造検証 | `new CsvValidateCommand(requiredColumns)` |
| | `JsonValidateCommand` | JSON スキーマ検証 | `new JsonValidateCommand("schema.json")` |
| | `xml.ValidateCommand` | XML スキーマ検証 | `new ValidateCommand("schema.xsd")` |

## 📖 詳細ドキュメント

- **[📚 ドキュメント一覧](docs/)** - 全ドキュメントのインデックス
- **[🏗️ システムアーキテクチャ](docs/ARCHITECTURE.md)** - StreamConverter全体アーキテクチャと設計原則
- **[🔧 コマンドアーキテクチャ](docs/COMMAND_ARCHITECTURE.md)** - コマンドパターンと拡張方法
- **[📝 自動ログ機能](docs/AUTO_LOGGING.md)** - ログ機能の詳細と設定
- **[🔗 コンテキスト伝播](docs/reports/CONTEXT_PROPAGATION_ARCHITECTURE.md)** - マルチスレッド環境でのMDC管理
- **[🔢 バージョン管理](docs/VERSION_MANAGEMENT.md)** - サポートバージョンとポリシー
- **[🛡️ セキュリティ](SECURITY.md)** - セキュリティポリシーと脆弱性報告

## 🎯 使用例

詳細な使用例は以下のサンプルコードを参照してください：

- **[QuickStart.java](streamconverter-examples/src/main/java/com/streamConverter/QuickStart.java)** - 基本的な使用方法
- **[AutoLoggingDemo.java](streamconverter-examples/src/main/java/com/streamConverter/AutoLoggingDemo.java)** - ログ機能のデモ
- **[ContextPropagationDemo.java](streamconverter-examples/src/main/java/com/streamConverter/ContextPropagationDemo.java)** - コンテキスト伝播のデモ
- **[MDCMultiThreadExample.java](streamconverter-examples/src/main/java/com/streamConverter/MDCMultiThreadExample.java)** - MDCマルチスレッド検証
- **[DataProcessingExamples.java](streamconverter-examples/src/main/java/com/streamConverter/DataProcessingExamples.java)** - 実用的な処理例
- **[EnterpriseIntegrationPatterns.java](streamconverter-examples/src/main/java/com/streamConverter/EnterpriseIntegrationPatterns.java)** - エンタープライズパターン

## 🛠️ ビルドとテスト

```bash
# ビルド
./gradlew build

# テスト実行
./gradlew test

# ベンチマークテスト
./gradlew benchmarkAll

# 統合Javadoc生成
./gradlew javadocAll

# 詳細なテスト戦略とガイドは docs/TESTING.md を参照

# コードスタイル適用
./gradlew spotlessApply

# サンプル実行
./gradlew runQuickStart
./gradlew runAutoLoggingDemo
./gradlew runContextDemo
./gradlew runMDC
```

## 開発ガイドライン

### コミットルール

このプロジェクトでは [Conventional Commits](https://www.conventionalcommits.org/) 仕様に従ったコミットメッセージを使用しています。詳細は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。

### コードスタイル
このプロジェクトでは [Spotless](https://github.com/diffplug/spotless) を使用してコードスタイルを統一しています。コードスタイルを適用するには：

```bash
./gradlew spotlessApply
```

### ドキュメント生成
Javadocは全モジュールを統合して生成されます：

```bash
# 統合Javadoc生成（全モジュール）
./gradlew javadocAll

# 生成されたドキュメント
open build/docs/javadoc/index.html
```

**オンライン版**: [📖 API Documentation](https://3211133.github.io/StreamConverter/)

- 🔄 **自動更新**: developブランチへのpush時に自動的にGitHub Pagesで更新
- 🗂️ **統合表示**: 全モジュール（core、web、examples、tools）のAPIを統一表示
- 🚫 **競合防止**: 生成ファイルはGitの追跡対象外でマージ競合を回避

## 📚 詳細ドキュメント

プロジェクトの詳細な情報は [`docs/`](docs/) ディレクトリにあります：

- **[テスト戦略とガイド](docs/TESTING.md)** - 包括的なテスト実行方法とベンチマーク
- **[システムアーキテクチャ](docs/ARCHITECTURE.md)** - StreamConverter全体の設計思想と4層アーキテクチャ
- **[コマンドアーキテクチャ](docs/COMMAND_ARCHITECTURE.md)** - コマンドパターンとController層の設計
- **[セキュリティ分析](docs/SECURITY_ANALYSIS.md)** - セキュリティ対策と脆弱性分析
- **[ベンチマーク実装](docs/BENCHMARK_IMPLEMENTATION.md)** - 大容量データ処理のパフォーマンス測定
- **[自動ログ機能](docs/AUTO_LOGGING.md)** - MDCとコンテキスト伝播の詳細

開発に参加する場合は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。
