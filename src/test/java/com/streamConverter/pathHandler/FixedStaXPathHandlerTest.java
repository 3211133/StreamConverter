package com.streamConverter.pathHandler;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class FixedStaXPathHandlerTest {

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
    assertTrue(handler.isTargetXpath(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathDoesNotMatchInSize() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of("a", "b");
    assertFalse(handler.isTargetXpath(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathDoesNotMatchInContent() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of("a", "x", "c");
    assertFalse(handler.isTargetXpath(input));
  }

  @Test
  void isTargetXpath_shouldReturnFalse_whenXpathIsEmpty() {
    FixedStaXPathHandler handler = new FixedStaXPathHandler("a/b/c");
    List<String> input = List.of();
    assertFalse(handler.isTargetXpath(input));
  }
}
