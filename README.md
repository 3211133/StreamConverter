# StreamConverter

Java 21 &nbsp;|&nbsp; [📚 完全なドキュメント一覧](docs/INDEX.md) &nbsp;|&nbsp; [📖 クイックスタート](docs/README.md) &nbsp;|&nbsp; [🔗 API Javadoc](https://3211133.github.io/StreamConverter/)

大容量ファイルをストリームで処理するための Java ライブラリ/ツールキットです。パイプライン化された `IStreamCommand` を組み合わせて、
文字コード変換・CSV/JSON/XML ナビゲーション・バリデーションなどをメモリ効率良く実行できます。

---

## 🧱 モジュール構成

| モジュール | 役割 |
| --- | --- |
| `streamconverter-core` | パイプラインエンジンと主要コマンド実装（`StreamConverter` / `PipelineContext` / Rule API） |
| `streamconverter-http` | HTTP 連携コマンド実装 |
| `streamconverter-db` | DB ルール連携・拡張 |
| `streamconverter-web` | Spring Boot 製の REST API ラッパー |
| `streamconverter-examples` | サンプルコードと実行タスク |
| `streamconverter-tools` | 実験・解析ユーティリティ |

## ✨ 主な機能

### 🔄 ストリーミングパイプライン
- `StreamConverter` は入力ストリームをパイプ経由で渡し、各コマンドを非同期実行します。
- `StreamConverter.run(...)` は `void` を返し、結果は出力ストリームとログで確認します。

### 📊 観測性とコンテキスト伝播
- `PipelineContext` によるパイプライン内の共有値連携に対応。
- `MdcPropagatingRule` と `PipelineContextTurboFilter` を組み合わせると、抽出値を MDC へ自動反映できます。

---

## 🚀 クイックスタート

```java
import com.streamconverter.StreamConverter;
import com.streamconverter.command.impl.csv.CsvNavigateCommand;
import com.streamconverter.command.rule.impl.string.TrimRule;
import com.streamconverter.path.CSVPath;

StreamConverter converter = StreamConverter.create(
    CsvNavigateCommand.create(CSVPath.fromHeaderName("name"), new TrimRule()));

converter.run(inputStream, outputStream);
```

> 詳細なハンズオンは [docs/quickstart/basic-usage.md](docs/quickstart/basic-usage.md) を参照してください。

---

## 📋 利用可能な主なコマンド

| カテゴリ | クラス | 概要 |
| --- | --- | --- |
| CSV | `CsvNavigateCommand`, `CsvFilterCommand`, `CsvValidateCommand` | 列変換・行フィルタ・構造検証 |
| JSON | `JsonNavigateCommand`, `JsonFilterCommand` | JSONPath 変換・フィルタ |
| XML | `XmlNavigateCommand`, `XmlFilterCommand`, `ConvertCommand`, `ValidateCommand` | XPath ナビゲーションと XSD 検証 |
| 文字列 | `LineEndingNormalizeCommand`, `CharacterConvertCommand` | 改行正規化・エンコーディング変換 |

---

## 🛠️ ビルド & 実行
```bash
# 全モジュールのビルド
./gradlew build

# コアモジュールのテストのみ
./gradlew :streamconverter-core:test

# Web API を起動
./gradlew :streamconverter-web:bootRun

# サンプルコードを実行
./gradlew :streamconverter-examples:runQuickStart
./gradlew :streamconverter-examples:runMDC
```

---

## 📚 ドキュメント
- [INDEX.md](docs/INDEX.md): 対象者別の完全なドキュメント一覧
- [docs/README.md](docs/README.md): ハイライトとナビゲーションガイド
- [handbook/README.md](docs/handbook/README.md): What/Why/How ベースの機能ガイド
- [handbook/logging.md](docs/handbook/logging.md): MDC と自動ロギング
- [handbook/validation.md](docs/handbook/validation.md): CSV/XML バリデーション
- [handbook/web-api.md](docs/handbook/web-api.md): REST API の利用手順

---

## 📄 ライセンス

本プロジェクトはリポジトリ内の [LICENSE](LICENSE) に従います。

- **[GitHub Issues](https://github.com/3211133/StreamConverter/issues)** - バグ報告・機能要求
- **[セキュリティポリシー](SECURITY.md)** - 脆弱性報告
