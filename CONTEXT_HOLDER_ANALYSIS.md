# ExecutionContextとExecutionContextHolderが分かれている理由

## 質問: なぜ分かれているのか？

```java
// なぜこれが...
ExecutionContext context = new ExecutionContext(...);
ExecutionContextHolder.set(context);

// こうじゃないのか？
ExecutionContext.set(...);
ExecutionContext.get();
```

---

## 現在の設計

### ExecutionContext（データクラス）
```java
public class ExecutionContext {
    // データ保持
    private final String executionId;
    private final Instant startTime;
    private final Map<String, String> globalContext;
    private final Map<String, String> userContext;
    private final ConcurrentHashMap<String, String> sharedContext;

    // データアクセス
    public String getExecutionId() { ... }
    public Map<String, String> getAllSharedContext() { ... }
    public void setSharedContext(String key, String value) { ... }
}
```

**役割**: データの保持とアクセス

### ExecutionContextHolder（静的ユーティリティ）
```java
public class ExecutionContextHolder {
    private static final ThreadLocal<ExecutionContext> holder = new ThreadLocal<>();

    public static void set(ExecutionContext context) { ... }
    public static ExecutionContext get() { ... }
    public static void clear() { ... }
}
```

**役割**: ThreadLocalによるスレッドごとの管理

---

## 分離されている理由（公式の設計意図）

### 理由1: 関心の分離（Separation of Concerns）

| クラス | 関心事 | 状態管理 |
|--------|--------|---------|
| **ExecutionContext** | データの保持・アクセス | インスタンスごと |
| **ExecutionContextHolder** | スレッドごとの管理 | 静的（グローバル） |

```java
// ExecutionContextは普通のJavaオブジェクト
ExecutionContext context1 = ExecutionContext.create();
ExecutionContext context2 = ExecutionContext.create();
context1.setSharedContext("key", "value1");
context2.setSharedContext("key", "value2");  // 独立している

// ExecutionContextHolderはスレッドごとの管理
ExecutionContextHolder.set(context1);  // このスレッドはcontext1
// 別のスレッドではcontext2をsetできる
```

**利点**: ExecutionContextはThreadLocalの存在を知らなくて良い

---

### 理由2: テスタビリティ

```java
// ExecutionContext単体でテスト可能
@Test
void testExecutionContext() {
    ExecutionContext context = ExecutionContext.create();
    context.setSharedContext("userId", "USER123");

    assertEquals("USER123", context.getSharedContext("userId"));
    // ThreadLocalに依存しない
}

// ExecutionContextHolderのテスト
@Test
void testHolder() {
    ExecutionContext context = ExecutionContext.create();
    ExecutionContextHolder.set(context);

    assertEquals(context, ExecutionContextHolder.get());

    ExecutionContextHolder.clear();
    assertNull(ExecutionContextHolder.get());
}
```

**利点**: ThreadLocalに依存しないテストが可能

---

### 理由3: マルチスレッド環境での柔軟性

```java
// 同じExecutionContextインスタンスを複数スレッドで共有
ExecutionContext sharedContext = ExecutionContext.create();

// Thread 1
new Thread(() -> {
    ExecutionContextHolder.set(sharedContext);  // 同じインスタンス
    log.info("Thread 1");
    ExecutionContextHolder.clear();
}).start();

// Thread 2
new Thread(() -> {
    ExecutionContextHolder.set(sharedContext);  // 同じインスタンス
    log.info("Thread 2");
    ExecutionContextHolder.clear();
}).start();
```

**利点**: ExecutionContextはスレッド間で共有可能、Holderはスレッドローカル

---

## 他のフレームワークとの比較

### Spring Security
```java
// 同じパターン
SecurityContext context = SecurityContextHolder.getContext();
SecurityContextHolder.setContext(context);
SecurityContextHolder.clearContext();
```

**構造**:
- `SecurityContext` - データ保持
- `SecurityContextHolder` - ThreadLocal管理

### SLF4J MDC
```java
// 統合されたパターン
MDC.put("key", "value");
MDC.get("key");
MDC.clear();
```

**構造**:
- `MDC` - 静的ユーティリティ（ThreadLocal直接管理）
- データクラスなし（`Map<String, String>`を直接ThreadLocalに保存）

### 比較

| フレームワーク | データクラス | Holderクラス | パターン |
|--------------|-------------|------------|---------|
| **StreamConverter** | ExecutionContext | ExecutionContextHolder | ✅ 分離 |
| **Spring Security** | SecurityContext | SecurityContextHolder | ✅ 分離 |
| **SLF4J MDC** | なし | MDC | ❌ 統合 |
| **Log4j** | なし | ThreadContext | ❌ 統合 |

**結論**: Spring Securityと同じパターン（分離）

---

## 分離のメリット

### 1. 単一責任の原則（SRP）

```java
// ExecutionContext: データ管理の責任のみ
public class ExecutionContext {
    private final String executionId;

    public String getExecutionId() { return executionId; }
    public void setSharedContext(String key, String value) { ... }
}

// ExecutionContextHolder: ThreadLocal管理の責任のみ
public class ExecutionContextHolder {
    private static final ThreadLocal<ExecutionContext> holder = new ThreadLocal<>();

    public static void set(ExecutionContext context) { holder.set(context); }
    public static ExecutionContext get() { return holder.get(); }
    public static void clear() { holder.remove(); }
}
```

