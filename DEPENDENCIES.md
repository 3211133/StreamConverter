# StreamConverter 依存関係ドキュメント

このドキュメントでは、StreamConverterプロジェクトで使用されている外部ライブラリとその役割について詳細に説明します。

## 概要

StreamConverterは以下の主要な依存関係を使用しています：

- **Spring Boot**: WebAPIフレームワーク
- **Data Processing**: JSON/XML/CSV処理ライブラリ
- **Security**: 認証・認可・CORS設定
- **Testing**: 単体テスト・統合テストフレームワーク
- **Documentation**: API仕様書自動生成

## Spring Boot 依存関係

### Core Framework

| 依存関係 | バージョン | 用途 | 説明 |
|---------|-----------|------|------|
| `spring-boot-starter-web` | 3.3.2 | WebAPI基盤 | REST API、組み込みTomcat、Jackson JSON処理 |
| `spring-boot-starter-validation` | 3.3.2 | バリデーション | リクエストデータの入力検証（Bean Validation） |
| `spring-boot-starter-actuator` | 3.3.2 | 監視・管理 | ヘルスチェック、メトリクス、アプリケーション監視 |
| `spring-boot-starter-security` | 3.3.2 | セキュリティ | 認証・認可、CORS設定、基本認証 |

#### spring-boot-starter-web の内部依存関係
```
├── spring-boot-starter
├── spring-boot-starter-tomcat (組み込みTomcatサーバー)
├── spring-webmvc (Spring MVC)
├── spring-web (Web基盤)
└── jackson-databind (JSON処理)
```

#### spring-boot-starter-actuator の機能
```
/actuator/health     - アプリケーション状態確認
/actuator/info       - アプリケーション情報
/actuator/metrics    - パフォーマンスメトリクス
/actuator/prometheus - Prometheus連携（設定時）
```

## データ処理ライブラリ

### Validation & Processing

| 依存関係 | バージョン | 用途 | 説明 |
|---------|-----------|------|------|
| `json-schema-validator` | 1.5.3 | JSONスキーマ検証 | JSON Schema Draft-07準拠のバリデーション |
| `opencsv` | 5.9 | CSV処理 | CSVファイルの読み書き・バリデーション |
| `commons-lang3` | 3.18.0 | ユーティリティ | 文字列処理、リフレクション、共通ユーティリティ |
| `commons-io` | 2.20.0 | I/O処理 | ファイル・ストリーム操作の便利メソッド |

#### json-schema-validator の機能
```java
// 使用例
JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
    .getSchema(schemaNode);
Set<ValidationMessage> errors = schema.validate(jsonNode);
```

#### opencsv の機能
```java
// 使用例
CSVReader reader = new CSVReader(new InputStreamReader(inputStream));
List<String[]> records = reader.readAll();
```

## API Documentation

| 依存関係 | バージョン | 用途 | 説明 |
|---------|-----------|------|------|
| `springdoc-openapi-starter-webmvc-ui` | 2.6.0 | API仕様書 | OpenAPI 3.0仕様書自動生成、Swagger UI |

#### Swagger UI 機能
```
/swagger-ui.html     - インタラクティブAPI仕様書
/api-docs            - OpenAPI 3.0 JSON仕様
/api-docs.yaml       - OpenAPI 3.0 YAML仕様
```

## テスト依存関係

### Testing Framework

| 依存関係 | バージョン | スコープ | 用途 | 説明 |
|---------|-----------|---------|------|------|
| `spring-boot-starter-test` | 3.3.2 | test | Spring統合テスト | Spring Boot統合テスト、MockMvc |
| `spring-security-test` | 6.3.1 | test | セキュリティテスト | セキュリティ機能のテスト支援 |
| `junit-bom` | 5.13.4 | test | テスト基盤 | JUnit 5の依存関係管理 |
| `junit-jupiter` | 5.13.4 | test | 単体テスト | JUnit 5テストエンジン |
| `mockito-core` | 5.18.0 | test | モック作成 | モックオブジェクト作成 |
| `mockito-junit-jupiter` | 5.18.0 | test | Mockito統合 | JUnit 5との統合 |
| `pitest-junit5-plugin` | 1.2.3 | test | 変異テスト | コードカバレッジ品質向上 |

#### spring-boot-starter-test の内部依存関係
```
├── junit-jupiter (JUnit 5)
├── mockito-core (モック作成)
├── assertj-core (流暢なアサーション)
├── hamcrest (マッチャー)
├── spring-test (Springテスト支援)
└── spring-boot-test (Spring Boot統合テスト)
```

