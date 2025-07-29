# StreamConverter WebAPI化 & 機能拡張ロードマップ

## 概要
StreamConverterをWebAPIとして機能させ、スキーマバリデーションとMDCログ機能を追加する開発計画

## 現在の状況（2025-07-27）
- 基本的なストリーム処理パイプライン機能完成
- SendHttpCommand（外部API通信）機能実装済み
- XMLバリデーション機能実装済み
- MDC対応のログパターン設定済み（logback.xml）

## フェーズ1: ログ機能強化 (1-2週間) ✅ 完了
### 1.1 MDC機能実装 ✅ 完了
- [x] ExecutionContextクラス作成 - ユニークID発行とコンテキスト管理
- [x] IContextAwareStreamCommandインターフェース追加
- [x] ContextPropagatingDecoratorクラス作成 - 既存コマンドの自動ラップ
- [x] ContextAwareStreamConverter実装 - マルチスレッド対応実行エンジン
- [x] MDC機能の単体テスト作成とデモンストレーション実装

### 1.2 スキーマバリデーション機能拡張
- [x] XMLバリデーション（ValidateCommand）- 実装済み
- [x] JSONスキーマバリデーション機能追加 - 実装済み
- [x] CSVスキーマバリデーション機能追加 - 実装済み
- [x] 統一的なバリデーション結果処理 - ValidationResult使用

## フェーズ2: WebAPI基盤構築 (2-3週間) ✅ 完了
### 2.1 Spring Boot統合 ✅ 完了
- [x] build.gradle.kts更新（Spring Boot依存関係追加）
- [x] WebAPIコントローラー作成
- [x] セキュリティ設定（CORS、認証基盤）

### 2.2 メインAPIエンドポイント実装 ✅ 完了
```
POST /api/v1/transform
{
  "inputFormat": "json|xml|csv",
  "outputFormat": "json|xml|csv", 
  "targetApi": "http://internal.api.com",
  "extractPath": "$.user.id | //xpath | columnName",
  "validationSchema": "schema.json|schema.xsd|schema.csv",
  "data": "..."
}
```

### 2.3 処理フロー実装
1. 入力データのスキーマバリデーション
2. 指定パスから値抽出→MDCに格納
3. SendHttpCommandで内部API送信
4. レスポンス受信
5. 指定形式への逆変換
6. クライアントに返却

## フェーズ3: 逆変換機能実装 (1-2週間)
### 3.1 逆変換コマンド作成
- [ ] ReverseJsonCommand: JSON形式への逆変換
- [ ] ReverseCsvCommand: CSV形式への逆変換  
- [ ] ReverseXmlCommand: XML形式への逆変換

### 3.2 双方向パイプライン設計
- [ ] ForwardPipeline: リクエスト変換パイプライン
- [ ] BackwardPipeline: レスポンス逆変換パイプライン
- [ ] PipelineConfig: 変換設定管理クラス

## フェーズ4: 運用機能実装 (1-2週間)
### 4.1 モニタリング・ログ強化
- [ ] Spring Boot Actuator導入
- [ ] メトリクス収集（Micrometer）
- [ ] 既存ログ機能との統合

### 4.2 エラーハンドリング・バリデーション
- [ ] 統一例外処理（@ControllerAdvice）
- [ ] 入力データバリデーション強化
- [ ] レート制限機能

## フェーズ5: テスト・デプロイメント (1週間)
### 5.1 テスト強化
- [ ] WebMvcTest追加
- [ ] 統合テスト拡張
- [ ] パフォーマンステスト

### 5.2 コンテナ化・デプロイ
- [ ] Dockerfile作成
- [ ] Docker Compose環境構築
- [ ] ヘルスチェック実装

## 技術スタック
- **Base Framework**: 既存StreamConverter + Spring Boot 3.x
- **Logging**: SLF4J + Logback + MDC
- **Validation**: JSON Schema Validator, XML Schema (XSD)
- **Security**: Spring Security + JWT（将来的に）
- **Documentation**: OpenAPI 3.0 (springdoc-openapi)
- **Container**: Docker
- **Testing**: 既存JUnit 5 + MockMvc + WebMvcTest

## 主要API設計（想定）
```
POST /api/v1/transform          # メイン変換API
GET  /api/v1/health             # ヘルスチェック
GET  /api/v1/metrics            # メトリクス情報
POST /api/v1/validate           # バリデーション専用API
```

## 見積もり総期間: 6-10週間

