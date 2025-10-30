# StreamConverter

**Version**: 0.0.0（開発中） | [📚 ドキュメントハブ](docs/README.md) | [🚀 クイックスタート](docs/quickstart/basic-usage.md) | [🔗 API Documentation](https://3211133.github.io/StreamConverter/)
大容量ファイルのストリーム処理を効率的に行うためのJavaライブラリです。メモリ使用量を抑えながら、複数の処理を連結するパイプライン型アーキテクチャを提供します。

> **ドキュメント優先**: このプロジェクトでは、ドキュメントを真実のソース (Single Source of Truth) として扱います。詳細は [📚 ドキュメントハブ](docs/README.md) を参照してください。

## ✨ 主要機能

### 🔄 パイプライン処理
- **コマンドパターン**: 処理単位をコマンドとして分離し、柔軟な組み合わせが可能
- **メモリ効率**: ストリーミング処理により大容量ファイルも少ないメモリで処理
- **並行処理**: 複数コマンドを並行実行してスループットを向上

### 📊 自動ログ機能
- **統合ログ**: 全コマンドの実行状況を自動記録
- **パフォーマンス測定**: 実行時間とメモリ使用量の自動追跡
- **エラー追跡**: 詳細なスタックトレースと実行コンテキスト
- **コンテキスト伝播（実験的）**: `ExecutionContext` を介したMDC同期機能

### 🔧 豊富なコマンド
- **データ変換**: CSV、JSON、XMLの特定のタグの中身を変換
- **形式変換**: 文字エンコーディング、XML変換
- **通信**: HTTP API呼び出し
- **バリデーション**: XMLスキーマ検証

## 🚀 クイックスタート

### 基本的な使用方法
```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.SampleStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.path.CSVPath;
import java.util.Locale;

// ルールはIRuleを実装する必要があります（ラムダでの指定も可能）
IRule uppercaseRule = value -> value == null ? null : value.toUpperCase(Locale.ROOT);

IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("productName"), uppercaseRule),
    new SampleStreamCommand("audit")
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### コンテキスト対応処理（MDC連携 / 実験的）

```java
import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.SampleStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.IRule;
import com.streamconverter.context.ExecutionContext;
import com.streamconverter.path.CSVPath;
import java.util.List;
import java.util.Locale;

ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user-789")
    .build();

IRule uppercaseRule = value -> value == null ? null : value.toUpperCase(Locale.ROOT);

IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("productName"), uppercaseRule),
    new SampleStreamCommand("audit")
};

StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);

