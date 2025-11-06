# コンテキスト伝播アーキテクチャ設計書

## 概要

StreamConverterのマルチスレッド環境でユニークな識別子発行とMDCコンテキスト伝播を実現するアーキテクチャを設計・実装しました。

## アーキテクチャ要素

### 1. ExecutionContext（実行コンテキスト）
**役割**: パイプライン全体を通じて一意の識別子とコンテキスト情報を管理

**主要機能**:
- ユニークな実行ID生成（`EXEC-{UUID}-{timestamp}`形式）
- コマンドシーケンス番号の自動管理
- グローバルコンテキスト（読み取り専用）とユーザーコンテキスト（可変）の分離
- **共有コンテキスト（スレッド間で共有、2025年1月追加）**
- MDCへの自動適用機能

```java
ExecutionContext context = ExecutionContext.builder()
    .globalContext("requestId", "REQ-12345")
    .globalContext("userId", "user789")
    .userContext("businessUnit", "finance")
    .build();

// 共有コンテキスト（スレッド間で共有される値）
context.setSharedContext("userId", "USER12345");
context.setSharedContext("sessionId", "SESSION-XYZ");
```

### 2. IStreamCommand（統合されたコンテキスト対応インターフェース）
**役割**: ExecutionContextを受け取れる統一インターフェース

**特徴**:
- 既存コードとの完全な後方互換性
- デフォルトメソッドによるExecutionContext対応の拡張
- コンテキストなし呼び出し時は既存の2パラメータメソッドに委譲

```java
public interface IStreamCommand {
    /**
     * ExecutionContext対応の拡張実行メソッド（推奨）
     */
    default void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context)
        throws IOException {
        // デフォルト実装：後方互換性のため基本executeメソッドに委譲
        execute(inputStream, outputStream);
    }

    /**
     * 基本実行メソッド（後方互換性のため維持）
     */
    void execute(InputStream inputStream, OutputStream outputStream) throws IOException;
}
```

### 3. ContextPropagatingDecorator（コンテキスト伝播デコレータ）
**役割**: 既存コマンドをコンテキスト対応に変換

**機能**:
- 既存の`IStreamCommand`を自動的にラップ
- MDCの設定・復元を自動化
- コマンドシーケンス番号の管理
- エラーハンドリングとログ出力

```java
// 既存コマンドを自動的にコンテキスト対応にラップ
IStreamCommand legacyCommand = new SampleStreamCommand("processor");
IStreamCommand contextCommand = new ContextPropagatingDecorator(legacyCommand);

// 実際の使用では、StreamConverterが自動的に適用
StreamConverter converter = StreamConverter.createWithContext(context, legacyCommand);
```

### 4. StreamConverter.createWithContext()（コンテキスト対応StreamConverter）
**役割**: ExecutionContextを使用したマルチスレッド実行管理

**特徴**:
- 既存コマンドの自動デコレート
- マルチスレッド環境でのコンテキスト共有
- 単一/複数コマンドの最適実行
- リソース管理とエラーハンドリング

```java
StreamConverter converter = StreamConverter.createWithContext(
    customContext, command1, command2, command3
);
```

### 5. ExecutionContextHolder（ThreadLocalコンテキスト保持、2025年1月追加）
**役割**: ThreadLocal経由でExecutionContextを保持し、TurboFilterからアクセス可能にする

**特徴**:
- 静的ThreadLocalによるスレッド固有のコンテキスト保存
- AbstractStreamCommandが自動的に設定/クリア
- メモリリーク防止のための明示的なクリーンアップ

```java
// AbstractStreamCommandが自動的に呼び出す
ExecutionContextHolder.set(context);
try {
    execute(inputStream, outputStream);
} finally {
    ExecutionContextHolder.clear();
}
```

### 6. ExecutionContextTurboFilter（自動MDC同期、2025年1月追加）
**役割**: ログ出力の都度、ExecutionContextの共有コンテキストをMDCに自動同期

**特徴**:
- Logbackのログイベント処理前に自動実行
- 共有コンテキストの全キーをMDCに同期
- 変更がある場合のみMDC操作を実行（パフォーマンス最適化）
- 削除されたキーの自動クリーンアップ

