# Javadoc Management Guide

## 問題の背景

Javadocファイルは自動生成されるため、複数の開発者が同時に変更するとGitでコンフリクトが頻発します。この問題を解決するための管理戦略を提供します。

## 解決策

### 1. 🚀 推奨ワークフロー: CI自動生成 + 手動管理スクリプト

#### GitHub Actions による自動生成
- `main`/`develop` ブランチへのpush時にJavadocを自動生成
- GitHub Pagesに自動デプロイ
- 設定ファイル: `.github/workflows/javadoc.yml`

#### ローカル開発時
- **Pre-commitフック**: Javadoc構文チェックのみ（ファイル生成なし）
- **手動更新**: 必要時のみ `scripts/update-javadoc.sh` を実行

### 2. 📁 ファイル管理戦略

```
docs/javadoc/          # 既存ファイル (GitHub Pages用)
├── *.html            # Git追跡対象 (削除不可)
└── ...

.gitignore            # 追加設定
├── docs/javadoc/     # 新規生成ファイルを除外
```

### 3. 🔧 スクリプトの機能

`scripts/update-javadoc.sh` の特徴:
- ✅ **自動バックアップ**: 既存Javadocをバックアップ
- ✅ **コンフリクト検出**: リモートとの差分チェック
- ✅ **安全なマージ**: リモート変更を優先して取り込み
- ✅ **変更サマリー**: 追加/変更/削除ファイル数を表示
- ✅ **インタラクティブ**: コミット・プッシュの確認

### 4. 🎯 GitHub Actions設定

`.github/workflows/javadoc.yml` の機能:
- **トリガー**: Java ファイル変更時のみ実行
- **効率化**: 変更がない場合はスキップ
- **自動コミット**: 更新があれば自動コミット
- **Pages配信**: GitHub Pagesに自動デプロイ

## 🚨 緊急時対応

### Javadocコンフリクトでマージできない場合
```bash
# 1. リモートのJavadocを優先
git checkout --theirs docs/javadoc/
git add docs/javadoc/

# 2. マージ完了
git commit

# 3. 再生成（必要なら）
./scripts/update-javadoc.sh
```

### CIでJavadoc生成が失敗する場合
```bash
# ローカルで確認
./gradlew javadoc

# エラーを修正後、再プッシュ
git add src/main/java/
git commit -m "fix: javadoc errors"
git push
```

## 📋 ベストプラクティス

### DO ✅
- **定期的なプル**: `git pull` でリモートJavadocを取得
- **スクリプト使用**: 手動更新は専用スクリプトを使用
- **CI依存**: 基本的にCIでの自動生成に任せる
- **構文チェック**: Pre-commitでJavadoc構文エラーを確認

### DON'T ❌
- **直接編集**: Javadocファイルを手動編集しない
- **強制プッシュ**: `git push --force` でJavadocコンフリクトを解決しない
- **無視**: Javadoc警告を放置しない
- **頻繁更新**: 開発中に毎回Javadocを生成しない

## 🔧 設定詳細

### .gitignore 設定
```gitignore
# 新規生成されるJavadocファイルを除外
docs/javadoc/

# ただし、既存ファイルは追跡継続
```

### CI環境変数（必要な場合）
```yaml
env:
  JAVA_OPTS: "-Xmx2048m"
  GRADLE_OPTS: "-Dorg.gradle.daemon=false"
```

この管理方法により、Javadocコンフリクトを最小限に抑えながら、常に最新のドキュメントを維持できます。