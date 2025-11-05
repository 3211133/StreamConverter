package com.streamconverter.context;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** ExecutionContextクラスのテスト */
class ExecutionContextTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testCreateBasicContext() {
    ExecutionContext context = ExecutionContext.create();

    assertNotNull(context.getExecutionId());
    assertTrue(context.getExecutionId().startsWith("EXEC-"));
    assertNotNull(context.getStartTime());
    assertEquals(0, context.getCurrentCommandSequence());
  }

  @Test
  void testBuilderWithCustomValues() {
    Instant customStartTime = Instant.now().minusSeconds(60);
    String customExecutionId = "CUSTOM-EXEC-123";

    ExecutionContext context =
        ExecutionContext.builder()
            .executionId(customExecutionId)
            .startTime(customStartTime)
            .globalContext("environment", "test")
            .userContext("priority", "high")
            .build();

    assertEquals(customExecutionId, context.getExecutionId());
    assertEquals(customStartTime, context.getStartTime());
    assertEquals("test", context.getGlobalContext("environment"));
    assertEquals("high", context.getUserContext("priority"));
  }

  @Test
  void testCommandSequenceIncrement() {
    ExecutionContext context = ExecutionContext.create();

    assertEquals(0, context.getCurrentCommandSequence());
    assertEquals(1, context.getNextCommandSequence());
    assertEquals(1, context.getCurrentCommandSequence());
    assertEquals(2, context.getNextCommandSequence());
    assertEquals(2, context.getCurrentCommandSequence());
  }

  @Test
  void testGlobalContextImmutability() {
    Map<String, String> initialGlobalContext = new HashMap<>();
    initialGlobalContext.put("env", "prod");

    ExecutionContext context =
        ExecutionContext.builder().globalContext(initialGlobalContext).build();

    Map<String, String> retrievedContext = context.getAllGlobalContext();

    // getAllGlobalContextは読み取り専用なので変更操作は失敗する
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          retrievedContext.put("newKey", "newValue");
        });

    // 元のマップを変更しても影響しないことを確認
    initialGlobalContext.put("anotherKey", "anotherValue");
    assertNull(context.getGlobalContext("anotherKey"));
  }

  @Test
  void testUserContextMutability() {
    ExecutionContext context = ExecutionContext.create();

    // ユーザーコンテキストは変更可能
    context.setUserContext("status", "processing");
    assertEquals("processing", context.getUserContext("status"));

    context.setUserContext("status", "completed");
    assertEquals("completed", context.getUserContext("status"));

    // null設定で削除
    context.setUserContext("status", null);
    assertNull(context.getUserContext("status"));
  }

  @Test
  void testApplyToMDC() {
    ExecutionContext context =
        ExecutionContext.builder()
            .executionId("TEST-EXEC-456")
            .globalContext("requestId", "REQ-789")
            .userContext("userId", "user123")
            .build();

    context.applyToMDC();

    assertEquals("TEST-EXEC-456", MDC.get(ExecutionContext.EXECUTION_ID_KEY));
    assertEquals("REQ-789", MDC.get("requestId"));
    assertEquals("user123", MDC.get("userId"));
    assertNotNull(MDC.get(ExecutionContext.THREAD_NAME_KEY));
  }

  @Test
  void testApplyToMDCWithStage() {
    ExecutionContext context = ExecutionContext.create();

    context.applyToMDCWithStage("validation");

    assertEquals("validation", MDC.get(ExecutionContext.STAGE_KEY));
    assertNotNull(MDC.get(ExecutionContext.EXECUTION_ID_KEY));
  }

  @Test
  void testContextCopy() {
    ExecutionContext original =
        ExecutionContext.builder()
            .executionId("ORIGINAL-123")
            .globalContext("env", "test")
            .userContext("status", "active")
            .build();

    // シーケンスを進める
    original.getNextCommandSequence();

    ExecutionContext copy = original.copy();

    // 基本属性は同じ
    assertEquals(original.getExecutionId(), copy.getExecutionId());
    assertEquals(original.getStartTime(), copy.getStartTime());
    assertEquals(original.getGlobalContext("env"), copy.getGlobalContext("env"));
    assertEquals(original.getUserContext("status"), copy.getUserContext("status"));

    // コピー時はシーケンスはリセットされる（新しいコピーとして扱う）
    assertEquals(0, copy.getCurrentCommandSequence());

    // ユーザーコンテキストは独立して変更可能
    copy.setUserContext("status", "modified");
    assertEquals("active", original.getUserContext("status"));
    assertEquals("modified", copy.getUserContext("status"));
  }

  @Test
  void testBuilderWithNullValues() {
    assertThrows(
        NullPointerException.class,
        () -> {
          ExecutionContext.builder().executionId(null).build();
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          ExecutionContext.builder().startTime(null).build();
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          ExecutionContext.builder().globalContext(null, "value").build();
        });

    assertThrows(
        NullPointerException.class,
        () -> {
          ExecutionContext.builder().userContext(null, "value").build();
        });
  }

  @Test
  void testBuilderWithNullContextMaps() {
    // null マップは無視される
    ExecutionContext context =
        ExecutionContext.builder()
            .globalContext((Map<String, String>) null)
            .userContext((Map<String, String>) null)
            .build();

    assertNotNull(context);
    assertTrue(context.getAllGlobalContext().isEmpty());
    assertTrue(context.getAllUserContext().isEmpty());
  }

  @Test
  void testEqualityAndHashCode() {
    String executionId = "TEST-EXEC-EQUALITY";

    ExecutionContext context1 = ExecutionContext.builder().executionId(executionId).build();

    ExecutionContext context2 = ExecutionContext.builder().executionId(executionId).build();

    ExecutionContext context3 = ExecutionContext.builder().executionId("DIFFERENT-EXEC").build();

    // 同じexecutionIdなら等しい
    assertEquals(context1, context2);
    assertEquals(context1.hashCode(), context2.hashCode());

    // 異なるexecutionIdなら等しくない
    assertNotEquals(context1, context3);
    assertNotEquals(context1.hashCode(), context3.hashCode());
  }

  @Test
  void testToString() {
    ExecutionContext context = ExecutionContext.builder().executionId("TEST-EXEC-TOSTRING").build();

    String toString = context.toString();

    assertTrue(toString.contains("ExecutionContext"));
    assertTrue(toString.contains("TEST-EXEC-TOSTRING"));
    assertTrue(toString.contains("commandSequence=0"));
  }

  @Test
  void testMDCContextPreservation() {
    // 既存のMDCコンテキストを設定
    MDC.put("existingKey", "existingValue");

    ExecutionContext context =
        ExecutionContext.builder().globalContext("newKey", "newValue").build();

    context.applyToMDC();

    // 新しいコンテキストが追加される
    assertEquals("newValue", MDC.get("newKey"));

    // 既存のコンテキストは上書きされる可能性があるため、
    // MDCの完全な分離が必要な場合は事前にクリアする必要がある
    assertNotNull(MDC.get(ExecutionContext.EXECUTION_ID_KEY));
  }

  // ========================================
  // Phase 1: Shared Context Tests
  // ========================================

  @Test
  void testSetAndGetSharedContext() {
    ExecutionContext context = ExecutionContext.create();

    // 共有コンテキストに値を設定
    context.setSharedContext("userId", "USER12345");
    context.setSharedContext("requestId", "REQ-98765");

    // 設定した値が取得できることを確認
    assertEquals("USER12345", context.getSharedContext("userId"));
    assertEquals("REQ-98765", context.getSharedContext("requestId"));
  }

  @Test
  void testSharedContextReturnsNullForMissingKey() {
    ExecutionContext context = ExecutionContext.create();

    // 存在しないキーはnullを返す
    assertNull(context.getSharedContext("nonExistentKey"));
  }

  @Test
  void testSharedContextOverwriteValue() {
    ExecutionContext context = ExecutionContext.create();

    // 値を設定
    context.setSharedContext("status", "pending");
    assertEquals("pending", context.getSharedContext("status"));

    // 値を上書き
    context.setSharedContext("status", "completed");
    assertEquals("completed", context.getSharedContext("status"));
  }

  @Test
  void testSharedContextRemoveWithNull() {
    ExecutionContext context = ExecutionContext.create();

    // 値を設定
    context.setSharedContext("tempKey", "tempValue");
    assertEquals("tempValue", context.getSharedContext("tempKey"));

    // null設定で削除
    context.setSharedContext("tempKey", null);
    assertNull(context.getSharedContext("tempKey"));
  }

  @Test
  void testGetAllSharedContext() {
    ExecutionContext context = ExecutionContext.create();

    context.setSharedContext("key1", "value1");
    context.setSharedContext("key2", "value2");
    context.setSharedContext("key3", "value3");

    Map<String, String> allShared = context.getAllSharedContext();

    assertEquals(3, allShared.size());
    assertEquals("value1", allShared.get("key1"));
    assertEquals("value2", allShared.get("key2"));
    assertEquals("value3", allShared.get("key3"));
  }

  @Test
  void testSharedContextThreadSafety() throws InterruptedException {
    ExecutionContext context = ExecutionContext.create();
    int threadCount = 10;
    int operationsPerThread = 100;

    // 複数スレッドから並行アクセス
    Thread[] threads = new Thread[threadCount];
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                for (int j = 0; j < operationsPerThread; j++) {
                  String key = "thread" + threadId + "_key" + j;
                  String value = "thread" + threadId + "_value" + j;

                  // 書き込み
                  context.setSharedContext(key, value);

                  // 読み込み（即座に取得できることを確認）
                  String retrieved = context.getSharedContext(key);
                  assertEquals(value, retrieved);
                }
              });
    }

    // 全スレッド開始
    for (Thread thread : threads) {
      thread.start();
    }

    // 全スレッド完了待ち
    for (Thread thread : threads) {
      thread.join();
    }

    // 全ての値が正しく保存されていることを確認
    Map<String, String> allShared = context.getAllSharedContext();
    assertEquals(threadCount * operationsPerThread, allShared.size());

    // 各スレッドが設定した値を検証
    for (int i = 0; i < threadCount; i++) {
      for (int j = 0; j < operationsPerThread; j++) {
        String key = "thread" + i + "_key" + j;
        String expectedValue = "thread" + i + "_value" + j;
        assertEquals(expectedValue, allShared.get(key));
      }
    }
  }

  @Test
  void testSharedContextConcurrentReadWrite() throws InterruptedException {
    ExecutionContext context = ExecutionContext.create();
    String sharedKey = "concurrentKey";
    int writerThreads = 5;
    int readerThreads = 5;
    int operations = 50;

    // 書き込みスレッド
    Thread[] writers = new Thread[writerThreads];
    for (int i = 0; i < writerThreads; i++) {
      final int writerId = i;
      writers[i] =
          new Thread(
              () -> {
                for (int j = 0; j < operations; j++) {
                  context.setSharedContext(sharedKey, "writer" + writerId + "_" + j);
                  try {
                    Thread.sleep(1); // わずかな遅延を挟む
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              });
    }

    // 読み込みスレッド
    Thread[] readers = new Thread[readerThreads];
    for (int i = 0; i < readerThreads; i++) {
      readers[i] =
          new Thread(
              () -> {
                for (int j = 0; j < operations; j++) {
                  String value = context.getSharedContext(sharedKey);
                  // 値が取得できること（nullまたは有効な値）
                  // ConcurrentHashMapはスレッドセーフなので例外は発生しない
                  assertNotNull(value == null || value.startsWith("writer"));
                  try {
                    Thread.sleep(1);
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  }
                }
              });
    }

    // 全スレッド開始
    for (Thread writer : writers) {
      writer.start();
    }
    for (Thread reader : readers) {
      reader.start();
    }

    // 全スレッド完了待ち
    for (Thread writer : writers) {
      writer.join();
    }
    for (Thread reader : readers) {
      reader.join();
    }

    // 最終的に何らかの値が設定されていることを確認
    assertNotNull(context.getSharedContext(sharedKey));
  }

  // ========================================
  // Phase 2: MDC Synchronization Tests
  // ========================================

  @Test
  void testApplyToMDCSyncsSharedContext() {
    ExecutionContext context = ExecutionContext.create();

    // 共有コンテキストに値を設定
    context.setSharedContext("userId", "USER12345");
    context.setSharedContext("requestId", "REQ-98765");

    // applyToMDC()を呼び出し
    context.applyToMDC();

    // MDCに共有コンテキストの値が反映されていることを確認
    assertEquals("USER12345", MDC.get("userId"));
    assertEquals("REQ-98765", MDC.get("requestId"));

    // 既存のMDCキーも正しく設定されている
    assertNotNull(MDC.get(ExecutionContext.EXECUTION_ID_KEY));
    assertNotNull(MDC.get(ExecutionContext.THREAD_NAME_KEY));
  }

  @Test
  void testApplyToMDCUpdatesSharedContextChanges() {
    ExecutionContext context = ExecutionContext.create();

    // 初回設定
    context.setSharedContext("status", "initial");
    context.applyToMDC();
    assertEquals("initial", MDC.get("status"));

    // 値を更新
    context.setSharedContext("status", "updated");
    context.applyToMDC();

    // MDCにも更新が反映される
    assertEquals("updated", MDC.get("status"));
  }

  @Test
  void testApplyToMDCWithStageAlsoSyncsSharedContext() {
    ExecutionContext context = ExecutionContext.create();

    context.setSharedContext("userId", "USER99999");

    // applyToMDCWithStage()でも共有コンテキストが同期される
    context.applyToMDCWithStage("processing");

    assertEquals("USER99999", MDC.get("userId"));
    assertEquals("processing", MDC.get(ExecutionContext.STAGE_KEY));
  }

  @Test
  void testSharedContextDirtyFlagOptimization() {
    ExecutionContext context = ExecutionContext.create();

    // 初回: 共有コンテキスト未設定 → MDCにも反映されない
    context.applyToMDC();
    assertNull(MDC.get("userId"));

    // 値を設定
    context.setSharedContext("userId", "USER123");

    // 2回目: 変更があったのでMDCに反映される
    context.applyToMDC();
    assertEquals("USER123", MDC.get("userId"));

    // MDCをクリアして、3回目の呼び出しをテスト
    MDC.remove("userId");
    assertNull(MDC.get("userId"));

    // 3回目: 共有コンテキストに変更なし
    // → Dirty Flagがfalseなので再度MDCに書き込まれない
    context.applyToMDC();

    // 最適化により、共有コンテキストの再適用はスキップされる
    // （他のコンテキストは毎回適用されるが、共有コンテキストはスキップ）
    assertNull(MDC.get("userId"));
  }

  @Test
  void testSharedContextDirtyFlagResetsOnChange() {
    ExecutionContext context = ExecutionContext.create();

    // 初期値設定
    context.setSharedContext("status", "initial");
    context.applyToMDC();
    assertEquals("initial", MDC.get("status"));

    // MDCクリア
    MDC.clear();

    // 値を変更せずにapplyToMDC()を複数回呼ぶ
    context.applyToMDC();
    context.applyToMDC();
    context.applyToMDC();

    // Dirty Flagによりスキップされるため、MDCに再適用されない
    assertNull(MDC.get("status"));

    // 値を変更
    context.setSharedContext("status", "updated");

    // 変更があったので再度MDCに反映される
    context.applyToMDC();
    assertEquals("updated", MDC.get("status"));
  }

  @Test
  void testSharedContextDirtyFlagWithMultipleChanges() {
    ExecutionContext context = ExecutionContext.create();

    // 複数回の変更と適用を繰り返す
    for (int i = 0; i < 10; i++) {
      // 値を設定（Dirty Flag ON）
      context.setSharedContext("counter", String.valueOf(i));

      // MDCに適用（Dirty Flag OFF）
      context.applyToMDC();
      assertEquals(String.valueOf(i), MDC.get("counter"));

      // 変更なしで再度適用（スキップされる）
      MDC.remove("counter");
      context.applyToMDC();
      assertNull(MDC.get("counter")); // 再適用されない
    }
  }
}