for (CommandResult result : results) {
    System.out.printf(
        "Command=%s duration=%dms success=%s%n",
        result.getCommandName(), result.getDurationMs(), result.isSuccess());
}
```

> ℹ️ `ExecutionContext` によるMDC同期は現在実験的です。マルチスレッド環境ではスレッド境界をまたぐ利用に制約があります。

## 📋 利用可能なコマンド
| カテゴリ | コマンド | 用途 | 使用例 |
|----------|----------|------|--------|
| **データ変換** | `csv.CsvNavigateCommand` | CSV 特定列の変換 | `CsvNavigateCommand.create(new CSVPath("name"), rule)` |
| | `json.JsonNavigateCommand` | JSON 特定パスの変換 | `JsonNavigateCommand.create(TreePath.fromJson("$.user.id"), rule)` |
| | `xml.XmlNavigateCommand` | XML 特定要素の変換 | `XmlNavigateCommand.create(TreePath.fromXml("person/name"), rule)` |
| | `CharacterConvertCommand` | 文字エンコーディング変換 | `new CharacterConvertCommand("UTF-8", "Shift_JIS")` |
| | `LineEndingNormalizeCommand` | 改行コード正規化 | `new LineEndingNormalizeCommand(LineEndingType.UNIX)` |
| | `xml.ConvertCommand` | XSLT 変換 | `new ConvertCommand(rule, "style.xsl")` |
| **フィルタリング** | `csv.CsvFilterCommand` | CSV 行フィルタリング | `new CsvFilterCommand("columnName", true)` |
| | `json.JsonFilterCommand` | JSON 要素フィルタリング | `new JsonFilterCommand("$.path")` |
| | `xml.XmlFilterCommand` | XML 要素フィルタリング | `new XmlFilterCommand("//element/path")` |
| **通信** | `SendHttpCommand` | HTTP リクエスト | `new SendHttpCommand("http://api.example.com")` |
| **検証** | `csv.CsvValidateCommand` | CSV 構造検証 | `new CsvValidateCommand(requiredColumns)` |
| | `json.JsonValidateCommand` | JSON スキーマ検証 | `JsonValidateCommand.create("schema.json")` |
| | `json.JsonStreamingValidateCommand` | JSON ストリーミング検証 | `JsonStreamingValidateCommand.create("schema.json")` |
| | `xml.ValidateCommand` | XML スキーマ検証 | `new ValidateCommand("schema.xsd")` |

## 📖 ドキュメント

| 分類 | ドキュメント | 説明 |
|------|-------------|------|
| **🚀 はじめに** | [クイックスタート](docs/quickstart/basic-usage.md) | 基本的な使用方法 |
| | [ドキュメントハブ](docs/README.md) | ドキュメント全体の案内 |
| **🏗️ アーキテクチャ** | [システム設計](docs/ARCHITECTURE.md) | 4層アーキテクチャと設計原則 |
| | [アーキテクチャ図](docs/ARCHITECTURE_DIAGRAMS.md) | UMLとクラス関係図 |
| **🛠️ 機能ガイド** | [Web API](docs/handbook/web-api.md) | REST API の使用方法 |
| | [ログ機能](docs/handbook/logging.md) | MDC連携とコンテキスト伝播 |
| | [バリデーション](docs/handbook/validation.md) | データ検証機能 |
| **🚀 運用** | Docker環境ガイド（準備中） | コンテナ対応は現在計画中 |
| | [セキュリティ](SECURITY.md) | セキュリティポリシー |

## 🎯 使用例

詳細な使用例は以下のサンプルコードを参照してください：

- **[QuickStart.java](streamconverter-examples/src/main/java/com/streamconverter/examples/QuickStart.java)** - 基本的な使用方法
- **[StreamConverterMDCDemo.java](streamconverter-examples/src/main/java/com/streamconverter/examples/StreamConverterMDCDemo.java)** - ExecutionContext と MDC のサンプル
- **[ComplexPipelineExample.java](streamconverter-examples/src/main/java/com/streamconverter/examples/ComplexPipelineExample.java)** - パイプライン構成の応用例
- **[DatabaseRuleDemo.java](streamconverter-examples/src/main/java/com/streamconverter/examples/DatabaseRuleDemo.java)** - 外部ルール連携のデモ
- **[PerformanceOptimizationExamples.java](streamconverter-examples/src/main/java/com/streamconverter/examples/PerformanceOptimizationExamples.java)** - パフォーマンスチューニング例
- **[ValidationExample.java](streamconverter-examples/src/main/java/com/streamconverter/examples/ValidationExample.java)** - 検証コマンドの使い方

## 🛠️ ビルドと実行

### 基本コマンド
```bash
# プロジェクトビルド
./gradlew build

# テスト実行
./gradlew test

# サンプル実行
./gradlew runQuickStart      # 基本的な使用例
./gradlew runMDC             # MDC/コンテキスト伝播デモ
```

### Docker環境（予定）

> ⚠️ **Dockerサポートは現在未対応です。将来的な提供に向けて準備中です。**

Docker向けタスクやイメージはまだ公開されていません。対応状況は [GitHub Issues](https://github.com/3211133/StreamConverter/issues) をご確認ください。

### その他
```bash
# コードスタイル適用
./gradlew spotlessApply

# API ドキュメント生成
./gradlew javadocAll

# ベンチマークテスト
./gradlew benchmarkAll
```

詳細は [テスト戦略ガイド](docs/reference/TESTING.md) を参照してください。コンテナ対応に関する情報は整備中です。

## 🔧 開発環境セットアップ

### 必要なシステム要件

- **Java**: 21 以上
- **Gradle**: プロジェクトに同梱されている Gradle Wrapper (`./gradlew`) を使用してください。
- **IDE**: IntelliJ IDEA、VS Code（Java 拡張機能）など任意のJava対応IDE

### 推奨セットアップ手順

1. リポジトリをクローンします。
2. 必要に応じて `JAVA_HOME` を Java 21 以上の JDK に設定します。
3. 依存関係のダウンロードとビルド確認のために `./gradlew build` を実行します。
4. コードスタイル調整は `./gradlew spotlessApply` を利用してください。

> 💡 Language Server や MCP などの高度なIDE連携は任意です。必要に応じて各IDEのガイドに従って設定してください。

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

## 🤝 コントリビューション

開発に参加する場合は以下を参照してください：

- **[コントリビューションガイド](CONTRIBUTING.md)** - 開発参加方法とルール
- **[開発環境セットアップ](docs/guides/PRE_COMMIT_SETUP.md)** - 開発環境の構築
- **[ブランチ戦略](docs/guides/BRANCH_STRATEGY.md)** - Git ワークフロー

## 📞 サポート

- **[GitHub Issues](https://github.com/3211133/StreamConverter/issues)** - バグ報告・機能要求
- **[セキュリティポリシー](SECURITY.md)** - 脆弱性報告
- **[ドキュメントハブ](docs/README.md)** - 詳細な技術情報

---

**StreamConverter v0.0.0** - 効率的なストリーム処理ライブラリ（開発中）
