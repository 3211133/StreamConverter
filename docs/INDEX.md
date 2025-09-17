# StreamConverter ドキュメント索引

StreamConverterプロジェクトの包括的なドキュメント案内です。

## 🚀 はじめに

| ドキュメント | 説明 | 対象者 |
|-------------|------|--------|
| [クイックスタート](quickstart/basic-usage.md) | 基本的な使用方法と最小構成 | 初心者 |
| [基本概念](handbook/architecture.md) | アーキテクチャと設計思想 | 開発者 |
| [API リファレンス](https://3211133.github.io/StreamConverter/) | Javadoc API ドキュメント | 開発者 |

## 📖 ユーザーガイド

### 基本操作
- [基本的な使用方法](quickstart/basic-usage.md) - StreamConverterの基本的な使い方
- [コマンド一覧](features/RULES_CATALOG.md) - 利用可能なコマンドとその使用例
- [バリデーション](handbook/validation.md) - データ検証機能の使い方

### 高度な機能
- [Web API](handbook/web-api.md) - REST API の使用方法
- [ロギング](handbook/logging.md) - ログ機能とMDC連携
- [通信機能](handbook/communication.md) - HTTP通信とセキュリティ

## 🛠️ 開発者ガイド

### アーキテクチャ
- [システムアーキテクチャ](ARCHITECTURE.md) - 全体設計と4層アーキテクチャ
- [アーキテクチャ図](ARCHITECTURE_DIAGRAMS.md) - UMLとクラス関係図
- [コンテキスト伝播](reports/CONTEXT_PROPAGATION_ARCHITECTURE.md) - MDCとマルチスレッド設計

### 開発環境
- [開発環境セットアップ](guides/PRE_COMMIT_SETUP.md) - 開発環境の構築方法
- [ブランチ戦略](guides/BRANCH_STRATEGY.md) - Git ワークフローとブランチ管理
- [設定ガイド](development/CONFIGURATION.md) - プロジェクト設定の詳細

### テストとQA
- [テスト戦略](reference/TESTING.md) - 包括的なテスト実行ガイド
- [ベンチマーク](guides/BENCHMARK_IMPLEMENTATION.md) - パフォーマンステストの実行方法
- [クロスプラットフォーム考慮事項](reference/CROSS_PLATFORM_TEST_CONSIDERATIONS.md) - 環境差異への対応

## 🚀 運用ガイド

### デプロイメント
- [Docker コンテナ化](deployment/docker.md) - Docker による一貫した環境構築
- [バージョン管理](reference/VERSION_MANAGEMENT.md) - リリースとバージョニング戦略

### セキュリティ
- [セキュリティ分析](reference/SECURITY_ANALYSIS.md) - セキュリティ対策と脆弱性分析
- [セキュリティポリシー](../SECURITY.md) - 脆弱性報告とセキュリティポリシー

## 📊 レポートと分析

### 設計分析
- [MDC分析レポート](reports/MDC_ANALYSIS_REPORT.md) - MDC機能の詳細分析
- [エージェント分析](reports/AGENTS.md) - 自動化エージェントの分析結果

### パフォーマンス
- [メモリ効率テスト戦略](reference/MEMORY_EFFICIENCY_TEST_STRATEGY.md) - メモリ使用量最適化
- [テストインベントリ](reference/TEST_INVENTORY.md) - テストケースの網羅状況

## 🔧 リファレンス

### API ドキュメント
- [Javadoc 管理](reference/JAVADOC_MANAGEMENT.md) - API ドキュメント生成と管理
- [統合 Javadoc](https://3211133.github.io/StreamConverter/) - オンライン API リファレンス

### 開発ツール
- [PMD 自動修正](../scripts/README-PMD-AUTO-FIX.md) - コード品質改善ツール
- [コミットコマンド](development/commit-commands-summary.md) - 開発コマンド一覧

## 📚 アーカイブ

### 過去のドキュメント
- [archived/](archived/) - 廃止されたファクトリーパターン関連ドキュメント
- [旧アーキテクチャ](archived/OLD_ARCHITECTURE.md) - 過去のアーキテクチャ設計
- [旧自動ログ設計](archived/auto-logging-design.md) - 過去のログ機能設計

## 🎯 ドキュメント分類

### 形式別
- **📘 ガイド**: 手順や方法を説明する実用的なドキュメント
- **📙 リファレンス**: 仕様や詳細情報を記載した参照用ドキュメント
- **📗 ハンドブック**: What/Why/How の簡潔な説明
- **📕 レポート**: 分析結果や調査結果のドキュメント

### 対象者別
- **🟢 初心者**: 基本的な使用方法とクイックスタート
- **🟡 開発者**: アーキテクチャ、API、開発ガイド
- **🟠 運用者**: デプロイ、監視、トラブルシューティング
- **🔴 アーキテクト**: 設計思想、セキュリティ、パフォーマンス分析

## 🔗 外部リソース

- [GitHub リポジトリ](https://github.com/3211133/StreamConverter)
- [Issues・バグ報告](https://github.com/3211133/StreamConverter/issues)
- [コントリビューションガイド](../CONTRIBUTING.md)
- [変更履歴](../ROADMAP.md)

---

**ドキュメント更新**: このインデックスは定期的に更新されます。新しいドキュメントの追加や構造変更があった場合は、このファイルも併せて更新してください。