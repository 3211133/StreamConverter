# StreamConverter Docker コンテナ化ガイド

## 概要

StreamConverterをDockerコンテナとして実行することで、環境差異を排除し、一貫した実行環境を提供します。

## 利用可能なコンテナイメージ

### 1. Production Image (`streamconverter/web`)
- 軽量なJRE Alpine Linuxベース
- Web APIサーバーとして動作
- ヘルスチェック機能付き
- セキュリティ強化（非rootユーザー実行）

### 2. Development Image (`streamconverter/dev`)
- フル開発環境（JDK + Gradle）
- リモートデバッグ対応（ポート5005）
- ホットリロード対応

## クイックスタート

### 基本的な実行

```bash
# 1. Docker イメージをビルド
./gradlew dockerBuild

# 2. サービスを起動
./gradlew dockerRun

# 3. ヘルスチェック
curl http://localhost:8080/actuator/health

# 4. サービスを停止
./gradlew dockerStop
```

### 開発環境

```bash
# 開発環境を起動（デバッグポート付き）
./gradlew dockerDev

# Web API: http://localhost:8081
# Debug port: 5005
```

### 本番環境

```bash
# 本番環境を起動（Nginx + HTTPS）
./gradlew dockerProd

# Web API: http://localhost:80
```

### 監視環境

```bash
# 監視スタックを起動
./gradlew dockerMonitor

# Prometheus: http://localhost:9090
# Grafana: http://localhost:3000 (admin/admin123)
```

## Docker Compose プロファイル

### デフォルト
- streamconverter-web
- redis
- postgres

### development
- 上記 + streamconverter-dev（デバッグ用）

### production
- 上記 + nginx（リバースプロキシ）

### monitoring
- 上記 + prometheus + grafana

## サービス構成

| サービス | ポート | 説明 |
|---------|-------|------|
| streamconverter-web | 8080 | メインAPI |
| streamconverter-dev | 8081, 5005 | 開発用API + デバッグ |
| redis | 6379 | キャッシュ・セッション |
| postgres | 5432 | メタデータ・設定 |
| nginx | 80, 443 | リバースプロキシ |
| prometheus | 9090 | メトリクス収集 |
| grafana | 3000 | 可視化ダッシュボード |

## 永続化データ

### Named Volumes
- `streamconverter-redis-data`: Redisデータ
- `streamconverter-postgres-data`: PostgreSQLデータ
- `streamconverter-prometheus-data`: Prometheusメトリクス
- `streamconverter-grafana-data`: Grafanaダッシュボード
- `streamconverter-gradle-cache`: Gradleキャッシュ

### Bind Mounts
- `./data`: 入出力データファイル
- `./logs`: アプリケーションログ

## 環境設定

### 環境変数

#### Java/Spring Boot
```bash
JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseContainerSupport"
SPRING_PROFILES_ACTIVE=production
```

#### データベース接続
```bash
POSTGRES_DB=streamconverter
POSTGRES_USER=streamconverter
POSTGRES_PASSWORD=streamconverter123
```

### リソース制限

#### メモリ要件
- **Production**: 最小512MB、推奨2GB
- **Development**: 最小1GB、推奨3GB
- **Large Data Processing**: 3GB以上（5GBベンチマーク用）

#### CPU要件
- **Production**: 最小1コア、推奨2コア
- **Development**: 推奨2コア以上

## 利用可能なGradleタスク

```bash
# Docker関連タスク一覧
./gradlew tasks --group docker

# ビルド
./gradlew dockerBuild        # イメージビルド
./gradlew dockerClean        # リソースクリーンアップ

# 実行
./gradlew dockerRun          # 基本構成で起動
./gradlew dockerDev          # 開発環境で起動
./gradlew dockerProd         # 本番環境で起動
./gradlew dockerMonitor      # 監視環境で起動
./gradlew dockerStop         # 全サービス停止
```

## 直接Docker Composeコマンド

```bash
# 基本操作
docker compose up -d                    # バックグラウンド起動
docker compose down                     # 停止・削除
docker compose logs -f                  # ログ監視

# プロファイル指定
docker compose --profile development up -d
docker compose --profile production up -d
docker compose --profile monitoring up -d

# サービス個別操作
docker compose up -d redis postgres       # 特定サービスのみ
docker compose restart streamconverter-web
docker compose logs streamconverter-web
```

## セキュリティ

### コンテナセキュリティ
- 非rootユーザーで実行
- 最小権限原則
- セキュリティヘッダー設定（Nginx）
- レート制限機能

### ネットワークセキュリティ
- 専用Dockerネットワーク
- 内部通信の暗号化
- ポート制限

### データ保護
- 設定ファイルは環境変数で管理
- 機密情報はDocker Secretsまたは外部KMS推奨

## トラブルシューティング

### 一般的な問題

#### メモリ不足
```bash
# コンテナのメモリ使用量確認
docker stats

# 大容量処理用にメモリ増加
export JAVA_OPTS="-Xms1g -Xmx4g"
docker compose up -d
```

#### ポート競合
```bash
# ポート使用状況確認
netstat -tulpn | grep :8080

# docker-compose.override.yml でポートを上書き
cat <<'EOF' > docker-compose.override.yml
services:
  streamconverter-web:
    ports:
      - "8081:8080"
EOF

docker compose up -d
```

#### データベース接続エラー
```bash
# PostgreSQL接続確認
docker compose exec postgres psql -U streamconverter -d streamconverter -c "SELECT 1;"

# ログ確認
docker compose logs postgres
```

### ログ確認

```bash
# 全サービスのログ
docker compose logs -f

# 特定サービスのログ
docker compose logs -f streamconverter-web

# エラーログのみ
docker compose logs --tail=100 streamconverter-web | grep ERROR
```

### パフォーマンス最適化

#### Docker設定
```bash
# Docker Desktop - Settings - Resources
# Memory: 4GB以上
# CPU: 2コア以上
# Swap: 1GB以上
```

#### JVM最適化
```bash
# プロダクション最適化
JAVA_OPTS="-Xms1g -Xmx2g -XX:+UseG1GC -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# 大容量データ処理用
JAVA_OPTS="-Xms2g -Xmx4g -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler"
```

## CI/CD統合

### GitHub Actions例
```yaml
# .github/workflows/docker.yml
- name: Build Docker images
  run: ./gradlew dockerBuild

- name: Run integration tests
  run: |
    ./gradlew dockerRun
    # テスト実行
    ./gradlew dockerStop
```

### Jenkins例
```groovy
pipeline {
    stages {
        stage('Docker Build') {
            steps {
                sh './gradlew dockerBuild'
            }
        }
        stage('Deploy') {
            steps {
                sh './gradlew dockerProd'
            }
        }
    }
}
```

## 本番デプロイ考慮事項

### スケーリング
- Kubernetes対応（Deployment/Service定義）
- Docker Swarm対応
- ロードバランサー設定

### 監視・ロギング
- Prometheus/Grafanaメトリクス
- 集中ログ管理（ELK Stack）
- APM統合（New Relic, Datadog）

### バックアップ・復旧
- データベースバックアップ自動化
- 設定ファイルのバージョン管理
- 災害復旧計画

## サポート

問題が発生した場合は、以下の情報と共にIssueを作成してください：

1. 実行したコマンド
2. エラーメッセージ
3. 環境情報（OS、Dockerバージョン）
4. ログファイル（`docker compose logs`）