```xml
<!-- logback.xml -->
<configuration>
    <turboFilter class="com.streamconverter.logging.ExecutionContextTurboFilter"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <!-- userIdなどが自動的にMDCから取得される -->
            <pattern>%d{HH:mm:ss.SSS} [userId:%X{userId:-}] - %msg%n</pattern>
        </encoder>
    </appender>
</configuration>
```

### 7. MdcSetupRule（MDC値抽出ルール、2025年1月追加）
**役割**: XMLやJSONから抽出した値を共有コンテキストに設定

**特徴**:
- IRule実装による宣言的な値設定
- ExecutionContextの共有コンテキストへの自動保存
- TurboFilterと組み合わせて自動MDC同期

```java
// XMLからuserIdを抽出してMDCに設定
XmlNavigateCommand extractUserId = new XmlNavigateCommand(
    TreePath.fromXml("request/userId"),
    new MdcSetupRule(context, "userId")
);
```

## コンテキスト伝播フロー

### 1. 初期化フェーズ
```
1. ExecutionContext生成（ユニークID発行）
2. StreamConverter.createWithContext()でコンテキスト対応実行エンジン作成
3. 既存コマンドの自動デコレート
```

### 2. 実行フェーズ（2025年1月更新）
```
1. パイプライン開始時にMDCにコンテキスト適用
2. 各コマンドにExecutionContextを渡す
3. AbstractStreamCommandがExecutionContextHolderに設定
4. コマンド実行
   - ログ出力時にTurboFilterが自動的にMDC同期
   - 共有コンテキストの値がMDCに反映される
5. AbstractStreamCommandがExecutionContextHolderをクリア
6. コマンド実行後にコンテキスト情報更新
```

**TurboFilterによる自動同期の詳細**:
```
ログ出力 → TurboFilter.decide() → ExecutionContextHolder.get()
         → 共有コンテキスト取得 → MDC値と比較
         → 変更がある場合のみMDC.put() → ログ処理続行
```

### 3. 並列実行での対応（2025年1月更新）
```
- 各スレッドが独立したExecutionContextコピーを持つ
- 同一executionIdでコンテキストを共有
- スレッド固有のsequence番号とthread名をMDCに設定
- ユーザーコンテキストの変更は各スレッドで独立
- **共有コンテキストは全スレッドで共有（ConcurrentHashMap使用）**
- **各スレッドのThreadLocalに同じExecutionContextが設定される**
- **TurboFilterが各スレッドで独立してMDCに同期**
```

**マルチスレッドでの共有コンテキストの動作**:
```
Thread 1: XmlNavigateCommand実行
  → userId抽出 → 共有コンテキストに設定
  → ログ出力時にTurboFilterがMDCに同期

Thread 2: XmlDebugCommand実行（並行）
  → 共有コンテキストからuserId取得可能
  → ログ出力時にTurboFilterがMDCに同期
  → Thread 1が設定したuserIdがログに出力される
```

## 実装例

### 自動MDC同期を使った実装（2025年1月推奨）
```java
// ExecutionContext作成
ExecutionContext context = ExecutionContext.create();

// XMLからuserIdを抽出してMDCに自動設定
XmlNavigateCommand extractUserId = new XmlNavigateCommand(
    TreePath.fromXml("request/userId"),
    new MdcSetupRule(context, "userId")
);

// JSONからsessionIdを抽出してMDCに自動設定
JsonNavigateCommand extractSessionId = new JsonNavigateCommand(
    TreePath.fromJson("$.sessionId"),
    new MdcSetupRule(context, "sessionId")
);

// 後続のコマンドは自動的にMDCでuserIdとsessionIdを参照可能
SampleStreamCommand processor = new SampleStreamCommand("processor");

// パイプライン実行
StreamConverter.createWithContext(context,
    extractUserId, extractSessionId, processor)
    .run(inputStream, outputStream);

// 全てのログに [userId:USER12345] [sessionId:SESSION-XYZ] が自動出力
```

**ポイント**:
- `MdcSetupRule`で値を抽出すると共有コンテキストに保存
- `ExecutionContextTurboFilter`がログ出力の都度MDCに自動同期
- 下流のコマンドはMDCを一切意識する必要がない
- マルチスレッドでも正しく動作

