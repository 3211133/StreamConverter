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
- [x] StreamConverter.createWithContext()実装 - マルチスレッド対応実行エンジン
- [x] MDC機能の単体テスト作成とデモンストレーション実装

### 1.2 スキーマバリデーション機能拡張
- [x] XMLバリデーション（ValidateCommand）- 実装済み
- [ ] JSONスキーマバリデーション機能追加
- [ ] CSVスキーマバリデーション機能追加
- [ ] 統一的なバリデーション結果処理

## フェーズ2: WebAPI基盤構築 (2-3週間)
### 2.1 Spring Boot統合
- [ ] build.gradle.kts更新（Spring Boot依存関係追加）
- [ ] WebAPIコントローラー作成
- [ ] セキュリティ設定（CORS、認証基盤）

### 2.2 メインAPIエンドポイント実装
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

## 次のマイルストーン
1. ValueExtractionDecoratorの実装完了
2. JSONスキーマバリデーション機能追加
3. Spring Boot統合開始