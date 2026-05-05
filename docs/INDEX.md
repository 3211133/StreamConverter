# StreamConverter Documentation Index

> 📚 **このインデックスについて**: プロジェクト全体の完全なドキュメント一覧（カテゴリ別リファレンス）です。
> クイックスタートやハイライトは [README.md](README.md) を参照してください。

この索引はドキュメントの全体像を把握するためのナビゲーションです。目的に応じて以下のカテゴリから参照してください。

## 🚀 Getting Started
- **[../README.md](../README.md)**: リポジトリ概要と最新のハイライト
- **[quickstart/basic-usage.md](quickstart/basic-usage.md)**: 最初のパイプライン実装
- **[handbook/README.md](handbook/README.md)**: What / Why / How 形式の機能ガイド

## 🧱 Architecture
- **[ARCHITECTURE.md](ARCHITECTURE.md)**: 4 層アーキテクチャと設計原則
- **[ARCHITECTURE_DIAGRAMS.md](ARCHITECTURE_DIAGRAMS.md)**: UML / クラス関係図
- **[handbook/architecture.md](handbook/architecture.md)**: パイプライン内部の詳細
- **[reference/CONTEXT_PROPAGATION_ARCHITECTURE.md](reference/CONTEXT_PROPAGATION_ARCHITECTURE.md)**: コンテキスト伝播の背景調査

## 🧰 Features
- **[handbook/logging.md](handbook/logging.md)**: 自動ログ・MDC 連携
- **[handbook/validation.md](handbook/validation.md)**: 入力検証
- **[handbook/web-api.md](handbook/web-api.md)**: Web API の操作方法
- **[handbook/communication.md](handbook/communication.md)**: 非同期コミュニケーション指針
- **[AUTO_LOGGING.md](AUTO_LOGGING.md)**: ログ装飾の詳細
- **[features/VALIDATION.md](features/VALIDATION.md)** / **[features/RULES_CATALOG.md](features/RULES_CATALOG.md)**: コマンドとルールの仕様

## 🛠️ Development Workflow
- **[guides/PRE_COMMIT_SETUP.md](guides/PRE_COMMIT_SETUP.md)**: 開発環境の準備
- **[guides/BRANCH_STRATEGY.md](guides/BRANCH_STRATEGY.md)**: ブランチ戦略・運用ガイドライン
- **[guides/BENCHMARK_IMPLEMENTATION.md](guides/BENCHMARK_IMPLEMENTATION.md)**: ベンチマーク実装詳細
- **[development/CONFIGURATION.md](development/CONFIGURATION.md)**: IDE & MCP 設定
- **[development/INTEGRATION_EXAMPLES.md](development/INTEGRATION_EXAMPLES.md)**: 連携サンプル
- **[scripts/create-issues.sh](../scripts/create-issues.sh)**: 自動化スクリプト

## 🧪 Quality & Testing
- **[reference/TESTING.md](reference/TESTING.md)**: テスト戦略と実行方法
  ストリーミング契約テストの目的、判定分類、実行コマンドを含む
- **[reference/TEST_INVENTORY.md](reference/TEST_INVENTORY.md)**: テストケース一覧
- **[reference/MEMORY_EFFICIENCY_TEST_STRATEGY.md](reference/MEMORY_EFFICIENCY_TEST_STRATEGY.md)**: 大容量データ検証
- **[reference/CROSS_PLATFORM_TEST_CONSIDERATIONS.md](reference/CROSS_PLATFORM_TEST_CONSIDERATIONS.md)**: マルチプラットフォーム対応
- **[reference/JAVADOC_MANAGEMENT.md](reference/JAVADOC_MANAGEMENT.md)**: API ドキュメント生成
- **[reference/COMMAND_REFERENCE.md](reference/COMMAND_REFERENCE.md)**: Gradle/Git コマンドリファレンス
- **[reference/COMMAND_EXAMPLES.md](reference/COMMAND_EXAMPLES.md)**: コマンド使用例（CSV/JSON/XML/HTTP）

## 🔐 Security & Operations
- **[reference/SECURITY_ANALYSIS.md](reference/SECURITY_ANALYSIS.md)**: セキュリティレビュー
- **[reference/VERSION_MANAGEMENT.md](reference/VERSION_MANAGEMENT.md)**: バージョン管理・リリース方針
- **[WEB_API.md](WEB_API.md)**: API 公開時の注意事項
- **[logging-rules.md](logging-rules.md)**: ログ運用ポリシー

## 🤖 AI Assistant Guides
- **[../CLAUDE.md](../CLAUDE.md)**: Claude Code 向けメインガイド（コマンド、ワークフロー）
- **[reference/CLAUDE_ARCHITECTURE.md](reference/CLAUDE_ARCHITECTURE.md)**: アーキテクチャパターンとコマンドタイプ
- **[reference/CLAUDE_EXAMPLES.md](reference/CLAUDE_EXAMPLES.md)**: コード例と使用パターン
- **[reference/CLAUDE_IMPLEMENTATION.md](reference/CLAUDE_IMPLEMENTATION.md)**: ストリーム処理制約とセキュリティ

## 📊 Reports & Research
- **[reference/MDC_ANALYSIS_REPORT.md](reference/MDC_ANALYSIS_REPORT.md)**: MDC 分析

## 🗂️ Archive
- **[archived/](archived/)**: 旧アーキテクチャや破棄済み設計資料

> この一覧に載っていない場合は `docs/` 以下を直接検索してください (`rg` や IDE の検索機能が便利です)。
