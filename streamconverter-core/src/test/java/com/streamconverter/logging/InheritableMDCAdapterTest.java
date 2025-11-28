package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** InheritableMDCAdapterの動作を検証するテスト */
class InheritableMDCAdapterTest {

  private InheritableMDCAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InheritableMDCAdapter();
    adapter.clear();
  }

  @AfterEach
  void tearDown() {
    adapter.clear();
  }

  @Test
  void testBasicPutAndGet() {
    adapter.put("key1", "value1");
    assertEquals("value1", adapter.get("key1"));

    adapter.put("key2", "value2");
    assertEquals("value2", adapter.get("key2"));
  }

  @Test
  void testRemove() {
    adapter.put("key1", "value1");
    assertEquals("value1", adapter.get("key1"));

    adapter.remove("key1");
    assertNull(adapter.get("key1"));
  }

  @Test
  void testClear() {
    adapter.put("key1", "value1");
    adapter.put("key2", "value2");

    adapter.clear();

    assertNull(adapter.get("key1"));
    assertNull(adapter.get("key2"));
  }

  @Test
  void testGetCopyOfContextMap() {
    adapter.put("key1", "value1");
    adapter.put("key2", "value2");

    Map<String, String> copy = adapter.getCopyOfContextMap();
    assertNotNull(copy);
    assertEquals(2, copy.size());
    assertEquals("value1", copy.get("key1"));
    assertEquals("value2", copy.get("key2"));

    // コピーを変更しても元のMDCには影響しない
    copy.put("key3", "value3");
    assertNull(adapter.get("key3"));
  }

  @Test
  void testSetContextMap() {
    adapter.put("oldKey", "oldValue");

    Map<String, String> newMap = Map.of("key1", "value1", "key2", "value2");
    adapter.setContextMap(newMap);

    assertNull(adapter.get("oldKey"));
    assertEquals("value1", adapter.get("key1"));
    assertEquals("value2", adapter.get("key2"));
  }

  @Test
  void testPlatformThreads_InheritsMDC() throws Exception {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    // 親スレッドでMDC設定
    adapter.put("userId", "USER123");
    adapter.put("requestId", "REQ456");

    // 子スレッドで確認
    CompletableFuture<String> userIdFuture =
        CompletableFuture.supplyAsync(() -> adapter.get("userId"), executor);
    CompletableFuture<String> requestIdFuture =
        CompletableFuture.supplyAsync(() -> adapter.get("requestId"), executor);

    // InheritableMDCAdapterを使うと子スレッドに伝播する
    assertEquals("USER123", userIdFuture.get());
    assertEquals("REQ456", requestIdFuture.get());

    executor.shutdown();
  }

  @Test
  void testVirtualThreads_InheritsMDC() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    adapter.put("userId", "USER123");
    adapter.put("requestId", "REQ456");

    // Virtual Threadで確認
    CompletableFuture<String> userIdFuture =
        CompletableFuture.supplyAsync(() -> adapter.get("userId"), executor);
    CompletableFuture<String> requestIdFuture =
        CompletableFuture.supplyAsync(() -> adapter.get("requestId"), executor);

    // InheritableMDCAdapterを使うとVirtual Threadでも伝播する
    assertEquals("USER123", userIdFuture.get());
    assertEquals("REQ456", requestIdFuture.get());

    executor.shutdown();
  }

  @Test
  void testChildThreadModification_DoesNotAffectParent() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    adapter.put("userId", "USER123");

    // 子スレッドで値を変更
    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              assertEquals("USER123", adapter.get("userId"));
              adapter.put("userId", "USER999"); // 子スレッドで変更
              assertEquals("USER999", adapter.get("userId"));
            },
            executor);

    future.get();

    // 親スレッドの値は変更されていない
    assertEquals("USER123", adapter.get("userId"));

    executor.shutdown();
  }

  @Test
  void testMultipleChildThreads_IndependentMDC() throws Exception {
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // 親スレッドでMDC設定
    adapter.put("userId", "PARENT");

    // 複数の子スレッドで独立して変更
    CompletableFuture<String> future1 =
        CompletableFuture.supplyAsync(
            () -> {
              adapter.put("userId", "CHILD1");
              return adapter.get("userId");
            },
            executor);

    CompletableFuture<String> future2 =
        CompletableFuture.supplyAsync(
            () -> {
              adapter.put("userId", "CHILD2");
              return adapter.get("userId");
            },
            executor);

    // 各子スレッドは独立したMDC値を持つ
    assertEquals("CHILD1", future1.get());
    assertEquals("CHILD2", future2.get());

    // 親スレッドの値は変更されていない
    assertEquals("PARENT", adapter.get("userId"));

    executor.shutdown();
  }
}
