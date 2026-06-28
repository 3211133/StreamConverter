# Command Reference — Gradle / Git チートシート

> このファイルは StreamConverter プロジェクトで使用する **Gradle コマンドおよび Git コマンド**のリファレンスです。StreamConverter の Java API（コマンドクラス）については [COMMAND_EXAMPLES.md](COMMAND_EXAMPLES.md) を参照してください。

## Build Commands

### `./gradlew build`
すべてのモジュールをビルドします（コンパイル、テスト、アーティファクト作成）。

### `./gradlew buildAll`
全モジュールのビルドを一括実行します。

### `./gradlew clean`
ビルド成果物を削除します。

## Testing Commands

### 基本的なテスト実行

```bash
# すべてのテストを実行（ベンチマークを除く）
./gradlew test

# 全モジュールのテストを実行
./gradlew testAll

# 特定モジュールのテスト
./gradlew :streamconverter-core:test

# 特定のテストクラスを実行
./gradlew test --tests "com.streamConverter.StreamConverterTest"

# 特定のテストメソッドを実行
./gradlew test --tests "com.streamConverter.StreamConverterTest.testRunWithValidStreams"

# パッケージ単位でのテスト実行
./gradlew test --tests "com.streamConverter.command.*"

# ネットワークテストを含める（通常はスキップ）
./gradlew test -DskipNetworkTests=false
```

### ベンチマークテスト

```bash
# 全ベンチマークテスト
./gradlew benchmarkAll

# 大容量データテスト（5GB tests - requires 3GB heap）
./gradlew benchmarkLargeData

# メモリ効率テスト
./gradlew benchmarkMemoryEfficiency

# ベンチマーク基盤テスト
./gradlew benchmarkInfrastructure
```

**メモリ要件**:
- Large data benchmarks: `-Xmx3g -Xms1g` (自動設定)
- Memory efficiency tests: `-Xms1g -Xmx2g` (自動設定)
- Regular tests: `-Xmx1g -Xms512m` (自動設定)

## Code Quality

### `./gradlew spotlessApply`
コードフォーマットを適用します（Google Java Format）。

### `./gradlew spotlessCheck`
コードフォーマットをチェックします（CI用）。

### `./gradlew check`
静的解析を実行します（PMD + SpotBugs）。

### `./gradlew pmdMain`
PMD 静的解析を実行します。

### `./gradlew spotbugsMain`
SpotBugs 静的解析を実行します。

### `./gradlew pitest`
ミューテーションテストを実行します（オプション）。

## Documentation

### `./gradlew javadoc`
単一モジュールの Javadoc を生成します。

### `./gradlew javadocAll`
全モジュールの統合 Javadoc を生成します。

**出力先**: `build/docs/javadoc/index.html`

## Web Module Commands

### `./gradlew :streamconverter-web:bootRun`
Web モジュールの Spring Boot アプリケーションを起動します。

デフォルトポート: `http://localhost:8080`

## Example Commands

```bash
# 使用例を実行
./gradlew :streamconverter-examples:runPipelineBasics
./gradlew :streamconverter-examples:runNavigateAndRule
./gradlew :streamconverter-examples:runPipelineContext
./gradlew :streamconverter-examples:runValidationPipeline
```

## Git Commands

### Pre-commit

```bash
# 変更をステージング
git add <files>

# コミット（pre-commit フックが自動実行される）
git commit -m "message"

# Pre-commit フックの内容:
# - コードフォーマットチェック（spotlessCheck）
# - コンパイルチェック
# - 高速テスト実行
```

### Conventional Commits

このプロジェクトは Conventional Commits 仕様を使用します：

```bash
feat(core): add CSV streaming validator
fix(web): handle 400 on invalid path
docs(readme): update installation instructions
refactor(core): simplify command factory
test(core): add integration tests for HTTP commands
```

## 開発ワークフロー

### 典型的な開発サイクル

```bash
# 1. コードを変更

# 2. フォーマット適用
./gradlew spotlessApply

# 3. テスト実行
./gradlew test

# 4. 静的解析
./gradlew check

# 5. ビルド確認
./gradlew build

# 6. コミット
git add .
git commit -m "feat: add new feature"
```

### Pull Request 作成前

```bash
# 完全なビルドとテストを実行
./gradlew clean build

# ベンチマークテスト（パフォーマンス変更時）
./gradlew benchmarkAll

# すべての静的解析を通過
./gradlew check
```

## トラブルシューティング

### ビルドキャッシュをクリア

```bash
./gradlew clean --refresh-dependencies
```

### Gradle デーモンを再起動

```bash
./gradlew --stop
```

### 詳細なログ出力

```bash
./gradlew test --info
./gradlew test --debug
```

## 関連ドキュメント

- [TESTING.md](TESTING.md) - テスト戦略と詳細
- [PRE_COMMIT_SETUP.md](../guides/PRE_COMMIT_SETUP.md) - Pre-commit フックの設定
- [WEB_API.md](../WEB_API.md) - Web API の起動と使用方法