### 2. 依存性の逆転

```java
// ExecutionContextはThreadLocalに依存しない
// → 他の方法でも管理可能

// 例: リクエストスコープで管理
@RequestScope
public class WebExecutionContext extends ExecutionContext { ... }

// 例: インメモリで管理（テスト用）
public class InMemoryExecutionContext extends ExecutionContext { ... }
```

### 3. モック化の容易さ

```java
@Test
void testWithMock() {
    // ExecutionContextをモック化
    ExecutionContext mockContext = mock(ExecutionContext.class);
    when(mockContext.getExecutionId()).thenReturn("MOCK-123");

    ExecutionContextHolder.set(mockContext);

    // テスト実行
    // ...
}
```

---

## 分離のデメリット

### 1. API の冗長性

```java
// 2ステップ必要
ExecutionContext context = ExecutionContext.create();
ExecutionContextHolder.set(context);

// 1ステップで済む場合と比較
ExecutionContext.set(...);  // こうできない
```

### 2. 開発者の混乱

```java
// どちらを使えば良い？
ExecutionContext context = ExecutionContext.create();  // データ生成
ExecutionContextHolder.set(context);  // スレッドに設定

// vs

ExecutionContext.create();  // データ生成（でもスレッドには未設定）
```

### 3. コード量の増加

```
ExecutionContext.java        : 377行
ExecutionContextHolder.java  : 60行
------------------------------------
合計                         : 437行

統合した場合（推定）         : 300行程度
```

---

## 統合した場合の設計

### 案: ExecutionContextに統合

```java
public class ExecutionContext {
    // ThreadLocalを内部に持つ
    private static final ThreadLocal<ExecutionContext> currentContext = new ThreadLocal<>();

    // データフィールド
    private final String executionId;
    private final Map<String, String> sharedContext;

    // コンストラクタ（private）
    private ExecutionContext(String executionId) {
        this.executionId = executionId;
        this.sharedContext = new ConcurrentHashMap<>();
    }

    // ファクトリメソッド
    public static ExecutionContext create() {
        ExecutionContext context = new ExecutionContext(generateId());
        currentContext.set(context);  // 自動的にセット
        return context;
    }

    // 静的アクセサ
    public static ExecutionContext current() {
        return currentContext.get();
    }

    public static void clear() {
        currentContext.remove();
    }

    // インスタンスメソッド
    public String getExecutionId() { return executionId; }
    public void setSharedContext(String key, String value) { ... }
}
```

**使用例**:
```java
// 1ステップで済む
ExecutionContext context = ExecutionContext.create();  // 自動的にThreadLocalにセット

// 静的アクセス
ExecutionContext current = ExecutionContext.current();

// クリア
ExecutionContext.clear();
```

**利点**:
- シンプルなAPI
- 1クラスで完結
- 混乱が少ない

**欠点**:
- ExecutionContextがThreadLocalに依存
- テスタビリティの低下
- 単一責任の原則に違反

---

## 実際のところ、どちらが良いか？

### 分離パターンが適している場合

1. ✅ **複雑なデータ構造** - ExecutionContextが多くの機能を持つ
2. ✅ **テスタビリティ重視** - ThreadLocalに依存しないテストが必要
3. ✅ **柔軟な管理方法** - ThreadLocal以外の管理方法も想定

### 統合パターンが適している場合

1. ✅ **シンプルなデータ構造** - MDCのような単純なMap
2. ✅ **使いやすさ重視** - 開発者がすぐに理解できる
3. ✅ **単一の管理方法** - ThreadLocalのみで十分

### StreamConverterの場合

現在のExecutionContextは**複雑**（377行）で**多機能**なため、**分離パターンが適切**

しかし、**MDC同期だけ**が目的なら、統合パターンの方がシンプル

---

## 結論

### なぜ分かれているか？

1. **Spring Securityと同じパターン** - 実績のある設計
2. **単一責任の原則** - データ管理とThreadLocal管理を分離
3. **テスタビリティ** - ThreadLocalに依存しないテストが可能
4. **柔軟性** - ExecutionContextはThreadLocalの存在を知らない

### 本当に必要か？

**ExecutionContextが複雑で多機能なら**: ✅ 分離は妥当

**MDC同期だけが目的なら**: ❌ 統合した方がシンプル

### 改善案

現在のExecutionContextは**多目的すぎる**ため、以下のように分離すると良い：

```
【案1】 機能別に分離
- ExecutionIdentifier (executionId, startTime)
- CommandSequenceManager (commandSequence)
- ContextData (globalContext, userContext, sharedContext)
- MDCSync (MDC同期専用)

【案2】 MDC同期専用クラスを作る
- ExecutionContext (現状維持、MDC機能削除)
- MDCContextHolder (MDC同期専用、シンプル)
```

**最もシンプルな解**:
```java
public class MDCContext {
    private static final ThreadLocal<Map<String, String>> holder = new ThreadLocal<>();

    public static void set(Map<String, String> values) { holder.set(values); }
    public static Map<String, String> get() { return holder.get(); }
    public static void clear() { holder.remove(); }
}
```

これだけで**MDC同期の要件は満たせる**。ExecutionContextの大部分は**MDC同期とは無関係**。
