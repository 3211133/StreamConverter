# StreamConverter Web API

StreamConverter の処理を HTTP 経由で利用するための Spring Boot WebFlux モジュールです。

## 🚀 起動方法

```bash
# Web モジュールを起動
./gradlew :streamconverter-web:bootRun

# Fat Jar を作成
./gradlew :streamconverter-web:bootJar
```

デフォルトは `http://localhost:8080` で起動します。

## 📡 API エンドポイント

### ヘルスチェック
`GET /api/v1/stream/health`

### CSV ナビゲーション（構造保持）
`POST /api/v1/stream/csv/extract?columnName=name`

指定列に `PassThroughRule` を適用しながら CSV 構造全体を出力します（列のみを抽出するのではありません）。

### JSON ナビゲーション（構造保持）
`POST /api/v1/stream/json/extract?jsonPath=$.user.name`

指定パスに `PassThroughRule` を適用しながら JSON 構造全体を出力します（パスの値のみを抽出するのではありません）。

### XML ナビゲーション（構造保持）
`POST /api/v1/stream/xml/navigate?xpath=root/element`

指定パスに変換ルールを適用しながら XML 構造全体を出力します（パスの要素のみを抽出するのではありません）。

### パイプライン処理
`POST /api/v1/stream/process`

- `X-Pipeline-Config` ヘッダーで実行チェーンを指定
- 例: `csv:name,json:$.user.id`

## 🧪 テスト

```bash
# Web モジュールのテスト
./gradlew :streamconverter-web:test

# 特定テストクラス
./gradlew :streamconverter-web:test --tests "*StreamProcessingControllerTest*"
```
