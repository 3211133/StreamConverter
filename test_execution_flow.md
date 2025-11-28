# ExecutionContext実行フロー追跡

このドキュメントは、ExecutionContext関連の各関数が実際にどのタイミングで呼ばれるかを追跡します。

## テストケース: 単一コマンド実行

```java
ExecutionContext context = ExecutionContext.create();
SampleStreamCommand command = new SampleStreamCommand("test");
StreamConverter converter = StreamConverter.createWithContext(context, command);
converter.run(inputStream, outputStream);
```

## 実行フロー

### 1. ExecutionContext.create()
**呼び出し**: アプリケーション起動時
**場所**: `ExecutionContext.java:60-62`

```java
public static ExecutionContext create() {
    return new Builder().build();
}
```

**内部で呼ばれる関数**:
- `Builder()` コンストラクタ
- `Builder.generateExecutionId()` - ユニークID生成
- `Builder.build()` - ExecutionContextインスタンス生成

**生成される値**:
```
executionId: "EXEC-c226aa5a-1753633953001"
startTime: Instant.now()
commandSequence: AtomicInteger(0)
globalContext: {}
userContext: {}
sharedContext: ConcurrentHashMap()
```

---

### 2. StreamConverter.createWithContext()
**呼び出し**: コンバーター生成時
**場所**: `StreamConverter.java:132-138`

```java
public static StreamConverter createWithContext(
    ExecutionContext context, IStreamCommand... commands) {
    Objects.requireNonNull(context, "context cannot be null");
    StreamConverter converter = new StreamConverter(commands);
    converter.defaultContext = context;  // ← コンテキストを保存
    return converter;
}
```

**効果**: StreamConverterインスタンスにExecutionContextが紐付けられる

---

### 3. StreamConverter.run()
**呼び出し**: パイプライン実行開始
**場所**: `StreamConverter.java:176-182`

```java
public List<CommandResult> run(InputStream inputStream, OutputStream outputStream) {
    ExecutionContext contextToUse =
        defaultContext != null ? defaultContext : ExecutionContext.create();
    return run(inputStream, outputStream, contextToUse);
}
```

**次に呼ぶ**: `run(InputStream, OutputStream, ExecutionContext)`

---

### 4. StreamConverter.run(3パラメータ版)
**場所**: `StreamConverter.java:193-212`

```java
public List<CommandResult> run(
    InputStream inputStream, OutputStream outputStream, ExecutionContext context) {

    // 【重要1】パイプライン開始時にMDC設定
    context.applyToMDCWithStage("pipeline-start");  // ★この関数が呼ばれる

    LOG.info("Starting StreamConverter with {} commands (executionId: {})",
        commands.size(), context.getExecutionId());

    // 【重要2】マルチコマンド実行
    return executeMultipleCommandsWithMDC(inputStream, outputStream, context);
}
```

---

### 5. ExecutionContext.applyToMDCWithStage()
**呼び出し**: パイプライン開始時、各コマンド実行前
**場所**: `ExecutionContext.java:229-232`

```java
public void applyToMDCWithStage(String stageName) {
    applyToMDC();  // ★この関数を呼ぶ
    MDC.put(STAGE_KEY, stageName);
}
```

---

### 6. ExecutionContext.applyToMDC()
**呼び出し**: 上記関数から
**場所**: `ExecutionContext.java:206-222`

```java
public void applyToMDC() {
    // 基本コンテキスト情報をMDCに設定
    MDC.put(EXECUTION_ID_KEY, executionId);
    MDC.put(START_TIME_KEY, startTime.toString());
    MDC.put(COMMAND_SEQUENCE_KEY, String.valueOf(getCurrentCommandSequence()));
    MDC.put(THREAD_NAME_KEY, Thread.currentThread().getName());

    // グローバル・ユーザー・共有コンテキストをMDCに設定
    globalContext.forEach(MDC::put);
    userContext.forEach(MDC::put);
    sharedContext.forEach(MDC::put);  // ★共有コンテキストも反映
}
```

**MDCに設定される内容**:
```
executionId: "EXEC-c226aa5a-1753633953001"
startTime: "2025-11-28T10:30:45.123Z"
commandSequence: "0"
threadName: "main"
stage: "pipeline-start"
```

---

### 7. StreamConverter.executeMultipleCommandsWithMDC()
**呼び出し**: run()から
**場所**: `StreamConverter.java:215-269`

```java
private List<CommandResult> executeMultipleCommandsWithMDC(...) {
    try (AutoCloseableExecutorService executor = ...) {
        for (int i = 0; i < commands.size(); i++) {
            IStreamCommand command = commands.get(i);

            // 各コマンドを非同期実行
            CompletableFuture<CommandResult> future = executor.supplyAsync(() -> {
                // 【重要3】スレッド固有のMDC設定
                int sequence = context.getNextCommandSequence();  // ★呼ばれる
                String stageName = command.getClass().getSimpleName() + "-" + sequence;
                context.applyToMDCWithStage(stageName);  // ★再び呼ばれる

                try {
                    // 【重要4】コマンド実行
                    command.execute(commandInput, commandOutput, context);  // ★
                    ...
                }
            });
        }
    }
}
```

