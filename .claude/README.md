# Claude Code Configuration

このディレクトリには、Claude Code の設定ファイルが含まれています。

## ファイル構成

- **`settings.json`** - プロジェクト共有設定（Gitで管理）
- **`settings.local.json`** - ユーザー固有設定（Gitで管理しない）
- **`agents/`** - カスタムエージェント定義
- **`commands/`** - スラッシュコマンド定義

## Codexレビュー統合

このプロジェクトでは、作業開始前と完了後にCodexと連携してコードレビューを実施できます。

### 前提条件

Codex CLIがインストールされている必要があります：
```bash
# インストール確認
which codex

# ヘルプ表示
codex --help
```

### スラッシュコマンド

#### `/plan-review` - 作業開始前の方針確認

実装を始める前にCodexに技術的アプローチを相談します。

**使用例:**
```
/plan-review
```

Claude Codeが以下を実施：
1. 実装予定の内容をヒアリング
2. 技術的アプローチの確認
3. Codexにレビュー依頼（日本語）
4. フィードバックを提示

**レビュー観点:**
- 技術的アプローチの妥当性
- 潜在的な問題点やリスク
- より良い代替案の有無
- アーキテクチャとの整合性

#### `/code-review` - 作業完了後のコードレビュー

実装完了後にCodexにコードレビューを依頼します。

**使用例:**
```
/code-review
```

Claude Codeが以下を実施：
1. git diffを取得
2. 変更内容の概要を確認
3. Codexにレビュー依頼（日本語）
4. フィードバックを分類して提示
   - 🚨 Critical issues（必須修正）
   - ⚠️ Important suggestions（推奨）
   - 💡 Nice-to-have improvements（任意）

**レビュー観点:**
- コードの品質と可読性
- 潜在的なバグやエッジケース
- パフォーマンスへの影響
- セキュリティ上の懸念
- より良い実装方法の提案
- テストカバレッジの十分性

#### `/back-to-develop` - developブランチ（origin/developと同期）への安全な復帰

作業ブランチからローカルのdevelopブランチに安全に戻り、origin/developと同期します。

**使用例:**
```
/back-to-develop
```

Claude Codeが以下を実施：
1. 現在のgit状態を確認
2. 未コミット変更の処理方法を確認
   - コミットする
   - stashに保存する
   - 破棄する（要確認）
3. developブランチに切り替え
4. 最新の変更を取得（git pull）
5. オプションで古いブランチを削除

**安全機能:**
- 未コミット変更の保護
- 破壊的操作前の確認
- 詳細な状態表示
- リカバリー方法の提示

### カスタムエージェント

#### `workflow-manager` - 完全ワークフロー管理

作業の開始から完了までをCodexレビュー統合で管理します。

**使用例:**
```
@workflow-manager 新しいJSON処理機能を追加したい
```

**ワークフローフェーズ:**

1. **計画フェーズ**
   - 要件のヒアリング
   - 実装計画の作成
   - Codexによる計画レビュー
   - フィードバック反映

2. **実装フェーズ**
   - 承認された計画に従って実装
   - TodoWriteで進捗管理
   - コード品質の維持

3. **レビューフェーズ**
   - git diffの収集
   - Codexによるコードレビュー
   - 問題点の修正

4. **完成フェーズ**
   - 全ての問題が解決済みか確認
   - テストの実行
   - PR作成準備

**メリット:**
- 設計段階で問題を早期発見
- 実装後の手戻りを削減
- 一貫した品質基準の維持
- 体系的なレビュープロセス

### レビューアーティファクト

レビュー時に生成される一時ファイル：
- `/tmp/plan-review-*.md` - 計画レビュー依頼
- `/tmp/code-review-*.diff` - コード変更差分
- `/tmp/code-review-*.md` - レビュー依頼概要
- `/tmp/workflow-plan-*.md` - ワークフロー計画
- `/tmp/workflow-review-*.diff` - ワークフロー変更差分

これらのファイルは各レビューセッション後も参照可能です。

## その他のカスタムエージェント

### `branch-manager` - ブランチ管理とワークフロー支援

ブランチの作成、切り替え、クリーンアップを安全に実行します。

**使用例:**
```
@branch-manager developに戻りたい
@branch-manager 新しい機能ブランチを作成して
@branch-manager 古いブランチを整理したい
```

**主な機能:**
- 安全なブランチ切り替え（未コミット変更の処理）
- origin/developへの復帰ワークフロー
- ブランチクリーンアップ（ローカル・リモート）
- 未コミット変更の管理（commit/stash/discard）
- エラーハンドリング（conflicts、detached HEAD等）

**ワークフロー:**
1. 現在の状態を確認
2. 未コミット変更を適切に処理
3. ブランチを安全に切り替え
4. 最新の変更を取得
5. オプションで古いブランチを削除

### `pr-creator-ja` - 日本語PR作成とレビュー調整

開発作業完了後、日本語のPR説明文を作成し、Codexにレビューを依頼します。

**使用例:**
```
ユーザー: 新しいCSV処理機能を実装し終わりました
```

**主な機能:**
- ブランチ管理（作成・切り替え）
- 包括的な日本語PR説明文の生成
  - 解決した課題
  - 解決方法
  - テスト内容
  - その他の注意事項
- PR説明文内で@codexにメンション
- レビューフィードバックの処理

## Slack通知の設定（オプション）

コマンド実行前にSlackに通知を送信する機能があります。

### 1. Slack Webhook URLの取得

1. Slack アプリを作成: https://api.slack.com/apps
2. "Incoming Webhooks" を有効化
3. Webhook URL をコピー（例: `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX`）

### 2. 環境変数の設定

#### bashの場合 (`~/.bashrc` または `~/.bash_profile`)
```bash
export WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

#### zshの場合 (`~/.zshrc`)
```zsh
export WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

設定後、ターミナルを再起動するか `source ~/.bashrc` を実行してください。

### 3. 動作確認

環境変数が設定されていれば、Claude Code のツール使用時に自動的にSlackに通知されます。

通知内容：
- 🤖 ツール名
- 📝 実行の説明
- 💻 実行コマンド/パラメータ

### 無効化

環境変数を設定しなければ、Slack通知は実行されません。

## Permissions（許可リスト）

`settings.json` には、承認なしで実行できるコマンドのリストが含まれています：

- **ビルドツール**: `./gradlew`, `java`
- **Git操作**: `git add`, `commit`, `push`, `pull`, `checkout`, `fetch`, `stash pop`
- **Git情報**: `git status`, `diff`, `log`, `branch`, `reset`
- **GitHub CLI**: `gh pr`, `gh issue`, `gh api`
- **Codex CLI**: `codex`（コードレビュー統合用）
- **その他**: `find`

### 拒否リスト

安全性のため、以下のコマンドは明示的に拒否されています：

- `cd` - ディレクトリ移動
- `rm -rf` - 危険な削除

## Hooks（フック）

### PostToolUse - ファイル編集後

`Write|Edit|MultiEdit` 後に `./gradlew spotlessApply` を自動実行し、コードフォーマットを適用します。

### PreToolUse - ツール使用前

1. **全ツール**: Slack通知（環境変数が設定されている場合）
2. **PR作成前**: `./gradlew test` を実行してテストが通過することを確認

### Stop - セッション終了時

`./gradlew test` を実行してテストが通過することを確認します。

## カスタマイズ

ユーザー固有の設定は `settings.local.json` に追加してください：

```json
{
  "permissions": {
    "allow": [
      "追加で許可したいコマンド"
    ]
  }
}
```

このファイルは `.gitignore` に含まれているため、個人的な設定がリポジトリにコミットされることはありません。
