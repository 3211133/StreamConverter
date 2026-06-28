# ブランチ戦略・運用ガイドライン

## 概要

StreamConverterプロジェクトのブランチ運用標準です。

## 現状の課題と解決策

### 解決された課題
- ✅ **ブランチ保護設定**: develop/mainブランチに保護設定が適用済み
- ✅ **自動クリーンアップ**: branch-cleanup.ymlワークフローが実装済み

### 改善項目
- 🔄 **命名規則の統一**: 一部のブランチが統一規則に従っていない
- 🔄 **長期間ブランチの整理**: 複数の長期間存在するfeatureブランチが存在

## ブランチ構成

### メインブランチ
- **`develop`** - 統合ブランチ（デフォルト）
  - 全機能の統合とテスト
  - CI/CDの実行対象
  - PR必須、レビュー必須

- **`main`** - 本番リリース用
  - 安定版のみマージ
  - タグ付きリリース用
  - 自動デプロイ対象

### 作業ブランチ

#### 機能開発
```
feature/ISSUE-NUMBER-short-description
例: feature/145-branch-strategy-improvements
```
- 新機能、機能改善
- developから分岐
- PR後squash merge

#### バグ修正
```
fix/ISSUE-NUMBER-bug-description
例: fix/146-null-pointer-exception
```
- バグ修正、軽微な改善
- developから分岐
- PR後squash merge

#### ドキュメント
```
docs/ISSUE-NUMBER-doc-type
例: docs/147-api-documentation
```
- ドキュメント更新
- developから分岐
- PR後squash merge

#### 緊急修正
```
hotfix/ISSUE-NUMBER-critical-fix
例: hotfix/148-security-vulnerability
```
- 本番緊急対応
- mainから分岐
- 修正後main, developの両方にマージ

## ワークフロー

### 1. 標準開発フロー
```bash
# 1. Issue作成・番号確認
gh issue create --title "機能名" --body "詳細"

# 2. ブランチ作成
git checkout develop
git pull origin develop
git checkout -b feature/ISSUE-NUMBER-description

# 3. 開発・コミット
git add .
git commit -m "feat: 機能の説明"

# 4. プッシュ・PR作成
git push -u origin feature/ISSUE-NUMBER-description
gh pr create --title "feat: 機能名" --body "Issue #NUMBER"

# 5. レビュー・マージ後クリーンアップ
git checkout develop
git pull origin develop
git branch -d feature/ISSUE-NUMBER-description
```

### 2. 緊急修正フロー
```bash
# 1. mainから分岐
git checkout main
git pull origin main
git checkout -b hotfix/ISSUE-NUMBER-description

# 2. 修正・テスト
# ... 修正作業 ...

# 3. mainにマージ
gh pr create --base main --title "hotfix: 修正内容"

# 4. developにも反映
git checkout develop
git merge hotfix/ISSUE-NUMBER-description
```

## コミットメッセージ規則

Conventional Commits 仕様に従います。詳細は [COMMAND_REFERENCE.md](../reference/COMMAND_REFERENCE.md#conventional-commits) を参照してください。

## マージ戦略

### feature/fix/docs → develop
- **Squash merge**
- 1つのコミットに集約
- PR番号をコミットメッセージに含める

### develop → main
- **Merge commit**
- リリース履歴を保持
- タグ付けしてリリース

### hotfix → main/develop
- **状況に応じて選択**
- 緊急度と影響範囲で判断

## ブランチ保護設定

### develop
- PR必須
- レビュー1名以上必須
- CI通過必須
- 最新の変更に対する再レビュー必須

### main
- PR必須
- レビュー必須
- 全CI通過必須
- 管理者権限でも適用

## 自動化

### ブランチクリーンアップ
- マージ後の自動削除: ON
- staleブランチの定期削除

### CI/CD
- develop: 全テスト + 品質チェック
- main: 全テスト + デプロイ
- PR: 差分テスト + Lint

## ブランチ運用ルール

### DO
- Issue番号を含める
- 短期間での開発（1-2週間以内）
- 明確な目的を持つ
- レビュー前のセルフチェック

### DON'T
- 長期間のブランチ保持
- 複数機能の混在
- developへの直接push
- レビューなしのマージ

## トラブルシューティング

### コンフリクト解決
```bash
git checkout develop
git pull origin develop
git checkout feature/your-branch
git merge develop
# コンフリクト解決
git commit
git push
```

### 間違ったマージの取り消し
```bash
# マージコミットの取り消し
git revert -m 1 <merge-commit-hash>
```

## 関連リンク
- [Issue #145 - ブランチ戦略改善](https://github.com/3211133/StreamConverter/issues/145)
- [GitHub Flow Documentation](https://guides.github.com/introduction/flow/)
- [Conventional Commits](https://www.conventionalcommits.org/)