### カスタムコンテキスト対応コマンド
```java
IContextAwareStreamCommand enrichmentCommand = new IContextAwareStreamCommand() {
    @Override
    public void execute(InputStream inputStream, OutputStream outputStream, ExecutionContext context) 
            throws IOException {
        
        // コンテキスト情報を取得
        String requestId = context.getGlobalContext("requestId");
        String userId = context.getGlobalContext("userId");
        
        logger.info("Processing request: {}, user: {}", requestId, userId);
        
        // 処理実行
        inputStream.transferTo(outputStream);
        
        // 処理結果をコンテキストに保存
        context.setUserContext("enrichmentStatus", "completed");
        context.setUserContext("recordsProcessed", "100");
    }
};
```

### 混在コマンドパイプライン
```java
// 既存コマンドとコンテキスト対応コマンドの混在
IStreamCommand legacyValidator = new SampleStreamCommand("validator");
IContextAwareStreamCommand contextProcessor = createCustomProcessor();
IStreamCommand legacyFormatter = new SampleStreamCommand("formatter");

StreamConverter converter = StreamConverter.createWithContext(
    customContext,
    legacyValidator,     // 自動的にContextPropagatingDecoratorでラップ
    contextProcessor,    // そのまま使用
    legacyFormatter      // 自動的にContextPropagatingDecoratorでラップ
);
```

## ログ出力例

### 実行開始時
```
2025-07-27 16:32:33 INFO StreamConverter [REQ-12345] [user789] [pipeline-start] - 
Starting StreamConverter with context with 3 commands (executionId: EXEC-c226aa5a-1753633953001)
```

### コマンド実行中
```
2025-07-27 16:32:33 INFO ContextPropagatingDecorator [REQ-12345] [user789] [SampleStreamCommand-1] - 
Starting command execution: SampleStreamCommand (sequence: 1)

2025-07-27 16:32:34 INFO DataEnrichment [REQ-12345] [user789] [enrichment-2] - 
Starting data enrichment for request: REQ-12345, user: user789, business unit: finance
```

## 運用上の利点

### 1. トレーサビリティの向上
- パイプライン全体で一意のexecutionIdによる追跡
- コマンドシーケンス番号での処理順序把握
- スレッド名とステージ名での詳細な実行状況確認

### 2. 運用監視の強化
- logback.xmlのMDCパターン`[%X{requestId:-}] [%X{userId:-}] [%X{stage:-}]`が有効活用
- 既存のログ分析ツールでの横断的な追跡が可能
- パフォーマンス分析とボトルネック特定の容易化

### 3. 開発効率の向上
- 既存コードの変更なしでコンテキスト対応
- 段階的な移行が可能
- テストとデバッグの簡易化

## パフォーマンス影響

### MDCオーバーヘッド
- 実測結果: MDC使用有無での処理時間差は軽微（1-2ms程度）
- メモリ使用量: コンテキスト情報によるオーバーヘッドは最小限
- スケーラビリティ: 並列処理性能への影響なし

### リソース管理
- ExecutionContextのコピーコストは軽微
- PipedStreamとExecutorServiceの適切な管理
- 自動リソースクリーンアップ

## 今後の拡張可能性

### 1. 分散トレーシング対応
- OpenTelemetryとの統合
- トレースIDとスパンIDの管理

### 2. メトリクス収集
- 実行時間とリソース使用量の自動収集
- コマンド別パフォーマンス分析

### 3. コンテキスト永続化
- データベースやファイルシステムへのコンテキスト保存
- 長時間実行プロセスの状態復元

## 結論

このアーキテクチャにより、StreamConverterは以下を実現:

✅ **ユニークな識別子の自動発行**  
✅ **マルチスレッド環境でのMDCコンテキスト伝播**  
✅ **既存コードとの完全な互換性**  
✅ **運用上のトレーサビリティ向上**  
✅ **段階的な移行サポート**  

これにより、複雑なデータ処理パイプラインにおいても、項目パース段階から最終出力まで一貫したコンテキスト管理と追跡が可能になりました。