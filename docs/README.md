# Documentation Index

StreamConverterの包括的なドキュメントです。目的に応じて適切なドキュメントを参照してください。

## 📚 ドキュメント構成

### 🚀 Getting Started
- [**メインREADME**](../README.md) - プロジェクト概要と基本的な使用方法
- [**クイックスタート**](../src/main/java/com/streamConverter/examples/QuickStart.java) - 簡単な使用例
- [**コントリビューション**](../doc/CONTRIBUTING.md) - 開発参加ガイドライン

### 🏗️ Architecture & Design
- [**Command Architecture**](COMMAND_ARCHITECTURE.md) - コマンドパターンとファクトリー設計
- [**Context Propagation Architecture**](../CONTEXT_PROPAGATION_ARCHITECTURE.md) - マルチスレッド環境でのMDC管理
- [**Auto-Logging Infrastructure**](AUTO_LOGGING.md) - 自動ログ機能の設計と使用方法
- [**Auto-Logging Design**](auto-logging-design.md) - ログ機能の設計詳細
- [**Auto-Logging Implementation**](auto-logging-implementation.md) - 実装詳細

### 📋 Reference
- [**Version Management**](VERSION_MANAGEMENT.md) - バージョン管理とサポートポリシー
- [**Security Policy**](../SECURITY.md) - セキュリティポリシーと脆弱性報告
- [**Logging Rules**](logging-rules.md) - ログ出力ルールとガイドライン
- [**Javadoc**](javadoc/index.html) - API詳細リファレンス

### 🛠️ Development
- [**Test Strategy**](TESTING.md) - テスト戦略とガイドライン
- [**Examples**](../src/main/java/com/streamConverter/examples/) - 実用的な使用例集
- [**Demo Applications**](../src/main/java/com/streamConverter/demo/) - デモアプリケーション

## 📖 学習パス

### 初めてのユーザー
1. [メインREADME](../README.md) - プロジェクト概要
2. [QuickStart](../src/main/java/com/streamConverter/examples/QuickStart.java) - 基本的な使用方法
3. [Context Propagation Demo](../src/main/java/com/streamConverter/examples/ContextPropagationDemo.java) - コンテキスト伝播の活用
4. [Auto-Logging](AUTO_LOGGING.md) - ログ機能の活用

### 開発者
1. [Command Architecture](COMMAND_ARCHITECTURE.md) - アーキテクチャ理解
2. [Contributing Guidelines](../doc/CONTRIBUTING.md) - 開発参加方法
3. [Test Strategy](TESTING.md) - テスト方針

### システム管理者
1. [Version Management](VERSION_MANAGEMENT.md) - サポートバージョン
2. [Security Policy](../SECURITY.md) - セキュリティ対応
3. [Logging Rules](logging-rules.md) - ログ管理

## 🔧 設定とカスタマイズ

### ログ設定
- [Auto-Logging](AUTO_LOGGING.md#ログ設定のカスタマイズ) - ログ設定の詳細
- [Logging Rules](logging-rules.md) - ログレベルとフォーマット

### パフォーマンス調整
- [Command Architecture](COMMAND_ARCHITECTURE.md#パフォーマンス考慮事項) - パフォーマンス最適化
- [Auto-Logging](AUTO_LOGGING.md#パフォーマンス考慮事項) - ログのオーバーヘッド管理

## 🆘 トラブルシューティング

### よくある問題
- [Auto-Logging](AUTO_LOGGING.md#トラブルシューティング) - ログ関連の問題
- [Command Architecture](COMMAND_ARCHITECTURE.md#エラーハンドリング) - コマンド実行エラー

### サポート情報
- [Security Policy](../SECURITY.md#reporting-a-vulnerability) - セキュリティ問題の報告
- [GitHub Issues](https://github.com/3211133/StreamConverter/issues) - バグ報告と機能要求

## 📝 ドキュメント更新履歴

- 2025-07-27: コンテキスト伝播アーキテクチャドキュメント追加、MDCマルチスレッド対応
- 2025-07-26: ドキュメント構造の再整理、自動ログ機能ドキュメント追加
- 2025-07-XX: コマンドアーキテクチャドキュメント追加
- 2025-07-XX: バージョン管理ポリシー策定

---

💡 **Tip**: 特定の機能について詳しく知りたい場合は、対応するJavaクラスのJavadocも参照してください。[Javadoc Index](javadoc/index.html)から検索できます。