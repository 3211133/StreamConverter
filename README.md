# StreamConverter

**Version**: 0.0.0（開発中） &nbsp;|&nbsp; Java 21 &nbsp;|&nbsp; [📚 完全なドキュメント一覧](docs/INDEX.md) &nbsp;|&nbsp; [📖 クイックスタート](docs/README.md) &nbsp;|&nbsp; [🔗 API Javadoc](https://3211133.github.io/StreamConverter/)

> 💡 **Version Info**: Authoritative version is defined in [build.gradle.kts](build.gradle.kts) (search for `version = "`). The version displayed above is for visibility and requires manual update when releasing. See [VERSION_MANAGEMENT.md](docs/reference/VERSION_MANAGEMENT.md) for version history and policy.

大容量ファイルをストリームで処理するための Java ライブラリ/ツールキットです。パイプライン化された `IStreamCommand` を組み合わせて、
文字コード変換・CSV/JSON/XML ナビゲーション・HTTP 連携・バリデーションなどをメモリ効率良く実行できます。

---

## 🧱 モジュール構成

| モジュール | 役割 |
| --- | --- |
| `streamconverter-core` | パイプラインエンジンとコマンド実装、`StreamConverter` / `ExecutionContext` / ルール API | 
| `streamconverter-web` | Spring Boot 製の REST API ラッパー。CSV/JSON 抽出や任意パイプライン実行エンドポイントを提供 | 
| `streamconverter-examples` | サンプルコードと `runQuickStart` / `runMDC` などの実行タスク | 
| `streamconverter-tools` | 実験・解析ユーティリティ（Gradle タスク経由で利用） |

## ✨ 主な機能

### 🔄 ストリーミングパイプライン
- `StreamConverter` は入力ストリームをパイプ経由で渡し、各コマンドを並列最適化されたスレッドで実行します。
- `CSVPath` / `TreePath` による型安全なパス指定と `IRule` 実装で、ナビゲーション＋変換を一度に表現できます。

### 📊 観測性とコンテキスト伝播
- `CommandResult` に各コマンドの成功可否・実行時間・入出力バイト数を集約。
- `ExecutionContext` がリクエスト ID やユーザー情報を MDC に自動連携し、マルチスレッドでもログトレースを維持します。

### 🧰 コマンドカタログ
- CSV/JSON/XML ナビゲーション、ラインエンディング正規化、文字コード変換、HTTP 送信、スキーマ検証などを同梱。
- `LowerCaseRule` や `ChainRule` などのルールを組み合わせてフィールド単位の変換を定義可能。

---

## 🚀 クイックスタート

### 1. CSV 列を加工する最小構成
```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.IStreamCommand;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.string.LowerCaseRule;
import com.streamconverter.path.CSVPath;

IStreamCommand[] pipeline = {
    CsvNavigateCommand.create(new CSVPath("name"), new LowerCaseRule())
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### 2. 実行コンテキストと結果メトリクス
```java
import java.util.List;

import com.streamconverter.CommandResult;
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.json.JsonNavigateCommand;
import com.streamconverter.command.impl.SendHttpCommand;
import com.streamconverter.command.rule.PassThroughRule;
import com.streamconverter.context.ExecutionContext;
import com.streamconverter.path.TreePath;

ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .userContext("operator", "batch-service")
    .build();

StreamConverter converter = StreamConverter.createWithContext(
    context,
    JsonNavigateCommand.create(TreePath.fromJson("$.result"), new PassThroughRule()),
    new SendHttpCommand("https://api.example.com/ingest"));

List<CommandResult> results = converter.run(inputStream, outputStream);
results.forEach(result ->
    LOG.info("{}: {} ms", result.getCommandName(), result.getExecutionTimeMillis()));
```

> 詳細なハンズオンは [docs/quickstart/basic-usage.md](docs/quickstart/basic-usage.md) を参照してください。

---

## 📋 利用可能な主なコマンド

| カテゴリ | クラス | 概要 |
| --- | --- | --- |
| CSV | `CsvNavigateCommand`, `CsvFilterCommand`, `CsvValidateCommand` | 列変換・行フィルタ・構造検証 |
| JSON | `JsonNavigateCommand`, `JsonFilterCommand`, `JsonValidateCommand`, `JsonStreamingValidateCommand` | JSONPath 変換・フィルタ・スキーマ検証 |
| XML | `XmlNavigateCommand`, `XmlFilterCommand`, `ConvertCommand`, `ValidateCommand` | XPath ナビゲーションと XSD 検証 |
| 文字列 | `LineEndingNormalizeCommand`, `CharacterConvertCommand`, `SampleStreamCommand` | 改行正規化・エンコーディング変換・テスト用テンプレート |
| 通信 | `SendHttpCommand` | 外部 HTTP API へのストリーム送信（安全な URL チェック付き） |

---

## 📚 ドキュメント
- [INDEX.md](docs/INDEX.md): 対象者別の完全なドキュメント一覧
- [docs/README.md](docs/README.md): ハイライトとナビゲーションガイド
- [handbook/README.md](docs/handbook/README.md): What/Why/How ベースの機能ガイド
- [handbook/logging.md](docs/handbook/logging.md): MDC と自動ロギング
- [handbook/validation.md](docs/handbook/validation.md): CSV/JSON/XML 検証
- [handbook/web-api.md](docs/handbook/web-api.md): REST API の利用手順
- [reference/TESTING.md](docs/reference/TESTING.md): テスト戦略とカバレッジ

---

## 💡 サンプルコード

| 例 | 説明 |
| --- | --- |
| [QuickStart.java](streamconverter-examples/src/main/java/com/streamconverter/examples/QuickStart.java) | 最小限のパイプライン実装 |
| [StreamConverterMDCDemo.java](streamconverter-examples/src/main/java/com/streamconverter/examples/StreamConverterMDCDemo.java) | MDC 連携とマルチスレッドデモ |
| [ComplexPipelineExample.java](streamconverter-examples/src/main/java/com/streamconverter/examples/ComplexPipelineExample.java) | 多段変換パイプライン |
| [ValidationExample.java](streamconverter-examples/src/main/java/com/streamconverter/examples/ValidationExample.java) | CSV/JSON/XML のバリデーション統合 |
| [DatabaseRuleDemo.java](streamconverter-examples/src/main/java/com/streamconverter/examples/DatabaseRuleDemo.java) | ルールチェーンと外部依存デモ |
| [PerformanceOptimizationExamples.java](streamconverter-examples/src/main/java/com/streamconverter/examples/PerformanceOptimizationExamples.java) | 大規模データでの最適化テクニック |

---

## 🛠️ ビルド & 実行
```bash
# 全モジュールのビルド（Spotless/テスト/静的解析込み）
./gradlew build

# コアモジュールのテストのみ
./gradlew :streamconverter-core:test

# Web API を起動
./gradlew :streamconverter-web:bootRun

# サンプルコードを実行
./gradlew :streamconverter-examples:runQuickStart
./gradlew :streamconverter-examples:runMDC

# ドキュメント・ベンチマーク
./gradlew javadocAll
./gradlew benchmarkAll
```

---

## 🧑‍💻 開発メモ
- 推奨 JDK: 21（Gradle Toolchain 対応済み）
- コードスタイル: Spotless + Google Java Format（`./gradlew spotlessApply`）
- 詳細なセットアップ手順は [docs/development/CONFIGURATION.md](docs/development/CONFIGURATION.md) と [docs/guides/PRE_COMMIT_SETUP.md](docs/guides/PRE_COMMIT_SETUP.md) を参照してください。

---

## 📄 ライセンス

本プロジェクトはリポジトリ内の [LICENSE](LICENSE) に従います。
- **[GitHub Issues](https://github.com/3211133/StreamConverter/issues)** - バグ報告・機能要求
- **[セキュリティポリシー](SECURITY.md)** - 脆弱性報告
- **[ドキュメントハブ](docs/README.md)** - 詳細な技術情報

---

**StreamConverter v0.0.0** - 効率的なストリーム処理ライブラリ（開発中）
