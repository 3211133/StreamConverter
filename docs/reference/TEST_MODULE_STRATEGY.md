# モジュール別テスト責任境界

## 概要

このドキュメントは、StreamConverter の各モジュールが担うテスト責任の境界を定義します。
どのモジュールに何をテストすべきかを明文化することで、テストの重複・抜けを防ぎます。

## モジュール構成とテスト責任

### `streamconverter-core`

**責任範囲:** コアライブラリの単体テスト・統合テスト

| テストカテゴリ | 内容 | 場所 |
|--------------|------|------|
| 単体テスト | `StreamConverter`, `IStreamCommand`, `AbstractStreamCommand`, ルール実装 | `src/test/java/` |
| パイプライン統合テスト | 複数コマンドの連携動作 | `StreamConverterIntegrationTest` |
| コンテキスト伝播テスト | `PipelineContext`, MDC 継承 | `PipelineContextTest`, `StreamConverterMDCIntegrationTest` |
| メモリ効率テスト | ストリーミング設計原理の検証 | `MemoryEfficiencyTest` |
| セキュリティテスト | XXE, XPath injection 防止 | `security/` パッケージ下 |

**テスト除外:** ベンチマーク (`@Tag("benchmark")`) は `streamconverter-tools` に委譲。

### `streamconverter-http`

**責任範囲:** HTTP コマンドの単体・セキュリティテスト

| テストカテゴリ | 内容 | 場所 |
|--------------|------|------|
| 単体テスト | `SendHttpCommand` の正常系・異常系 | `SendHttpCommandTest` |
| セキュリティテスト | URL バリデーション、SSRF 防止 | `SendHttpCommandSecurityTest` |

**テスト除外:** HTTP 統合テスト（外部ネットワーク依存）は CI では `-DskipNetworkTests=true` でスキップ。

### `streamconverter-db`

**責任範囲:** データベース連携コマンドのテスト

| テストカテゴリ | 内容 |
|--------------|------|
| 単体テスト | `DatabaseFetchRule` の SQL パラメータ化クエリ動作 |
| セキュリティテスト | SQL インジェクション防止 |
| 統合テスト | H2 インメモリ DB を使用したルール動作確認 |

**テスト除外:** 本番 DB への接続テストは実施しない。H2 で代替。

### `streamconverter-web`

**責任範囲:** Web API レイヤーのテスト

| テストカテゴリ | 内容 |
|--------------|------|
| コントローラーテスト | REST エンドポイントのリクエスト/レスポンス検証 |
| 統合テスト | パイプライン呼び出しの E2E 動作 |

### `streamconverter-tools`

**責任範囲:** ベンチマーク・パフォーマンステスト

| テストカテゴリ | 内容 | Gradle タスク |
|--------------|------|-------------|
| 大容量データベンチマーク | 5GB/50MB データの処理性能 | `benchmarkLargeData` |
| インフラベンチマーク | ベンチマーク基盤の動作確認 | `benchmarkInfrastructure` |
| メモリ効率ベンチマーク | ヒープ使用量プロファイリング | `benchmarkMemoryEfficiency` |
| 全ベンチマーク | 全ベンチマークスイート | `benchmarkAll` |

**タグ規約:** すべてのベンチマークテストには `@Tag("benchmark")` を付与すること。
大容量データ使用のテストには追加で `@Tag("large-data")` を付与すること。

### `streamconverter-examples`

**責任範囲:** 使用例の動作検証

| テストカテゴリ | 内容 |
|--------------|------|
| スモークテスト | サンプルコードが例外なく動作することの確認 |
| API 互換性テスト | 公開 API の使用例が最新 API と一致していること |

## ベンチマーク配置の原則

1. **配置場所**: ベンチマークテストは `streamconverter-tools/src/test/java/**/benchmark/` に集約する。
2. **タグ付け**: `@Tag("benchmark")` を必ず付与し、通常テスト実行 (`./gradlew test`) から除外されることを保証する。
3. **専用タスク**: `benchmarkAll` タスクで一括実行できること。
4. **メモリ設定**: ベンチマークタスクには `-Xmx3g` 以上を設定すること。

## テスト実行コマンド

```bash
# 通常のテスト（ベンチマーク除外）
./gradlew test

# ベンチマークのみ実行
./gradlew :streamconverter-tools:benchmarkAll

# 特定モジュールのテスト
./gradlew :streamconverter-core:test
./gradlew :streamconverter-http:test

# 全テスト + 静的解析
./gradlew check
```

## 参照

- [TESTING.md](TESTING.md) — テスト戦略の全体像
- Issue #509 — モジュール別テスト戦略の整備
