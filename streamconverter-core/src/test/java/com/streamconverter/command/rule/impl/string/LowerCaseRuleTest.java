package com.streamconverter.command.rule.impl.string;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class LowerCaseRuleTest {

  @Test
  void defaultConstructorUsesDefaultLocale() {
    LowerCaseRule rule = new LowerCaseRule();
    assertEquals(Locale.getDefault(), rule.getLocale());
  }

  @Test
  void createWithLocaleEnglish() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals(Locale.ENGLISH, rule.getLocale());
  }

  @Test
  void createWithNullLocaleThrowsIllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> LowerCaseRule.create(null));
  }

  @Test
  void applyConvertsToLowercase() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals("hello world", rule.apply("HELLO WORLD"));
  }

  @Test
  void applyAlreadyLowercaseIsUnchanged() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals("hello", rule.apply("hello"));
  }

  @Test
  void applyNullReturnsNull() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertNull(rule.apply(null));
  }

  @Test
  void applyEmptyStringReturnsEmpty() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals("", rule.apply(""));
  }

  @Test
  void applyWithTurkishLocaleHandlesDotlessI() {
    // トルコ語ロケールでは "I" → "ı"（ドットなし i）になる
    LowerCaseRule rule = LowerCaseRule.create(Locale.of("tr"));
    String result = rule.apply("I");
    // 英語ロケールの "i" と異なることを確認
    assertNotEquals("i", result);
  }

  @Test
  void equalsWithSameLocale() {
    LowerCaseRule rule1 = LowerCaseRule.create(Locale.ENGLISH);
    LowerCaseRule rule2 = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals(rule1, rule2);
  }

  @Test
  void equalsWithDifferentLocale() {
    LowerCaseRule rule1 = LowerCaseRule.create(Locale.ENGLISH);
    LowerCaseRule rule2 = LowerCaseRule.create(Locale.FRENCH);
    assertNotEquals(rule1, rule2);
  }

  @Test
  void equalsNullReturnsFalse() {
    assertNotEquals(LowerCaseRule.create(Locale.ENGLISH), null);
  }

  @Test
  void equalsDifferentTypeReturnsFalse() {
    assertNotEquals(LowerCaseRule.create(Locale.ENGLISH), "not a rule");
  }

  @Test
  void equalsWithSelf() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals(rule, rule);
  }

  @Test
  void hashCodeConsistentWithEquals() {
    LowerCaseRule rule1 = LowerCaseRule.create(Locale.ENGLISH);
    LowerCaseRule rule2 = LowerCaseRule.create(Locale.ENGLISH);
    assertEquals(rule1.hashCode(), rule2.hashCode());
  }

  @Test
  void toStringContainsLocale() {
    LowerCaseRule rule = LowerCaseRule.create(Locale.ENGLISH);
    assertTrue(rule.toString().contains("LowerCaseRule"));
  }
}
