# ExecutionContext 実行トレース - どの関数が効いているか

このドキュメントは、実際に**どの関数が、いつ、何回呼ばれるか**を具体的に示します。

## 実行例

```java
ExecutionContext context = ExecutionContext.create();
SampleStreamCommand command = new SampleStreamCommand("test");
StreamConverter converter = StreamConverter.createWithContext(context, command);
converter.run(inputStream, outputStream);
```

---

# 関数呼び出しトレース（時系列順）

## Phase 1: 初期化フェーズ

### 1. `ExecutionContext.create()`
```
呼び出し元: アプリケーションコード
場所: ExecutionContext.java:60
戻り値: ExecutionContext インスタンス

内部で呼ばれる関数:
  └─ new Builder()
  └─ Builder.generateExecutionId()
      → "EXEC-a1b2c3d4-1732773045123" を生成
  └─ Builder.build()
      → ExecutionContext(executionId, startTime, ...)
```

**この時点で設定される値**:
```java
executionId = "EXEC-a1b2c3d4-1732773045123"
startTime = Instant.now()  // 2025-11-28T10:30:45.123Z
commandSequence = AtomicInteger(0)
globalContext = {}
userContext = {}
sharedContext = ConcurrentHashMap()
```

### 2. `StreamConverter.createWithContext(context, command)`
```
呼び出し元: アプリケーションコード
場所: StreamConverter.java:132

内部処理:
  └─ Objects.requireNonNull(context)
  └─ new StreamConverter(commands)
  └─ converter.defaultContext = context  ← ここで紐付け
```

**効果**: StreamConverterが特定のExecutionContextを持つ

---

## Phase 2: 実行開始フェーズ

### 3. `converter.run(inputStream, outputStream)`
```
呼び出し元: アプリケーションコード
場所: StreamConverter.java:176

内部処理:
  └─ contextToUse = defaultContext != null ? defaultContext : ExecutionContext.create()
  └─ run(inputStream, outputStream, contextToUse)  ← 3パラメータ版を呼ぶ
```

### 4. `run(inputStream, outputStream, context)`
```
呼び出し元: 上記から
場所: StreamConverter.java:193

重要な処理:
  Line 201: context.applyToMDCWithStage("pipeline-start")  ★ここが効く
  Line 204: LOG.info("Starting StreamConverter...")         ★ログ出力
  Line 211: executeMultipleCommandsWithMDC(...)            ★次へ
```

### 5. `context.applyToMDCWithStage("pipeline-start")`
```
呼び出し元: StreamConverter.run()
場所: ExecutionContext.java:229

内部処理:
  Line 230: applyToMDC()           ★ここが効く
  Line 231: MDC.put(STAGE_KEY, "pipeline-start")
```

### 6. `context.applyToMDC()`
```
呼び出し元: 上記から
場所: ExecutionContext.java:206

実際の処理:
  Line 208: MDC.put("executionId", "EXEC-a1b2c3d4-1732773045123")
  Line 209: MDC.put("startTime", "2025-11-28T10:30:45.123Z")
  Line 210: MDC.put("commandSequence", "0")
  Line 211: MDC.put("threadName", "main")
  Line 214: globalContext.forEach(MDC::put)  // 空の場合は何もしない
  Line 217: userContext.forEach(MDC::put)    // 空の場合は何もしない
  Line 221: sharedContext.forEach(MDC::put)  // 空の場合は何もしない
```

**この時点のMDC内容**:
```
executionId = "EXEC-a1b2c3d4-1732773045123"
startTime = "2025-11-28T10:30:45.123Z"
commandSequence = "0"
threadName = "main"
stage = "pipeline-start"
```

### 7. `LOG.info("Starting StreamConverter...")` の処理
```
呼び出し元: StreamConverter.run()
場所: StreamConverter.java:204

Logbackの処理フロー:
  1. TurboFilterチェーン実行
  2. ExecutionContextTurboFilter.decide() 呼び出し  ★ここが効く
  3. パターン展開（%X{executionId}など）
  4. Appenderへ出力
```

---

## Phase 3: TurboFilterフェーズ（ログ出力の都度）

