# StreamConverter 設定・構成ガイド

## 📋 概要

StreamConverterプロジェクトは、Spring Boot 3.4.7をベースとした設定管理システムを採用しています。本ガイドでは、アプリケーション設定、セキュリティ設定、パフォーマンス調整、および環境別設定について詳細に説明します。

## 🔧 基本設定

### アプリケーション設定ファイル

#### application.yml（開発環境用）

**ファイル**: `streamconverter-core/src/main/resources/application.yml`

```yaml
server:
  port: 8080
  
spring:
  application:
    name: stream-converter-api
  main:
    allow-bean-definition-overriding: true
    
logging:
  level:
    com.streamConverter: INFO
    org.springframework.web: INFO
    root: WARN
```

### 設定項目詳細

| 設定項目 | デフォルト値 | 説明 |
|---------|-------------|------|
| `server.port` | 8080 | WebAPIサーバーのポート番号 |
| `spring.application.name` | stream-converter-api | アプリケーション名 |
| `spring.main.allow-bean-definition-overriding` | true | Bean定義上書きの許可 |
| `logging.level.com.streamConverter` | INFO | StreamConverterライブラリのログレベル |
| `logging.level.org.springframework.web` | INFO | Spring Webのログレベル |
| `logging.level.root` | WARN | ルートロガーのログレベル |

## 🛡️ セキュリティ設定

### 本番環境用セキュリティ設定

**ファイル**: `streamconverter-core/src/main/resources/application-prod.properties`

#### XML セキュリティ設定

```properties
# XML外部エンティティ攻撃防止
security.xml.disable-external-entities=true
security.xml.disable-doctype-declarations=true
security.xml.load-external-dtd=false
```

#### XPath セキュリティ設定

```properties
# XPathインジェクション防止
security.xpath.validation.enabled=true
security.xpath.strict-mode=true
```

#### パストラバーサル防止

```properties
# パストラバーサル攻撃防止
security.path-traversal.prevention=true
security.path-traversal.allow-parent-references=false
security.file-access.restrict-to-workspace=true
```

#### 入力値検証

```properties
# 入力値検証強化
security.input.max-file-size=100MB
security.input.allowed-file-extensions=.json,.xml,.csv,.txt
security.input.validate-encoding=true
```

#### セキュリティヘッダー

```properties
# セキュリティヘッダー設定
security.headers.content-type-options=nosniff
security.headers.frame-options=DENY
security.headers.xss-protection=1; mode=block
```

## 📊 パフォーマンス設定

### メモリ管理

```properties
# メモリ管理設定
performance.memory.max-heap-usage=80
performance.memory.gc-after-large-operations=true
```

### ストリーミング処理

```properties
# ストリーミング処理設定
performance.streaming.buffer-size=64KB
performance.streaming.enable-compression=false
```

### タイムアウト設定

```properties
# タイムアウト設定（ミリ秒）
performance.timeout.command-execution=300000    # 5分
performance.timeout.file-processing=600000      # 10分
```

### Gradle 並列ビルド設定

- 既定では `org.gradle.parallel` を無効（false）に戻し、Gradle Build Server や VS Code の Gradle インポートで発生していた断続的な失敗を防いでいます。
- 並列ビルドを再度有効にすると、構成フェーズの副作用を持つプラグインやビルドスクリプトが競合し、IDE での同期失敗やテスト結果の取りこぼしが発生するリスクがあります。必要な場面でのみ有効化してください。

#### VS Code で並列実行を再度有効にする

1. コマンドパレット（`Ctrl`+`Shift`+`P`）で「Preferences: Open Settings (JSON)」を開きます。
2. 以下の設定を追記し、Gradle インポート時に `-Dorg.gradle.parallel=true` を付与します。

   ```jsonc
   {
     "java.import.gradle.arguments": [
       "-Dorg.gradle.parallel=true"
     ]
   }
   ```

3. プロジェクトを再インポートし、並列実行で問題がないことを確認します。問題が再発した場合は設定を削除して既定値（false）に戻してください。

## 📈 ログ・監査設定

### セキュリティログ設定

```properties
# セキュリティイベントログ
logging.security.enabled=true
logging.security.level=WARN
logging.security.file=logs/security.log

# 機密データ保護
logging.mask-sensitive-data=true
logging.exclude-request-body=true
logging.exclude-response-body=true
```

### 監査ログ設定

```properties
# 監査ログ設定
audit.enabled=true
audit.log-file=logs/audit.log
audit.include-user-actions=true
audit.include-system-events=true
```

### パフォーマンス監視

```properties
# パフォーマンス監視設定
monitoring.enabled=true
monitoring.metrics.memory=true
monitoring.metrics.processing-time=true
monitoring.metrics.throughput=true
```

## 🏗️ 環境別設定

### プロファイル管理

StreamConverterは以下の環境プロファイルをサポートします：

#### 開発環境（dev）
- **デフォルト設定**: `application.yml`
- **特徴**: デバッグ情報有効、詳細ログ出力
- **起動**: `java -jar app.jar --spring.profiles.active=dev`

#### 本番環境（prod）
- **設定ファイル**: `application-prod.properties`
- **特徴**: セキュリティ強化、監査ログ有効
- **起動**: `java -jar app.jar --spring.profiles.active=prod`

#### テスト環境（test）
- **設定**: 開発環境ベース + テスト専用設定
- **特徴**: インメモリ処理、モック使用
- **起動**: `java -jar app.jar --spring.profiles.active=test`

### 環境変数による設定

重要な設定は環境変数でオーバーライド可能です：