---

### 8. ExecutionContext.getNextCommandSequence()
**呼び出し**: 各コマンド実行前
**場所**: `ExecutionContext.java:96-98`

```java
public int getNextCommandSequence() {
    return commandSequence.incrementAndGet();  // ★スレッドセーフにインクリメント
}
```

**戻り値**: 1, 2, 3, ... (呼ばれるたびに増加)

---

### 9. IStreamCommand.execute(3パラメータ版)
**呼び出し**: StreamConverterから
**場所**: `IStreamCommand.java:59-63`

```java
default void execute(InputStream inputStream, OutputStream outputStream,
                    ExecutionContext context) throws IOException {
    // デフォルト実装：2パラメータ版に委譲
    execute(inputStream, outputStream);
}
```

**AbstractStreamCommandの場合**: オーバーライドされている

---

### 10. AbstractStreamCommand.execute(3パラメータ版)
**呼び出し**: StreamConverterから
**場所**: `AbstractStreamCommand.java:44-59`

```java
@Override
public final void execute(
    InputStream inputStream, OutputStream outputStream, ExecutionContext context) {

    // 【重要5】ThreadLocalにコンテキストを設定
    try {
        ExecutionContextHolder.set(context);  // ★この関数が呼ばれる

        // 2パラメータ版のexecuteに委譲
        execute(inputStream, outputStream);

    } finally {
        // 【重要6】ThreadLocalをクリーンアップ
        ExecutionContextHolder.clear();  // ★この関数が呼ばれる
    }
}
```

---

### 11. ExecutionContextHolder.set()
**呼び出し**: AbstractStreamCommand.execute()から
**場所**: `ExecutionContextHolder.java:33-35`

```java
public static void set(ExecutionContext context) {
    holder.set(context);  // ★ThreadLocalに保存
}
```

**効果**: 現在のスレッドにExecutionContextが紐付けられる

---

### 12. AbstractStreamCommand.execute(2パラメータ版)
**呼び出し**: 上記から
**場所**: `AbstractStreamCommand.java:72-141`

```java
@Override
public final void execute(InputStream inputStream, OutputStream outputStream) {
    String commandName = this.getClass().getSimpleName();
    long startTime = System.currentTimeMillis();

    // 実行開始ログ
    if (log.isInfoEnabled()) {
        log.info("Starting command execution: {}", commandName);  // ★ログ出力
    }

    try (MeasuredInputStream measuredInput = new MeasuredInputStream(inputStream);
         MeasuredOutputStream measuredOutput = new MeasuredOutputStream(outputStream)) {

        // 実際の処理実行
        executeInternal(measuredInput, measuredOutput);  // ★サブクラスが実装

        // 成功時のログ
        log.info("Command execution completed: {} ({}ms, ...)", ...);  // ★ログ出力
    }
}
```

**ログ出力時に何が起こるか**: 次のステップへ

---

### 13. log.info() → ExecutionContextTurboFilter.decide()
**呼び出し**: ログ出力の都度（Logbackが自動的に呼ぶ）
**場所**: `ExecutionContextTurboFilter.java:76-122`

```java
@Override
public FilterReply decide(Marker marker, Logger logger, Level level,
                         String format, Object[] params, Throwable t) {

    // 【重要7】ThreadLocalからExecutionContextを取得
    ExecutionContext context = ExecutionContextHolder.get();  // ★呼ばれる

    if (context != null) {
        // 【重要8】共有コンテキストを取得
        Map<String, String> sharedContext = context.getAllSharedContext();  // ★
        Set<String> currentKeys = new HashSet<>();

        // 各キーについてMDCと比較
        for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            currentKeys.add(key);

            String currentValue = MDC.get(key);
            if (!value.equals(currentValue)) {
                MDC.put(key, value);  // ★変更があった場合のみ更新
            }
        }

        // 削除されたキーのクリーンアップ
        Set<String> previousKeys = managedKeys.get();
        for (String key : previousKeys) {
            if (!currentKeys.contains(key)) {
                MDC.remove(key);  // ★削除
            }
        }

        managedKeys.set(currentKeys);
    }

    return FilterReply.NEUTRAL;  // ログを通過させる
}
```

---

### 14. ExecutionContextHolder.get()
**呼び出し**: TurboFilterから（ログ出力の都度）
**場所**: `ExecutionContextHolder.java:42-44`

```java
public static ExecutionContext get() {
    return holder.get();  // ★ThreadLocalから取得
}
```

**戻り値**: 現在のスレッドに設定されているExecutionContext

---

### 15. ExecutionContext.getAllSharedContext()
**呼び出し**: TurboFilterから
**場所**: `ExecutionContext.java:195-197`

```java
public Map<String, String> getAllSharedContext() {
    return new HashMap<>(sharedContext);  // ★コピーを返す
}
```

---

### 16. executeInternal() （サブクラスが実装）
**呼び出し**: AbstractStreamCommand.execute()から
**例**: `SampleStreamCommand.java:40-46`

