package com.streamconverter.command.rule.impl.string;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TrimRuleTest {

  private final TrimRule rule = new TrimRule();

  @Test
  void applyTrimsLeadingAndTrailingWhitespace() {
    assertEquals("hello", rule.apply("  hello  "));
  }

  @Test
  void applyTrimsTabsAndNewlines() {
    assertEquals("world", rule.apply("\t\nworld\r\n"));
  }

  @Test
  void applyAlreadyTrimmedStringIsUnchanged() {
    assertEquals("already trimmed", rule.apply("already trimmed"));
  }

  @Test
  void applyWhitespaceOnlyReturnsEmpty() {
    assertEquals("", rule.apply("   "));
  }

  @Test
  void applyEmptyStringReturnsEmpty() {
    assertEquals("", rule.apply(""));
  }

  @Test
  void applyNullReturnsNull() {
    assertNull(rule.apply(null));
  }

  @Test
  void equalsTrimRuleInstance() {
    assertEquals(rule, new TrimRule());
  }

  @Test
  void equalsNullReturnsFalse() {
    assertNotEquals(rule, null);
  }

  @Test
  void equalsDifferentTypeReturnsFalse() {
    assertNotEquals(rule, "not a TrimRule");
  }

  @Test
  void hashCodeIsConsistent() {
    assertEquals(rule.hashCode(), new TrimRule().hashCode());
  }

  @Test
  void toStringContainsTrimRule() {
    assertTrue(rule.toString().contains("TrimRule"));
  }
}