## 現在の実装済み機能
- ✅ コマンドパターンによるパイプライン処理
- ✅ CSV/JSON/XML項目抽出機能
- ✅ HTTP通信機能（SendHttpCommand）
- ✅ XMLバリデーション機能
- ✅ ログ機能（AbstractStreamCommand, LoggingDecorator）
- ✅ MDC対応ログパターン設定

## フェーズ6: アドバンス機能実装 (2-3週間)
### 6.1 キャッシュ機能
- [ ] バリデーション結果キャッシュ（Redis/Caffeine）
- [ ] 変換設定キャッシュ
- [ ] レスポンス時間改善（<100ms目標）

### 6.2 バッチ処理機能
- [ ] 一括変換API（複数データセット対応）
- [ ] 非同期処理キュー（Spring Async）
- [ ] 進捗状況通知機能

### 6.3 高度な変換機能
- [ ] テンプレートベース変換（Handlebars/Mustache）
- [ ] 条件分岐変換ロジック
- [ ] カスタムトランスフォーマープラグイン

## フェーズ7: セキュリティ強化 (1-2週間)
### 7.1 認証・認可
- [ ] JWT認証実装
- [ ] ロールベースアクセス制御（RBAC）
- [ ] API Key管理システム

### 7.2 セキュリティ監査
- [ ] 入力サニタイゼーション強化
- [ ] SQLインジェクション対策
- [ ] XSS防止機能
- [ ] セキュリティヘッダー設定

## フェーズ8: 運用監視強化 (1週間)
### 8.1 メトリクス・アラート
- [ ] カスタムメトリクス（変換成功率、応答時間）
- [ ] Prometheus/Grafana統合
- [ ] アラート機能（エラー率閾値超過時）

### 8.2 ログ解析
- [ ] 構造化ログ（JSON形式）
- [ ] ログ集約（ELK Stack対応）
- [ ] パフォーマンス分析ダッシュボード

## 長期展望（2025年Q4以降）
### V2.0: エンタープライズ対応
- [ ] マルチテナント機能
- [ ] SaaS化対応（組織管理、課金システム）
- [ ] 高可用性構成（冗長化、自動復旧）

### V3.0: AI統合
- [ ] 機械学習ベース変換パターン学習
- [ ] 自動スキーマ推論機能
- [ ] 異常データ検出・補正

## 技術的負債・改善項目
### コード品質
- [ ] テストカバレッジ90%以上達成
- [ ] 循環的複雑度改善（10以下に制限）
- [ ] API文書自動生成（Swagger UI）

### パフォーマンス
- [ ] メモリ使用量最適化
- [ ] GC停止時間最小化
- [ ] 並行処理性能向上

## 現在の進捗状況（2025-07-29更新）
### 完了済み（フェーズ1）
- ✅ MDC機能実装（ExecutionContext、ContextAwareStreamCommand）
- ✅ ContextPropagatingDecorator実装
- ✅ ContextAwareStreamConverter実装
- ✅ XMLバリデーション機能
- ✅ 基本的な単体テスト・デモ実装

### 進行中
- 🔄 Javadoc自動生成・管理システム
- 🔄 Pre-commit hook統合

### 新規完了（2025-07-29）
- ✅ ValueExtractionDecorator実装 - JSON/XML/CSV対応値抽出とMDC統合
- ✅ ValueExtractionDecoratorTest - 包括的な単体テスト
- ✅ ValueExtractionDemo - 実用的なデモとサンプルコード
- ✅ Spring Boot WebAPI基盤構築 - コントローラー、サービス、セキュリティ設定
- ✅ WebAPIエンドポイント実装 - /api/v1/transform, /api/v1/validate, /api/v1/health
- ✅ OpenAPI/Swagger統合 - API仕様書自動生成
- ✅ グローバル例外ハンドリング - 統一的なエラーレスポンス
- ✅ Spring Boot単体テスト - Controller・Service層テスト

## 次のマイルストーン
1. **今日完了**: Spring Boot WebAPI基盤構築 ✅
2. **今週内**: 逆変換機能実装（フェーズ3開始）
3. **来週**: 運用機能・監視強化（フェーズ4）

## 見積もり更新
- **フェーズ2-3**: 4-5週間（WebAPI基盤 + 逆変換機能）
- **フェーズ4-5**: 2-3週間（運用機能 + テスト・デプロイ）
- **フェーズ6-8**: 4-6週間（アドバンス機能 + セキュリティ + 監視）
- **総期間**: 10-14週間（約3-4ヶ月）