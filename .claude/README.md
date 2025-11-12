# Claude Code Configuration

このディレクトリには、Claude Code の設定ファイルが含まれています。

## ファイル構成

- **`settings.json`** - プロジェクト共有設定（Gitで管理）
- **`settings.local.json`** - ユーザー固有設定（Gitで管理しない）
- **`agents/`** - カスタムエージェント定義

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
