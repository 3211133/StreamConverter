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
import com.streamConverter.command.impl.csv.CsvNavigateCommand;
import com.streamConverter.command.impl.SendHttpCommand;
import com.streamConverter.command.impl.json.JsonNavigateCommand;

// CSV → HTTP API → JSON 処理パイプライン
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),      // CSV から製品名を抽出
    new SendHttpCommand("http://api.example.com"), // API に送信
    new JsonNavigateCommand("$.result")         // レスポンスから結果を抽出
};

StreamConverter converter = StreamConverter.create(pipeline);
converter.run(inputStream, outputStream);
```

### コンテキスト対応処理（MDC連携）

```java
import com.streamConverter.StreamConverter;
import com.streamConverter.context.ExecutionContext;
import com.streamConverter.CommandResult;

// カスタムコンテキストで実行追跡
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .build();

// パイプライン定義
IStreamCommand[] pipeline = {
    new CsvNavigateCommand("productName"),
    new SendHttpCommand("http://api.example.com"),
    new JsonNavigateCommand("$.result")
};

// コンテキスト付きで実行（推奨方法）
StreamConverter converter = StreamConverter.createWithContext(context, pipeline);
List<CommandResult> results = converter.run(inputStream, outputStream);

// 実行結果の確認
for (CommandResult result : results) {
    System.out.println("Command: " + result.getCommandName() + 
                      ", Duration: " + result.getDurationMs() + "ms" +
                      ", Success: " + result.isSuccess());
}
```

**MDC機能の利点:**
- **トレーサビリティ**: マルチスレッド環境での実行追跡
- **ログ相関**: requestIdによるログの関連付け
- **パフォーマンス測定**: 各コマンドの実行時間とリソース使用量
- **エラー追跡**: 実行コンテキスト付きエラー情報

## 📋 利用可能なコマンド

| カテゴリ | コマンド | 用途 | 使用例 |
|----------|----------|------|--------|
| **データ変換** | `csv.CsvNavigateCommand` | CSV 特定列の変換 | `new CsvNavigateCommand("name")` |
| | `json.JsonNavigateCommand` | JSON 特定パスの変換 | `new JsonNavigateCommand("$.user.id")` |
| | `xml.XmlNavigateCommand` | XML 特定要素の変換（IRule 必須） | `new XmlNavigateCommand(new XPath("//item/@id"), rule)` |
| | `CharacterConvertCommand` | 文字エンコーディング変換 | `new CharacterConvertCommand("UTF-8", "Shift_JIS")` |
| | `LineEndingNormalizeCommand` | 改行コード正規化 | `new LineEndingNormalizeCommand(LineEndingType.UNIX)` |
| | `xml.ConvertCommand` | XSLT 変換 | `new ConvertCommand(rule, "style.xsl")` |
| **フィルタリング** | `csv.CsvFilterCommand` | CSV 行フィルタリング | `new CsvFilterCommand("columnName", true)` |
| | `json.JsonFilterCommand` | JSON 要素フィルタリング | `new JsonFilterCommand("$.path")` |
| | `xml.XmlFilterCommand` | XML 要素フィルタリング | `new XmlFilterCommand("//element/path")` |
| **通信** | `SendHttpCommand` | HTTP リクエスト | `new SendHttpCommand("http://api.example.com")` |
| **検証** | `csv.CsvValidateCommand` | CSV 構造検証 | `new CsvValidateCommand(requiredColumns)` |
| | `json.JsonValidateCommand` | JSON スキーマ検証 | `new JsonValidateCommand("schema.json")` |
| | `json.JsonStreamingValidateCommand` | JSON ストリーミング検証 | `new JsonStreamingValidateCommand("schema.json")` |
| | `xml.ValidateCommand` | XML スキーマ検証 | `new ValidateCommand("schema.xsd")` |

## 📖 詳細ドキュメント

- **[📚 ドキュメント一覧](docs/)** - 全ドキュメントのインデックス
- **[🏗️ システムアーキテクチャ](docs/ARCHITECTURE.md)** - StreamConverter全体アーキテクチャと設計原則
- **[🔧 コマンドアーキテクチャ](docs/ARCHITECTURE.md)** - コマンドパターンと拡張方法（旧ドキュメント: [`docs/archived/COMMAND_ARCHITECTURE.md`](docs/archived/COMMAND_ARCHITECTURE.md)）
- **[📝 自動ログ機能](docs/AUTO_LOGGING.md)** - ログ機能の詳細と設定
- **[🔗 コンテキスト伝播](docs/reports/CONTEXT_PROPAGATION_ARCHITECTURE.md)** - マルチスレッド環境でのMDC管理
- **[🔢 バージョン管理](docs/VERSION_MANAGEMENT.md)** - サポートバージョンとポリシー
- **[🛡️ セキュリティ](SECURITY.md)** - セキュリティポリシーと脆弱性報告

## 🎯 使用例

詳細な使用例は以下のサンプルコードを参照してください：

- **[QuickStart.java](streamconverter-examples/src/main/java/com/streamConverter/examples/QuickStart.java)** - 基本的な使用方法
- **[AutoLoggingDemo.java](streamconverter-examples/src/main/java/com/streamConverter/examples/AutoLoggingDemo.java)** - ログ機能のデモ
- **[ContextPropagationDemo.java](streamconverter-examples/src/main/java/com/streamConverter/examples/ContextPropagationDemo.java)** - コンテキスト伝播のデモ
- **[MDCMultiThreadExample.java](streamconverter-examples/src/main/java/com/streamConverter/examples/MDCMultiThreadExample.java)** - MDCマルチスレッド検証
- **[DataProcessingExamples.java](streamconverter-examples/src/main/java/com/streamConverter/examples/DataProcessingExamples.java)** - 実用的な処理例
- **[EnterpriseIntegrationPatterns.java](streamconverter-examples/src/main/java/com/streamConverter/examples/EnterpriseIntegrationPatterns.java)** - エンタープライズパターン

## 🛠️ ビルドとテスト

```bash
# ビルド
./gradlew build

