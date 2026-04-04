# StreamConverter ログ出力ルール

## 現在のログ実装の分析結果

### 1. ログフレームワーク
- **採用**: SLF4J + Logback
- **設定ファイル**: `src/main/resources/logback.xml`
- **ログ出力先**: コンソール（STDOUT）+ ファイル（logs/myapp.log）

### 2. 現在のログ出力状況

#### SLF4J使用クラス (推奨)
- `AbstractStreamCommand` - デバッグレベルで実行開始/終了ログ
- `FixedStaXPathHandler` - デバッグレベルでXPath正規化ログ
- `DatabaseFetchRule` - 詳細未確認
- `LoggingTest` - テストクラス

#### System.out/err使用クラス (要改善)
- `Main` - アプリケーション実行メッセージ
- `MemoryEfficiencyTest` - テスト実行結果
- `ValidateCommand` - 詳細未確認

## 統一されたログ出力ルール

### 1. ログレベル定義

#### ERROR
- **用途**: システムエラー、例外、致命的な問題
- **例**: IOException、XMLStreamException、設定エラー
- **フォーマット**: `log.error("Error processing XML: {}", e.getMessage(), e);`

#### WARN
- **用途**: 警告、回復可能な問題、非推奨機能使用
- **例**: 無効なパラメータ、パフォーマンス警告
- **フォーマット**: `log.warn("Invalid parameter '{}', using default value", param);`

#### INFO
- **用途**: 重要なビジネスロジック、処理開始/完了、設定情報
- **例**: 処理パイプライン実行、変換完了、システム起動
- **フォーマット**: `log.info("Processing {} records completed in {} ms", count, time);`

#### DEBUG
- **用途**: 詳細な実行トレース、デバッグ情報
- **例**: メソッド実行開始/終了、内部状態変更
- **フォーマット**: `log.debug("Method {} started with parameters: {}", method, params);`

#### TRACE
- **用途**: 最詳細レベル、ループ処理、データ変換過程
- **例**: 個別レコード処理、XMLイベント処理
- **フォーマット**: `log.trace("Processing record {}: {}", index, record);`

### 2. ログ出力パターン

#### 処理開始/終了
```java
log.info("Starting {} processing", operationType);
// 処理実行
log.info("Completed {} processing: {} records in {} ms", operationType, count, duration);
```

#### エラーハンドリング
```java
try {
    // 処理
} catch (SpecificException e) {
    log.error("Failed to process {}: {}", resourceName, e.getMessage(), e);
    throw new ProcessingException("Processing failed", e);
}
```

#### パフォーマンス情報
```java
long startTime = System.currentTimeMillis();
// 処理実行
long duration = System.currentTimeMillis() - startTime;
log.info("Processing completed: {} records in {} ms (avg: {} ms/record)", 
         count, duration, duration / count);
```

### 3. ログ出力対象

#### 必須ログ出力
- StreamConverter実行開始/終了
- 各コマンド実行開始/終了
- 例外発生時のエラー情報
- 重要な設定変更

#### 推奨ログ出力
- パフォーマンス測定結果
- 処理件数・処理時間
- 設定値の読み込み結果
- 外部システム連携状況

#### 避けるべきログ出力
- 機密情報（パスワード、個人情報）
- 過度に詳細な内部状態
- 高頻度ループ内での詳細ログ（TRACEレベル以外）

### 4. 例外ログ出力ルール

#### スタックトレース付き
```java
log.error("Critical system error occurred", exception);
```

#### メッセージのみ
```java
log.warn("Validation failed: {}", exception.getMessage());
```

#### 例外の再スロー
```java
catch (IOException e) {
    log.error("IO error in {}: {}", methodName, e.getMessage(), e);
    throw new ProcessingException("Processing failed", e);
}
```

## 実装ガイドライン

### 1. ロガーの宣言
```java
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
```

### 2. System.out/err からの移行
- **System.out.println()** → **log.info()**
- **System.err.println()** → **log.error()**
- **デバッグ用出力** → **log.debug()**

### 3. 設定ファイル更新
- ルートレベルをINFOに設定
- 開発時はDEBUGレベルに変更可能
- 本番環境ではWARN以上を推奨

### 4. パフォーマンス考慮
- 高頻度ログにはレベルチェック使用
```java
if (log.isDebugEnabled()) {
    log.debug("Expensive operation result: {}", computeExpensiveValue());
}
```

## 移行計画

### フェーズ1: 基盤整備
- ログ設定ファイル最適化
- 共通ユーティリティクラス作成

### フェーズ2: コアクラス更新
- AbstractStreamCommand
- StreamConverter
- 各NavigateCommand

### フェーズ3: サンプル・デモクラス更新
- examples パッケージ
- demo パッケージ
- テストクラス

### フェーズ4: 検証・最適化
- ログ出力量の調整
- パフォーマンス影響確認
- 本番環境用設定作成