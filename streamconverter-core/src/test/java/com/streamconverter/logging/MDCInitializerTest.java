package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/** MDCInitializerの動作を検証するテスト */
class MDCInitializerTest {

  @BeforeEach
  void setUp() {
    MDC.clear();
    MDCInitializer.initialize();
  }

  @AfterEach
  void tearDown() {
    MDC.clear();
  }

  @Test
  void testInitialize_IdempotentCall() {
    // 初期化は冪等であることを確認
    assertTrue(MDCInitializer.isInitialized());

    // 2回目の呼び出しでも問題ない
    MDCInitializer.initialize();
    assertTrue(MDCInitializer.isInitialized());
  }

  @Test
  void testMDC_PropagatesTo_PlatformThreads() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    // 親スレッドでMDC設定
    MDC.put("userId", "USER123");
    MDC.put("requestId", "REQ456");

    // 子スレッドで確認
    CompletableFuture<String> userIdFuture =
        CompletableFuture.supplyAsync(() -> MDC.get("userId"), executor);
    CompletableFuture<String> requestIdFuture =
        CompletableFuture.supplyAsync(() -> MDC.get("requestId"), executor);

    // InheritableMDCAdapterにより、子スレッドに伝播する
    assertEquals("USER123", userIdFuture.get());
    assertEquals("REQ456", requestIdFuture.get());

    executor.shutdown();
  }

  @Test
  void testMDC_PropagatesTo_VirtualThreads() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    MDC.put("userId", "USER123");
    MDC.put("requestId", "REQ456");

    // Virtual Threadで確認
    CompletableFuture<String> userIdFuture =
        CompletableFuture.supplyAsync(() -> MDC.get("userId"), executor);
    CompletableFuture<String> requestIdFuture =
        CompletableFuture.supplyAsync(() -> MDC.get("requestId"), executor);

    // InheritableMDCAdapterにより、Virtual Threadにも伝播する
    assertEquals("USER123", userIdFuture.get());
    assertEquals("REQ456", requestIdFuture.get());

    executor.shutdown();
  }

  @Test
  void testMDC_ChildModification_DoesNotAffectParent() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    MDC.put("userId", "USER123");

    // 子スレッドで値を変更
    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              assertEquals("USER123", MDC.get("userId"));
              MDC.put("userId", "USER999"); // 子スレッドで変更
              assertEquals("USER999", MDC.get("userId"));
            },
            executor);

    future.get();

    // 親スレッドの値は変更されていない
    assertEquals("USER123", MDC.get("userId"));

    executor.shutdown();
  }

  @Test
  void testMDC_MultipleChildThreads_IndependentContexts() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    MDC.put("userId", "PARENT");

    // 複数の子スレッドで独立して変更
    CompletableFuture<String> future1 =
        CompletableFuture.supplyAsync(
            () -> {
              MDC.put("userId", "CHILD1");
              return MDC.get("userId");
            },
            executor);

    CompletableFuture<String> future2 =
        CompletableFuture.supplyAsync(
            () -> {
              MDC.put("userId", "CHILD2");
              return MDC.get("userId");
            },
            executor);

    // 各子スレッドは独立したMDC値を持つ
    assertEquals("CHILD1", future1.get());
    assertEquals("CHILD2", future2.get());

    // 親スレッドの値は変更されていない
    assertEquals("PARENT", MDC.get("userId"));

    executor.shutdown();
  }
}
