# Claude Code - クイックリファレンス

StreamConverterプロジェクトで利用可能なClaude Code機能のクイックリファレンスです。

## スラッシュコマンド

### 開発ワークフロー

| コマンド | 用途 | タイミング |
|---------|------|-----------|
| `/plan-review` | 実装計画のレビュー依頼 | 実装開始前 |
| `/code-review` | 完成コードのレビュー依頼 | 実装完了後 |
| `/back-to-develop` | origin/developへ安全に復帰 | 作業完了時 |

### 使用例

```bash
# 新機能を実装する前に
/plan-review

# 実装完了後にレビューを依頼
/code-review

# 作業が終わったらdevelopに戻る
/back-to-develop
```

## カスタムエージェント

### @branch-manager

ブランチ管理の総合エージェント

**できること:**
- 新しい機能ブランチの作成
- ブランチ間の安全な切り替え
- 未コミット変更の適切な処理
- 古いブランチのクリーンアップ
- エラーリカバリー

**使用例:**
```
@branch-manager developに戻りたい
@branch-manager 新しい機能ブランチを作成して
@branch-manager 古いブランチを整理したい
@branch-manager feature/add-parserブランチに切り替えて
```

### @workflow-manager

開発ワークフロー全体の管理エージェント

**ワークフローフェーズ:**
1. **計画** - 要件ヒアリング → 実装計画 → Codexレビュー
2. **実装** - 承認された計画に従って実装
3. **レビュー** - Codexによるコードレビュー
4. **完成** - テスト実行 → PR作成準備

**使用例:**
```
@workflow-manager 新しいJSON処理機能を追加したい
```

### @pr-creator-ja

日本語PR作成とレビュー調整エージェント

**できること:**
- 日本語でのPR説明文生成
- @codexへのレビュー依頼
- レビューフィードバックの処理

**使用例:**
```
ユーザー: 新しいCSV処理機能を実装し終わりました
```

## 典型的なワークフローパターン

### パターン1: 新機能の実装（完全版）

```
1. @workflow-manager 新しいXXX機能を実装したい
   → 計画レビュー → 実装 → コードレビュー → 完成

2. ユーザー: PR作成して
   → PR作成 → @codexメンション

3. /back-to-develop
   → developに復帰 → ブランチクリーンアップ
```

### パターン2: 簡易実装（レビューなし）

```
1. @branch-manager feature/xxx ブランチを作成

2. （実装作業）

3. ユーザー: コミットして
   → git add + commit + push

4. /back-to-develop
   → developに復帰
```

### パターン3: レビュー重視型

```
1. /plan-review
   → 実装計画をCodexにレビュー依頼

2. （フィードバック反映後、実装作業）

3. /code-review
   → 実装コードをCodexにレビュー依頼

4. （問題修正後）PR作成

5. /back-to-develop
```

## よくある操作

### 未コミット変更がある状態でdevelopに戻りたい

```
/back-to-develop
```

Claude Codeが自動的に以下を確認:
1. 未コミット変更があることを検出
2. 処理方法の選択肢を提示
   - コミットする
   - stashに保存する
   - 破棄する（要確認）
3. 選択に応じて処理を実行
4. developに安全に切り替え

### 複数ブランチを整理したい

```
@branch-manager 古いブランチを整理したい
```

Claude Codeが:
1. 全ブランチをリスト表示
2. マージ済みブランチを特定
3. 削除候補を提案
4. 確認後に削除実行

### 誤って変更を破棄してしまった場合

```
# Stashから復元
git stash list
git stash apply stash@{0}

# Reflogから復元
git reflog
git checkout <commit-hash>
git checkout -b recovery-branch
```

## 安全機能

### 自動フォーマット

ファイル編集後に自動実行:
```bash
./gradlew spotlessApply
```

### テスト自動実行

セッション終了時に自動実行:
```bash
./gradlew test
```

### 許可リスト

以下のコマンドは承認なしで実行可能:
- Git操作: `add`, `commit`, `push`, `pull`, `checkout`, `fetch`, `stash`
- ビルド: `./gradlew`, `java`
- GitHub: `gh pr`, `gh issue`, `gh api`
- Codex: `codex`

### 拒否リスト

安全性のため以下は拒否:
- `cd` - ディレクトリ移動
- `rm -rf` - 危険な削除

## トラブルシューティング

### Merge conflicts発生時

```
# マージを中止
git merge --abort

# またはStashして再試行
git stash
git checkout develop
git pull --ff-only origin develop
```

### Detached HEAD状態

```
# 現在の状態を保存
git checkout -b rescue-branch

# その後通常のワークフローに戻る
```

### コマンドがタイムアウト

```
# Codexレビューで大きすぎる差分の場合
# 変更を複数に分けてレビュー依頼
```

## ヒントとベストプラクティス

1. **実装前に計画レビュー** - `/plan-review`で早期フィードバック
2. **小さく頻繁にコミット** - 作業を細かく区切る
3. **完了後は必ずレビュー** - `/code-review`で品質担保
4. **developは常に最新に** - 定期的に`git pull --ff-only origin develop`
5. **ブランチは定期的に整理** - `@branch-manager`でクリーンアップ

## 詳細ドキュメント

- **完全ガイド**: `.claude/README.md`
- **スラッシュコマンド定義**: `.claude/commands/`
- **エージェント定義**: `.claude/agents/`
- **設定ファイル**: `.claude/settings.json`

## サポート

問題が発生した場合:
1. `.claude/README.md`の詳細ドキュメントを確認
2. Claude Codeに直接質問
3. Git操作は慎重に - 不明な点は確認してから実行
