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

### CSV 抽出
`POST /api/v1/stream/csv/extract?columnName=name`

### JSON 抽出
`POST /api/v1/stream/json/extract?jsonPath=$.user.name`

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