# テスト実行（ベンチマークはデフォルトでスキップされます）
./gradlew test

# ベンチマークテスト（必要な場合のみ）
./gradlew benchmarkAll

# 統合Javadoc生成
./gradlew javadocAll

# 詳細なテスト戦略とガイドは docs/TESTING.md を参照

# コードスタイル適用
./gradlew spotlessApply

# サンプル実行
./gradlew runQuickStart      # 基本のCSV/JSON/XML + パイプラインの最短例
./gradlew runMDC             # MDC/コンテキスト伝播の代表例
# optional
./gradlew runComplexPipeline # 複合パイプライン（ネットワーク非依存の流れを擬似）
./gradlew runValidationExample # バリデーション系の使い方
./gradlew runDatabaseRuleDemo  # H2を使ったDBルール適用例
```

## 🔧 開発環境セットアップ

> **MCP (Model Context Protocol)** は、開発環境でコード補完やエラー検出などの高度なIDE機能を提供するためのプロトコルです。
### MCP (Model Context Protocol) サポート（オプション）

このプロジェクトはMCPをサポートしており、Java Language Server (JDTLS) を使用してコード補完、エラー検出、リファクタリングなどの機能を提供します。**MCPは開発効率を向上させる便利なツールですが、プロジェクトのビルドや実行には必須ではありません。**

#### 必要なシステム要件

- **Java**: 17 以上（プロジェクトの要件と同じ）
- **JDTLS**: Eclipse Java Language Server

#### JDTLS インストール方法

> **注意**: MCPサポートはオプション機能です。開発に必須ではありませんが、IDE機能（自動補完、エラー検出等）を利用したい場合に有用です。

**手動インストール（推奨）:**
1. [Eclipse JDT Language Server リリースページ](https://github.com/eclipse-jdtls/eclipse.jdt.ls/releases) から最新版をダウンロード
2. `/opt/jdtls` ディレクトリを作成して展開:
   ```bash
   sudo mkdir -p /opt/jdtls
   # ダウンロードしたファイル名に合わせて、jdt-language-server-*.tar.gz を置き換えてください
   sudo tar -xzf jdt-language-server-<version>.tar.gz -C /opt/jdtls
   sudo chmod +x /opt/jdtls/bin/jdtls
   ```

**macOS (Homebrew):**
```bash
brew install jdtls
# シンボリックリンクを作成
sudo ln -sf $(brew --prefix jdtls)/libexec /opt/jdtls
```

**Ubuntu/Debian パッケージマネージャ:**
> **注意**: `jdtls` の snap パッケージは公式にメンテナンスされていない場合があります。インストール前に [Snapcraft](https://snapcraft.io/jdtls) でパッケージの提供元と最新情報を確認してください。
```bash
# snapパッケージ（利用可能な場合）
sudo snap install jdtls --classic

# または直接インストール
sudo apt update && sudo apt install openjdk-17-jdk
# その後手動インストール方法を実行
```

#### MCP サーバー起動

```bash
# MCP サーバーを起動（バックグラウンド実行）
./.lsmcp/jdtls.sh &

# プロジェクトをビルドしてJDTLSが解析できるようにする
./gradlew build -q
```

#### トラブルシューティング

**JDTLS が見つからない場合:**
```bash
# インストール確認
ls -la /opt/jdtls/bin/jdtls

# Java バージョン確認（17+ が必要）
java -version

# 権限確認
sudo chmod +x /opt/jdtls/bin/jdtls
```

**メモリ不足の場合:**
JDTLSの設定（`.lsmcp/jdtls.sh`）でヒープサイズを調整できます：
```bash
--jvm-arg=-Xmx2G  # デフォルトは1G
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
- **[コマンドアーキテクチャ](docs/ARCHITECTURE.md)** - コマンドパターンとController層の設計（旧ドキュメント: [`docs/archived/COMMAND_ARCHITECTURE.md`](docs/archived/COMMAND_ARCHITECTURE.md)）
- **[セキュリティ分析](docs/SECURITY_ANALYSIS.md)** - セキュリティ対策と脆弱性分析
- **[ベンチマーク実装](docs/BENCHMARK_IMPLEMENTATION.md)** - 大容量データ処理のパフォーマンス測定
- **[自動ログ機能](docs/AUTO_LOGGING.md)** - MDCとコンテキスト伝播の詳細

開発に参加する場合は [CONTRIBUTING.md](CONTRIBUTING.md) を参照してください。
