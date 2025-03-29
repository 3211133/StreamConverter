# コミット時に実行したコマンド群のまとめ

このファイルは、コミット時のチェックスクリプトを整理・統合する際に実行したコマンドをまとめたものです。

## 1. 環境構築

```bash
# scriptsディレクトリの作成
mkdir -p scripts

# スクリプトファイルの作成と実行権限の付与
chmod +x scripts/commit-checks.sh
```

## 2. Gitフックの設定

```bash
# Gitフックのパスを設定
git config core.hooksPath .githooks

# フックスクリプトに実行権限を付与
chmod +x .githooks/pre-commit
chmod +x .githooks/commit-msg
chmod +x setup-hooks.sh
```

## 3. コードスタイルチェック

```bash
# Spotlessによるコードスタイルチェック
./gradlew spotlessCheck

# コードスタイルの自動修正
./gradlew spotlessApply

# キャッシュのクリア（問題が発生した場合）
rm -rf .gradle/configuration-cache
```

## 4. コミットメッセージチェック

```bash
# コミットメッセージのフォーマットチェック
npx commitlint --edit <commit-msg-file>

# コミットメッセージのパイプによるチェック
cat <commit-msg-file> | npx commitlint
```

## 5. 統合スクリプトの実行

```bash
# pre-commitモードでの実行（コードスタイルチェック）
./scripts/commit-checks.sh pre-commit

# commit-msgモードでの実行（コミットメッセージチェック）
./scripts/commit-checks.sh commit-msg <commit-msg-file>

# allモードでの実行（すべてのチェック）
./scripts/commit-checks.sh all [<commit-msg-file>]
```

## 6. Gitコミット

```bash
# 変更をステージングエリアに追加
git add <files>

# コミットの実行
git commit -m "<type>(<scope>): <subject>"

# フックをバイパスしてコミット（テスト時のみ）
git commit -m "<type>(<scope>): <subject>" --no-verify
```

## 7. コミットメッセージの例

```
refactor(git-hooks): consolidate commit-time checks into a single script

This commit consolidates all commit-time checks into a single script for better maintainability.
```

## 8. トラブルシューティング

```bash
# Spotlessのキャッシュクリア
rm -rf .gradle/configuration-cache

# コミットリントの依存関係インストール
npm install --save-dev @commitlint/cli @commitlint/config-conventional
```