```bash
# サーバーポート
export SERVER_PORT=9090

# ログレベル
export LOGGING_LEVEL_COM_STREAMCONVERTER=DEBUG

# セキュリティ設定
export SECURITY_XML_DISABLE_EXTERNAL_ENTITIES=true

# パフォーマンス設定
export PERFORMANCE_MEMORY_MAX_HEAP_USAGE=90
```

## 📦 依存関係設定

### 主要ライブラリバージョン

**ファイル**: `build.gradle.kts`

```kotlin
plugins {
    id("org.springframework.boot") version "3.4.7"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    // Core Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // JSON処理
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    
    // Netty
    implementation("io.netty:netty-all:4.1.118.Final")
    
    // CSV処理
    implementation("com.opencsv:opencsv:5.12.0")
    
    // JSON Schema Validation (2.x introduces SchemaRegistry/Error APIs)
    implementation("com.networknt:json-schema-validator:2.0.0")
    
    // ユーティリティ
    implementation("com.google.guava:guava:33.4.0-jre")
}
```

### 依存関係の更新

依存関係更新時の注意点：

1. **セキュリティ更新の優先**
   - CVE対応のため定期的な更新

2. **互換性テスト**
   - メジャーバージョン更新前の包括的テスト
   - `json-schema-validator` 2.x では `SchemaRegistry`/`Error` API に移行したため、1.x からのアップグレード時はコンパイルエラー（"symbol not found"）が発生しないか確認してください。

3. **パフォーマンス検証**
   - 更新後のベンチマーク実行

## 🚀 WebAPI設定

### Spring Boot WebFlux統合

**実装**: `StreamConverterWebApplication.java`

```java
@SpringBootApplication
@ComponentScan(basePackages = "com.streamConverter")
public class StreamConverterWebApplication {
    public static void main(String[] args) {
        SpringApplication.run(StreamConverterWebApplication.class, args);
    }
}
```

### エンドポイント設定

| エンドポイント | メソッド | 説明 |
|---------------|---------|------|
| `/api/v1/stream/health` | GET | ヘルスチェック |
| `/api/v1/stream/csv/extract` | POST | CSV列抽出 |
| `/api/v1/stream/json/extract` | POST | JSONパス抽出 |
| `/api/v1/stream/process` | POST | パイプライン処理 |

### WebAPI設定カスタマイズ

```yaml
# application.yml - WebAPI設定拡張
server:
  port: 8080
  servlet:
    context-path: /api/v1
  compression:
    enabled: true
    mime-types: application/json,application/xml,text/csv

spring:
  webflux:
    multipart:
      max-in-memory-size: 1MB
      max-disk-usage-per-part: 10MB
```

## 🔧 カスタム設定

### コマンドファクトリ設定

```properties
# EnhancedCommandFactory設定
command.factory.optimization.enabled=true
command.factory.cache.size=100
command.factory.performance.monitoring=true
```

### バリデーション設定

```properties
# バリデーション設定
validation.csv.strict-mode=true
validation.json.schema-cache.enabled=true
validation.xml.xxe-protection=true
```

### ベンチマーク設定

```properties
# PerformanceAnalyzer設定
benchmark.enabled=true
benchmark.detailed-reports=true
benchmark.memory-tracking=true
benchmark.output-directory=benchmark-results
```

## 🛠️ 開発者向け設定

### IDEデバッグ設定

```yaml
# application-dev.yml
logging:
  level:
    com.streamConverter: DEBUG
    org.springframework: DEBUG
    
spring:
  devtools:
    restart:
      enabled: true
    livereload:
      enabled: true
```

### テスト設定

```properties
# application-test.properties
spring.test.context.cache.maxSize=100
test.performance.quick-mode=true
test.memory.limit=512MB
```

## 📚 設定管理ベストプラクティス

### 1. 機密情報の管理

```bash
# 機密情報は環境変数で管理
export DATABASE_PASSWORD=secure_password
export API_SECRET_KEY=your_secret_key
```

### 2. 設定の外部化

```yaml
# 設定の外部ファイル化
spring:
  config:
    import: "optional:file:./config/custom.properties"
```

### 3. プロファイル戦略

```bash
# 複数プロファイルの組み合わせ
java -jar app.jar --spring.profiles.active=prod,monitoring,audit
```

### 4. 設定値検証

```java
@ConfigurationProperties(prefix = "streamconverter")
@Validated
public class StreamConverterProperties {
    @NotNull
    @Min(1024)
    private Integer bufferSize;
    
    @NotNull
    @Pattern(regexp = "^(DEBUG|INFO|WARN|ERROR)$")
    private String logLevel;
}
```

## 🔍 トラブルシューティング

### よくある設定問題

1. **ポート競合**
   ```bash
   # ポート使用状況確認
   netstat -tulpn | grep :8080
   
   # 代替ポートで起動
   java -jar app.jar --server.port=8081
   ```

2. **メモリ不足**
   ```bash
   # JVM ヒープサイズ調整
   java -Xmx2g -Xms1g -jar app.jar
   ```

3. **ログ出力問題**
   ```yaml
   # ログ設定の確認と調整
   logging:
     file:
       name: logs/application.log
     level:
       com.streamConverter: DEBUG
   ```

## 📚 関連ドキュメント

- **[セキュリティ分析](../security/SECURITY_ANALYSIS.md)** - セキュリティ設定の詳細
- **[アーキテクチャ](../architecture/ARCHITECTURE.md)** - システム全体構成
- **[テスト戦略](TESTING.md)** - テスト設定とベストプラクティス
- **[バリデーション機能](../features/VALIDATION.md)** - バリデーション設定

## 🔄 更新履歴

- **2025-08-16**: 初版作成 - 基本設定、セキュリティ設定、パフォーマンス設定の文書化