## 依存関係の目的別分類

### 1. WebAPI基盤 (Production)
```gradle
// 必須のWebAPI機能
implementation("org.springframework.boot:spring-boot-starter-web")
implementation("org.springframework.boot:spring-boot-starter-validation")
implementation("org.springframework.boot:spring-boot-starter-security")
```

### 2. データ処理 (Production)
```gradle
// StreamConverter コア機能
implementation("com.networknt:json-schema-validator:1.5.3")
implementation("com.opencsv:opencsv:5.9")
implementation("org.apache.commons:commons-lang3:3.18.0")
implementation("commons-io:commons-io:2.20.0")
```

### 3. 運用・監視 (Production)
```gradle
// アプリケーション監視
implementation("org.springframework.boot:spring-boot-starter-actuator")
implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
```

### 4. 開発・テスト (Development)
```gradle
// テスト環境
testImplementation("org.springframework.boot:spring-boot-starter-test")
testImplementation("org.springframework.security:spring-security-test")
testImplementation("org.mockito:mockito-core:5.18.0")
```

## バージョン管理戦略

### Spring Boot BOM (Bill of Materials)
Spring Boot 3.3.2のBOMにより、以下の依存関係のバージョンが自動管理されています：

```
spring-core: 6.1.10
spring-web: 6.1.10
spring-webmvc: 6.1.10
spring-security: 6.3.1
jackson-core: 2.17.1
logback: 1.5.6
```

### 明示的バージョン指定
以下の依存関係は明示的にバージョンを指定：

- `commons-lang3: 3.18.0` - 最新安定版を使用
- `commons-io: 2.20.0` - 最新安定版を使用
- `json-schema-validator: 1.5.3` - JSON Schema Draft-07対応
- `opencsv: 5.9` - CSVバリデーション機能対応

## セキュリティ考慮事項

### 脆弱性管理
定期的な依存関係更新が必要な理由：

1. **セキュリティパッチ**: 既知の脆弱性修正
2. **パフォーマンス向上**: 最新最適化の適用
3. **新機能活用**: 最新機能の利用

### 推奨更新頻度
- **Spring Boot**: 四半期ごと（セキュリティパッチは即座）
- **その他ライブラリ**: 半年ごと
- **テストライブラリ**: 年1回

## 代替ライブラリの検討

### JSON Schema Validation
- **現在**: networknt/json-schema-validator
- **代替案**: everit-org/json-schema, leadpony/justify
- **選択理由**: Spring Boot互換性、パフォーマンス

### CSV Processing
- **現在**: opencsv
- **代替案**: Apache Commons CSV, univocity-parsers
- **選択理由**: バリデーション機能、Spring統合

## 依存関係の追加・削除プロセス

### 新規依存関係追加時のチェックリスト
1. **ライセンス確認** - Apache 2.0, MIT等の互換性
2. **セキュリティ確認** - 既知の脆弱性チェック
3. **メンテナンス状況** - アクティブな開発・更新
4. **サイズ影響** - JAR サイズへの影響評価
5. **パフォーマンス影響** - 起動時間・メモリ使用量

### 依存関係削除時の注意点
1. **影響範囲調査** - 使用箇所の全特定
2. **機能代替** - 代替実装または別ライブラリ
3. **テスト確認** - 全テストの実行確認
4. **ドキュメント更新** - 本文書の更新

## トラブルシューティング

### 依存関係競合の解決
```gradle
// 特定バージョンの強制指定
configurations.all {
    resolutionStrategy {
        force 'org.springframework:spring-core:6.1.10'
    }
}
```

### 依存関係の確認コマンド
```bash
# 依存関係ツリー表示
./gradlew dependencies

# 特定設定の依存関係確認
./gradlew dependencies --configuration runtimeClasspath

# 脆弱性チェック
./gradlew dependencyCheckAnalyze
```

## 更新履歴

| 日付 | 変更内容 | 理由 |
|------|----------|------|
| 2025-07-29 | Spring Boot 3.3.2に更新 | WebAPI基盤構築のため |
| 2025-07-29 | OpenAPI統合追加 | API仕様書自動生成のため |
| 2025-07-29 | json-schema-validator追加 | JSONバリデーション機能のため |

---

**注意**: この文書は依存関係の変更と共に定期的に更新する必要があります。