### 8. `ExecutionContextTurboFilter.decide(...)`
```
呼び出し元: Logback（ログ出力時に自動）
場所: ExecutionContextTurboFilter.java:76

実際の処理:
  Line 80: ExecutionContext context = ExecutionContextHolder.get()  ★効く
           → まだsetされていないので null を返す

  Line 82: if (context != null) {
           → falseなのでスキップ

  Line 110: } else {
  Line 112:   Set<String> previousKeys = managedKeys.get()
  Line 113:   for (String key : previousKeys) {
  Line 114:     MDC.remove(key)
  Line 117:   managedKeys.remove()

  Line 121: return FilterReply.NEUTRAL  // ログを通過させる
```

**この時点**: ExecutionContextHolderはまだnull（コマンド実行前のため）

---

## Phase 4: コマンド実行フェーズ

### 9. `executeMultipleCommandsWithMDC(...)`
```
呼び出し元: StreamConverter.run()
場所: StreamConverter.java:215

Virtual Threadでの非同期実行:
  Line 248: executor.supplyAsync(() -> {
    Line 251:   int sequence = context.getNextCommandSequence()  ★効く → 1
    Line 252:   String stageName = "SampleStreamCommand-1"
    Line 253:   context.applyToMDCWithStage(stageName)           ★効く
    Line 269:   command.execute(commandInput, commandOutput, context)  ★効く
  })
```

### 10. `context.getNextCommandSequence()`
```
呼び出し元: executeMultipleCommandsWithMDC()
場所: ExecutionContext.java:96

実際の処理:
  Line 97: return commandSequence.incrementAndGet()
           → 0 → 1 にインクリメント
           → 戻り値: 1
```

### 11. `context.applyToMDCWithStage("SampleStreamCommand-1")`
```
呼び出し元: executeMultipleCommandsWithMDC()
場所: ExecutionContext.java:229

実際の処理（2回目）:
  Line 230: applyToMDC()
  Line 231: MDC.put("stage", "SampleStreamCommand-1")
```

**この時点のMDC内容**:
```
executionId = "EXEC-a1b2c3d4-1732773045123"
startTime = "2025-11-28T10:30:45.123Z"
commandSequence = "1"  ← 更新された
threadName = "stream-converter-1"  ← Virtual Thread名
stage = "SampleStreamCommand-1"  ← 更新された
```

### 12. `command.execute(input, output, context)`
```
呼び出し元: executeMultipleCommandsWithMDC()
場所: AbstractStreamCommand.java:44（3パラメータ版）

実際の処理:
  Line 51: try {
  Line 52:   ExecutionContextHolder.set(context)  ★ここが効く！
  Line 54:   execute(inputStream, outputStream)    ★2パラメータ版を呼ぶ
  Line 56: } finally {
  Line 58:   ExecutionContextHolder.clear()        ★必ず実行される
```

### 13. `ExecutionContextHolder.set(context)`
```
呼び出し元: AbstractStreamCommand.execute()
場所: ExecutionContextHolder.java:33

実際の処理:
  Line 34: holder.set(context)
           → ThreadLocal<ExecutionContext> に context を保存
```

**この時点**: ExecutionContextHolderに値が入った！

---

## Phase 5: コマンド内部処理フェーズ

### 14. `execute(inputStream, outputStream)` (2パラメータ版)
```
呼び出し元: AbstractStreamCommand.execute(3パラメータ版)
場所: AbstractStreamCommand.java:72

実際の処理:
  Line 78: if (log.isInfoEnabled()) {
  Line 79:   log.info("Starting command execution: {}", commandName)  ★ログ1

  Line 88: try (MeasuredInputStream measuredInput = ...) {
  Line 92:   executeInternal(measuredInput, measuredOutput)  ★実処理
  Line 103:  log.info("Command execution completed: {} ...", ...)  ★ログ2
```

### 15. `log.info("Starting command execution...")` の処理
```
呼び出し元: AbstractStreamCommand.execute()
場所: AbstractStreamCommand.java:79

Logbackの処理フロー（再び）:
  1. TurboFilterチェーン実行
  2. ExecutionContextTurboFilter.decide() 呼び出し  ★今度は効く
  3. パターン展開
  4. Appenderへ出力
```

