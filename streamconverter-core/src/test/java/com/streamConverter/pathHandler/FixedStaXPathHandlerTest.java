package com.streamConverter.pathHandler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Additional tests for FixedStaXPathHandler edge cases. */
class FixedStaXPathHandlerTest {

  @Test
  public void testLeadingSlashHandling() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("/root/child");
    List<String> expected = Arrays.asList("root", "child");
    assertEquals(expected, handler.getTargetXpath(), "Leading slash should be normalized");
  }

  @Test
  public void testTrailingSlashHandling() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("root/child/");
    List<String> expected = Arrays.asList("root", "child");
    assertEquals(expected, handler.getTargetXpath(), "Trailing slash should be normalized");
  }

  @Test
  public void testMultipleSlashHandling() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("//root///child//");
    List<String> expected = Arrays.asList("root", "child");
    assertEquals(expected, handler.getTargetXpath(), "Multiple slashes should be normalized");
  }

  @Test
  public void testOnlySlashesThrowsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new FixedStaXPathHandler("///"),
        "Only slashes should throw IllegalArgumentException");
  }

  @Test
  public void testWhitespaceHandling() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("  /root/child  ");
    List<String> expected = Arrays.asList("root", "child");
    assertEquals(expected, handler.getTargetXpath(), "Whitespace should be trimmed");
  }

  @Test
  public void testComplexPathMatching() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("document/section/paragraph");

    assertTrue(
        handler.isTarget(Arrays.asList("document", "section", "paragraph")),
        "Exact match should return true");

    assertFalse(
        handler.isTarget(Arrays.asList("document", "section")),
        "Partial match should return false");

    assertFalse(
        handler.isTarget(Arrays.asList("document", "section", "paragraph", "extra")),
        "Longer path should return false");
  }

  @Test
  public void testNullXpathListThrowsException() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("root/child");

    assertThrows(
        IllegalArgumentException.class,
        () -> handler.isTarget(null),
        "Null xpath list should throw IllegalArgumentException");
  }

  @Test
  void constructor_shouldThrowException_whenXpathIsNull() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new FixedStaXPathHandler(null);
            });
    assertEquals("xpath must not be null", exception.getMessage());
  }

  @Test
  void isTargetXpath_shouldReturnTrue_whenXpathMatchesExactly() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of("a", "b", "c");
    assertTrue(handler.isTarget(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathDoesNotMatchInSize() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of("a", "b");
    assertFalse(handler.isTarget(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathDoesNotMatchInContent() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of("a", "x", "c");
    assertFalse(handler.isTarget(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathIsEmpty() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of();
    assertFalse(handler.isTarget(input));
  }

  @Test
  void constructor_shouldThrowException_whenXpathIsEmptyString() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new FixedStaXPathHandler("");
            });
    assertEquals("xpath must not be empty", exception.getMessage());
  }

  @Test
  void constructor_shouldThrowException_whenXpathIsWhitespace() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              new FixedStaXPathHandler("   ");
            });
    assertEquals("xpath must not be empty", exception.getMessage());
  }

  @Test
  void constructor_shouldNormalizeLeadingSlashes() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("/root/child");

    assertTrue(handler.isTarget(List.of("root", "child")));
    assertEquals(List.of("root", "child"), handler.getTargetXpath());
  }

  @Test
  void constructor_shouldNormalizeTrailingSlashes() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("root/child/");

    assertTrue(handler.isTarget(List.of("root", "child")));
    assertEquals(List.of("root", "child"), handler.getTargetXpath());
  }

  @Test
  void constructor_shouldNormalizeMultipleSlashes() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("root//child///grandchild");

    assertTrue(handler.isTarget(List.of("root", "child", "grandchild")));
    assertEquals(List.of("root", "child", "grandchild"), handler.getTargetXpath());
  }

  @Test
  void constructor_shouldThrowException_whenXpathIsJustSlashes() {
    assertThrows(IllegalArgumentException.class, () -> new FixedStaXPathHandler("/"));
    assertThrows(IllegalArgumentException.class, () -> new FixedStaXPathHandler("//"));
    assertThrows(IllegalArgumentException.class, () -> new FixedStaXPathHandler("///"));
  }

  @Test
  void isTarget_shouldThrowException_whenXpathListIsNull() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("root/child");

    assertThrows(IllegalArgumentException.class, () -> handler.isTarget(null));
  }
}
