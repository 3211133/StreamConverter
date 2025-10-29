# StreamConverter

**Version**: 1.2.0 | [📚 ドキュメント索引](docs/INDEX.md) | [🚀 クイックスタート](docs/quickstart/basic-usage.md) | [🔗 API Documentation](https://3211133.github.io/StreamConverter/)

大容量ファイルのストリーム処理を効率的に行うためのJavaライブラリです。メモリ使用量を抑えながら、複数の処理を連結するパイプライン型アーキテクチャを提供します。

> **ドキュメント優先**: このプロジェクトでは、ドキュメントを真実のソース (Single Source of Truth) として扱います。詳細は [📚 ドキュメント索引](docs/INDEX.md) を参照してください。

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
| | `json.JsonValidateCommand` | JSON スキーマ検証 | `JsonValidateCommand.create("schema.json")` |
| | `json.JsonStreamingValidateCommand` | JSON ストリーミング検証 | `JsonStreamingValidateCommand.create("schema.json")` |
| | `xml.ValidateCommand` | XML スキーマ検証 | `new ValidateCommand("schema.xsd")` |

## 📖 ドキュメント

| 分類 | ドキュメント | 説明 |
|------|-------------|------|
| **🚀 はじめに** | [クイックスタート](docs/quickstart/basic-usage.md) | 基本的な使用方法 |
| | [ドキュメント索引](docs/INDEX.md) | 全ドキュメントの案内 |
| **🏗️ アーキテクチャ** | [システム設計](docs/ARCHITECTURE.md) | 4層アーキテクチャと設計原則 |
| | [アーキテクチャ図](docs/ARCHITECTURE_DIAGRAMS.md) | UMLとクラス関係図 |
| **🛠️ 機能ガイド** | [Web API](docs/handbook/web-api.md) | REST API の使用方法 |
| | [ログ機能](docs/handbook/logging.md) | MDC連携とコンテキスト伝播 |
| | [バリデーション](docs/handbook/validation.md) | データ検証機能 |
| **🚀 運用** | [Docker化](docs/deployment/docker.md) | コンテナ環境での実行 |
| | [セキュリティ](SECURITY.md) | セキュリティポリシー |

## 🎯 使用例

詳細な使用例は以下のサンプルコードを参照してください：

- **[QuickStart.java](streamconverter-examples/src/main/java/com/streamConverter/examples/QuickStart.java)** - 基本的な使用方法
- **[AutoLoggingDemo.java](streamconverter-examples/src/main/java/com/streamConverter/examples/AutoLoggingDemo.java)** - ログ機能のデモ
- **[ContextPropagationDemo.java](streamconverter-examples/src/main/java/com/streamConverter/examples/ContextPropagationDemo.java)** - コンテキスト伝播のデモ
- **[MDCMultiThreadExample.java](streamconverter-examples/src/main/java/com/streamConverter/examples/MDCMultiThreadExample.java)** - MDCマルチスレッド検証
- **[DataProcessingExamples.java](streamconverter-examples/src/main/java/com/streamConverter/examples/DataProcessingExamples.java)** - 実用的な処理例
- **[EnterpriseIntegrationPatterns.java](streamconverter-examples/src/main/java/com/streamConverter/examples/EnterpriseIntegrationPatterns.java)** - エンタープライズパターン

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

### Docker環境
```bash
# Docker イメージビルド
./gradlew dockerBuild

# コンテナ環境で実行
./gradlew dockerRun

# 開発環境（デバッグ付き）
./gradlew dockerDev
```

### その他
```bash
# コードスタイル適用
./gradlew spotlessApply

# API ドキュメント生成
./gradlew javadocAll

# ベンチマークテスト
./gradlew benchmarkAll
```

詳細は [テスト戦略ガイド](docs/reference/TESTING.md) と [Docker ガイド](docs/deployment/docker.md) を参照してください。

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

## 🤝 コントリビューション

開発に参加する場合は以下を参照してください：

- **[コントリビューションガイド](CONTRIBUTING.md)** - 開発参加方法とルール
- **[開発環境セットアップ](docs/guides/PRE_COMMIT_SETUP.md)** - 開発環境の構築
- **[ブランチ戦略](docs/guides/BRANCH_STRATEGY.md)** - Git ワークフロー

## 📞 サポート

- **[GitHub Issues](https://github.com/3211133/StreamConverter/issues)** - バグ報告・機能要求
- **[セキュリティポリシー](SECURITY.md)** - 脆弱性報告
- **[ドキュメント索引](docs/INDEX.md)** - 詳細な技術情報

---

**StreamConverter v1.2.0** - 効率的なストリーム処理ライブラリ