### 16. `ExecutionContextTurboFilter.decide(...)` （2回目）
```
呼び出し元: Logback（ログ出力時）
場所: ExecutionContextTurboFilter.java:76

実際の処理（今度は context != null）:
  Line 80: ExecutionContext context = ExecutionContextHolder.get()  ★効く
           → context が返る（さっき set したから）

  Line 82: if (context != null) {  ← true！

  Line 84:   Map<String, String> sharedContext = context.getAllSharedContext()  ★効く
           → 空のMapが返る

  Line 85:   Set<String> currentKeys = new HashSet<>()

  Line 88:   for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
           → 空なのでループしない

  Line 100:  Set<String> previousKeys = managedKeys.get()
  Line 101:  for (String key : previousKeys) {
           → 初回なので空

  Line 108:  managedKeys.set(currentKeys)
           → 空のSetを保存

  Line 121: return FilterReply.NEUTRAL
```

### 17. `context.getAllSharedContext()`
```
呼び出し元: ExecutionContextTurboFilter.decide()
場所: ExecutionContext.java:195

実際の処理:
  Line 196: return new HashMap<>(sharedContext)
           → 空の HashMap が返る（まだ何も設定されていない）
```

**ログ出力結果**:
```
10:30:45.234 INFO SampleStreamCommand [execId:EXEC-a1b2c3d4-1732773045123, seq:1] - Starting command execution: SampleStreamCommand
```

### 18. `executeInternal(measuredInput, measuredOutput)`
```
呼び出し元: AbstractStreamCommand.execute()
場所: SampleStreamCommand.java:40（サブクラスが実装）

実際の処理:
  Line 43: Objects.requireNonNull(inputStream)
  Line 44: Objects.requireNonNull(outputStream)
  Line 45: inputStream.transferTo(outputStream)  ★データコピー
```

### 19. `log.info("Command execution completed...")` の処理
```
呼び出し元: AbstractStreamCommand.execute()
場所: AbstractStreamCommand.java:103

Logbackの処理フロー（3回目）:
  同じく ExecutionContextTurboFilter.decide() が呼ばれる
  → 同じ処理が実行される
```

**ログ出力結果**:
```
10:30:45.345 INFO SampleStreamCommand [execId:EXEC-a1b2c3d4-1732773045123, seq:1] - Command execution completed: SampleStreamCommand (111ms, input: 10bytes, output: 10bytes, memory: 2MB)
```

---

## Phase 6: クリーンアップフェーズ

### 20. `ExecutionContextHolder.clear()`
```
呼び出し元: AbstractStreamCommand.execute() の finally ブロック
場所: ExecutionContextHolder.java:51

実際の処理:
  Line 52: holder.remove()
           → ThreadLocal から ExecutionContext を削除
```

**効果**: メモリリーク防止

---

# MdcSetupRuleを使った場合の追加トレース

## シナリオ: XMLからuserIdを抽出

```java
ExecutionContext context = ExecutionContext.create();
XmlNavigateCommand extractUserId = new XmlNavigateCommand(
    TreePath.fromXml("request/userId"),
    new MdcSetupRule(context, "userId")
);
StreamConverter.createWithContext(context, extractUserId, otherCommand).run(...);
```

### 21. `XmlNavigateCommand.executeInternal()`
```
内部で値を抽出後、Ruleを適用:
  extractedValue = "USER12345"
  rule.apply(extractedValue)  ★ここが効く
```

### 22. `MdcSetupRule.apply("USER12345")`
```
呼び出し元: XmlNavigateCommand
場所: MdcSetupRule.java:60

実際の処理:
  Line 62: context.setSharedContext(mdcKey, extractedValue)  ★効く
  Line 63: return extractedValue
```

### 23. `context.setSharedContext("userId", "USER12345")`
```
呼び出し元: MdcSetupRule.apply()
場所: ExecutionContext.java:181

実際の処理:
  Line 182: if (value == null) {
           → false

  Line 185:   sharedContext.put(key, value)  ★ConcurrentHashMapに設定
                → sharedContext.put("userId", "USER12345")

  Line 187: sharedContextDirty.set(true)
```

**この時点の sharedContext**:
```java
{
  "userId": "USER12345"
}
```

