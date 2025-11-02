# StreamConverter Web API

## このドキュメントの基礎資料
このドキュメントは以下の実装を基に作成されています：
- [StreamProcessingController.java](../streamconverter-web/src/main/java/com/streamconverter/web/StreamProcessingController.java) - REST APIエンドポイントの実装

> 💡 **クイック概要**: まず [Web API Handbook](handbook/web-api.md) で What/Why/How を理解することをお勧めします。

StreamConverterの既存機能をWebAPIとして提供するRESTfulサービスです。

## 🚀 起動方法

```bash
# Web APIサーバーとして起動
./gradlew bootRun

# または、Jarファイルから起動
./gradlew bootJar
java -jar build/libs/StreamConverter-*.jar
```

サーバーは `http://localhost:8080` で起動します。

## 📡 API エンドポイント

### 1. ヘルスチェック
```bash
GET /api/v1/stream/health
```

### 2. CSV列抽出
```bash
POST /api/v1/stream/csv/extract?columnName=name
Content-Type: application/octet-stream

# 例: CSVファイルから特定の列を抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/csv/extract?columnName=name"
```

### 3. JSON パス抽出
```bash
POST /api/v1/stream/json/extract?jsonPath=$.user.name
Content-Type: application/octet-stream

# 例: JSONからパス指定で値を抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.json \
  "http://localhost:8080/api/v1/stream/json/extract?jsonPath=name"
```

### 4. パイプライン処理
```bash
POST /api/v1/stream/process
Content-Type: application/octet-stream
X-Pipeline-Config: csv:name,process:validator,json:$.result

# 例: 複数のコマンドをパイプライン実行
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  -H "X-Pipeline-Config: csv:name,process:validator" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/process"
```

## 🔧 パイプライン設定

`X-Pipeline-Config` ヘッダーでコマンドチェーンを指定できます：

- `csv:columnName` - CSV列抽出
- `json:jsonPath` - JSONパス抽出  
- `process:processorName` - カスタム処理

複数のコマンドはカンマで区切ります。

## 📝 使用例

### CSV処理の例
```bash
# input.csv
name,age,city
John,30,NYC
Jane,25,LA

# name列を抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.csv \
  "http://localhost:8080/api/v1/stream/csv/extract?columnName=name"
```

### JSON処理の例
```bash
# input.json
{"user":{"name":"John","age":30},"city":"NYC"}

# user.name を抽出
curl -X POST \
  -H "Content-Type: application/octet-stream" \
  --data-binary @input.json \
  "http://localhost:8080/api/v1/stream/json/extract?jsonPath=user.name"
```

## 🏗️ アーキテクチャ

- **Spring Boot WebFlux**: 非同期・ノンブロッキング処理
- **既存StreamConverter統合**: 既存のコマンドパイプラインをそのまま活用
- **InputStream/OutputStream対応**: バイナリストリーミング処理
- **メモリ効率**: 大容量データも少ないメモリで処理

## 🧪 テスト実行

```bash
# Web APIテストを実行
./gradlew test --tests "*StreamProcessingControllerTest*"

# 全テストを実行
./gradlew test
```

## 📊 監視・ログ

- **ログレベル**: `application.yml`で設定可能
- **ヘルスチェック**: `/api/v1/stream/health`
- **Spring Boot Actuator**: 運用監視機能（必要に応じて有効化）