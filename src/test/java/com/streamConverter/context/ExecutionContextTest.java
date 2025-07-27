package com.streamConverter.context;

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
    assertThrows(UnsupportedOperationException.class, () -> {
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
}