### 24. 後続のコマンドで `log.info("処理中")`
```
Logbackの処理フロー:
  ExecutionContextTurboFilter.decide() が呼ばれる

実際の処理:
  Line 80: ExecutionContext context = ExecutionContextHolder.get()
           → context が返る

  Line 84: Map<String, String> sharedContext = context.getAllSharedContext()  ★効く
           → {"userId": "USER12345"} が返る

  Line 88: for (Map.Entry<String, String> entry : sharedContext.entrySet()) {
    Line 89:   String key = "userId"
    Line 90:   String value = "USER12345"
    Line 91:   currentKeys.add("userId")

    Line 93:   String currentValue = MDC.get("userId")
               → null（まだ設定されていない）

    Line 94:   if (!value.equals(currentValue)) {  ← true
    Line 95:     MDC.put("userId", "USER12345")  ★ここでMDCに設定される！
```

**ログ出力結果**:
```
10:30:45.456 INFO OtherCommand [execId:EXEC-..., seq:2, userId:USER12345] - 処理中
```

**効果**: 抽出した値が自動的にログに出力される！

---

# 関数呼び出し回数まとめ

## 単一コマンド、ログ3回出力の場合

| 関数 | 呼び出し回数 | 効くタイミング |
|------|-------------|---------------|
| `ExecutionContext.create()` | 1回 | 初期化時 |
| `ExecutionContext.applyToMDC()` | 2回 | パイプライン開始時、コマンド実行前 |
| `ExecutionContext.getNextCommandSequence()` | 1回 | コマンド実行前 |
| `ExecutionContextHolder.set()` | 1回 | コマンド実行開始時 |
| `ExecutionContextHolder.get()` | 3回 | **ログ出力の都度** |
| `ExecutionContextTurboFilter.decide()` | 3回 | **ログ出力の都度** |
| `ExecutionContext.getAllSharedContext()` | 3回 | TurboFilterから呼ばれる |
| `ExecutionContextHolder.clear()` | 1回 | コマンド実行終了時 |
| `MdcSetupRule.apply()` | (使用時のみ) | Navigate実行時 |
| `ExecutionContext.setSharedContext()` | (使用時のみ) | MdcSetupRuleから |

---

# 最も重要な関数トップ5

## 1位: `ExecutionContextTurboFilter.decide()`
**効く回数**: ログ出力の都度（最多）
**効果**: 自動MDC同期の実体
**場所**: `ExecutionContextTurboFilter.java:76`

## 2位: `ExecutionContextHolder.set()` / `get()` / `clear()`
**効く回数**: コマンドごとに set 1回、get N回（ログ出力回数）、clear 1回
**効果**: ThreadLocalによるコンテキスト管理
**場所**: `ExecutionContextHolder.java:33,42,51`

## 3位: `ExecutionContext.getAllSharedContext()`
**効く回数**: ログ出力の都度
**効果**: 共有コンテキストの取得（スレッド間で共有される値）
**場所**: `ExecutionContext.java:195`

## 4位: `ExecutionContext.setSharedContext()`
**効く回数**: MdcSetupRule使用時に抽出ごと
**効果**: 動的に抽出した値をスレッド間で共有
**場所**: `ExecutionContext.java:181`

## 5位: `ExecutionContext.applyToMDC()`
**効く回数**: パイプライン開始時とコマンド実行前（各1回）
**効果**: ExecutionContextの内容を明示的にMDCに設定
**場所**: `ExecutionContext.java:206`

---

# まとめ: どの関数が効いているか

## 常に効く関数（必ず呼ばれる）

1. **`ExecutionContextTurboFilter.decide()`** - ログ出力の都度
2. **`ExecutionContextHolder.get()`** - ログ出力の都度
3. **`ExecutionContext.getAllSharedContext()`** - ログ出力の都度
4. **`ExecutionContextHolder.set()`** - コマンド実行開始時
5. **`ExecutionContextHolder.clear()`** - コマンド実行終了時

## 条件付きで効く関数

1. **`ExecutionContext.setSharedContext()`** - MdcSetupRule使用時
2. **`MdcSetupRule.apply()`** - Navigate + MdcSetupRule使用時
3. **`MDC.put()`** - 値が変更された場合のみ（TurboFilter内）

## キーポイント

- **ログを1回出力するたび**に `TurboFilter.decide()` → `ExecutionContextHolder.get()` → `getAllSharedContext()` が呼ばれる
- **共有コンテキストへの設定**は `MdcSetupRule` 経由で行われる
- **自動MDC同期**は差分検知により効率化されている（変更があった場合のみ `MDC.put()` 実行）

この仕組みにより、アプリケーションコードは **`log.info()` を呼ぶだけ** で、自動的に最新のコンテキスト情報がログに含まれるようになっています。