```java
@Override
public void executeInternal(InputStream inputStream, OutputStream outputStream) {
    Objects.requireNonNull(inputStream);
    Objects.requireNonNull(outputStream);
    inputStream.transferTo(outputStream);  // ★実際の処理
}
```

---

### 17. ExecutionContextHolder.clear()
**呼び出し**: AbstractStreamCommand.execute()のfinallyブロック
**場所**: `ExecutionContextHolder.java:51-53`

```java
public static void clear() {
    holder.remove();  // ★ThreadLocalをクリア
}
```

**効果**: メモリリークを防止

---

## MdcSetupRuleを使った場合の追加フロー

### 18. XmlNavigateCommand.execute() → MdcSetupRule.apply()
**場所**: `MdcSetupRule.java:60-64`

```java
@Override
public String apply(String extractedValue) {
    context.setSharedContext(mdcKey, extractedValue);  // ★共有コンテキストに設定
    return extractedValue;
}
```

---

### 19. ExecutionContext.setSharedContext()
**呼び出し**: MdcSetupRule.apply()から
**場所**: `ExecutionContext.java:181-188`

```java
public void setSharedContext(String key, String value) {
    if (value == null) {
        sharedContext.remove(key);
    } else {
        sharedContext.put(key, value);  // ★ConcurrentHashMapに設定
    }
    sharedContextDirty.set(true);
}
```

**効果**: 全スレッドで共有される値が設定される

---

## 関数呼び出しの時系列まとめ

```
1. ExecutionContext.create()
   └─ Builder.generateExecutionId()
   └─ Builder.build()

2. StreamConverter.createWithContext(context, command)
   └─ new StreamConverter(commands)

3. converter.run(input, output)
   └─ run(input, output, context)
       └─ context.applyToMDCWithStage("pipeline-start")
           └─ context.applyToMDC()
               └─ MDC.put(executionId, ...)
               └─ MDC.put(startTime, ...)
       └─ executeMultipleCommandsWithMDC()
           └─ executor.supplyAsync(() -> {
               └─ context.getNextCommandSequence()  // → 1
               └─ context.applyToMDCWithStage("SampleStreamCommand-1")
               └─ command.execute(input, output, context)
                   └─ [AbstractStreamCommand]
                       └─ ExecutionContextHolder.set(context)
                       └─ try {
                           └─ execute(input, output)
                               └─ log.info("Starting...")  ← ★ログ出力
                                   └─ [Logback TurboFilter]
                                       └─ ExecutionContextTurboFilter.decide()
                                           └─ ExecutionContextHolder.get()
                                           └─ context.getAllSharedContext()
                                           └─ MDC.put(key, value)  // 差分のみ
                               └─ executeInternal(input, output)
                                   └─ inputStream.transferTo(outputStream)
                               └─ log.info("Completed...")  ← ★ログ出力
                                   └─ [同じTurboFilterプロセス]
                       └─ } finally {
                           └─ ExecutionContextHolder.clear()
                       }
           })

4. (MdcSetupRuleを使う場合)
   └─ XmlNavigateCommand.execute()
       └─ rule.apply(extractedValue)
           └─ context.setSharedContext("userId", "USER123")
               └─ sharedContext.put("userId", "USER123")

5. (後続のコマンド)
   └─ log.info("処理中")
       └─ TurboFilter.decide()
           └─ context.getAllSharedContext()  // userId="USER123"が含まれる
           └─ MDC.put("userId", "USER123")  // ★自動的にMDCに反映
```

---

## 各関数が呼ばれる回数（単一コマンドの場合）

| 関数 | 呼び出し回数 | タイミング |
|------|-------------|-----------|
| `ExecutionContext.create()` | 1回 | 起動時 |
| `ExecutionContext.applyToMDC()` | 2回 | パイプライン開始時、コマンド実行前 |
| `ExecutionContext.getNextCommandSequence()` | 1回 | コマンド実行前 |
| `ExecutionContextHolder.set()` | 1回 | コマンド実行開始時 |
| `ExecutionContextHolder.get()` | ログ出力回数分 | ログ出力の都度 |
| `ExecutionContextTurboFilter.decide()` | ログ出力回数分 | ログ出力の都度 |
| `ExecutionContext.getAllSharedContext()` | ログ出力回数分 | TurboFilterから |
| `ExecutionContextHolder.clear()` | 1回 | コマンド実行終了時 |
| `MdcSetupRule.apply()` | 抽出回数分 | Navigate実行時 |
| `ExecutionContext.setSharedContext()` | 抽出回数分 | MdcSetupRuleから |

---

## 重要ポイント

1. **ExecutionContextHolder.set/get/clear** がThreadLocal管理の中心
2. **ExecutionContextTurboFilter.decide()** はログ出力の都度自動実行される
3. **ExecutionContext.getAllSharedContext()** で共有コンテキストを取得
4. **MDC.put()** は差分があった場合のみ実行（パフォーマンス最適化）
5. **finallyブロック** で必ずExecutionContextHolder.clear()が実行される

この仕組みにより、アプリケーションコードは何も意識せずに、ログに自動的にコンテキスト情報が含まれるようになっています。
