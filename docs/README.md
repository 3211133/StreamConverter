# StreamConverter Documentation

StreamConverter を初めて使う方は、以下の順番で読み進めることをおすすめします。

---

## 🚀 まず読むべきドキュメント（3つ）

1. **[quickstart/basic-usage.md](quickstart/basic-usage.md)** - 最初のパイプライン実装（5分で動かす）
2. **[ARCHITECTURE.md](ARCHITECTURE.md)** - 4層アーキテクチャの全体像を理解する
3. **[handbook/README.md](handbook/README.md)** - 機能カタログ（What / Why / How）

この3つを読めば、StreamConverter の基本的な使い方と設計思想が理解できます。

---

## 🎯 目的別ガイド

### パイプラインを作りたい
→ **[handbook/architecture.md](handbook/architecture.md)** - コマンドの組み合わせ方とデータフロー

### Web API として使いたい
→ **[handbook/web-api.md](handbook/web-api.md)** - Spring Boot 統合と REST エンドポイント

### 開発に参加したい
→ **[guides/PRE_COMMIT_SETUP.md](guides/PRE_COMMIT_SETUP.md)** - 開発環境のセットアップ
→ **[../CONTRIBUTING.md](../CONTRIBUTING.md)** - コントリビューションガイドライン

### バリデーション機能を使いたい
→ **[handbook/validation.md](handbook/validation.md)** - CSV/JSON/XML のスキーマ検証

### ログ・トレーシングを設定したい
→ **[handbook/logging.md](handbook/logging.md)** - MDC 連携と自動ログ

---

## 📚 さらに詳しく知りたい場合

- **全ドキュメント一覧**: [INDEX.md](INDEX.md) から目的のドキュメントを検索
- **API リファレンス**: [Javadoc](https://3211133.github.io/StreamConverter/)
- **セキュリティ**: [reference/SECURITY_ANALYSIS.md](reference/SECURITY_ANALYSIS.md)
- **テスト戦略**: [reference/TESTING.md](reference/TESTING.md)
