package com.streamconverter.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InheritableMDCAdapter}. */
class InheritableMDCAdapterTest {

  private InheritableMDCAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new InheritableMDCAdapter();
  }

  @AfterEach
  void tearDown() {
    adapter.clear();
  }

  @Test
  void put_and_get_returnsValue() {
    adapter.put("key", "value");
    assertEquals("value", adapter.get("key"));
  }

  @Test
  void get_missingKey_returnsNull() {
    assertNull(adapter.get("nonexistent"));
  }

  @Test
  void remove_removesKey() {
    adapter.put("key", "value");
    adapter.remove("key");
    assertNull(adapter.get("key"));
  }

  @Test
  void clear_removesAllKeys() {
    adapter.put("a", "1");
    adapter.put("b", "2");
    adapter.clear();
    assertNull(adapter.get("a"));
    assertNull(adapter.get("b"));
  }

  @Test
  void clear_alsoRemovesDequeEntries() {
    adapter.pushByKey("stack", "value");
    adapter.clear();
    // clear() must also release the deque ThreadLocal slot
    assertNull(adapter.getCopyOfDequeByKey("stack"));
  }

  @Test
  void getCopyOfContextMap_returnsDefensiveCopy() {
    adapter.put("key", "value");
    Map<String, String> copy = adapter.getCopyOfContextMap();
    assertNotNull(copy);
    assertEquals("value", copy.get("key"));

    // Mutating the copy does not affect the adapter
    copy.put("key", "modified");
    assertEquals("value", adapter.get("key"));
  }

  @Test
  void getCopyOfContextMap_emptyMap_returnsNull() {
    assertNull(adapter.getCopyOfContextMap());
  }

  @Test
  void setContextMap_replacesEntries() {
    adapter.put("old", "oldValue");
    adapter.setContextMap(Map.of("new", "newValue"));
    assertNull(adapter.get("old"));
    assertEquals("newValue", adapter.get("new"));
  }

  @Test
  void setContextMap_null_clearsMap() {
    adapter.put("key", "value");
    adapter.setContextMap(null);
    assertNull(adapter.get("key"));
  }

  @Test
  void put_nullKey_throwsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> adapter.put(null, "value"));
  }

  @Test
  void childPlatformThread_inheritsParentMDC() throws InterruptedException {
    adapter.put("requestId", "REQ-001");

    AtomicReference<String> childValue = new AtomicReference<>();
    Thread child = new Thread(() -> childValue.set(adapter.get("requestId")));
    child.start();
    child.join();

    assertEquals("REQ-001", childValue.get());
  }

  @Test
  void childVirtualThread_inheritsParentMDC() throws InterruptedException {
    adapter.put("requestId", "VREQ-001");

    AtomicReference<String> childValue = new AtomicReference<>();
    Thread child = Thread.ofVirtual().start(() -> childValue.set(adapter.get("requestId")));
    child.join();

    assertEquals("VREQ-001", childValue.get());
  }

  @Test
  void childThread_doesNotAffectParentMDC() throws InterruptedException {
    adapter.put("key", "parent");

    Thread child =
        new Thread(
            () -> {
              adapter.put("key", "child");
            });
    child.start();
    child.join();

    // Parent's value should be unchanged
    assertEquals("parent", adapter.get("key"));
  }

  @Test
  void independentChildThreads_haveIndependentCopies() throws InterruptedException {
    adapter.put("shared", "original");

    AtomicReference<String> child1Value = new AtomicReference<>();
    AtomicReference<String> child2Value = new AtomicReference<>();

    Thread child1 =
        new Thread(
            () -> {
              adapter.put("shared", "from-child1");
              child1Value.set(adapter.get("shared"));
            });
    Thread child2 =
        new Thread(
            () -> {
              adapter.put("shared", "from-child2");
              child2Value.set(adapter.get("shared"));
            });

    child1.start();
    child2.start();
    child1.join();
    child2.join();

    assertEquals("from-child1", child1Value.get());
    assertEquals("from-child2", child2Value.get());
    // Parent is unaffected
    assertEquals("original", adapter.get("shared"));
  }

  @Test
  void pushByKey_and_popByKey_workLIFO() {
    adapter.pushByKey("stack", "first");
    adapter.pushByKey("stack", "second");
    assertEquals("second", adapter.popByKey("stack"));
    assertEquals("first", adapter.popByKey("stack"));
    // Empty stack throws NoSuchElementException per SLF4J spec
    assertThrows(NoSuchElementException.class, () -> adapter.popByKey("stack"));
  }

  @Test
  void popByKey_nullKey_returnsNull() {
    assertNull(adapter.popByKey(null));
  }

  @Test
  void pushByKey_nullKey_isNoOp() {
    // null key is no-op per SLF4J spec
    assertDoesNotThrow(() -> adapter.pushByKey(null, "value"));
  }

  @Test
  void getCopyOfDequeByKey_returnsDefensiveCopy() {
    adapter.pushByKey("stack", "value");
    var copy = adapter.getCopyOfDequeByKey("stack");
    assertNotNull(copy);
    assertEquals(1, copy.size());

    // Mutating the copy does not affect the adapter
    copy.clear();
    assertEquals(1, adapter.getCopyOfDequeByKey("stack").size());
  }

  @Test
  void clearDequeByKey_clearsDeque() {
    adapter.pushByKey("stack", "value");
    adapter.clearDequeByKey("stack");
    // The deque is cleared (emptied) but still present; getCopyOfDequeByKey returns empty deque
    var deque = adapter.getCopyOfDequeByKey("stack");
    assertNotNull(deque);
    assertTrue(deque.isEmpty());
  }
}
